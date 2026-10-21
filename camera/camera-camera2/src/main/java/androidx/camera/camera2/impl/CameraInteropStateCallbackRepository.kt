/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.CameraInterop.CameraCaptureSessionId
import androidx.camera.common.CameraId
import androidx.camera.core.impl.SessionConfig

/**
 * Repository for Camera2Interop state callbacks scoped to a `CameraFactoryAdapter` instance. It
 * supplies camera-pipe with internal callbacks on initialization and routes device and capture
 * session state events to the callbacks registered by each camera's active [SessionConfig].
 *
 * Callbacks are tracked across two lifetimes:
 * - Bound callbacks per [CameraId], active from [updateCallbacks] until [clearWhenClosed].
 * - Active hardware callbacks per opened [CameraDevice] (`onOpened` until `onClosed`) and per
 *   configured capture session (`onConfigured` until closed or replaced), ensuring terminal
 *   `onClosed` events are delivered during async teardown without retaining callbacks afterward.
 */
public class CameraInteropStateCallbackRepository {

    private class BoundCallbacks(
        val deviceCallbacks: List<CameraDevice.StateCallback>,
        val sessionCallbacks: List<CameraCaptureSession.StateCallback>,
    )

    private class OpenDevice(
        val cameraId: CameraId?,
        var callbacks: List<CameraDevice.StateCallback>,
        var previousCallbacks: List<CameraDevice.StateCallback>? = null,
        var isClosing: Boolean = false,
        var isSuperseded: Boolean = false,
    )

    private class OpenSession(
        val cameraId: CameraId?,
        val callbacks: List<CameraCaptureSession.StateCallback>,
        var isSuperseded: Boolean = false,
    )

    private val lock = Any()
    @GuardedBy("lock") private val boundCallbacks = mutableMapOf<CameraId?, BoundCallbacks>()
    @GuardedBy("lock") private val openDevices = mutableMapOf<CameraDevice, OpenDevice>()
    @GuardedBy("lock")
    private val openSessions = mutableMapOf<CameraCaptureSessionId, OpenSession>()

    public val deviceStateCallback: CameraDeviceStateCallbacks = CameraDeviceStateCallbacks()

    public val sessionStateCallback: CameraInterop.CaptureSessionListener =
        CaptureSessionStateCallbacks()

    /**
     * Updates the callbacks for [cameraId] from the merged [sessionConfig] used to create a camera
     * graph.
     *
     * @param sessionConfig the final merged [SessionConfig] used to create the camera graph
     * @param cameraId the [CameraId] of the camera graph being configured, or `null` if unspecified
     * @param reuseOpenDevices `true` when camera-pipe reuses an already-open [CameraDevice] across
     *   a UseCase rebind without invoking [CameraDevice.StateCallback.onOpened] again
     */
    public fun updateCallbacks(
        sessionConfig: SessionConfig,
        cameraId: CameraId? = null,
        reuseOpenDevices: Boolean = true,
    ) {
        val callbacks =
            BoundCallbacks(
                sessionConfig.deviceStateCallbacks.toList(),
                sessionConfig.sessionStateCallbacks.toList(),
            )
        synchronized(lock) {
            boundCallbacks[cameraId] = callbacks
            for (openDevice in openDevices.values) {
                if (
                    openDevice.cameraId == cameraId ||
                        cameraId == null ||
                        openDevice.cameraId == null
                ) {
                    if (reuseOpenDevices && !openDevice.isClosing) {
                        if (openDevice.previousCallbacks == null) {
                            openDevice.previousCallbacks = openDevice.callbacks
                        }
                        openDevice.callbacks = callbacks.deviceCallbacks
                    } else {
                        openDevice.previousCallbacks?.let { openDevice.callbacks = it }
                        openDevice.previousCallbacks = null
                        openDevice.isClosing = true
                    }
                }
            }
        }
    }

    /**
     * Unbinds the callbacks for [cameraId] when no UseCases remain attached. Any already-opened
     * [CameraDevice] or configured capture session retains its callbacks until its terminal
     * `onClosed` callback is delivered.
     */
    public fun clearWhenClosed(cameraId: CameraId? = null) {
        synchronized(lock) {
            if (cameraId != null) {
                boundCallbacks.remove(cameraId)
            } else {
                boundCallbacks.clear()
            }
        }
    }

    /** Immediately clears all registered and active callbacks. */
    public fun clearCallbacks() {
        synchronized(lock) {
            boundCallbacks.clear()
            openDevices.clear()
            openSessions.clear()
        }
    }

    @GuardedBy("lock")
    private fun boundKeyFor(cameraId: CameraId?): CameraId? =
        when {
            cameraId != null && boundCallbacks.containsKey(cameraId) -> cameraId
            boundCallbacks.containsKey(null) -> null
            cameraId == null && boundCallbacks.size == 1 -> boundCallbacks.keys.single()
            else -> cameraId
        }

    public inner class CameraDeviceStateCallbacks : CameraDevice.StateCallback() {

        private fun CameraDevice.toCameraIdOrNull(): CameraId? = runCatching {
            id
        }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { CameraId.fromCamera2Id(it) }

        @GuardedBy("lock")
        private fun supersedeOpenDevicesFor(key: CameraId?) {
            openDevices.entries.removeAll { (_, state) ->
                state.cameraId == key && state.isSuperseded
            }
            for (state in openDevices.values) {
                if (state.cameraId == key) {
                    state.previousCallbacks?.let { state.callbacks = it }
                    state.previousCallbacks = null
                    state.isClosing = true
                    state.isSuperseded = true
                }
            }
        }

        override fun onOpened(cameraDevice: CameraDevice) {
            val currentCallbacks =
                synchronized(lock) {
                    val key = boundKeyFor(cameraDevice.toCameraIdOrNull())
                    val callbacks = boundCallbacks[key]?.deviceCallbacks
                    if (callbacks == null) {
                        if (openDevices.values.none { it.cameraId == key }) {
                            openDevices[cameraDevice] = OpenDevice(key, emptyList())
                        }
                        return
                    }
                    openDevices.remove(cameraDevice)
                    supersedeOpenDevicesFor(key)
                    openDevices[cameraDevice] = OpenDevice(key, callbacks)
                    callbacks
                }
            for (callback in currentCallbacks) {
                callback.onOpened(cameraDevice)
            }
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            val currentCallbacks =
                synchronized(lock) {
                    val openDevice = openDevices.remove(cameraDevice) ?: return
                    val key = openDevice.cameraId
                    if (!openDevice.isSuperseded) {
                        openDevices.values.removeAll { it.cameraId == key && it.isSuperseded }
                    }
                    if (openDevices.values.none { it.cameraId == key }) {
                        // A capture session cannot outlive its CameraDevice, even if the HAL omits
                        // CameraCaptureSession.StateCallback.onClosed (b/146773463).
                        openSessions.values.removeAll { it.cameraId == key }
                    } else if (openDevice.isSuperseded) {
                        openSessions.values.removeAll { it.cameraId == key && it.isSuperseded }
                    }
                    openDevice.previousCallbacks ?: openDevice.callbacks
                }
            for (callback in currentCallbacks) {
                callback.onClosed(cameraDevice)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            for (callback in callbacksFor(cameraDevice)) {
                callback.onDisconnected(cameraDevice)
            }
        }

        override fun onError(cameraDevice: CameraDevice, errorCode: Int) {
            for (callback in callbacksFor(cameraDevice)) {
                callback.onError(cameraDevice, errorCode)
            }
        }

        private fun callbacksFor(cameraDevice: CameraDevice): List<CameraDevice.StateCallback> =
            synchronized(lock) {
                openDevices[cameraDevice]?.let { openDevice ->
                    openDevice.isClosing = true
                    openDevice.previousCallbacks = null
                    openDevice.callbacks
                }
                    ?: run {
                        val key = boundKeyFor(cameraDevice.toCameraIdOrNull())
                        val callbacks = boundCallbacks[key]?.deviceCallbacks ?: return emptyList()
                        supersedeOpenDevicesFor(key)
                        openDevices[cameraDevice] = OpenDevice(key, callbacks, isClosing = true)
                        callbacks
                    }
            }
    }

    public inner class CaptureSessionStateCallbacks : CameraInterop.CaptureSessionListener {
        private val placeholderSession = RejectOperationCameraCaptureSession()

        override fun onConfigured(cameraId: CameraId, captureSessionId: CameraCaptureSessionId) {
            val currentCallbacks =
                synchronized(lock) {
                    val key = boundKeyFor(cameraId)
                    val bound = boundCallbacks[key] ?: return
                    // Evict any session that was already superseded by an earlier session
                    // (bounding retention if the HAL omits onClosed per b/249258992), while
                    // keeping the immediately preceding session so a trailing onClosed arriving
                    // after the new session's onConfigured is still delivered.
                    openSessions.values.removeAll { it.cameraId == key && it.isSuperseded }
                    for (session in openSessions.values) {
                        if (session.cameraId == key) {
                            session.isSuperseded = true
                        }
                    }
                    for (openDevice in openDevices.values) {
                        if (openDevice.cameraId == key && !openDevice.isClosing) {
                            openDevice.previousCallbacks = null
                        }
                    }
                    openSessions[captureSessionId] = OpenSession(key, bound.sessionCallbacks)
                    bound.sessionCallbacks
                }
            for (callback in currentCallbacks) {
                callback.onConfigured(placeholderSession)
            }
        }

        override fun onConfigureFailed(
            cameraId: CameraId,
            captureSessionId: CameraCaptureSessionId,
        ) {
            for (callback in callbacksFor(cameraId, captureSessionId, allowBoundFallback = true)) {
                callback.onConfigureFailed(placeholderSession)
            }
        }

        override fun onReady(cameraId: CameraId, captureSessionId: CameraCaptureSessionId) {
            for (callback in callbacksFor(cameraId, captureSessionId)) {
                callback.onReady(placeholderSession)
            }
        }

        override fun onActive(cameraId: CameraId, captureSessionId: CameraCaptureSessionId) {
            for (callback in callbacksFor(cameraId, captureSessionId)) {
                callback.onActive(placeholderSession)
            }
        }

        override fun onCaptureQueueEmpty(
            cameraId: CameraId,
            captureSessionId: CameraCaptureSessionId,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val currentCallbacks = callbacksFor(cameraId, captureSessionId)
                Api26CompatImpl.onCaptureQueueEmpty(placeholderSession, currentCallbacks)
            } else {
                Camera2Logger.error { "onCaptureQueueEmpty called for unsupported OS version." }
            }
        }

        override fun onClosed(cameraId: CameraId, captureSessionId: CameraCaptureSessionId) {
            val currentCallbacks =
                synchronized(lock) {
                    val openSession = openSessions.remove(captureSessionId) ?: return
                    if (!openSession.isSuperseded) {
                        openSessions.values.removeAll {
                            it.cameraId == openSession.cameraId && it.isSuperseded
                        }
                    }
                    openSession.callbacks
                }
            for (callback in currentCallbacks) {
                callback.onClosed(placeholderSession)
            }
        }

        override fun onSurfacePrepared(
            cameraId: CameraId,
            captureSessionId: CameraCaptureSessionId,
            surface: Surface,
        ) {
            for (callback in callbacksFor(cameraId, captureSessionId, allowBoundFallback = true)) {
                callback.onSurfacePrepared(placeholderSession, surface)
            }
        }

        private fun callbacksFor(
            cameraId: CameraId,
            captureSessionId: CameraCaptureSessionId,
            allowBoundFallback: Boolean = false,
        ): List<CameraCaptureSession.StateCallback> =
            synchronized(lock) {
                openSessions[captureSessionId]?.callbacks
                    ?: if (allowBoundFallback) {
                        boundCallbacks[boundKeyFor(cameraId)]?.sessionCallbacks.orEmpty()
                    } else {
                        emptyList()
                    }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26CompatImpl {
        @JvmStatic
        fun onCaptureQueueEmpty(
            session: CameraCaptureSession,
            callbacks: List<CameraCaptureSession.StateCallback>,
        ) {
            for (callback in callbacks) {
                callback.onCaptureQueueEmpty(session)
            }
        }
    }
}
