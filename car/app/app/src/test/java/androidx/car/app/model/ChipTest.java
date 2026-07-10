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

import static androidx.car.app.model.CarIcon.ALERT;
import static androidx.car.app.model.CarIcon.BACK;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Chip}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class ChipTest {

    @Test
    public void create_defaultValues() {
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.getTitle().toString()).isEqualTo("Title");
        assertThat(chip.getStartIcon()).isNull();
        assertThat(chip.getEndIcon()).isNull();
        assertThat(chip.getOnClickDelegate()).isNotNull();
        assertThat(chip.isSelected()).isFalse();
        assertThat(chip.getStyle()).isNull();
    }

    @Test
    public void build_throws_ifNoContent() {
        assertThrows(
                IllegalStateException.class,
                () -> new Chip.Builder().setOnClickListener(() -> {}).build());
    }

    @Test
    public void build_throws_ifNoOnClickListener() {
        assertThrows(
                IllegalStateException.class,
                () -> new Chip.Builder().setTitle("Title").build());
    }

    @Test
    public void title_charSequence() {
        String title = "foo";
        Chip chip = new Chip.Builder()
                .setTitle(title)
                .setOnClickListener(() -> {})
                .build();
        assertThat(CarText.create(title)).isEqualTo(chip.getTitle());
    }

    @Test
    public void title_withVariants() {
        CarText title = new CarText.Builder("Long Title")
                .addVariant("Short")
                .build();
        Chip chip = new Chip.Builder()
                .setTitle(title)
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.getTitle()).isEqualTo(title);
        assertThat(chip.getTitle().getVariants()).hasSize(1);
        assertThat(chip.getTitle().getVariants().get(0).toString()).isEqualTo("Short");
    }

    @Test
    public void title_unsupportedSpans_throws() {
        CharSequence title = TestUtils.getCharSequenceWithClickableSpan("Title");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Chip.Builder()
                        .setTitle(title)
                        .setOnClickListener(() -> {})
                        .build()
        );

        // DurationSpan and DistanceSpan do not throw
        CharSequence title3 = TestUtils.getCharSequenceWithDistanceAndDurationSpans("Title");
        new Chip.Builder().setTitle(title3).setOnClickListener(() -> {}).build();
    }

    @Test
    public void setStartIcon() {
        CarIcon icon = BACK;
        Chip chip = new Chip.Builder()
                .setStartIcon(icon)
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.getStartIcon()).isEqualTo(icon);
    }

    @Test
    public void setEndIcon() {
        CarIcon icon = ALERT;
        Chip chip = new Chip.Builder()
                .setEndIcon(icon)
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.getEndIcon()).isEqualTo(icon);
    }

    @Test
    public void setSelected() {
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setSelected(true)
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.isSelected()).isTrue();
    }

    @Test
    public void setOnClickListener() {
        OnClickListener onClickListener = mock(OnClickListener.class);
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setOnClickListener(onClickListener)
                .build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        chip.getOnClickDelegate().sendClick(onDoneCallback);
        verify(onClickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void setStyle() {
        ChipStyle style = new ChipStyle.Builder().build();
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setStyle(style)
                .setOnClickListener(() -> {})
                .build();
        assertThat(chip.getStyle()).isEqualTo(style);
    }

    @Test
    public void equals() {
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setStartIcon(BACK)
                .setEndIcon(ALERT)
                .setSelected(true)
                .setOnClickListener(() -> {})
                .setStyle(new ChipStyle.Builder().build())
                .build();

        assertThat(new Chip.Builder()
                .setTitle("Title")
                .setStartIcon(BACK)
                .setEndIcon(ALERT)
                .setSelected(true)
                .setOnClickListener(() -> {})
                .setStyle(new ChipStyle.Builder().build())
                .build())
                .isEqualTo(chip);
    }

    @Test
    public void notEquals_differentTitle() {
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setOnClickListener(() -> {})
                .build();
        assertThat(new Chip.Builder()
                .setTitle("Other")
                .setOnClickListener(() -> {})
                .build()
        ).isNotEqualTo(chip);
    }

    @Test
    public void notEquals_differentStartIcon() {
        Chip chip = new Chip.Builder()
                .setStartIcon(BACK)
                .setOnClickListener(() -> {})
                .build();
        assertThat(new Chip.Builder()
                .setStartIcon(ALERT)
                .setOnClickListener(() -> {})
                .build()
        ).isNotEqualTo(chip);
    }

    @Test
    public void notEquals_differentEndIcon() {
        Chip chip = new Chip.Builder()
                .setEndIcon(BACK)
                .setOnClickListener(() -> {})
                .build();
        assertThat(new Chip.Builder()
                .setEndIcon(ALERT)
                .setOnClickListener(() -> {})
                .build()
        ).isNotEqualTo(chip);
    }

    @Test
    public void notEquals_differentSelected() {
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setSelected(true)
                .setOnClickListener(() -> {})
                .build();
        assertThat(new Chip.Builder()
                .setTitle("Title")
                .setSelected(false)
                .setOnClickListener(() -> {})
                .build()
        ).isNotEqualTo(chip);
    }

    @Test
    public void notEquals_differentStyle() {
        ChipStyle style1 = new ChipStyle.Builder()
                .setBackgroundColor(CarColor.RED)
                .build();
        ChipStyle style2 = new ChipStyle.Builder()
                .setBackgroundColor(CarColor.BLUE)
                .build();
        Chip chip = new Chip.Builder()
                .setTitle("Title")
                .setStyle(style1)
                .setOnClickListener(() -> {})
                .build();
        assertThat(new Chip.Builder()
                .setTitle("Title")
                .setStyle(style2)
                .setOnClickListener(() -> {})
                .build()
        ).isNotEqualTo(chip);
    }
}
