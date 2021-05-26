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

/** Information about car specific gyroscopes available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class Gyroscope {

    /** Gyroscope request parameters. */
    public static final class Params {
        private final @UpdateRate.Value int mRate;

        /**
         * Construct gyroscope parameter instance.
         */
        public Params(@UpdateRate.Value int rate) {
            mRate = rate;
        }

        /** Gets the requested data rate for the gyroscope. */
        public @UpdateRate.Value int getRate() {
            return mRate;
        }

        /** Gets an {@link Gyroscope.Params} instance with default values set. */
        public static @NonNull Gyroscope.Params getDefault() {
            return new Params(UpdateRate.DEFAULT);
        }
    }

    @Keep
    @NonNull
    private final CarValue<Float[]> mGyroscope;

    /** Returns the raw gyroscope data from the car sensor. */
    @NonNull
    public CarValue<Float[]> getGyroscope() {
        return requireNonNull(mGyroscope);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ gyroscope: " + mGyroscope + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mGyroscope);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Gyroscope)) {
            return false;
        }
        Gyroscope otherGyroscope = (Gyroscope) other;

        return Objects.equals(mGyroscope, otherGyroscope.mGyroscope);
    }

    Gyroscope(Builder builder) {
        mGyroscope = requireNonNull(builder.mGyroscope);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Gyroscope() {
        mGyroscope = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
    }

    /** A builder of {@link Gyroscope}. */
    public static final class Builder {
        @Nullable
        CarValue<Float[]> mGyroscope;

        /**
         * Sets the raw gyroscope data.
         *
         * @throws NullPointerException if {@code gyroscope} is {@code null}
         */
        @NonNull
        public Builder setGyroscope(@NonNull CarValue<Float[]> gyroscope) {
            mGyroscope = requireNonNull(gyroscope);
            return this;
        }

        /**
         * Constructs the {@link Gyroscope} defined by this builder.
         *
         * <p>Any fields which have not been set are added with {@code null} value and
         * {@link CarValue#STATUS_UNIMPLEMENTED}.
         */
        @NonNull
        public Gyroscope build() {
            if (mGyroscope == null) {
                mGyroscope = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
            }
            return new Gyroscope(this);
        }
    }
}
