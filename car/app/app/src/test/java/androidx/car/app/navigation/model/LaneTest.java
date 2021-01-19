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

import static androidx.car.app.navigation.model.LaneDirection.SHAPE_NORMAL_LEFT;
import static androidx.car.app.navigation.model.LaneDirection.SHAPE_SHARP_LEFT;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Lane}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class LaneTest {

    @Test
    public void createInstance() {
        LaneDirection laneDirection1 = LaneDirection.create(SHAPE_SHARP_LEFT, true);
        LaneDirection laneDirection2 = LaneDirection.create(SHAPE_NORMAL_LEFT, false);
        Lane lane = new Lane.Builder().addDirection(laneDirection1).addDirection(
                laneDirection2).build();

        assertThat(lane.getDirections()).hasSize(2);
        assertThat(laneDirection1).isEqualTo(lane.getDirections().get(0));
        assertThat(laneDirection2).isEqualTo(lane.getDirections().get(1));
    }

    @Test
    public void equals() {
        LaneDirection laneDirection = LaneDirection.create(SHAPE_SHARP_LEFT, true);
        Lane lane = new Lane.Builder().addDirection(laneDirection).build();

        assertThat(new Lane.Builder().addDirection(laneDirection).build()).isEqualTo(lane);
    }

    @Test
    public void notEquals_differentDirections() {
        LaneDirection laneDirection = LaneDirection.create(SHAPE_SHARP_LEFT, true);
        Lane lane = new Lane.Builder().addDirection(laneDirection).build();

        assertThat(new Lane.Builder().addDirection(laneDirection).addDirection(
                laneDirection).build())
                .isNotEqualTo(lane);
    }
}
