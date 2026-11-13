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

package androidx.camera.camera2.compat.workaround

import android.hardware.camera2.params.DynamicRangeProfiles
import androidx.annotation.RequiresApi
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.DolbyVisionStreamIncorrectColorQuirk
import androidx.camera.core.DynamicRange

/**
 * Overrides the camera stream dynamic range profile used for [DynamicRange.DOLBY_VISION_10_BIT] on
 * devices where the Dolby Vision camera profile produces incorrect colors.
 *
 * @see DolbyVisionStreamIncorrectColorQuirk
 */
@RequiresApi(33)
public class DolbyVisionStreamProfileCorrector(
    private val isQuirkEnabled: Boolean =
        DeviceQuirks[DolbyVisionStreamIncorrectColorQuirk::class.java] != null
) {
    /**
     * Returns the Camera2 dynamic range profile to configure for [dynamicRange], or `null` to use
     * the default mapping.
     *
     * Returns [DynamicRangeProfiles.HLG10] for [DynamicRange.DOLBY_VISION_10_BIT] when the quirk is
     * enabled and the camera supports `HLG10`.
     */
    public fun getStreamProfileOverride(
        dynamicRange: DynamicRange,
        dynamicRangeProfiles: DynamicRangeProfiles,
    ): Long? {
        if (!isQuirkEnabled || dynamicRange != DynamicRange.DOLBY_VISION_10_BIT) {
            return null
        }
        return DynamicRangeProfiles.HLG10.takeIf {
            dynamicRangeProfiles.supportedProfiles.contains(it)
        }
    }
}
