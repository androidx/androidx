/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import android.content.res.Configuration;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.layout.WindowLayoutInfo;

/**
 * A developer-defined {@link SplitAttributes} calculator to compute the current split layout with
 * the current device and window state.
 *
 * @see ActivityEmbeddingComponent#setSplitAttributesCalculator(SplitAttributesCalculator)
 * @see ActivityEmbeddingComponent#clearSplitAttributesCalculator()
 * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
 */
public interface SplitAttributesCalculator {
    /**
     * Computes the {@link SplitAttributes} with the current device state.
     *
     * @param params See {@link SplitAttributesCalculatorParams}
     */
    @NonNull
    SplitAttributes computeSplitAttributesForParams(
            @NonNull SplitAttributesCalculatorParams params
    );

    /** The container of {@link SplitAttributesCalculator} parameters */
    class SplitAttributesCalculatorParams {
        @NonNull
        private final WindowMetrics mParentWindowMetrics;
        @NonNull
        private final Configuration mParentConfiguration;
        @NonNull
        private final SplitAttributes mDefaultSplitAttributes;
        private final boolean mIsDefaultMinSizeSatisfied;
        @NonNull
        private final WindowLayoutInfo mParentWindowLayoutInfo;
        @Nullable
        private final String mSplitRuleTag;

        /** Returns the parent container's {@link WindowMetrics} */
        @NonNull
        public WindowMetrics getParentWindowMetrics() {
            return mParentWindowMetrics;
        }

        /** Returns the parent container's {@link Configuration} */
        @NonNull
        public Configuration getParentConfiguration() {
            return new Configuration(mParentConfiguration);
        }

        /**
         * Returns the {@link SplitRule#getDefaultSplitAttributes()}. It could be from
         * {@link SplitRule} Builder APIs
         * ({@link SplitPairRule.Builder#setDefaultSplitAttributes(SplitAttributes)} or
         * {@link SplitPlaceholderRule.Builder#setDefaultSplitAttributes(SplitAttributes)}) or from
         * the {@code splitRatio} and {@code splitLayoutDirection} attributes from static rule
         * definitions.
         */
        @NonNull
        public SplitAttributes getDefaultSplitAttributes() {
            return mDefaultSplitAttributes;
        }

        /**
         * Returns whether the {@link #getParentWindowMetrics()} satisfies
         * {@link SplitRule#checkParentMetrics(WindowMetrics)} with the minimal size requirement
         * specified in the {@link SplitRule} Builder constructors.
         *
         * @see SplitPairRule.Builder
         * @see SplitPlaceholderRule.Builder
         */
        public boolean isDefaultMinSizeSatisfied() {
            return mIsDefaultMinSizeSatisfied;
        }

        /** Returns the parent container's {@link WindowLayoutInfo} */
        @NonNull
        public WindowLayoutInfo getParentWindowLayoutInfo() {
            return mParentWindowLayoutInfo;
        }

        /** Returns {@link SplitRule#getTag()} to apply the {@link SplitAttributes} result. */
        @Nullable
        public String getSplitRuleTag() {
            return mSplitRuleTag;
        }

        SplitAttributesCalculatorParams(
                @NonNull WindowMetrics parentWindowMetrics,
                @NonNull Configuration parentConfiguration,
                @NonNull SplitAttributes defaultSplitAttributes,
                boolean isDefaultMinSizeSatisfied,
                @NonNull WindowLayoutInfo parentWindowLayoutInfo,
                @Nullable String splitRuleTag
        ) {
            mParentWindowMetrics = parentWindowMetrics;
            mParentConfiguration = parentConfiguration;
            mDefaultSplitAttributes = defaultSplitAttributes;
            mIsDefaultMinSizeSatisfied = isDefaultMinSizeSatisfied;
            mParentWindowLayoutInfo = parentWindowLayoutInfo;
            mSplitRuleTag = splitRuleTag;
        }
    }
}
