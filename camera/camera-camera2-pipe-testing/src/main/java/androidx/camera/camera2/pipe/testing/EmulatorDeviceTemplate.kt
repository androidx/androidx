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
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStreamConfigurationMap
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.StreamFormat

/**
 * Default implementation of [DeviceTemplate] simulating a standard Android emulator (Cuttlefish).
 *
 * Use this template to ensure baseline compatibility and correctness on limited virtual hardware.
 * Emulators typically lack physical flash and support a "LIMITED" hardware level. This template
 * helps verify that code does not crash or fail when running in standard test environments.
 */
public object EmulatorDeviceTemplate : DeviceTemplate {

    private val emulatorCharacteristics =
        mapOf(
            CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_FAST
                ),
            CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
                ),
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON),
            CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE to Range(-6, 6),
            CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP to Rational(1, 2),
            CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE to true,
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO),
            CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_OFF),
            CameraCharacteristics.CONTROL_AVAILABLE_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_MODE_AUTO),
            CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_SCENE_MODE_DISABLED),
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                ),
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO),
            CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE to true,
            CameraCharacteristics.FLASH_INFO_AVAILABLE to false,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES to
                arrayOf(Size(0, 0), Size(256, 192)),
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                ),
            CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH to 8.toByte(),
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
            CameraCharacteristics.SCALER_CROPPING_TYPE to
                android.hardware.camera2.CameraMetadata.SCALER_CROPPING_TYPE_CENTER_ONLY,
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE to
                android.hardware.camera2.CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN,
            CameraCharacteristics.SENSOR_ORIENTATION to 90,
            CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES to
                intArrayOf(android.hardware.camera2.CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF),
            CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT to 0,
            CameraCharacteristics.SYNC_MAX_LATENCY to
                android.hardware.camera2.CameraMetadata.SYNC_MAX_LATENCY_UNKNOWN,
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 0.0f,
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(4.38f),
            CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(1.8f),
            CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES to floatArrayOf(0.0f),
            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION to
                intArrayOf(
                    android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                ),
            CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE to 0.0f,
        )

    private fun createDefaultCameraStreamConfigurationMap(): CameraStreamConfigurationMap {
        return FakeCameraStreamConfigurationMap(
            outputTable =
                listOf(
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                ),
            outputClassTypes =
                setOf(
                    android.view.SurfaceHolder::class.java,
                    android.graphics.SurfaceTexture::class.java,
                ),
        )
    }

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
            CameraCharacteristics.FLASH_INFO_AVAILABLE to false,
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

        val allCharacteristics =
            emulatorCharacteristics + lensFacingOverrides + characteristicsOverrides
        val defaultMetadata =
            mapOf(
                CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                    createDefaultCameraStreamConfigurationMap()
            )
        val allMetadata = defaultMetadata + metadataOverrides

        return FakeCameraMetadata(
            characteristics = allCharacteristics,
            metadata = allMetadata,
            keys = allCharacteristics.keys,
            requestKeys =
                setOf(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.CONTROL_AE_LOCK,
                    CaptureRequest.CONTROL_AWB_LOCK,
                ) + requestKeysOverrides,
            resultKeys =
                setOf(
                    CaptureResult.CONTROL_AE_MODE,
                    CaptureResult.CONTROL_AF_MODE,
                    CaptureResult.CONTROL_AWB_MODE,
                    CaptureResult.CONTROL_AE_STATE,
                    CaptureResult.CONTROL_AF_STATE,
                    CaptureResult.CONTROL_AWB_STATE,
                ) + resultKeysOverrides,
            cameraId = cameraId,
        )
    }
}
