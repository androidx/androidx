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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.hardware.common.CarValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Information about EV car port status */
@CarProtocol
@ExperimentalCarApi
@KeepFields
public class EvStatus {
    private final @NonNull CarValue<Boolean> mEvChargePortOpen;

    private final @NonNull CarValue<Boolean> mEvChargePortConnected;

    /**
     * Returns a {@link CarValue} to indicate if the EV charge port is open.
     */
    public @NonNull CarValue<Boolean> getEvChargePortOpen() {
        return requireNonNull(mEvChargePortOpen);
    }

    /**
     * Returns a {@link CarValue} to indicate if the EV charge port is connected.
     */
    public @NonNull CarValue<Boolean> getEvChargePortConnected() {
        return requireNonNull(mEvChargePortConnected);
    }

    @Override
    public @NonNull String toString() {
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
        mEvChargePortOpen = CarValue.UNKNOWN_BOOLEAN;
        mEvChargePortConnected = CarValue.UNKNOWN_BOOLEAN;
    }

    /**
     * A builder of {@link EvStatus}.
     */
    public static final class Builder {
        CarValue<Boolean> mEvChargePortOpen = CarValue.UNKNOWN_BOOLEAN;
        CarValue<Boolean> mEvChargePortConnected = CarValue.UNKNOWN_BOOLEAN;

        /**
         * Sets if the EV charge port is open with a {@link CarValue}.
         *
         * @throws NullPointerException if {@code evChargePortOpen} is {@code null}
         */
        public EvStatus.@NonNull Builder setEvChargePortOpen(
                @NonNull CarValue<Boolean> evChargePortOpen) {
            mEvChargePortOpen = requireNonNull(evChargePortOpen);
            return this;
        }

        /**
         * Sets if the EV charge port is connected with a {@link CarValue}. .
         *
         * @throws NullPointerException if {@code evChargePortConnected} is {@code null}
         */
        public EvStatus.@NonNull Builder setEvChargePortConnected(
                @NonNull CarValue<Boolean> evChargePortConnected) {
            mEvChargePortConnected = requireNonNull(evChargePortConnected);
            return this;
        }

        /**
         * Constructs the {@link EvStatus} defined by this builder.
         */
        public @NonNull EvStatus build() {
            return new EvStatus(this);
        }
    }
}
