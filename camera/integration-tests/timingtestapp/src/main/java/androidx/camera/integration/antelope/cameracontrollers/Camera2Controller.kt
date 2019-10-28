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

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.FocusMode
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.integration.antelope.getOrientation
import androidx.camera.integration.antelope.setAutoFlash
import java.lang.Thread.sleep
import java.util.Arrays

/** State of the camera during an image capture - */
internal enum class CameraState {
    UNINITIALIZED,
    PREVIEW_RUNNING,
    WAITING_FOCUS_LOCK,
    WAITING_EXPOSURE_LOCK,
    IMAGE_REQUESTED
}

/**
 * Opens the camera using the Camera 2 API and starts the open counter. The open call will complete
 * in the DeviceStateCallback asynchronously. For switch tests, the camera id will be swizzling so
 * the original camera id is saved.
 */
fun camera2OpenCamera(activity: MainActivity, params: CameraParams?, testConfig: TestConfig) {
    if (null == params)
        return

    val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        // TODO make the switch test methodology more robust and handle physical cameras
        if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
            (testConfig.currentRunningTest == TestType.MULTI_SWITCH)) {
            testConfig.switchTestRealCameraId = params.id // Save the original camera ID
            params.id = testConfig.switchTestCurrentCamera
        }

        // Might be a new test, update callbacks to match the test config
        params.camera2DeviceStateCallback = Camera2DeviceStateCallback(params, activity, testConfig)
        params.camera2CaptureSessionCallback =
            Camera2CaptureSessionCallback(activity, params, testConfig)

        params.timer.openStart = System.currentTimeMillis()
        logd("openCamera: " + params.id + " running test: " +
            testConfig.currentRunningTest.toString())

        manager.openCamera(params.id, params.camera2DeviceStateCallback!!, params.backgroundHandler)
    } catch (e: CameraAccessException) {
        logd("openCamera CameraAccessException: " + params.id)
        e.printStackTrace()
    } catch (e: SecurityException) {
        logd("openCamera SecurityException: " + params.id)
        e.printStackTrace()
    }
}

/**
 * Setup the camera preview session and output surface.
 */
fun createCameraPreviewSession(
    activity: MainActivity,
    params: CameraParams,
    testConfig: TestConfig
) {

    logd("In createCameraPreviewSession.")
    if (!params.isOpen) {
        return
    }

    try {
        val surface = params.previewSurfaceView?.holder?.surface
        if (null == surface)
            return

        val imageSurface = params.imageReader?.surface
        if (null == imageSurface)
            return

        params.captureRequestBuilder =
            params.device?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        if (params.previewSurfaceView?.holder?.surface != null)
            params.captureRequestBuilder?.removeTarget(params.previewSurfaceView?.holder?.surface!!)
        params.captureRequestBuilder?.addTarget(surface)

        params.device?.createCaptureSession(Arrays.asList(surface, imageSurface),
            Camera2PreviewSessionStateCallback(activity, params, testConfig), null)
    } catch (e: CameraAccessException) {
        MainActivity.logd("createCameraPreviewSession CameraAccessException: " + e.message)
        e.printStackTrace()
    } catch (e: IllegalStateException) {
        MainActivity.logd("createCameraPreviewSession IllegalStateException: " + e.message)
        e.printStackTrace()
    }
}

/**
 * Set up timers for a still capture. The preview stream is allowed to run here in order to fill the
 * pipeline with images to simulate more realistic camera conditions.
 */
fun initializeStillCapture(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    logd("TakePicture: capture start.")

    if (!params.isOpen) {
        return
    }

    if (params.timer.isFirstPhoto) {
        params.timer.isFirstPhoto = false
    }

    logd("Camera2 initializeStillCapture: 1st photo in a multi-photo test. " +
        "Pausing for " + PrefHelper.getPreviewBuffer(activity) + "ms to let preview run.")
    params.timer.previewFillStart = System.currentTimeMillis()
    sleep(PrefHelper.getPreviewBuffer(activity))
    params.timer.previewFillEnd = System.currentTimeMillis()

    params.timer.captureStart = System.currentTimeMillis()
    params.timer.autofocusStart = System.currentTimeMillis()
    lockFocus(activity, params, testConfig)
}

/**
 * Initiate the auto-focus routine if required.
 */
fun lockFocus(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    logd("In lockFocus.")
    if (!params.isOpen) {
        return
    }

    try {
        if (null != params.device) {
            setAutoFlash(params, params.captureRequestBuilder)
            if (params.imageReader?.surface != null)
                params.captureRequestBuilder?.addTarget(params.imageReader?.surface!!)

            // If this lens can focus, we need to start a focus search and wait for focus lock
            if (params.hasAF &&
                FocusMode.AUTO == testConfig.focusMode) {
                logd("In lockFocus. About to request focus lock and call capture.")

                params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO)
                params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
                params.camera2CaptureSession?.capture(params.captureRequestBuilder?.build()!!,
                    params.camera2CaptureSessionCallback, params.backgroundHandler)

                params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO)
                params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)

                params.state = CameraState.WAITING_FOCUS_LOCK

                params.autoFocusStuckCounter = 0
                params.camera2CaptureSession?.capture(params.captureRequestBuilder?.build()!!,
                    params.camera2CaptureSessionCallback, params.backgroundHandler)
            } else {
                // If no auto-focus requested, go ahead to the still capture routine
                logd("In lockFocus. Fixed focus or continuous focus, calling captureStillPicture.")
                params.state = CameraState.IMAGE_REQUESTED
                captureStillPicture(activity, params, testConfig)
            }
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
}

/**
 * Request pre-capture auto-exposure (AE) metering
 */
fun runPrecaptureSequence(params: CameraParams) {
    if (!params.isOpen) {
        return
    }

    try {
        if (null != params.device) {
            setAutoFlash(params, params.captureRequestBuilder)
            params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)

            params.state = CameraState.WAITING_EXPOSURE_LOCK
            params.camera2CaptureSession?.capture(params.captureRequestBuilder?.build()!!,
                params.camera2CaptureSessionCallback, params.backgroundHandler)
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
}

/**
 * Make a still capture request. At this point, AF and AE should be converged or unnecessary.
 */
fun captureStillPicture(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    if (!params.isOpen) {
        return
    }

    try {
        logd("In captureStillPicture. Current test: " + testConfig.currentRunningTest.toString())

        if (null != params.device) {
            params.timer.autofocusEnd = System.currentTimeMillis()

            params.captureRequestBuilder =
                params.device?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

            if (params.imageReader?.surface != null)
                params.captureRequestBuilder?.addTarget(params.imageReader?.surface!!)

            when (testConfig.focusMode) {
                FocusMode.CONTINUOUS -> {
                    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                }
                FocusMode.AUTO -> {
                    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                }
                FocusMode.FIXED -> {
                }
            }

            // Disable HDR+ for Pixel devices
            // This is a hack, Pixel devices do not have Sepia mode, but this forces HDR+ off
            if (android.os.Build.MANUFACTURER.equals("Google")) {
                //    params.captureRequestBuilder?.set(CaptureRequest.CONTROL_EFFECT_MODE,
                //        CaptureRequest.CONTROL_EFFECT_MODE_SEPIA)
            }

            // Orientation
            val rotation = activity.windowManager.defaultDisplay.rotation
            val capturedImageRotation = getOrientation(params, rotation)
            params.captureRequestBuilder
                ?.set(CaptureRequest.JPEG_ORIENTATION, capturedImageRotation)

            // Flash
            setAutoFlash(params, params.captureRequestBuilder)

            val captureCallback = Camera2CaptureCallback(activity, params, testConfig)
            params.camera2CaptureSession?.capture(params.captureRequestBuilder?.build()!!,
                captureCallback, params.backgroundHandler)
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    } catch (e: IllegalStateException) {
        logd("captureStillPicture IllegalStateException, aborting: " + e.message)
    }
}

/**
 * Close preview stream and camera device. If this was a switch test, restore the camera id
 */
fun camera2CloseCamera(params: CameraParams?, testConfig: TestConfig) {
    if (params == null)
        return

    MainActivity.logd("closePreviewAndCamera: " + params.id)
    if (params.isPreviewing) {
        params.timer.previewCloseStart = System.currentTimeMillis()
        params.camera2CaptureSession?.close()
    } else {
        params.timer.cameraCloseStart = System.currentTimeMillis()
        params.device?.close()
    }

    if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
        (testConfig.currentRunningTest == TestType.MULTI_SWITCH)) {
        params.id = testConfig.switchTestRealCameraId // Restore the actual camera ID
    }
}

/**
 * An abort request has been received. Abandon everything
 */
fun camera2Abort(activity: MainActivity, params: CameraParams) {
    params.camera2CaptureSession?.abortCaptures()
    activity.stopBackgroundThread(params)
}