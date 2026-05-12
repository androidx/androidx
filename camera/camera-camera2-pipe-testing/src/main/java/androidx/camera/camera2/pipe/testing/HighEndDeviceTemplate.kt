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

package androidx.camera.camera2.pipe.testing

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.*
import android.hardware.camera2.CameraMetadata.*
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
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
            INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            REQUEST_PIPELINE_MAX_DEPTH to 7.toByte(),
        )

    private val highEndDefaults =
        mapOf(
            REQUEST_AVAILABLE_CAPABILITIES to
                intArrayOf(
                    REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                    REQUEST_AVAILABLE_CAPABILITIES_RAW,
                ),
            CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(CONTROL_AE_MODE_ON, CONTROL_AE_MODE_ON_ALWAYS_FLASH),
            CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(CONTROL_AF_MODE_AUTO, CONTROL_AF_MODE_CONTINUOUS_PICTURE),
            SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
            SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
            SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4032, 3024),
            SENSOR_INFO_COLOR_FILTER_ARRANGEMENT to SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB,
            SENSOR_INFO_WHITE_LEVEL to 1023,
            SENSOR_MAX_ANALOG_SENSITIVITY to 800,
            SENSOR_INFO_TIMESTAMP_SOURCE to SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME,
            SENSOR_INFO_SENSITIVITY_RANGE to Range(50, 10000),
            SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(100000L, 30000000000L),
            SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
            SCALER_CROPPING_TYPE to SCALER_CROPPING_TYPE_CENTER_ONLY,
            CONTROL_AE_LOCK_AVAILABLE to true,
            CONTROL_AWB_LOCK_AVAILABLE to true,
            CONTROL_AWB_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AWB_MODE_AUTO,
                    CONTROL_AWB_MODE_INCANDESCENT,
                    CONTROL_AWB_MODE_FLUORESCENT,
                    CONTROL_AWB_MODE_WARM_FLUORESCENT,
                    CONTROL_AWB_MODE_DAYLIGHT,
                    CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
                    CONTROL_AWB_MODE_TWILIGHT,
                    CONTROL_AWB_MODE_SHADE,
                ),
            TONEMAP_MAX_CURVE_POINTS to 64,
        )

    private val frontFacingOverrides =
        mapOf(
            LENS_FACING to LENS_FACING_FRONT,
            SENSOR_ORIENTATION to 270,
            FLASH_INFO_AVAILABLE to false,
            LENS_INFO_MINIMUM_FOCUS_DISTANCE to 5.0f,
            CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AF_MODE_OFF,
                    CONTROL_AF_MODE_AUTO,
                    CONTROL_AF_MODE_MACRO,
                    CONTROL_AF_MODE_CONTINUOUS_VIDEO,
                    CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    CONTROL_AF_MODE_EDOF,
                ),
            STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES to
                intArrayOf(
                    STATISTICS_FACE_DETECT_MODE_OFF,
                    STATISTICS_FACE_DETECT_MODE_SIMPLE,
                    STATISTICS_FACE_DETECT_MODE_FULL,
                ),
            STATISTICS_INFO_MAX_FACE_COUNT to 10,
        )

    private val backFacingOverrides =
        mapOf(
            LENS_FACING to LENS_FACING_BACK,
            SENSOR_ORIENTATION to 90,
            FLASH_INFO_AVAILABLE to true,
            LENS_INFO_MINIMUM_FOCUS_DISTANCE to 10.0f,
        )

    override fun createCameraMetadata(
        cameraId: CameraId,
        lensFacing: Int?,
        characteristicsOverrides: Map<CameraCharacteristics.Key<*>, Any?>,
        metadataOverrides: Map<Metadata.Key<*>, Any?>,
        requestKeysOverrides: Set<CaptureRequest.Key<*>>,
        resultKeysOverrides: Set<CaptureResult.Key<*>>,
    ): FakeCameraMetadata {
        val lensFacingOverrides =
            if ((lensFacing ?: LENS_FACING_BACK) == LENS_FACING_FRONT) {
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
