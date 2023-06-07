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

package androidx.webkit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build.VERSION_CODES;

import androidx.core.graphics.ColorUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Instrumentation tests for pre-33 dark mode behavior.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = VERSION_CODES.LOLLIPOP)
public class WebSettingsCompatForceDarkTest extends
        WebSettingsCompatDarkModeTestBase<WebViewLightThemeTestActivity> {
    public WebSettingsCompatForceDarkTest() {
        super(WebViewLightThemeTestActivity.class);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testForceDark_default. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testForceDark_default() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);

        Assert.assertEquals("The default force dark state should be AUTO",
                WebSettingsCompat.FORCE_DARK_AUTO,
                WebSettingsCompat.getForceDark(getSettingsOnUiThread()));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testForceDark_rendersDark. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testForceDark_rendersDark() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize();

        // Loading about:blank into a force-dark-on webview should result in a dark background
        WebSettingsCompat.setForceDark(
                getSettingsOnUiThread(), WebSettingsCompat.FORCE_DARK_ON);
        assertEquals("Force dark should have been set to ON",
                WebSettingsCompat.FORCE_DARK_ON,
                WebSettingsCompat.getForceDark(getSettingsOnUiThread()));

        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be dark",
                ColorUtils.calculateLuminance(getWebPageColor()) < 0.5f);

        // Loading about:blank into a force-dark-off webview should result in a light background
        WebSettingsCompat.setForceDark(
                getSettingsOnUiThread(), WebSettingsCompat.FORCE_DARK_OFF);
        assertEquals("Force dark should have been set to OFF",
                WebSettingsCompat.FORCE_DARK_OFF,
                WebSettingsCompat.getForceDark(getSettingsOnUiThread()));

        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
    }

    /**
     * Test to exercise USER_AGENT_DARKENING_ONLY option,
     * i.e. web contents are always darkened by a user agent.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testForceDark_userAgentDarkeningOnly() {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK_STRATEGY);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize();

        // Loading empty page with or without dark theme support into a force-dark-on webview with
        // force dark only algorithm should result in a dark background.
        WebSettingsCompat.setForceDark(
                getSettingsOnUiThread(), WebSettingsCompat.FORCE_DARK_ON);
        WebSettingsCompat.setForceDarkStrategy(getSettingsOnUiThread(),
                WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY);


        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be dark",
                ColorUtils.calculateLuminance(getWebPageColor()) < 0.5f);
        assertFalse(prefersDarkTheme());

        getWebViewOnUiThread().loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html",
                "base64");
        assertTrue("Bitmap colour should be dark",
                ColorUtils.calculateLuminance(getWebPageColor()) < 0.5f);
        assertFalse(prefersDarkTheme());
    }

    /**
     * Test to exercise WEB_THEME_DARKENING_ONLY option,
     * i.e. web contents are darkened only by web theme.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testForceDark_webThemeDarkeningOnly() {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK_STRATEGY);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize();

        WebSettingsCompat.setForceDark(
                getSettingsOnUiThread(), WebSettingsCompat.FORCE_DARK_ON);
        WebSettingsCompat.setForceDarkStrategy(getSettingsOnUiThread(),
                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);

        // Loading a page without dark-theme support should result in a light background as web
        // page is not darken by a user agent
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
        assertTrue(prefersDarkTheme());

        // Loading a page with dark-theme support should result in a green background (as
        // specified in media-query)
        getWebViewOnUiThread().loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html",
                "base64");
        assertThat("Bitmap colour should be green", getWebPageColor(), isGreen());
        assertTrue(prefersDarkTheme());
    }
}
