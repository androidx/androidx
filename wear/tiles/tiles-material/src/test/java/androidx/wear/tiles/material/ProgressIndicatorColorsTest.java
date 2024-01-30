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
public class ProgressIndicatorColorsTest {
    private static final int ARGB_BACKGROUND_COLOR = 0x12345678;
    private static final int ARGB_CONTENT_COLOR = 0x11223344;
    private static final androidx.wear.tiles.ColorBuilders.ColorProp TRACK_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_BACKGROUND_COLOR);
    private static final androidx.wear.tiles.ColorBuilders.ColorProp INDICATOR_COLOR =
            androidx.wear.tiles.ColorBuilders.argb(ARGB_CONTENT_COLOR);
    private static final Colors COLORS = new Colors(0x123, 0x234, 0x345, 0x456);

    @Test
    public void testCreateProgressIndicatorColorsFromArgb() {
        ProgressIndicatorColors progressIndicatorColors =
                new ProgressIndicatorColors(ARGB_CONTENT_COLOR, ARGB_BACKGROUND_COLOR);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(TRACK_COLOR.getArgb());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(INDICATOR_COLOR.getArgb());
    }

    @Test
    public void testCreateProgressIndicatorColorsFromColorProp() {
        ProgressIndicatorColors progressIndicatorColors =
                new ProgressIndicatorColors(INDICATOR_COLOR, TRACK_COLOR);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(TRACK_COLOR.getArgb());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(INDICATOR_COLOR.getArgb());
    }

    @Test
    public void testCreateProgressIndicatorColorsFromHelper() {
        ProgressIndicatorColors progressIndicatorColors =
                ProgressIndicatorColors.progressIndicatorColors(COLORS);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(COLORS.getSurface());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(COLORS.getPrimary());
    }
}
