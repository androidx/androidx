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

package androidx.camera.core.impl.utils;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.resolutionselector.ResolutionSelector;

/**
 * Utility class for resolution selector related operations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ResolutionSelectorUtil {
    private ResolutionSelectorUtil() {
    }

    /**
     * Merges two resolution selectors.
     *
     * @param baseResolutionSelector       the base resolution selector.
     * @param resolutionSelectorToOverride the resolution selector that the inside settings can
     *                                     override the corresponding setting in the base
     *                                     resolution selector.
     * @return {@code null} if both the input resolution selectors are null. Otherwise, returns
     * the merged resolution selector.
     */
    @Nullable
    public static ResolutionSelector overrideResolutionSelectors(
            @Nullable ResolutionSelector baseResolutionSelector,
            @Nullable ResolutionSelector resolutionSelectorToOverride) {
        if (resolutionSelectorToOverride == null) {
            return baseResolutionSelector;
        } else if (baseResolutionSelector == null) {
            return resolutionSelectorToOverride;
        }

        ResolutionSelector.Builder builder =
                ResolutionSelector.Builder.fromResolutionSelector(baseResolutionSelector);

        if (resolutionSelectorToOverride.getAspectRatioStrategy() != null) {
            builder.setAspectRatioStrategy(resolutionSelectorToOverride.getAspectRatioStrategy());
        }

        if (resolutionSelectorToOverride.getResolutionStrategy() != null) {
            builder.setResolutionStrategy(resolutionSelectorToOverride.getResolutionStrategy());
        }

        if (resolutionSelectorToOverride.getResolutionFilter() != null) {
            builder.setResolutionFilter(resolutionSelectorToOverride.getResolutionFilter());
        }

        if (resolutionSelectorToOverride.getAllowedResolutionMode()
                != ResolutionSelector.ALLOWED_RESOLUTIONS_NORMAL) {
            builder.setAllowedResolutionMode(
                    resolutionSelectorToOverride.getAllowedResolutionMode());
        }

        return builder.build();
    }
}
