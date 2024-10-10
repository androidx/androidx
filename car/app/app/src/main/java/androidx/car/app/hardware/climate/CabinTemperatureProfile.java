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

import static androidx.car.app.hardware.common.CarZone.CAR_ZONE_GLOBAL;

import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarZone;

import com.google.common.collect.ImmutableMap;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about the {@link
 * ClimateProfileRequest#FEATURE_CABIN_TEMPERATURE} feature such as supported min/max values and
 * increments for the feature.
 */
@ExperimentalCarApi
public final class CabinTemperatureProfile {

    @VisibleForTesting
    static final float DEFAULT_TEMPERATURE_INCREMENT = -1f;
    @VisibleForTesting
    static final Pair<Float, Float> DEFAULT_TEMPERATURE_RANGE = new Pair<>(-1f, -1f);
    @VisibleForTesting
    static final Map<Set<CarZone>, Pair<Float, Float>> DEFAULT_CELSIUS_TEMPERATURE_MAP =
            ImmutableMap.<Set<CarZone>, Pair<Float, Float>>builder().put(Collections.singleton(
                    CAR_ZONE_GLOBAL), new Pair<>(-1f, -1f)).buildKeepingLast();

    private final Pair<Float, Float> mSupportedMinMaxCelsiusRange;

    private final Pair<Float, Float> mSupportedMinMaxFahrenheitRange;

    private final Map<Set<CarZone>, Pair<Float, Float>> mCarZoneSetsToCabinCelsiusTemperatureRanges;

    private final float mCelsiusSupportedIncrement;

    private final float mFahrenheitSupportedIncrement;

    /** Reports whether the min/max Celsius from the config array is available or not. */
    public boolean hasSupportedMinMaxCelsiusRange() {
        return !mSupportedMinMaxCelsiusRange.equals(DEFAULT_TEMPERATURE_RANGE);
    }

    /** Reports whether the min/max Fahrenheit from the config array is available or not. */
    public boolean hasSupportedMinMaxFahrenheitRange() {
        return !mSupportedMinMaxFahrenheitRange.equals(DEFAULT_TEMPERATURE_RANGE);
    }

    /** Reports whether the min/max Celsius mapped to the car zones is available or not. */
    public boolean hasCarZoneSetsToCabinCelsiusTemperatureRanges() {
        return mCarZoneSetsToCabinCelsiusTemperatureRanges != DEFAULT_CELSIUS_TEMPERATURE_MAP;
    }

    /** Reports whether the increment value in Celsius is available or not. */
    public boolean hasCelsiusSupportedIncrement() {
        return mCelsiusSupportedIncrement != DEFAULT_TEMPERATURE_INCREMENT;
    }

    /** Reports whether the increment value in Fahrenheit is available or not. */
    public boolean hasFahrenheitSupportedIncrement() {
        return mFahrenheitSupportedIncrement != DEFAULT_TEMPERATURE_INCREMENT;
    }

    /**
     * Returns a pair of min and max range for the values of the property in Celsius.
     */
    public @NonNull Pair<Float, Float> getSupportedMinMaxCelsiusRange() {
        if (hasSupportedMinMaxCelsiusRange()) {
            return mSupportedMinMaxCelsiusRange;
        }
        throw new IllegalStateException("Celsius min/max range is not available.");
    }

    /**
     * Returns a pair of min and max range for the values of the property in Fahrenheit.
     */
    public @NonNull Pair<Float, Float> getSupportedMinMaxFahrenheitRange() {
        if (hasSupportedMinMaxFahrenheitRange()) {
            return mSupportedMinMaxFahrenheitRange;
        }
        throw new IllegalStateException("Fahrenheit min/max range is not available.");
    }

    /**
     * Returns a pair of supported min/max range values in Celsius for the feature mapped to the
     * set of car zones.
     *
     * <p>The values that can be regulated together for a set of car zones are combined together.
     */
    public @NonNull Map<Set<CarZone>, Pair<Float, Float>>
            getCarZoneSetsToCabinCelsiusTemperatureRanges() {
        if (hasCarZoneSetsToCabinCelsiusTemperatureRanges()) {
            return mCarZoneSetsToCabinCelsiusTemperatureRanges;
        }
        throw new IllegalStateException("Celsius min/max range corresponding to car zones is not "
                + "available.");
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
        mCarZoneSetsToCabinCelsiusTemperatureRanges =
                builder.mCarZoneSetsToCabinCelsiusTemperatureRanges;
        mCelsiusSupportedIncrement = builder.mCelsiusSupportedIncrement;
        mFahrenheitSupportedIncrement = builder.mFahrenheitSupportedIncrement;
    }

    /** A builder for CabinTemperatureProfile. */
    public static final class Builder {
        Pair<Float, Float> mSupportedMinMaxCelsiusRange = DEFAULT_TEMPERATURE_RANGE;
        Pair<Float, Float> mSupportedMinMaxFahrenheitRange = DEFAULT_TEMPERATURE_RANGE;
        Map<Set<CarZone>, Pair<Float, Float>> mCarZoneSetsToCabinCelsiusTemperatureRanges =
                DEFAULT_CELSIUS_TEMPERATURE_MAP;
        float mCelsiusSupportedIncrement = DEFAULT_TEMPERATURE_INCREMENT;
        float mFahrenheitSupportedIncrement = DEFAULT_TEMPERATURE_INCREMENT;

        /** Sets the supported min/max Celsius range for the {@link CabinTemperatureProfile}. */
        public @NonNull Builder setSupportedMinMaxCelsiusRange(
                @NonNull Pair<Float, Float> supportedMinMaxCelsiusRange) {
            mSupportedMinMaxCelsiusRange = supportedMinMaxCelsiusRange;
            return this;
        }

        /** Sets the supported min/max Fahrenheit range for the {@link CabinTemperatureProfile}. */
        public @NonNull Builder setSupportedMinMaxFahrenheitRange(
                @NonNull Pair<Float, Float> supportedMinMaxFahrenheitRange) {
            mSupportedMinMaxFahrenheitRange = supportedMinMaxFahrenheitRange;
            return this;
        }

        /** Sets the car zone to Celsius range mapping for the
         *  {@link CabinTemperatureProfile}. */
        public @NonNull Builder setCarZoneSetsToCabinCelsiusTemperatureRanges(
                @NonNull Map<Set<CarZone>, Pair<Float, Float>>
                        carZoneSetsToCabinCelsiusTemperatureRanges) {
            mCarZoneSetsToCabinCelsiusTemperatureRanges =
                    carZoneSetsToCabinCelsiusTemperatureRanges;
            return this;
        }

        /** Sets the supported Celsius increment for the {@link CabinTemperatureProfile}. */
        public @NonNull Builder setCelsiusSupportedIncrement(
                float celsiusSupportedIncrement) {
            mCelsiusSupportedIncrement = celsiusSupportedIncrement;
            return this;
        }

        /** Sets the supported Fahrenheit increment for the {@link CabinTemperatureProfile}. */
        public @NonNull Builder setFahrenheitSupportedIncrement(
                float fahrenheitSupportedIncrement) {
            mFahrenheitSupportedIncrement = fahrenheitSupportedIncrement;
            return this;
        }
        /** Creates an instance of builder. */
        public Builder() {};

        /** Create a CabinTemperatureProfile. */
        public @NonNull CabinTemperatureProfile build() {
            return new CabinTemperatureProfile(this);
        }

    }
}
