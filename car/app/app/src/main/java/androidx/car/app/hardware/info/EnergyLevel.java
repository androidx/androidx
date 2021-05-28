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

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/** Information of the energy (fuel and battery) levels from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class EnergyLevel {

    @Keep
    @NonNull
    private final CarValue<Float> mBatteryPercent;

    @Keep
    @NonNull
    private final CarValue<Float> mFuelPercent;

    @Keep
    @NonNull
    private final CarValue<Boolean> mEnergyIsLow;

    @Keep
    @NonNull
    private final CarValue<Float> mRangeRemaining;

    @Keep
    @NonNull
    private final CarValue<Integer> mDistanceDisplayUnit;

    /** Returns the battery percentage remaining from the car hardware. */
    @NonNull
    public CarValue<Float> getBatteryPercent() {
        return requireNonNull(mBatteryPercent);
    }

    /** Returns the fuel percentage remaining from the car hardware. */
    @NonNull
    public CarValue<Float> getFuelPercent() {
        return requireNonNull(mFuelPercent);
    }

    /** Returns if the remaining car energy is low from the car hardware. */
    @NonNull
    public CarValue<Boolean> getEnergyIsLow() {
        return requireNonNull(mEnergyIsLow);
    }

    /** Returns the range remaining from the car hardware in meters. */
    @NonNull
    public CarValue<Float> getRangeRemaining() {
        return requireNonNull(mRangeRemaining);
    }

    /**
     * Returns the distance display unit from the car hardware.
     *
     * <p>See {@link CarUnit} for possible distance values.
     */
    @NonNull
    public CarValue<Integer> getDistanceDisplayUnit() {
        return requireNonNull(mDistanceDisplayUnit);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ battery percent: "
                + mBatteryPercent
                + ", fuel percent: "
                + mFuelPercent
                + ", energyIsLow: "
                + mEnergyIsLow
                + ", range remaining: "
                + mRangeRemaining
                + ", distance display unit: "
                + mDistanceDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBatteryPercent, mFuelPercent, mEnergyIsLow, mRangeRemaining,
                mDistanceDisplayUnit);
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
                && Objects.equals(mRangeRemaining, otherEnergyLevel.mRangeRemaining)
                && Objects.equals(mDistanceDisplayUnit, otherEnergyLevel.mDistanceDisplayUnit);
    }

    EnergyLevel(Builder builder) {
        mBatteryPercent = requireNonNull(builder.mBatteryPercent);
        mFuelPercent = requireNonNull(builder.mFuelPercent);
        mEnergyIsLow = requireNonNull(builder.mEnergyIsLow);
        mRangeRemaining = requireNonNull(builder.mRangeRemaining);
        mDistanceDisplayUnit = requireNonNull(builder.mDistanceDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private EnergyLevel() {
        mBatteryPercent = CarValue.UNIMPLEMENTED_FLOAT;
        mFuelPercent = CarValue.UNIMPLEMENTED_FLOAT;
        mEnergyIsLow = CarValue.UNIMPLEMENTED_BOOLEAN;
        mRangeRemaining = CarValue.UNIMPLEMENTED_FLOAT;
        mDistanceDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link EnergyLevel}. */
    public static final class Builder {
        CarValue<Float> mBatteryPercent = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<Float> mFuelPercent = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<Boolean> mEnergyIsLow = CarValue.UNIMPLEMENTED_BOOLEAN;
        CarValue<Float> mRangeRemaining = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<Integer> mDistanceDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;

        /** Sets the remaining batter percentage. */
        @NonNull
        public Builder setBatteryPercent(@NonNull CarValue<Float> batteryPercent) {
            mBatteryPercent = requireNonNull(batteryPercent);
            return this;
        }

        /**
         * Sets the remaining fuel percentage.
         *
         * @throws NullPointerException if {@code fuelPercent} is {@code null}
         */
        @NonNull
        public Builder setFuelPercent(@NonNull CarValue<Float> fuelPercent) {
            mFuelPercent = requireNonNull(fuelPercent);
            return this;
        }

        /**
         * Sets if the remaining energy is low.
         *
         * @throws NullPointerException if {@code energyIsLow} is {@code null}
         */
        @NonNull
        public Builder setEnergyIsLow(@NonNull CarValue<Boolean> energyIsLow) {
            mEnergyIsLow = requireNonNull(energyIsLow);
            return this;
        }

        /**
         * Sets the range of the remaining fuel in meters.
         *
         * @throws NullPointerException if {@code rangeRemaining} is {@code null}
         */
        @NonNull
        public Builder setRangeRemaining(@NonNull CarValue<Float> rangeRemaining) {
            mRangeRemaining = requireNonNull(rangeRemaining);
            return this;
        }

        /**
         * Sets the distance display unit.
         *
         * <p>Valid values are in {@link CarUnit}.
         *
         * @throws NullPointerException if {@code distanceDisplayUnit} is {@code null}
         */
        @NonNull
        public Builder setDistanceDisplayUnit(@NonNull CarValue<Integer> distanceDisplayUnit) {
            mDistanceDisplayUnit = requireNonNull(distanceDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link EnergyLevel} defined by this builder.
         */
        @NonNull
        public EnergyLevel build() {
            return new EnergyLevel(this);
        }
    }
}
