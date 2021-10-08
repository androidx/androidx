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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarText;
import androidx.car.app.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents information about a trip including destinations, steps, and travel estimates.
 *
 * <p>This information data <b>may</b> be displayed in different places in the car such as the
 * instrument cluster screens and heads-up display.
 */
@CarProtocol
public final class Trip {
    @Keep
    private final List<Destination> mDestinations;
    @Keep
    private final List<Step> mSteps;
    @Keep
    private final List<TravelEstimate> mDestinationTravelEstimates;
    @Keep
    private final List<TravelEstimate> mStepTravelEstimates;
    @Keep
    @Nullable
    private final CarText mCurrentRoad;
    @Keep
    private final boolean mIsLoading;

    /**
     * Returns whether the trip is in a loading state.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the list of destinations for this trip.
     *
     * @see Builder#addDestination(Destination, TravelEstimate)
     */
    @NonNull
    public List<Destination> getDestinations() {
        return CollectionUtils.emptyIfNull(mDestinations);
    }

    /**
     * Returns the list of steps for the trip.
     *
     * @see Builder#addStep(Step, TravelEstimate)
     */
    @NonNull
    public List<Step> getSteps() {
        return CollectionUtils.emptyIfNull(mSteps);
    }

    /**
     * Returns the list of {@link TravelEstimate}s for the {@link Destination}s in the trip.
     *
     * @see Builder#addDestination(Destination, TravelEstimate)
     */
    @NonNull
    public List<TravelEstimate> getDestinationTravelEstimates() {
        return CollectionUtils.emptyIfNull(mDestinationTravelEstimates);
    }

    /**
     * Returns the list of {@link TravelEstimate}s for the {@link Step}s in the trip.
     *
     * @see Builder#addDestination(Destination, TravelEstimate)
     */
    @NonNull
    public List<TravelEstimate> getStepTravelEstimates() {
        return CollectionUtils.emptyIfNull(mStepTravelEstimates);
    }

    /**
     * Returns the text that describes the current road.
     *
     * @see Builder#setCurrentRoad(CharSequence)
     */
    @Nullable
    public CarText getCurrentRoad() {
        return mCurrentRoad;
    }

    @Override
    @NonNull
    public String toString() {
        return "[ destinations : "
                + mDestinations.toString()
                + ", steps: "
                + mSteps.toString()
                + ", dest estimates: "
                + mDestinationTravelEstimates.toString()
                + ", step estimates: "
                + mStepTravelEstimates.toString()
                + ", road: "
                + CarText.toShortString(mCurrentRoad)
                + ", isLoading: "
                + mIsLoading
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mDestinations, mSteps, mDestinationTravelEstimates, mStepTravelEstimates,
                mCurrentRoad);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Trip)) {
            return false;
        }

        Trip otherTrip = (Trip) other;
        return Objects.equals(mDestinations, otherTrip.mDestinations)
                && Objects.equals(mSteps, otherTrip.mSteps)
                && Objects.equals(
                mDestinationTravelEstimates, otherTrip.mDestinationTravelEstimates)
                && Objects.equals(mStepTravelEstimates, otherTrip.mStepTravelEstimates)
                && Objects.equals(mCurrentRoad, otherTrip.mCurrentRoad)
                && Objects.equals(mIsLoading, otherTrip.mIsLoading);
    }

    Trip(Builder builder) {
        mDestinations = CollectionUtils.unmodifiableCopy(builder.mDestinations);
        mSteps = CollectionUtils.unmodifiableCopy(builder.mSteps);
        mDestinationTravelEstimates = CollectionUtils.unmodifiableCopy(
                builder.mDestinationTravelEstimates);
        mStepTravelEstimates = CollectionUtils.unmodifiableCopy(builder.mStepTravelEstimates);
        mCurrentRoad = builder.mCurrentRoad;
        mIsLoading = builder.mIsLoading;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Trip() {
        mDestinations = Collections.emptyList();
        mSteps = Collections.emptyList();
        mDestinationTravelEstimates = Collections.emptyList();
        mStepTravelEstimates = Collections.emptyList();
        mCurrentRoad = null;
        mIsLoading = false;
    }

    /** A builder of {@link Trip}. */
    public static final class Builder {
        final List<Destination> mDestinations = new ArrayList<>();
        final List<Step> mSteps = new ArrayList<>();
        final List<TravelEstimate> mDestinationTravelEstimates = new ArrayList<>();
        final List<TravelEstimate> mStepTravelEstimates = new ArrayList<>();
        @Nullable
        CarText mCurrentRoad;
        boolean mIsLoading;

        /**
         * Adds a destination to the trip.
         *
         * <p>Destinations must be added in order of arrival. A destination is not required. Display
         * surfaces may or may not use the destination and if multiple destinations are added the
         * display may only show information about the first destination.
         *
         * <p>For every destination added, a corresponding {@link TravelEstimate} must be
         * provided. Display surfaces may or may not use the destination travel estimate and if
         * multiple destination travel estimates are added the display may only show information
         * about the first destination travel estimate.
         *
         * @throws NullPointerException if {@code step} or {@code stepTravelEstimate} are {@code
         *                              null}
         */
        @NonNull
        public Builder addDestination(@NonNull Destination destination,
                @NonNull TravelEstimate destinationTravelEstimate) {
            mDestinations.add(requireNonNull(destination));
            mDestinationTravelEstimates.add(requireNonNull(destinationTravelEstimate));
            return this;
        }

        /**
         * Adds a step to the trip.
         *
         * <p>Steps must be added in order of arrival. A step is not required. Display surfaces
         * may or may not use the step and if multiple steps are added the display may only show
         * information about the first step.
         *
         * <p>For every step added, a corresponding {@link TravelEstimate} must be provided.
         * Display surfaces may or may not use the step travel estimate and if multiple
         * step travel estimates are added the display may only show information about the first
         * step travel estimate.
         *
         * @throws NullPointerException if {@code step} or {@code stepTravelEstimate} are {@code
         *                              null}
         */
        @NonNull
        public Builder addStep(@NonNull Step step, @NonNull TravelEstimate stepTravelEstimate) {
            mSteps.add(requireNonNull(step));
            mStepTravelEstimates.add(requireNonNull(stepTravelEstimate));
            return this;
        }

        /**
         * Sets a description of the current road.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code currentRoad} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setCurrentRoad(@NonNull CharSequence currentRoad) {
            mCurrentRoad = CarText.create(requireNonNull(currentRoad));
            return this;
        }

        /**
         * Sets whether the {@link Trip} is in a loading state.
         *
         * <p>If set to {@code true}, the UI may show a loading indicator, and adding any steps
         * or step travel estimates will throw an {@link IllegalArgumentException}.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Constructs the {@link Trip} defined by this builder.
         */
        @NonNull
        public Trip build() {
            if (mDestinations.size() != mDestinationTravelEstimates.size()) {
                throw new IllegalArgumentException(
                        "Destinations and destination travel estimates sizes must match");
            }
            if (mSteps.size() != mStepTravelEstimates.size()) {
                throw new IllegalArgumentException(
                        "Steps and step travel estimates sizes must match");
            }
            if (mIsLoading && !mSteps.isEmpty()) {
                throw new IllegalArgumentException("Step information may not be set while loading");
            }
            return new Trip(this);
        }

        /** Constructs an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
