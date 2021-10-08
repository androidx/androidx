/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.navigation.model;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.model.constraints.CarColorConstraints;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Represents the travel estimates to a destination of a trip or for a trip segment, including the
 * remaining time and distance to the destination.
 */
@SuppressWarnings("MissingSummary")
@CarProtocol
public final class TravelEstimate {
    /** A value used to represent an unknown remaining amount of time. */
    public static final long REMAINING_TIME_UNKNOWN = -1L;

    @Keep
    @Nullable
    private final Distance mRemainingDistance;
    @Keep
    private final long mRemainingTimeSeconds;
    @Keep
    @Nullable
    private final DateTimeWithZone mArrivalTimeAtDestination;
    @Keep
    private final CarColor mRemainingTimeColor;
    @Keep
    private final CarColor mRemainingDistanceColor;

    /**
     * Returns the remaining {@link Distance} until arriving at the destination,  or {@code null}
     * if not set.
     *
     * @see Builder#Builder(Distance, DateTimeWithZone)
     */
    @Nullable
    public Distance getRemainingDistance() {
        return mRemainingDistance;
    }

    /**
     * Returns the remaining time until arriving at the destination, in seconds.
     *
     * @see Builder#setRemainingTimeSeconds(long)
     */
    @SuppressWarnings("MethodNameUnits")
    public long getRemainingTimeSeconds() {
        return mRemainingTimeSeconds >= 0 ? mRemainingTimeSeconds : REMAINING_TIME_UNKNOWN;
    }

    /**
     * Returns the arrival time until at the destination or {@code null} if not set.
     *
     * @see Builder#Builder(Distance, DateTimeWithZone)
     */
    @Nullable
    public DateTimeWithZone getArrivalTimeAtDestination() {
        return mArrivalTimeAtDestination;
    }

    /**
     * Sets the color of the remaining time text or {@code null} if not set.
     *
     * @see Builder#setRemainingTimeColor(CarColor)
     */
    @Nullable
    public CarColor getRemainingTimeColor() {
        return mRemainingTimeColor;
    }

    /**
     * Sets the color of the remaining distance text or {@code null} if not set.
     *
     * @see Builder#setRemainingDistanceColor(CarColor)
     */
    @Nullable
    public CarColor getRemainingDistanceColor() {
        return mRemainingDistanceColor;
    }

    @Override
    @NonNull
    public String toString() {
        return "[ remaining distance: "
                + mRemainingDistance
                + ", time (s): " + mRemainingTimeSeconds
                + ", ETA: " + mArrivalTimeAtDestination
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mRemainingDistance,
                mRemainingTimeSeconds,
                mArrivalTimeAtDestination,
                mRemainingTimeColor,
                mRemainingDistanceColor);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TravelEstimate)) {
            return false;
        }
        TravelEstimate otherInfo = (TravelEstimate) other;

        return Objects.equals(mRemainingDistance, otherInfo.mRemainingDistance)
                && mRemainingTimeSeconds == otherInfo.mRemainingTimeSeconds
                && Objects.equals(mArrivalTimeAtDestination, otherInfo.mArrivalTimeAtDestination)
                && Objects.equals(mRemainingTimeColor, otherInfo.mRemainingTimeColor)
                && Objects.equals(mRemainingDistanceColor, otherInfo.mRemainingDistanceColor);
    }

    /** Constructs an empty instance, used by serialization code. */
    private TravelEstimate() {
        mRemainingDistance = null;
        mRemainingTimeSeconds = 0;
        mArrivalTimeAtDestination = null;
        mRemainingTimeColor = CarColor.DEFAULT;
        mRemainingDistanceColor = CarColor.DEFAULT;
    }

    TravelEstimate(Builder builder) {
        mRemainingDistance = builder.mRemainingDistance;
        mRemainingTimeSeconds = builder.mRemainingTimeSeconds;
        mArrivalTimeAtDestination = builder.mArrivalTimeAtDestination;
        mRemainingTimeColor = builder.mRemainingTimeColor;
        mRemainingDistanceColor = builder.mRemainingDistanceColor;
    }

    /** A builder of {@link TravelEstimate}. */
    public static final class Builder {
        final Distance mRemainingDistance;
        long mRemainingTimeSeconds = REMAINING_TIME_UNKNOWN;
        final DateTimeWithZone mArrivalTimeAtDestination;
        CarColor mRemainingTimeColor = CarColor.DEFAULT;
        CarColor mRemainingDistanceColor = CarColor.DEFAULT;

        /**
         * Constructs a new builder of {@link TravelEstimate}.
         *
         * @param remainingDistance        The estimated remaining {@link Distance} until
         *                                 arriving at the destination
         * @param arrivalTimeAtDestination The arrival time with the time zone information
         *                                 provided for the destination
         * @throws NullPointerException if {@code remainingDistance} or
         *                              {@code arrivalTimeAtDestination} are {@code null}
         */
        public Builder(
                @NonNull Distance remainingDistance,
                @NonNull DateTimeWithZone arrivalTimeAtDestination) {
            mRemainingDistance = requireNonNull(remainingDistance);
            mArrivalTimeAtDestination = requireNonNull(arrivalTimeAtDestination);
        }

        /**
         * Constructs a new builder of {@link TravelEstimate}.
         *
         * @param remainingDistance        The estimated remaining {@link Distance} until
         *                                 arriving at the destination
         * @param arrivalTimeAtDestination The arrival time with the time zone information
         *                                 provided for the destination
         * @throws NullPointerException if {@code remainingDistance} or
         *                              {@code arrivalTimeAtDestination} are {@code null}
         */
        @RequiresApi(26)
        @SuppressWarnings("AndroidJdkLibsChecker")
        public Builder(
                @NonNull Distance remainingDistance,
                @NonNull ZonedDateTime arrivalTimeAtDestination) {
            mRemainingDistance = requireNonNull(remainingDistance);
            mArrivalTimeAtDestination =
                    DateTimeWithZone.create(requireNonNull(arrivalTimeAtDestination));
        }

        /**
         * Sets the estimated time remaining until arriving at the destination, in seconds.
         *
         * <p>If not set, {@link #REMAINING_TIME_UNKNOWN} will be used.
         *
         * <p>Note that {@link #REMAINING_TIME_UNKNOWN} may not be supported depending on where the
         * {@link TravelEstimate} is used. See the documentation of where {@link TravelEstimate}
         * is used for any restrictions that might apply.
         *
         * @throws IllegalArgumentException if {@code remainingTimeSeconds} is a negative value
         *                                  but not {@link #REMAINING_TIME_UNKNOWN}
         */
        @NonNull
        public Builder setRemainingTimeSeconds(@IntRange(from = -1) long remainingTimeSeconds) {
            mRemainingTimeSeconds = validateRemainingTime(remainingTimeSeconds);
            return this;
        }

        /**
         * Sets the estimated time remaining until arriving at the destination.
         *
         * <p>If not set, {@link #REMAINING_TIME_UNKNOWN} will be used.
         *
         * @throws IllegalArgumentException if {@code remainingTime} is a negative duration
         *                                  but not {@link #REMAINING_TIME_UNKNOWN}
         * @throws NullPointerException     if {@code remainingTime} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder"})
        @RequiresApi(26)
        @NonNull
        public Builder setRemainingTime(@NonNull Duration remainingTime) {
            return Api26Impl.setRemainingTime(this, remainingTime);
        }

        /**
         * Sets the color of the remaining time text.
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         * factors, the color may be ignored by the host or overridden by the vehicle system.
         *
         * <p>If not set, {@link CarColor#DEFAULT} will be used.
         *
         * <p>Custom colors created with {@link CarColor#createCustom} are not supported.
         *
         * @throws IllegalArgumentException if {@code remainingTimeColor} is not supported
         * @throws NullPointerException     if {@code remainingTimecolor} is {@code null}
         */
        @NonNull
        public Builder setRemainingTimeColor(@NonNull CarColor remainingTimeColor) {
            CarColorConstraints.STANDARD_ONLY.validateOrThrow(requireNonNull(remainingTimeColor));
            mRemainingTimeColor = remainingTimeColor;
            return this;
        }

        /**
         * Sets the color of the remaining distance text.
         *
         * <p>Depending on contrast requirements, capabilities of the vehicle screens, or other
         * factors, the color may be ignored by the host or overridden by the vehicle system.
         *
         * <p>If not set, {@link CarColor#DEFAULT} will be used.
         *
         * <p>Custom colors created with {@link CarColor#createCustom} are not supported.
         *
         * @throws IllegalArgumentException if {@code remainingDistanceColor} is not supported
         * @throws NullPointerException     if {@code remainingDistanceColor} is {@code null}
         */
        @NonNull
        public Builder setRemainingDistanceColor(@NonNull CarColor remainingDistanceColor) {
            CarColorConstraints.STANDARD_ONLY.validateOrThrow(
                    requireNonNull(remainingDistanceColor));
            mRemainingDistanceColor = remainingDistanceColor;
            return this;
        }

        /** Constructs the {@link TravelEstimate} defined by this builder. */
        @NonNull
        public TravelEstimate build() {
            return new TravelEstimate(this);
        }

        static long validateRemainingTime(long remainingTimeSeconds) {
            if (remainingTimeSeconds < 0 && remainingTimeSeconds != REMAINING_TIME_UNKNOWN) {
                throw new IllegalArgumentException(
                        "Remaining time must be a larger than or equal to zero, or set to"
                                + " REMAINING_TIME_UNKNOWN");
            }
            return remainingTimeSeconds;
        }

        /**
         * Version-specific static inner class to avoid verification errors that negatively affect
         * run-time performance.
         */
        @RequiresApi(26)
        private static final class Api26Impl {
            private Api26Impl() {
            }

            @DoNotInline
            @NonNull
            public static Builder setRemainingTime(Builder builder,
                    @NonNull Duration remainingTime) {
                requireNonNull(remainingTime);
                builder.mRemainingTimeSeconds =
                        Builder.validateRemainingTime(remainingTime.getSeconds());
                return builder;
            }
        }
    }
}
