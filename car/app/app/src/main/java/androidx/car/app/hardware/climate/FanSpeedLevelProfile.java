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

package androidx.car.app.hardware.climate;

import android.util.Pair;

import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarZone;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_FAN_SPEED} feature such as supported min/max range values for
 * the feature.
 */
@ExperimentalCarApi
public final class FanSpeedLevelProfile {

    private final @NonNull Map<Set<CarZone>, Pair<Integer, Integer>>
            mCarZoneSetsToFanSpeedLevelRanges;

    /**
     * Returns a pair of supported min/max range values for the feature mapped to the set of car
     * zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    public @NonNull Map<Set<CarZone>, Pair<Integer, Integer>>
            getCarZoneSetsToFanSpeedLevelRanges() {
        return mCarZoneSetsToFanSpeedLevelRanges;
    }

    FanSpeedLevelProfile(FanSpeedLevelProfile.Builder builder) {
        mCarZoneSetsToFanSpeedLevelRanges = Collections.unmodifiableMap(
                builder.mCarZoneSetsToFanSpeedLevelRanges);
    }

    /** A builder for FanSpeedLevelProfile. */
    public static final class Builder {
        Map<Set<CarZone>, Pair<Integer, Integer>> mCarZoneSetsToFanSpeedLevelRanges;

        /**
         * Creates an instance of builder.
         *
         * @param carZoneSetsToFanSpeedLevelRanges   map of min/max range values in meters per
         *                                           second for the property corresponding to the
         *                                           set of car zones. The min/max values are
         *                                           not in a specific unit but represent fan
         *                                           speed level settings. They can take
         *                                           values in the range [1,7] but always greater
         *                                           than 0.
         */
        public Builder(@NonNull Map<Set<CarZone>, Pair<Integer, Integer>>
                carZoneSetsToFanSpeedLevelRanges) {
            mCarZoneSetsToFanSpeedLevelRanges = Collections.unmodifiableMap(
                    carZoneSetsToFanSpeedLevelRanges);
        }

        /** Create a FanSpeedLevelProfile. */
        public @NonNull FanSpeedLevelProfile build() {
            return new FanSpeedLevelProfile(this);
        }
    }
}



