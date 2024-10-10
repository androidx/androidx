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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Container class for information about property value and status.
 *
 * <p>{@link PropertyManager} uses it to give response to front-end components such as
 * {@link androidx.car.app.hardware.info.AutomotiveCarInfo}.
 *
 * @param <T> is the value type of response.
 */
@RestrictTo(LIBRARY)
@AutoValue
public abstract class CarPropertyResponse<T> {

    /** Returns one of the values in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

    /** Returns one of the values in {@link CarValue.StatusCode}. */
    public abstract @CarValue.StatusCode int getStatus();

    /**
     * Returns the time in milliseconds at which the event happened.
     *
     * <p>For a given property, sensor, or action, each new response's timestamp should be
     * monotonically increasing using the same time base as SystemClock.elapsedRealtime().
     */
    public abstract long getTimestampMillis();

    /**
     * If {@link #getStatus()} is {@link CarValue#STATUS_SUCCESS}, then it return response's
     * {@code T} value. Otherwise, it returns {@code null}.
     */
    public abstract @Nullable T getValue();


    /** Returns a list of {@link CarZone}s. */
    public abstract @NonNull ImmutableList<CarZone> getCarZones();

    /** Get a builder class for {@link CarPropertyResponse}. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static <T> @NonNull Builder<T> builder() {
        return new AutoValue_CarPropertyResponse.Builder<T>()
                .setCarZones(Collections.singletonList(CarZone.CAR_ZONE_GLOBAL))
                .setValue(null)
                .setTimestampMillis(0);
    }

    /**
     * A builder for {@link CarPropertyResponse}
     *
     * @param <T> is the value type of {@link CarPropertyResponse#getValue()}
     */
    @AutoValue.Builder
    public abstract static class Builder<T> {
        /** Sets a property ID for the {@link CarPropertyResponse}. */
        public abstract @NonNull Builder<T> setPropertyId(int propertyId);

        /** Sets a timestamp for the {@link CarPropertyResponse}. */
        public abstract @NonNull Builder<T> setTimestampMillis(long timestampMillis);

        /**
         * Sets a value for the {@link CarPropertyResponse}.
         *
         * <p>If set with a non {@code null} value, then {@link #setStatus(int)} must be set with
         * {@link CarValue#STATUS_SUCCESS}. If this condition is not met, {@link #build()} will
         * throw an {@link IllegalArgumentException}.
         */
        public abstract @NonNull Builder<T> setValue(@Nullable T value);

        /**
         * Sets a status code for the {@link CarPropertyResponse}.
         *
         * <p>If status is set to {@link CarValue#STATUS_SUCCESS}, then {@link #setValue(Object)}
         * must be set with a non {@code null} value. If this condition is not met, {@link #build()}
         * will throw an {@link IllegalArgumentException}.
         */
        public abstract @NonNull Builder<T> setStatus(@CarValue.StatusCode int status);

        /** Sets the list of {@link CarZone}s for the {@link CarPropertyResponse}. */
        public abstract @NonNull Builder<T> setCarZones(@NonNull List<CarZone> carZones);

        /**
         * Package-private method used internally by {@link #build()}. Declaring this method
         * allows {@link #build()} to check {@link Preconditions}.
         */
        abstract @NonNull CarPropertyResponse<T> autoBuild();

        /**
         * Create an instance of {@link CarPropertyResponse}.
         *
         * @throws IllegalArgumentException If {@link #getStatus()} is not
         *                                  {@link CarValue#STATUS_SUCCESS} and {@link #getValue()}
         *                                  is not {@code null}, or {@link #getStatus()} is
         *                                  {@link CarValue#STATUS_SUCCESS} and {@link #getValue()}
         *                                  is {@code null}.
         */
        public final @NonNull CarPropertyResponse<T> build() {
            CarPropertyResponse<T> carPropertyResponse = autoBuild();
            Preconditions.checkState((carPropertyResponse.getStatus() == CarValue.STATUS_SUCCESS
                            && carPropertyResponse.getValue() != null) || (
                            carPropertyResponse.getStatus() != CarValue.STATUS_SUCCESS
                                    && carPropertyResponse.getValue() == null),
                    "Invalid status and value combo: " + carPropertyResponse);
            return carPropertyResponse;
        }
    }
}
