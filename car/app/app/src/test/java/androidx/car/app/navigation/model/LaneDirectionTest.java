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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link LaneDirection}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class LaneDirectionTest {

    @Test
    public void createInstance() {
        int shape = SHAPE_NORMAL_LEFT;
        LaneDirection laneDirection = LaneDirection.create(shape, true);

        assertThat(shape).isEqualTo(laneDirection.getShape());
        assertThat(laneDirection.isRecommended()).isTrue();
    }

    @Test
    public void equals() {
        LaneDirection laneDirection = LaneDirection.create(SHAPE_NORMAL_LEFT, true);
        assertThat(LaneDirection.create(SHAPE_NORMAL_LEFT, true)).isEqualTo(laneDirection);
    }

    @Test
    public void notEquals_differentShape() {
        LaneDirection laneDirection = LaneDirection.create(SHAPE_NORMAL_LEFT, true);
        assertThat(LaneDirection.create(LaneDirection.SHAPE_STRAIGHT, true))
                .isNotEqualTo(laneDirection);
    }

    @Test
    public void notEquals_differentHighlighted() {
        LaneDirection laneDirection = LaneDirection.create(SHAPE_NORMAL_LEFT, true);
        assertThat(LaneDirection.create(SHAPE_NORMAL_LEFT, false)).isNotEqualTo(laneDirection);
    }
}
