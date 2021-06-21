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

import static androidx.car.app.model.CarColor.BLUE;
import static androidx.car.app.model.CarColor.GREEN;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarIconSpan}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ForegroundCarColorSpanTest {
    @Test
    public void constructor() {
        ForegroundCarColorSpan span = ForegroundCarColorSpan.create(BLUE);

        assertThat(span.getColor()).isEqualTo(BLUE);
    }

    @Test
    public void equals() {
        ForegroundCarColorSpan span = ForegroundCarColorSpan.create(BLUE);
        assertThat(ForegroundCarColorSpan.create(BLUE)).isEqualTo(span);
    }

    @Test
    public void notEquals() {
        ForegroundCarColorSpan span = ForegroundCarColorSpan.create(BLUE);
        assertThat(ForegroundCarColorSpan.create(GREEN)).isNotEqualTo(span);
    }

    @Test
    public void customColorAllowed() {
        CarColor customColor = CarColor.createCustom(0xdead, 0xbeef);
        ForegroundCarColorSpan span = ForegroundCarColorSpan.create(customColor);
        assertThat(span.getColor()).isEqualTo(customColor);
    }
}
