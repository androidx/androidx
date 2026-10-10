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

package androidx.camera.common.testing.samples

import android.hardware.camera2.CaptureResult
import androidx.annotation.Sampled
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.common.CaptureResultWrapper
import androidx.camera.common.Metadata
import androidx.camera.common.testing.FakeCaptureRequest
import androidx.camera.common.testing.FakeCaptureResult

@Sampled
fun fakeCaptureResultSample() {
    val customKey = Metadata.Key<String>("com.example.custom_result_data")
    val fakeRequest = FakeCaptureRequest()

    // Create a FakeCaptureResult in Kotlin using strongly typed CameraId and CameraFrameNumber:
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

    // Or create a FakeCaptureResult using FakeCaptureResult.create(...) with primitive types:
    val fakeResultFromCreate: CaptureResultWrapper =
        FakeCaptureResult.create(
            cameraId = "0",
            frameNumber = 43L,
            captureRequest = fakeRequest,
            resultParameters = mapOf(CaptureResult.LENS_STATE to CaptureResult.LENS_STATE_MOVING),
        )

    // Query values and verify assertions in unit tests:
    val cameraId = fakeResult.cameraId
    val frameNumber = fakeResult.frameNumber
    val lensState = fakeResult[CaptureResult.LENS_STATE]
    val customValue = fakeResult[customKey]
    val nextFrameNumber = fakeResultFromCreate.frameNumber
}
