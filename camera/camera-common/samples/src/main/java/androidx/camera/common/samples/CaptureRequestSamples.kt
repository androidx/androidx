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

import android.hardware.camera2.CaptureRequest
import androidx.annotation.Sampled
import androidx.camera.common.CaptureRequestWrapper
import androidx.camera.common.CaptureRequestWrappers
import androidx.camera.common.Metadata

@Sampled
fun wrapCaptureRequestSample(captureRequest: CaptureRequest) {
    // Wrap a native CaptureRequest into a CaptureRequestWrapper.
    val requestWrapper: CaptureRequestWrapper = CaptureRequestWrappers.wrap(captureRequest)

    // Query native Camera2 CaptureRequest keys:
    val aeMode = requestWrapper[CaptureRequest.CONTROL_AE_MODE]

    // Or attach custom library/application metadata to the request:
    val customKey = Metadata.Key<String>("com.example.custom_request_tag")
    val requestWithMetadata =
        CaptureRequestWrappers.wrap(
            captureRequest,
            metadata = mapOf(customKey to "sample_tag_value"),
        )
    val customTagValue = requestWithMetadata[customKey]
}
