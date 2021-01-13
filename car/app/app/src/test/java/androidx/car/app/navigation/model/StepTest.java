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

import static androidx.car.app.navigation.model.LaneDirection.SHAPE_SHARP_LEFT;
import static androidx.car.app.navigation.model.Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.model.CarIcon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Step}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class StepTest {
    @Test
    public void createInstance() {
        Lane lane = new Lane.Builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Maneuver maneuver =
                new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                        .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        String cue = "Left at State street.";
        String road = "State St.";
        Step step =
                new Step.Builder(cue)
                        .addLane(lane)
                        .setLanesImage(CarIcon.APP_ICON)
                        .setManeuver(maneuver)
                        .setRoad(road)
                        .build();

        assertThat(step.getLanes()).hasSize(1);
        assertThat(lane).isEqualTo(step.getLanes().get(0));
        assertThat(CarIcon.APP_ICON).isEqualTo(step.getLanesImage());
        assertThat(maneuver).isEqualTo(step.getManeuver());
        assertThat(cue).isEqualTo(step.getCue().getText());
        assertThat(road).isEqualTo(step.getRoad().getText());
    }

    @Test
    public void clearLanes() {
        Lane lane1 = new Lane.Builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Lane lane2 = new Lane.Builder()
                .addDirection(LaneDirection.create(LaneDirection.SHAPE_SHARP_RIGHT, true))
                .build();
        String cue = "Left at State street.";
        Step step = new Step.Builder(cue).addLane(lane1).addLane(lane2).clearLanes().build();

        assertThat(step.getLanes()).hasSize(0);
    }

    @Test
    public void createInstance_lanesImage_no_lanes_throws() {
        String cue = "Left at State street.";

        assertThrows(
                IllegalStateException.class,
                () -> new Step.Builder(cue).setLanesImage(CarIcon.APP_ICON).build());
    }

    @Test
    public void equals() {
        Lane lane = new Lane.Builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Maneuver maneuver = new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                .setIcon(CarIcon.APP_ICON)
                .build();
        String cue = "Left at State street.";
        String road = "State St.";
        Step step = new Step.Builder(cue)
                .addLane(lane)
                .setLanesImage(CarIcon.APP_ICON)
                .setManeuver(maneuver)
                .setRoad(road)
                .build();

        assertThat(new Step.Builder(cue)
                .addLane(lane)
                .setLanesImage(CarIcon.APP_ICON)
                .setManeuver(maneuver)
                .setRoad(road)
                .build())
                .isEqualTo(step);
    }

    @Test
    public void notEquals_differentCue() {
        String cue = "Left at State street.";
        Step step = new Step.Builder(cue).build();

        assertThat(new Step.Builder("foo").build()).isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentLane() {
        Lane lane = new Lane.Builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        String cue = "Left at State street.";

        Step step = new Step.Builder(cue).addLane(lane).build();

        assertThat(new Step.Builder(cue)
                .addLane(new Lane.Builder()
                        .addDirection(LaneDirection.create(SHAPE_SHARP_LEFT, false))
                        .build())
                .build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentLanesImage() {
        String cue = "Left at State street.";
        Lane lane = new Lane.Builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();

        Step step = new Step.Builder(cue).addLane(lane).setLanesImage(CarIcon.APP_ICON).build();

        assertThat(new Step.Builder(cue).addLane(lane).setLanesImage(CarIcon.ALERT).build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentManeuver() {
        Maneuver maneuver =
                new Maneuver.Builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                        .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        String cue = "Left at State street.";

        Step step = new Step.Builder(cue).setManeuver(maneuver).build();

        assertThat(new Step.Builder(cue)
                .setManeuver(new Maneuver.Builder(Maneuver.TYPE_DESTINATION).setIcon(
                        CarIcon.APP_ICON).build())
                .build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentRoad() {
        String cue = "Left at State street.";

        Step step = new Step.Builder(cue).setRoad("road").build();

        assertThat(new Step.Builder(cue).setRoad("foo").build()).isNotEqualTo(step);
    }
}
