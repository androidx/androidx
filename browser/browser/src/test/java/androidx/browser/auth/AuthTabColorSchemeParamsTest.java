/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.auth;

import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK;
import static androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link AuthTabColorSchemeParams}. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class AuthTabColorSchemeParamsTest {
    @Test
    public void testParamsForBothSchemes() {
        AuthTabColorSchemeParams lightParams = new AuthTabColorSchemeParams.Builder()
                .setToolbarColor(0x0000ff)
                .setNavigationBarColor(0xff0000)
                .setNavigationBarDividerColor(0x00ff00)
                .build();

        AuthTabColorSchemeParams darkParams = new AuthTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .setNavigationBarColor(0xffaa00)
                .setNavigationBarDividerColor(0xffffff)
                .build();

        Intent intent = new AuthTabIntent.Builder()
                .setColorSchemeParams(COLOR_SCHEME_LIGHT, lightParams)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build().intent;

        AuthTabColorSchemeParams lightParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_LIGHT);

        AuthTabColorSchemeParams darkParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_DARK);

        assertSchemeParamsEqual(lightParams, lightParamsFromIntent);
        assertSchemeParamsEqual(darkParams, darkParamsFromIntent);
    }

    @Test
    public void testWithDefaultsForOneScheme() {
        int defaultToolbarColor = 0x0000ff;
        int defaultNavigationBarColor = 0xaabbcc;
        int defaultNavigationBarDividerColor = 0xdddddd;

        AuthTabColorSchemeParams darkParams = new AuthTabColorSchemeParams.Builder()
                .setToolbarColor(0xff0000)
                .setNavigationBarColor(0xccbbaa)
                .setNavigationBarDividerColor(0xffffff)
                .build();

        AuthTabColorSchemeParams defaultParams = new AuthTabColorSchemeParams.Builder()
                .setToolbarColor(defaultToolbarColor)
                .setNavigationBarColor(defaultNavigationBarColor)
                .setNavigationBarDividerColor(defaultNavigationBarDividerColor)
                .build();

        Intent intent = new AuthTabIntent.Builder()
                .setDefaultColorSchemeParams(defaultParams)
                .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                .build()
                .intent;

        AuthTabColorSchemeParams lightParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_LIGHT);

        AuthTabColorSchemeParams darkParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_DARK);

        assertSchemeParamsEqual(defaultParams, lightParamsFromIntent);
        assertSchemeParamsEqual(darkParams, darkParamsFromIntent);
    }

    @Test
    public void testParamsNotProvided() {
        Intent intent = new AuthTabIntent.Builder().build().intent;

        AuthTabColorSchemeParams lightParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_LIGHT);

        AuthTabColorSchemeParams darkParamsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_DARK);

        assertNull(lightParamsFromIntent.getToolbarColor());
        assertNull(lightParamsFromIntent.getNavigationBarColor());
        assertNull(lightParamsFromIntent.getNavigationBarDividerColor());

        assertNull(darkParamsFromIntent.getToolbarColor());
        assertNull(darkParamsFromIntent.getNavigationBarColor());
        assertNull(darkParamsFromIntent.getNavigationBarDividerColor());
    }

    @Test
    public void testColorsAreSolid() {
        AuthTabColorSchemeParams params = new AuthTabColorSchemeParams.Builder()
                .setToolbarColor(0x610000ff)
                .setNavigationBarColor(0x88ff0000)
                .setNavigationBarDividerColor(0x00ff00)
                .build();

        Intent intent = new AuthTabIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .build()
                .intent;

        AuthTabColorSchemeParams paramsFromIntent = AuthTabIntent.getColorSchemeParams(intent,
                COLOR_SCHEME_LIGHT);

        assertEquals(0xff0000ff, paramsFromIntent.getToolbarColor().intValue());
        assertEquals(0xffff0000, paramsFromIntent.getNavigationBarColor().intValue());
    }

    private void assertSchemeParamsEqual(AuthTabColorSchemeParams params1,
            AuthTabColorSchemeParams params2) {
        assertEquals(params1.getToolbarColor(), params2.getToolbarColor());
        assertEquals(params1.getNavigationBarColor(), params2.getNavigationBarColor());
        assertEquals(params1.getNavigationBarDividerColor(),
                params2.getNavigationBarDividerColor());
    }
}
