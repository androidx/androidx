/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.app;

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a collection of ranking features that can be enabled or disabled for specific
 * search operations.
 * @exportToFramework:hide
 */
//TODO(b/387291182) unhide this class when it is supported in SearchSpec
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_SET_SEARCH_AND_RANKING_FEATURE)
@SuppressWarnings("HiddenSuperclass")
public class RankingFeatures extends EnabledFeatures {

    RankingFeatures(List<String> enabledFeatures) {
        super(enabledFeatures);
    }

    /**
     * Returns whether the ScorablePropertyRanking feature is enabled.
     */
    @ExperimentalAppSearchApi
    @FlaggedApi(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
    public boolean isScorablePropertyRankingEnabled() {
        return mEnabledFeatures.contains(FeatureConstants.SCHEMA_SCORABLE_PROPERTY_CONFIG);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RankingFeatures)) {
            return false;
        }
        RankingFeatures that = (RankingFeatures) o;
        return Objects.equals(mEnabledFeatures, that.mEnabledFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEnabledFeatures);
    }

    /** Builder class for {@link RankingFeatures}.*/
    public static final class Builder {
        private final ArraySet<String> mEnabledFeatures = new ArraySet<>();

        /**
         * Sets the ScorablePropertyRanking feature as enabled or disabled.
         *
         * <p>If enabled, 'getScorableProperty' function can be used in the advanced ranking
         * expression. For details, see {@link SearchSpec.Builder#setRankingStrategy(String)}.
         *
         * @param enabled Enables the feature if true, otherwise disables it.
         */
        @CanIgnoreReturnValue
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_SCORABLE_PROPERTY_CONFIG)
        @ExperimentalAppSearchApi
        @FlaggedApi(Flags.FLAG_ENABLE_SCORABLE_PROPERTY)
        public @NonNull Builder setScorablePropertyRankingEnabled(boolean enabled) {
            modifyEnabledFeature(FeatureConstants.SCHEMA_SCORABLE_PROPERTY_CONFIG, enabled);
            return this;
        }

        /** Builds a {@link androidx.appsearch.app.RankingFeatures} instances. */
        @NonNull
        public RankingFeatures build() {
            return new RankingFeatures(new ArrayList<>(mEnabledFeatures));
        }

        private void modifyEnabledFeature(@NonNull String feature, boolean enabled) {
            if (enabled) {
                mEnabledFeatures.add(feature);
            } else {
                mEnabledFeatures.remove(feature);
            }
        }
    }
}
