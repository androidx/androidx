/*
 * Copyright 2022 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build.VERSION_CODES;

import androidx.core.graphics.ColorUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;


@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebSettingsCompatLightThemeTest extends
        WebSettingsCompatDarkModeTestBase<WebViewLightThemeTestActivity> {
    public WebSettingsCompatLightThemeTest() {
        // targetSdkVersion to T, it is min version the algorithmic darkening works.
        // TODO(http://b/214741472): Use VERSION_CODES.TIRAMISU once available.
        super(WebViewLightThemeTestActivity.class, VERSION_CODES.CUR_DEVELOPMENT);
    }

    /**
     * Test the algorithmic darkening is disabled by default.
     */
    @Test
    public void testSimplifiedDarkMode_default() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.ALGORITHMIC_DARKENING);

        assertFalse("Algorithmic darkening should be disallowed by default",
                WebSettingsCompat.isAlgorithmicDarkeningAllowed(getSettingsOnUiThread()));
    }

    /**
     * Test web content is always light regardless the algorithmic darkening is allowed or not.
     */
    @Test
    public void testSimplifiedDarkMode_rendersLight() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.ALGORITHMIC_DARKENING);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);
        // Loading about:blank which doesn't support dark style result in a light background.
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());
        // Allowing algorithmic darkening in dark theme app should result in a dark background.
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                getSettingsOnUiThread(), true);
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be dark",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());
    }

    /**
     * Test web content is always light (if supported) on the light theme app.
     */
    @Test
    public void testSimplifiedDarkMode_pageSupportDarkTheme() {
        WebkitUtils.checkFeature(WebViewFeature.ALGORITHMIC_DARKENING);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);

        // Loading about:blank which doesn't support dark style result in a light background.
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());

        // Loading a page with dark-theme support should result in a light background (as
        // specified in media-query) in light theme app.
        getWebViewOnUiThread().loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html",
                "base64");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > 0.5f);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());
    }
}
