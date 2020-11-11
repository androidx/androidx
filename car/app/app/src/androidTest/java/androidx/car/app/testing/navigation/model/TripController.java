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

package androidx.car.app.testing.navigation.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.testing.model.ControllerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller that allows testing of a {@link Trip}.
 *
 * <p>this controller allows retrieving the following fields:
 *
 * <ul>
 *   <li>The {@link Destination}s set via {@link Trip.Builder#addDestination}.
 *   <li>The {@link Step}s set via {@link Trip.Builder#addStep}.
 *   <li>The {@link TravelEstimate}s set via {@link Trip.Builder#addDestinationTravelEstimate}.
 *   <li>The {@link TravelEstimate}s set via {@link Trip.Builder#addStepTravelEstimate}.
 *   <li>The current road set via {@link Trip.Builder#setCurrentRoad}.
 *   <li>The loading state set via {@link Trip.Builder#setIsLoading}.
 * </ul>
 */
public class TripController {
    private final Trip mTrip;

    /** Creates a {@link TripController} to control a {@link Trip} for testing. */
    @NonNull
    public static TripController of(@NonNull Trip trip) {
        return new TripController(requireNonNull(trip));
    }

    /**
     * Returns a list of {@link DestinationController}s, each containing a {@link Destination} added
     * via {@link Trip.Builder#addDestination}.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<DestinationController> getDestinations() {
        List<Destination> destinations =
                (List<Destination>) ControllerUtil.getFieldOrThrow(mTrip, "destinations");
        List<DestinationController> toReturn = new ArrayList<>();

        for (Destination destination : destinations) {
            toReturn.add(DestinationController.of(destination));
        }
        return toReturn;
    }

    /**
     * Returns a list of {@link StepController}s, each containing a {@link Step} added via {@link
     * Trip.Builder#addStep}.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<StepController> getSteps() {
        List<Step> steps = (List<Step>) ControllerUtil.getFieldOrThrow(mTrip, "steps");
        List<StepController> toReturn = new ArrayList<>();

        for (Step step : steps) {
            toReturn.add(StepController.of(step));
        }
        return toReturn;
    }

    /**
     * Returns a list of {@link TravelEstimate}s added via {@link
     * Trip.Builder#addDestinationTravelEstimate}.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<TravelEstimate> getDestinationTravelEstimates() {
        return (List<TravelEstimate>)
                ControllerUtil.getFieldOrThrow(mTrip, "destinationTravelEstimates");
    }

    /**
     * Returns a list of {@link TravelEstimate}s added via
     * {@link Trip.Builder#addStepTravelEstimate}.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public List<TravelEstimate> getStepTravelEstimates() {
        return (List<TravelEstimate>) ControllerUtil.getFieldOrThrow(mTrip, "stepTravelEstimates");
    }

    /**
     * Retrieves the current road set in the {@link Trip} being controlled, or {@code null} if
     * none is
     * present.
     *
     * <p>The values returned are the {@link CharSequence#toString} for the road provided.
     */
    @Nullable
    public String getCurrentRoad() {
        Object currentRoad = ControllerUtil.getFieldOrThrow(mTrip, "currentRoad");
        return currentRoad == null ? null : currentRoad.toString();
    }

    public boolean isLoading() {
        Object isLoading = ControllerUtil.getFieldOrThrow(mTrip, "isLoading");
        return isLoading == null ? false : (Boolean) isLoading;
    }

    private TripController(Trip trip) {
        this.mTrip = trip;
    }
}
