/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.customtabs;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests for color scheme parameters api.
 * In particular, for {@link CustomTabsIntent.Builder#setColorSchemeParams} and
 * {@link CustomTabsIntent#getColorSchemeParams}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CustomTabColorSchemeParamsTest {
    @Test
    public void testParamsProvidedForBothSchemes() {
        // Parameters for both schemes are provided as CustomTabColorSchemeParams.

        CustomTabColorSchemeParams lightParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0x0000ff)
                .setSecondaryToolbarColor(0x00aaff)
                .setNavigationBarColor(0xaabbcc)
                .build();

        CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .setSecondaryToolbarColor(0xff8800)
                .setNavigationBarColor(0x112233)
                .build();

        Intent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(0xaaaaaa) // These colors should get overridden.
                .setSecondaryToolbarColor(0x555555)
                .setNavigationBarColor(0x111111)
                .setColorSchemeParams(COLOR_SCHEME_LIGHT, lightParams)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build()
                .intent;

        CustomTabColorSchemeParams lightParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_LIGHT);

        CustomTabColorSchemeParams darkParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_DARK);

        assertSchemeParamsEqual(lightParams, lightParamsFromIntent);
        assertSchemeParamsEqual(darkParams, darkParamsFromIntent);
    }


    @Test
    public void testWithDefaultsForOneOfSchemes() {
        // Light mode parameters are provided as defaults, i.e. set directly on
        // CustomTabIntent.Builder.

        int defaultToolbarColor = 0x0000ff;
        int defaultSecondaryToolbarColor = 0x00aaff;
        int defaultNavigationBarColor = 0xaabbcc;

        CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .setSecondaryToolbarColor(0xff8800)
                .setNavigationBarColor(0x112233)
                .build();

        Intent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(defaultToolbarColor)
                .setSecondaryToolbarColor(defaultSecondaryToolbarColor)
                .setNavigationBarColor(defaultNavigationBarColor)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build()
                .intent;

        CustomTabColorSchemeParams lightParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_LIGHT);

        CustomTabColorSchemeParams darkParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_DARK);

        CustomTabColorSchemeParams expectedLightParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(defaultToolbarColor)
                .setSecondaryToolbarColor(defaultSecondaryToolbarColor)
                .setNavigationBarColor(defaultNavigationBarColor)
                .build();

        assertSchemeParamsEqual(expectedLightParams, lightParamsFromIntent);
        assertSchemeParamsEqual(darkParams, darkParamsFromIntent);
    }

    @Test
    public void testWithCommonParams() {
        // secondaryToolbarColor is common for both schemes and is set directly on
        // CustomTabIntent.Builder, while toolbarColor differs.

        int secondaryToolbarColor = 0x00aaff;

        CustomTabColorSchemeParams lightParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0x0000ff)
                .build();

        CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .build();

        Intent intent = new CustomTabsIntent.Builder()
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .setColorSchemeParams(COLOR_SCHEME_LIGHT, lightParams)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build()
                .intent;

        CustomTabColorSchemeParams lightParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_LIGHT);

        CustomTabColorSchemeParams darkParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_DARK);

        CustomTabColorSchemeParams expectedLightParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(lightParams.toolbarColor)
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .build();

        CustomTabColorSchemeParams expectedDarkParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(darkParams.toolbarColor)
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .build();

        assertSchemeParamsEqual(expectedLightParams, lightParamsFromIntent);
        assertSchemeParamsEqual(expectedDarkParams, darkParamsFromIntent);
    }

    @Test
    public void testBackwardCompatibilityWithOldClients() {
        // Backward compatibility test with a client not using CustomTabColorSchemeParams api,
        // but a browser is using it.

        int toolbarColor = 0x0000ff;
        int secondaryToolbarColor = 0x00aaff;

        Intent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(toolbarColor)
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .build()
                .intent;

        CustomTabColorSchemeParams lightParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_LIGHT);

        CustomTabColorSchemeParams darkParamsFromIntent =
                CustomTabsIntent.getColorSchemeParams(intent, COLOR_SCHEME_DARK);

        CustomTabColorSchemeParams expectedParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .build();

        assertSchemeParamsEqual(expectedParams, lightParamsFromIntent);
        assertSchemeParamsEqual(expectedParams, darkParamsFromIntent);
    }

    @Test
    public void testBackwardCompatibilityWithOldBrowsers() {
        // Backward compatibility test with a client that uses CustomTabColorSchemeParams api,
        // but a browser is not using it. The client must use CustomTabIntent.Builder methods
        // to ensure this compatibility.

        int defaultToolbarColor = 0x112233;
        int defaultSecondaryToolbarColor = 0x445566;

        CustomTabColorSchemeParams lightParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0x0000ff)
                .setSecondaryToolbarColor(0x00aaff)
                .build();

        CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .setSecondaryToolbarColor(0xff8800)
                .build();

        Intent intent = new CustomTabsIntent.Builder()
                .setToolbarColor(defaultToolbarColor)
                .setSecondaryToolbarColor(defaultSecondaryToolbarColor)
                .setColorSchemeParams(COLOR_SCHEME_LIGHT, lightParams)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build()
                .intent;

        assertEquals(defaultToolbarColor,
                intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        assertEquals(defaultSecondaryToolbarColor,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR, 0));
    }


    private void assertSchemeParamsEqual(CustomTabColorSchemeParams params1,
            CustomTabColorSchemeParams params2) {
        assertEquals(params1.toolbarColor, params2.toolbarColor);
        assertEquals(params1.secondaryToolbarColor, params2.secondaryToolbarColor);
        assertEquals(params1.navigationBarColor, params2.navigationBarColor);
    }
}
