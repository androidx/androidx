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

import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.RequestMetadata

/**
 * Copies applied capture request values into the matching [CaptureResult] keys, the same way a
 * camera device reports the settings it used for a frame.
 *
 * The list below holds every public key that exists in both `CaptureRequest` and `CaptureResult` in
 * the Android SDK, matched by name (for example `android.control.aeMode`). Values are copied as
 * requested, so settings that a running AE, AF, or AWB routine replaces on a real device, such as
 * `SENSOR_EXPOSURE_TIME`, show the requested value.
 */
internal object RequestToResultMetadataMapper {
    private val resultKeysByName: Map<String, CaptureResult.Key<*>> =
        buildList<CaptureResult.Key<*>> {
                add(CaptureResult.BLACK_LEVEL_LOCK)
                add(CaptureResult.COLOR_CORRECTION_ABERRATION_MODE)
                add(CaptureResult.COLOR_CORRECTION_GAINS)
                add(CaptureResult.COLOR_CORRECTION_MODE)
                add(CaptureResult.COLOR_CORRECTION_TRANSFORM)
                add(CaptureResult.CONTROL_AE_ANTIBANDING_MODE)
                add(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)
                add(CaptureResult.CONTROL_AE_LOCK)
                add(CaptureResult.CONTROL_AE_MODE)
                add(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER)
                add(CaptureResult.CONTROL_AE_REGIONS)
                add(CaptureResult.CONTROL_AE_TARGET_FPS_RANGE)
                add(CaptureResult.CONTROL_AF_MODE)
                add(CaptureResult.CONTROL_AF_REGIONS)
                add(CaptureResult.CONTROL_AF_TRIGGER)
                add(CaptureResult.CONTROL_AWB_LOCK)
                add(CaptureResult.CONTROL_AWB_MODE)
                add(CaptureResult.CONTROL_AWB_REGIONS)
                add(CaptureResult.CONTROL_CAPTURE_INTENT)
                add(CaptureResult.CONTROL_EFFECT_MODE)
                add(CaptureResult.CONTROL_MODE)
                add(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST)
                add(CaptureResult.CONTROL_SCENE_MODE)
                add(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE)
                add(CaptureResult.EDGE_MODE)
                add(CaptureResult.FLASH_MODE)
                add(CaptureResult.HOT_PIXEL_MODE)
                add(CaptureResult.JPEG_GPS_LOCATION)
                add(CaptureResult.JPEG_ORIENTATION)
                add(CaptureResult.JPEG_QUALITY)
                add(CaptureResult.JPEG_THUMBNAIL_QUALITY)
                add(CaptureResult.JPEG_THUMBNAIL_SIZE)
                add(CaptureResult.LENS_APERTURE)
                add(CaptureResult.LENS_FILTER_DENSITY)
                add(CaptureResult.LENS_FOCAL_LENGTH)
                add(CaptureResult.LENS_FOCUS_DISTANCE)
                add(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE)
                add(CaptureResult.NOISE_REDUCTION_MODE)
                add(CaptureResult.REPROCESS_EFFECTIVE_EXPOSURE_FACTOR)
                add(CaptureResult.SCALER_CROP_REGION)
                add(CaptureResult.SENSOR_EXPOSURE_TIME)
                add(CaptureResult.SENSOR_FRAME_DURATION)
                add(CaptureResult.SENSOR_SENSITIVITY)
                add(CaptureResult.SENSOR_TEST_PATTERN_DATA)
                add(CaptureResult.SENSOR_TEST_PATTERN_MODE)
                add(CaptureResult.SHADING_MODE)
                add(CaptureResult.STATISTICS_FACE_DETECT_MODE)
                add(CaptureResult.STATISTICS_HOT_PIXEL_MAP_MODE)
                add(CaptureResult.STATISTICS_LENS_SHADING_MAP_MODE)
                add(CaptureResult.TONEMAP_CURVE)
                add(CaptureResult.TONEMAP_GAMMA)
                add(CaptureResult.TONEMAP_MODE)
                add(CaptureResult.TONEMAP_PRESET_CURVE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    add(CaptureResult.CONTROL_ENABLE_ZSL)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(CaptureResult.DISTORTION_CORRECTION_MODE)
                    add(CaptureResult.STATISTICS_OIS_DATA_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(CaptureResult.CONTROL_EXTENDED_SCENE_MODE)
                    add(CaptureResult.CONTROL_ZOOM_RATIO)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(CaptureResult.SCALER_ROTATE_AND_CROP)
                    add(CaptureResult.SENSOR_PIXEL_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    add(CaptureResult.CONTROL_AUTOFRAMING)
                    add(CaptureResult.CONTROL_SETTINGS_OVERRIDE)
                    add(CaptureResult.EXTENSION_STRENGTH)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    add(CaptureResult.FLASH_STRENGTH_LEVEL)
                }
                if (Build.VERSION.SDK_INT >= 36) {
                    add(CaptureResult.COLOR_CORRECTION_COLOR_TEMPERATURE)
                    add(CaptureResult.COLOR_CORRECTION_COLOR_TINT)
                    add(CaptureResult.CONTROL_AE_PRIORITY_MODE)
                    add(CaptureResult.CONTROL_ZOOM_METHOD)
                }
                if (Build.VERSION.SDK_INT >= 37) {
                    add(CaptureResult.LOGICAL_MULTI_CAMERA_ADDITIONAL_RESULTS)
                }
            }
            .associateBy { it.name }

    fun mapRequestToResult(requestMetadata: RequestMetadata): Map<CaptureResult.Key<*>, Any?> =
        buildMap {
            for (requestKey in requestMetadata.keys) {
                val resultKey = resultKeysByName[requestKey.name] ?: continue
                val value = requestMetadata[requestKey] ?: continue
                put(resultKey, value)
            }
        }
}
