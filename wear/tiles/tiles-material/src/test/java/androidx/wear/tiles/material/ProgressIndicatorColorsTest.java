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
public class ProgressIndicatorColorsTest {
    private static final int ARGB_BACKGROUND_COLOR = 0x12345678;
    private static final int ARGB_CONTENT_COLOR = 0x11223344;
    private final ColorProp mTrackColor = argb(ARGB_BACKGROUND_COLOR);
    private final ColorProp mIndicatorColor = argb(ARGB_CONTENT_COLOR);
    private final Colors mColors = new Colors(0x123, 0x234, 0x345, 0x456);

    @Test
    public void testCreateProgressIndicatorColorsFromArgb() {
        ProgressIndicatorColors progressIndicatorColors =
                new ProgressIndicatorColors(ARGB_CONTENT_COLOR, ARGB_BACKGROUND_COLOR);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(mTrackColor.getArgb());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(mIndicatorColor.getArgb());
    }

    @Test
    public void testCreateProgressIndicatorColorsFromColorProp() {
        ProgressIndicatorColors progressIndicatorColors =
                new ProgressIndicatorColors(mIndicatorColor, mTrackColor);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(mTrackColor.getArgb());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(mIndicatorColor.getArgb());
    }

    @Test
    public void testCreateProgressIndicatorColorsFromHelper() {
        ProgressIndicatorColors progressIndicatorColors =
                ProgressIndicatorColors.progressIndicatorColors(mColors);

        assertThat(progressIndicatorColors.getTrackColor().getArgb())
                .isEqualTo(mColors.getSurface());
        assertThat(progressIndicatorColors.getIndicatorColor().getArgb())
                .isEqualTo(mColors.getPrimary());
    }
}
