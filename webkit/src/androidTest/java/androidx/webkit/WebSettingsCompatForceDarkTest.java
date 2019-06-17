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
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class WebSettingsCompatForceDarkTest {
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
    @SmallTest
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
    @SmallTest
    public void testForceDark_rendersDark() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.FORCE_DARK);
        setWebViewSize(64, 64);
        Map<Integer, Integer> histogram;
        Integer[] colourValues;

        // Loading about:blank into a force-dark-on webview should result in a dark background
        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        assertEquals("Force dark should have been set to ON",
                WebSettingsCompat.getForceDark(mWebViewOnUiThread.getSettings()),
                WebSettingsCompat.FORCE_DARK_ON);

        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        histogram = getBitmapHistogram(mWebViewOnUiThread.captureBitmap(), 0, 0, 64, 64);
        assertEquals("Bitmap should have a single colour", histogram.size(), 1);
        colourValues = histogram.keySet().toArray(new Integer[0]);
        assertTrue("Bitmap colour should be dark", Color.luminance(colourValues[0]) < 0.5f);

        // Loading about:blank into a force-dark-off webview should result in a light background
        WebSettingsCompat.setForceDark(
                mWebViewOnUiThread.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
        assertEquals("Force dark should have been set to OFF",
                WebSettingsCompat.getForceDark(mWebViewOnUiThread.getSettings()),
                WebSettingsCompat.FORCE_DARK_OFF);

        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        histogram = getBitmapHistogram(
                mWebViewOnUiThread.captureBitmap(), 0, 0, 64, 64);
        assertEquals("Bitmap should have a single colour", histogram.size(), 1);
        colourValues = histogram.keySet().toArray(new Integer[0]);
        assertTrue("Bitmap colour should be light",
                Color.luminance(colourValues[0]) > 0.5f);
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

    private Map<Integer, Integer> getBitmapHistogram(
            Bitmap bitmap, int x, int y, int width, int height) {
        Map<Integer, Integer> histogram = new HashMap<>();
        for (int pixel : getBitmapPixels(bitmap, x, y, width, height)) {
            histogram.put(pixel, histogram.getOrDefault(pixel, 0) + 1);
        }
        return histogram;
    }

    private  int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height) {
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, x, y, width, height);
        return pixels;
    }
}
