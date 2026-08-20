/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common.samples

import android.hardware.camera2.CaptureResult
import androidx.annotation.Sampled
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.common.CaptureRequestWrappers
import androidx.camera.common.CaptureResultWrapper
import androidx.camera.common.CaptureResultWrappers
import androidx.camera.common.Metadata
import androidx.camera.common.testing.FakeCaptureRequest
import androidx.camera.common.testing.FakeCaptureResult

@Sampled
fun wrapCaptureResultSample(captureResult: CaptureResult) {
    val cameraId = CameraId("0")
    val captureRequest = CaptureRequestWrappers.wrap(captureResult.request)

    // Wrap a native CaptureResult into a CaptureResultWrapper.
    val resultWrapper: CaptureResultWrapper =
        CaptureResultWrappers.wrap(
            captureResult = captureResult,
            cameraId = cameraId,
            captureRequest = captureRequest,
        )

    // Query standard Camera2 result keys:
    val lensState = resultWrapper[CaptureResult.LENS_STATE]
    val frameNumber = resultWrapper.frameNumber
    val originatingRequest = resultWrapper.captureRequest
}

@Sampled
fun fakeCaptureResultSample() {
    val customKey = Metadata.Key<String>("com.example.custom_result_data")
    val fakeRequest = FakeCaptureRequest()

    // Create a FakeCaptureResult for unit testing without requiring a real camera device.
    val fakeResult: CaptureResultWrapper =
        FakeCaptureResult(
            cameraId = CameraId("0"),
            frameNumber = CameraFrameNumber(42L),
            captureRequest = fakeRequest,
            resultParameters =
                mapOf(
                    CaptureResult.LENS_STATE to CaptureResult.LENS_STATE_STATIONARY,
                    CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
                ),
            resultMetadata = mapOf(customKey to "test_result_value"),
        )

    // Query values and verify assertions in unit tests:
    val lensState = fakeResult[CaptureResult.LENS_STATE]
    val frameNumber = fakeResult.frameNumber
    val customValue = fakeResult[customKey]
}
