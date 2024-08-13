/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.util.Size
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ExtraCroppingQuirk
import androidx.camera.core.impl.SurfaceConfig

/** Helper class that overrides the maximum preview size used in surface combination check. */
public class MaxPreviewSize
constructor(
    private val extraCroppingQuirk: ExtraCroppingQuirk? =
        DeviceQuirks[ExtraCroppingQuirk::class.java]
) {

    /**
     * Gets the max preview resolution based on the default preview max resolution.
     *
     * If select resolution is larger than the default resolution, return the select resolution. The
     * select resolution has been manually tested on the device. Otherwise, return the default max
     * resolution.
     */
    public fun getMaxPreviewResolution(defaultMaxPreviewResolution: Size): Size {
        if (extraCroppingQuirk == null) {
            return defaultMaxPreviewResolution
        }
        val selectResolution: Size =
            extraCroppingQuirk.getVerifiedResolution(SurfaceConfig.ConfigType.PRIV)
                ?: return defaultMaxPreviewResolution
        val isSelectResolutionLarger =
            (selectResolution.width * selectResolution.height >
                defaultMaxPreviewResolution.width * defaultMaxPreviewResolution.height)
        return if (isSelectResolutionLarger) selectResolution else defaultMaxPreviewResolution
    }
}
