/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk;
import androidx.camera.core.impl.SurfaceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that overrides user configured resolution with resolution selected based on device
 * quirks.
 */
@RequiresApi(21)
public class ResolutionSelector {

    @Nullable
    private final ExtraCroppingQuirk mExtraCroppingQuirk;

    /**
     * Constructs new {@link ResolutionSelector}.
     */
    public ResolutionSelector() {
        this(DeviceQuirks.get(ExtraCroppingQuirk.class));
    }

    /**
     * Constructs new {@link ResolutionSelector}.
     */
    @VisibleForTesting
    ResolutionSelector(@Nullable ExtraCroppingQuirk extraCroppingQuirk) {
        mExtraCroppingQuirk = extraCroppingQuirk;
    }

    /**
     * Returns a new list of resolution with the selected resolution inserted or prioritized.
     *
     * <p> If the list contains the selected resolution, move it to be the first element; if it
     * does not contain the selected resolution, insert it as the first element; if there is no
     * device quirk, return the original list.
     *
     * @param configType           the config type based on which the supported resolution is
     *                             calculated.
     * @param supportedResolutions a ordered list of resolutions calculated by CameraX.
     */
    @NonNull
    public List<Size> insertOrPrioritize(
            @NonNull SurfaceConfig.ConfigType configType,
            @NonNull List<Size> supportedResolutions) {
        if (mExtraCroppingQuirk == null) {
            return supportedResolutions;
        }
        Size selectResolution = mExtraCroppingQuirk.getVerifiedResolution(configType);
        if (selectResolution == null) {
            return supportedResolutions;
        }
        List<Size> newResolutions = new ArrayList<>();
        newResolutions.add(selectResolution);
        for (Size size : supportedResolutions) {
            if (!size.equals(selectResolution)) {
                newResolutions.add(size);
            }
        }
        return newResolutions;
    }
}
