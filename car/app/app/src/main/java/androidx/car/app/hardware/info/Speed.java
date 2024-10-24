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

import static androidx.car.app.hardware.common.CarUnit.CarSpeedUnit;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Information about the current car speed.
 */
@CarProtocol
@RequiresCarApi(3)
@KeepFields
public final class Speed {
    private final @Nullable CarValue<Float> mRawSpeedMetersPerSecond;

    private final @Nullable CarValue<Float> mDisplaySpeedMetersPerSecond;

    private final @NonNull CarValue<@CarSpeedUnit Integer> mSpeedDisplayUnit;

    /**
     * Returns the raw speed of the car in meters/second.
     *
     * <p>The value is positive when the vehicle is moving forward, negative when moving
     * backwards and zero when stopped.
     */
    public @NonNull CarValue<Float> getRawSpeedMetersPerSecond() {
        return requireNonNull(mRawSpeedMetersPerSecond);
    }

    /**
     * Returns the display speed of the car in meters/second.
     *
     * <p>Some cars display a slightly slower speed than the actual speed. This is usually
     * displayed on the speedometer.
     *
     * <p>The value is positive when the vehicle is moving forward, negative when moving
     * backwards and zero when stopped.
     */
    public @NonNull CarValue<Float> getDisplaySpeedMetersPerSecond() {
        return requireNonNull(mDisplaySpeedMetersPerSecond);
    }

    /**
     * Returns the units used to display speed from the car settings.
     *
     * <p>See {@link CarUnit} for valid speed units.
     */
    public @NonNull CarValue<@CarSpeedUnit Integer> getSpeedDisplayUnit() {
        return requireNonNull(mSpeedDisplayUnit);
    }

    @Override
    public @NonNull String toString() {
        return "[ raw speed: "
                + getRawSpeedMetersPerSecond()
                + ", display speed: "
                + getDisplaySpeedMetersPerSecond()
                + ", speed display unit: "
                + mSpeedDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawSpeedMetersPerSecond(), getDisplaySpeedMetersPerSecond(),
                mSpeedDisplayUnit);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Speed)) {
            return false;
        }
        Speed otherSpeed = (Speed) other;

        return Objects.equals(getRawSpeedMetersPerSecond(), otherSpeed.getRawSpeedMetersPerSecond())
                && Objects.equals(getDisplaySpeedMetersPerSecond(),
                otherSpeed.getDisplaySpeedMetersPerSecond())
                && Objects.equals(mSpeedDisplayUnit, otherSpeed.mSpeedDisplayUnit);
    }

    Speed(Builder builder) {
        mRawSpeedMetersPerSecond = requireNonNull(builder.mRawSpeedMetersPerSecond);
        mDisplaySpeedMetersPerSecond = requireNonNull(builder.mDisplaySpeedMetersPerSecond);
        mSpeedDisplayUnit = requireNonNull(builder.mSpeedDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Speed() {
        mRawSpeedMetersPerSecond = CarValue.UNKNOWN_FLOAT;
        mDisplaySpeedMetersPerSecond = CarValue.UNKNOWN_FLOAT;
        mSpeedDisplayUnit = CarValue.UNKNOWN_INTEGER;
    }

    /** A builder of {@link Speed}. */
    public static final class Builder {
        CarValue<Float> mRawSpeedMetersPerSecond = CarValue.UNKNOWN_FLOAT;
        CarValue<Float> mDisplaySpeedMetersPerSecond = CarValue.UNKNOWN_FLOAT;
        CarValue<@CarSpeedUnit Integer> mSpeedDisplayUnit = CarValue.UNKNOWN_INTEGER;

        /**
         * Sets the raw speed in meters per second.
         *
         * @throws NullPointerException if {@code rawSpeedMetersPerSecond} is {@code null}
         */
        public @NonNull Builder setRawSpeedMetersPerSecond(
                @NonNull CarValue<Float> rawSpeedMetersPerSecond) {
            mRawSpeedMetersPerSecond = requireNonNull(rawSpeedMetersPerSecond);
            return this;
        }

        /**
         * Sets the display speed in meters per second. *
         *
         * @throws NullPointerException if {@code displaySpeedMetersPerSecond} is {@code null}
         */
        public @NonNull Builder setDisplaySpeedMetersPerSecond(
                @NonNull CarValue<Float> displaySpeedMetersPerSecond) {
            mDisplaySpeedMetersPerSecond = requireNonNull(displaySpeedMetersPerSecond);
            return this;
        }

        /**
         * Sets the units used to display speed from the car hardware settings.
         *
         * <p>See {@link CarUnit} for valid speed units.
         *
         * @throws NullPointerException if {@code speedDisplayUnit} is {@code null}
         */
        public @NonNull Builder setSpeedDisplayUnit(
                @NonNull CarValue<@CarSpeedUnit Integer> speedDisplayUnit) {
            mSpeedDisplayUnit = requireNonNull(speedDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link Speed} defined by this builder.
         */
        public @NonNull Speed build() {
            return new Speed(this);
        }
    }
}
