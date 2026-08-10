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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * Quirk required to filter out RAW sizes that do not match the sensor size.
 *
 * QuirkSummary
 * - Bug Id: 544524419
 * - Description: Sensor dimensions are typically larger than the RAW buffer produced by the HAL on
 *   these devices (e.g. cropped RAW). DngCreator requires the input image dimensions to match
 *   either the pixel array size or pre-correction active array size.
 * - Device(s): Redmi 8/8A
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class DngCreatorSizeMismatchQuirk : Quirk {
    public companion object {
        public fun load(): Boolean {
            return "olivelite".equals(Build.DEVICE, ignoreCase = true) ||
                "olive".equals(Build.DEVICE, ignoreCase = true)
        }
    }
}
