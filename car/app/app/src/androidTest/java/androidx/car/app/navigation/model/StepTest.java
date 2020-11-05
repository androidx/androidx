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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link Step}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class StepTest {
    @Test
    public void createInstance() {
        Lane lane = Lane.builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Maneuver maneuver =
                Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                        .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        String cue = "Left at State street.";
        String road = "State St.";
        Step step =
                Step.builder(cue)
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
        Lane lane1 = Lane.builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Lane lane2 = Lane.builder()
                .addDirection(LaneDirection.create(LaneDirection.SHAPE_SHARP_RIGHT, true))
                .build();
        String cue = "Left at State street.";
        Step step = Step.builder(cue).addLane(lane1).addLane(lane2).clearLanes().build();

        assertThat(step.getLanes()).hasSize(0);
    }

    @Test
    public void createInstance_lanesImage_no_lanes_throws() {
        String cue = "Left at State street.";

        assertThrows(
                IllegalStateException.class,
                () -> Step.builder(cue).setLanesImage(CarIcon.APP_ICON).build());
    }

    @Test
    public void equals() {
        Lane lane = Lane.builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        Maneuver maneuver = Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                .setIcon(CarIcon.APP_ICON)
                .build();
        String cue = "Left at State street.";
        String road = "State St.";
        Step step = Step.builder(cue)
                .addLane(lane)
                .setLanesImage(CarIcon.APP_ICON)
                .setManeuver(maneuver)
                .setRoad(road)
                .build();

        assertThat(Step.builder(cue)
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
        Step step = Step.builder(cue).build();

        assertThat(Step.builder("foo").build()).isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentLane() {
        Lane lane = Lane.builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();
        String cue = "Left at State street.";

        Step step = Step.builder(cue).addLane(lane).build();

        assertThat(Step.builder(cue)
                .addLane(Lane.builder()
                        .addDirection(LaneDirection.create(SHAPE_SHARP_LEFT, false))
                        .build())
                .build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentLanesImage() {
        String cue = "Left at State street.";
        Lane lane = Lane.builder().addDirection(
                LaneDirection.create(SHAPE_SHARP_LEFT, true)).build();

        Step step = Step.builder(cue).addLane(lane).setLanesImage(CarIcon.APP_ICON).build();

        assertThat(Step.builder(cue).addLane(lane).setLanesImage(CarIcon.ALERT).build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentManeuver() {
        Maneuver maneuver =
                Maneuver.builder(TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                        .setRoundaboutExitNumber(/*roundaboutExitNumber=*/ 2)
                        .setIcon(CarIcon.APP_ICON)
                        .build();
        String cue = "Left at State street.";

        Step step = Step.builder(cue).setManeuver(maneuver).build();

        assertThat(Step.builder(cue)
                .setManeuver(Maneuver.builder(Maneuver.TYPE_DESTINATION).setIcon(
                        CarIcon.APP_ICON).build())
                .build())
                .isNotEqualTo(step);
    }

    @Test
    public void notEquals_differentRoad() {
        String cue = "Left at State street.";

        Step step = Step.builder(cue).setRoad("road").build();

        assertThat(Step.builder(cue).setRoad("foo").build()).isNotEqualTo(step);
    }
}
