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
 * ClimateProfileRequest#FEATURE_CAR_ZONE_MAPPING} feature such as supported values for the feature.
 */
@ExperimentalCarApi
public final class CarZoneMappingInfoProfile {

    private final @NonNull List<Set<CarZone>> mSupportedCarZoneSets;

    /**
     * Returns a list of supported zones in a car for the feature.
     */
    public @NonNull List<Set<CarZone>> getSupportedCarZoneSets() {
        return mSupportedCarZoneSets;
    }

    CarZoneMappingInfoProfile(Builder builder) {
        mSupportedCarZoneSets = builder.mSupportedCarZoneSets;
    }

    /** A builder for CarZoneMappingInfoProfile. */
    public static final class Builder {
        final List<Set<CarZone>> mSupportedCarZoneSets;

        /**
         * Creates an instance of builder.
         *
         * @param supportedCarZoneSets   a list of all car zones grouped together based on the bit
         *                            map of each area Id.
         */
        public Builder(@NonNull List<Set<CarZone>>  supportedCarZoneSets) {
            mSupportedCarZoneSets = supportedCarZoneSets;
        }

        /** Create a CarZoneMappingInfoProfile. */
        public @NonNull CarZoneMappingInfoProfile build() {
            return new CarZoneMappingInfoProfile(this);
        }
    }
}

