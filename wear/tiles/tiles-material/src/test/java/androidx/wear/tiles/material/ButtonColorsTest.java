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
public class ButtonColorsTest {
    private static final int ARGB_BACKGROUND_COLOR = 0x12345678;
    private static final int ARGB_CONTENT_COLOR = 0x11223344;
    private static final androidx.wear.tiles.ColorBuilders.ColorProp BACKGROUND_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_BACKGROUND_COLOR);
    private static final androidx.wear.tiles.ColorBuilders.ColorProp CONTENT_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_CONTENT_COLOR);
    private static final Colors COLORS = new Colors(0x123, 0x234, 0x345, 0x456);

    @Test
    public void testCreateButtonColorsFromArgb() {
        ButtonColors buttonColors = new ButtonColors(ARGB_BACKGROUND_COLOR, ARGB_CONTENT_COLOR);

        assertThat(buttonColors.getBackgroundColor().getArgb())
                .isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(buttonColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateButtonColorsFromColorProp() {
        ButtonColors buttonColors = new ButtonColors(BACKGROUND_COLOR, CONTENT_COLOR);

        assertThat(buttonColors.getBackgroundColor().getArgb())
                .isEqualTo(BACKGROUND_COLOR.getArgb());
        assertThat(buttonColors.getContentColor().getArgb()).isEqualTo(CONTENT_COLOR.getArgb());
    }

    @Test
    public void testCreateButtonColorsFromHelperPrimary() {
        ButtonColors buttonColors = ButtonColors.primaryButtonColors(COLORS);

        assertThat(buttonColors.getBackgroundColor().getArgb()).isEqualTo(COLORS.getPrimary());
        assertThat(buttonColors.getContentColor().getArgb()).isEqualTo(COLORS.getOnPrimary());
    }

    @Test
    public void testCreateButtonColorsFromHelperSurface() {
        ButtonColors buttonColors = ButtonColors.secondaryButtonColors(COLORS);

        assertThat(buttonColors.getBackgroundColor().getArgb()).isEqualTo(COLORS.getSurface());
        assertThat(buttonColors.getContentColor().getArgb()).isEqualTo(COLORS.getOnSurface());
    }
}
