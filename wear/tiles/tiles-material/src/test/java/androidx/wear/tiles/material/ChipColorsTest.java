/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material;

import static androidx.wear.tiles.ColorBuilders.argb;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.ColorBuilders.ColorProp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class ChipColorsTest {
    private static final int ARGB_BACKGROUND_COLOR = 0x12345678;
    private static final int ARGB_CONTENT_COLOR = 0x11223344;
    private static final int ARGB_SECONDARY_CONTENT_COLOR = 0x11223355;
    private static final int ARGB_ICON_COLOR = 0x11223366;
    private final ColorProp mBackgroundColor = argb(ARGB_BACKGROUND_COLOR);
    private final ColorProp mContentColor = argb(ARGB_CONTENT_COLOR);
    private final ColorProp mIconColor = argb(ARGB_ICON_COLOR);
    private final ColorProp mSecondaryContentColor = argb(ARGB_SECONDARY_CONTENT_COLOR);
    private final Colors mColors = new Colors(0x123, 0x234, 0x345, 0x456);

    @Test
    public void testCreateChipColorsFromArgb() {
        ChipColors chipColors = new ChipColors(ARGB_BACKGROUND_COLOR, ARGB_CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mBackgroundColor.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mContentColor.getArgb());
    }

    @Test
    public void testCreateChipColorsFromColorProp() {
        ChipColors chipColors = new ChipColors(mBackgroundColor, mContentColor);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mBackgroundColor.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mContentColor.getArgb());
    }

    @Test
    public void testCreateChipColorsFullFromArgb() {
        ChipColors chipColors =
                new ChipColors(
                        ARGB_BACKGROUND_COLOR,
                        ARGB_ICON_COLOR,
                        ARGB_CONTENT_COLOR,
                        ARGB_SECONDARY_CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mBackgroundColor.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mIconColor.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mSecondaryContentColor.getArgb());
    }

    @Test
    public void testCreateChipColorsFullFromColorProp() {
        ChipColors chipColors =
                new ChipColors(mBackgroundColor, mIconColor, mContentColor, mSecondaryContentColor);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mBackgroundColor.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mIconColor.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mContentColor.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mSecondaryContentColor.getArgb());
    }

    @Test
    public void testCreateChipColorsFromHelperPrimary() {
        ChipColors chipColors = ChipColors.primaryChipColors(mColors);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mColors.getPrimary());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mColors.getOnPrimary());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mColors.getOnPrimary());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mColors.getOnPrimary());
    }

    @Test
    public void testCreateChipColorsFromHelperSurface() {
        ChipColors chipColors = ChipColors.secondaryChipColors(mColors);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(mColors.getSurface());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(mColors.getOnSurface());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(mColors.getOnSurface());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(mColors.getOnSurface());
    }
}
