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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarUnit;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/**
 * Information about the current car speed.
 */
@CarProtocol
@RequiresCarApi(3)
public final class Speed {
    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.
    @Keep
    @Nullable
    private final CarValue<Float> mRawSpeed;

    @Keep
    @Nullable
    private final CarValue<Float> mRawSpeedMetersPerSecond;

    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.
    @Keep
    @Nullable
    private final CarValue<Float> mDisplaySpeed;

    @Keep
    @Nullable
    private final CarValue<Float> mDisplaySpeedMetersPerSecond;

    @Keep
    @NonNull
    private final CarValue<@CarSpeedUnit Integer> mSpeedDisplayUnit;

    /**
     * Returns the raw speed of the car in meters/second.
     *
     * <p>The value is positive when the vehicle is moving forward, negative when moving
     * backwards and zero when stopped.
     */
    @NonNull
    public CarValue<Float> getRawSpeedMetersPerSecond() {
        if (mRawSpeedMetersPerSecond != null) {
            return requireNonNull(mRawSpeedMetersPerSecond);
        }
        return requireNonNull(mRawSpeed);
    }

    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

    /**
     * Returns the raw speed of the car in meters/second.
     *
     * <p>The value is positive when the vehicle is moving forward, negative when moving
     * backwards and zero when stopped.
     *
     * @deprecated use {@link #getRawSpeedMetersPerSecond()}
     */
    @NonNull
    @Deprecated
    public CarValue<Float> getRawSpeed() {
        return getRawSpeedMetersPerSecond();
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
    @NonNull
    public CarValue<Float> getDisplaySpeedMetersPerSecond() {
        if (mRawSpeedMetersPerSecond != null) {
            return requireNonNull(mDisplaySpeedMetersPerSecond);
        }
        return requireNonNull(mDisplaySpeed);
    }

    // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

    /**
     * Returns the display speed of the car in meters/second.
     *
     * <p>Some cars display a slightly slower speed than the actual speed. This is usually
     * displayed on the speedometer.
     *
     * <p>The value is positive when the vehicle is moving forward, negative when moving
     * backwards and zero when stopped.
     *
     * @deprecated use {@link #getDisplaySpeedMetersPerSecond()}
     */
    @NonNull
    @Deprecated
    public CarValue<Float> getDisplaySpeed() {
        return getDisplaySpeedMetersPerSecond();
    }

    /**
     * Returns the units used to display speed from the car settings.
     *
     * <p>See {@link CarUnit} for valid speed units.
     */
    @NonNull
    public CarValue<@CarSpeedUnit Integer> getSpeedDisplayUnit() {
        return requireNonNull(mSpeedDisplayUnit);
    }

    @Override
    @NonNull
    public String toString() {
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
        mRawSpeed = null;
        mRawSpeedMetersPerSecond = requireNonNull(builder.mRawSpeedMetersPerSecond);
        mDisplaySpeed = null;
        mDisplaySpeedMetersPerSecond = requireNonNull(builder.mDisplaySpeedMetersPerSecond);
        mSpeedDisplayUnit = requireNonNull(builder.mSpeedDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Speed() {
        mRawSpeed = null;
        mRawSpeedMetersPerSecond = CarValue.UNIMPLEMENTED_FLOAT;
        mDisplaySpeed = null;
        mDisplaySpeedMetersPerSecond = CarValue.UNIMPLEMENTED_FLOAT;
        mSpeedDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link Speed}. */
    public static final class Builder {
        CarValue<Float> mRawSpeedMetersPerSecond = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<Float> mDisplaySpeedMetersPerSecond = CarValue.UNIMPLEMENTED_FLOAT;
        CarValue<@CarSpeedUnit Integer> mSpeedDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;

        // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

        /**
         * Sets the raw speed in meters per second.
         *
         * @throws NullPointerException if {@code rawSpeedMetersPerSecond} is {@code null}
         * @deprecated use {@link #setRawSpeedMetersPerSecond}
         */
        @Deprecated
        @NonNull
        public Builder setRawSpeed(@NonNull CarValue<Float> rawSpeed) {
            mRawSpeedMetersPerSecond = requireNonNull(rawSpeed);
            return this;
        }

        /**
         * Sets the raw speed in meters per second.
         *
         * @throws NullPointerException if {@code rawSpeedMetersPerSecond} is {@code null}
         */
        @NonNull
        public Builder setRawSpeedMetersPerSecond(
                @NonNull CarValue<Float> rawSpeedMetersPerSecond) {
            mRawSpeedMetersPerSecond = requireNonNull(rawSpeedMetersPerSecond);
            return this;
        }

        // TODO(b/192106888): Remove when new values fully supported by Android Auto Host.

        /**
         * Sets the display speed in meters per second. *
         *
         * @throws NullPointerException if {@code displaySpeedMetersPerSecond} is {@code null}
         * @deprecated use {@link #setDisplaySpeedMetersPerSecond}
         */
        @Deprecated
        @NonNull
        public Builder setDisplaySpeed(@NonNull CarValue<Float> displaySpeed) {
            mDisplaySpeedMetersPerSecond = requireNonNull(displaySpeed);
            return this;
        }

        /**
         * Sets the display speed in meters per second. *
         *
         * @throws NullPointerException if {@code displaySpeedMetersPerSecond} is {@code null}
         */
        @NonNull
        public Builder setDisplaySpeedMetersPerSecond(
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
        @NonNull
        public Builder setSpeedDisplayUnit(
                @NonNull CarValue<@CarSpeedUnit Integer> speedDisplayUnit) {
            mSpeedDisplayUnit = requireNonNull(speedDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link Speed} defined by this builder.
         */
        @NonNull
        public Speed build() {
            return new Speed(this);
        }
    }
}
