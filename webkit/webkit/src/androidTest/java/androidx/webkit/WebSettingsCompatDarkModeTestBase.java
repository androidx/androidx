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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for dark mode related test.
 */
public class WebSettingsCompatDarkModeTestBase<T extends Activity> {
    public final String mDarkThemeSupport = Base64.encodeToString((
            "<html>"
                    + "  <head>"
                    + "    <meta name=\"color-scheme\" content=\"light dark\">"
                    + "    <style>"
                    + "      @media (prefers-color-scheme: dark) {"
                    + "      body {background-color: rgba(0, 255, 0, 1); }"
                    + "    </style>"
                    + "  </head>"
                    + "  <body>"
                    + "  </body>"
                    + "</html>"
    ).getBytes(), Base64.NO_PADDING);

    private WebViewOnUiThread mWebViewOnUiThread;

    // LayoutParams are null until WebView has a parent Activity.
    // Test require LayoutParams to define width and height of WebView to capture its bitmap
    // representation.
    @SuppressWarnings("deprecation")
    @Rule
    public final TargetSdkActivityTestRule<T> mActivityRule;

    public WebSettingsCompatDarkModeTestBase(Class<T> activityClass, int targetSdk) {
        mActivityRule = new TargetSdkActivityTestRule<T>(activityClass,
                targetSdk);
    }

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread(
                ((WebViewTestActivity) mActivityRule.getActivity()).getWebView());
        mWebViewOnUiThread.getSettings().setJavaScriptEnabled(true);
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @NonNull
    public WebViewOnUiThread getWebViewOnUiThread() {
        return mWebViewOnUiThread;
    }

    @NonNull
    public WebSettings getSettingsOnUiThread() {
        return getWebViewOnUiThread().getSettings();
    }

    public void setWebViewSize(final int width, final int height) {
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
    public int getWebPageColor() {
        Map<Integer, Integer> histogram =
                getBitmapHistogram(mWebViewOnUiThread.captureBitmap(), 0, 0, 64, 64);
        Map.Entry<Integer, Integer> maxEntry = null;
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }
        assertNotNull("There must be at least one color on the screen", maxEntry);
        assertTrue(
                "The majority color should be at least 90% of the pixels",
                1.0 * maxEntry.getValue() / (64 * 64) > 0.9);
        return maxEntry.getKey();
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

    private int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height) {
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, x, y, width, height);
        return pixels;
    }

    public boolean prefersDarkTheme() {
        final String colorSchemeSelector =
                "window.matchMedia('(prefers-color-scheme: dark)').matches";
        String result = mWebViewOnUiThread.evaluateJavascriptSync(colorSchemeSelector);

        return "true".equals(result);
    }

    /**
     * Returns a matcher to check if a color int is mostly green.
     */
    public static Matcher<Integer> isGreen() {
        return new TypeSafeMatcher<Integer>() {
            private int mPageColor;

            @Override
            public boolean matchesSafely(Integer pageColor) {
                mPageColor = pageColor;
                return Color.green(pageColor) > 200
                        && Color.red(pageColor) < 90
                        && Color.blue(pageColor) < 90;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("expected color to be green but was "
                        + toHex(mPageColor) + " (in ARGB format)");
            }
        };
    }

    private static String toHex(int i) {
        long l = Integer.toUnsignedLong(i);
        return "0x" + Long.toString(l, 16);
    }
}
