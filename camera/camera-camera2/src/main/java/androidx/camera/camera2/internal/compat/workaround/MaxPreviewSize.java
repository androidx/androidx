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

/**
 * Helper class that overrides the maximum preview size used in surface combination check.
 *
 * @see androidx.camera.camera2.internal.SupportedSurfaceCombination
 */
@RequiresApi(21)
public class MaxPreviewSize {

    @Nullable
    private final ExtraCroppingQuirk mExtraCroppingQuirk;

    /**
     * Constructs new {@link MaxPreviewSize}.
     */
    public MaxPreviewSize() {
        this(DeviceQuirks.get(ExtraCroppingQuirk.class));
    }

    /**
     * Constructs new {@link MaxPreviewSize}.
     */
    @VisibleForTesting
    MaxPreviewSize(@Nullable ExtraCroppingQuirk extraCroppingQuirk) {
        mExtraCroppingQuirk = extraCroppingQuirk;
    }

    /**
     * Gets the max preview resolution based on the default preview max resolution.
     *
     * <p> If select resolution is larger than the default resolution, return the select
     * resolution. The select resolution has been manually tested on the device. Otherwise,
     * return the default max resolution.
     */
    @NonNull
    public Size getMaxPreviewResolution(@NonNull Size defaultMaxPreviewResolution) {
        if (mExtraCroppingQuirk == null) {
            return defaultMaxPreviewResolution;
        }
        Size selectResolution = mExtraCroppingQuirk.getVerifiedResolution(
                SurfaceConfig.ConfigType.PRIV);
        if (selectResolution == null) {
            return defaultMaxPreviewResolution;
        }
        boolean isSelectResolutionLarger =
                selectResolution.getWidth() * selectResolution.getHeight()
                        > defaultMaxPreviewResolution.getWidth()
                        * defaultMaxPreviewResolution.getHeight();
        return isSelectResolutionLarger ? selectResolution : defaultMaxPreviewResolution;
    }
}
