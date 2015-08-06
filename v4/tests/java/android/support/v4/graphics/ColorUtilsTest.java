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

package android.support.v4.graphics;

import android.graphics.Color;
import android.test.AndroidTestCase;

import java.lang.Integer;
import java.util.ArrayList;

/**
 * @hide
 */
public class ColorUtilsTest extends AndroidTestCase {

    // 0.5% of the max value
    private static final float ALLOWED_OFFSET_HUE = 360 * 0.005f;
    private static final float ALLOWED_OFFSET_SATURATION = 0.005f;
    private static final float ALLOWED_OFFSET_LIGHTNESS = 0.005f;
    private static final float ALLOWED_OFFSET_MIN_ALPHA = 0.01f;

    private static final int ALLOWED_OFFSET_RGB_COMPONENT = 2;

    private static final ArrayList<TestEntry> sEntryList = new ArrayList<>();

    static {
        sEntryList.add(new TestEntry(Color.BLACK).setHsl(0f, 0f, 0f)
                .setWhiteMinAlpha30(0.35f).setWhiteMinAlpha45(0.46f));
        sEntryList.add(new TestEntry(Color.WHITE).setHsl(0f, 0f, 1f)
                .setBlackMinAlpha30(0.42f).setBlackMinAlpha45(0.54f));
        sEntryList.add(new TestEntry(Color.BLUE).setHsl(240f, 1f, 0.5f)
                .setWhiteMinAlpha30(0.55f).setWhiteMinAlpha45(0.71f));
        sEntryList.add(new TestEntry(Color.GREEN).setHsl(120f, 1f, 0.5f)
                .setBlackMinAlpha30(0.43f).setBlackMinAlpha45(0.55f));
        sEntryList.add(new TestEntry(Color.RED).setHsl(0f, 1f, 0.5f)
                .setWhiteMinAlpha30(0.84f).setBlackMinAlpha30(0.55f).setBlackMinAlpha45(0.78f));
        sEntryList.add(new TestEntry(Color.CYAN).setHsl(180f, 1f, 0.5f)
                .setBlackMinAlpha30(0.43f).setBlackMinAlpha45(0.55f));
        sEntryList.add(new TestEntry(0xFF2196F3).setHsl(207f, 0.9f, 0.54f)
                .setBlackMinAlpha30(0.52f).setWhiteMinAlpha30(0.97f).setBlackMinAlpha45(0.7f));
        sEntryList.add(new TestEntry(0xFFD1C4E9).setHsl(261f, 0.46f, 0.84f)
                .setBlackMinAlpha30(0.45f).setBlackMinAlpha45(0.58f));
        sEntryList.add(new TestEntry(0xFF311B92).setHsl(251.09f, 0.687f, 0.339f)
                .setWhiteMinAlpha30(0.39f).setWhiteMinAlpha45(0.54f));
    }

    public void testToHSL() {
        for (TestEntry entry : sEntryList) {
            testColorToHSL(entry.rgb, entry.hsl);
        }
    }

    public void testFromHSL() {
        for (TestEntry entry : sEntryList) {
            testHSLToColor(entry.hsl, entry.rgb);
        }
    }

    public void testToHslLimits() {
        final float[] hsl = new float[3];

        for (TestEntry entry : sEntryList) {
            ColorUtils.colorToHSL(entry.rgb, hsl);

            assertTrue(hsl[0] >= 0f && hsl[0] <= 360f);
            assertTrue(hsl[1] >= 0f && hsl[1] <= 1f);
            assertTrue(hsl[2] >= 0f && hsl[2] <= 1f);
        }
    }

    public void testMinAlphas() {
        for (TestEntry entry : sEntryList) {
            testMinAlpha("Black title", entry.rgb, entry.blackMinAlpha30,
                    ColorUtils.calculateMinimumAlpha(Color.BLACK, entry.rgb, 3.0f));
            testMinAlpha("Black body", entry.rgb, entry.blackMinAlpha45,
                    ColorUtils.calculateMinimumAlpha(Color.BLACK, entry.rgb, 4.5f));
            testMinAlpha("White title", entry.rgb, entry.whiteMinAlpha30,
                    ColorUtils.calculateMinimumAlpha(Color.WHITE, entry.rgb, 3.0f));
            testMinAlpha("White body", entry.rgb, entry.whiteMinAlpha45,
                    ColorUtils.calculateMinimumAlpha(Color.WHITE, entry.rgb, 4.5f));
        }
    }

    private static void testMinAlpha(String title, int color, float expected, int actual) {
        final String message = title + " text within error for #" + Integer.toHexString(color);
        if (expected < 0) {
            assertEquals(message, actual, -1);
        } else {
            assertClose(message, expected, actual / 255f, ALLOWED_OFFSET_MIN_ALPHA);
        }
    }

    private static void assertClose(String message, float expected, float actual,
            float allowedOffset) {
        StringBuilder sb = new StringBuilder(message);
        sb.append(". Expected: ").append(expected).append(". Actual: ").append(actual);

        assertTrue(sb.toString(), Math.abs(expected - actual) <= allowedOffset);
    }

    private static void assertClose(String message, int expected, int actual,
            int allowedOffset) {
        StringBuilder sb = new StringBuilder(message);
        sb.append(". Expected: ").append(expected).append(". Actual: ").append(actual);

        assertTrue(sb.toString(), Math.abs(expected - actual) <= allowedOffset);
    }

    private static void testColorToHSL(int color, float[] expectedHsl) {
        float[] actualHSL = new float[3];
        ColorUtils.colorToHSL(color, actualHSL);

        assertClose("Hue not within offset", expectedHsl[0], actualHSL[0],
                ALLOWED_OFFSET_HUE);
        assertClose("Saturation not within offset", expectedHsl[1], actualHSL[1],
                ALLOWED_OFFSET_SATURATION);
        assertClose("Lightness not within offset", expectedHsl[2], actualHSL[2],
                ALLOWED_OFFSET_LIGHTNESS);
    }

    private static void testHSLToColor(float[] hsl, int expectedRgb) {
        final int actualRgb = ColorUtils.HSLToColor(hsl);

        assertClose("Red not within offset",
                Color.red(expectedRgb), Color.red(actualRgb), ALLOWED_OFFSET_RGB_COMPONENT);
        assertClose("Green not within offset",
                Color.green(expectedRgb), Color.green(actualRgb), ALLOWED_OFFSET_RGB_COMPONENT);
        assertClose("Blue not within offset",
                Color.blue(expectedRgb), Color.blue(actualRgb), ALLOWED_OFFSET_RGB_COMPONENT);
    }

    private static class TestEntry {
        final int rgb;
        final float[] hsl = new float[3];
        float blackMinAlpha45 = -1;
        float blackMinAlpha30 = -1;
        float whiteMinAlpha45 = -1;
        float whiteMinAlpha30 = -1;

        TestEntry(int rgb) {
            this.rgb = rgb;
        }

        TestEntry setHsl(float h, float s, float l) {
            hsl[0] = h;
            hsl[1] = s;
            hsl[2] = l;
            return this;
        }

        TestEntry setBlackMinAlpha30(float minAlpha) {
            blackMinAlpha30 = minAlpha;
            return this;
        }

        TestEntry setBlackMinAlpha45(float minAlpha) {
            blackMinAlpha45 = minAlpha;
            return this;
        }

        TestEntry setWhiteMinAlpha30(float minAlpha) {
            whiteMinAlpha30 = minAlpha;
            return this;
        }

        TestEntry setWhiteMinAlpha45(float minAlpha) {
            whiteMinAlpha45 = minAlpha;
            return this;
        }
    }
}