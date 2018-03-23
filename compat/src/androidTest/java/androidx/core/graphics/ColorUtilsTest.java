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

package androidx.core.graphics;

import static android.graphics.ColorSpace.Named.CIE_LAB;
import static android.graphics.ColorSpace.Named.SRGB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.graphics.Color;
import android.graphics.ColorSpace;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ColorUtilsTest {

    // 0.5% of the max value
    private static final float ALLOWED_OFFSET_HUE = 360 * 0.005f;
    private static final float ALLOWED_OFFSET_SATURATION = 0.005f;
    private static final float ALLOWED_OFFSET_LIGHTNESS = 0.005f;
    private static final float ALLOWED_OFFSET_MIN_ALPHA = 0.01f;
    private static final double ALLOWED_OFFSET_LAB = 0.01;
    private static final double ALLOWED_OFFSET_XYZ = 0.01;

    private static final int ALLOWED_OFFSET_RGB_COMPONENT = 2;

    private static final ArrayList<TestEntry> sEntryList = new ArrayList<>();

    static {
        sEntryList.add(new TestEntry(Color.BLACK).setHsl(0f, 0f, 0f)
                .setLab(0, 0, 0).setXyz(0, 0, 0)
                .setWhiteMinAlpha30(0.35f).setWhiteMinAlpha45(0.46f));

        sEntryList.add(new TestEntry(Color.WHITE).setHsl(0f, 0f, 1f)
                .setLab(100, 0.005, -0.01).setXyz(95.05, 100, 108.9)
                .setBlackMinAlpha30(0.42f).setBlackMinAlpha45(0.54f));

        sEntryList.add(new TestEntry(Color.BLUE).setHsl(240f, 1f, 0.5f)
                .setLab(32.303, 79.197, -107.864).setXyz(18.05, 7.22, 95.05)
                .setWhiteMinAlpha30(0.55f).setWhiteMinAlpha45(0.71f));

        sEntryList.add(new TestEntry(Color.GREEN).setHsl(120f, 1f, 0.5f)
                .setLab(87.737, -86.185, 83.181).setXyz(35.76, 71.520, 11.920)
                .setBlackMinAlpha30(0.43f).setBlackMinAlpha45(0.55f));

        sEntryList.add(new TestEntry(Color.RED).setHsl(0f, 1f, 0.5f)
                .setLab(53.233, 80.109, 67.22).setXyz(41.24, 21.26, 1.93)
                .setWhiteMinAlpha30(0.84f).setBlackMinAlpha30(0.55f).setBlackMinAlpha45(0.78f));

        sEntryList.add(new TestEntry(Color.CYAN).setHsl(180f, 1f, 0.5f)
                .setLab(91.117, -48.08, -14.138).setXyz(53.81, 78.74, 106.97)
                .setBlackMinAlpha30(0.43f).setBlackMinAlpha45(0.55f));

        sEntryList.add(new TestEntry(0xFF2196F3).setHsl(207f, 0.9f, 0.54f)
                .setLab(60.433, 2.091, -55.116).setXyz(27.711, 28.607, 88.855)
                .setBlackMinAlpha30(0.52f).setWhiteMinAlpha30(0.97f).setBlackMinAlpha45(0.7f));

        sEntryList.add(new TestEntry(0xFFD1C4E9).setHsl(261f, 0.46f, 0.84f)
                .setLab(81.247, 11.513, -16.677).setXyz(60.742, 58.918, 85.262)
                .setBlackMinAlpha30(0.45f).setBlackMinAlpha45(0.58f));

        sEntryList.add(new TestEntry(0xFF311B92).setHsl(251.09f, 0.687f, 0.339f)
                .setLab(21.988, 44.301, -60.942).setXyz(6.847, 3.512, 27.511)
                .setWhiteMinAlpha30(0.39f).setWhiteMinAlpha45(0.54f));
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test public void addColorsDifferentModels() {
        Color lab = Color.valueOf(54.0f, 80.0f, 70.0f, 1.0f, ColorSpace.get(CIE_LAB));
        Color rgb = Color.valueOf(0.0f, 0.5f, 0.0f, 0.5f, ColorSpace.get(SRGB));
        try {
            ColorUtils.compositeColors(rgb, lab);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Color models must match (RGB vs. LAB)", e.getMessage());
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test public void addColorsSameColorSpace() {
        Color result = ColorUtils.compositeColors(Color.valueOf(0x7f007f00),
                Color.valueOf(0x7f7f0000));
        assertEquals(0.16f, result.red(), 1e-2f);
        assertEquals(0.33f, result.green(), 1e-2f);
        assertEquals(0.00f, result.blue(), 1e-2f);
        assertEquals(0.75f, result.alpha(), 1e-2f);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test public void addColorsDifferentColorSpace() {
        ColorSpace p3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3);
        Color red = Color.valueOf(0.5f, 0.0f, 0.0f, 0.5f, p3);
        Color green = Color.valueOf(0x7f007f00);

        Color mixed = ColorUtils.compositeColors(green, red);
        assertEquals(p3, mixed.getColorSpace());
        assertEquals(0.31f, mixed.red(), 1e-2f);
        assertEquals(0.33f, mixed.green(), 1e-2f);
        assertEquals(0.09f, mixed.blue(), 1e-2f);
        assertEquals(0.75f, mixed.alpha(), 1e-2f);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test public void addColorsZeroAlpha() {
        // Test potential divide by zero
        assertEquals(0, ColorUtils.compositeColors(Color.valueOf(0x00007f00),
                Color.valueOf(0x007f0000)).toArgb());

        // Test low alpha
        Color result = ColorUtils.compositeColors(Color.valueOf(0.0f, 1.0f, 0.0f, 0.0001f),
                Color.valueOf(1.0f, 0.0f, 0.0f, 0.0001f));
        assertEquals(0.50f, result.red(), 1e-2f);
        assertEquals(0.50f, result.green(), 1e-2f);
        assertEquals(0.00f, result.blue(), 1e-2f);
        assertEquals(2e-4f, result.alpha(), 1e-5f);
    }

    @Test
    public void testColorToHSL() {
        for (TestEntry entry : sEntryList) {
            verifyColorToHSL(entry.rgb, entry.hsl);
        }
    }

    @Test
    public void testHSLToColor() {
        for (TestEntry entry : sEntryList) {
            verifyHSLToColor(entry.hsl, entry.rgb);
        }
    }

    @Test
    public void testColorToHslLimits() {
        final float[] hsl = new float[3];

        for (TestEntry entry : sEntryList) {
            ColorUtils.colorToHSL(entry.rgb, hsl);

            assertTrue(hsl[0] >= 0f && hsl[0] <= 360f);
            assertTrue(hsl[1] >= 0f && hsl[1] <= 1f);
            assertTrue(hsl[2] >= 0f && hsl[2] <= 1f);
        }
    }

    @Test
    public void testColorToXYZ() {
        for (TestEntry entry : sEntryList) {
            verifyColorToXYZ(entry.rgb, entry.xyz);
        }
    }

    @Test
    public void testColorToLAB() {
        for (TestEntry entry : sEntryList) {
            verifyColorToLAB(entry.rgb, entry.lab);
        }
    }

    @Test
    public void testLABToXYZ() {
        for (TestEntry entry : sEntryList) {
            verifyLABToXYZ(entry.lab, entry.xyz);
        }
    }

    @Test
    public void testXYZToColor() {
        for (TestEntry entry : sEntryList) {
            verifyXYZToColor(entry.xyz, entry.rgb);
        }
    }

    @Test
    public void testLABToColor() {
        for (TestEntry entry : sEntryList) {
            verifyLABToColor(entry.lab, entry.rgb);
        }
    }

    @Test
    public void testMinAlphas() {
        for (TestEntry entry : sEntryList) {
            verifyMinAlpha("Black title", entry.rgb, entry.blackMinAlpha30,
                    ColorUtils.calculateMinimumAlpha(Color.BLACK, entry.rgb, 3.0f));
            verifyMinAlpha("Black body", entry.rgb, entry.blackMinAlpha45,
                    ColorUtils.calculateMinimumAlpha(Color.BLACK, entry.rgb, 4.5f));
            verifyMinAlpha("White title", entry.rgb, entry.whiteMinAlpha30,
                    ColorUtils.calculateMinimumAlpha(Color.WHITE, entry.rgb, 3.0f));
            verifyMinAlpha("White body", entry.rgb, entry.whiteMinAlpha45,
                    ColorUtils.calculateMinimumAlpha(Color.WHITE, entry.rgb, 4.5f));
        }
    }

    @Test
    public void testCircularInterpolationForwards() {
        assertEquals(0f, ColorUtils.circularInterpolate(0, 180, 0f), 0f);
        assertEquals(90f, ColorUtils.circularInterpolate(0, 180, 0.5f), 0f);
        assertEquals(180f, ColorUtils.circularInterpolate(0, 180, 1f), 0f);
    }

    @Test
    public void testCircularInterpolationBackwards() {
        assertEquals(180f, ColorUtils.circularInterpolate(180, 0, 0f), 0f);
        assertEquals(90f, ColorUtils.circularInterpolate(180, 0, 0.5f), 0f);
        assertEquals(0f, ColorUtils.circularInterpolate(180, 0, 1f), 0f);
    }

    @Test
    public void testCircularInterpolationCrossZero() {
        assertEquals(270f, ColorUtils.circularInterpolate(270, 90, 0f), 0f);
        assertEquals(180f, ColorUtils.circularInterpolate(270, 90, 0.5f), 0f);
        assertEquals(90f, ColorUtils.circularInterpolate(270, 90, 1f), 0f);
    }

    private static void verifyMinAlpha(String title, int color, float expected, int actual) {
        final String message = title + " text within error for #" + Integer.toHexString(color);
        if (expected < 0) {
            assertEquals(message, actual, -1);
        } else {
            assertEquals(message, expected, actual / 255f, ALLOWED_OFFSET_MIN_ALPHA);
        }
    }

    private static void verifyColorToHSL(int color, float[] expected) {
        float[] actualHSL = new float[3];
        ColorUtils.colorToHSL(color, actualHSL);

        assertEquals("Hue not within offset", expected[0], actualHSL[0],
                ALLOWED_OFFSET_HUE);
        assertEquals("Saturation not within offset", expected[1], actualHSL[1],
                ALLOWED_OFFSET_SATURATION);
        assertEquals("Lightness not within offset", expected[2], actualHSL[2],
                ALLOWED_OFFSET_LIGHTNESS);
    }

    private static void verifyHSLToColor(float[] hsl, int expected) {
        final int actualRgb = ColorUtils.HSLToColor(hsl);

        assertEquals("Red not within offset", Color.red(expected), Color.red(actualRgb),
                ALLOWED_OFFSET_RGB_COMPONENT);
        assertEquals("Green not within offset", Color.green(expected), Color.green(actualRgb),
                ALLOWED_OFFSET_RGB_COMPONENT);
        assertEquals("Blue not within offset", Color.blue(expected), Color.blue(actualRgb),
                ALLOWED_OFFSET_RGB_COMPONENT);
    }

    private static void verifyColorToLAB(int color, double[] expected) {
        double[] result = new double[3];
        ColorUtils.colorToLAB(color, result);

        assertEquals("L not within offset", expected[0], result[0], ALLOWED_OFFSET_LAB);
        assertEquals("A not within offset", expected[1], result[1], ALLOWED_OFFSET_LAB);
        assertEquals("B not within offset", expected[2], result[2], ALLOWED_OFFSET_LAB);
    }

    private static void verifyColorToXYZ(int color, double[] expected) {
        double[] result = new double[3];
        ColorUtils.colorToXYZ(color, result);

        assertEquals("X not within offset", expected[0], result[0], ALLOWED_OFFSET_XYZ);
        assertEquals("Y not within offset", expected[1], result[1], ALLOWED_OFFSET_XYZ);
        assertEquals("Z not within offset", expected[2], result[2], ALLOWED_OFFSET_XYZ);
    }

    private static void verifyLABToXYZ(double[] lab, double[] expected) {
        double[] result = new double[3];
        ColorUtils.LABToXYZ(lab[0], lab[1], lab[2], result);

        assertEquals("X not within offset", expected[0], result[0], ALLOWED_OFFSET_XYZ);
        assertEquals("Y not within offset", expected[1], result[1], ALLOWED_OFFSET_XYZ);
        assertEquals("Z not within offset", expected[2], result[2], ALLOWED_OFFSET_XYZ);
    }

    private static void verifyXYZToColor(double[] xyz, int expected) {
        final int result = ColorUtils.XYZToColor(xyz[0], xyz[1], xyz[2]);
        verifyRGBComponentsClose(expected, result);
    }

    private static void verifyLABToColor(double[] lab, int expected) {
        final int result = ColorUtils.LABToColor(lab[0], lab[1], lab[2]);
        verifyRGBComponentsClose(expected, result);
    }

    private static void verifyRGBComponentsClose(int expected, int actual) {
        final String message = "Expected: #" + Integer.toHexString(expected)
                + ", Actual: #" + Integer.toHexString(actual);
        assertEquals("R not equal: " + message, Color.red(expected), Color.red(actual), 1);
        assertEquals("G not equal: " + message, Color.green(expected), Color.green(actual), 1);
        assertEquals("B not equal: " + message, Color.blue(expected), Color.blue(actual), 1);
    }

    private static class TestEntry {
        final int rgb;
        final float[] hsl = new float[3];
        final double[] xyz = new double[3];
        final double[] lab = new double[3];

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

        TestEntry setXyz(double x, double y, double z) {
            xyz[0] = x;
            xyz[1] = y;
            xyz[2] = z;
            return this;
        }

        TestEntry setLab(double l, double a, double b) {
            lab[0] = l;
            lab[1] = a;
            lab[2] = b;
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
