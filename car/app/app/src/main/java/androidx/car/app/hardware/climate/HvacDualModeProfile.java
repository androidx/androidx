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
 * ClimateProfileRequest#FEATURE_HVAC_DUAL_MODE} feature such as supported values for the feature.
 */
@ExperimentalCarApi
public final class HvacDualModeProfile {

    private final @NonNull List<Set<CarZone>> mSupportedCarZoneSets;

    /**
     * Returns a list of supported zones in a car for the feature.
     */
    public @NonNull List<Set<CarZone>> getSupportedCarZoneSets() {
        return mSupportedCarZoneSets;
    }

    HvacDualModeProfile(Builder builder) {
        mSupportedCarZoneSets = builder.mSupportedCarZoneSets;
    }

    /** A builder for HvacDualModeProfile. */
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

        /** Create a HvacDualModeProfile. */
        public @NonNull HvacDualModeProfile build() {
            return new HvacDualModeProfile(this);
        }
    }
}

