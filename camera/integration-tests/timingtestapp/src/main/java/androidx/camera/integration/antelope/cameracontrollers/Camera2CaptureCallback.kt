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
import androidx.annotation.NonNull
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.TestConfig
import androidx.camera.integration.antelope.TestType
import androidx.camera.integration.antelope.testEnded

/**
 * Image capture callback for Camera 2 API. Tracks state of an image capture request.
 */
class Camera2CaptureCallback(
    internal val activity: MainActivity,
    internal val params: CameraParams,
    internal val testConfig: TestConfig
) : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureSequenceAborted(session: CameraCaptureSession?, sequenceId: Int) {
        MainActivity.logd("captureStillPicture captureCallback: Sequence aborted. Current test: " +
            testConfig.currentRunningTest.toString())
        super.onCaptureSequenceAborted(session, sequenceId)
    }

    override fun onCaptureFailed(
        session: CameraCaptureSession?,
        request: CaptureRequest?,
        failure: CaptureFailure?
    ) {

        if (!params.isOpen) {
            return
        }

        MainActivity.logd("captureStillPicture captureCallback: Capture Failed. Failure: " +
            failure?.reason + " Current test: " + testConfig.currentRunningTest.toString())

        // The session failed. Let's just try again (yay infinite loops)
        closePreviewAndCamera(activity, params, testConfig)
        camera2OpenCamera(activity, params, testConfig)
        super.onCaptureFailed(session, request, failure)
    }

    override fun onCaptureStarted(
        session: CameraCaptureSession?,
        request: CaptureRequest?,
        timestamp: Long,
        frameNumber: Long
    ) {
        MainActivity.logd("captureStillPicture captureCallback: Capture Started. Current test: " +
            testConfig.currentRunningTest.toString() + ", frame number: " + frameNumber)
        super.onCaptureStarted(session, request, timestamp, frameNumber)
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession?,
        request: CaptureRequest?,
        partialResult: CaptureResult?
    ) {
        MainActivity.logd("captureStillPicture captureCallback: Capture progressed. " +
            "Current test: " + testConfig.currentRunningTest.toString())
        super.onCaptureProgressed(session, request, partialResult)
    }

    override fun onCaptureBufferLost(
        session: CameraCaptureSession?,
        request: CaptureRequest?,
        target: Surface?,
        frameNumber: Long
    ) {
        MainActivity.logd("captureStillPicture captureCallback: Buffer lost. Current test: " +
            testConfig.currentRunningTest.toString())
        super.onCaptureBufferLost(session, request, target, frameNumber)
    }

    override fun onCaptureCompleted(
        @NonNull session: CameraCaptureSession,
        @NonNull request: CaptureRequest,
        @NonNull result: TotalCaptureResult
    ) {

        if (!params.isOpen) {
            return
        }

        MainActivity.logd("Camera2 onCaptureCompleted. CaptureEnd. Current test: " +
            testConfig.currentRunningTest.toString())

        params.timer.captureEnd = System.currentTimeMillis()

        params.captureRequestBuilder?.removeTarget(params.imageReader?.surface)

        // ImageReader might get the image before this callback is called, if so, the test is done
        if (0L != params.timer.imageSaveEnd) {
            params.timer.imageReaderStart = params.timer.imageReaderEnd // No ImageReader delay
            MainActivity.logd("Camera2 onCaptureCompleted. Image already saved. " +
                "Ending Test and closing camera.")

            if (TestType.MULTI_PHOTO_CHAIN == testConfig.currentRunningTest) {
                testEnded(activity, params, testConfig)
            } else {
                testConfig.testFinished = true
                closePreviewAndCamera(activity, params, testConfig)
            }

            // Otherwise the test isn't done until the image appears in the reader
        } else {
            MainActivity.logd("Camera2 onCaptureCompleted. Still waiting on imageReader.")
            params.timer.imageReaderStart = System.currentTimeMillis()
        }
    }
}