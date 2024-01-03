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
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.view.Surface
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.integration.antelope.testEnded

/**
 * Callbacks that track an image capture session
 */
class CameraXCaptureSessionCallback(
    internal val activity: MainActivity,
    internal val params: CameraParams,
    internal val testConfig: TestConfig
) : CameraCaptureSession.CaptureCallback() {

    /** Capture has been aborted. */
    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        MainActivity.logd(
            "CameraX captureCallback: Sequence aborted. Current test: " +
                testConfig.currentRunningTest.toString()
        )
        super.onCaptureSequenceAborted(session, sequenceId)
    }

    /** Capture has failed, try to restart */
    override fun onCaptureFailed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        failure: CaptureFailure
    ) {
        MainActivity.logd(
            "CameraX captureStillPicture captureCallback: Capture Failed. Failure: " +
                failure.reason + " Current test: " + testConfig.currentRunningTest.toString()
        )
        closeCameraX(activity, params, testConfig)
        cameraXOpenCamera(activity, params, testConfig)
    }

    /** Unused but retained here as it can be useful for debugging  */
    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long
    ) {
        // MainActivity.logd("CameraX captureStillPicture captureCallback: Capture Started.")
        super.onCaptureStarted(session, request, timestamp, frameNumber)
    }

    /** Unused but retained here as it can be useful for debugging  */
    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult
    ) {
        // MainActivity.logd("CameraX captureStillPicture captureCallback: Capture progressed.")
        super.onCaptureProgressed(session, request, partialResult)
    }

    /** Unused but retained here as it can be useful for debugging  */
    override fun onCaptureBufferLost(
        session: CameraCaptureSession,
        request: CaptureRequest,
        target: Surface,
        frameNumber: Long
    ) {
        // MainActivity.logd("CameraX captureStillPicture captureCallback: Buffer lost.")
        super.onCaptureBufferLost(session, request, target, frameNumber)
    }

    /**
     * Still capture has completed. Record timing and proceed to next test or finish.
     */
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {

        if (params.cameraXLifecycle.isFinished()) {
            cameraXAbort(activity, params, testConfig)
            return
        }

        logd("CameraX onCaptureCompleted!! " + request.tag)
        // Prevent duplicate captures from being triggered
        if (testConfig.isFirstOnCaptureComplete) {
            testConfig.isFirstOnCaptureComplete = false
        } else {
            return
        }

        params.timer.captureEnd = System.currentTimeMillis()
        MainActivity.logd(
            "CameraX StillCapture completed. CaptureEnd. Current test: " +
                testConfig.currentRunningTest.toString()
        )

        // ImageReader might get the image before this callback is called, if so, the test is done
        if (0L != params.timer.imageSaveEnd) {
            params.timer.imageReaderStart = params.timer.imageReaderEnd // No ImageReader delay
            MainActivity.logd(
                "StillCapture completed. Ending Test. Current test: " +
                    testConfig.currentRunningTest.toString()
            )

            if (TestType.MULTI_PHOTO_CHAIN == testConfig.currentRunningTest) {
                testEnded(activity, params, testConfig)
            } else {
                testConfig.testFinished = true
                closeCameraX(activity, params, testConfig)
            }
        } else {
            MainActivity.logd(
                "StillCapture completed. Waiting on imageReader. Current test: " +
                    testConfig.currentRunningTest.toString()
            )
            params.timer.imageReaderStart = System.currentTimeMillis()
        }
    }
}
