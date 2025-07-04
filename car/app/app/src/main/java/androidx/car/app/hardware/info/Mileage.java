/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.car.app.hardware.info;

import static androidx.car.app.hardware.common.CarUnit.CarDistanceUnit;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Information about car mileage. */
@CarProtocol
@RequiresCarApi(3)
@KeepFields
public final class Mileage {
    // This field is named "meters"; however, this field actually stores the value in kilometers.
    // In order to not break backwards compatibility, we've kept the underlying field the same while
    // introducing a new public API method that reflects the actual units that are returned.
    private final @Nullable CarValue<Float> mOdometerMeters;

    private final @NonNull CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit;

    /**
     * Returns the value of the odometer from the car hardware in KILOMETERS.
     *
     * Despite the name "meters", this method actually returns the odometer in kilometers. It was
     * incorrectly introduced as meters and due to backwards compatibility concerns, can no longer
     * be renamed or removed.
     *
     * @deprecated use {@link #getOdometerInKilometers()} which does the same as this method, except
     * that the correct units are in the API name
     */
    @Deprecated
    public @NonNull CarValue<Float> getOdometerMeters() {
        return getOdometerInKilometers();
    }

    /**
     * Returns the value of the odometer from the car hardware in kilometers.
     *
     * The precision of this value depends on the hardware implementation. Some vehicles may return
     * whole kilomters, while others may return down to tenths or hundredths of a kilometer.
     */
    public @NonNull CarValue<Float> getOdometerInKilometers() {
        return requireNonNull(mOdometerMeters);
    }

    /**
     * Returns the distance display unit from the car hardware.
     *
     * <p>See {@link CarUnit} for possible distance values.
     */
    public @NonNull CarValue<@CarDistanceUnit Integer> getDistanceDisplayUnit() {
        return requireNonNull(mDistanceDisplayUnit);
    }

    @Override
    public @NonNull String toString() {
        return "[ odometer: "
                + getOdometerInKilometers()
                + ", distance display unit: "
                + mDistanceDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOdometerInKilometers(), mDistanceDisplayUnit);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Mileage)) {
            return false;
        }
        Mileage otherMileage = (Mileage) other;

        return Objects.equals(getOdometerInKilometers(), otherMileage.getOdometerInKilometers())
                && Objects.equals(mDistanceDisplayUnit, otherMileage.mDistanceDisplayUnit);
    }

    Mileage(Builder builder) {
        mOdometerMeters = requireNonNull(builder.mOdometerMeters);
        mDistanceDisplayUnit = requireNonNull(builder.mDistanceDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Mileage() {
        mOdometerMeters = CarValue.UNKNOWN_FLOAT;
        mDistanceDisplayUnit = CarValue.UNKNOWN_INTEGER;
    }

    /** A builder of {@link Mileage}. */
    public static final class Builder {
        CarValue<Float> mOdometerMeters = CarValue.UNKNOWN_FLOAT;
        CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit =
                CarValue.UNKNOWN_INTEGER;

        /**
         * Sets the odometer value in KILOMETERS.
         *
         * The API name is staying as "meters" in order to not break backwards compatibility.
         *
         * @throws NullPointerException if {@code odometer} is {@code null}
         */
        public @NonNull Builder setOdometerMeters(@NonNull CarValue<Float> odometerMeters) {
            mOdometerMeters = requireNonNull(odometerMeters);
            return this;
        }

        /**
         * Sets the mileage display unit.
         *
         * <p>Valid values are in {@link CarUnit}.
         *
         * @throws NullPointerException if {@code mileageDisplayUnit} is {@code null}
         */
        public @NonNull Builder setDistanceDisplayUnit(
                @NonNull CarValue<@CarDistanceUnit Integer> mileageDisplayUnit) {
            mDistanceDisplayUnit = requireNonNull(mileageDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link Mileage} defined by this builder.
         */
        public @NonNull Mileage build() {
            return new Mileage(this);
        }

    }
}
