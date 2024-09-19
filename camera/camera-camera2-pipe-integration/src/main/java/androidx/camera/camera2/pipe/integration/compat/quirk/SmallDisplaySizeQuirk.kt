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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import android.util.Size
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/287341266
 * - Description: Quirk required to return the display size for problematic devices. Some devices
 *   might return abnormally small display size (16x16). This might cause PREVIEW size to be
 *   incorrectly determined and all supported output sizes are filtered out.
 * - Device(s): Redmi Note8, Redmi Note 7, SM-A207M (see b/287341266 for the devices list)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class SmallDisplaySizeQuirk : Quirk {
    public val displaySize: Size
        get() = MODEL_TO_DISPLAY_SIZE_MAP[Build.MODEL.uppercase()]!!

    public companion object {
        private val MODEL_TO_DISPLAY_SIZE_MAP =
            mapOf(
                "REDMI NOTE 8" to Size(1080, 2340),
                "REDMI NOTE 7" to Size(1080, 2340),
                "SM-A207M" to Size(720, 1560),
                "REDMI NOTE 7S" to Size(1080, 2340),
                "SM-A127F" to Size(720, 1600),
                "SM-A536E" to Size(1080, 2400),
                "220233L2I" to Size(720, 1600),
                "V2149" to Size(720, 1600),
                "VIVO 1920" to Size(1080, 2340),
                "CPH2223" to Size(1080, 2400),
                "V2029" to Size(720, 1600),
                "CPH1901" to Size(720, 1520),
                "REDMI Y3" to Size(720, 1520),
                "SM-A045M" to Size(720, 1600),
                "SM-A146U" to Size(1080, 2408),
                "CPH1909" to Size(720, 1520),
                "NOKIA 4.2" to Size(720, 1520),
                "SM-G960U1" to Size(1440, 2960),
                "SM-A137F" to Size(1080, 2408),
                "VIVO 1816" to Size(720, 1520),
                "INFINIX X6817" to Size(720, 1612),
                "SM-A037F" to Size(720, 1600),
                "NOKIA 2.4" to Size(720, 1600),
                "SM-A125M" to Size(720, 1600),
                "INFINIX X670" to Size(1080, 2400)
            )

        public fun load(): Boolean {
            return MODEL_TO_DISPLAY_SIZE_MAP.containsKey(Build.MODEL.uppercase())
        }
    }
}
