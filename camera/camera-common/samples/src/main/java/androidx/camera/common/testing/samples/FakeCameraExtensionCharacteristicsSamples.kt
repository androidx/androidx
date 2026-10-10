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

import android.graphics.ImageFormat
import android.hardware.camera2.CameraExtensionCharacteristics
import android.util.Range
import android.util.Size
import androidx.annotation.Sampled
import androidx.camera.common.CameraExtensionCharacteristicsWrapper
import androidx.camera.common.CameraId
import androidx.camera.common.testing.FakeCameraExtensionCharacteristics

@Sampled
fun fakeCameraExtensionCharacteristicsSample() {
    val captureSize = Size(1920, 1080)
    val postviewSize = Size(640, 480)

    // In Kotlin, use the companion invoke operator with a strongly typed CameraId:
    val characteristics: CameraExtensionCharacteristicsWrapper =
        FakeCameraExtensionCharacteristics(
            cameraId = CameraId("0"),
            cameraExtension = CameraExtensionCharacteristics.EXTENSION_BOKEH,
            isPostviewSupported = true,
            outputSizesFormat = mapOf(ImageFormat.JPEG to setOf(captureSize)),
            postviewSizes = mapOf((captureSize to ImageFormat.JPEG) to setOf(postviewSize)),
            latencies = mapOf((captureSize to ImageFormat.JPEG) to Range(100L, 300L)),
        )

    val bokehJpegSizes = characteristics.getOutputSizes(ImageFormat.JPEG)
    val bokehPostviewSizes = characteristics.getPostviewSizes(captureSize, ImageFormat.JPEG)
    val latencyRange =
        characteristics.getEstimatedCaptureLatencyRangeMillis(captureSize, ImageFormat.JPEG)

    // Or use FakeCameraExtensionCharacteristics.create(...) with a String camera ID:
    val characteristicsFromCreate: CameraExtensionCharacteristicsWrapper =
        FakeCameraExtensionCharacteristics.create(
            cameraId = "0",
            cameraExtension = CameraExtensionCharacteristics.EXTENSION_HDR,
            outputSizesFormat = mapOf(ImageFormat.YUV_420_888 to setOf(captureSize)),
        )
    val hdrYuvSizes = characteristicsFromCreate.getOutputSizes(ImageFormat.YUV_420_888)
}
