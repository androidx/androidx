/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.Utils;

import org.junit.Test;

import java.util.Random;

public class ColorUtilsTest {

    /** Color utils to cross check implementation */
    private static float[] convertRGBtoHSV(int r, int g, int b) {

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        float[] hsv = new float[3];

        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        int delta = max - min;

        float v = (float) max / 255.0f;
        hsv[2] = v;

        if (delta == 0) {
            hsv[0] = 0.0f;
            hsv[1] = 0.0f;
            return hsv;
        }

        float s = (float) delta / (float) max;
        hsv[1] = s;

        float h;
        if (r == max) {
            h = (float) (g - b) / (float) delta;
        } else if (g == max) {
            h = 2.0f + (float) (b - r) / (float) delta;
        } else {
            h = 4.0f + (float) (r - g) / (float) delta;
        }

        h *= 60.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        hsv[0] = h;
        return hsv;
    }

    /** Color utils to cross check implementation */
    private static int convertHSVtoRGB(int alpha, float[] hsv) {
        if (hsv == null || hsv.length < 3) {
            throw new IllegalArgumentException(
                    "HSV array must be non-null and have at least 3 elements.");
        }

        float h = hsv[0];
        float s = Math.max(0.0f, Math.min(1.0f, hsv[1]));
        float v = Math.max(0.0f, Math.min(1.0f, hsv[2]));
        alpha = Math.max(0, Math.min(255, alpha));
        int v_byte = Math.round(v * 255.0f);
        if (s == 0.0f) {
            return (alpha << 24) | (v_byte << 16) | (v_byte << 8) | v_byte;
        }

        float hx = (h < 0.0f || h >= 360.0f) ? 0.0f : h / 60.0f;
        float w_float = (float) Math.floor(hx);
        int w = (int) w_float;
        float f = hx - w_float;
        int p = Math.round((1.0f - s) * v * 255.0f);
        int q = Math.round((1.0f - (s * f)) * v * 255.0f);

        int t = Math.round((1.0f - (s * (1.0f - f))) * v * 255.0f);
        int r, g, b;
        switch (w % 6) {
            case 0:
                r = v_byte;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v_byte;
                b = p;
                break;
            case 2:
                r = p;
                g = v_byte;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v_byte;
                break;
            case 4:
                r = t;
                g = p;
                b = v_byte;
                break;
            case 5:
            default:
                r = v_byte;
                g = p;
                b = q;
                break;
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** This compares the hsv and rgb utils against ones based on the android implementation */
    @Test
    public void testHSVtoRGB() {
        Random r = new Random(12323);
        for (int i = 0; i < 20; i++) {
            float h = r.nextFloat();
            float s = r.nextFloat();
            float v = r.nextFloat();

            int rgb = Utils.hsvToRgb(h, s, v);
            int expected = convertHSVtoRGB(255, new float[] {h * 360, s, v});
            assertEquals(Integer.toHexString(expected), Integer.toHexString(rgb));
        }
        for (int i = 0; i < 20; i++) {
            int rgb = r.nextInt();

            float h = Utils.getHue(rgb);
            float s = Utils.getSaturation(rgb);
            float v = Utils.getBrightness(rgb);

            float[] expected = convertRGBtoHSV((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
            System.out.println(expected[0] / 360 + " " + expected[1] + " " + expected[2]);
            System.out.println(h + " " + s + " " + v);
            assertEquals(expected[0] / 360, h, 0.01f);
            assertEquals(expected[1], s, 0.01f);
            assertEquals(expected[2], v, 0.01f);
        }
    }
}
