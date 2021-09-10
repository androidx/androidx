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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.core.graphics.drawable.IconCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RoutingInfoTest}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RoutingInfoTest {

    private final Maneuver mManeuver =
            new Maneuver.Builder(Maneuver.TYPE_FERRY_BOAT).setIcon(CarIcon.APP_ICON).build();
    private final Step mCurrentStep =
            new Step.Builder("Go Straight").setManeuver(mManeuver).setRoad("405").build();
    private final Distance mCurrentDistance =
            Distance.create(/* displayDistance= */ 100, Distance.UNIT_METERS);

    @Test
    public void noCurrentStep_throws() {
        assertThrows(IllegalStateException.class, () -> new RoutingInfo.Builder().build());
    }

    @Test
    public void isLoading_throws_when_not_empty() {
        assertThrows(
                IllegalStateException.class,
                () -> new RoutingInfo.Builder()
                        .setLoading(true)
                        .setCurrentStep(mCurrentStep, mCurrentDistance)
                        .build());
    }

    @Test
    public void invalidCarIcon_throws() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(ContentResolver.SCHEME_CONTENT);
        builder.appendPath("foo/bar");
        Uri iconUri = builder.build();
        CarIcon carIcon = new CarIcon.Builder(IconCompat.createWithContentUri(iconUri)).build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutingInfo.Builder().setJunctionImage(carIcon));
    }

    /** Tests basic construction of a template with a minimal data. */
    @Test
    public void createMinimalInstance() {
        RoutingInfo routingInfo =
                new RoutingInfo.Builder().setCurrentStep(mCurrentStep, mCurrentDistance).build();
        assertThat(routingInfo.getCurrentStep()).isEqualTo(mCurrentStep);
        assertThat(routingInfo.getNextStep()).isNull();
    }

    /** Tests construction of a template with all data. */
    @Test
    public void createFullInstance() {
        Maneuver nextManeuver =
                new Maneuver.Builder(Maneuver.TYPE_U_TURN_LEFT).setIcon(CarIcon.APP_ICON).build();
        Step nextStep = new Step.Builder("Turn Around").setManeuver(nextManeuver).setRoad(
                "520").build();

        RoutingInfo routingInfo = new RoutingInfo.Builder()
                .setCurrentStep(mCurrentStep, mCurrentDistance)
                .setNextStep(nextStep)
                .build();
        assertThat(routingInfo.getCurrentStep()).isEqualTo(mCurrentStep);
        assertThat(routingInfo.getCurrentDistance()).isEqualTo(mCurrentDistance);
        assertThat(routingInfo.getNextStep()).isEqualTo(nextStep);
    }

    @Test
    public void laneInfo_set_no_lanesImage_throws() {
        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(new Lane.Builder()
                                .addDirection(LaneDirection.create(
                                        LaneDirection.SHAPE_NORMAL_LEFT, false))
                                .build())
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        assertThrows(
                IllegalStateException.class,
                () -> new RoutingInfo.Builder().setCurrentStep(currentStep,
                        currentDistance).build());
    }

    @Test
    public void laneInfo_set_with_lanesImage_doesnt_throws() {
        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(new Lane.Builder()
                                .addDirection(LaneDirection.create(
                                        LaneDirection.SHAPE_NORMAL_LEFT, false))
                                .build())
                        .setLanesImage(CarIcon.APP_ICON)
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);
        new RoutingInfo.Builder().setCurrentStep(currentStep, currentDistance).build();
    }

    @Test
    public void equals() {
        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(new Lane.Builder()
                                .addDirection(LaneDirection.create(
                                        LaneDirection.SHAPE_NORMAL_LEFT, false))
                                .build())
                        .setLanesImage(CarIcon.ALERT)
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        RoutingInfo routingInfo =
                new RoutingInfo.Builder()
                        .setCurrentStep(currentStep, currentDistance)
                        .setJunctionImage(CarIcon.ALERT)
                        .setNextStep(currentStep)
                        .build();

        assertThat(routingInfo)
                .isEqualTo(new RoutingInfo.Builder()
                        .setCurrentStep(currentStep, currentDistance)
                        .setJunctionImage(CarIcon.ALERT)
                        .setNextStep(currentStep)
                        .build());
    }

    @Test
    public void notEquals_differentCurrentStep() {
        Step currentStep =
                new Step.Builder("Hop on a ferry")
                        .addLane(new Lane.Builder()
                                .addDirection(LaneDirection.create(
                                        LaneDirection.SHAPE_NORMAL_LEFT, false))
                                .build())
                        .setLanesImage(CarIcon.APP_ICON)
                        .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        RoutingInfo routingInfo =
                new RoutingInfo.Builder().setCurrentStep(currentStep, currentDistance).build();

        assertThat(routingInfo)
                .isNotEqualTo(new RoutingInfo.Builder()
                        .setCurrentStep(new Step.Builder("do a back flip")
                                        .addLane(new Lane.Builder()
                                                .addDirection(LaneDirection.create(
                                                        LaneDirection.SHAPE_NORMAL_LEFT,
                                                        false))
                                                .build())
                                        .setLanesImage(CarIcon.APP_ICON)
                                        .build(),
                                currentDistance)
                        .build());
    }

    @Test
    public void notEquals_differentCurrentDistance() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.APP_ICON)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        RoutingInfo routingInfo =
                new RoutingInfo.Builder().setCurrentStep(currentStep, currentDistance).build();

        assertThat(routingInfo)
                .isNotEqualTo(new RoutingInfo.Builder()
                        .setCurrentStep(currentStep,
                                Distance.create(/* displayDistance= */ 200, Distance.UNIT_METERS))
                        .build());
    }

    @Test
    public void notEquals_differentJunctionImage() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.ALERT)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        RoutingInfo routingInfo = new RoutingInfo.Builder()
                .setCurrentStep(currentStep, currentDistance)
                .setJunctionImage(CarIcon.ALERT)
                .setNextStep(currentStep)
                .build();

        assertThat(routingInfo)
                .isNotEqualTo(new RoutingInfo.Builder()
                        .setCurrentStep(currentStep, currentDistance)
                        .setJunctionImage(CarIcon.ERROR)
                        .setNextStep(currentStep)
                        .build());
    }

    @Test
    public void notEquals_differentNextStep() {
        Step currentStep = new Step.Builder("Hop on a ferry")
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(
                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                        .build())
                .setLanesImage(CarIcon.ALERT)
                .build();
        Distance currentDistance = Distance.create(/* displayDistance= */ 100,
                Distance.UNIT_METERS);

        RoutingInfo routingInfo = new RoutingInfo.Builder()
                .setCurrentStep(currentStep, currentDistance)
                .setNextStep(currentStep)
                .build();

        assertThat(routingInfo)
                .isNotEqualTo(new RoutingInfo.Builder()
                        .setCurrentStep(currentStep, currentDistance)
                        .setNextStep(new Step.Builder("Do a backflip")
                                .addLane(new Lane.Builder()
                                        .addDirection(LaneDirection.create(
                                                LaneDirection.SHAPE_NORMAL_LEFT, false))
                                        .build())
                                .setLanesImage(CarIcon.ALERT)
                                .build())
                        .build());
    }
}
