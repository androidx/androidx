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
import android.util.Size
import androidx.annotation.Sampled
import androidx.camera.common.StreamConfigurationMapWrapper
import androidx.camera.common.testing.FakeStreamConfigurationMap
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputKey
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputValues

@Sampled
fun fakeStreamConfigurationMapSample() {
    val size1080p = Size(1920, 1080)
    val fakeMap: StreamConfigurationMapWrapper =
        FakeStreamConfigurationMap(
            outputsTable =
                linkedMapOf(
                    OutputKey(ImageFormat.YUV_420_888, size1080p) to
                        OutputValues(
                            minDuration = 33_333_333L // ~30fps minimum frame duration
                        ),
                    OutputKey(ImageFormat.JPEG, size1080p) to
                        OutputValues(
                            stallDuration = 200_000_000L // 200ms stall duration for JPEG
                        ),
                )
        )

    val outputFormats = fakeMap.getOutputFormats()
    val isYuvSupported = fakeMap.isOutputSupportedFor(ImageFormat.YUV_420_888)
    val jpegSizes = fakeMap.getOutputSizes(ImageFormat.JPEG)
    val yuvMinFrameDuration = fakeMap.getOutputMinFrameDuration(ImageFormat.YUV_420_888, size1080p)
    val jpegStallDuration = fakeMap.getOutputStallDuration(ImageFormat.JPEG, size1080p)
}
