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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

/**
 * Container class for information about property value and status.
 *
 * <p>{@link PropertyManager} uses it to give response to front-end components such as
 * {@link androidx.car.app.hardware.info.AutomotiveCarInfo}.
 *
 * @param <T> is the value type of response.
 * @hide
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

    /** Returns response's value. */
    @Nullable
    public abstract T getValue();


    /** Returns a list of {@link CarZone}s. */
    @NonNull
    public abstract ImmutableList<CarZone> getCarZones();

    /** Get a builder class for {@link CarPropertyResponse}*/
    @NonNull
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static <T> Builder<T> builder() {
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
        @NonNull
        public abstract Builder<T> setPropertyId(int propertyId);

        /** Sets a timestamp for the {@link CarPropertyResponse}. */
        @NonNull
        public abstract Builder<T> setTimestampMillis(long timestampMillis);

        /** Sets a value for the {@link CarPropertyResponse}. */
        @NonNull
        public abstract Builder<T> setValue(@Nullable T value);

        /** Sets a status code for the {@link CarPropertyResponse}. */
        @NonNull
        public abstract Builder<T> setStatus(@CarValue.StatusCode int status);

        /** Sets the list of {@link CarZone}s for the {@link CarPropertyResponse}. */
        @NonNull
        public abstract Builder<T> setCarZones(@NonNull List<CarZone> carZones);

        /** Create an instance of {@link CarPropertyResponse}. */
        @NonNull
        public abstract CarPropertyResponse<T> build();
    }
}
