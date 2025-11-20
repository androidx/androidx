/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.appcompat.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatTextView} class.
 */
@SmallTest
public class AppCompatTextHelperTest {

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @SmallTest
    public void createVariationInstance_cache() {
        Typeface tf = AppCompatTextHelper.Api26Impl.createVariationInstance(
                Typeface.SANS_SERIF, "'wght' 500.0", 0);
        Typeface tf2 = AppCompatTextHelper.Api26Impl.createVariationInstance(
                Typeface.SANS_SERIF, "'wght' 500.0", 0);

        // The same argument should return cached instance.
        assertSame(tf, tf2);

        // Different font variation settings should return different instance.
        Typeface tf3 = AppCompatTextHelper.Api26Impl.createVariationInstance(
                Typeface.SANS_SERIF, "'wght' 300.0", 0);
        assertNotSame(tf, tf3);

        // Different base typeface should return different instance.
        Typeface tf4 = AppCompatTextHelper.Api26Impl.createVariationInstance(
                Typeface.SERIF, "'wght' 500.0", 0);
        assertNotSame(tf, tf4);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @SmallTest
    public void createVariationInstance_adjustment() {
        Typeface tf = Typeface.SANS_SERIF;

        Typeface w500_no_adjust = AppCompatTextHelper.Api26Impl.createVariationInstance(
                tf, "'wght' 500.0", 0);

        Typeface w500_300_adjust = AppCompatTextHelper.Api26Impl.createVariationInstance(
                tf, "'wght' 500.0", 300);

        // Adjustment should produce different Typeface.
        assertNotSame(w500_no_adjust, w500_300_adjust);
    }

    /**
     * Helper class for better test case readability.
     * See {@link #testAdjustment()} for actual test cases.
     */
    @RequiresApi(26)
    private static class Matcher {
        private final FontVariationAxis[] mActual;
        Matcher(FontVariationAxis[] actual) {
            mActual = actual;
        }

        void equals(String expectedVariationSettings) {
            FontVariationAxis[] expected;
            if (TextUtils.isEmpty(expectedVariationSettings)) {
                expected = new FontVariationAxis[0];
            } else {
                expected = FontVariationAxis.fromFontVariationSettings(expectedVariationSettings);
            }

            FontVariationAxis[] adjusted = mActual;
            assertNotNull(expected);
            assertNotNull(adjusted);
            assertEquals(expected.length, adjusted.length);
            Arrays.sort(expected, Comparator.comparing(FontVariationAxis::getTag));
            Arrays.sort(adjusted, Comparator.comparing(FontVariationAxis::getTag));

            // For the readability, string compare of the font variation settings.
            assertEquals(FontVariationAxis.toFontVariationSettings(expected),
                    FontVariationAxis.toFontVariationSettings(mActual));
        }
    }

    /**
     * Execute tests that adjust the given variation settings
     *
     * This function does weight adjustment for variation settings and verifies that the resulting
     * variation settings is same to the expected one.
     */
    @RequiresApi(26)
    private static Matcher assertAdjustment(String baseVariationSettings, int adjustment) {
        String adjustedStr = AppCompatTextHelper.Api26Impl.adjustFontVariationSettings(
                baseVariationSettings, adjustment);
        FontVariationAxis[] adjusted;
        if (TextUtils.isEmpty(adjustedStr)) {
            adjusted = new FontVariationAxis[0];
        } else {
            adjusted = FontVariationAxis.fromFontVariationSettings(adjustedStr);
        }
        return new Matcher(adjusted);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testAdjustment() {
        // No adjustment, no variation settings case.
        assertAdjustment("", 0).equals("");

        // If weight is specified in the font variation settings, it should be adjusted.
        assertAdjustment("'wght' 400.0", 300).equals("'wght' 700.0");

        // If weight is not specified, it is adjusted with assuming weight 400.
        assertAdjustment("", 300).equals("'wght' 700.0");

        // Unrelated axes should be preserved.
        assertAdjustment("'slnt' -10, 'wght' 400", 300).equals("'slnt' -10, 'wght' 700.0");
        assertAdjustment("'slnt' -10", 300).equals("'slnt' -10, 'wght' 700.0");

        // If the weight exceeds the maximum allowed value, it should be clamped.
        assertAdjustment("'wght' 700.0", 300).equals("'wght' 1000.0");

        // If the weight exceeds the minimum allowed value, it should be clamped.
        assertAdjustment("'wght' 400.0", -700).equals("'wght' 1.0");
    }
}
