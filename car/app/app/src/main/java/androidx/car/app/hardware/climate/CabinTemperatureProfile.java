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

import androidx.annotation.NonNull;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarZone;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} feature such as feature Id and supported
 * values for the feature.
 */
@ExperimentalCarApi
public final class CabinTemperatureProfile {

    @NonNull
    private final Map<Set<CarZone>, Pair<Float, Float>> mSupportedCarZoneSetsToCelsiusRange;

    @NonNull
    private final Map<Set<CarZone>, Pair<Float, Float>> mSupportedCarZoneSetsToFahrenheitRange;

    private final float mCelsiusSupportedIncrement;

    private final float mFahrenheitSupportedIncrement;

    /**
     * Returns a min/max range for the values of the property in Celsius, mapped to the set of car
     * zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    @NonNull
    public Map<Set<CarZone>, Pair<Float, Float>> getSupportedCarZoneSetsToCelsiusRange() {
        return mSupportedCarZoneSetsToCelsiusRange;
    }

    /**
     * Returns a min/max range for the values of the property in Fahrenheit, mapped to the set of
     * car zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    @NonNull
    public Map<Set<CarZone>, Pair<Float, Float>> getSupportedCarZoneSetsToFahrenheitRange() {
        return mSupportedCarZoneSetsToFahrenheitRange;
    }

    /** Reports whether the increment value in Celsius is available or not. */
    public boolean hasCelsiusSupportedIncrement() {
        return mCelsiusSupportedIncrement != -1.0f;
    }

    /** Reports whether the increment value in Fahrenheit is available or not. */
    public boolean hasFahrenheitSupportedIncrement() {
        return mFahrenheitSupportedIncrement != -1.0f;
    }

    /**
     * Returns the increment number by which the Celsius values in the range differ.
     *
     * <p>For example, for the range [16.0, 28.0] and increment 0.5 would mean possible values
     * like [16.0, 16.5, 17.0, ...., 27.5, 28.0].
     *
     * <p> throws IllegalStateException if the values of the increment is not present. </p>
     */
    public float getCelsiusSupportedIncrement() {
        if (hasCelsiusSupportedIncrement()) {
            return mCelsiusSupportedIncrement;
        }
        throw new IllegalStateException("Celsius increment value is not available.");
    }

    /**
     * Returns the increment number by which the Fahrenheit values in the range differ.
     *
     * <p>For example, for the range [60.5, 82.5] and increment 1.0 would mean possible values
     * like [60.5, 61.5, 62.0, ...., 81.5, 82.5].
     *
     * <p> throws IllegalStateException if the values of the increment is not present. </p>
     */
    public float getFahrenheitSupportedIncrement() {
        if (hasFahrenheitSupportedIncrement()) {
            return mFahrenheitSupportedIncrement;
        }
        throw new IllegalStateException("Fahrenheit increment value is not available.");
    }

    CabinTemperatureProfile(Builder builder) {
        mSupportedCarZoneSetsToCelsiusRange = Collections.unmodifiableMap(
                builder.mSupportedCarZoneSetsToCelsiusRange);
        mSupportedCarZoneSetsToFahrenheitRange = Collections.unmodifiableMap(
                builder.mSupportedCarZoneSetsToFahrenheitRange);
        mCelsiusSupportedIncrement = builder.mCelsiusSupportedIncrement;
        mFahrenheitSupportedIncrement = builder.mFahrenheitSupportedIncrement;
    }

    /** A builder for CabinTemperatureProfile. */
    public static final class Builder {
        final Map<Set<CarZone>, Pair<Float, Float>> mSupportedCarZoneSetsToCelsiusRange;
        final Map<Set<CarZone>, Pair<Float, Float>> mSupportedCarZoneSetsToFahrenheitRange;
        float mCelsiusSupportedIncrement;
        float mFahrenheitSupportedIncrement;

        /**
         * Creates an instance of builder.
         *
         * @param supportedCarZoneSetsToCelsiusRange   map of possible Celsius value range to the
         *                                             set of car zones.
         * @param supportedCarZoneSetsToFahrenheitRange   map of possible Fahrenheit value range to
         *                                                the set of car zones.
         */
        public Builder(@NonNull Map<Set<CarZone>, Pair<Float, Float>>
                supportedCarZoneSetsToCelsiusRange,
                @NonNull Map<Set<CarZone>, Pair<Float, Float>>
                        supportedCarZoneSetsToFahrenheitRange) {
            mSupportedCarZoneSetsToCelsiusRange = Collections.unmodifiableMap(
                    supportedCarZoneSetsToCelsiusRange);
            mSupportedCarZoneSetsToFahrenheitRange = Collections.unmodifiableMap(
                    supportedCarZoneSetsToFahrenheitRange);
            mCelsiusSupportedIncrement = -1.0f;
            mFahrenheitSupportedIncrement = -1.0f;
        }

        /**
         * Set the increment for supported Celsius values range.
         *
         * <p>If increment is not set for the feature, the Builder will create the feature
         * with aan empty increment.
         *
         * @param celsiusSupportedIncrement Increment value for the Celsius value range
         */
        @NonNull
        public Builder setCelsiusSupportedIncrement(float celsiusSupportedIncrement) {
            mCelsiusSupportedIncrement = celsiusSupportedIncrement;
            return this;
        }

        /**
         * Set the increment for supported Fahrenheit values range.
         *
         * <p>If increment is not set for the feature, the Builder will create the feature
         * with aan empty increment.
         *
         * @param fahrenheitSupportedIncrement Increment value for the Fahrenheit value range
         */
        @NonNull
        public Builder setFahrenheitSupportedIncrement(float fahrenheitSupportedIncrement) {
            mFahrenheitSupportedIncrement = fahrenheitSupportedIncrement;
            return this;
        }

        /** Create a CabinTemperatureProfile. */
        @NonNull
        public CabinTemperatureProfile build() {
            return new CabinTemperatureProfile(this);
        }

    }
}
