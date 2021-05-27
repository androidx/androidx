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

/** Information about car specific compass available from the car hardware. */
@CarProtocol
@RequiresCarApi(3)
public final class Compass {

    /** Compass request parameters. */
    public static final class Params {
        private final @UpdateRate.Value int mRate;

        /**
         * Construct compass parameter instance.
         */
        public Params(@UpdateRate.Value int rate) {
            mRate = rate;
        }

        /** Gets the requested data rate for the compass. */
        public @UpdateRate.Value int getRate() {
            return mRate;
        }

        /** Gets an {@link Compass.Params} instance with default values set. */
        public static @NonNull Compass.Params getDefault() {
            return new Params(UpdateRate.DEFAULT);
        }
    }

    @Keep
    @NonNull
    private final CarValue<Float[]> mCompass;

    /**
     * Returns the raw compass data from the car sensor.
     *
     * <p>Follows the same format as {@link android.hardware.SensorEvent#values}.
     */
    @NonNull
    public CarValue<Float[]> getCompass() {
        return requireNonNull(mCompass);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ compass: " + mCompass + " ]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCompass);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Compass)) {
            return false;
        }
        Compass otherCompass = (Compass) other;

        return Objects.equals(mCompass, otherCompass.mCompass);
    }

    Compass(Builder builder) {
        mCompass = requireNonNull(builder.mCompass);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Compass() {
        mCompass = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
    }

    /** A builder of {@link Compass}. */
    public static final class Builder {
        @Nullable
        CarValue<Float[]> mCompass;

        /**
         * Sets the raw compass data.
         *
         * @throws NullPointerException if {@code compass} is {@code null}
         */
        @NonNull
        public Builder setCompass(@NonNull CarValue<Float[]> compass) {
            mCompass = requireNonNull(compass);
            return this;
        }

        /**
         * Constructs the {@link Compass} defined by this builder.
         *
         * <p>Any fields which have not been set are added with {@code null} value and
         * {@link CarValue#STATUS_UNIMPLEMENTED}.
         */
        @NonNull
        public Compass build() {
            if (mCompass == null) {
                mCompass = CarValue.UNIMPLEMENTED_FLOAT_ARRAY;
            }
            return new Compass(this);
        }
    }
}
