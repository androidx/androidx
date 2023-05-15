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

package androidx.camera.camera2.pipe.compat

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threading
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic

internal interface Camera2DeviceCloser {
    fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper? = null,
        cameraDevice: CameraDevice? = null,
        closeUnderError: Boolean = false,
        androidCameraState: AndroidCameraState,
    )
}

@Singleton
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2DeviceCloserImpl @Inject constructor(
    val threads: Threads,
    private val camera2Quirks: Camera2Quirks,
) : Camera2DeviceCloser {
    override fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper?,
        cameraDevice: CameraDevice?,
        closeUnderError: Boolean,
        androidCameraState: AndroidCameraState,
    ) {
        Log.debug { "Closing $cameraDeviceWrapper and/or $cameraDevice" }
        val unwrappedCameraDevice = cameraDeviceWrapper?.unwrapAs(CameraDevice::class)
        if (unwrappedCameraDevice != null) {
            cameraDevice?.let {
                check(unwrappedCameraDevice.id == it.id) {
                    "Unwrapped camera device has camera ID ${unwrappedCameraDevice.id}, " + "" +
                        "but the accompanied camera device has camera ID ${it.id}"
                }
            }
            closeCameraDevice(unwrappedCameraDevice, closeUnderError, androidCameraState)
            cameraDeviceWrapper.onDeviceClosed()

            // We only need to close the device once (don't want to create another capture session).
            // Return here.
            return
        }
        cameraDevice?.let { closeCameraDevice(it, closeUnderError, androidCameraState) }
    }

    private fun closeCameraDevice(
        cameraDevice: CameraDevice,
        closeUnderError: Boolean,
        androidCameraState: AndroidCameraState,
    ) {
        val cameraId = CameraId.fromCamera2Id(cameraDevice.id)
        if (camera2Quirks.shouldCreateCaptureSessionBeforeClosing(cameraId) && !closeUnderError) {
            Debug.trace("Camera2DeviceCloserImpl#createCaptureSession") {
                Log.debug { "Creating an empty capture session before closing camera $cameraId" }
                createCaptureSession(cameraDevice)
                Log.debug { "Empty capture session quirk completed" }
            }
        }
        Log.debug { "Closing $cameraDevice" }
        Threading.runBlockingWithTimeout(threads.backgroundDispatcher, 2000L) {
            cameraDevice.closeWithTrace()
        }
        if (camera2Quirks.shouldWaitForCameraDeviceOnClosed(cameraId)) {
            Log.debug { "Waiting for camera device to be completely closed" }
            if (androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 2000)) {
                Log.debug { "Camera device is closed" }
            } else {
                Log.warn { "Failed to wait for camera device to close after 2000ms" }
            }
        }
    }

    private fun createCaptureSession(cameraDevice: CameraDevice) {
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)
        val surfaceReleased = atomic(false)
        val sessionConfigured = CountDownLatch(1)
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.debug { "Empty capture session configured. Closing it" }
                // We don't need to wait for the session to close, instead we can just invoke
                // close() and end here.
                session.close()
                sessionConfigured.countDown()
            }

            override fun onClosed(session: CameraCaptureSession) {
                Log.debug { "Empty capture session closed" }
                if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                    surface.release()
                    surfaceTexture.release()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.debug { "Empty capture session configure failed" }
                if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                    surface.release()
                    surfaceTexture.release()
                }
                sessionConfigured.countDown()
            }
        }
        try {
            // This function was deprecated in Android Q, but is required since this quirk is
            // needed on older API levels.
            @Suppress("deprecation")
            cameraDevice.createCaptureSession(listOf(surface), callback, threads.camera2Handler)
        } catch (throwable: Throwable) {
            Log.error(throwable) {
                "Failed to create a blank capture session. " +
                    "Surfaces may not be disconnected properly"
            }
            if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                surface.release()
                surfaceTexture.release()
            }
        }
        sessionConfigured.await()
    }
}