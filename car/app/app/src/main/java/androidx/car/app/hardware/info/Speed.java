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
import androidx.car.app.hardware.common.UpdateRate;

import java.util.Objects;

/**
 * Information about the current car speed.
 */
@CarProtocol
@RequiresCarApi(3)
public final class Speed {

    /**
     * Parameters for speed requests.
     */
    public static final class Params {
        private final @UpdateRate.Value int mRate;

        public Params(@UpdateRate.Value int rate) {
            mRate = rate;
        }

        public @UpdateRate.Value int getRate() {
            return mRate;
        }

        public static @NonNull Speed.Params getDefault() {
            return new Params(UpdateRate.DEFAULT);
        }
    }

    @Keep
    @NonNull
    private final CarValue<Float> mRawSpeed;

    @Keep
    @NonNull
    private final CarValue<Float> mDisplaySpeed;

    @Keep
    @NonNull
    private final CarValue<Integer> mSpeedDisplayUnit;

    /** Returns the raw speed of the car in meters/second. */
    @NonNull
    public CarValue<Float> getRawSpeed() {
        return requireNonNull(mRawSpeed);
    }

    /** Returns the display speed of the car in meters/second. */
    @NonNull
    public CarValue<Float> getDisplaySpeed() {
        return requireNonNull(mDisplaySpeed);
    }

    /**
     * Returns the units used to display speed from the car settings.
     *
     * <p>See {@link CarUnit} for valid speed units.
     */
    @NonNull
    public CarValue<Integer> getSpeedDisplayUnit() {
        return requireNonNull(mSpeedDisplayUnit);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ raw speed: "
                + mRawSpeed
                + ", display speed: "
                + mDisplaySpeed
                + ", speed display unit: "
                + mSpeedDisplayUnit
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRawSpeed, mDisplaySpeed, mSpeedDisplayUnit);
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

        return Objects.equals(mRawSpeed, otherSpeed.mRawSpeed)
                && Objects.equals(mDisplaySpeed, otherSpeed.mDisplaySpeed)
                && Objects.equals(mSpeedDisplayUnit, otherSpeed.mSpeedDisplayUnit);
    }

    Speed(Builder builder) {
        mRawSpeed = requireNonNull(builder.mRawSpeed);
        mDisplaySpeed = requireNonNull(builder.mDisplaySpeed);
        mSpeedDisplayUnit = requireNonNull(builder.mSpeedDisplayUnit);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Speed() {
        mRawSpeed = CarValue.UNIMPLEMENTED_FLOAT;
        mDisplaySpeed = CarValue.UNIMPLEMENTED_FLOAT;
        mSpeedDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link Speed}. */
    public static final class Builder {
        @Nullable
        CarValue<Float> mRawSpeed;
        @Nullable
        CarValue<Float> mDisplaySpeed;

        @Nullable
        CarValue<Integer> mSpeedDisplayUnit;

        /**
         * Sets the raw speed.
         *
         * @throws NullPointerException if {@code rawSpeed} is {@code null}
         */
        @NonNull
        public Builder setRawSpeed(@NonNull CarValue<Float> rawSpeed) {
            mRawSpeed = requireNonNull(rawSpeed);
            return this;
        }

        /**
         * Sets the display speed. *
         *
         * @throws NullPointerException if {@code displaySpeed} is {@code null}
         */
        @NonNull
        public Builder setDisplaySpeed(@NonNull CarValue<Float> displaySpeed) {
            mDisplaySpeed = requireNonNull(displaySpeed);
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
        public Builder setSpeedDisplayUnit(@NonNull CarValue<Integer> speedDisplayUnit) {
            mSpeedDisplayUnit = requireNonNull(speedDisplayUnit);
            return this;
        }

        /**
         * Constructs the {@link Speed} defined by this builder.
         *
         * <p>Any fields which have not been set are added with {@code null} value and
         * {@link CarValue#STATUS_UNIMPLEMENTED}.
         */
        @NonNull
        public Speed build() {
            if (mRawSpeed == null) {
                mRawSpeed = CarValue.UNIMPLEMENTED_FLOAT;
            }
            if (mDisplaySpeed == null) {
                mDisplaySpeed = CarValue.UNIMPLEMENTED_FLOAT;
            }
            if (mSpeedDisplayUnit == null) {
                mSpeedDisplayUnit = CarValue.UNIMPLEMENTED_INTEGER;
            }
            return new Speed(this);
        }
    }
}
