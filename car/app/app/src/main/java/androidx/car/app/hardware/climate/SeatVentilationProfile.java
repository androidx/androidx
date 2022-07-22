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

import androidx.annotation.NonNull;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarZone;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_SEAT_VENTILATION_LEVEL} feature such as feature Id and supported
 * values for the feature.
 */
@ExperimentalCarApi
public final class SeatVentilationProfile {

    @NonNull
    private final Map<Set<CarZone>, List<Integer>> mCarZoneSetsToSeatVentilationValues;

    /**
     * Returns a list of supported values for the feature mapped to the set of car zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    @NonNull
    public Map<Set<CarZone>, List<Integer>> getCarZoneSetsToSeatVentilationValues() {
        return mCarZoneSetsToSeatVentilationValues;
    }

    SeatVentilationProfile(Builder builder) {
        mCarZoneSetsToSeatVentilationValues = Collections.unmodifiableMap(
                builder.mCarZoneSetsToSeatVentilationValues);
    }

    /** A builder for SeatVentilationProfile. */
    public static final class Builder {
        Map<Set<CarZone>, List<Integer>> mCarZoneSetsToSeatVentilationValues;

        /**
         * Creates an instance of builder.
         *
         * @param carZoneSetsToSeatVentilationValues   map of possible values for the property
         *                                             corresponding to the set of car zones.
         */
        public Builder(
                @NonNull Map<Set<CarZone>, List<Integer>> carZoneSetsToSeatVentilationValues) {
            mCarZoneSetsToSeatVentilationValues = Collections.unmodifiableMap(
                    carZoneSetsToSeatVentilationValues);
        }

        /** Create a SeatVentilationProfile. */
        @NonNull
        public SeatVentilationProfile build() {
            return new SeatVentilationProfile(this);
        }
    }
}
