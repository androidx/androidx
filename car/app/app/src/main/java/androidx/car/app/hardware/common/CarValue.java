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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A data value object returned from car hardware with associated metadata including status,
 * timestamp, and the actual value.
 *
 * @param <T> data type which is returned by the {@link CarValue}
 */
@CarProtocol
@RequiresCarApi(3)
public final class CarValue<T> {
    /**
     * Defines the possible status codes when trying to access car hardware properties, sensors,
     * and actions.
     *
     * @hide
     */
    @IntDef({
            STATUS_UNKNOWN,
            STATUS_SUCCESS,
            STATUS_UNIMPLEMENTED,
            STATUS_UNAVAILABLE,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface StatusCode {
    }

    /**
     * {@link CarValue} has unknown status.
     */
    @StatusCode
    public static final int STATUS_UNKNOWN = 0;

    /**
     * {@link CarValue} was obtained successfully.
     */
    @StatusCode
    public static final int STATUS_SUCCESS = 1;

    /**
     * {@link CarValue} attempted for unimplemented property, sensor, or action.
     *
     * <p>For example, the car hardware might not be able to return a value such as speed or
     * energy level and will set the status to this value.
     */
    @StatusCode
    public static final int STATUS_UNIMPLEMENTED = 2;

    /**
     * {@link CarValue} attempted for unavailable property, sensor, or action.
     *
     * <p>For example, the car hardware might not be able to return a value such as climate at the
     * current time because the engine is off and will set the status to this value.
     */
    @StatusCode
    public static final int STATUS_UNAVAILABLE = 3;

    @Nullable
    private final T mValue;
    private final long mTimestampMillis;
    @StatusCode
    private final int mStatus;

    private static <T> CarValue<T> unimplemented() {
        return new CarValue<>(null, 0, CarValue.STATUS_UNIMPLEMENTED);
    }

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<Integer> UNIMPLEMENTED_INTEGER = unimplemented();

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<Boolean> UNIMPLEMENTED_BOOLEAN = unimplemented();

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<Float> UNIMPLEMENTED_FLOAT = unimplemented();

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<String> UNIMPLEMENTED_STRING = unimplemented();

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<Float[]> UNIMPLEMENTED_FLOAT_ARRAY = unimplemented();

    /** @hide */
    @RestrictTo(LIBRARY)
    public static final CarValue<Integer[]> UNIMPLEMENTED_INTEGER_ARRAY = unimplemented();

    /**
     * Returns a the data value or {@code null} if the status is not successful.
     *
     * @see #getStatus
     */
    @Nullable
    public T getValue() {
        return mValue;
    }

    /**
     * Returns the time in milliseconds at which the event happened.
     *
     * <p>For a given property, sensor, or action, each new value's timestamp should be
     * monotonically increasing using the same time base as SystemClock.elapsedRealtime().
     */
    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    /**
     * Returns the status of this particular result such as success, unavailable, or unimplemented.
     */
    @StatusCode
    public int getStatus() {
        return mStatus;
    }

    @Override
    @NonNull
    public String toString() {
        return "[value: "
                + mValue
                + ", timestamp: "
                + mTimestampMillis
                + ", Status: "
                + mStatus
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValue, mTimestampMillis, mStatus);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarValue<?>)) {
            return false;
        }
        CarValue<?> otherCarValue = (CarValue<?>) other;
        return Objects.equals(mValue, otherCarValue.mValue)
                && mTimestampMillis == otherCarValue.mTimestampMillis
                && mStatus == otherCarValue.mStatus;
    }

    /**
     * Constructs a new instance of a {@link CarValue}.
     *
     * @param value           data to be returned with the result
     * @param timestampMillis the time in milliseconds when the value was generated. See
     * {@link #getTimestampMillis}
     * @param status          the status code associated with this value
     */
    public CarValue(@Nullable T value, long timestampMillis, @StatusCode int status) {
        mValue = value;
        mTimestampMillis = timestampMillis;
        mStatus = status;
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarValue() {
        mValue = null;
        mTimestampMillis = 0;
        mStatus = STATUS_UNKNOWN;
    }
}
