/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log.info

/**
 * Logs the required device info, e.g. camera hardware level required by CameraXHardwareLevelPlugin.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
object DeviceInfoLogger {
    fun logDeviceInfo(cameraProperties: CameraProperties) {
        // Extend by adding logging here as needed.
        logDeviceLevel(cameraProperties)
    }

    private fun logDeviceLevel(cameraProperties: CameraProperties) {
        val levelString: String
        val deviceLevel = cameraProperties.metadata.getOrDefault(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1
        )

        levelString =
            when (deviceLevel) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ->
                    "INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
                    "INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ->
                    "INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ->
                    "INFO_SUPPORTED_HARDWARE_LEVEL_FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 ->
                    "INFO_SUPPORTED_HARDWARE_LEVEL_3"
                else -> "Unknown value: $deviceLevel"
            }

        info { "Device Level: $levelString" }
    }
}
