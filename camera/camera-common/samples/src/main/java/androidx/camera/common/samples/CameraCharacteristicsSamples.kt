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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.annotation.Sampled
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraCharacteristicsWrappers
import androidx.camera.common.CameraCharacteristicsWrappers.streamConfigurationMap
import androidx.camera.common.CameraId

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
fun accessCameraCharacteristicsPropertiesSample(
    // Accepting CameraCharacteristicsWrapper directly allows this logic to be tested
    // by providing a fake implementation of CameraCharacteristicsWrapper (such as
    // FakeCameraCharacteristics) in unit tests.
    cameraCharacteristics: CameraCharacteristicsWrapper
) {
    // Access standard CameraCharacteristics keys using the get/bracket operator:
    val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]
    val lensFacing = cameraCharacteristics[CameraCharacteristics.LENS_FACING]

    // Access custom properties (such as streamConfigurationMap) using the extension
    // functions/properties.
    // Custom properties can also be supplied or configured in tests via test fakes.
    val streamConfigurationMap = cameraCharacteristics.streamConfigurationMap
}
