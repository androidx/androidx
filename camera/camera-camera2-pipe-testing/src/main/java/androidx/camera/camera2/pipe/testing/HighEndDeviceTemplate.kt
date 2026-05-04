/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata

/**
 * Sanitized implementation of [DeviceTemplate] simulating a high-end device.
 *
 * Use this template when testing features that require advanced camera capabilities, such as manual
 * sensor control, RAW capture, or physical flash availability. It provides a baseline of "FULL"
 * hardware level and advanced capabilities. Uses only standard Android keys and safe values
 * extracted from a device dump.
 */
public object HighEndDeviceTemplate : DeviceTemplate {

    private val requiredCharacteristics =
        mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.SENSOR_ORIENTATION to 90,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH to 7.toByte(),
            CameraCharacteristics.FLASH_INFO_AVAILABLE to true,
        )

    private val highEndDefaults =
        mapOf(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    android.hardware.camera2.CameraMetadata
                        .REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                    android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW,
                ),
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON,
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                ),
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO,
                    android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                ),
        )

    private val frontFacingOverrides =
        mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT,
            CameraCharacteristics.SENSOR_ORIENTATION to 270,
            CameraCharacteristics.FLASH_INFO_AVAILABLE to false,
            CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES to
                intArrayOf(0, 1, 2),
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 5.0f,
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to intArrayOf(0, 1, 2, 3, 4, 5),
        )

    private val backFacingOverrides =
        mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.SENSOR_ORIENTATION to 90,
            CameraCharacteristics.FLASH_INFO_AVAILABLE to true,
        )

    override fun createCameraMetadata(
        cameraId: CameraId,
        lensFacing: Int?,
        characteristicsOverrides: Map<CameraCharacteristics.Key<*>, Any?>,
        metadataOverrides: Map<Metadata.Key<*>, Any?>,
        requestKeysOverrides: Set<CaptureRequest.Key<*>>,
        resultKeysOverrides: Set<CaptureResult.Key<*>>,
    ): CameraMetadata {
        val lensFacingOverrides =
            if (
                (lensFacing ?: CameraCharacteristics.LENS_FACING_BACK) ==
                    CameraCharacteristics.LENS_FACING_FRONT
            ) {
                frontFacingOverrides
            } else {
                backFacingOverrides
            }

        // Combine them in order of priority: Required < Default < FacingOverrides < Overrides
        val allCharacteristics =
            requiredCharacteristics +
                highEndDefaults +
                lensFacingOverrides +
                characteristicsOverrides

        return FakeCameraMetadata(
            characteristics = allCharacteristics,
            cameraId = cameraId,
            metadata = metadataOverrides,
            requestKeys =
                setOf(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.FLASH_MODE,
                ) + requestKeysOverrides,
            resultKeys =
                setOf(CaptureResult.CONTROL_AE_STATE, CaptureResult.CONTROL_AF_STATE) +
                    resultKeysOverrides,
        )
    }
}
