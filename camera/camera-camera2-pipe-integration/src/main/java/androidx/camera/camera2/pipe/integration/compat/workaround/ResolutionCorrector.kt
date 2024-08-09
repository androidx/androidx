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
import androidx.camera.core.impl.SurfaceConfig.ConfigType

/**
 * Helper class that overrides user configured resolution with resolution selected based on device
 * quirks.
 */
public class ResolutionCorrector {
    private val extraCroppingQuirk = DeviceQuirks[ExtraCroppingQuirk::class.java]

    /**
     * Returns a new list of resolution with the selected resolution inserted or prioritized.
     *
     * If the list contains the selected resolution, move it to be the first element; if it does not
     * contain the selected resolution, insert it as the first element; if there is no device quirk,
     * return the original list.
     *
     * @param configType the config type based on which the supported resolution is calculated.
     * @param supportedResolutions a ordered list of resolutions calculated by CameraX.
     */
    public fun insertOrPrioritize(
        configType: ConfigType,
        supportedResolutions: List<Size>,
    ): List<Size> {
        if (extraCroppingQuirk == null) {
            return supportedResolutions
        }
        val selectResolution: Size =
            extraCroppingQuirk.getVerifiedResolution(configType) ?: return supportedResolutions
        val newResolutions: MutableList<Size> = mutableListOf()
        newResolutions.add(selectResolution)
        for (size in supportedResolutions) {
            if (size != selectResolution) {
                newResolutions.add(size)
            }
        }
        return newResolutions
    }
}
