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

import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarZone;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_FAN_DIRECTION} feature such as supported values for the feature
 * specific to sets of car zones.
 */
@ExperimentalCarApi
public final class FanDirectionProfile {

    private final @NonNull Map<Set<CarZone>, Set<Integer>> mCarZoneSetsToFanDirectionValues;

    /**
     * Returns a set of supported values for the feature mapped to the set of car
     * zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    public @NonNull Map<Set<CarZone>, Set<Integer>> getCarZoneSetsToFanDirectionValues() {
        return mCarZoneSetsToFanDirectionValues;
    }

    FanDirectionProfile(FanDirectionProfile.Builder builder) {
        mCarZoneSetsToFanDirectionValues = Collections.unmodifiableMap(
                builder.mCarZoneSetsToFanDirectionValues);
    }

    /** A builder for FanDirectionProfile. */
    public static final class Builder {
        Map<Set<CarZone>, Set<Integer>> mCarZoneSetsToFanDirectionValues;

        /**
         * Creates an instance of builder.
         *
         * @param carZoneSetsToFanDirectionValues   map of supported values for the property
         *                                 corresponding to the set of car zones. The
         *                                 range values could be one of the values
         *                                 from [0,6] specified in {@link
         *                                 androidx.car.app.hardware.common.CarPropertyProfile#
         *                                 #VehicleHvacFanDirection}.
         */
        public Builder(@NonNull Map<Set<CarZone>, Set<Integer>>
                carZoneSetsToFanDirectionValues) {
            mCarZoneSetsToFanDirectionValues = Collections.unmodifiableMap(
                    carZoneSetsToFanDirectionValues);
        }

        /** Create a FanDirectionProfile. */
        public @NonNull FanDirectionProfile build() {
            return new FanDirectionProfile(this);
        }
    }
}
