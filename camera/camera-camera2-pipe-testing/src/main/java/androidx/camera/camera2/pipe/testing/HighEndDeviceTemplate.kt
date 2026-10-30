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
import android.hardware.camera2.params.BlackLevelPattern
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.media.ImageReader
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import android.util.SizeF
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMultiResolutionStreamConfigurationMap
import androidx.camera.camera2.pipe.CameraStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata

/**
 * A [DeviceTemplate] that simulates a modern, high-end smartphone.
 *
 * Use this template for tests that require advanced features like manual sensor control, RAW
 * capture, or physical flash availability. It provides a baseline of "FULL" hardware level and
 * advanced capabilities.
 *
 * Sensor geometry, optics, controls, and tuning values are aligned with a Pixel 10 Pro hardware
 * profile. Treat these values as a realistic, stable test baseline, not as a hardware reference.
 */
public object HighEndDeviceTemplate : DeviceTemplate {

    private val defaultStreamConfigurationMap: CameraStreamConfigurationMap =
        createDefaultCameraStreamConfigurationMap()

    private val frontStreamConfigurationMap: CameraStreamConfigurationMap =
        createFrontCameraStreamConfigurationMap()

    private val defaultMultiResolutionStreamConfigurationMap:
        CameraMultiResolutionStreamConfigurationMap? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            createDefaultMultiResolutionStreamConfigurationMap()
        } else {
            null
        }
    }

    private val requiredCharacteristics =
        mapOf(
            INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            REQUEST_PIPELINE_MAX_DEPTH to 7.toByte(),
        )

    private val highEndDefaults =
        mapOf(
            COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES to
                intArrayOf(COLOR_CORRECTION_ABERRATION_MODE_OFF),
            CONTROL_AE_AVAILABLE_ANTIBANDING_MODES to intArrayOf(CONTROL_AE_ANTIBANDING_MODE_AUTO),
            CONTROL_AE_COMPENSATION_RANGE to Range(-24, 24),
            CONTROL_AE_COMPENSATION_STEP to Rational(1, 6),
            CONTROL_AE_LOCK_AVAILABLE to true,
            CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AF_MODE_OFF,
                    CONTROL_AF_MODE_AUTO,
                    CONTROL_AF_MODE_MACRO,
                    CONTROL_AF_MODE_CONTINUOUS_VIDEO,
                    CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    CONTROL_AF_MODE_EDOF,
                ),
            CONTROL_AVAILABLE_EFFECTS to intArrayOf(CONTROL_EFFECT_MODE_OFF),
            CONTROL_AVAILABLE_MODES to
                intArrayOf(CONTROL_MODE_OFF, CONTROL_MODE_AUTO, CONTROL_MODE_USE_SCENE_MODE),
            CONTROL_AVAILABLE_SCENE_MODES to
                intArrayOf(
                    CONTROL_SCENE_MODE_DISABLED,
                    CONTROL_SCENE_MODE_FACE_PRIORITY,
                    CONTROL_SCENE_MODE_NIGHT,
                    CONTROL_SCENE_MODE_PORTRAIT,
                    100,
                    101,
                    102,
                    103,
                    104,
                    105,
                ),
            CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                intArrayOf(
                    CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
                ),
            CONTROL_AWB_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AWB_MODE_OFF,
                    CONTROL_AWB_MODE_AUTO,
                    CONTROL_AWB_MODE_INCANDESCENT,
                    CONTROL_AWB_MODE_FLUORESCENT,
                    CONTROL_AWB_MODE_WARM_FLUORESCENT,
                    CONTROL_AWB_MODE_DAYLIGHT,
                    CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
                    CONTROL_AWB_MODE_TWILIGHT,
                    CONTROL_AWB_MODE_SHADE,
                ),
            CONTROL_AWB_LOCK_AVAILABLE to true,
            CONTROL_MAX_REGIONS_AE to 1,
            CONTROL_MAX_REGIONS_AF to 1,
            CONTROL_MAX_REGIONS_AWB to 0,
            CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE to Range(100, 1599),
            EDGE_AVAILABLE_EDGE_MODES to
                intArrayOf(EDGE_MODE_OFF, EDGE_MODE_FAST, EDGE_MODE_HIGH_QUALITY),
            HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES to
                intArrayOf(HOT_PIXEL_MODE_OFF, HOT_PIXEL_MODE_FAST, HOT_PIXEL_MODE_HIGH_QUALITY),
            JPEG_AVAILABLE_THUMBNAIL_SIZES to
                arrayOf(
                    Size(0, 0),
                    Size(176, 144),
                    Size(240, 144),
                    Size(256, 144),
                    Size(240, 160),
                    Size(256, 154),
                    Size(246, 184),
                    Size(240, 240),
                    Size(320, 216),
                    Size(320, 228),
                    Size(320, 232),
                    Size(320, 240),
                    Size(408, 308),
                ),
            LENS_INFO_AVAILABLE_FILTER_DENSITIES to floatArrayOf(0.0f),
            LENS_INFO_FOCUS_DISTANCE_CALIBRATION to
                LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE,
            LENS_POSE_TRANSLATION to floatArrayOf(0.0f, 0.0f, 0.0f),
            NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES to
                intArrayOf(
                    NOISE_REDUCTION_MODE_OFF,
                    NOISE_REDUCTION_MODE_FAST,
                    NOISE_REDUCTION_MODE_HIGH_QUALITY,
                ),
            REPROCESS_MAX_CAPTURE_STALL to 0,
            REQUEST_MAX_NUM_INPUT_STREAMS to 0,
            REQUEST_MAX_NUM_OUTPUT_PROC to 3,
            REQUEST_MAX_NUM_OUTPUT_PROC_STALLING to 2,
            REQUEST_MAX_NUM_OUTPUT_RAW to 1,
            REQUEST_PARTIAL_RESULT_COUNT to 2,
            SCALER_CROPPING_TYPE to SCALER_CROPPING_TYPE_CENTER_ONLY,
            SENSOR_AVAILABLE_TEST_PATTERN_MODES to intArrayOf(SENSOR_TEST_PATTERN_MODE_OFF, 5),
            SENSOR_INFO_COLOR_FILTER_ARRANGEMENT to SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB,
            SENSOR_INFO_LENS_SHADING_APPLIED to false,
            SENSOR_INFO_TIMESTAMP_SOURCE to SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME,
            SENSOR_REFERENCE_ILLUMINANT1 to SENSOR_REFERENCE_ILLUMINANT1_STANDARD_A,
            SENSOR_REFERENCE_ILLUMINANT2 to SENSOR_REFERENCE_ILLUMINANT1_D65.toByte(),
            SHADING_AVAILABLE_MODES to
                intArrayOf(SHADING_MODE_OFF, SHADING_MODE_FAST, SHADING_MODE_HIGH_QUALITY),
            STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES to
                intArrayOf(
                    STATISTICS_FACE_DETECT_MODE_OFF,
                    STATISTICS_FACE_DETECT_MODE_SIMPLE,
                    STATISTICS_FACE_DETECT_MODE_FULL,
                ),
            STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to booleanArrayOf(false),
            STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES to
                intArrayOf(
                    STATISTICS_LENS_SHADING_MAP_MODE_OFF,
                    STATISTICS_LENS_SHADING_MAP_MODE_ON,
                ),
            STATISTICS_INFO_MAX_FACE_COUNT to 10,
            SYNC_MAX_LATENCY to SYNC_MAX_LATENCY_PER_FRAME_CONTROL,
            TONEMAP_AVAILABLE_TONE_MAP_MODES to
                intArrayOf(
                    TONEMAP_MODE_CONTRAST_CURVE,
                    TONEMAP_MODE_FAST,
                    TONEMAP_MODE_HIGH_QUALITY,
                ),
            TONEMAP_MAX_CURVE_POINTS to 81,
        ) +
            (if (Build.VERSION.SDK_INT >= 28) {
                mapOf(
                    DISTORTION_CORRECTION_AVAILABLE_MODES to
                        intArrayOf(
                            DISTORTION_CORRECTION_MODE_OFF,
                            DISTORTION_CORRECTION_MODE_FAST,
                        ),
                    LENS_POSE_REFERENCE to LENS_POSE_REFERENCE_PRIMARY_CAMERA,
                    LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to
                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE_APPROXIMATE,
                    STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES to
                        intArrayOf(STATISTICS_OIS_DATA_MODE_OFF),
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 33) {
                mapOf(
                    REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE to DynamicRangeProfiles.HLG10
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 34) {
                mapOf(SENSOR_READOUT_TIMESTAMP to SENSOR_READOUT_TIMESTAMP_NOT_SUPPORTED)
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 35) {
                mapOf(CONTROL_LOW_LIGHT_BOOST_INFO_LUMINANCE_RANGE to Range(0.1f, 15.0f))
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 36) {
                mapOf(
                    COLOR_CORRECTION_AVAILABLE_MODES to
                        intArrayOf(
                            COLOR_CORRECTION_MODE_TRANSFORM_MATRIX,
                            COLOR_CORRECTION_MODE_FAST,
                            COLOR_CORRECTION_MODE_HIGH_QUALITY,
                        ),
                    CONTROL_AE_AVAILABLE_PRIORITY_MODES to intArrayOf(CONTROL_AE_PRIORITY_MODE_OFF),
                )
            } else {
                emptyMap()
            })

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

    private val frontFacingOverrides =
        mapOf(
            CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AE_MODE_OFF,
                    CONTROL_AE_MODE_ON,
                    CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
                ),
            CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to
                arrayOf(
                    Range(15, 15),
                    Range(15, 24),
                    Range(24, 24),
                    Range(15, 30),
                    Range(24, 30),
                    Range(30, 30),
                    Range(15, 60),
                    Range(60, 60),
                ),
            FLASH_INFO_AVAILABLE to false,
            LENS_FACING to LENS_FACING_FRONT,
            LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(2.2f),
            LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(2.713f),
            LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION to
                intArrayOf(LENS_OPTICAL_STABILIZATION_MODE_OFF),
            LENS_INFO_HYPERFOCAL_DISTANCE to 0.83691454f,
            LENS_INFO_MINIMUM_FOCUS_DISTANCE to 5.0f,
            LENS_INTRINSIC_CALIBRATION to
                floatArrayOf(1937.8573f, 1937.8573f, 1720.0f, 1224.0f, 0.0f),
            LENS_POSE_ROTATION to floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f),
            REQUEST_AVAILABLE_CAPABILITIES to
                intArrayOf(
                    REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                    REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                    REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                    REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                    REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                    REQUEST_AVAILABLE_CAPABILITIES_RAW,
                    REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA,
                    REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                ),
            SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 10.0f,
            SENSOR_CALIBRATION_TRANSFORM1 to
                ColorSpaceTransform(
                    intArrayOf(1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1)
                ),
            SENSOR_CALIBRATION_TRANSFORM2 to
                ColorSpaceTransform(
                    intArrayOf(1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1)
                ),
            SENSOR_COLOR_TRANSFORM1 to
                ColorSpaceTransform(
                    intArrayOf(
                        13027,
                        10000,
                        -6529,
                        10000,
                        436,
                        10000,
                        -6157,
                        10000,
                        16761,
                        10000,
                        7,
                        10000,
                        -449,
                        10000,
                        2245,
                        10000,
                        7009,
                        10000,
                    )
                ),
            SENSOR_COLOR_TRANSFORM2 to
                ColorSpaceTransform(
                    intArrayOf(
                        8074,
                        10000,
                        -2164,
                        10000,
                        -807,
                        10000,
                        -5767,
                        10000,
                        14627,
                        10000,
                        784,
                        10000,
                        -1561,
                        10000,
                        3647,
                        10000,
                        4497,
                        10000,
                    )
                ),
            SENSOR_FORWARD_MATRIX1 to
                ColorSpaceTransform(
                    intArrayOf(
                        6217,
                        10000,
                        2079,
                        10000,
                        1347,
                        10000,
                        2134,
                        10000,
                        7425,
                        10000,
                        442,
                        10000,
                        -817,
                        10000,
                        -5493,
                        10000,
                        14561,
                        10000,
                    )
                ),
            SENSOR_FORWARD_MATRIX2 to
                ColorSpaceTransform(
                    intArrayOf(
                        6788,
                        10000,
                        2035,
                        10000,
                        819,
                        10000,
                        2689,
                        10000,
                        7804,
                        10000,
                        -493,
                        10000,
                        117,
                        10000,
                        -4223,
                        10000,
                        12357,
                        10000,
                    )
                ),
            SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 3440, 2448),
            SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(68360L, 1000000628L),
            SENSOR_INFO_MAX_FRAME_DURATION to 1000000628L,
            SENSOR_INFO_PHYSICAL_SIZE to SizeF(4.816f, 3.4272f),
            SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(3440, 2448),
            SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to Rect(0, 0, 3440, 2448),
            SENSOR_INFO_SENSITIVITY_RANGE to Range(56, 20000),
            SENSOR_INFO_WHITE_LEVEL to 1023,
            SENSOR_MAX_ANALOG_SENSITIVITY to 1250,
            SENSOR_ORIENTATION to 270,
        ) +
            (if (Build.VERSION.SDK_INT >= 28) {
                mapOf(LENS_DISTORTION to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 30) {
                mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(0.8958334f, 10.0f))
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 33) {
                mapOf(
                    SCALER_AVAILABLE_STREAM_USE_CASES to
                        longArrayOf(
                            SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL.toLong(),
                            65537L,
                        )
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 34) {
                mapOf(SENSOR_BLACK_LEVEL_PATTERN to BlackLevelPattern(intArrayOf(64, 64, 64, 64)))
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 35) {
                mapOf(
                    FLASH_SINGLE_STRENGTH_DEFAULT_LEVEL to 1,
                    FLASH_SINGLE_STRENGTH_MAX_LEVEL to 1,
                    FLASH_TORCH_STRENGTH_DEFAULT_LEVEL to 1,
                    FLASH_TORCH_STRENGTH_MAX_LEVEL to 1,
                )
            } else {
                emptyMap()
            })

    private val backFacingOverrides =
        mapOf(
            CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(
                    CONTROL_AE_MODE_OFF,
                    CONTROL_AE_MODE_ON,
                    CONTROL_AE_MODE_ON_AUTO_FLASH,
                    CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
                ),
            CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to
                arrayOf(
                    Range(15, 15),
                    Range(24, 24),
                    Range(15, 30),
                    Range(24, 30),
                    Range(30, 30),
                    Range(15, 60),
                    Range(60, 60),
                ),
            FLASH_INFO_AVAILABLE to true,
            LENS_FACING to LENS_FACING_BACK,
            LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(1.68f),
            LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(6.9f),
            LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION to
                intArrayOf(LENS_OPTICAL_STABILIZATION_MODE_OFF, LENS_OPTICAL_STABILIZATION_MODE_ON),
            LENS_INFO_HYPERFOCAL_DISTANCE to 0.16937616f,
            LENS_INFO_MINIMUM_FOCUS_DISTANCE to 9.523809f,
            LENS_INTRINSIC_CALIBRATION to
                floatArrayOf(2853.4824f, 2853.4824f, 2034.2717f, 1542.0305f, 0.0f),
            LENS_POSE_ROTATION to floatArrayOf(-0.70710677f, 0.70710677f, 0.0f, 0.0f),
            REQUEST_AVAILABLE_CAPABILITIES to
                intArrayOf(
                    REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                    REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                    REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                    REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                    REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                    REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                    REQUEST_AVAILABLE_CAPABILITIES_RAW,
                    REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO,
                    REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA,
                    REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                ),
            SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 30.0f,
            SENSOR_CALIBRATION_TRANSFORM1 to
                ColorSpaceTransform(
                    intArrayOf(6846, 6826, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 3623, 3535)
                ),
            SENSOR_CALIBRATION_TRANSFORM2 to
                ColorSpaceTransform(
                    intArrayOf(5000, 4971, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 6553, 6377)
                ),
            SENSOR_COLOR_TRANSFORM1 to
                ColorSpaceTransform(
                    intArrayOf(
                        11190,
                        10000,
                        -4931,
                        10000,
                        -630,
                        10000,
                        -3865,
                        10000,
                        14026,
                        10000,
                        617,
                        10000,
                        -325,
                        10000,
                        2082,
                        10000,
                        7151,
                        10000,
                    )
                ),
            SENSOR_COLOR_TRANSFORM2 to
                ColorSpaceTransform(
                    intArrayOf(
                        7002,
                        10000,
                        -849,
                        10000,
                        -1246,
                        10000,
                        -5989,
                        10000,
                        15710,
                        10000,
                        -16,
                        10000,
                        -2134,
                        10000,
                        4452,
                        10000,
                        4157,
                        10000,
                    )
                ),
            SENSOR_FORWARD_MATRIX1 to
                ColorSpaceTransform(
                    intArrayOf(
                        6116,
                        10000,
                        1737,
                        10000,
                        1790,
                        10000,
                        1363,
                        10000,
                        8365,
                        10000,
                        272,
                        10000,
                        -417,
                        10000,
                        -5848,
                        10000,
                        14516,
                        10000,
                    )
                ),
            SENSOR_FORWARD_MATRIX2 to
                ColorSpaceTransform(
                    intArrayOf(
                        7144,
                        10000,
                        -9,
                        10000,
                        2507,
                        10000,
                        2755,
                        10000,
                        6223,
                        10000,
                        1022,
                        10000,
                        513,
                        10000,
                        -5060,
                        10000,
                        12799,
                        10000,
                    )
                ),
            SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4080, 3072),
            SENSOR_INFO_COLOR_FILTER_ARRANGEMENT to SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG,
            SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(26345L, 16000001084L),
            SENSOR_INFO_MAX_FRAME_DURATION to 16000001084L,
            SENSOR_INFO_PHYSICAL_SIZE to SizeF(9.792f, 7.3728f),
            SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4080, 3072),
            SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4080, 3072),
            SENSOR_INFO_SENSITIVITY_RANGE to Range(21, 5333),
            SENSOR_INFO_WHITE_LEVEL to 4095,
            SENSOR_MAX_ANALOG_SENSITIVITY to 333,
            SENSOR_ORIENTATION to 90,
        ) +
            (if (Build.VERSION.SDK_INT >= 28) {
                mapOf(
                    LENS_DISTORTION to
                        floatArrayOf(0.090790525f, -0.14565963f, 0.09545045f, 0.0f, 0.0f)
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 30) {
                mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(0.50783783f, 30.0f))
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 31) {
                mapOf(
                    LENS_DISTORTION_MAXIMUM_RESOLUTION to
                        floatArrayOf(0.090790525f, -0.14565963f, 0.09545045f, 0.0f, 0.0f),
                    LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION to
                        floatArrayOf(5706.965f, 5706.965f, 4068.5435f, 3084.061f, 0.0f),
                    SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to Rect(0, 0, 8160, 6144),
                    SENSOR_INFO_BINNING_FACTOR to Size(2, 2),
                    SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION to Size(8160, 6144),
                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                        Rect(0, 0, 8160, 6144),
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 33) {
                mapOf(
                    FLASH_INFO_STRENGTH_DEFAULT_LEVEL to 21,
                    FLASH_INFO_STRENGTH_MAXIMUM_LEVEL to 21,
                    SCALER_AVAILABLE_STREAM_USE_CASES to
                        longArrayOf(
                            SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong(),
                            SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL.toLong(),
                            65536L,
                            65537L,
                        ),
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 34) {
                mapOf(
                    SENSOR_BLACK_LEVEL_PATTERN to BlackLevelPattern(intArrayOf(256, 256, 256, 256))
                )
            } else {
                emptyMap()
            }) +
            (if (Build.VERSION.SDK_INT >= 35) {
                mapOf(
                    FLASH_SINGLE_STRENGTH_DEFAULT_LEVEL to 51,
                    FLASH_SINGLE_STRENGTH_MAX_LEVEL to 51,
                    FLASH_TORCH_STRENGTH_DEFAULT_LEVEL to 23,
                    FLASH_TORCH_STRENGTH_MAX_LEVEL to 23,
                )
            } else {
                emptyMap()
            })

    private val physicalExtraJpegOutputs =
        listOf(
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1600, 1200),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1920, 960),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1792, 1008),
                minDuration = 16666666L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1440, 1080),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1344, 1008),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1280, 960),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1080, 1080),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1280, 720),
                minDuration = 16666666L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1024, 768),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(1024, 576),
                minDuration = 16666666L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(800, 600),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(720, 480),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(640, 480),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(640, 360),
                minDuration = 16666666L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(352, 288),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(320, 240),
                minDuration = 33333333L,
            ),
            FakeCameraStreamConfigurationMap.OutputTableEntry(
                StreamFormat.JPEG,
                Size(176, 144),
                minDuration = 33333333L,
            ),
        )

    private val physical3StreamConfigurationMap by lazy {
        createPhysicalStreamConfigurationMap(
            baseMap = defaultStreamConfigurationMap,
            overrides =
                physicalExtraJpegOutputs +
                    listOf(
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW_SENSOR,
                            Size(4032, 3024),
                            minDuration = 33333333L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW_SENSOR,
                            Size(4032, 2272),
                            minDuration = 16666666L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW_SENSOR,
                            Size(2016, 1512),
                            minDuration = 16666666L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW_SENSOR,
                            Size(1920, 1080),
                            minDuration = 16666666L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW10,
                            Size(4032, 3024),
                            minDuration = 33333333L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW10,
                            Size(4032, 2272),
                            minDuration = 16666666L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW10,
                            Size(2016, 1512),
                            minDuration = 16666666L,
                        ),
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            StreamFormat.RAW10,
                            Size(1920, 1080),
                            minDuration = 16666666L,
                        ),
                    ),
            excludedSizesByFormat =
                mapOf(
                    StreamFormat.RAW_SENSOR to setOf(Size(4080, 3072)),
                    StreamFormat.JPEG to setOf(Size(4080, 3072)),
                    StreamFormat.PRIVATE to setOf(Size(4080, 3072)),
                    StreamFormat.YUV_420_888 to setOf(Size(4080, 3072)),
                    StreamFormat.RAW10 to
                        setOf(
                            Size(4080, 3072),
                            Size(4080, 2288),
                            Size(2032, 1536),
                            Size(2016, 1136),
                        ),
                    StreamFormat.RAW12 to setOf(Size(4080, 3072)),
                    StreamFormat.YCBCR_P010 to setOf(Size(4080, 3072)),
                    StreamFormat.JPEG_R to setOf(Size(4080, 3072)),
                ),
        )
    }

    private val physical5StreamConfigurationMap by lazy {
        createPhysicalStreamConfigurationMap(
            baseMap = defaultStreamConfigurationMap,
            overrides =
                listOf(
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(4032, 2268),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3840, 2160),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1600, 1200),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1920, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1440, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1344, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1280, 960),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1080, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1024, 768),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(800, 600),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(720, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(640, 480),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(352, 288),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(320, 240),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.JPEG,
                        Size(176, 144),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(4032, 2268),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3840, 2160),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.PRIVATE,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(4032, 2268),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3840, 2160),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YUV_420_888,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(4032, 2268),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3840, 2160),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(3264, 1836),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(2688, 1512),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1920, 1080),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1792, 1008),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1280, 720),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(1024, 576),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.YCBCR_P010,
                        Size(640, 360),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW_DEPTH10,
                        Size(2032, 384),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW_DEPTH10,
                        Size(4080, 768),
                        minDuration = 33333333L,
                    ),
                ),
            excludedSizesByFormat =
                mapOf(
                    StreamFormat.RAW10 to
                        setOf(Size(4080, 2288), Size(2032, 1536), Size(2016, 1136)),
                    StreamFormat.RAW12 to setOf(Size(4080, 3072)),
                ),
        )
    }

    private val physical9StreamConfigurationMap by lazy {
        createPhysicalStreamConfigurationMap(
            baseMap = physical5StreamConfigurationMap,
            overrides =
                listOf(
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW_SENSOR,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                    FakeCameraStreamConfigurationMap.OutputTableEntry(
                        StreamFormat.RAW10,
                        Size(4032, 3024),
                        minDuration = 33333333L,
                    ),
                ),
            excludedSizesByFormat =
                mapOf(
                    StreamFormat.RAW_SENSOR to setOf(Size(4080, 3072)),
                    StreamFormat.JPEG to setOf(Size(4080, 3072)),
                    StreamFormat.PRIVATE to setOf(Size(4080, 3072)),
                    StreamFormat.YUV_420_888 to setOf(Size(4080, 3072)),
                    StreamFormat.RAW10 to setOf(Size(4080, 3072)),
                    StreamFormat.YCBCR_P010 to setOf(Size(4080, 3072)),
                    StreamFormat.RAW_DEPTH10 to setOf(Size(2032, 384), Size(4080, 768)),
                    StreamFormat.JPEG_R to setOf(Size(4080, 3072)),
                ),
        )
    }

    private val frontPhysicalMetadata by lazy {
        mapOf(
            CameraId("7") to
                FakeCameraMetadata(
                    cameraId = CameraId("7"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                frontFacingOverrides +
                                mapOf(
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    LENS_INTRINSIC_CALIBRATION to
                                        floatArrayOf(
                                            1937.8573f,
                                            1937.8573f,
                                            1920.0f,
                                            1368.0f,
                                            0.0f,
                                        ),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7734,
                                                7520,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                3682,
                                                3721,
                                            )
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                5283,
                                                5107,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                6709,
                                                6729,
                                            )
                                        ),
                                    SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 3840, 2736),
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(68360L, 1000000813L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 1000000813L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(5.376f, 3.8304f),
                                    SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(3840, 2736),
                                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                                        Rect(0, 0, 3840, 2736),
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(
                                        LENS_DISTORTION to
                                            floatArrayOf(
                                                0.03838266f,
                                                0.00781571f,
                                                -0.0326626f,
                                                0.0f,
                                                0.0f,
                                            ),
                                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 10.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 31) {
                                    mapOf(SCALER_DEFAULT_SECURE_IMAGE_SIZE to Size(1600, 1200))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                createPhysicalStreamConfigurationMap(
                                    baseMap = frontStreamConfigurationMap,
                                    overrides =
                                        physicalExtraJpegOutputs +
                                            listOf(
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW_SENSOR,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW_SENSOR,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW_SENSOR,
                                                    Size(1920, 1368),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW_SENSOR,
                                                    Size(1920, 1080),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG,
                                                    Size(3264, 1836),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG,
                                                    Size(2688, 1512),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.PRIVATE,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.PRIVATE,
                                                    Size(3264, 1836),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.PRIVATE,
                                                    Size(2688, 1512),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.PRIVATE,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YUV_420_888,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YUV_420_888,
                                                    Size(3264, 1836),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YUV_420_888,
                                                    Size(2688, 1512),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YUV_420_888,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW10,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW10,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW10,
                                                    Size(1920, 1368),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW10,
                                                    Size(1920, 1080),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YCBCR_P010,
                                                    Size(3840, 2160),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YCBCR_P010,
                                                    Size(3264, 1836),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YCBCR_P010,
                                                    Size(2688, 1512),
                                                    minDuration = 16666666L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.YCBCR_P010,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.RAW_DEPTH10,
                                                    Size(3840, 684),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG_R,
                                                    Size(3840, 2160),
                                                    minDuration = 33333333L,
                                                ),
                                                FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                    StreamFormat.JPEG_R,
                                                    Size(3840, 2736),
                                                    minDuration = 33333333L,
                                                ),
                                            ),
                                    excludedSizesByFormat =
                                        mapOf(
                                            StreamFormat.RAW_SENSOR to setOf(Size(3440, 2448)),
                                            StreamFormat.JPEG to setOf(Size(3440, 2448)),
                                            StreamFormat.PRIVATE to setOf(Size(3440, 2448)),
                                            StreamFormat.YUV_420_888 to setOf(Size(3440, 2448)),
                                            StreamFormat.RAW10 to setOf(Size(3440, 2448)),
                                            StreamFormat.YCBCR_P010 to setOf(Size(3440, 2448)),
                                            StreamFormat.JPEG_R to setOf(Size(3440, 2448)),
                                        ),
                                )
                        ),
                ),
            CameraId("8") to
                FakeCameraMetadata(
                    cameraId = CameraId("8"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                frontFacingOverrides +
                                mapOf(
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_AVAILABLE_TEST_PATTERN_MODES to
                                        intArrayOf(
                                            SENSOR_TEST_PATTERN_MODE_OFF,
                                            SENSOR_TEST_PATTERN_MODE_SOLID_COLOR,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY,
                                        ),
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null)
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                createPhysicalStreamConfigurationMap(
                                    baseMap = frontStreamConfigurationMap,
                                    overrides =
                                        physicalExtraJpegOutputs +
                                            FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                StreamFormat.RAW_DEPTH10,
                                                Size(3840, 684),
                                                minDuration = 33333333L,
                                            ),
                                )
                        ),
                ),
        )
    }

    private val backPhysicalMetadata by lazy {
        mapOf(
            CameraId("2") to
                FakeCameraMetadata(
                    cameraId = CameraId("2"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE to Range(100, 3199),
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_AVAILABLE_TEST_PATTERN_MODES to
                                        intArrayOf(
                                            SENSOR_TEST_PATTERN_MODE_OFF,
                                            SENSOR_TEST_PATTERN_MODE_SOLID_COLOR,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY,
                                            SENSOR_TEST_PATTERN_MODE_PN9,
                                        ),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null)
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65536L,
                                                65537L,
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                createPhysicalStreamConfigurationMap(
                                    baseMap = defaultStreamConfigurationMap,
                                    overrides =
                                        physicalExtraJpegOutputs +
                                            FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                StreamFormat.RAW_DEPTH10,
                                                Size(4080, 768),
                                                minDuration = 33333333L,
                                            ),
                                )
                        ),
                ),
            CameraId("3") to
                FakeCameraMetadata(
                    cameraId = CameraId("3"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(1.7f),
                                    LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(2.02f),
                                    LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION to
                                        intArrayOf(LENS_OPTICAL_STABILIZATION_MODE_OFF),
                                    LENS_INFO_HYPERFOCAL_DISTANCE to 1.1665524f,
                                    LENS_INFO_MINIMUM_FOCUS_DISTANCE to 50.0f,
                                    LENS_INTRINSIC_CALIBRATION to
                                        floatArrayOf(
                                            1410.9261f,
                                            1410.9261f,
                                            2029.2601f,
                                            1513.0553f,
                                            0.0f,
                                        ),
                                    LENS_POSE_ROTATION to
                                        floatArrayOf(
                                            -0.7065638f,
                                            0.70764244f,
                                            -0.00020584058f,
                                            -0.0031233004f,
                                        ),
                                    LENS_POSE_TRANSLATION to
                                        floatArrayOf(-0.015000385f, 0.00056129985f, 0.00025831343f),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_CALIBRATION_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7236,
                                                7188,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                3711,
                                                3721,
                                            )
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                4883,
                                                4834,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                6572,
                                                6484,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                10015,
                                                10000,
                                                -4012,
                                                10000,
                                                1277,
                                                10000,
                                                -5797,
                                                10000,
                                                15203,
                                                10000,
                                                3272,
                                                10000,
                                                -785,
                                                10000,
                                                2282,
                                                10000,
                                                7962,
                                                10000,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7081,
                                                10000,
                                                -1492,
                                                10000,
                                                -813,
                                                10000,
                                                -6046,
                                                10000,
                                                14550,
                                                10000,
                                                1099,
                                                10000,
                                                -2062,
                                                10000,
                                                3978,
                                                10000,
                                                4568,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7391,
                                                10000,
                                                1889,
                                                10000,
                                                363,
                                                10000,
                                                2698,
                                                10000,
                                                8598,
                                                10000,
                                                -1295,
                                                10000,
                                                -82,
                                                10000,
                                                -5439,
                                                10000,
                                                13772,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7165,
                                                10000,
                                                1439,
                                                10000,
                                                1039,
                                                10000,
                                                2943,
                                                10000,
                                                7786,
                                                10000,
                                                -729,
                                                10000,
                                                470,
                                                10000,
                                                -4644,
                                                10000,
                                                12426,
                                                10000,
                                            )
                                        ),
                                    SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_COLOR_FILTER_ARRANGEMENT to
                                        SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB,
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(68360L, 16000000852L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 16000000852L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(5.6448f, 4.2336f),
                                    SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4032, 3024),
                                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                                        Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_SENSITIVITY_RANGE to Range(52, 2310),
                                    SENSOR_INFO_WHITE_LEVEL to 1023,
                                    SENSOR_MAX_ANALOG_SENSITIVITY to 1155,
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(
                                        LENS_DISTORTION to
                                            floatArrayOf(
                                                0.0012464672f,
                                                -0.0012614542f,
                                                0.00045113004f,
                                                -0.00041404009f,
                                                -0.00037766283f,
                                            ),
                                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 31) {
                                    mapOf(
                                        LENS_DISTORTION_MAXIMUM_RESOLUTION to null,
                                        LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_BINNING_FACTOR to null,
                                        SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65537L,
                                                SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW
                                                    .toLong(),
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 34) {
                                    mapOf(
                                        SENSOR_BLACK_LEVEL_PATTERN to
                                            BlackLevelPattern(intArrayOf(64, 64, 64, 64))
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                physical3StreamConfigurationMap
                        ),
                ),
            CameraId("4") to
                FakeCameraMetadata(
                    cameraId = CameraId("4"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(2.8f),
                                    LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(17.906f),
                                    LENS_INFO_HYPERFOCAL_DISTANCE to 0.024452256f,
                                    LENS_INFO_MINIMUM_FOCUS_DISTANCE to 3.3333333f,
                                    LENS_INTRINSIC_CALIBRATION to
                                        floatArrayOf(
                                            12539.53f,
                                            12539.53f,
                                            2033.3752f,
                                            1508.5767f,
                                            0.0f,
                                        ),
                                    LENS_POSE_ROTATION to
                                        floatArrayOf(
                                            -0.68077904f,
                                            0.73109436f,
                                            -0.0021147707f,
                                            -0.045127213f,
                                        ),
                                    LENS_POSE_TRANSLATION to
                                        floatArrayOf(-0.028417222f, -7.121015e-05f, 0.011595403f),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_CALIBRATION_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                8125,
                                                8154,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                3457,
                                                3477,
                                            )
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                5684,
                                                5674,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                5947,
                                                5977,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                10819,
                                                10000,
                                                -4334,
                                                10000,
                                                1380,
                                                10000,
                                                -5797,
                                                10000,
                                                15203,
                                                10000,
                                                3272,
                                                10000,
                                                -766,
                                                10000,
                                                2228,
                                                10000,
                                                7774,
                                                10000,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7639,
                                                10000,
                                                -1609,
                                                10000,
                                                -877,
                                                10000,
                                                -6046,
                                                10000,
                                                14550,
                                                10000,
                                                1099,
                                                10000,
                                                -1987,
                                                10000,
                                                3834,
                                                10000,
                                                4402,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7391,
                                                10000,
                                                1889,
                                                10000,
                                                363,
                                                10000,
                                                2698,
                                                10000,
                                                8598,
                                                10000,
                                                -1295,
                                                10000,
                                                -83,
                                                10000,
                                                -5439,
                                                10000,
                                                13773,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7166,
                                                10000,
                                                1438,
                                                10000,
                                                1039,
                                                10000,
                                                2943,
                                                10000,
                                                7786,
                                                10000,
                                                -730,
                                                10000,
                                                469,
                                                10000,
                                                -4645,
                                                10000,
                                                12427,
                                                10000,
                                            )
                                        ),
                                    SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(68360L, 16000000852L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 16000000852L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(5.6448f, 4.2336f),
                                    SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4032, 3024),
                                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                                        Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_SENSITIVITY_RANGE to Range(47, 16897),
                                    SENSOR_INFO_WHITE_LEVEL to 1023,
                                    SENSOR_MAX_ANALOG_SENSITIVITY to 1056,
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(
                                        LENS_DISTORTION to
                                            floatArrayOf(
                                                0.49675465f,
                                                -3.2955956f,
                                                26.93868f,
                                                0.0f,
                                                0.0f,
                                            ),
                                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 31) {
                                    mapOf(
                                        LENS_DISTORTION_MAXIMUM_RESOLUTION to null,
                                        LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_BINNING_FACTOR to null,
                                        SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION to null,
                                        SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65537L,
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 34) {
                                    mapOf(
                                        SENSOR_BLACK_LEVEL_PATTERN to
                                            BlackLevelPattern(intArrayOf(64, 64, 64, 64))
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                createPhysicalStreamConfigurationMap(
                                    baseMap = physical3StreamConfigurationMap,
                                    overrides =
                                        listOf(
                                            FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                StreamFormat.RAW_DEPTH10,
                                                Size(4032, 756),
                                                minDuration = 33333333L,
                                            )
                                        ),
                                )
                        ),
                ),
            CameraId("5") to
                FakeCameraMetadata(
                    cameraId = CameraId("5"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE to Range(100, 3199),
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    LENS_INFO_HYPERFOCAL_DISTANCE to 0.08468808f,
                                    LENS_POSE_ROTATION to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_AVAILABLE_TEST_PATTERN_MODES to
                                        intArrayOf(
                                            SENSOR_TEST_PATTERN_MODE_OFF,
                                            SENSOR_TEST_PATTERN_MODE_SOLID_COLOR,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS,
                                            SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY,
                                            SENSOR_TEST_PATTERN_MODE_PN9,
                                        ),
                                    SENSOR_COLOR_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                11130,
                                                10000,
                                                -4904,
                                                10000,
                                                -627,
                                                10000,
                                                -3865,
                                                10000,
                                                14026,
                                                10000,
                                                617,
                                                10000,
                                                -324,
                                                10000,
                                                2078,
                                                10000,
                                                7137,
                                                10000,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                6993,
                                                10000,
                                                -848,
                                                10000,
                                                -1244,
                                                10000,
                                                -5989,
                                                10000,
                                                15710,
                                                10000,
                                                -16,
                                                10000,
                                                -2117,
                                                10000,
                                                4416,
                                                10000,
                                                4123,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                6116,
                                                10000,
                                                1736,
                                                10000,
                                                1791,
                                                10000,
                                                1363,
                                                10000,
                                                8365,
                                                10000,
                                                272,
                                                10000,
                                                -418,
                                                10000,
                                                -5849,
                                                10000,
                                                14518,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7145,
                                                10000,
                                                -8,
                                                10000,
                                                2506,
                                                10000,
                                                2755,
                                                10000,
                                                6223,
                                                10000,
                                                1021,
                                                10000,
                                                513,
                                                10000,
                                                -5061,
                                                10000,
                                                12799,
                                                10000,
                                            )
                                        ),
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(27253L, 16000001816L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 16000001816L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(4.896f, 3.6864f),
                                    SENSOR_INFO_SENSITIVITY_RANGE to Range(21, 10666),
                                    SENSOR_INFO_WHITE_LEVEL to 1023,
                                    SENSOR_MAX_ANALOG_SENSITIVITY to 666,
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null)
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65537L,
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 34) {
                                    mapOf(
                                        SENSOR_BLACK_LEVEL_PATTERN to
                                            BlackLevelPattern(intArrayOf(64, 64, 64, 64))
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                physical5StreamConfigurationMap
                        ),
                ),
            CameraId("6") to
                FakeCameraMetadata(
                    cameraId = CameraId("6"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    DEPTH_DEPTH_IS_EXCLUSIVE to false,
                                    LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(2.8f),
                                    LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(17.906f),
                                    LENS_INFO_HYPERFOCAL_DISTANCE to 0.012226128f,
                                    LENS_INFO_MINIMUM_FOCUS_DISTANCE to 3.3333333f,
                                    LENS_INTRINSIC_CALIBRATION to
                                        floatArrayOf(
                                            12539.53f,
                                            12539.53f,
                                            2033.3752f,
                                            1508.5767f,
                                            0.0f,
                                        ),
                                    LENS_POSE_ROTATION to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 10.0f,
                                    SENSOR_CALIBRATION_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                8125,
                                                8154,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                3457,
                                                3477,
                                            )
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                5684,
                                                5674,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                5947,
                                                5977,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                10800,
                                                10000,
                                                -4326,
                                                10000,
                                                1378,
                                                10000,
                                                -5797,
                                                10000,
                                                15203,
                                                10000,
                                                3272,
                                                10000,
                                                -766,
                                                10000,
                                                2229,
                                                10000,
                                                7777,
                                                10000,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7610,
                                                10000,
                                                -1603,
                                                10000,
                                                -873,
                                                10000,
                                                -6046,
                                                10000,
                                                14550,
                                                10000,
                                                1099,
                                                10000,
                                                -1981,
                                                10000,
                                                3821,
                                                10000,
                                                4388,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7391,
                                                10000,
                                                1889,
                                                10000,
                                                363,
                                                10000,
                                                2698,
                                                10000,
                                                8598,
                                                10000,
                                                -1296,
                                                10000,
                                                -84,
                                                10000,
                                                -5440,
                                                10000,
                                                13775,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7167,
                                                10000,
                                                1439,
                                                10000,
                                                1037,
                                                10000,
                                                2944,
                                                10000,
                                                7786,
                                                10000,
                                                -730,
                                                10000,
                                                470,
                                                10000,
                                                -4644,
                                                10000,
                                                12425,
                                                10000,
                                            )
                                        ),
                                    SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(122559L, 16000005957L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 16000005957L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(2.8224f, 2.1168f),
                                    SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4032, 3024),
                                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                                        Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_SENSITIVITY_RANGE to Range(33, 4224),
                                    SENSOR_INFO_WHITE_LEVEL to 1023,
                                    SENSOR_MAX_ANALOG_SENSITIVITY to 264,
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(
                                        LENS_DISTORTION to
                                            floatArrayOf(
                                                0.49675465f,
                                                -3.2955956f,
                                                26.93868f,
                                                0.0f,
                                                0.0f,
                                            ),
                                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null,
                                        STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES to
                                            intArrayOf(
                                                STATISTICS_OIS_DATA_MODE_OFF,
                                                STATISTICS_OIS_DATA_MODE_ON,
                                            ),
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 10.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 31) {
                                    mapOf(
                                        LENS_DISTORTION_MAXIMUM_RESOLUTION to
                                            floatArrayOf(
                                                0.49675465f,
                                                -3.2955956f,
                                                26.93868f,
                                                0.0f,
                                                0.0f,
                                            ),
                                        LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION to
                                            floatArrayOf(
                                                25079.06f,
                                                25079.06f,
                                                4066.7505f,
                                                3017.1533f,
                                                0.0f,
                                            ),
                                        SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Rect(0, 0, 8064, 6048),
                                        SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Size(8064, 6048),
                                        SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Rect(0, 0, 8064, 6048),
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65537L,
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 34) {
                                    mapOf(
                                        SENSOR_BLACK_LEVEL_PATTERN to
                                            BlackLevelPattern(intArrayOf(64, 64, 64, 64))
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                createPhysicalStreamConfigurationMap(
                                    baseMap = physical9StreamConfigurationMap,
                                    overrides =
                                        listOf(
                                            FakeCameraStreamConfigurationMap.OutputTableEntry(
                                                StreamFormat.RAW_DEPTH10,
                                                Size(2016, 1512),
                                                minDuration = 33333333L,
                                            )
                                        ),
                                )
                        ),
                ),
            CameraId("9") to
                FakeCameraMetadata(
                    cameraId = CameraId("9"),
                    characteristics =
                        (requiredCharacteristics +
                                highEndDefaults +
                                backFacingOverrides +
                                mapOf(
                                    LENS_INFO_AVAILABLE_APERTURES to floatArrayOf(1.7f),
                                    LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(2.02f),
                                    LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION to
                                        intArrayOf(LENS_OPTICAL_STABILIZATION_MODE_OFF),
                                    LENS_INFO_HYPERFOCAL_DISTANCE to 0.5832762f,
                                    LENS_INFO_MINIMUM_FOCUS_DISTANCE to 50.0f,
                                    LENS_INTRINSIC_CALIBRATION to
                                        floatArrayOf(
                                            1410.9261f,
                                            1410.9261f,
                                            2029.2601f,
                                            1513.0553f,
                                            0.0f,
                                        ),
                                    LENS_POSE_ROTATION to
                                        floatArrayOf(
                                            -0.7065638f,
                                            0.70764244f,
                                            -0.00020584058f,
                                            -0.0031233004f,
                                        ),
                                    LENS_POSE_TRANSLATION to
                                        floatArrayOf(-0.015000385f, 0.00056129985f, 0.00025831343f),
                                    REQUEST_AVAILABLE_CAPABILITIES to
                                        intArrayOf(
                                            REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                                            REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS,
                                            REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
                                            REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE,
                                            REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE,
                                            REQUEST_AVAILABLE_CAPABILITIES_RAW,
                                            REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO,
                                            REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT,
                                        ),
                                    SCALER_AVAILABLE_MAX_DIGITAL_ZOOM to 8.0f,
                                    SENSOR_CALIBRATION_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7236,
                                                7188,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                3711,
                                                3721,
                                            )
                                        ),
                                    SENSOR_CALIBRATION_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                4883,
                                                4834,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                1,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                0,
                                                1,
                                                6572,
                                                6484,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                10015,
                                                10000,
                                                -4012,
                                                10000,
                                                1277,
                                                10000,
                                                -5797,
                                                10000,
                                                15203,
                                                10000,
                                                3272,
                                                10000,
                                                -785,
                                                10000,
                                                2282,
                                                10000,
                                                7962,
                                                10000,
                                            )
                                        ),
                                    SENSOR_COLOR_TRANSFORM2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7081,
                                                10000,
                                                -1492,
                                                10000,
                                                -813,
                                                10000,
                                                -6046,
                                                10000,
                                                14550,
                                                10000,
                                                1099,
                                                10000,
                                                -2062,
                                                10000,
                                                3978,
                                                10000,
                                                4568,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX1 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7391,
                                                10000,
                                                1889,
                                                10000,
                                                363,
                                                10000,
                                                2698,
                                                10000,
                                                8598,
                                                10000,
                                                -1295,
                                                10000,
                                                -82,
                                                10000,
                                                -5439,
                                                10000,
                                                13772,
                                                10000,
                                            )
                                        ),
                                    SENSOR_FORWARD_MATRIX2 to
                                        ColorSpaceTransform(
                                            intArrayOf(
                                                7165,
                                                10000,
                                                1439,
                                                10000,
                                                1039,
                                                10000,
                                                2943,
                                                10000,
                                                7786,
                                                10000,
                                                -729,
                                                10000,
                                                470,
                                                10000,
                                                -4644,
                                                10000,
                                                12426,
                                                10000,
                                            )
                                        ),
                                    SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_COLOR_FILTER_ARRANGEMENT to
                                        SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB,
                                    SENSOR_INFO_EXPOSURE_TIME_RANGE to Range(122559L, 16000005957L),
                                    SENSOR_INFO_MAX_FRAME_DURATION to 16000005957L,
                                    SENSOR_INFO_PHYSICAL_SIZE to SizeF(2.8224f, 2.1168f),
                                    SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(4032, 3024),
                                    SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                                        Rect(0, 0, 4032, 3024),
                                    SENSOR_INFO_SENSITIVITY_RANGE to Range(36, 4620),
                                    SENSOR_INFO_WHITE_LEVEL to 1023,
                                    SENSOR_MAX_ANALOG_SENSITIVITY to 288,
                                    STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES to
                                        booleanArrayOf(false, true),
                                ) +
                                (if (Build.VERSION.SDK_INT >= 28) {
                                    mapOf(
                                        LENS_DISTORTION to
                                            floatArrayOf(
                                                0.0012464672f,
                                                -0.0012614542f,
                                                0.00045113004f,
                                                -0.00041404009f,
                                                -0.00037766283f,
                                            ),
                                        LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE to null,
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 30) {
                                    mapOf(CONTROL_ZOOM_RATIO_RANGE to Range(1.0f, 8.0f))
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 31) {
                                    mapOf(
                                        LENS_DISTORTION_MAXIMUM_RESOLUTION to
                                            floatArrayOf(
                                                0.0012464672f,
                                                -0.0012614542f,
                                                0.00045113004f,
                                                -0.00041404009f,
                                                -0.00037766283f,
                                            ),
                                        LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION to
                                            floatArrayOf(
                                                2821.8523f,
                                                2821.8523f,
                                                4058.5203f,
                                                3026.1106f,
                                                0.0f,
                                            ),
                                        SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Rect(0, 0, 8064, 6048),
                                        SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Size(8064, 6048),
                                        SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION to
                                            Rect(0, 0, 8064, 6048),
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 33) {
                                    mapOf(
                                        SCALER_AVAILABLE_STREAM_USE_CASES to
                                            longArrayOf(
                                                SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                                                    .toLong(),
                                                SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL
                                                    .toLong(),
                                                65537L,
                                            )
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 34) {
                                    mapOf(
                                        SENSOR_BLACK_LEVEL_PATTERN to
                                            BlackLevelPattern(intArrayOf(64, 64, 64, 64))
                                    )
                                } else {
                                    emptyMap()
                                }) +
                                (if (Build.VERSION.SDK_INT >= 36) {
                                    mapOf(CONTROL_AE_AVAILABLE_PRIORITY_MODES to null)
                                } else {
                                    emptyMap()
                                }))
                            .filterValues { it != null },
                    metadata =
                        mapOf(
                            CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to
                                physical9StreamConfigurationMap
                        ),
                ),
        )
    }

    private val defaultPhysicalRequestKeys =
        setOfNotNull(
            CaptureRequest.COLOR_CORRECTION_GAINS,
            CaptureRequest.COLOR_CORRECTION_TRANSFORM,
            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
            if (Build.VERSION.SDK_INT >= 30) CaptureRequest.CONTROL_ZOOM_RATIO else null,
            CaptureRequest.LENS_FOCUS_DISTANCE,
            CaptureRequest.SENSOR_EXPOSURE_TIME,
            CaptureRequest.SENSOR_SENSITIVITY,
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
        val resolvedLensFacing =
            (characteristicsOverrides[LENS_FACING] as? Int) ?: lensFacing ?: LENS_FACING_BACK
        val lensFacingOverrides =
            if (resolvedLensFacing == LENS_FACING_FRONT) {
                frontFacingOverrides
            } else {
                backFacingOverrides
            }

        // Combine them in order of priority: Required < Default < FacingOverrides < Overrides
        val allCharacteristics =
            (requiredCharacteristics +
                    highEndDefaults +
                    lensFacingOverrides +
                    characteristicsOverrides)
                .filterValues { it != null }

        val defaultStreamConfigurationMap =
            if (resolvedLensFacing == LENS_FACING_FRONT) {
                frontStreamConfigurationMap
            } else {
                this.defaultStreamConfigurationMap
            }
        val isLogicalMultiCamera =
            (allCharacteristics[REQUEST_AVAILABLE_CAPABILITIES] as? IntArray)?.contains(
                REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            ) == true
        val defaultMultiResolutionStreamConfigurationMap =
            if (resolvedLensFacing == LENS_FACING_FRONT || !isLogicalMultiCamera) {
                null
            } else {
                this.defaultMultiResolutionStreamConfigurationMap
            }
        val defaultMetadata =
            mapOf(
                CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP to defaultStreamConfigurationMap,
                CameraMetadata.CAMERA_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP to
                    defaultMultiResolutionStreamConfigurationMap,
            )
        val allMetadata = (defaultMetadata + metadataOverrides).filterValues { it != null }

        return FakeCameraMetadata(
            characteristics = allCharacteristics,
            cameraId = cameraId,
            metadata = allMetadata,
            requestKeys =
                setOfNotNull(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.FLASH_MODE,
                ) + requestKeysOverrides,
            resultKeys =
                setOfNotNull(CaptureResult.CONTROL_AE_STATE, CaptureResult.CONTROL_AF_STATE) +
                    resultKeysOverrides,
            extensionMetadata = extensionMetadata,
            physicalMetadata =
                physicalMetadata.ifEmpty {
                    if (!isLogicalMultiCamera) {
                        emptyMap()
                    } else if (resolvedLensFacing == LENS_FACING_FRONT) {
                        frontPhysicalMetadata
                    } else {
                        backPhysicalMetadata
                    }
                },
            physicalRequestKeys =
                if (!isLogicalMultiCamera) {
                    emptySet()
                } else {
                    defaultPhysicalRequestKeys
                },
        )
    }

    private fun createPhysicalStreamConfigurationMap(
        baseMap: CameraStreamConfigurationMap,
        overrides: List<FakeCameraStreamConfigurationMap.OutputTableEntry> = emptyList(),
        excludedSizesByFormat: Map<StreamFormat, Set<Size>> = emptyMap(),
    ): CameraStreamConfigurationMap {
        val baseEntries =
            baseMap.getOutputFormats().flatMap { format ->
                val excludedSizes = excludedSizesByFormat[format].orEmpty()
                baseMap
                    .getOutputSizes(format)
                    .filterNot { it in excludedSizes }
                    .map { size ->
                        FakeCameraStreamConfigurationMap.OutputTableEntry(
                            format = format,
                            size = size,
                            minDuration = baseMap.getOutputMinFrameDuration(format, size),
                            stallDuration = baseMap.getOutputStallDuration(format, size),
                        )
                    }
            }
        val overrideByFormatAndSize = overrides.associateBy { it.format to it.size }
        val mergedEntries =
            baseEntries.map { overrideByFormatAndSize[it.format to it.size] ?: it } +
                overrides.filterNot { override ->
                    baseEntries.any { it.format == override.format && it.size == override.size }
                }
        return FakeCameraStreamConfigurationMap(
            outputTable = mergedEntries,
            outputClassTypes =
                setOf(
                    SurfaceHolder::class.java,
                    SurfaceTexture::class.java,
                    ImageReader::class.java,
                ),
        )
    }

    @RequiresApi(31)
    private fun createDefaultMultiResolutionStreamConfigurationMap():
        CameraMultiResolutionStreamConfigurationMap =
        FakeCameraMultiResolutionStreamConfigurationMap(
            outputFormats = listOf(StreamFormat.RAW_SENSOR, StreamFormat.RAW10),
            outputMultiResStreamFormatsByFormat =
                mapOf(
                    StreamFormat.RAW_SENSOR to
                        listOf(
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4080,
                                /* streamHeight= */ 3072,
                                /* physicalCameraId= */ "2",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4032,
                                /* streamHeight= */ 3024,
                                /* physicalCameraId= */ "3",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4032,
                                /* streamHeight= */ 3024,
                                /* physicalCameraId= */ "4",
                            ),
                        ),
                    StreamFormat.RAW10 to
                        listOf(
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4080,
                                /* streamHeight= */ 3072,
                                /* physicalCameraId= */ "2",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 8160,
                                /* streamHeight= */ 6144,
                                /* physicalCameraId= */ "2",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4032,
                                /* streamHeight= */ 3024,
                                /* physicalCameraId= */ "3",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 4032,
                                /* streamHeight= */ 3024,
                                /* physicalCameraId= */ "4",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 8064,
                                /* streamHeight= */ 6048,
                                /* physicalCameraId= */ "6",
                            ),
                            MultiResolutionStreamInfo(
                                /* streamWidth= */ 8064,
                                /* streamHeight= */ 6048,
                                /* physicalCameraId= */ "9",
                            ),
                        ),
                ),
        )
}
