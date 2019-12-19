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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebSettingsCompatForceDarkTest {
    private final String mNoDarkThemeSupport = Base64.encodeToString((
                      "<html>\n"
                    + "  <head>"
                    + "  </head>"
                    + "  <body>"
                    + "  </body>"
                    + "</html>").getBytes(), Base64.NO_PADDING);
    private final String mDarkThemeSupport = Base64.encodeToString((
                      "<html>"
                    + "  <head>"
                    + "    <meta name=\"color-scheme\" content=\"light dark\">"
                    + "    <style>"
                    + "      @media (prefers-color-scheme: dark) {"
                    + "      body {background-color: green; }"
                    + "    </style>"
                    + "  </head>"
                    + "  <body>"
                    + "  </body>"
                    + "</html>"

    ).getBytes(), Base64.NO_PADDING);

    // LayoutParams are null until WebView has a parent Activity.
    // Test testForceDark_rendersDark requires LayoutParams to define
    // width and height of WebView to capture its bitmap representation.
    @Rule
    public final ActivityTestRule<WebViewTestActivity> mActivityRule =
            new ActivityTestRule<>(WebViewTestActivity.class);
    private WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread(mActivityRule.getActivity().getWebView());
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testForceDark_default. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testForceDark_default() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);

        assertEquals("The default force dark state should be AUTO",
                WebSettingsCompat.getForceDark(mWebViewOnUiThread.getSettings()),
                WebSettingsCompat.FORCE_DARK_AUTO);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testForceDark_rendersDark. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testForceDark_rendersDark() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);

        // Loading about:blank into a force-dark-on webview should result in a dark background
        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        assertEquals("Force dark should have been set to ON",
                WebSettingsCompat.getForceDark(mWebViewOnUiThread.getSettings()),
                WebSettingsCompat.FORCE_DARK_ON);

        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be dark", Color.luminance(getWebPageColor()) < 0.5f);

        // Loading about:blank into a force-dark-off webview should result in a light background
        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
        assertEquals("Force dark should have been set to OFF",
                WebSettingsCompat.getForceDark(mWebViewOnUiThread.getSettings()),
                WebSettingsCompat.FORCE_DARK_OFF);

        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        assertTrue("Bitmap colour should be light",
                Color.luminance(getWebPageColor()) > 0.5f);
    }

    /**
     * Test to exercise USER_AGENT_DARKENING_ONLY option,
     * i.e. web contents are always darkened by a user agent.
     */
    @Test
    public void testForceDark_userAgentDarkeningOnly() {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK_STRATEGY);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);

        // Loading empty page with or without dark theme support into a force-dark-on webview with
        // force dark only algorithm should result in a dark background.
        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        WebSettingsCompat.setForceDarkStrategy(mWebViewOnUiThread.getSettings(),
                WebSettingsCompat.USER_AGENT_DARKENING_ONLY);

        mWebViewOnUiThread.loadDataAndWaitForCompletion(mNoDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be dark", Color.luminance(getWebPageColor()) < 0.5f);

        mWebViewOnUiThread.loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be dark", Color.luminance(getWebPageColor()) < 0.5f);
    }

    /**
     * Test to exercise WEB_THEME_DARKENING_ONLY option,
     * i.e. web contents are darkened only by web theme.
     */
    // TODO(amalova): Enable test when meta-tag is supported by WV
    @Test
    @Ignore
    public void testForceDark_webThemeDarkeningOnly() {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK_STRATEGY);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);

        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        WebSettingsCompat.setForceDarkStrategy(mWebViewOnUiThread.getSettings(),
                WebSettingsCompat.WEB_THEME_DARKENING_ONLY);

        // Loading a page without dark-theme support should result in a light background as web
        // page is not darken by a user agent
        mWebViewOnUiThread.loadDataAndWaitForCompletion(mNoDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be light", Color.luminance(getWebPageColor()) > 0.5f);

        // Loading a page with dark-theme support should result in a green background (as
        // specified in media-query)
        mWebViewOnUiThread.loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be green", Color.GREEN  == getWebPageColor());
    }

    /**
     * Test to exercise PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING option,
     * i.e. web contents are darkened by a user agent if there is no dark web theme.
     */
    // TODO(amalova): Enable test when meta-tag is supported by WV
    @Test
    @Ignore
    public void testForceDark_preferWebThemeOverUADarkening() {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK_STRATEGY);
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        setWebViewSize(64, 64);

        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        WebSettingsCompat.setForceDarkStrategy(mWebViewOnUiThread.getSettings(),
                WebSettingsCompat.PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);

        // Loading a page without dark-theme support should result in a dark background as
        // web page is darken by a user agent
        mWebViewOnUiThread.loadDataAndWaitForCompletion(mNoDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be dark", Color.luminance(getWebPageColor()) < 0.5f);

        // Loading a page with dark-theme support should result in a green background (as
        // specified in media-query)
        mWebViewOnUiThread.loadDataAndWaitForCompletion(mDarkThemeSupport, "text/html", "base64");
        assertTrue("Bitmap colour should be green", Color.GREEN == getWebPageColor());
    }

    private void setWebViewSize(final int width, final int height) {
        WebkitUtils.onMainThreadSync(() -> {
            WebView webView = mWebViewOnUiThread.getWebViewOnCurrentThread();
            ViewGroup.LayoutParams params = webView.getLayoutParams();
            params.height = height;
            params.width = width;
            webView.setLayoutParams(params);
        });
    }

    // Requires {@link WebViewFeature.OFF_SCREEN_PRERASTER} for {@link
    // WebViewOnUiThread#captureBitmap}.
    private int getWebPageColor() {
        Map<Integer, Integer> histogram;
        Integer[] colourValues;

        histogram = getBitmapHistogram(mWebViewOnUiThread.captureBitmap(), 0, 0, 64, 64);
        assertEquals("Bitmap should have a single colour", histogram.size(), 1);
        colourValues = histogram.keySet().toArray(new Integer[0]);

        return colourValues[0];
    }

    private Map<Integer, Integer> getBitmapHistogram(
            Bitmap bitmap, int x, int y, int width, int height) {
        Map<Integer, Integer> histogram = new HashMap<>();
        for (int pixel : getBitmapPixels(bitmap, x, y, width, height)) {
            Integer count = histogram.get(pixel);
            histogram.put(pixel, count == null ? 1 : count + 1);
        }
        return histogram;
    }

    private  int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height) {
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, x, y, width, height);
        return pixels;
    }
}
