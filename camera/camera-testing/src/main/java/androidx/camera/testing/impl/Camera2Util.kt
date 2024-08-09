/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.DoNotInline
import kotlinx.coroutines.CompletableDeferred

/** Convenient suspend functions for invoking camera2 APIs. */
public object Camera2Util {
    /** Open the camera device and return the [CameraDevice] instance. */
    @DoNotInline
    public suspend fun openCameraDevice(
        cameraManager: CameraManager,
        cameraId: String,
        handler: Handler
    ): CameraDevice {
        val deferred = CompletableDeferred<CameraDevice>()
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    deferred.complete(cameraDevice)
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    deferred.completeExceptionally(RuntimeException("Camera Disconnected"))
                }

                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    deferred.completeExceptionally(
                        RuntimeException("Camera onError(error=$cameraDevice)")
                    )
                }
            },
            handler
        )
        return deferred.await()
    }

    /** Creates and returns a configured [CameraCaptureSession]. */
    public suspend fun openCaptureSession(
        cameraDevice: CameraDevice,
        surfaceList: List<Surface>,
        handler: Handler
    ): CameraCaptureSession {
        val deferred = CompletableDeferred<CameraCaptureSession>()
        @Suppress("deprecation")
        cameraDevice.createCaptureSession(
            surfaceList,
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    deferred.complete(session)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    deferred.completeExceptionally(RuntimeException("onConfigureFailed"))
                }
            },
            handler
        )
        return deferred.await()
    }

    /**
     * Submits a single capture request to the [CameraCaptureSession] and returns the
     * [TotalCaptureResult].
     */
    public suspend fun submitSingleRequest(
        cameraDevice: CameraDevice,
        session: CameraCaptureSession,
        surfaces: List<Surface>,
        handler: Handler
    ): TotalCaptureResult {
        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        for (surface in surfaces) {
            builder.addTarget(surface)
        }
        val deferredCapture = CompletableDeferred<TotalCaptureResult>()
        session.capture(
            builder.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    deferredCapture.complete(result)
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    deferredCapture.completeExceptionally(RuntimeException("capture failed"))
                }
            },
            handler
        )
        return deferredCapture.await()
    }

    /**
     * Starts the repeating request, and invokes the given block when [TotalCaptureResult] arrives.
     */
    public fun startRepeating(
        cameraDevice: CameraDevice,
        session: CameraCaptureSession,
        surfaces: List<Surface>,
        blockForCaptureResult: (TotalCaptureResult) -> Unit
    ) {
        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        for (surface in surfaces) {
            builder.addTarget(surface)
        }
        val deferredCapture = CompletableDeferred<TotalCaptureResult>()
        session.setRepeatingRequest(
            builder.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    blockForCaptureResult.invoke(result)
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    deferredCapture.completeExceptionally(RuntimeException("capture failed"))
                }
            },
            Handler(Looper.getMainLooper())
        )
    }
}
