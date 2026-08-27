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

package androidx.car.app.media.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import androidx.car.app.model.CarColor;
import androidx.car.app.model.StrokeCap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link MediaPlaybackStyle}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class MediaPlaybackStyleTest {
    @Test
    public void create_defaultValues() {
        MediaPlaybackStyle style = new MediaPlaybackStyle.Builder().build();
        assertNull(style.getMediaAccentColor());
        assertEquals(style.getProgressBarStrokeCap(), StrokeCap.DEFAULT);
    }

    @Test
    public void setMediaAccentColor() {
        MediaPlaybackStyle style =
                new MediaPlaybackStyle.Builder().setMediaAccentColor(CarColor.BLUE).build();
        assertEquals(style.getMediaAccentColor(), CarColor.BLUE);
        assertEquals(style.getProgressBarStrokeCap(), StrokeCap.DEFAULT);
    }

    @Test
    public void setProgressBarStrokeCap() {
        MediaPlaybackStyle style =
                new MediaPlaybackStyle.Builder().setProgressBarStrokeCap(StrokeCap.ROUND).build();
        assertNull(style.getMediaAccentColor());
        assertEquals(style.getProgressBarStrokeCap(), StrokeCap.ROUND);
    }

    @Test
    public void equals_and_hashCode() {
        MediaPlaybackStyle style1 =
                new MediaPlaybackStyle.Builder()
                        .setMediaAccentColor(CarColor.GREEN)
                        .setProgressBarStrokeCap(StrokeCap.SQUARE)
                        .build();
        MediaPlaybackStyle style2 =
                new MediaPlaybackStyle.Builder()
                        .setMediaAccentColor(CarColor.GREEN)
                        .setProgressBarStrokeCap(StrokeCap.SQUARE)
                        .build();

        assertEquals(style1, style2);
        assertEquals(style1.hashCode(), style2.hashCode());
    }

    @Test
    public void notEquals_differentColors() {
        MediaPlaybackStyle style1 =
                new MediaPlaybackStyle.Builder().setMediaAccentColor(CarColor.BLUE).build();
        MediaPlaybackStyle style2 =
                new MediaPlaybackStyle.Builder().setMediaAccentColor(CarColor.RED).build();

        assertNotEquals(style1, style2);
    }

    @Test
    public void notEquals_differentStrokeCaps() {
        MediaPlaybackStyle style1 =
                new MediaPlaybackStyle.Builder().setProgressBarStrokeCap(StrokeCap.ROUND).build();
        MediaPlaybackStyle style2 =
                new MediaPlaybackStyle.Builder().setProgressBarStrokeCap(StrokeCap.SQUARE).build();

        assertNotEquals(style1, style2);
    }

    @Test
    public void copyBuilder_copiesAllFields() {
        MediaPlaybackStyle style =
                new MediaPlaybackStyle.Builder()
                        .setMediaAccentColor(CarColor.YELLOW)
                        .setProgressBarStrokeCap(StrokeCap.ROUND)
                        .build();
        MediaPlaybackStyle copy = new MediaPlaybackStyle.Builder(style).build();

        assertEquals(style, copy);
        assertEquals(copy.getMediaAccentColor(), CarColor.YELLOW);
        assertEquals(copy.getProgressBarStrokeCap(), StrokeCap.ROUND);
    }
}
