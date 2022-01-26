/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.hardware.common.CarValue;

import java.util.Objects;

/** Information about EV car port status */
@CarProtocol
@ExperimentalCarApi
public class EvStatus {

    @Keep
    @NonNull
    private final CarValue<Boolean> mEvChargePortOpen;

    @Keep
    @NonNull
    private final CarValue<Boolean> mEvChargePortConnected;

    /**
     * Returns a {@link CarValue} to indicate if the EV charge port is open.
     */
    @NonNull
    public CarValue<Boolean> getEvChargePortOpen() {
        return requireNonNull(mEvChargePortOpen);
    }

    /**
     * Returns a {@link CarValue} to indicate if the EV charge port is connected.
     */
    @NonNull
    public CarValue<Boolean> getEvChargePortConnected() {
        return requireNonNull(mEvChargePortConnected);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ EV charge port open: " + mEvChargePortOpen + ", EV charge port connected: "
                + mEvChargePortConnected + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEvChargePortOpen, mEvChargePortConnected);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EvStatus)) {
            return false;
        }
        EvStatus otherEvStatus = (EvStatus) other;

        return Objects.equals(mEvChargePortConnected, otherEvStatus.mEvChargePortConnected)
                && Objects.equals(mEvChargePortOpen, otherEvStatus.mEvChargePortOpen);
    }

    EvStatus(Builder builder) {
        mEvChargePortConnected = builder.mEvChargePortConnected;
        mEvChargePortOpen = builder.mEvChargePortOpen;
    }

    /**
     * Constructs an empty instance, used by serialization code.
     */
    private EvStatus() {
        mEvChargePortOpen = CarValue.UNIMPLEMENTED_BOOLEAN;
        mEvChargePortConnected = CarValue.UNIMPLEMENTED_BOOLEAN;
    }

    /**
     * A builder of {@link EvStatus}.
     */
    public static final class Builder {
        CarValue<Boolean> mEvChargePortOpen = CarValue.UNIMPLEMENTED_BOOLEAN;
        CarValue<Boolean> mEvChargePortConnected = CarValue.UNIMPLEMENTED_BOOLEAN;

        /**
         * Sets if the EV charge port is open with a {@link CarValue}.
         *
         * @throws NullPointerException if {@code evChargePortOpen} is {@code null}
         */
        @NonNull
        public EvStatus.Builder setEvChargePortOpen(@NonNull CarValue<Boolean> evChargePortOpen) {
            mEvChargePortOpen = requireNonNull(evChargePortOpen);
            return this;
        }

        /**
         * Sets if the EV charge port is connected with a {@link CarValue}. .
         *
         * @throws NullPointerException if {@code evChargePortConnected} is {@code null}
         */
        @NonNull
        public EvStatus.Builder setEvChargePortConnected(
                @NonNull CarValue<Boolean> evChargePortConnected) {
            mEvChargePortConnected = requireNonNull(evChargePortConnected);
            return this;
        }

        /**
         * Constructs the {@link EvStatus} defined by this builder.
         */
        @NonNull
        public EvStatus build() {
            return new EvStatus(this);
        }
    }
}
