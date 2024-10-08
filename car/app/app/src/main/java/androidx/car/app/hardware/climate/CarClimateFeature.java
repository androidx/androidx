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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A data value used in climate related requests to indicate the features and zones the
 * caller is interested in.
 */
@RequiresCarApi(5)
@MainThread
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class CarClimateFeature {
    @ClimateProfileRequest.ClimateProfileFeature
    private final int mFeature;

    @NonNull
    private final List<CarZone> mCarZones;

    /**
     * Returns a list of CarZones.
     *
     * <p>If the application is interested in all zones, the return value will only contains a
     * CarZone#CAR_ZONE_GLOBAL. Otherwise, the return values will contain every individual
     * seats the application requested for.
     * <p>For example, the application requested information for the second row seats. The list
     * will contain three individual CarZones. They are the left side seat of the second
     * row, the center seat of the second row and the right side seat of the second row.
     */
    @NonNull
    public List<CarZone> getCarZones() {
        return mCarZones;
    }

    /**
     * Returns an integer flag of the feature.
     *
     * <p> The feature flag must be one of ClimateProfileRequest.ClimateProfileFeatures.
     */
    @ClimateProfileRequest.ClimateProfileFeature
    public int getFeature() {
        return mFeature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarClimateFeature that = (CarClimateFeature) o;
        return Objects.equals(mFeature, that.mFeature) && Objects.equals(
                mCarZones, that.mCarZones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFeature, mCarZones);
    }

    @NonNull
    @Override
    public String toString() {
        return "ClimateProfileFeature{"
                + "mFeature='" + mFeature
                + '\'' + ", mCarZones=" + mCarZones
                + '}';
    }

    CarClimateFeature(Builder builder) {
        mCarZones = Collections.unmodifiableList(builder.mCarZones);
        mFeature = builder.mFeature;
    }

    /** A builder for CarClimateFeature. */
    public static final class Builder {
        final int mFeature;
        List<CarZone> mCarZones;

        /**
         * Creates an instance of builder.
         *
         * @param feature one of integer flags in ClimateStateFeature or
         *                ClimateProfileFeatures
         */
        public Builder(@ClimateProfileRequest.ClimateProfileFeature int feature) {
            mFeature = feature;
            mCarZones = new ArrayList<>();
        }

        /**
         * Adds CarZones into the CarClimateFeature.
         *
         * <p>If carZones are not set in the feature, the Builder will create the feature
         * with all available zones.
         *
         * @param carZones CarZones for this CarClimateFeature
         */
        @NonNull
        public Builder addCarZones(@NonNull CarZone... carZones) {
            mCarZones.addAll(Arrays.asList(carZones));
            return this;
        }

        /** Create a CarClimateFeature. */
        @NonNull
        public CarClimateFeature build() {
            return new CarClimateFeature(this);
        }
    }
}
