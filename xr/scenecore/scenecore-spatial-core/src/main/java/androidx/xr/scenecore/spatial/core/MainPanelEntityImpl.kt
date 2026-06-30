/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.xr.scenecore.spatial.core

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import com.android.extensions.xr.XrExtensionResult
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * MainPanelEntity is a special instance of a PanelEntity that is backed by the WindowLeash CPM
 * node. The content of this PanelEntity is assumed to have been previously defined and associated
 * with the Window Leash Node.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
internal class MainPanelEntityImpl(
    activity: Activity,
    node: Node,
    extensions: XrExtensions,
    sceneNodeRegistry: SceneNodeRegistry,
    executor: ScheduledExecutorService,
) : BasePanelEntity(activity, node, extensions, sceneNodeRegistry, executor), PanelEntity {
    // Note that we expect the Node supplied here to be the WindowLeash node.
    init {
        // Read the Pixel dimensions for the primary panel off the Activity's WindowManager. Note
        // that this requires MinAPI 30.
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        super.sizeInPixels =
            PixelDimensions(boundsFromWindowManager.width(), boundsFromWindowManager.height())
        val cornerRadius = defaultCornerRadiusInMeters
        extensions.createNodeTransaction().use { transaction ->
            transaction.setCornerRadius(node, cornerRadius).apply()
        }
        super.cornerRadiusValue = cornerRadius
    }

    private val resizeCompleteListeners = AtomicReference<Map<Runnable, Executor>>(emptyMap())

    /**
     * Registers a callback to be invoked after an asynchronous set size operation completes.
     *
     * The callback is guaranteed to be invoked whether the underlying IPC call succeeds or fails
     * (e.g., throws an exception), ensuring listeners can safely recover their state.
     *
     * The listener will be executed on the provided [executor].
     *
     * @param executor The executor on which to run the listener.
     * @param listener The task to execute upon completion.
     */
    internal fun addOnSetSizeCompleteListener(executor: Executor, listener: Runnable) {
        resizeCompleteListeners.updateAndGet { it + (listener to executor) }
    }

    /**
     * Unregisters a previously added size-change listener. The provided `listener` must be the same
     * instance used for registration to prevent memory leaks.
     *
     * @param listener The listener instance to unregister.
     */
    internal fun removeOnSetSizeCompleteListener(listener: Runnable) {
        resizeCompleteListeners.updateAndGet { it - listener }
    }

    private fun notifyOnSetSizeComplete() {
        val currentListeners = resizeCompleteListeners.get()
        for ((listener, executor) in currentListeners) {
            try {
                executor.execute(listener)
            } catch (e: RejectedExecutionException) {
                // Catch this exception to prevent the loop from terminating early,
                // ensuring subsequent listeners are still notified.
                Log.e(
                    TAG,
                    "Internal Error: Failed to notify OnSetSizeCompleteListener. " +
                        "The internal executor rejected the task. Listener: $listener",
                    e,
                )
            } catch (e: RuntimeException) {
                Log.e(
                    TAG,
                    "Internal Error: Unexpected exception thrown by an internal OnSetSizeCompleteListener. " +
                        "Listener: $listener",
                    e,
                )
            }
        }
    }

    private val isSetSizePending = AtomicBoolean(false)

    internal fun isWaitingForSetSize(): Boolean = isSetSizePending.get()

    private val resizeLock = Any()

    /** Indicates if the asynchronous IPC call to set the window size is currently in-flight. */
    @GuardedBy("resizeLock") private var isExecutingSetMainWindowSizeAsync = false

    /**
     * Holds the most recent size request received while an operation is in-flight. Older pending
     * requests are overwritten to ensure we only process the latest state.
     */
    @GuardedBy("resizeLock") private var latestDeferredPixelSizeRequest: PixelDimensions? = null

    private val boundsFromWindowManager: Rect
        get() = activity!!.windowManager.currentWindowMetrics.bounds

    override var size: Dimensions
        get() {
            // The main panel bounds can change in HSM without JXRCore. Always read the bounds from
            // the WindowManager.
            return Dimensions(
                boundsFromWindowManager.width() / defaultPixelDensity,
                boundsFromWindowManager.height() / defaultPixelDensity,
                0f,
            )
        }
        set(value) {
            super.size = value
        }

    override var sizeInPixels: PixelDimensions
        get() {
            // The main panel bounds can change in HSM without JXRCore. Always read the bounds from
            // the WindowManager.
            return PixelDimensions(
                boundsFromWindowManager.width(),
                boundsFromWindowManager.height(),
            )
        }
        set(value) {
            // TODO: b/376126162 - Consider calling setPixelDimensions() either when
            // setMainWindowSize's callback is called, or when the next spatial state callback with
            // the expected size is called.
            if (value == super.sizeInPixels) {
                return
            }
            super.sizeInPixels = value
            isSetSizePending.set(true)

            var shouldExecuteNow = false
            // Execute immediately, or overwrite the pending request if already busy.
            synchronized(resizeLock) {
                if (isExecutingSetMainWindowSizeAsync) {
                    latestDeferredPixelSizeRequest = value
                } else {
                    isExecutingSetMainWindowSizeAsync = true
                    shouldExecuteNow = true
                }
            }
            if (shouldExecuteNow) {
                executeSetMainWindowSizeAsync(value)
            }
        }

    private fun executeSetMainWindowSizeAsync(targetSize: PixelDimensions) {
        // TODO: b/376934871 - Check async results.
        try {
            extensions.setMainWindowSize(
                activity,
                targetSize.width,
                targetSize.height,
                { obj: Runnable -> obj.run() },
                { _: XrExtensionResult? -> handleSetSizeComplete() },
            )
        } catch (e: Exception) {
            synchronized(resizeLock) {
                isExecutingSetMainWindowSizeAsync = false
                latestDeferredPixelSizeRequest = null
            }
            isSetSizePending.set(false)

            // Notify listeners to ensure UI state (e.g., restoring alpha)
            // is properly recovered even if the IPC call fails.
            notifyOnSetSizeComplete()

            throw e
        }
    }

    private fun handleSetSizeComplete() {
        var nextSizeToExecute: PixelDimensions? = null

        synchronized(resizeLock) {
            if (latestDeferredPixelSizeRequest != null) {
                nextSizeToExecute = latestDeferredPixelSizeRequest
                latestDeferredPixelSizeRequest = null
            } else {
                isExecutingSetMainWindowSizeAsync = false
            }
        }

        if (nextSizeToExecute != null) {
            executeSetMainWindowSizeAsync(nextSizeToExecute)
        } else {
            isSetSizePending.set(false)
            notifyOnSetSizeComplete()
        }
    }

    private companion object {
        const val TAG = "MainPanelEntityImpl"
    }
}
