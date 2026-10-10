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
import androidx.camera.common.CameraId
import androidx.camera.common.CaptureRequestWrappers
import androidx.camera.common.CaptureResultWrapper
import androidx.camera.common.CaptureResultWrappers

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
