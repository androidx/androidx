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

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.CameraXImageAvailableListener
import androidx.camera.integration.antelope.CustomLifecycle
import androidx.camera.integration.antelope.FocusMode
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
@kotlin.OptIn(DelicateCoroutinesApi::class)
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
            (testConfig.currentRunningTest == TestType.MULTI_SWITCH)
        ) {
            testConfig.switchTestRealCameraId = params.id // Save the actual camera ID
            params.id = testConfig.switchTestCurrentCamera
        }

        params.cameraXDeviceStateCallback = CameraXDeviceStateCallback(params, activity, testConfig)
        params.cameraXPreviewSessionStateCallback =
            CameraXPreviewSessionStateCallback(activity, params, testConfig)

        if (params.cameraXDeviceStateCallback != null &&
            params.cameraXPreviewSessionStateCallback != null
        ) {
            params.cameraXPreviewBuilder =
                cameraXPreviewUseCaseBuilder(
                    testConfig.focusMode,
                    params.cameraXDeviceStateCallback!!,
                    params.cameraXPreviewSessionStateCallback!!
                )
        }

        if (!params.cameraXLifecycle.isFinished()) {
            logd("Lifecycle not finished, finishing it.")
            params.cameraXLifecycle.pauseAndStop()
            params.cameraXLifecycle.finish()
        }
        params.cameraXLifecycle = CustomLifecycle()

        val lifecycleOwner: LifecycleOwner = params.cameraXLifecycle
        val previewUseCase = params.cameraXPreviewBuilder.build()

        // Set preview to observe the surface texture
        activity.runOnUiThread {
            previewUseCase.setSurfaceProvider { surfaceRequest ->
                // Create the SurfaceTexture and Surface
                val surfaceTexture = SurfaceTexture(0)
                surfaceTexture.setDefaultBufferSize(
                    surfaceRequest.resolution.width,
                    surfaceRequest.resolution.height
                )
                surfaceTexture.detachFromGLContext()
                val surface = Surface(surfaceTexture)

                // Attach the SurfaceTexture on the TextureView
                if (!isCameraSurfaceTextureReleased(surfaceTexture)) {
                    val viewGroup = params.cameraXPreviewTexture?.parent as ViewGroup
                    viewGroup.removeView(params.cameraXPreviewTexture)
                    viewGroup.addView(params.cameraXPreviewTexture)
                    params.cameraXPreviewTexture?.setSurfaceTexture(surfaceTexture)
                }

                // Surface provided to camera for producing buffers into and
                // Release the SurfaceTexture and Surface once camera is done with it
                surfaceRequest.provideSurface(
                    surface, CameraXExecutors.directExecutor(),
                    Consumer {
                        surface.release()
                        surfaceTexture.release()
                    }
                )
            }
        }

        // TODO: As of 0.3.0 CameraX can only use front and back cameras.
        //  Update in future versions
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        val cameraXcameraID = if (params.id == "0") {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraXcameraID).build()
        when (testConfig.currentRunningTest) {
            //  Only the preview is required
            TestType.PREVIEW,
            TestType.SWITCH_CAMERA,
            TestType.MULTI_SWITCH -> {
                params.timer.openStart = System.currentTimeMillis()
                GlobalScope.launch(Dispatchers.Main) {
                    val cameraProvider = cameraProviderFuture.await()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector,
                        previewUseCase
                    )
                    params.cameraXLifecycle.start()
                }
            }
            else -> {
                // Both preview and image capture are needed
                params.cameraXCaptureSessionCallback =
                    CameraXCaptureSessionCallback(activity, params, testConfig)

                if (params.cameraXDeviceStateCallback != null &&
                    params.cameraXCaptureSessionCallback != null
                ) {
                    params.cameraXCaptureBuilder =
                        cameraXImageCaptureUseCaseBuilder(
                            testConfig.focusMode,
                            params.cameraXDeviceStateCallback!!,
                            params.cameraXCaptureSessionCallback!!
                        )
                }

                params.cameraXImageCaptureUseCase = params.cameraXCaptureBuilder.build()

                params.timer.openStart = System.currentTimeMillis()

                GlobalScope.launch(Dispatchers.Main) {
                    val cameraProvider = cameraProviderFuture.await()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector,
                        previewUseCase, params.cameraXImageCaptureUseCase
                    )
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
@kotlin.OptIn(DelicateCoroutinesApi::class)
internal fun closeCameraX(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    logd("In closecameraX, camera: " + params.id + ",  test: " + testConfig.currentRunningTest)

    params.timer.cameraCloseStart = System.currentTimeMillis()

    if (!params.cameraXLifecycle.isFinished()) {
        params.cameraXLifecycle.pauseAndStop()
        params.cameraXLifecycle.finish()

        // CameraX calls need to be on the main thread
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        GlobalScope.launch(Dispatchers.Main) {
            val cameraProvider = cameraProviderFuture.await()
            cameraProvider.unbindAll()
        }
    }
    if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
        (testConfig.currentRunningTest == TestType.MULTI_SWITCH)
    ) {
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
    logd(
        "CameraX TakePicture. Pausing for " +
            PrefHelper.getPreviewBuffer(activity) + "ms to let preview run."
    )

    params.timer.previewFillStart = System.currentTimeMillis()
    Thread.sleep(PrefHelper.getPreviewBuffer(activity))
    params.timer.previewFillEnd = System.currentTimeMillis()

    params.timer.captureStart = System.currentTimeMillis()
    params.timer.autofocusStart = System.currentTimeMillis()
    params.timer.autofocusEnd = System.currentTimeMillis()

    logd("Capture timer started: " + params.timer.captureStart)
    activity.runOnUiThread {
        params.cameraXImageCaptureUseCase
            .takePicture(
                CameraXExecutors.mainThreadExecutor(),
                CameraXImageAvailableListener(activity, params, testConfig)
            )
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
@OptIn(ExperimentalCamera2Interop::class)
private fun cameraXPreviewUseCaseBuilder(
    focusMode: FocusMode,
    deviceStateCallback: CameraDevice.StateCallback,
    sessionCaptureStateCallback: CameraCaptureSession.StateCallback
): Preview.Builder {

    val configBuilder = Preview.Builder()
    Camera2Interop.Extender(configBuilder)
        .setDeviceStateCallback(deviceStateCallback)
        .setSessionStateCallback(sessionCaptureStateCallback)
    // TODO(b/142915154): Enables focusMode when CameraX support direct AF mode setting.

    // Prints a log to suppress "fix Parameter 'focusMode' is never used" build error"
    Log.d("Antelope", "focusMode($focusMode) Not enabled.")
    return configBuilder
}

/**
 * Setup the Camera X image capture use case
 */
@OptIn(ExperimentalCamera2Interop::class)
private fun cameraXImageCaptureUseCaseBuilder(
    focusMode: FocusMode,
    deviceStateCallback:
        CameraDevice.StateCallback,
    sessionCaptureCallback: CameraCaptureSession.CaptureCallback
): ImageCapture.Builder {

    val configBuilder =
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    Camera2Interop.Extender(configBuilder)
        .setDeviceStateCallback(deviceStateCallback)
        .setSessionCaptureCallback(sessionCaptureCallback)
    // TODO(b/142915154): Enables focusMode when CameraX support direct AF mode setting.

    // Prints a log to suppress "fix Parameter 'focusMode' is never used" build error"
    Log.d("Antelope", "focusMode($focusMode) Not enabled.")
    return configBuilder
}