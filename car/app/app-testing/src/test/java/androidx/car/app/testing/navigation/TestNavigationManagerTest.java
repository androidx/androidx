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

package androidx.car.app.testing.navigation;

import static com.google.common.truth.Truth.assertThat;

import androidx.car.app.model.CarText;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/** Tests for {@link TestNavigationManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TestNavigationManagerTest {
    private TestCarContext mCarContext;

    @Before
    public void setup() {
        mCarContext = TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void getNavigationStatesSent() {
        // First need to set a listener and start navigation.
        NavigationManagerCallback listener1 = new TestNavigationManagerCallback();
        mCarContext.getCarService(NavigationManager.class).setNavigationManagerCallback(listener1);
        mCarContext.getCarService(NavigationManager.class).navigationStarted();

        Destination destination1 =
                new Destination.Builder().setName("Home").setAddress("123 State Street").build();
        Step step1 = new Step.Builder("Straight Ahead").build();
        TravelEstimate stepTravelEstimate1 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 100, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T15:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofMinutes(1)).build();
        TravelEstimate destinationTravelEstimate1 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 10000, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T16:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofHours(1)).build();
        String road1 = "State St.";
        Trip trip1 =
                new Trip.Builder()
                        .addDestination(destination1, destinationTravelEstimate1)
                        .addStep(step1, stepTravelEstimate1)
                        .setCurrentRoad(road1)
                        .build();
        mCarContext.getCarService(NavigationManager.class).updateTrip(trip1);
        List<Trip> tripsSent1 =
                mCarContext.getCarService(TestNavigationManager.class).getTripsSent();
        assertThat(tripsSent1).hasSize(1);
        checkTrip(
                tripsSent1.get(0),
                ImmutableList.of(destination1),
                ImmutableList.of(step1),
                ImmutableList.of(destinationTravelEstimate1),
                ImmutableList.of(stepTravelEstimate1),
                road1);

        Destination destination2 =
                new Destination.Builder().setName("Gas").setAddress("456 State Street").build();
        Step step2 = new Step.Builder("Turn Left").build();
        TravelEstimate stepTravelEstimate2 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 200, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T15:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofMinutes(2)).build();
        TravelEstimate destinationTravelEstimate2 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 20000, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T17:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofHours(2)).build();
        String road2 = "6th St.";
        Trip trip2 =
                new Trip.Builder()
                        .addDestination(destination2, destinationTravelEstimate2)
                        .addStep(step2, stepTravelEstimate2)
                        .setCurrentRoad(road2)
                        .build();

        Destination destination3 =
                new Destination.Builder().setName("Work").setAddress("789 State Street").build();
        Step step3 = new Step.Builder("Turn Right").build();
        TravelEstimate stepTravelEstimate3 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 300, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T15:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofMinutes(3)).build();
        TravelEstimate destinationTravelEstimate3 =
                new TravelEstimate.Builder(
                        Distance.create(/* displayDistance= */ 30000, Distance.UNIT_METERS),
                        createDateTimeWithZone("2020-04-14T15:57:00",
                                "US/Pacific")).setRemainingTime(Duration.ofHours(3)).build();
        String road3 = "Kirkland Way";
        Trip trip3 =
                new Trip.Builder()
                        .addDestination(destination3, destinationTravelEstimate3)
                        .addStep(step3, stepTravelEstimate3)
                        .setCurrentRoad(road3)
                        .build();

        mCarContext.getCarService(NavigationManager.class).updateTrip(trip2);
        mCarContext.getCarService(NavigationManager.class).updateTrip(trip3);
        List<Trip> tripsSent2 =
                mCarContext.getCarService(TestNavigationManager.class).getTripsSent();
        assertThat(tripsSent2).hasSize(3);

        checkTrip(
                tripsSent2.get(0),
                ImmutableList.of(destination1),
                ImmutableList.of(step1),
                ImmutableList.of(destinationTravelEstimate1),
                ImmutableList.of(stepTravelEstimate1),
                road1);
        checkTrip(
                tripsSent2.get(1),
                ImmutableList.of(destination2),
                ImmutableList.of(step2),
                ImmutableList.of(destinationTravelEstimate2),
                ImmutableList.of(stepTravelEstimate2),
                road2);
        checkTrip(
                tripsSent2.get(2),
                ImmutableList.of(destination3),
                ImmutableList.of(step3),
                ImmutableList.of(destinationTravelEstimate3),
                ImmutableList.of(stepTravelEstimate3),
                road3);
    }

    @Test
    public void getNavigationManagerCallbacksSet() {
        NavigationManagerCallback listener1 = new TestNavigationManagerCallback();

        mCarContext.getCarService(NavigationManager.class).setNavigationManagerCallback(listener1);
        assertThat(
                mCarContext
                        .getCarService(TestNavigationManager.class)
                        .getNavigationManagerCallback())
                .isEqualTo(listener1);

        NavigationManagerCallback listener2 = new TestNavigationManagerCallback();
        NavigationManagerCallback listener3 = new TestNavigationManagerCallback();

        mCarContext.getCarService(NavigationManager.class).setNavigationManagerCallback(listener2);
        mCarContext.getCarService(NavigationManager.class).setNavigationManagerCallback(listener3);

        assertThat(
                mCarContext
                        .getCarService(TestNavigationManager.class)
                        .getNavigationManagerCallback())
                .isEqualTo(listener3);

        mCarContext.getCarService(NavigationManager.class).navigationEnded();
        mCarContext.getCarService(NavigationManager.class).clearNavigationManagerCallback();
        assertThat(
                mCarContext
                        .getCarService(TestNavigationManager.class)
                        .getNavigationManagerCallback())
                .isEqualTo(null);
    }

    @Test
    public void getNavigationStartedCount() {
        mCarContext
                .getCarService(NavigationManager.class)
                .setNavigationManagerCallback(new TestNavigationManagerCallback());

        assertThat(
                mCarContext.getCarService(TestNavigationManager.class).getNavigationStartedCount())
                .isEqualTo(0);

        mCarContext.getCarService(NavigationManager.class).navigationStarted();
        assertThat(
                mCarContext.getCarService(TestNavigationManager.class).getNavigationStartedCount())
                .isEqualTo(1);
    }

    @Test
    public void getNavigationEndedCount() {
        assertThat(mCarContext.getCarService(TestNavigationManager.class).getNavigationEndedCount())
                .isEqualTo(0);

        mCarContext.getCarService(NavigationManager.class).navigationEnded();
        assertThat(mCarContext.getCarService(TestNavigationManager.class).getNavigationEndedCount())
                .isEqualTo(1);
    }

    private static void checkTrip(
            Trip trip,
            List<Destination> destinations,
            List<Step> steps,
            List<TravelEstimate> destinationTravelEstimates,
            List<TravelEstimate> stepTravelEstimates,
            String currentRoad) {
        assertThat(trip.getDestinations()).hasSize(destinations.size());
        int destinationIndex = 0;
        for (Destination destination : destinations) {
            assertThat(trip.getDestinations().get(destinationIndex++))
                    .isEqualTo(destination);
        }
        assertThat(trip.getSteps()).hasSize(steps.size());
        int stepIndex = 0;
        for (Step step : steps) {
            assertThat(trip.getSteps().get(stepIndex++)).isEqualTo(step);
        }
        assertThat(trip.getDestinationTravelEstimates())
                .containsExactlyElementsIn(destinationTravelEstimates);
        assertThat(trip.getStepTravelEstimates())
                .containsExactlyElementsIn(stepTravelEstimates);
        assertThat(trip.getCurrentRoad()).isEqualTo(CarText.create(currentRoad));
    }

    /** A no-op callback for testing purposes. */
    private static class TestNavigationManagerCallback implements NavigationManagerCallback {
        @Override
        public void onStopNavigation() {
        }

        @Override
        public void onAutoDriveEnabled() {
        }
    }

    /**
     * Returns a {@link DateTimeWithZone} instance from a date string and a time zone id.
     *
     * @param dateTimeString The string in ISO format, for example "2020-04-14T15:57:00".
     * @param zoneIdString   An Olson DB time zone identifier, for example "US/Pacific".
     */
    private DateTimeWithZone createDateTimeWithZone(String dateTimeString, String zoneIdString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TimeZone timeZone = TimeZone.getTimeZone(zoneIdString);
        dateFormat.setTimeZone(timeZone);
        Date date;
        try {
            date = dateFormat.parse(dateTimeString);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse string: " + dateTimeString, e);
        }
        if (date == null) {
            throw new IllegalArgumentException("Failed to parse string: " + dateTimeString);
        }
        return DateTimeWithZone.create(date.getTime(), timeZone);
    }
}
