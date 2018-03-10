/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.palette.graphics;

import static androidx.core.graphics.ColorUtils.HSLToColor;
import static androidx.core.graphics.ColorUtils.calculateContrast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SwatchTests {

    private static final float MIN_CONTRAST_TITLE_TEXT = 3.0f;
    private static final float MIN_CONTRAST_BODY_TEXT = 4.5f;

    @Test
    @SmallTest
    public void testTextColorContrasts() {
        final Palette p = Palette.from(TestUtils.loadSampleBitmap()).generate();

        for (Palette.Swatch swatch : p.getSwatches()) {
            testSwatchTextColorContrasts(swatch);
        }
    }

    @Test
    @SmallTest
    public void testHslNotNull() {
        final Palette p = Palette.from(TestUtils.loadSampleBitmap()).generate();

        for (Palette.Swatch swatch : p.getSwatches()) {
            assertNotNull(swatch.getHsl());
        }
    }

    @Test
    @SmallTest
    public void testHslIsRgb() {
        final Palette p = Palette.from(TestUtils.loadSampleBitmap()).generate();

        for (Palette.Swatch swatch : p.getSwatches()) {
            assertEquals(HSLToColor(swatch.getHsl()), swatch.getRgb());
        }
    }

    private void testSwatchTextColorContrasts(Palette.Swatch swatch) {
        final int bodyTextColor = swatch.getBodyTextColor();
        assertTrue(calculateContrast(bodyTextColor, swatch.getRgb()) >= MIN_CONTRAST_BODY_TEXT);

        final int titleTextColor = swatch.getTitleTextColor();
        assertTrue(calculateContrast(titleTextColor, swatch.getRgb()) >= MIN_CONTRAST_TITLE_TEXT);
    }

    @Test
    @SmallTest
    public void testEqualsWhenSame() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.WHITE, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.WHITE, 50);
        assertEquals(swatch1, swatch2);
    }

    @Test
    @SmallTest
    public void testEqualsWhenColorDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLACK, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.WHITE, 50);
        assertFalse(swatch1.equals(swatch2));
    }

    @Test
    @SmallTest
    public void testEqualsWhenPopulationDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLACK, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.BLACK, 100);
        assertFalse(swatch1.equals(swatch2));
    }

    @Test
    @SmallTest
    public void testEqualsWhenDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLUE, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.BLACK, 100);
        assertFalse(swatch1.equals(swatch2));
    }
}
