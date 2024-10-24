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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about property profile such as the car zones and supported
 * property values associated with them.
 *
 * <p>{@link PropertyManager} uses it to give response to front-end components such as
 * {@link androidx.car.app.hardware.climate.AutomotiveCarClimate}.
 *
 * @param <T> is the value type of response.
 *
 */
@RestrictTo(LIBRARY)
@AutoValue
public abstract class CarPropertyProfile<T> {

    /**
     * Possible hvac fan direction values.
     */
    @IntDef({
            UNKNOWN,
            FACE,
            FLOOR,
            DEFROST,
            /*
             * FACE_FLOOR = FACE | FLOOR
             */
            FACE_FLOOR,
            /*
             * FLOOR_DEFROST = FLOOR | DEFROST
             */
            FLOOR_DEFROST,
            /*
             * FACE_DEFROST = FACE | DEFROST | FLOOR
             */
            FACE_DEFROST,
            /*
             * FACE_FLOOR_DEFROST = FACE | FLOOR | DEFROST
             */
            FACE_FLOOR_DEFROST
    })
    @Target(value = TYPE_USE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface HvacFanDirection {
    }

    /** Refers to 'unknown' value of fan direction. */
    @HvacFanDirection
    public static final int UNKNOWN = 0x0;

    /** Refers to 'face' value of fan direction. */
    @HvacFanDirection
    public static final int FACE = 0x1;

    /** Refers to 'floor' value of fan direction. */
    @HvacFanDirection
    public static final int FLOOR = 0x2;

    /** Refers to 'fan and floor' value of fan direction. */
    @HvacFanDirection
    public static final int FACE_FLOOR = 0x3;

    /** Refers to 'defrost' value of fan direction. */
    @HvacFanDirection
    public static final int DEFROST = 0x4;

    /** Refers to 'face and defrost' value of fan direction. */
    @HvacFanDirection
    public static final int FACE_DEFROST = 0x5;

    /** Refers to 'defrost and floor' value of fan direction. */
    @HvacFanDirection
    public static final int FLOOR_DEFROST = 0x6;

    /** Refers to 'face, floor and defrost' value of fan direction. */
    @HvacFanDirection
    public static final int FACE_FLOOR_DEFROST = 0x7;

    /** Returns one of the values in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

    /**
     * Returns combination of the values in HvacFanDirection corresponding to a set of car zones.
     *
     * <p>The set of car zones represent the zones in which the associated feature can be regulated
     * together.
     */
    @HvacFanDirection
    public abstract @Nullable ImmutableMap<Set<CarZone>, Set<Integer>> getHvacFanDirection();


    /** Returns a map of min/max values for a property corresponding to a set of car zones.
     *
     * <p>The set of car zones represent the zones in which the associated feature can be regulated
     * together.
     */
    public abstract @Nullable ImmutableMap<Set<CarZone>, Pair<T, T>> getCarZoneSetsToMinMaxRange();

    /** Returns a pair of min and max values for the temperature set in Celsius.
     *
     * <p> Not all the values within this range may be supported in the car.
     * If getCelsiusIncrement() returns a non-null value, then Min/Max values combined with the
     * Celsius increment can be used to determine the supported temperature values.</p>
     */
    public abstract @Nullable Pair<Float, Float> getCelsiusRange();

    /** Returns a pair of min and max values for the temperature set in Fahrenheit.
     *
     * <p> Not all the values within this range may be supported in the car.
     * If getFahrenheitRange() returns a non-null value, then Min/Max values combined with the
     * Fahrenheit increment can be used to determine the supported temperature values.</p>
     */

    public abstract @Nullable Pair<Float, Float> getFahrenheitRange();

    /** Returns the increment value for the temperature set config in Celsius. */
    public abstract float getCelsiusIncrement();

    /** Returns the increment value for the temperature set config in Fahrenheit. */
    public abstract float getFahrenheitIncrement();

    /** Returns a list of set of {@link CarZone}s controlled together. */
    public abstract @NonNull ImmutableList<Set<CarZone>> getCarZones();

    /** Returns one of the values in {@link CarValue.StatusCode}. */
    public abstract @CarValue.StatusCode int getStatus();

    /** Gets a builder class for {@link CarPropertyProfile}. */
    public static <T> @NonNull Builder<T> builder() {
        return new AutoValue_CarPropertyProfile.Builder<T>()
                .setCarZones(Collections.singletonList(
                        Collections.singleton(CarZone.CAR_ZONE_GLOBAL)))
                .setCarZoneSetsToMinMaxRange(null)
                .setCelsiusRange(null)
                .setFahrenheitRange(null)
                .setCelsiusIncrement(-1f)
                .setFahrenheitIncrement(-1f)
                .setHvacFanDirection(null);
    }

    /**
     * A builder for {@link CarPropertyProfile}
     *
     * @param <T> is the type for all min/max values.
     */
    @AutoValue.Builder
    public abstract static class Builder<T> {
        /** Sets a property ID for the {@link CarPropertyProfile}. */
        public abstract @NonNull Builder<T> setPropertyId(int propertyId);

        /**
         * Sets the fan direction values grouped per car zone for the
         * {@link CarPropertyProfile}.
         */
        public abstract @NonNull Builder<T> setHvacFanDirection(@Nullable Map<Set<CarZone>,
                Set<@HvacFanDirection Integer>> hvacFanDirection);

        /**
         * Sets a status code for the {@link CarPropertyProfile}.
         */
        public abstract @NonNull Builder<T> setStatus(@CarValue.StatusCode int status);

        /** Sets a min/max range pair value for the {@link CarPropertyProfile}. */
        public abstract @NonNull Builder<T> setCarZoneSetsToMinMaxRange(
                @Nullable Map<Set<CarZone>, Pair<T, T>> minMaxRange);

        /** Sets a min/max range for temperature in Celsius. */
        public abstract @NonNull Builder<T> setCelsiusRange(
                @Nullable Pair<Float, Float> celsiusRange);

        /** Sets a min/max range for temperature in Fahrenheit. */
        public abstract @NonNull Builder<T> setFahrenheitRange(
                @Nullable Pair<Float, Float> fahrenheitRange);

        /** Sets the value of increment for temperature set config in Celsius. */
        public abstract @NonNull Builder<T> setCelsiusIncrement(
                float celsiusIncrement);

        /** Sets the value of increment for temperature set config in Fahrenheit. */
        public abstract @NonNull Builder<T> setFahrenheitIncrement(
                float fahrenheitIncrement);

        /** Sets the list of set of {@link CarZone}s for the {@link CarPropertyProfile}. */
        public abstract @NonNull Builder<T> setCarZones(
                @NonNull List<Set<CarZone>> carZones);

        /** Creates an instance of {@link CarPropertyProfile}. */
        public abstract @NonNull CarPropertyProfile<T> build();
    }
}
