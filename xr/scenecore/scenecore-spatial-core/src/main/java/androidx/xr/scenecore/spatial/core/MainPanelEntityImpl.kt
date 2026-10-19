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

import android.app.Activity
import android.graphics.Rect
import androidx.annotation.GuardedBy
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.requiresApiLevel
import com.android.extensions.xr.XrExtensionResult
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService

/**
 * MainPanelEntity is a special instance of a PanelEntity that is backed by the WindowLeash CPM
 * node. The content of this PanelEntity is assumed to have been previously defined and associated
 * with the Window Leash Node.
 */
internal class MainPanelEntityImpl(
    activity: Activity,
    node: Node,
    extensions: XrExtensions,
    sceneNodeRegistry: SceneNodeRegistry,
    executor: ScheduledExecutorService,
) : BasePanelEntity(activity, node, extensions, sceneNodeRegistry, executor), PanelEntity {
    // Note that we expect the Node supplied here to be the WindowLeash node.
    init {
        val cornerRadius = defaultCornerRadiusInMeters
        extensions.createNodeTransaction().use { transaction ->
            transaction.setCornerRadius(node, cornerRadius).apply()
        }
        super.cornerRadiusValue = cornerRadius
    }

    private val resizeLock = Any()

    /**
     * Tracks the target [PixelDimensions] of the active asynchronous resize, or `null` when idle.
     *
     * Set when a user drags the main panel's resize handle via
     * [androidx.xr.scenecore.runtime.ResizableComponent] or triggers a UI layout change that
     * updates [sizeInPixels] or [size]. Remains non-null while [XrExtensions.setMainWindowSize]
     * asynchronously resizes the [Activity] window, and resets to `null` (or advances to
     * [latestDeferredPixelSizeRequest]) in [handleSetSizeComplete].
     */
    @GuardedBy("resizeLock") private var inFlightTargetSize: PixelDimensions? = null

    /**
     * Stores the latest [PixelDimensions] requested while [inFlightTargetSize] is non-null,
     * overwriting older pending requests so only the most recent size runs next.
     */
    @GuardedBy("resizeLock") private var latestDeferredPixelSizeRequest: PixelDimensions? = null

    private val boundsFromWindowManager: Rect
        get() = requiresApiLevel(30) { activity!!.windowManager.currentWindowMetrics.bounds }

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
            var shouldExecuteNow = false
            synchronized(resizeLock) {
                if (inFlightTargetSize != null) {
                    // An asynchronous resize IPC is already in-flight. Compare against the latest
                    // pending target size (either a previously deferred request, or the currently
                    // executing in-flight target) to ignore redundant duplicate requests;
                    // otherwise, coalesce by overwriting the deferred request to be sent once the
                    // current IPC completes.
                    val targetInProgress = latestDeferredPixelSizeRequest ?: inFlightTargetSize
                    if (value == targetInProgress) {
                        return
                    }
                    latestDeferredPixelSizeRequest = value
                } else {
                    // No resize is in-flight. Compare directly against live WindowManager bounds
                    // (rather than a cached value, which can become stale if the system resizes the
                    // window externally) and skip the IPC if the window is already at the requested
                    // size.
                    if (value == sizeInPixels) {
                        return
                    }
                    inFlightTargetSize = value
                    isSetSizePending.set(true)
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
                inFlightTargetSize = null
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
                inFlightTargetSize = nextSizeToExecute
                latestDeferredPixelSizeRequest = null
            } else {
                inFlightTargetSize = null
            }
        }

        if (nextSizeToExecute != null) {
            executeSetMainWindowSizeAsync(nextSizeToExecute)
        } else {
            isSetSizePending.set(false)
            notifyOnSetSizeComplete()
        }
    }
}
