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

import android.os.Build;

import androidx.core.graphics.ColorUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for light mode on targetSdk >= 33.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class WebSettingsCompatLightThemeTest extends
        WebSettingsCompatDarkModeTestBase<WebViewLightThemeTestActivity> {
    public WebSettingsCompatLightThemeTest() {
        super(WebViewLightThemeTestActivity.class);
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
        setWebViewSize();
        float expected_luminance = 0.5f;
        // Theme.AppCompat.Light has a darker background in api 33 than previous.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            expected_luminance = 0.4f;
        }
        // Loading about:blank which doesn't support dark style result in a light background.
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > expected_luminance);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());
        // Allowing algorithmic darkening in light theme app should not take any effect.
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                getSettingsOnUiThread(), true);
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > expected_luminance);
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
        setWebViewSize();

        float expected_luminance = 0.5f;
        // Theme.AppCompat.Light has a darker background in api 33 than previous.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            expected_luminance = 0.4f;
        }
        // Loading about:blank which doesn't support dark style result in a light background.
        getWebViewOnUiThread().loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > expected_luminance);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());

        // Loading a page with dark-theme support should result in a light background (as
        // specified in media-query) in light theme app.
        getWebViewOnUiThread().loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html",
                "base64");
        assertTrue("Bitmap colour should be light",
                ColorUtils.calculateLuminance(getWebPageColor()) > expected_luminance);
        assertFalse("WebView should always prefers light color scheme in light theme app",
                prefersDarkTheme());
    }
}
