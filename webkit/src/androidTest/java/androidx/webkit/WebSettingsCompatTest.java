/*
 * Copyright 2018 The Android Open Source Project
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.webkit.WebSettings;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebSettingsCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testOffscreenPreRaster. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOffscreenPreRaster() {
        WebkitUtils.checkFeature(WebViewFeature.OFF_SCREEN_PRERASTER);

        assertFalse(WebSettingsCompat.getOffscreenPreRaster(mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setOffscreenPreRaster(mWebViewOnUiThread.getSettings(), true);
        assertTrue(WebSettingsCompat.getOffscreenPreRaster(mWebViewOnUiThread.getSettings()));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testEnableSafeBrowsing. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testEnableSafeBrowsing() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_ENABLE);

        WebSettingsCompat.setSafeBrowsingEnabled(mWebViewOnUiThread.getSettings(), false);
        assertFalse(WebSettingsCompat.getSafeBrowsingEnabled(mWebViewOnUiThread.getSettings()));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebSettingsTest#testDisabledActionModeMenuItems. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testDisabledActionModeMenuItems() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS);

        assertEquals(WebSettings.MENU_ITEM_NONE,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings(),
                WebSettings.MENU_ITEM_SHARE);
        assertEquals(WebSettings.MENU_ITEM_SHARE,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings(),
                WebSettings.MENU_ITEM_PROCESS_TEXT | WebSettings.MENU_ITEM_WEB_SEARCH);
        assertEquals(WebSettings.MENU_ITEM_PROCESS_TEXT | WebSettings.MENU_ITEM_WEB_SEARCH,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));
    }
}
