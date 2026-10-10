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
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.annotation.Sampled
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraCharacteristicsWrappers.streamConfigurationMap
import androidx.camera.common.CameraId
import androidx.camera.common.testing.FakeCameraCharacteristics
import androidx.camera.common.testing.FakeStreamConfigurationMap
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputKey
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputValues

@Sampled
fun fakeCameraCharacteristicsSample() {
    // Create a fake stream configuration map with custom resolutions and formats.
    val fakeStreamConfigurationMap =
        FakeStreamConfigurationMap(
            outputsTable =
                linkedMapOf(
                    OutputKey(ImageFormat.YUV_420_888, Size(1920, 1080)) to OutputValues(),
                    OutputKey(ImageFormat.JPEG, Size(1920, 1080)) to
                        OutputValues(
                            stallDuration = 200_000_000L // 200ms stall duration
                        ),
                )
        )

    // Supply the fake stream configuration map via STREAM_CONFIGURATION_MAP metadata key.
    val fakeCharacteristics: CameraCharacteristicsWrapper =
        FakeCameraCharacteristics(
            cameraId = CameraId("0"),
            cameraCharacteristics =
                mapOf(
                    CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                    CameraCharacteristics.SENSOR_ORIENTATION to 90,
                ),
            cameraMetadata =
                mapOf(
                    CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP to
                        fakeStreamConfigurationMap
                ),
        )

    // Query standard CameraCharacteristics keys via the bracket operator:
    val lensFacing = fakeCharacteristics[CameraCharacteristics.LENS_FACING]
    val sensorOrientation = fakeCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]

    // The extension property accesses the wrapped version (STREAM_CONFIGURATION_MAP) first,
    // returning the FakeStreamConfigurationMap in unit tests without needing real camera hardware.
    val streamConfigMap = checkNotNull(fakeCharacteristics.streamConfigurationMap)
    val supportedFormats = streamConfigMap.getOutputFormats()
    val yuvSizes = streamConfigMap.getOutputSizes(ImageFormat.YUV_420_888)
}
