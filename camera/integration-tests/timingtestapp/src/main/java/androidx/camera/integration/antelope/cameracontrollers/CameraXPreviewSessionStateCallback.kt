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

import android.hardware.camera2.CameraCaptureSession
import androidx.annotation.NonNull
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.PrefHelper
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType

/**
 * Callbacks that track the state of a preview capture session.
 */
class CameraXPreviewSessionStateCallback(
    internal var activity: MainActivity,
    internal var params: CameraParams,
    internal var testConfig: TestConfig
) : CameraCaptureSession.StateCallback() {

    /**
     * Preview session is open and frames are coming through. If the test is preview only, record
     * results and close the camera, if a switch or image capture test, proceed to the next step.
     *
     */
    override fun onActive(session: CameraCaptureSession?) {
        if (params.cameraXLifecycle.isFinished()) {
            cameraXAbort(activity, params, testConfig)
            return
        }

        // Prevent duplicate captures from being triggered if running a capture test
        if (testConfig.currentRunningTest != TestType.MULTI_SWITCH &&
            testConfig.currentRunningTest != TestType.SWITCH_CAMERA) {
            if (testConfig.isFirstOnActive) {
                testConfig.isFirstOnActive = false
            } else {
                return
            }
        }

        params.timer.previewEnd = System.currentTimeMillis()

        when (testConfig.currentRunningTest) {
            TestType.PREVIEW -> {
                testConfig.testFinished = true
                closeCameraX(activity, params, testConfig)
            }

            TestType.SWITCH_CAMERA, TestType.MULTI_SWITCH -> {
                if (testConfig.switchTestCurrentCamera == testConfig.switchTestCameras.get(0)) {
                    if (testConfig.testFinished) {
                        params.timer.switchToFirstEnd = System.currentTimeMillis()
                        Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                        closePreviewAndCamera(activity, params, testConfig)
                    } else {
                        Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                        params.timer.switchToSecondStart = System.currentTimeMillis()
                        closePreviewAndCamera(activity, params, testConfig)
                    }
                } else {
                    params.timer.switchToSecondEnd = System.currentTimeMillis()
                    Thread.sleep(PrefHelper.getPreviewBuffer(activity)) // Let preview run
                    params.timer.switchToFirstStart = System.currentTimeMillis()
                    closePreviewAndCamera(activity, params, testConfig)
                }
            }

            else -> {
                cameraXTakePicture(activity, params, testConfig)
            }
        }
        if (null != session)
            super.onActive(session)
    }

    /**
     * Preview session has been configured. Camera X handles the next step.
     */
    override fun onConfigured(@NonNull cameraCaptureSession: CameraCaptureSession) {
        MainActivity.logd("In onConfigured: CaptureSession configured!")
    }

    /**
     * Configuration of the preview stream failed, try again.
     */
    override fun onConfigureFailed(@NonNull cameraCaptureSession: CameraCaptureSession) {
        MainActivity.logd("CameraX preview initialization failed. Closing camera.")
        closeCameraX(activity, params, testConfig)
    }

    /**
     * Preview session has been closed. Camera X handles the next step.
     */
    override fun onClosed(session: CameraCaptureSession) {
        MainActivity.logd("In CameraXPreviewSessionStateCallback onClosed.")
    }
}