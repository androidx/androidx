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

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.annotation.Sampled
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraCharacteristicsWrappers
import androidx.camera.common.CameraCharacteristicsWrappers.availableColorSpaceProfiles
import androidx.camera.common.CameraCharacteristicsWrappers.availableDynamicRangeProfiles
import androidx.camera.common.CameraCharacteristicsWrappers.streamConfigurationMap
import androidx.camera.common.CameraId
import androidx.camera.common.testing.FakeCameraCharacteristics
import androidx.camera.common.testing.FakeStreamConfigurationMap
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputKey
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputValues

@Sampled
fun wrapCameraCharacteristicsSample(context: Context, cameraIdString: String) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val characteristics = cameraManager.getCameraCharacteristics(cameraIdString)
    val cameraId = CameraId(cameraIdString)

    // Wrap the native CameraCharacteristics instance into a CameraCharacteristicsWrapper.
    val cameraCharacteristics: CameraCharacteristicsWrapper =
        CameraCharacteristicsWrappers.wrap(cameraId, characteristics)
}

@Sampled
fun loadCameraCharacteristicsSample(context: Context, cameraIdString: String) {
    val cameraId = CameraId(cameraIdString)

    // Load CameraCharacteristicsWrapper directly using Context.
    // This checks camera permissions and marks isRestricted accordingly.
    val characteristicsFromContext: CameraCharacteristicsWrapper =
        CameraCharacteristicsWrappers.loadFrom(context, cameraId)

    // Or load directly using CameraManager:
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val characteristicsFromManager: CameraCharacteristicsWrapper =
        CameraCharacteristicsWrappers.loadFrom(cameraManager, cameraId)
}

@Sampled
fun accessCameraCharacteristicsPropertiesSample(
    // Accepting CameraCharacteristicsWrapper directly allows this logic to be tested
    // by providing a fake implementation of CameraCharacteristicsWrapper (such as
    // FakeCameraCharacteristics) in unit tests.
    cameraCharacteristics: CameraCharacteristicsWrapper
) {
    // Access standard CameraCharacteristics keys using the get/bracket operator:
    val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]
    val lensFacing = cameraCharacteristics[CameraCharacteristics.LENS_FACING]

    // Access extended/custom properties using the extension properties.
    // Note: Querying these properties will query custom/compatibility metadata keys first
    // (such as STREAM_CONFIGURATION_MAP, AVAILABLE_COLOR_SPACE_PROFILES,
    // AVAILABLE_DYNAMIC_RANGE_PROFILES) and fallback to creating internal wrappers from the
    // native platform characteristics. In unit tests, test fakes supply the custom wrapped
    // versions via these metadata keys.
    val streamConfigurationMap = cameraCharacteristics.streamConfigurationMap
    val colorSpaceProfiles = cameraCharacteristics.availableColorSpaceProfiles
    val dynamicRangeProfiles = cameraCharacteristics.availableDynamicRangeProfiles
}

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

    // The extension property accesses the wrapped version (STREAM_CONFIGURATION_MAP) first,
    // returning the FakeStreamConfigurationMap in unit tests without needing real camera hardware.
    val streamConfigMap = checkNotNull(fakeCharacteristics.streamConfigurationMap)
    val supportedFormats = streamConfigMap.getOutputFormats()
    val yuvSizes = streamConfigMap.getOutputSizes(ImageFormat.YUV_420_888)
}
