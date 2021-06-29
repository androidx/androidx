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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/** Information about car mileage. */
@CarProtocol
@RequiresCarApi(3)
public final class Mileage {
    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.
    @Keep
    @Nullable
    private final CarValue<Float> mOdometer;

    @Keep
    @Nullable
    private final CarValue<Float> mOdometerMeters;

    @Keep
    @NonNull
    private final CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit;

    /** Returns the value of the odometer from the car hardware in meters. */
    @NonNull
    public CarValue<Float> getOdometerMeters() {
        if (mOdometerMeters != null) {
            return requireNonNull(mOdometerMeters);
        }
        return requireNonNull(mOdometer);
    }

    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

    /**
     * Returns the value of the odometer from the car hardware in meters.
     *
     * @deprecated use {@link #getOdometerMeters()}
     */
    @NonNull
    @Deprecated
    public CarValue<Float> getOdometer() {
        return getOdometerMeters();
    }

    /**
     * Returns the distance display unit from the car hardware.
     *
     * <p>See {@link CarUnit} for possible distance values.
     */
    @NonNull
    public CarValue<@CarDistanceUnit Integer> getDistanceDisplayUnit() {
        return requireNonNull(mDistanceDisplayUnit);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ odometer: "
                + getOdometerMeters()
                + ", distance display unit: "
                + mDistanceDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOdometerMeters(), mDistanceDisplayUnit);
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

        return Objects.equals(getOdometerMeters(), otherMileage.getOdometerMeters())
                && Objects.equals(mDistanceDisplayUnit, otherMileage.mDistanceDisplayUnit);
    }

    Mileage(Builder builder) {
        mOdometer = null;
        mOdometerMeters = requireNonNull(builder.mOdometerMeters);
        mDistanceDisplayUnit = requireNonNull(builder.mDistanceDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Mileage() {
        mOdometer = CarValue.UNIMPLEMENTED_FLOAT;
        mOdometerMeters = CarValue.UNIMPLEMENTED_FLOAT;
        mDistanceDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link Mileage}. */
    public static final class Builder {
        CarValue<Float> mOdometerMeters = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<@CarDistanceUnit Integer> mDistanceDisplayUnit =
                CarValue.UNIMPLEMENTED_INTEGER;

        // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

        /**
         * Sets the odometer value in meters.
         *
         * @throws NullPointerException if {@code odometer} is {@code null}
         * @deprecated use {@link #setOdometerMeters}
         */
        @NonNull
        @Deprecated
        public Builder setOdometer(@NonNull CarValue<Float> odometer) {
            mOdometerMeters = requireNonNull(odometer);
            return this;
        }

        /**
         * Sets the odometer value in meters.
         *
         * @throws NullPointerException if {@code odometer} is {@code null}
         */
        @NonNull
        public Builder setOdometerMeters(@NonNull CarValue<Float> odometerMeters) {
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
        @NonNull
        public Builder setDistanceDisplayUnit(
                @NonNull CarValue<@CarDistanceUnit Integer> mileageDisplayUnit) {
            mDistanceDisplayUnit = requireNonNull(mileageDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link Mileage} defined by this builder.
         */
        @NonNull
        public Mileage build() {
            return new Mileage(this);
        }

    }
}
