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

import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.*
import android.hardware.camera2.CameraMetadata.*
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.media.ImageReader
import android.util.Range
import android.util.Size
import android.view.SurfaceHolder
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata

/**
 * Sanitized implementation of [DeviceTemplate] simulating a high-end device.
 *
 * Use this template when testing features that require advanced camera capabilities, such as manual
 * sensor control, RAW capture, or physical flash availability. It provides a baseline of "FULL"
 * hardware level and advanced capabilities. Uses only standard Android keys and safe values
 * extracted from a device dump.
 */
public object HighEndDeviceTemplate : DeviceTemplate {

    private val defaultStreamConfigurationMap: CameraStreamConfigurationMap =
        createDefaultCameraStreamConfigurationMap()

    private val frontStreamConfigurationMap: CameraStreamConfigurationMap =
        createFrontCameraStreamConfigurationMap()

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

    private fun createDefaultCameraStreamConfigurationMap(): CameraStreamConfigurationMap =
        FakeCameraStreamConfigurationMap(
            outputTable =
                listOf(
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW_SENSOR,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4000, 3000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4032, 2268),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3840, 2160),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4000, 2000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3264, 1836),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2688, 1512),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4000, 3000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4032, 2268),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3840, 2160),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4000, 2000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3264, 1836),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2688, 1512),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4000, 3000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4032, 2268),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3840, 2160),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4000, 2000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3264, 1836),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2688, 1512),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(4080, 2288),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(2032, 1536),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(2016, 1136),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW12,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4000, 3000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4032, 2268),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3840, 2160),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4000, 2000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3264, 1836),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2688, 1512),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(4000, 3000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(4032, 2268),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3840, 2160),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(4000, 2000),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(4080, 3072),
                        minDuration = 33333333L,
                    ),
                ),
            outputClassTypes =
                setOf(
                    SurfaceHolder::class.java,
                    SurfaceTexture::class.java,
                    ImageReader::class.java,
                ),
        )

    private fun createFrontCameraStreamConfigurationMap(): CameraStreamConfigurationMap =
        FakeCameraStreamConfigurationMap(
            outputTable =
                listOf(
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat(PixelFormat.RGB_888),
                        Size(550, 310),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat(PixelFormat.RGB_888),
                        Size(640, 480),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat(PixelFormat.RGB_888),
                        Size(800, 600),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat(PixelFormat.RGB_888),
                        Size(1024, 768),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW_SENSOR,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1080),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1792, 1008),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1280, 720),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1024, 576),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(640, 360),
                        minDuration = 16666666L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3264, 2448),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2560, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1920),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2560, 1280),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(2048, 1536),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1440),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG_R,
                        Size(3440, 2448),
                        minDuration = 33333333L,
                    ),
                ),
            outputClassTypes =
                setOf(
                    SurfaceHolder::class.java,
                    SurfaceTexture::class.java,
                    ImageReader::class.java,
                ),
        )

    override fun createCameraMetadata(
        cameraId: CameraId,
        lensFacing: Int?,
        characteristicsOverrides: Map<CameraCharacteristics.Key<*>, Any?>,
        metadataOverrides: Map<Metadata.Key<*>, Any?>,
        requestKeysOverrides: Set<CaptureRequest.Key<*>>,
        resultKeysOverrides: Set<CaptureResult.Key<*>>,
        extensionMetadata: Map<Int, FakeCameraExtensionMetadata>,
        physicalMetadata: Map<CameraId, FakeCameraMetadata>,
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

        val defaultStreamConfigurationMap =
            if ((lensFacing ?: LENS_FACING_BACK) == LENS_FACING_FRONT) {
                frontStreamConfigurationMap
            } else {
                this.defaultStreamConfigurationMap
            }
        val defaultMetadata =
            mapOf(CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to defaultStreamConfigurationMap)
        val allMetadata = (defaultMetadata + metadataOverrides).filterValues { it != null }

        return FakeCameraMetadata(
            characteristics = allCharacteristics,
            cameraId = cameraId,
            metadata = allMetadata,
            requestKeys =
                setOf(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.FLASH_MODE,
                ) + requestKeysOverrides,
            resultKeys =
                setOf(CaptureResult.CONTROL_AE_STATE, CaptureResult.CONTROL_AF_STATE) +
                    resultKeysOverrides,
            extensionMetadata = extensionMetadata,
            physicalMetadata = physicalMetadata,
        )
    }
}
