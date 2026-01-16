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

/** Tests for {@link FilterChipStyle}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class FilterChipStyleTest {

    @Test
    public void create_defaultValues() {
        FilterChipStyle style = new FilterChipStyle.Builder().build();
        assertThat(style.getBackgroundColor()).isNull();
        assertThat(style.getContentColor()).isNull();
        assertThat(style.getOutlineColor()).isNull();
    }

    @Test
    public void setBackgroundColor() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .build();
        assertThat(style.getBackgroundColor()).isEqualTo(CarColor.RED);
    }

    @Test
    public void setContentColor() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setContentColor(CarColor.BLUE)
                .build();
        assertThat(style.getContentColor()).isEqualTo(CarColor.BLUE);
    }

    @Test
    public void setStrokeColor() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setOutlineColor(CarColor.GREEN)
                .build();
        assertThat(style.getOutlineColor()).isEqualTo(CarColor.GREEN);
    }

    @Test
    public void equals() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .setContentColor(CarColor.BLUE)
                .setOutlineColor(CarColor.GREEN)
                .build();

        assertThat(new FilterChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .setContentColor(CarColor.BLUE)
                .setOutlineColor(CarColor.GREEN)
                .build())
                .isEqualTo(style);
    }

    @Test
    public void notEquals_differentBackgroundColor() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .build();
        assertThat(new FilterChipStyle.Builder()
                .setBackgroundColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }

    @Test
    public void notEquals_differentContentColor() {
        FilterChipStyle style = new FilterChipStyle.Builder().setContentColor(CarColor.RED).build();
        assertThat(new FilterChipStyle.Builder()
                .setContentColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }

    @Test
    public void notEquals_differentStrokeColor() {
        FilterChipStyle style = new FilterChipStyle.Builder()
                .setOutlineColor(CarColor.RED)
                .build();
        assertThat(new FilterChipStyle.Builder()
                .setOutlineColor(CarColor.BLUE)
                .build()
        ).isNotEqualTo(style);
    }
}
