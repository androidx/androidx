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
import static androidx.car.app.hardware.common.CarUnit.CarVolumeUnit;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Information of the energy (fuel and battery) levels from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
@KeepFields
public final class EnergyLevel {
    private final @NonNull CarValue<Float> mBatteryPercent;

    private final @NonNull CarValue<Float> mFuelPercent;

    private final @NonNull CarValue<Boolean> mEnergyIsLow;

    private final @Nullable CarValue<Float> mRangeRemainingMeters;

    private final @NonNull CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit;

    private final @NonNull CarValue<@CarVolumeUnit Integer> mFuelVolumeDisplayUnit;

    /** Returns the battery percentage remaining from the car hardware. */
    public @NonNull CarValue<Float> getBatteryPercent() {
        return requireNonNull(mBatteryPercent);
    }

    /** Returns the fuel percentage remaining from the car hardware. */
    public @NonNull CarValue<Float> getFuelPercent() {
        return requireNonNull(mFuelPercent);
    }

    /** Returns if the remaining car energy is low from the car hardware. */
    public @NonNull CarValue<Boolean> getEnergyIsLow() {
        return requireNonNull(mEnergyIsLow);
    }

    /** Returns the range remaining from the car hardware in meters. */
    public @NonNull CarValue<Float> getRangeRemainingMeters() {
        return requireNonNull(mRangeRemainingMeters);
    }

    /**
     * Returns the distance display unit from the car hardware.
     *
     * <p>See {@link CarUnit} for possible distance values.
     */
    public @NonNull CarValue<@CarDistanceUnit Integer> getDistanceDisplayUnit() {
        return requireNonNull(mDistanceDisplayUnit);
    }

    /**
     * Returns the fuel volume display unit from the car hardware.
     *
     * <p>See {@link CarUnit} for possible volume values.
     */
    // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
    @ExperimentalCarApi
    public @NonNull CarValue<@CarVolumeUnit Integer> getFuelVolumeDisplayUnit() {
        return requireNonNull(mFuelVolumeDisplayUnit);
    }

    @Override
    public @NonNull String toString() {
        return "[ battery percent: "
                + mBatteryPercent
                + ", fuel percent: "
                + mFuelPercent
                + ", energyIsLow: "
                + mEnergyIsLow
                + ", range remaining: "
                + getRangeRemainingMeters()
                + ", distance display unit: "
                + mDistanceDisplayUnit
                + ", fuel volume display unit: "
                + mFuelVolumeDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBatteryPercent, mFuelPercent, mEnergyIsLow, getRangeRemainingMeters(),
                mDistanceDisplayUnit, mFuelVolumeDisplayUnit);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnergyLevel)) {
            return false;
        }
        EnergyLevel otherEnergyLevel = (EnergyLevel) other;

        return Objects.equals(mBatteryPercent, otherEnergyLevel.mBatteryPercent)
                && Objects.equals(mFuelPercent, otherEnergyLevel.mFuelPercent)
                && Objects.equals(mEnergyIsLow, otherEnergyLevel.mEnergyIsLow)
                && Objects.equals(getRangeRemainingMeters(),
                otherEnergyLevel.getRangeRemainingMeters())
                && Objects.equals(mDistanceDisplayUnit, otherEnergyLevel.mDistanceDisplayUnit)
                && Objects.equals(mFuelVolumeDisplayUnit, otherEnergyLevel.mFuelVolumeDisplayUnit);
    }

    EnergyLevel(Builder builder) {
        mBatteryPercent = requireNonNull(builder.mBatteryPercent);
        mFuelPercent = requireNonNull(builder.mFuelPercent);
        mEnergyIsLow = requireNonNull(builder.mEnergyIsLow);
        mRangeRemainingMeters = requireNonNull(builder.mRangeRemainingMeters);
        mDistanceDisplayUnit = requireNonNull(builder.mDistanceDisplayUnit);
        mFuelVolumeDisplayUnit = requireNonNull(builder.mFuelVolumeDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private EnergyLevel() {
        mBatteryPercent = CarValue.UNKNOWN_FLOAT;
        mFuelPercent = CarValue.UNKNOWN_FLOAT;
        mEnergyIsLow = CarValue.UNKNOWN_BOOLEAN;
        mRangeRemainingMeters = CarValue.UNKNOWN_FLOAT;
        mDistanceDisplayUnit = CarValue.UNKNOWN_INTEGER;
        mFuelVolumeDisplayUnit = CarValue.UNKNOWN_INTEGER;
    }

    /** A builder of {@link EnergyLevel}. */
    public static final class Builder {
        CarValue<Float> mBatteryPercent = CarValue.UNKNOWN_FLOAT;
        CarValue<Float> mFuelPercent = CarValue.UNKNOWN_FLOAT;
        CarValue<Boolean> mEnergyIsLow = CarValue.UNKNOWN_BOOLEAN;
        CarValue<Float> mRangeRemainingMeters = CarValue.UNKNOWN_FLOAT;
        CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit =
                CarValue.UNKNOWN_INTEGER;
        CarValue<@CarVolumeUnit Integer> mFuelVolumeDisplayUnit =
                CarValue.UNKNOWN_INTEGER;

        /** Sets the remaining batter percentage. */
        public @NonNull Builder setBatteryPercent(@NonNull CarValue<Float> batteryPercent) {
            mBatteryPercent = requireNonNull(batteryPercent);
            return this;
        }

        /**
         * Sets the remaining fuel percentage.
         *
         * @throws NullPointerException if {@code fuelPercent} is {@code null}
         */
        public @NonNull Builder setFuelPercent(@NonNull CarValue<Float> fuelPercent) {
            mFuelPercent = requireNonNull(fuelPercent);
            return this;
        }

        /**
         * Sets if the remaining energy is low.
         *
         * @throws NullPointerException if {@code energyIsLow} is {@code null}
         */
        public @NonNull Builder setEnergyIsLow(@NonNull CarValue<Boolean> energyIsLow) {
            mEnergyIsLow = requireNonNull(energyIsLow);
            return this;
        }

        /**
         * Sets the range of the remaining fuel in meters.
         *
         * @throws NullPointerException if {@code rangeRemaining} is {@code null}
         */
        public @NonNull Builder setRangeRemainingMeters(
                @NonNull CarValue<Float> rangeRemainingMeters) {
            mRangeRemainingMeters = requireNonNull(rangeRemainingMeters);
            return this;
        }

        /**
         * Sets the distance display unit.
         *
         * <p>Valid values are in {@link CarUnit}.
         *
         * @throws NullPointerException if {@code distanceDisplayUnit} is {@code null}
         */
        public @NonNull Builder setDistanceDisplayUnit(
                @NonNull CarValue<@CarDistanceUnit Integer> distanceDisplayUnit) {
            mDistanceDisplayUnit = requireNonNull(distanceDisplayUnit);
            return this;
        }

        /**
         * Sets the fuel volume display unit.
         *
         * @throws NullPointerException if {@code fuelVolumeDisplayUnit} is {@code null}
         */
        // TODO(b/202303614): Remove this annotation once FuelVolumeDisplayUnit is ready.
        @ExperimentalCarApi
        public @NonNull Builder setFuelVolumeDisplayUnit(@NonNull CarValue<@CarVolumeUnit Integer>
                fuelVolumeDisplayUnit) {
            mFuelVolumeDisplayUnit = requireNonNull(fuelVolumeDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link EnergyLevel} defined by this builder.
         */
        public @NonNull EnergyLevel build() {
            return new EnergyLevel(this);
        }
    }
}
