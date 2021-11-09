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

import android.car.hardware.CarPropertyValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.util.concurrent.TimeUnit;

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
    /**
     * Creates a response for {@link androidx.car.app.hardware.info.AutomotiveCarInfo}.
     *
     * @param propertyId        one of the values in {@link android.car.VehiclePropertyIds}.
     * @param status            one of the values in {@link CarValue.StatusCode}
     * @param timestampMillis   timestamp in milliseconds
     * @param value             the same value in {@link CarPropertyValue#getValue()}
     * @param <T>               the value type of {@link CarPropertyResponse}
     */
    @NonNull
    public static <T> CarPropertyResponse<T> create(int propertyId,
            @CarValue.StatusCode int status, long timestampMillis, @Nullable T value) {
        return new AutoValue_CarPropertyResponse<>(propertyId, status, timestampMillis,
                value);
    }

    /**
     * Creates a response from {@link CarPropertyValue}.
     *
     * @see #create(int, int, long, Object)
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> CarPropertyResponse<T> createFromPropertyValue(
            @NonNull CarPropertyValue<T> propertyValue) {
        int status = PropertyUtils.mapToStatusCodeInCarValue(propertyValue.getStatus());
        long timestamp = TimeUnit.MILLISECONDS.convert(propertyValue.getTimestamp(),
                TimeUnit.NANOSECONDS);
        return create(propertyValue.getPropertyId(), status, timestamp,
                propertyValue.getValue());
    }


    /**
     * Creates an error response. The timestamp is always 0 and the value is always {@code null}.
     *
     * @see #create(int, int, long, Object)
     */
    @NonNull
    public static <T> CarPropertyResponse<T> createErrorResponse(int propertyId,
            @CarValue.StatusCode int status) {
        return create(propertyId, status, 0, null);
    }

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
}
