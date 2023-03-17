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

package androidx.camera.core.resolutionselector;

import static androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.UseCase;

/**
 * A set of requirements and priorities used to select a resolution for the {@link UseCase}.
 *
 * <p>The resolution selection mechanism is determined by the following three steps:
 * <ol>
 *     <li> Collect the supported output sizes and add them to the candidate resolution list.
 *     <li> Filter and sort the candidate resolution list according to the preference settings.
 *     <li> Consider all the resolution selector settings of bound {@link UseCase}s to find the
 *     resolution that best suits each {@link UseCase}.
 * </ol>
 *
 * <p>For the first step, all supported resolution output sizes are added to the candidate
 * resolution list as the starting point.
 *
 * <p>ResolutionSelector provides the following function for applications to adjust the candidate
 * resolution settings.
 * <ul>
 *     <li> {@link Builder#setHighResolutionEnabledFlags(int)}
 * </ul>
 *
 * <p>For the second step, ResolutionSelector provides the following three functions for
 * applications to determine which resolution should be selected with higher priority.
 * <ul>
 *     <li> {@link Builder#setAspectRatioStrategy(AspectRatioStrategy)}
 *     <li> {@link Builder#setResolutionStrategy(ResolutionStrategy)}
 *     <li> {@link Builder#setResolutionFilter(ResolutionFilter)}
 * </ul>
 *
 * <p>CameraX sorts the collected sizes according to the specified aspect ratio and resolution
 * strategies. The aspect ratio strategy has precedence over the resolution strategy for sorting
 * the resolution candidate list. If applications specify a custom resolution filter, CameraX
 * passes the resulting sizes list, sorted by the specified aspect ratio and resolution
 * strategies, to the resolution filter to get the final desired list.
 *
 * <p>Different types of {@link UseCase}s might have their own default settings. You can see the
 * {@link UseCase} builders’ {@code setResolutionSelector()} function to know the details for each
 * type of {@link UseCase}.
 *
 * <p>In the third step, CameraX selects the final resolution for the {@link UseCase} based on the
 * camera device's hardware level, capabilities, and the bound {@link UseCase} combination.
 * Applications can check which resolution is finally selected by using the {@link UseCase}'s
 * {@code getResolutionInfo()} function.
 *
 * <p>Note that a ResolutionSelector with more restricted settings may result in that no
 * resolution can be selected to use. Applications will receive {@link IllegalArgumentException}
 * when binding the {@link UseCase}s with such kind of ResolutionSelector. Applications can
 * specify the {@link AspectRatioStrategy} and {@link ResolutionStrategy} with proper fallback
 * rules to avoid the {@link IllegalArgumentException} or try-catch it and show a proper message
 * to the end users.
 *
 * <p>When creating a ResolutionSelector instance, the
 * {@link AspectRatioStrategy#RATIO_4_3_FALLBACK_AUTO_STRATEGY} will be the default
 * {@link AspectRatioStrategy} if it is not set. However, if neither the
 * {@link ResolutionStrategy} nor the high resolution enabled flags are set, there will be no
 * default value specified.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ResolutionSelector {
    @Nullable
    private final AspectRatioStrategy mAspectRatioStrategy;
    @Nullable
    private final ResolutionStrategy mResolutionStrategy;
    @Nullable
    private final ResolutionFilter mResolutionFilter;
    private final int mHighResolutionEnabledFlags;

    ResolutionSelector(
            @Nullable AspectRatioStrategy aspectRatioStrategy,
            @Nullable ResolutionStrategy resolutionStrategy,
            @Nullable ResolutionFilter resolutionFilter,
            int highResolutionEnabledFlags) {
        mAspectRatioStrategy = aspectRatioStrategy;
        mResolutionStrategy = resolutionStrategy;
        mResolutionFilter = resolutionFilter;
        mHighResolutionEnabledFlags = highResolutionEnabledFlags;
    }

    /**
     * Returns the specified {@link AspectRatioStrategy}, or null if not specified.
     */
    @Nullable
    public AspectRatioStrategy getAspectRatioStrategy() {
        return mAspectRatioStrategy;
    }

    /**
     * Returns the specified {@link ResolutionStrategy}, or null if not specified.
     */
    @Nullable
    public ResolutionStrategy getResolutionStrategy() {
        return mResolutionStrategy;
    }

    /**
     * Returns the specified {@link ResolutionFilter} implementation, or null if not specified.
     */
    @Nullable
    public ResolutionFilter getResolutionFilter() {
        return mResolutionFilter;
    }

    /**
     * Returns the specified high resolution enabled flags.
     */
    public int getHighResolutionEnabledFlags() {
        return mHighResolutionEnabledFlags;
    }

    /**
     * Builder for a {@link ResolutionSelector}.
     */
    public static final class Builder {
        @Nullable
        private AspectRatioStrategy mAspectRatioStrategy = RATIO_4_3_FALLBACK_AUTO_STRATEGY;
        @Nullable
        private ResolutionStrategy mResolutionStrategy = null;
        @Nullable
        private ResolutionFilter mResolutionFilter = null;
        private int mHighResolutionEnabledFlags = 0;

        /**
         * Creates a Builder instance.
         */
        public Builder() {
        }

        private Builder(@NonNull ResolutionSelector resolutionSelector) {
            mAspectRatioStrategy = resolutionSelector.getAspectRatioStrategy();
            mResolutionStrategy = resolutionSelector.getResolutionStrategy();
            mResolutionFilter = resolutionSelector.getResolutionFilter();
            mHighResolutionEnabledFlags = resolutionSelector.getHighResolutionEnabledFlags();
        }

        /**
         * Creates a Builder from an existing resolution selector.
         */
        @NonNull
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static Builder fromResolutionSelector(
                @NonNull ResolutionSelector resolutionSelector) {
            return new Builder(resolutionSelector);
        }

        /**
         * Sets the aspect ratio selection strategy for the {@link UseCase}. The aspect ratio
         * selection strategy determines how the {@link UseCase} will choose the aspect ratio of
         * the captured image.
         */
        @NonNull
        public Builder setAspectRatioStrategy(@NonNull AspectRatioStrategy aspectRatioStrategy) {
            mAspectRatioStrategy = aspectRatioStrategy;
            return this;
        }

        /**
         * Sets the resolution selection strategy for the {@link UseCase}. The resolution selection
         * strategy determines how the {@link UseCase} will choose the resolution of the captured
         * image.
         */
        @NonNull
        public Builder setResolutionStrategy(@NonNull ResolutionStrategy resolutionStrategy) {
            mResolutionStrategy = resolutionStrategy;
            return this;
        }

        /**
         * Sets the resolution filter to output the final desired sizes list. The resolution
         * filter will filter out unsuitable sizes and sort the resolution list in the preferred
         * order. The preferred order is the order in which the resolutions should be tried first.
         */
        @NonNull
        public Builder setResolutionFilter(@NonNull ResolutionFilter resolutionFilter) {
            mResolutionFilter = resolutionFilter;
            return this;
        }

        /**
         * Sets high resolutions enabled flags to allow the application to select high
         * resolutions for the {@link UseCase}s. This will enable the application to choose high
         * resolutions for the captured image, which may result in better quality images.
         *
         * <p>Now, only {@link HighResolution#FLAG_DEFAULT_MODE_ON} is allowed for this function.
         */
        @NonNull
        public Builder setHighResolutionEnabledFlags(int flags) {
            mHighResolutionEnabledFlags = flags;
            return this;
        }

        /**
         * Builds the resolution selector. This will create a resolution selector that can be
         * used to select the desired resolution for the captured image.
         */
        @NonNull
        public ResolutionSelector build() {
            return new ResolutionSelector(mAspectRatioStrategy, mResolutionStrategy,
                    mResolutionFilter, mHighResolutionEnabledFlags);
        }
    }
}
