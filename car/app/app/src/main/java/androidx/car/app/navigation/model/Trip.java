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
import androidx.car.app.model.CarText;

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

    /** Constructs a new builder of {@link Trip}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    public List<Destination> getDestinations() {
        return mDestinations;
    }

    @NonNull
    public List<Step> getSteps() {
        return mSteps;
    }

    @NonNull
    public List<TravelEstimate> getDestinationTravelEstimates() {
        return mDestinationTravelEstimates;
    }

    @NonNull
    public List<TravelEstimate> getStepTravelEstimates() {
        return mStepTravelEstimates;
    }

    @Nullable
    public CarText getCurrentRoad() {
        return mCurrentRoad;
    }

    public boolean isLoading() {
        return mIsLoading;
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

    private Trip(Builder builder) {
        this.mDestinations = new ArrayList<>(builder.mDestinations);
        this.mSteps = new ArrayList<>(builder.mSteps);
        this.mDestinationTravelEstimates = new ArrayList<>(builder.mDestinationTravelEstimates);
        this.mStepTravelEstimates = new ArrayList<>(builder.mStepTravelEstimates);
        this.mCurrentRoad = builder.mCurrentRoad;
        this.mIsLoading = builder.mIsLoading;
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
        private final List<Destination> mDestinations = new ArrayList<>();
        private final List<Step> mSteps = new ArrayList<>();
        private final List<TravelEstimate> mDestinationTravelEstimates = new ArrayList<>();
        private final List<TravelEstimate> mStepTravelEstimates = new ArrayList<>();
        @Nullable
        private CarText mCurrentRoad;
        private boolean mIsLoading;

        /**
         * Adds a destination to the trip.
         *
         * <p>Destinations must be added in order of arrival. A destination is not required. Display
         * surfaces may or may not use the destination and if multiple destinations are added the
         * display may only show information about the first destination.
         *
         * <p>For every destination added, a corresponding {@link TravelEstimate} must be added via
         * {@link #addDestinationTravelEstimate}.They are added separately so that travel
         * estimates can be updated frequently based on location.
         */
        @NonNull
        public Builder addDestination(@NonNull Destination destination) {
            mDestinations.add(requireNonNull(destination));
            return this;
        }

        /** Clears the list of destinations in the builder. */
        @NonNull
        public Builder clearDestinations() {
            mDestinations.clear();
            return this;
        }

        /**
         * Adds a step to the trip.
         *
         * <p>Steps must be added in order of arrival. A step is not required. Display surfaces
         * may or may not use the step and if multiple steps are added the display may only show
         * information about the first step.
         *
         * <p>For every step added, a corresponding {@link TravelEstimate} must be added via {@link
         * #addStepTravelEstimate}. They are added separately so that travel estimates can be
         * updated frequently based on location.
         */
        @NonNull
        public Builder addStep(@Nullable Step step) {
            mSteps.add(requireNonNull(step));
            return this;
        }

        /** Clears the list of steps in the builder. */
        @NonNull
        public Builder clearSteps() {
            mSteps.clear();
            return this;
        }

        /**
         * Adds a destination travel estimate to the trip.
         *
         * <p>Destination travel estimates must be added in order of arrival. A destination travel
         * estimate is not required. Display surfaces may or may not use the destination travel
         * estimate and if multiple destination travel estimates are added the display may only show
         * information about the first destination travel estimate.
         *
         * <p>For every destination travel estimate added, a corresponding destination must also be
         * added. They are added separately so that travel estimates can be updated frequently
         * based on location.
         */
        @NonNull
        public Builder addDestinationTravelEstimate(
                @NonNull TravelEstimate destinationTravelEstimate) {
            mDestinationTravelEstimates.add(requireNonNull(destinationTravelEstimate));
            return this;
        }

        /** Clears the list of destination travel estimates in the builder. */
        @NonNull
        public Builder clearDestinationTravelEstimates() {
            mDestinationTravelEstimates.clear();
            return this;
        }

        /**
         * Adds a step travel estimate to the trip.
         *
         * <p>Step travel estimates must be added in order of arrival. A step travel estimate is not
         * required. Display surfaces may or may not use the step travel estimate and if multiple
         * step travel estimates are added the display may only show information about the first
         * step travel estimate.
         *
         * <p>For every step travel estimate added, a corresponding step must also be added.
         */
        @NonNull
        public Builder addStepTravelEstimate(@NonNull TravelEstimate stepTravelEstimate) {
            mStepTravelEstimates.add(requireNonNull(stepTravelEstimate));
            return this;
        }

        /** Clears the list of destination travel estimates in the builder. */
        @NonNull
        public Builder clearStepTravelEstimates() {
            mStepTravelEstimates.clear();
            return this;
        }

        /** Sets a text description of the current road or {@code null} if unknown. */
        @NonNull
        public Builder setCurrentRoad(@Nullable CharSequence currentRoad) {
            this.mCurrentRoad = currentRoad == null ? null : CarText.create(currentRoad);
            return this;
        }

        /**
         * Sets whether the {@link Trip} is in a loading state.
         *
         * <p>If set to {@code true}, the UI may show a loading indicator, and adding any steps
         * or step travel estimates will throw an {@link IllegalArgumentException}.
         */
        // TODO(rampara): Consider renaming to setLoading()
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setIsLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
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
    }
}
