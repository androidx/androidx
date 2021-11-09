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

package androidx.car.app.model;

import static androidx.car.app.model.Distance.UNIT_KILOMETERS;
import static androidx.car.app.model.Distance.UNIT_METERS;
import static androidx.car.app.model.Distance.UNIT_YARDS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Distance}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DistanceTest {

    private static final double DISPLAY_DISTANCE = 1.2d;
    private static final int DISPLAY_UNIT = UNIT_KILOMETERS;
    private static final double DELTA = 0.00001;

    @Test
    public void createInstance_negativeMeter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Distance.create(/* displayDistance= */ -1, UNIT_METERS));
    }

    @Test
    public void createInstance() {
        Distance distance = Distance.create(DISPLAY_DISTANCE, DISPLAY_UNIT);
        assertThat(distance.getDisplayDistance()).isWithin(DELTA).of(DISPLAY_DISTANCE);
        assertThat(distance.getDisplayUnit()).isEqualTo(DISPLAY_UNIT);
    }

    @Test
    public void equals() {
        Distance distance = Distance.create(DISPLAY_DISTANCE, DISPLAY_UNIT);

        assertThat(Distance.create(DISPLAY_DISTANCE, DISPLAY_UNIT)).isEqualTo(distance);
    }

    @Test
    public void notEquals_differentDisplayValue() {
        Distance distance = Distance.create(DISPLAY_DISTANCE, DISPLAY_UNIT);

        assertThat(Distance.create(DISPLAY_DISTANCE + 1, DISPLAY_UNIT)).isNotEqualTo(distance);
    }

    @Test
    public void notEquals_differentDisplayUnit() {
        Distance distance = Distance.create(DISPLAY_DISTANCE, DISPLAY_UNIT);

        assertThat(Distance.create(DISPLAY_DISTANCE, UNIT_YARDS)).isNotEqualTo(distance);
    }
}
