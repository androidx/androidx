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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraInterop
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.core.impl.SessionConfig
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

/**
 * A application-level single-instance repository for Camera Interop callbacks. It supplies
 * camera-pipe with internal callbacks on CameraX initialization. During runtime, before a camera
 * graph is created, CameraX updates these internal callbacks with Camera Interop callbacks so that
 * they may be triggered in camera-pipe.
 */
public class CameraInteropStateCallbackRepository {

    private val _deviceStateCallback = CameraDeviceStateCallbacks()
    private val _sessionStateCallback = CaptureSessionStateCallbacks()

    /**
     * Called after merging all sessionConfigs from CameraX useCases and UseCases supplied by Camera
     * Interop. If the Interop has any callbacks, they would be contained in the sessionConfig.
     * CameraInteropStateCallbackRepository would store these callbacks to be triggered by
     * camera-pipe.
     *
     * @param sessionConfig the final merged sessionConfig used to create camera graph
     */
    public fun updateCallbacks(sessionConfig: SessionConfig) {
        _deviceStateCallback.updateCallbacks(sessionConfig)
        _sessionStateCallback.updateCallbacks(sessionConfig)
    }

    public val deviceStateCallback: CameraDeviceStateCallbacks
        get() = _deviceStateCallback

    public val sessionStateCallback: CameraInterop.CaptureSessionListener
        get() = _sessionStateCallback

    public class CameraDeviceStateCallbacks : CameraDevice.StateCallback() {

        private var callbacks: AtomicRef<List<CameraDevice.StateCallback>> = atomic(listOf())

        internal fun updateCallbacks(sessionConfig: SessionConfig) {
            callbacks.value = sessionConfig.deviceStateCallbacks.toList()
        }

        override fun onOpened(cameraDevice: CameraDevice) {
            for (callback in callbacks.value) {
                callback.onOpened(cameraDevice)
            }
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            for (callback in callbacks.value) {
                callback.onClosed(cameraDevice)
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            for (callback in callbacks.value) {
                callback.onDisconnected(cameraDevice)
            }
        }

        override fun onError(cameraDevice: CameraDevice, errorCode: Int) {
            for (callback in callbacks.value) {
                callback.onError(cameraDevice, errorCode)
            }
        }
    }

    public class CaptureSessionStateCallbacks : CameraInterop.CaptureSessionListener {
        private val placeholderSession = RejectOperationCameraCaptureSession()

        private var callbacks: AtomicRef<List<CameraCaptureSession.StateCallback>> =
            atomic(listOf())

        internal fun updateCallbacks(sessionConfig: SessionConfig) {
            callbacks.value = sessionConfig.sessionStateCallbacks.toList()
        }

        override fun onConfigured(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            for (callback in callbacks.value) {
                callback.onConfigured(placeholderSession)
            }
        }

        override fun onConfigureFailed(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            for (callback in callbacks.value) {
                callback.onConfigureFailed(placeholderSession)
            }
        }

        override fun onReady(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            for (callback in callbacks.value) {
                callback.onReady(placeholderSession)
            }
        }

        override fun onActive(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            for (callback in callbacks.value) {
                callback.onActive(placeholderSession)
            }
        }

        override fun onCaptureQueueEmpty(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Api26CompatImpl.onCaptureQueueEmpty(placeholderSession, callbacks)
            } else {
                Log.error { "onCaptureQueueEmpty called for unsupported OS version." }
            }
        }

        override fun onClosed(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
        ) {
            for (callback in callbacks.value) {
                callback.onClosed(placeholderSession)
            }
        }

        override fun onSurfacePrepared(
            cameraId: CameraId,
            captureSessionId: CameraInterop.CameraCaptureSessionId,
            surface: Surface,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Api23CompatImpl.onSurfacePrepared(placeholderSession, surface, callbacks)
            } else {
                Log.error { "onSurfacePrepared called for unsupported OS version." }
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private object Api23CompatImpl {
            @JvmStatic
            fun onSurfacePrepared(
                session: CameraCaptureSession,
                surface: Surface,
                callbacks: AtomicRef<List<CameraCaptureSession.StateCallback>>,
            ) {
                for (callback in callbacks.value) {
                    callback.onSurfacePrepared(session, surface)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private object Api26CompatImpl {
            @JvmStatic
            fun onCaptureQueueEmpty(
                session: CameraCaptureSession,
                callbacks: AtomicRef<List<CameraCaptureSession.StateCallback>>,
            ) {
                for (callback in callbacks.value) {
                    callback.onCaptureQueueEmpty(session)
                }
            }
        }
    }
}
