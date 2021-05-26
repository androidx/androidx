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
import androidx.car.app.hardware.common.CarValue;
import androidx.car.app.hardware.common.UpdateRate;

import java.util.Objects;

/** Information about car specific accelerometers available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class Accelerometer {

    /** Accelerometer request parameters. */
    public static final class Params {
        private final @UpdateRate.Value int mRate;

        /**
         * Construct accelerometer parameter instance.
         */
        public Params(@UpdateRate.Value int rate) {
            mRate = rate;
        }

        /** Gets the requested data rate for the accelerometer. */
        public @UpdateRate.Value int getRate() {
            return mRate;
        }

        /** Gets an {@link Accelerometer.Params} instance with default values set. */
        public static @NonNull Accelerometer.Params getDefault() {
            return new Params(UpdateRate.DEFAULT);
        }
    }

    @Keep
    @NonNull
    private final CarValue<Float[]> mAccelerometer;

    /**
     * Returns the raw accelerometer data from the car sensor.
     *
     * <p>Follows the same format as {@link android.hardware.SensorEvent#values}.
     */
    @NonNull
    public CarValue<Float[]> getAccelerometer() {
        return requireNonNull(mAccelerometer);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ accelerometer: " + mAccelerometer + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAccelerometer);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Accelerometer)) {
            return false;
        }
        Accelerometer otherAccelerometer = (Accelerometer) other;

        return Objects.equals(mAccelerometer, otherAccelerometer.mAccelerometer);
    }

    Accelerometer(Builder builder) {
        mAccelerometer = requireNonNull(builder.mAccelerometer);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Accelerometer() {
        mAccelerometer = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
    }

    /** A builder of {@link Accelerometer}. */
    public static final class Builder {
        @Nullable
        CarValue<Float[]> mAccelerometer;

        /**
         * Sets the raw accelerometer data.
         *
         * @throws NullPointerException if {@code accelerometer} is {@code null}
         */
        @NonNull
        public Builder setAccelerometer(@NonNull CarValue<Float[]> accelerometer) {
            mAccelerometer = requireNonNull(accelerometer);
            return this;
        }

        /**
         * Constructs the {@link Accelerometer} defined by this builder.
         *
         * <p>Any fields which have not been set are added with {@code null} value and
         * {@link CarValue#STATUS_UNIMPLEMENTED}.
         */
        @NonNull
        public Accelerometer build() {
            if (mAccelerometer == null) {
                mAccelerometer = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
            }
            return new Accelerometer(this);
        }
    }
}
