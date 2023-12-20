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

// Camera 1 API is deprecated, suppress warnings as Antelope expressly tests it
@file:Suppress("deprecation")

package androidx.camera.integration.antelope.cameracontrollers

import android.hardware.Camera
import android.util.Size
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.CompareSizesByArea
import androidx.camera.integration.antelope.FocusMode
import androidx.camera.integration.antelope.ImageCaptureSize
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.integration.antelope.testEnded
import androidx.camera.integration.antelope.writeFile
import java.util.Collections

internal var camera1: Camera? = null

/**
 * Opens the camera using the Camera1 API and measures the open time synchronously. For init tests,
 * this is the only measurement needed so end the test. Otherwise, move on to open the camera
 * preview.
 */
fun camera1OpenCamera(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    try {
        logd("openCamera: " + params.id)
        params.isOpen = true
        params.timer.openStart = System.currentTimeMillis()

        logd("Camera1Switch Open camera: " + testConfig.switchTestCurrentCamera.toInt())

        if ((testConfig.currentRunningTest == TestType.SWITCH_CAMERA) ||
            (testConfig.currentRunningTest == TestType.MULTI_SWITCH)
        )
            camera1 = Camera.open(testConfig.switchTestCurrentCamera.toInt())
        else
            camera1 = Camera.open(testConfig.camera.toInt())

        params.timer.openEnd = System.currentTimeMillis()

        // Due to the synchronous nature of Camera1, set up Camera1 specific parameters here
        val camera1Params: Camera.Parameters? = camera1?.parameters
        params.cam1AFSupported =
            camera1Params?.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_AUTO)
            ?: false

        when (testConfig.currentRunningTest) {
            TestType.INIT -> {
                // Camera opened, we're done
                testEnded(activity, params, testConfig)
            }

            else -> {
                startCamera1Preview(activity, params, testConfig)
            }
        }
    } catch (e: Exception) {
        logd("camera1OpenCamera exception: " + params.id + ". Error: " + e.printStackTrace())
        camera1 = null
    }
}

/**
 * Begin the preview using the Camera 1 API and synchronously measure the time to begin the stream.
 */
fun startCamera1Preview(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    val camera1Params: Camera.Parameters? = camera1?.parameters

    params.cam1AFSupported =
        camera1Params?.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_AUTO) ?: false

    // Get Camera1 image capture sizes
    // Cannot be done this before the camera is device opened
    val cam1Sizes = camera1Params?.getSupportedPictureSizes()
    if (null != cam1Sizes) {
        val saneSizes: ArrayList<Size> = ArrayList()

        for (size in cam1Sizes) {
            saneSizes.add(Size(size.width, size.height))
        }

        params.cam1MaxSize = Collections.max(saneSizes, CompareSizesByArea())
        params.cam1MinSize = Collections.min(saneSizes, CompareSizesByArea())
    }

    if (ImageCaptureSize.MIN == testConfig.imageCaptureSize)
        camera1Params?.setPictureSize(params.cam1MinSize.width, params.cam1MinSize.height)
    else
        camera1Params?.setPictureSize(params.cam1MaxSize.width, params.cam1MaxSize.height)

    if (params.cam1AFSupported) {
        if (FocusMode.CONTINUOUS == testConfig.focusMode)
            camera1Params?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        else
            camera1Params?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
    } else {
        camera1Params?.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
    }

    // After changing camera parameters, set them
    camera1?.parameters = camera1Params

    logd("startCamera1Preview: starting Camera1 preview.")

    params.isPreviewing = true
    params.timer.previewStart = System.currentTimeMillis()
    camera1?.startPreview()
    camera1?.setPreviewDisplay(params.previewSurfaceView?.holder)
    params.timer.previewEnd = System.currentTimeMillis()

    when (testConfig.currentRunningTest) {
        TestType.PREVIEW -> {
            testConfig.testFinished = true
            closePreviewAndCamera(activity, params, testConfig)
        }

        TestType.SWITCH_CAMERA, TestType.MULTI_SWITCH -> {
            logd("Camera1Switch preview running:")
            if (testConfig.switchTestCurrentCamera == testConfig.switchTestCameras.get(0)) {
                if (testConfig.testFinished) {
                    logd("Camera1Switch preview. On 1st camera, test finished. Closing 1st camera")
                    params.timer.switchToFirstEnd = System.currentTimeMillis()
                    Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                    closePreviewAndCamera(activity, params, testConfig)
                } else {
                    logd("Camera1Switch preview. On 1st camera, Closing 1st camera, then open 2nd")
                    Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                    params.timer.switchToSecondStart = System.currentTimeMillis()
                    closePreviewAndCamera(activity, params, testConfig)
                }
            } else {
                logd(
                    "Camera1Switch preview. On 2nd camera. Closing, ready to open first 1st " +
                        "camera"
                )
                params.timer.switchToSecondEnd = System.currentTimeMillis()
                Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                params.timer.switchToFirstStart = System.currentTimeMillis()
                closePreviewAndCamera(activity, params, testConfig)
            }
        }

        TestType.NONE -> {
            closeAllCameras(activity, testConfig)
        }

        else -> {
            camera1TakePicturePrep(activity, params, testConfig)
        }
    }
}

/**
 * Set up timers and focus mode for taking a picture with the Camera 1 API. If auto-focus is
 * requested, begin the auto-focus timer and asynchronously begin the auto-focus routine.
 */
fun camera1TakePicturePrep(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    if (params.timer.isFirstPhoto) {
        logd(
            "camera1TakePicturePrep: 1st photo in multi-chain test. Pausing for " +
                PrefHelper.getPreviewBuffer(activity) + "ms to let preview run."
        )
        params.timer.previewFillStart = System.currentTimeMillis()
        Thread.sleep(PrefHelper.getPreviewBuffer(activity))
        params.timer.previewFillEnd = System.currentTimeMillis()
        params.timer.isFirstPhoto = false
    }

    params.timer.captureStart = System.currentTimeMillis()

    if (params.cam1AFSupported &&
        FocusMode.AUTO == testConfig.focusMode
    ) {
        MainActivity.logd("camera1TakePicturePrep: starting autofocus.")
        params.timer.autofocusStart = System.currentTimeMillis()
        camera1?.autoFocus(Camera1AutofocusCallback(activity, params, testConfig))
    } else {
        camera1TakePicture(activity, params, testConfig)
    }
}

/**
 * Initiate the capture request
 */
fun camera1TakePicture(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {
    val camera1JpegCallback = Camera1PictureCallback(activity, params, testConfig)

    try {
        MainActivity.logd("camera1TakePicture: capture start. ")
        camera1?.takePicture(null, null, camera1JpegCallback)
    } catch (e: RuntimeException) {
        MainActivity.logd("camera1TakePicture: runtime exception: " + e.printStackTrace())
    }
}

/**
 * Close preview stream and camera device. If this is a switch test, begin the next step
 */
fun camera1CloseCamera(activity: MainActivity, params: CameraParams?, testConfig: TestConfig) {
    if (params == null)
        return

    if (params.isPreviewing) {
        params.timer.previewCloseStart = System.currentTimeMillis()
        camera1?.stopPreview()
        params.timer.previewCloseEnd = System.currentTimeMillis()
        params.isPreviewing = false
    }

    params.timer.cameraCloseStart = System.currentTimeMillis()
    camera1?.release()
    params.timer.cameraCloseEnd = System.currentTimeMillis()
    params.isOpen = false

    logd("Camera 1 Close camera: camera released.")

    if (testConfig.testFinished) {
        logd("Camera 1 Close camera: Test finished, returning")
        testEnded(activity, params, testConfig)
        return
    }

    when (testConfig.currentRunningTest) {
        TestType.SWITCH_CAMERA, TestType.MULTI_SWITCH -> {
            logd("Camera1Switch Close camera")
            // First camera closed, now start the second
            if (testConfig.switchTestCurrentCamera == testConfig.switchTestCameras.get(0)) {
                testConfig.switchTestCurrentCamera = testConfig.switchTestCameras.get(1)
                logd("Camera1Switch Close camera 1st camera is closed, opening the second")
                camera1OpenCamera(activity, params, testConfig)
            }

            // Second camera closed, now start the first
            else if (testConfig.switchTestCurrentCamera == testConfig.switchTestCameras.get(1)) {
                logd("Camera1Switch Close camera 2nd camera is closed, opening the first")
                testConfig.switchTestCurrentCamera = testConfig.switchTestCameras.get(0)
                testConfig.testFinished = true
                camera1OpenCamera(activity, params, testConfig)
            }
        }
        else -> {
            Unit // no-op
        }
    }
}

/**
 * Auto-focus is complete, record the elapsed time and request the capture
 */
class Camera1AutofocusCallback internal constructor(
    internal val activity: MainActivity,
    internal val params: CameraParams,
    internal val testConfig: TestConfig
) : Camera.AutoFocusCallback {

    override fun onAutoFocus(p0: Boolean, p1: Camera?) {
        MainActivity.logd("camera1AutofocusCallback: autofocus complete.")
        params.timer.autofocusEnd = System.currentTimeMillis()
        camera1TakePicture(activity, params, testConfig)
    }
}

/**
 * Image capture has completed. Record the time taken, synchronously write file to disk and measure
 * the time required. This test run is finished, call to finalize test or continue the test run.
 */
class Camera1PictureCallback internal constructor(
    internal val activity: MainActivity,
    internal val params: CameraParams,
    internal val testConfig: TestConfig
) : Camera.PictureCallback {

    override fun onPictureTaken(bytes: ByteArray?, p1: Camera?) {

        params.timer.captureEnd = System.currentTimeMillis()

        // With the Camera1 API, calling takePicture() stops the preview. In order to make sure the
        // close timings are comparable across APIs, restart it here.
        camera1?.startPreview()

        logd("in Camera1PictureCallback onPictureTaken")

        params.timer.imageReaderStart = System.currentTimeMillis()
        params.timer.imageReaderEnd = System.currentTimeMillis()
        params.timer.imageSaveStart = System.currentTimeMillis()

        if (null != bytes)
            writeFile(activity, bytes)

        params.timer.imageSaveEnd = System.currentTimeMillis()

        testConfig.testFinished = true
        closePreviewAndCamera(activity, params, testConfig)
    }
}
