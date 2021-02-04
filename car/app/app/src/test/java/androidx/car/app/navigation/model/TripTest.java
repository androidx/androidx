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

import static androidx.car.app.TestUtils.createDateTimeWithZone;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_SHARP_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.TimeUnit;

/** Tests for {@link Trip}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TripTest {

    private final Step mStep =
            new Step.Builder("Take the second exit of the roundabout.")
                    .addLane(new Lane.Builder().addDirection(
                            LaneDirection.create(SHAPE_SHARP_LEFT, true)).build())
                    .setManeuver(new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                            .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                            .setIcon(CarIcon.APP_ICON)
                            .build())
                    .build();
    private final Destination mDestination =
            new Destination.Builder().setName("Google BVE").setAddress("1120 112th Ave NE").build();
    private final TravelEstimate mStepTravelEstimate =
            new TravelEstimate.Builder(
                    Distance.create(/* displayDistance= */ 10, Distance.UNIT_KILOMETERS),
                    createDateTimeWithZone("2020-04-14T15:57:00", "US/Pacific"))
                    .setRemainingTimeSeconds(TimeUnit.HOURS.toSeconds(1)).build();
    private final TravelEstimate mDestinationTravelEstimate =
            new TravelEstimate.Builder(
                    Distance.create(/* displayDistance= */ 100, Distance.UNIT_KILOMETERS),
                    createDateTimeWithZone("2020-04-14T16:57:00", "US/Pacific"))
                    .setRemainingTimeSeconds(TimeUnit.HOURS.toSeconds(1)).build();
    private static final String ROAD = "State St.";

    @Test
    public void createInstance() {
        Trip trip =
                new Trip.Builder()
                        .addDestination(mDestination, mDestinationTravelEstimate)
                        .addStep(mStep, mStepTravelEstimate)
                        .setCurrentRoad(ROAD)
                        .setLoading(false)
                        .build();

        assertThat(trip.getDestinations()).hasSize(1);
        assertThat(mDestination).isEqualTo(trip.getDestinations().get(0));
        assertThat(trip.getSteps()).hasSize(1);
        assertThat(mStep).isEqualTo(trip.getSteps().get(0));
        assertThat(trip.getDestinationTravelEstimates()).hasSize(1);
        assertThat(mDestinationTravelEstimate).isEqualTo(
                trip.getDestinationTravelEstimates().get(0));
        assertThat(trip.getStepTravelEstimates()).hasSize(1);
        assertThat(mStepTravelEstimate).isEqualTo(trip.getStepTravelEstimates().get(0));
        assertThat(trip.isLoading()).isFalse();
    }

    @Test
    public void createInstance_loading_no_steps() {
        Trip trip =
                new Trip.Builder()
                        .addDestination(mDestination, mDestinationTravelEstimate)
                        .setCurrentRoad(ROAD)
                        .setLoading(true)
                        .build();

        assertThat(trip.getDestinations()).hasSize(1);
        assertThat(mDestination).isEqualTo(trip.getDestinations().get(0));
        assertThat(trip.getSteps()).hasSize(0);
        assertThat(trip.getDestinationTravelEstimates()).hasSize(1);
        assertThat(mDestinationTravelEstimate).isEqualTo(
                trip.getDestinationTravelEstimates().get(0));
        assertThat(trip.getStepTravelEstimates()).hasSize(0);
        assertThat(trip.isLoading()).isTrue();
    }

    @Test
    public void createInstance_loading_with_steps() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Trip.Builder()
                        .addStep(mStep, mStepTravelEstimate)
                        .setLoading(true)
                        .build());
    }
}