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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraMetadata

/**
 * An enum to match the CameraMetadata.CONTROL_AWB_MODE_* constants.
 */
enum class AwbMode(val value: Int) {
    AUTO(CameraMetadata.CONTROL_AWB_MODE_AUTO),
    CLOUDY_DAYLIGHT(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    DAYLIGHT(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    INCANDESCENT(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT);

    companion object {
        @JvmStatic
        fun fromIntOrNull(value: Int) = values().firstOrNull { it.value == value }
    }
}