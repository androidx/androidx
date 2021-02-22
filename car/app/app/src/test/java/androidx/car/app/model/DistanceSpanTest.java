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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link DistanceSpan}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class DistanceSpanTest {
    private final Distance mDistance =
            Distance.create(/* displayDistance= */ 10, Distance.UNIT_KILOMETERS);

    @Test
    public void constructor() {
        DistanceSpan span = DistanceSpan.create(mDistance);
        assertThat(span.getDistance()).isEqualTo(mDistance);
    }

    @Test
    public void equals() {
        DistanceSpan span = DistanceSpan.create(mDistance);
        assertThat(span).isEqualTo(DistanceSpan.create(mDistance));
    }

    @Test
    public void notEquals() {
        DistanceSpan span = DistanceSpan.create(mDistance);
        assertThat(span)
                .isNotEqualTo(
                        DistanceSpan.create(
                                Distance.create(/* displayDistance= */ 200, Distance.UNIT_METERS)));
    }
}
