/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.graphics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.test.InstrumentationTestCase;

import static android.support.v4.graphics.ColorUtils.HSLToColor;
import static android.support.v4.graphics.ColorUtils.calculateContrast;

/**
 * @hide
 */
public class SwatchTests extends InstrumentationTestCase {

    private static final float MIN_CONTRAST_TITLE_TEXT = 3.0f;
    private static final float MIN_CONTRAST_BODY_TEXT = 4.5f;

    private Bitmap mSource;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSource = BitmapFactory.decodeResource(getInstrumentation().getContext().getResources(),
                android.R.drawable.sym_def_app_icon);
    }

    public void testTextColorContrasts() {
        Palette p = Palette.from(mSource).generate();

        for (Palette.Swatch swatch : p.getSwatches()) {
            testSwatchTextColorContrasts(swatch);
        }
    }

    public void testHslNotNull() {
        Palette p = Palette.from(mSource).generate();

        for (Palette.Swatch swatch : p.getSwatches()) {
            assertNotNull(swatch.getHsl());
        }
    }

    public void testHslIsRgb() {
        Palette p = Palette.from(mSource).generate();

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

    public void testEqualsWhenSame() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.WHITE, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.WHITE, 50);
        assertEquals(swatch1, swatch2);
    }

    public void testEqualsWhenColorDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLACK, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.WHITE, 50);
        assertFalse(swatch1.equals(swatch2));
    }

    public void testEqualsWhenPopulationDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLACK, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.BLACK, 100);
        assertFalse(swatch1.equals(swatch2));
    }

    public void testEqualsWhenDifferent() {
        Palette.Swatch swatch1 = new Palette.Swatch(Color.BLUE, 50);
        Palette.Swatch swatch2 = new Palette.Swatch(Color.BLACK, 100);
        assertFalse(swatch1.equals(swatch2));
    }
}
