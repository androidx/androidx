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
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threading
import androidx.camera.camera2.pipe.core.Threads
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic

@JvmDefaultWithCompatibility
internal interface Camera2DeviceCloser {
    fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper? = null,
        cameraDevice: CameraDevice? = null,
        androidCameraState: AndroidCameraState,
        audioRestrictionController: AudioRestrictionController,
        shouldReopenCamera: Boolean = false,
        shouldCreateEmptyCaptureSession: Boolean = false,
    )
}

@Singleton
internal class Camera2DeviceCloserImpl
@Inject
constructor(
    val threads: Threads,
    private val camera2Quirks: Camera2Quirks,
    private val retryingCameraStateOpener: RetryingCameraStateOpener,
) : Camera2DeviceCloser {

    override fun closeCamera(
        cameraDeviceWrapper: CameraDeviceWrapper?,
        cameraDevice: CameraDevice?,
        androidCameraState: AndroidCameraState,
        audioRestrictionController: AudioRestrictionController,
        shouldReopenCamera: Boolean,
        shouldCreateEmptyCaptureSession: Boolean,
    ) {
        val unwrappedCameraDevice = cameraDeviceWrapper?.unwrapAs(CameraDevice::class)
        if (unwrappedCameraDevice != null) {
            val cameraId = CameraId.fromCamera2Id(unwrappedCameraDevice.id)
            cameraDevice?.let {
                check(cameraId.value == it.id) {
                    "Unwrapped camera device has camera ID ${cameraId.value}, " +
                        "but the wrapped camera device has camera ID ${it.id}!"
                }
            }

            val currentCameras =
                handleQuirksBeforeClosing(
                    cameraDeviceWrapper,
                    unwrappedCameraDevice,
                    androidCameraState,
                    shouldReopenCamera,
                    shouldCreateEmptyCaptureSession,
                )
            if (currentCameras == null) {
                Log.error { "Failed to handle quirks before closing the camera device!" }
                cameraDeviceWrapper.onDeviceClosed()
                androidCameraState.onFinalized(unwrappedCameraDevice)
                return
            }

            val (currentCameraDeviceWrapper, currentAndroidCameraState) = currentCameras
            val currentCameraDevice =
                checkNotNull(currentCameraDeviceWrapper.unwrapAs(CameraDevice::class))

            closeCameraDevice(currentCameraDevice, currentAndroidCameraState)
            cameraDeviceWrapper.onDeviceClosed()

            // If the camera was reopened, make sure to finalize the camera state to finish closing.
            if (shouldReopenCamera) {
                androidCameraState.onFinalized(unwrappedCameraDevice)
            }

            /**
             * Only remove the audio restriction when CameraDeviceWrapper is present. When
             * closeCamera is called without a CameraDeviceWrapper, that means a wrapper hadn't been
             * created for the opened camera.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                audioRestrictionController.removeListener(cameraDeviceWrapper)
            }

            // We only need to close the device once (don't want to create another capture session).
            // Return here.
            return
        }
        cameraDevice?.let { closeCameraDevice(it, androidCameraState) }
    }

    /**
     * Handle quirks before closing the camera device.
     *
     * [shouldReopenCamera] and [shouldCreateEmptyCaptureSession] determines what actions to do:
     * - ([shouldReopenCamera],[shouldCreateEmptyCaptureSession]) = (false, false) - No quirk
     *   handling necessary. Returns the existing camera device and state.
     * - ([shouldReopenCamera],[shouldCreateEmptyCaptureSession]) = (false, true) - Creates an empty
     *   capture session. Returns the existing camera device and state.
     * - ([shouldReopenCamera],[shouldCreateEmptyCaptureSession]) = (true, true) - Closes and
     *   reopens the camera device before creating an empty capture session. Returns the new camera
     *   device and state.
     *
     * For more details, please refer to
     * [Camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession] and
     * [Camera2Quirks.shouldCreateEmptyCaptureSessionBeforeClosing].
     */
    private fun handleQuirksBeforeClosing(
        cameraDeviceWrapper: CameraDeviceWrapper,
        cameraDevice: CameraDevice,
        androidCameraState: AndroidCameraState,
        shouldReopenCamera: Boolean,
        shouldCreateEmptyCaptureSession: Boolean,
    ): Pair<CameraDeviceWrapper, AndroidCameraState>? {
        Log.debug { "handleQuirksBeforeClosing($cameraDevice)" }
        val cameraId = cameraDeviceWrapper.cameraId
        val cameras =
            if (shouldReopenCamera) {
                Debug.trace("Camera2DeviceCloserImpl#reopenCameraDevice") {
                    Log.debug { "Reopening camera device" }
                    closeCameraDevice(cameraDevice, androidCameraState)
                    retryingCameraStateOpener.openAndAwaitCameraWithRetry(cameraId, this)
                }
            } else {
                AwaitOpenCameraResult(cameraDeviceWrapper, androidCameraState)
            }
        if (cameras.cameraDeviceWrapper == null || cameras.androidCameraState == null) {
            Log.error { "Failed to retain an opened camera device!" }
            return null
        }

        if (shouldCreateEmptyCaptureSession) {
            Debug.trace("Camera2DeviceCloserImpl#createCaptureSession") {
                Log.debug { "Creating an empty capture session before closing $cameraId" }
                createCaptureSession(cameras.cameraDeviceWrapper)
                Log.debug { "Created an empty capture session." }
            }
        }

        return Pair(cameras.cameraDeviceWrapper, cameras.androidCameraState)
    }

    private fun closeCameraDevice(
        cameraDevice: CameraDevice,
        androidCameraState: AndroidCameraState,
    ) {
        val cameraDeviceId = cameraDevice.id
        Log.debug { "closeCameraDevice($cameraDeviceId)" }
        var cameraDeviceClosed = false
        Threading.runBlockingCheckedOrNull(
            threads.blockingDispatcher,
            threads.backgroundDispatcher,
            CAMERA_CLOSE_TIMEOUT_MS,
        ) {
            cameraDevice.closeWithTrace()
            cameraDeviceClosed = true
        }
            ?: run {
                Log.error {
                    "Failed to close CameraDevice($cameraDeviceId) after " +
                        "${CAMERA_CLOSE_TIMEOUT_MS}ms. The camera is likely in a bad state."
                }
            }

        val cameraId = CameraId.fromCamera2Id(cameraDevice.id)
        // The Android camera framework invokes onClosed() only after CameraDevice.close() is
        // done. That means if CameraDevice.close() timed out, we wouldn't get onClosed(), so
        // waiting for it is unnecessary at this point and should be avoided.
        if (camera2Quirks.shouldWaitForCameraDeviceOnClosed(cameraId) && cameraDeviceClosed) {
            Log.debug { "Waiting for OnClosed from $cameraId" }
            if (androidCameraState.awaitCameraDeviceClosed(timeoutMillis = 2000)) {
                Log.debug { "Received OnClosed for $cameraId" }
            } else {
                Log.warn { "Failed to close $cameraId after 2000ms!" }
            }
        }
    }

    private fun createCaptureSession(cameraDeviceWrapper: CameraDeviceWrapper) {
        val surfaceTexture = SurfaceTexture(0).also { it.setDefaultBufferSize(640, 480) }
        val surface = Surface(surfaceTexture)
        val surfaceReleased = atomic(false)
        val sessionConfigured = CountDownLatch(1)
        val callback =
            object : CameraCaptureSessionWrapper.StateCallback {
                override fun onConfigured(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session configured. Closing it" }
                    // We don't need to wait for the session to close, instead we can just invoke
                    // close() and end here.
                    session.close()
                    sessionConfigured.countDown()
                }

                override fun onClosed(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session closed" }
                    if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                        surface.release()
                        surfaceTexture.release()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSessionWrapper) {
                    Log.debug { "Empty capture session configure failed" }
                    if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                        surface.release()
                        surfaceTexture.release()
                    }
                    sessionConfigured.countDown()
                }

                override fun onReady(session: CameraCaptureSessionWrapper) {}

                override fun onCaptureQueueEmpty(session: CameraCaptureSessionWrapper) {}

                override fun onSessionDisconnected() {}

                override fun onSessionFinalized() {}

                override fun onActive(session: CameraCaptureSessionWrapper) {}
            }
        if (cameraDeviceWrapper.createCaptureSession(listOf(surface), callback)) {
            sessionConfigured.await()
        } else {
            Log.error {
                "Failed to create a blank capture session! " +
                    "Surfaces may not be disconnected properly."
            }
            if (surfaceReleased.compareAndSet(expect = false, update = true)) {
                surface.release()
                surfaceTexture.release()
            }
        }
    }

    companion object {
        const val CAMERA_CLOSE_TIMEOUT_MS = 7_000L // 7s
    }
}
