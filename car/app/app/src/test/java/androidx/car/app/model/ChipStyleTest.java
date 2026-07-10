/*
 * Copyright 2025 The Android Open Source Project
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

/** Tests for {@link ChipStyle}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class ChipStyleTest {

    @Test
    public void create_defaultValues() {
        ChipStyle style = new ChipStyle.Builder().build();
        assertThat(style.getBackgroundColor()).isNull();
        assertThat(style.getContentColor()).isNull();
        assertThat(style.getOutlineColor()).isNull();
        assertThat(style.getShape()).isNull();
    }

    @Test
    public void setBackgroundColor() {
        ChipStyle style = new ChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .build();
        assertThat(style.getBackgroundColor()).isEqualTo(CarColor.RED);
    }

    @Test
    public void setContentColor() {
        ChipStyle style = new ChipStyle.Builder()
                .setContentColor(CarColor.BLUE)
                .build();
        assertThat(style.getContentColor()).isEqualTo(CarColor.BLUE);
    }

    @Test
    public void setStrokeColor() {
        ChipStyle style = new ChipStyle.Builder()
                .setOutlineColor(CarColor.GREEN)
                .build();
        assertThat(style.getOutlineColor()).isEqualTo(CarColor.GREEN);
    }

    @Test
    public void setShape() {
        ChipStyle style = new ChipStyle.Builder()
                .setShape(Shape.CORNER_SMALL)
                .build();
        assertThat(style.getShape()).isEqualTo(Shape.CORNER_SMALL);
    }

    @Test
    public void equals() {
        ChipStyle style = new ChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .setContentColor(CarColor.BLUE)
                .setOutlineColor(CarColor.GREEN)
                .setShape(Shape.CORNER_SMALL)
                .build();

        assertThat(new ChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .setContentColor(CarColor.BLUE)
                .setOutlineColor(CarColor.GREEN)
                .setShape(Shape.CORNER_SMALL)
                .build())
                .isEqualTo(style);
    }

    @Test
    public void notEquals_differentBackgroundColor() {
        ChipStyle style = new ChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .build();
        assertThat(new ChipStyle.Builder()
                .setBackgroundColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }

    @Test
    public void notEquals_differentContentColor() {
        ChipStyle style = new ChipStyle.Builder().setContentColor(CarColor.RED).build();
        assertThat(new ChipStyle.Builder()
                .setContentColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }

    @Test
    public void notEquals_differentStrokeColor() {
        ChipStyle style = new ChipStyle.Builder()
                .setOutlineColor(CarColor.RED)
                .build();
        assertThat(new ChipStyle.Builder()
                .setOutlineColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }

    @Test
    public void notEquals_differentShape() {
        ChipStyle style = new ChipStyle.Builder()
                .setShape(Shape.CORNER_SMALL)
                .build();
        assertThat(new ChipStyle.Builder()
                .setShape(Shape.CORNER_LARGE)
                .build()
        ).isNotEqualTo(style);
    }
}
