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

import java.util.List;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_HVAC_RECIRCULATION} feature such as supported values for the
 * feature.
 */
@ExperimentalCarApi
public final class HvacRecirculationProfile {

    private final @NonNull List<Set<CarZone>> mSupportedCarZones;

    /**
     * Returns a list of supported zones in a car for the feature.
     */
    public @NonNull List<Set<CarZone>> getSupportedCarZones() {
        return mSupportedCarZones;
    }

    HvacRecirculationProfile(Builder builder) {
        mSupportedCarZones = builder.mSupportedCarZones;
    }

    /** A builder for HvacRecirculationProfile. */
    public static final class Builder {
        final List<Set<CarZone>> mSupportedCarZones;

        /**
         * Creates an instance of builder.
         *
         * @param supportedCarZones   a list of all car zones grouped together based on the bit
         *                            map of each area Id.
         */
        public Builder(@NonNull List<Set<CarZone>>  supportedCarZones) {
            mSupportedCarZones = supportedCarZones;
        }

        /** Create a HvacRecirculationProfile. */
        public @NonNull HvacRecirculationProfile build() {
            return new HvacRecirculationProfile(this);
        }
    }
}

