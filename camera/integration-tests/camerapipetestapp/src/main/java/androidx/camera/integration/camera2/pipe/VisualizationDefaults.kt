/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe

/** Defines how metadata keys will be visualized */
object VisualizationDefaults {

    val keysVisualizedAsKeyValuePair: List<CameraMetadataKey> = listOf(
        CameraMetadataKey.CONTROL_AF_MODE,
        CameraMetadataKey.CONTROL_AWB_MODE,
        CameraMetadataKey.BLACK_LEVEL_LOCK,
        CameraMetadataKey.JPEG_QUALITY,
        CameraMetadataKey.LENS_FOCUS_DISTANCE,
        CameraMetadataKey.LENS_FOCAL_LENGTH,
        CameraMetadataKey.CONTROL_AE_MODE,
        CameraMetadataKey.COLOR_CORRECTION_ABERRATION_MODE,
        CameraMetadataKey.CONTROL_ZOOM_RATIO,
        CameraMetadataKey.JPEG_ORIENTATION,
        CameraMetadataKey.LENS_FILTER_DENSITY,
        CameraMetadataKey.SENSOR_SENSITIVITY
    )
    val keysVisualizedAsValueGraph: Set<CameraMetadataKey> = setOf(
        CameraMetadataKey.LENS_FOCUS_DISTANCE
    )

    val keysVisualizedAsStateGraph: Set<CameraMetadataKey> = setOf(
        CameraMetadataKey.CONTROL_AE_MODE,
        CameraMetadataKey.CONTROL_AF_MODE,
        CameraMetadataKey.CONTROL_AWB_MODE
    )
}

/** Defines the valid ranges of values for metadata keys when applicable */
object ValueRanges {
    val absoluteRanges: Map<CameraMetadataKey, Pair<Number, Number>> = mapOf(
        CameraMetadataKey.LENS_FOCUS_DISTANCE to Pair(0f, 1f),
        CameraMetadataKey.LENS_FOCAL_LENGTH to Pair(0f, 30f)
    )
}

/** Defines the integer to string description of states for metadata keys when applicable */
object StateDetails {
    val intToStringMap: Map<CameraMetadataKey, Map<Int, String>> = mapOf(
        CameraMetadataKey.CONTROL_AE_MODE to mapOf(
            0 to "OFF",
            1 to "ON",
            2 to "ON_AUTO_FLASH",
            3 to "ON_AUTO_FLASH_REDEYE",
            4 to "ON_EXTERNAL_FLASH"
        ),

        CameraMetadataKey.CONTROL_AF_MODE to mapOf(
            0 to "OFF",
            1 to "AUTO",
            2 to "MACRO",
            3 to "CONTINUOUS_VIDEO",
            4 to "CONTINUOUS_PICTURE",
            5 to "EDOF"
        ),

        CameraMetadataKey.CONTROL_AWB_MODE to mapOf(
            0 to "OFF",
            1 to "AUTO",
            2 to "INCANDESCENT",
            3 to "FLUORESCENT",
            4 to "WARM_FLUORESCENT",
            5 to "DAYLIGHT",
            6 to "CLOUDY_DAYLIGHT",
            7 to "TWILIGHT",
            8 to "SHADE"
        ),

        CameraMetadataKey.COLOR_CORRECTION_ABERRATION_MODE to mapOf(
            0 to "OFF",
            1 to "FAST",
            2 to "HIGH_QUALITY"
        )
    )
}
