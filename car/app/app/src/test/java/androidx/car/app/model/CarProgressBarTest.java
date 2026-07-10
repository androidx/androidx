/*
 * Copyright 2026 The Android Open Source Project
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

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarProgressBar}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class CarProgressBarTest {
    @Test
    public void create_defaultValues() {
        CarProgressBar bar = new CarProgressBar.Builder(0.5f).build();
        assertThat(bar.getProgress()).isEqualTo(0.5f);
        assertThat(bar.getColor()).isNull();
    }

    @Test
    public void create_withColor() {
        CarProgressBar bar = new CarProgressBar.Builder(0.5f).setColor(CarColor.RED).build();
        assertThat(bar.getProgress()).isEqualTo(0.5f);
        assertThat(bar.getColor()).isEqualTo(CarColor.RED);
    }

    @Test
    public void create_progressCoercedLower() {
        CarProgressBar bar = new CarProgressBar.Builder(-0.5f).build();
        assertThat(bar.getProgress()).isEqualTo(0.0f);
    }

    @Test
    public void create_progressCoercedUpper() {
        CarProgressBar bar = new CarProgressBar.Builder(1.5f).build();
        assertThat(bar.getProgress()).isEqualTo(1.0f);
    }

    @Test
    public void create_invalidProgress_throws() {
        assertThrows(IllegalArgumentException.class, () -> new CarProgressBar.Builder(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new CarProgressBar.Builder(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> new CarProgressBar.Builder(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void equals() {
        CarProgressBar bar1 = new CarProgressBar.Builder(0.5f).setColor(CarColor.RED).build();
        CarProgressBar bar2 = new CarProgressBar.Builder(0.5f).setColor(CarColor.RED).build();
        assertThat(bar1).isEqualTo(bar2);
    }

    @Test
    public void notEquals_differentProgress() {
        CarProgressBar bar1 = new CarProgressBar.Builder(0.5f).build();
        CarProgressBar bar2 = new CarProgressBar.Builder(0.6f).build();
        assertThat(bar1).isNotEqualTo(bar2);
    }

    @Test
    public void notEquals_differentColor() {
        CarProgressBar bar1 = new CarProgressBar.Builder(0.5f).setColor(CarColor.RED).build();
        CarProgressBar bar2 = new CarProgressBar.Builder(0.5f).setColor(CarColor.BLUE).build();
        assertThat(bar1).isNotEqualTo(bar2);
    }
}
