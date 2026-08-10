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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarProgressBarStyle}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class CarProgressBarStyleTest {
    @Test
    public void create_defaultValues() {
        CarProgressBarStyle style = new CarProgressBarStyle.Builder().build();
        assertThat(style.getColor()).isNull();
        assertThat(style.getStrokeCap()).isEqualTo(CarProgressBarStyle.STROKE_CAP_DEFAULT);
    }

    @Test
    public void setColor() {
        CarColor color = CarColor.BLUE;
        CarProgressBarStyle style = new CarProgressBarStyle.Builder().setColor(color).build();
        assertThat(style.getColor()).isEqualTo(color);
        assertThat(style.getStrokeCap()).isEqualTo(CarProgressBarStyle.STROKE_CAP_DEFAULT);
    }

    @Test
    public void setStrokeCap() {
        int strokeCap = CarProgressBarStyle.STROKE_CAP_SQUARE;
        CarProgressBarStyle style = new CarProgressBarStyle.Builder().setStrokeCap(
                strokeCap).build();
        assertThat(style.getColor()).isNull();
        assertThat(style.getStrokeCap()).isEqualTo(strokeCap);
    }

    @Test
    public void equals() {
        CarProgressBarStyle style1 = new CarProgressBarStyle.Builder().setColor(
                CarColor.BLUE).setStrokeCap(CarProgressBarStyle.STROKE_CAP_ROUND).build();
        CarProgressBarStyle style2 = new CarProgressBarStyle.Builder().setColor(
                CarColor.BLUE).setStrokeCap(CarProgressBarStyle.STROKE_CAP_ROUND).build();
        assertThat(style1).isEqualTo(style2);
    }

    @Test
    public void notEquals_differentColors() {
        CarProgressBarStyle style1 = new CarProgressBarStyle.Builder().setColor(
                CarColor.BLUE).build();
        CarProgressBarStyle style2 = new CarProgressBarStyle.Builder().setColor(
                CarColor.RED).build();
        assertThat(style1).isNotEqualTo(style2);
    }

    @Test
    public void notEquals_differentStrokeCaps() {
        CarProgressBarStyle style1 = new CarProgressBarStyle.Builder().setStrokeCap(
                CarProgressBarStyle.STROKE_CAP_SQUARE).build();
        CarProgressBarStyle style2 = new CarProgressBarStyle.Builder().setStrokeCap(
                CarProgressBarStyle.STROKE_CAP_ROUND).build();
        assertThat(style1).isNotEqualTo(style2);
    }

    @Test
    public void copy_equals() {
        CarProgressBarStyle style = new CarProgressBarStyle.Builder().setColor(
                CarColor.BLUE).setStrokeCap(CarProgressBarStyle.STROKE_CAP_ROUND).build();
        CarProgressBarStyle copiedStyle = new CarProgressBarStyle.Builder(style).build();
        assertThat(copiedStyle).isEqualTo(style);
    }
}
