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

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} feature such as supported min/max values and
 * increments for the feature.
 */
@ExperimentalCarApi
public final class CabinTemperatureProfile {

    @NonNull
    private final Pair<Float, Float> mSupportedMinMaxCelsiusRange;

    @NonNull
    private final Pair<Float, Float> mSupportedMinMaxFahrenheitRange;

    private final float mCelsiusSupportedIncrement;

    private final float mFahrenheitSupportedIncrement;

    /**
     * Returns a pair of min and max range for the values of the property in Celsius.
     */
    @NonNull
    public Pair<Float, Float> getSupportedMinMaxCelsiusRange() {
        return mSupportedMinMaxCelsiusRange;
    }

    /**
     * Returns a pair of min and max range for the values of the property in Fahrenheit.
     */
    @NonNull
    public Pair<Float, Float> getSupportedMinMaxFahrenheitRange() {
        return mSupportedMinMaxFahrenheitRange;
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
        mSupportedMinMaxCelsiusRange = builder.mSupportedMinMaxCelsiusRange;
        mSupportedMinMaxFahrenheitRange = builder.mSupportedMinMaxFahrenheitRange;
        mCelsiusSupportedIncrement = builder.mCelsiusSupportedIncrement;
        mFahrenheitSupportedIncrement = builder.mFahrenheitSupportedIncrement;
    }

    /** A builder for CabinTemperatureProfile. */
    public static final class Builder {
        final Pair<Float, Float> mSupportedMinMaxCelsiusRange;
        final Pair<Float, Float> mSupportedMinMaxFahrenheitRange;
        float mCelsiusSupportedIncrement;
        float mFahrenheitSupportedIncrement;

        /**
         * Creates an instance of builder.
         *
         * @param supportedMinMaxCelsiusRange   a pair of min and max range values in Celsius
         * @param supportedMinMaxFahrenheitRange   a pair of min and max range values in
         *                                                Fahrenheit
         * @param celsiusSupportedIncrement   increment number for the temperature values in
         *                                    Celsius
         * @param fahrenheitSupportedIncrement   increment number for the temperature values in
         *                                       Fahrenheit
         */
        public Builder(@NonNull Pair<Float, Float> supportedMinMaxCelsiusRange,
                @NonNull Pair<Float, Float> supportedMinMaxFahrenheitRange,
                float celsiusSupportedIncrement, float fahrenheitSupportedIncrement) {
            mSupportedMinMaxCelsiusRange = supportedMinMaxCelsiusRange;
            mSupportedMinMaxFahrenheitRange = supportedMinMaxFahrenheitRange;
            mCelsiusSupportedIncrement = celsiusSupportedIncrement;
            mFahrenheitSupportedIncrement = fahrenheitSupportedIncrement;
        }

        /** Create a CabinTemperatureProfile. */
        @NonNull
        public CabinTemperatureProfile build() {
            return new CabinTemperatureProfile(this);
        }

    }
}
