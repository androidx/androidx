/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope.cameracontrollers

import androidx.lifecycle.LifecycleOwner
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.view.ViewGroup
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.camera.integration.antelope.CameraXImageAvailableListener
import androidx.camera.integration.antelope.CustomLifecycle
import androidx.camera.integration.antelope.FocusMode
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType

/**
 * Opens the camera using the Camera X API and starts the open counter. The open call will complete
 * in the DeviceStateCallback asynchronously. For switch tests, the camera id will be swizzling so
 * the original camera id is saved.
 *
 * CameraX manages its lifecycle internally, for the purpose of repeated testing, Antelope uses a
 * custom lifecycle to allow for starting new tests cleanly which is started here.
 *
 * All the needed Cmaera X use cases should be bound before starting the lifecycle. Depending on
 * the test, bind either the preview case, or both the preview and image capture case.
 */
internal fun cameraXOpenCamera(
    activity: MainActivity,
    params: CameraParams,
    testConfig: TestConfig
) {

    try {
        // TODO make the switch test methodology more robust and handle physical cameras
        // Currently we swap out the ids behind the scenes
        // This requires to save the actual camera id for after the test
        if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
            (testConfig.currentRunningTest == TestType.MULTI_SWITCH)) {
            testConfig.switchTestRealCameraId = params.id // Save the actual camera ID
            params.id = testConfig.switchTestCurrentCamera
        }

        params.cameraXDeviceStateCallback = CameraXDeviceStateCallback(params, activity, testConfig)
        params.cameraXPreviewSessionStateCallback =
            CameraXPreviewSessionStateCallback(activity, params, testConfig)

        if (params.cameraXDeviceStateCallback != null &&
            params.cameraXPreviewSessionStateCallback != null) {
            params.cameraXPreviewConfig =
                cameraXPreviewUseCaseBuilder(params.id, testConfig.focusMode,
                    params.cameraXDeviceStateCallback!!,
                    params.cameraXPreviewSessionStateCallback!!)
        }

        if (!params.cameraXLifecycle.isFinished()) {
            logd("Lifecycle not finished, finishing it.")
            params.cameraXLifecycle.pauseAndStop()
            params.cameraXLifecycle.finish()
        }
        params.cameraXLifecycle = CustomLifecycle()

        val lifecycleOwner: LifecycleOwner = params.cameraXLifecycle
        val previewUseCase = Preview(params.cameraXPreviewConfig)

        // Set preview to observe the surface texture
        activity.runOnUiThread {
            previewUseCase.setOnPreviewOutputUpdateListener {
                viewFinderOutput: Preview.PreviewOutput? ->
                if (viewFinderOutput?.surfaceTexture != null) {
                    if (!isCameraSurfaceTextureReleased(viewFinderOutput.surfaceTexture)) {
                        // View swizzling required to for the view hierarchy to update correctly
                        val viewGroup = params.cameraXPreviewTexture?.parent as ViewGroup
                        viewGroup.removeView(params.cameraXPreviewTexture)
                        viewGroup.addView(params.cameraXPreviewTexture)
                        params.cameraXPreviewTexture?.surfaceTexture =
                            viewFinderOutput.surfaceTexture
                    }
                }
            }
        }

        when (testConfig.currentRunningTest) {
            //  Only the preview is required
            TestType.PREVIEW,
            TestType.SWITCH_CAMERA,
            TestType.MULTI_SWITCH -> {
                params.timer.openStart = System.currentTimeMillis()
                activity.runOnUiThread {
                    CameraX.bindToLifecycle(lifecycleOwner, previewUseCase)
                    params.cameraXLifecycle.start()
                }
            }
            else -> {
                // Both preview and image capture are needed
                params.cameraXCaptureSessionCallback =
                    CameraXCaptureSessionCallback(activity, params, testConfig)

                if (params.cameraXDeviceStateCallback != null &&
                    params.cameraXCaptureSessionCallback != null) {
                    params.cameraXCaptureConfig =
                        cameraXImageCaptureUseCaseBuilder(params.id, testConfig.focusMode,
                            params.cameraXDeviceStateCallback!!,
                            params.cameraXCaptureSessionCallback!!)
                }

                params.cameraXImageCaptureUseCase = ImageCapture(params.cameraXCaptureConfig)

                params.timer.openStart = System.currentTimeMillis()
                activity.runOnUiThread {
                    CameraX.bindToLifecycle(lifecycleOwner, previewUseCase,
                        params.cameraXImageCaptureUseCase)
                    params.cameraXLifecycle.start()
                }
            }
        }
    } catch (e: Exception) {
        MainActivity.logd("cameraXOpenCamera exception: " + params.id)
        e.printStackTrace()
    }
}

/**
 * End Camera X custom lifecycle, unbind use cases, and start timing the camera close.
 */
internal fun closeCameraX(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    logd("In closecameraX, camera: " + params.id + ",  test: " + testConfig.currentRunningTest)

    params.timer.cameraCloseStart = System.currentTimeMillis()

    if (!params.cameraXLifecycle.isFinished()) {
        params.cameraXLifecycle.pauseAndStop()
        params.cameraXLifecycle.finish()

        // CameraX calls need to be on the main thread
        activity.run {
            CameraX.unbindAll()
        }
    }
    if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
        (testConfig.currentRunningTest == TestType.MULTI_SWITCH)) {
        params.id = testConfig.switchTestRealCameraId // Restore the actual camera ID
    }

    params.isOpen = false
}

/**
 * Proceed to take and measure a still image capture.
 */
internal fun cameraXTakePicture(
    activity: MainActivity,
    params: CameraParams,
    testConfig: TestConfig
) {
    if (params.cameraXLifecycle.isFinished()) {
        cameraXAbort(activity, params, testConfig)
        return
    }

    logd("CameraX TakePicture: capture start.")

    // Pause in multi-captures to make sure HDR routines don't get overloaded
    logd("CameraX TakePicture. Pausing for " +
        PrefHelper.getPreviewBuffer(activity) + "ms to let preview run.")
    params.timer.previewFillStart = System.currentTimeMillis()
    Thread.sleep(PrefHelper.getPreviewBuffer(activity))
    params.timer.previewFillEnd = System.currentTimeMillis()

    params.timer.captureStart = System.currentTimeMillis()
    params.timer.autofocusStart = System.currentTimeMillis()
    params.timer.autofocusEnd = System.currentTimeMillis()

    logd("Capture timer started: " + params.timer.captureStart)
    activity.runOnUiThread {
        params.cameraXImageCaptureUseCase
            .takePicture(CameraXImageAvailableListener(activity, params, testConfig))
    }
}

/**
 * An abort request has been received. Abandon everything
 */
internal fun cameraXAbort(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    closeCameraX(activity, params, testConfig)
    return
}

/**
 * Try to determine if a SurfaceTexture is released.
 *
 * Prior to SDK 26 there was not built in mechanism for this. This method relies on expected
 * exceptions being thrown if a released SurfaceTexture is updated.
 */
private fun isCameraSurfaceTextureReleased(texture: SurfaceTexture): Boolean {
    var released = false

    if (26 <= android.os.Build.VERSION.SDK_INT) {
        released = texture.isReleased
    } else {
        // WARNING: This relies on some implementation details of the SurfaceTexture native code.
        // If the SurfaceTexture is released, we should get a RuntimeException. If not, we should
        // get an IllegalStateException since we are not in the same EGL context as the camera.
        var exception: Exception? = null
        try {
            texture.updateTexImage()
        } catch (e: IllegalStateException) {
            logd("in isCameraSurfaceTextureReleased: IllegalStateException: " + e.message)
            exception = e
            released = false
        } catch (e: RuntimeException) {
            logd("in isCameraSurfaceTextureReleased: RuntimeException: " + e.message)
            exception = e
            released = true
        }

        if (!released && exception == null) {
            throw RuntimeException("Unable to determine if SurfaceTexture is released")
        }
    }

    logd("The camera texture is: " + if (released) "RELEASED" else "NOT RELEASED")

    return released
}

/**
 * Setup the Camera X preview use case
 */
private fun cameraXPreviewUseCaseBuilder(
    id: String,
    focusMode: FocusMode,
    deviceStateCallback: CameraDevice.StateCallback,
    sessionCaptureStateCallback: CameraCaptureSession.StateCallback
): PreviewConfig {

    // TODO: As of 0.3.0 CameraX can only use front and back cameras. Update in future versions
    val cameraXcameraID = if (id.equals("0")) CameraX.LensFacing.BACK else CameraX.LensFacing.FRONT
    val configBuilder = PreviewConfig.Builder()
        .setLensFacing(cameraXcameraID)
    Camera2Config.Extender(configBuilder)
        .setDeviceStateCallback(deviceStateCallback)
        .setSessionStateCallback(sessionCaptureStateCallback)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
            when (focusMode) {
                FocusMode.AUTO -> CaptureRequest.CONTROL_AF_MODE_AUTO
                FocusMode.CONTINUOUS -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                FocusMode.FIXED -> CaptureRequest.CONTROL_AF_MODE_AUTO
            })
    return configBuilder.build()
}

/**
 * Setup the Camera X image capture use case
 */
private fun cameraXImageCaptureUseCaseBuilder(
    id: String,
    focusMode: FocusMode,
    deviceStateCallback:
    CameraDevice.StateCallback,
    sessionCaptureCallback: CameraCaptureSession.CaptureCallback
): ImageCaptureConfig {

    // TODO: As of 0.3.0 CameraX can only use front and back cameras. Update in future versions
    val cameraXcameraID = if (id.equals("0")) CameraX.LensFacing.BACK else CameraX.LensFacing.FRONT

    val configBuilder = ImageCaptureConfig.Builder()
        .setLensFacing(cameraXcameraID)
        .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
    Camera2Config.Extender(configBuilder)
        .setDeviceStateCallback(deviceStateCallback)
        .setSessionCaptureCallback(sessionCaptureCallback)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
            when (focusMode) {
                FocusMode.AUTO -> CaptureRequest.CONTROL_AF_MODE_AUTO
                FocusMode.CONTINUOUS -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                FocusMode.FIXED -> CaptureRequest.CONTROL_AF_MODE_AUTO
            })

    return configBuilder.build()
}