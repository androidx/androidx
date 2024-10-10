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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarZone;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Allows specification of a request for setting the car climate states using string flags
 * and car zones.
 *
 * <p> Applications need to use {@link Builder}  to create a {@link ClimateStateRequest}.
 *
 * @param <T> data type of request value
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
public final class ClimateStateRequest<T> {

    @ClimateProfileRequest.ClimateProfileFeature
    private final int mFeature;

    private final List<CarZone> mCarZones;
    private final T mRequestedValue;

    /**
     * Returns a feature flag in @ClimateProfileRequest.ClimateProfileFeature which is included in
     * this request.
     */
    @ClimateProfileRequest.ClimateProfileFeature
    public int getRequestedFeature() {
        return mFeature;
    }

    /** Returns a list of CarZones which are included in this request. */
    public @NonNull List<CarZone> getCarZones() {
        return mCarZones;
    }

    /** Returns the requested value which is included in this request. */
    public @NonNull T getRequestedValue() {
        return mRequestedValue;
    }

    ClimateStateRequest(Builder<T> builder) {
        mFeature = builder.mRequestedFeature;
        mRequestedValue = builder.mRequestedValue;
        if (builder.mCarZones.isEmpty()) {
            mCarZones = Collections.singletonList(CarZone.CAR_ZONE_GLOBAL);
        } else {
            mCarZones = builder.mCarZones;
        }
    }

    @Override
    public @NonNull String toString() {
        return "ClimateStateRequest{"
                + "mFeature='" + mFeature
                + '\'' + ", mCarZones=" + mCarZones
                + ", mRequestedValue=" + mRequestedValue
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClimateStateRequest<?> that = (ClimateStateRequest<?>) o;
        return Objects.equals(mFeature, that.mFeature)
                && Objects.equals(mCarZones, that.mCarZones)
                && Objects.equals(mRequestedValue, that.mRequestedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFeature, mCarZones, mRequestedValue);
    }

    /**
     * A builder of {@link ClimateStateRequest}
     *
     * @param <T> data type of request value
     */
    public static final class Builder<T> {
        List<CarZone> mCarZones = Collections.emptyList();
        final int mRequestedFeature;
        final T mRequestedValue;

        /**
         * Creates an instance of {@link Builder}.
         *
         * @param requestedFeature  one of integer flags in
         *                           ClimateProfileRequest.ClimateProfileFeature
         * @param requestedValue    the requested value for the feature
         */
        public Builder(@ClimateProfileRequest.ClimateProfileFeature int requestedFeature,
                T requestedValue) {
            mRequestedValue = requestedValue;
            mRequestedFeature = requestedFeature;
        }

        /**
         * Adds CarZone into {@link ClimateStateRequest}.
         *
         * <p>Without calling this method, the request will contain CarZone#CAR_ZONE_GLOBAL.
         *
         * @param carZone   the CarZone which the set operation will be applied
         */
        public @NonNull Builder<T> addCarZones(@NonNull CarZone carZone) {
            mCarZones.add(carZone);
            return this;
        }

        /** Constructs a {@link ClimateStateRequest} defined by this builder */
        public @NonNull ClimateStateRequest<T> build() {
            return new ClimateStateRequest<T>(this);
        }
    }
}
