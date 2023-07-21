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

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
public class ChipColorsTest {
    private static final int ARGB_BACKGROUND_COLOR = 0x12345678;
    private static final int ARGB_CONTENT_COLOR = 0x11223344;
    private static final int ARGB_SECONDARY_CONTENT_COLOR = 0x11223355;
    private static final int ARGB_ICON_COLOR = 0x11223366;
    private static final androidx.wear.tiles.ColorBuilders.ColorProp BACKGROUND_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_BACKGROUND_COLOR);
    private static final androidx.wear.tiles.ColorBuilders.ColorProp CONTENT_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_CONTENT_COLOR);
    private static final androidx.wear.tiles.ColorBuilders.ColorProp ICON_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_ICON_COLOR);
    private static final androidx.wear.tiles.ColorBuilders.ColorProp SECONDARY_CONTENT_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_SECONDARY_CONTENT_COLOR);
    private static final Colors COLORS = new Colors(0x123, 0x234, 0x345, 0x456);

    @Test
    public void testCreateChipColorsFromArgb() {
        ChipColors chipColors = new ChipColors(ARGB_BACKGROUND_COLOR, ARGB_CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateChipColorsFromColorProp() {
        ChipColors chipColors = new ChipColors(BACKGROUND_COLOR, CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateChipColorsFullFromArgb() {
        ChipColors chipColors =
                new ChipColors(
                        ARGB_BACKGROUND_COLOR,
                        ARGB_ICON_COLOR,
                        ARGB_CONTENT_COLOR,
                        ARGB_SECONDARY_CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(ICON_COLOR.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(SECONDARY_CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateChipColorsFullFromColorProp() {
        ChipColors chipColors =
                new ChipColors(
                        BACKGROUND_COLOR, ICON_COLOR, CONTENT_COLOR, SECONDARY_CONTENT_COLOR);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(ICON_COLOR.getArgb());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(SECONDARY_CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateChipColorsFromHelperPrimary() {
        ChipColors chipColors = ChipColors.primaryChipColors(COLORS);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(COLORS.getPrimary());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(COLORS.getOnPrimary());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(COLORS.getOnPrimary());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(COLORS.getOnPrimary());
    }

    @Test
    public void testCreateChipColorsFromHelperSurface() {
        ChipColors chipColors = ChipColors.secondaryChipColors(COLORS);

        assertThat(chipColors.getBackgroundColor().getArgb()).isEqualTo(COLORS.getSurface());
        assertThat(chipColors.getIconColor().getArgb()).isEqualTo(COLORS.getOnSurface());
        assertThat(chipColors.getContentColor().getArgb()).isEqualTo(COLORS.getOnSurface());
        assertThat(chipColors.getSecondaryContentColor().getArgb())
                .isEqualTo(COLORS.getOnSurface());
    }
}
