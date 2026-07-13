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

package androidx.camera.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.INVALID_ROTATION
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provider for receiving rotation updates from the device's sensors.
 *
 * This class monitors the motion sensor and notifies listeners about physical orientation changes
 * in the format of [android.view.Surface] rotation.
 *
 * To avoid rapid rotation changes, a hysteresis mechanism is applied. When the orientation is close
 * to a boundary, the rotation will not change until it crosses a threshold.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RotationProvider {

    private val lock = Any()

    private val appContext: Context

    @GuardedBy("lock") private var orientationListener: OrientationEventListener? = null

    @GuardedBy("lock") private val listeners = mutableMapOf<Listener, ListenerWrapper>()

    @Volatile private var rotation: Int = INVALID_ROTATION
    private val ignoreCanDetectForTest: Boolean
    @get:VisibleForTesting
    @Volatile
    public var isShutdown: Boolean = false
        private set

    @GuardedBy("lock") private var backgroundThread: HandlerThread? = null
    @GuardedBy("lock") private var backgroundHandler: Handler? = null
    private val hasAccelerometer: Boolean by lazy {
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
    }

    /**
     * Creates a new RotationProvider.
     *
     * @param appContext The application context.
     */
    public constructor(appContext: Context) : this(appContext, false)

    /**
     * Creates a new RotationProvider.
     *
     * @param appContext The application context.
     * @param ignoreCanDetectForTest Whether to ignore the check for whether the device can detect
     *   orientation. This should only be set to true for testing.
     */
    @VisibleForTesting
    public constructor(appContext: Context, ignoreCanDetectForTest: Boolean = false) {
        this.appContext = appContext.applicationContext
        this.ignoreCanDetectForTest = ignoreCanDetectForTest
    }

    private fun updateRotation(newRotation: Int) {
        if (rotation != newRotation) {
            rotation = newRotation
            val listenersSnapshot: List<ListenerWrapper>
            synchronized(lock) { listenersSnapshot = listeners.values.toList() }
            listenersSnapshot.forEach { it.onRotationChanged(newRotation) }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public fun updateOrientationForTesting(orientation: Int) {
        updateRotation(orientationToSurfaceRotation(orientation))
    }

    /**
     * Adds a listener that listens for rotation changes.
     *
     * @return false if the device cannot detect rotation changes. In that case, the listener will
     *   not be set.
     */
    public fun addListener(executor: Executor, listener: Listener): Boolean {
        synchronized(lock) {
            if (isShutdown) {
                return false
            }
            if (!ignoreCanDetectForTest) {
                // Perform a fast, non-blocking check for accelerometer availability.
                // This is equivalent to OrientationEventListener.canDetectOrientation()
                // but can be run synchronously on the caller thread without triggering
                // any Sensor HAL deadlocks.
                if (!hasAccelerometer) {
                    return false
                }
            }
            val listenerWrapper = ListenerWrapper(listener, executor)
            listeners[listener] = listenerWrapper
            if (rotation != INVALID_ROTATION) {
                @SuppressLint("WrongConstant") listenerWrapper.onRotationChanged(rotation)
            }
            if (listeners.size == 1) {
                if (backgroundThread == null) {
                    val thread = HandlerThread(CameraXThreads.TAG + "RotationProvider")
                    thread.start()
                    backgroundThread = thread
                    backgroundHandler = Handler(thread.looper)
                }
                backgroundHandler?.post {
                    synchronized(lock) {
                        if (isShutdown || listeners.isEmpty()) {
                            return@post
                        }
                        var localListener = orientationListener
                        if (localListener == null) {
                            localListener =
                                object : OrientationEventListener(appContext) {
                                    override fun onOrientationChanged(orientation: Int) {
                                        if (orientation == ORIENTATION_UNKNOWN) {
                                            return
                                        }
                                        val newRotation = orientationToSurfaceRotation(orientation)
                                        updateRotation(newRotation)
                                    }
                                }
                            orientationListener = localListener
                        }
                        localListener.enable()
                    }
                }
            }
        }
        return true
    }

    /**
     * Removes the given listener from this object.
     *
     * The removed listener will no longer receive rotation updates.
     */
    public fun removeListener(listener: Listener) {
        synchronized(lock) {
            val listenerWrapper = listeners[listener]
            if (listenerWrapper != null) {
                listenerWrapper.disable()
                listeners.remove(listener)
            }
            if (listeners.isEmpty()) {
                val threadToQuit = backgroundThread
                val listenerToDisable = orientationListener
                backgroundHandler?.post {
                    listenerToDisable?.disable()
                    threadToQuit?.quitSafely()
                }
                backgroundThread = null
                backgroundHandler = null
                orientationListener = null
                rotation = INVALID_ROTATION
            }
        }
    }

    /** Shuts down the listener. */
    public fun shutdown() {
        synchronized(lock) {
            isShutdown = true
            listeners.values.forEach { it.disable() }
            listeners.clear()
            val threadToQuit = backgroundThread
            val listenerToDisable = orientationListener
            backgroundHandler?.post {
                listenerToDisable?.disable()
                threadToQuit?.quitSafely()
            }
            backgroundThread = null
            backgroundHandler = null
            orientationListener = null
            rotation = INVALID_ROTATION
        }
    }

    private fun orientationToSurfaceRotation(orientation: Int) =
        if (rotation == INVALID_ROTATION) {
            // If current rotation is INVALID_ROTATION, calculate a result value without
            // hysteresis zone.
            when (orientation) {
                in 0..44 -> Surface.ROTATION_0
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0 // orientation in 315..359
            }
        } else {
            // Otherwise, the rotation value will be changed only when the new orientation value
            // has crossed a hysteresis range. For example, when the previous rotation is
            // ROTATION_0, the device needs to be rotated to 50 degrees to make the rotation value
            // updated as ROTATION_270.
            when (orientation) {
                in 0..39,
                in 320..359 -> Surface.ROTATION_0
                in 50..129 -> Surface.ROTATION_270
                in 140..219 -> Surface.ROTATION_180
                in 230..309 -> Surface.ROTATION_90
                else -> rotation
            }
        }

    /** Callback interface to receive rotation updates. */
    public fun interface Listener {
        /** Called when the physical rotation of the device changes. */
        public fun onRotationChanged(@ImageOutputConfig.RotationValue rotation: Int)
    }

    /** Wrapper of [RotationProvider.Listener] with the executor and a tombstone flag. */
    private class ListenerWrapper(private val listener: Listener, private val executor: Executor) {
        private val enabled = AtomicBoolean(true)

        fun onRotationChanged(@ImageOutputConfig.RotationValue rotation: Int) {
            if (enabled.get()) {
                try {
                    executor.execute {
                        if (enabled.get()) {
                            listener.onRotationChanged(rotation)
                        }
                    }
                } catch (e: RejectedExecutionException) {
                    Logger.w(
                        TAG,
                        "Failed to execute the command. Maybe the executor has been shutdown.",
                    )
                }
            }
        }

        /**
         * Once disabled, the app will not receive callback even if it has already been posted on
         * the callback thread.
         */
        fun disable() {
            enabled.set(false)
        }
    }

    private companion object {
        private const val TAG = "RotationProvider"
    }
}
