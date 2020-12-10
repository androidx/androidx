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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
     * Returns a new instance of a {@link TravelEstimate} for the given time and distance
     * parameters.
     *
     * @param remainingDistance        The estimated remaining {@link Distance} until arriving at
     *                                 the destination.
     * @param remainingTimeSeconds     The estimated time remaining until arriving at the
     *                                 destination, in seconds, or {@link #REMAINING_TIME_UNKNOWN}.
     * @param arrivalTimeAtDestination The arrival time with the time zone information provided
     *                                 for the destination.
     * @throws IllegalArgumentException if {@code remainingTimeSeconds} is a negative value.
     * @throws NullPointerException     if {@code remainingDistance} is {@code null}
     * @throws NullPointerException     if {@code arrivalTimeAtDestination} is {@code null}
     */
    @NonNull
    public static TravelEstimate create(
            @NonNull Distance remainingDistance,
            long remainingTimeSeconds,
            @NonNull DateTimeWithZone arrivalTimeAtDestination) {
        return builder(remainingDistance, arrivalTimeAtDestination).setRemainingTimeSeconds(
                remainingTimeSeconds).build();
    }

    /**
     * Returns a new instance of a {@link TravelEstimate} for the given time and distance
     * parameters.
     *
     * @param remainingDistance        The estimated remaining {@link Distance} until arriving at
     *                                 the destination.
     * @param remainingTime            The estimated time remaining until arriving at the
     *                                 destination, or {@code Duration.ofSeconds
     *                                 (REMAINING_TIME_UNKNOWN)}.
     * @param arrivalTimeAtDestination The arrival time with the time zone information provided for
     *                                 the destination.
     * @throws IllegalArgumentException if {@code remainingTime} contains a negative duration.
     * @throws NullPointerException     if {@code remainingDistance} is {@code null}
     * @throws NullPointerException     if {@code remainingTime} is {@code null}
     * @throws NullPointerException     if {@code arrivalTimeAtDestination} is {@code null}
     */
    @RequiresApi(26)
    @SuppressWarnings("AndroidJdkLibsChecker")
    @NonNull
    public static TravelEstimate create(
            @NonNull Distance remainingDistance,
            @NonNull Duration remainingTime,
            @NonNull ZonedDateTime arrivalTimeAtDestination) {
        return builder(remainingDistance, arrivalTimeAtDestination).setRemainingTime(
                remainingTime).build();
    }

    /**
     * Constructs a new builder of {@link TravelEstimate}.
     *
     * @param remainingDistance        The estimated remaining {@link Distance} until arriving at
     *                                 the destination.
     * @param arrivalTimeAtDestination The arrival time with the time zone information provided
     *                                 for the destination.
     * @throws NullPointerException if {@code remainingDistance} is {@code null}
     * @throws NullPointerException if {@code arrivalTimeAtDestination} is {@code null}
     */
    @NonNull
    public static Builder builder(
            @NonNull Distance remainingDistance,
            @NonNull DateTimeWithZone arrivalTimeAtDestination) {
        return new Builder(
                requireNonNull(remainingDistance),
                requireNonNull(arrivalTimeAtDestination));
    }

    /**
     * Constructs a new builder of {@link TravelEstimate}.
     *
     * @param remainingDistance        The estimated remaining {@link Distance} until arriving at
     *                                 the destination.
     * @param arrivalTimeAtDestination The arrival time with the time zone information provided for
     *                                 the destination.
     * @throws NullPointerException if {@code remainingDistance} is {@code null}
     * @throws NullPointerException if {@code arrivalTimeAtDestination} is {@code null}
     */
    @NonNull
    @RequiresApi(26)
    @SuppressWarnings("AndroidJdkLibsChecker")
    public static Builder builder(
            @NonNull Distance remainingDistance,
            @NonNull ZonedDateTime arrivalTimeAtDestination) {
        return new Builder(
                requireNonNull(remainingDistance),
                requireNonNull(arrivalTimeAtDestination));
    }

    @NonNull
    public Distance getRemainingDistance() {
        return requireNonNull(mRemainingDistance);
    }

    // TODO(rampara): Returned time values must be in milliseconds
    @SuppressWarnings("MethodNameUnits")
    public long getRemainingTimeSeconds() {
        return mRemainingTimeSeconds >= 0 ? mRemainingTimeSeconds : REMAINING_TIME_UNKNOWN;
    }

    @Nullable
    public DateTimeWithZone getArrivalTimeAtDestination() {
        return mArrivalTimeAtDestination;
    }

    @NonNull
    public CarColor getRemainingTimeColor() {
        return mRemainingTimeColor;
    }

    @NonNull
    public CarColor getRemainingDistanceColor() {
        return mRemainingDistanceColor;
    }

    @SuppressLint("UnsafeNewApiCall")
    // TODO(rampara): Move API 26 calls into separate class.
    @Override
    @NonNull
    @RequiresApi(26)
    @SuppressWarnings("AndroidJdkLibsChecker")
    public String toString() {
        return "[ remaining distance: "
                + mRemainingDistance
                + ", time: "
                + Duration.ofSeconds(mRemainingTimeSeconds)
                + ", ETA: "
                + mArrivalTimeAtDestination
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

    private TravelEstimate(Builder builder) {
        this.mRemainingDistance = builder.mRemainingDistance;
        this.mRemainingTimeSeconds = builder.mRemainingTimeSeconds;
        this.mArrivalTimeAtDestination = builder.mArrivalTimeAtDestination;
        this.mRemainingTimeColor = builder.mRemainingTimeColor;
        this.mRemainingDistanceColor = builder.mRemainingDistanceColor;
    }

    /** A builder of {@link TravelEstimate}. */
    public static final class Builder {
        private final Distance mRemainingDistance;
        private long mRemainingTimeSeconds = REMAINING_TIME_UNKNOWN;
        private final DateTimeWithZone mArrivalTimeAtDestination;
        private CarColor mRemainingTimeColor = CarColor.DEFAULT;
        private CarColor mRemainingDistanceColor = CarColor.DEFAULT;

        private Builder(
                Distance remainingDistance,
                DateTimeWithZone arrivalTimeAtDestination) {
            this.mRemainingDistance = requireNonNull(remainingDistance);
            this.mArrivalTimeAtDestination = requireNonNull(arrivalTimeAtDestination);
        }

        @SuppressLint("UnsafeNewApiCall")
        // TODO(rampara): Move API 26 calls into separate class.
        @RequiresApi(26)
        @SuppressWarnings("AndroidJdkLibsChecker")
        private Builder(
                Distance remainingDistance,
                ZonedDateTime arrivalTimeAtDestination) {
            this.mRemainingDistance = remainingDistance;
            this.mArrivalTimeAtDestination = DateTimeWithZone.create(arrivalTimeAtDestination);
        }

        /**
         * Sets the estimated time remaining until arriving at the destination, in seconds.
         *
         * <p>If not set, {@link #REMAINING_TIME_UNKNOWN} will be used.
         *
         * @throws IllegalArgumentException if {@code remainingTimeSeconds} is a negative value
         *                                  but not {@link #REMAINING_TIME_UNKNOWN}.
         */
        @NonNull
        public Builder setRemainingTimeSeconds(long remainingTimeSeconds) {
            this.mRemainingTimeSeconds = validateRemainingTime(remainingTimeSeconds);
            return this;
        }

        /**
         * Sets the estimated time remaining until arriving at the destination.
         *
         * <p>If not set, {@link #REMAINING_TIME_UNKNOWN} will be used.
         *
         * @throws IllegalArgumentException if {@code remainingTime} is a negative duration
         *                                  but not {@link #REMAINING_TIME_UNKNOWN}.
         * @throws NullPointerException     if {@code remainingTime} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "UnsafeNewApiCall"})
        // TODO(rampara): Move API 26 calls into separate class.
        @RequiresApi(26)
        @SuppressWarnings("AndroidJdkLibsChecker")
        @NonNull
        public Builder setRemainingTime(@NonNull Duration remainingTime) {
            requireNonNull(remainingTime);
            this.mRemainingTimeSeconds = validateRemainingTime(remainingTime.getSeconds());
            return this;
        }

        /**
         * Sets the color of the remaining time text.
         *
         * <p>The host may ignore this color depending on the capabilities of the target screen.
         *
         * <p>If not set, {@link CarColor#DEFAULT} will be used.
         *
         * <p>Custom colors created with {@link CarColor#createCustom} are not supported.
         *
         * @throws IllegalArgumentException if {@code remainingTimeColor} is not supported.
         * @throws NullPointerException     if {@code remainingTimecolor} is {@code null}
         */
        @NonNull
        public Builder setRemainingTimeColor(@NonNull CarColor remainingTimeColor) {
            CarColorConstraints.STANDARD_ONLY.validateOrThrow(requireNonNull(remainingTimeColor));
            this.mRemainingTimeColor = remainingTimeColor;
            return this;
        }

        /**
         * Sets the color of the remaining distance text.
         *
         * <p>The host may ignore this color depending on the capabilities of the target screen.
         *
         * <p>If not set, {@link CarColor#DEFAULT} will be used.
         *
         * <p>Custom colors created with {@link CarColor#createCustom} are not supported.
         *
         * @throws IllegalArgumentException if {@code remainingDistanceColor} is not supported.
         * @throws NullPointerException     if {@code remainingDistanceColor} is {@code null}.
         */
        @NonNull
        public Builder setRemainingDistanceColor(@NonNull CarColor remainingDistanceColor) {
            CarColorConstraints.STANDARD_ONLY.validateOrThrow(
                    requireNonNull(remainingDistanceColor));
            this.mRemainingDistanceColor = remainingDistanceColor;
            return this;
        }

        /** Constructs the {@link TravelEstimate} defined by this builder. */
        @NonNull
        public TravelEstimate build() {
            return new TravelEstimate(this);
        }

        private static long validateRemainingTime(long remainingTimeSeconds) {
            if (remainingTimeSeconds < 0 && remainingTimeSeconds != REMAINING_TIME_UNKNOWN) {
                throw new IllegalArgumentException(
                        "Remaining time must be a larger than or equal to zero, or set to"
                                + " REMAINING_TIME_UNKNOWN");
            }
            return remainingTimeSeconds;
        }
    }
}
