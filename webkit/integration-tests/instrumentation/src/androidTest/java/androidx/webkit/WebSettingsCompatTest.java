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

import android.os.Build;
import android.webkit.WebSettings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class WebSettingsCompatTest {
    public static final String TEST_APK_NAME = "androidx.webkit.instrumentation.test";
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

        assertEquals("Disabled action mode items should default to MENU_ITEM_NONE",
                WebSettings.MENU_ITEM_NONE,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings(),
                WebSettings.MENU_ITEM_SHARE);
        assertEquals("Disabled action mode items should have been set to MENU_ITEM_SHARE",
                WebSettings.MENU_ITEM_SHARE,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings(),
                WebSettings.MENU_ITEM_PROCESS_TEXT | WebSettings.MENU_ITEM_WEB_SEARCH);
        assertEquals("Disabled action mode items should have been set to (MENU_ITEM_PROCESS_TEXT "
                        + "| MENU_ITEM_WEB_SEARCH)",
                WebSettings.MENU_ITEM_PROCESS_TEXT | WebSettings.MENU_ITEM_WEB_SEARCH,
                WebSettingsCompat.getDisabledActionModeMenuItems(mWebViewOnUiThread.getSettings()));
    }

    @Test
    public void testEnterpriseAuthenticationAppLinkPolicyEnabled() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY);

        assertTrue(WebSettingsCompat.getEnterpriseAuthenticationAppLinkPolicyEnabled(
                mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(
                mWebViewOnUiThread.getSettings(), false);
        assertFalse(WebSettingsCompat.getEnterpriseAuthenticationAppLinkPolicyEnabled(
                mWebViewOnUiThread.getSettings()));

        WebSettingsCompat.setEnterpriseAuthenticationAppLinkPolicyEnabled(
                mWebViewOnUiThread.getSettings(), true);
        assertTrue(WebSettingsCompat.getEnterpriseAuthenticationAppLinkPolicyEnabled(
                mWebViewOnUiThread.getSettings()));
    }

    @Test
    public void testSetAppPackageNameXRequestedWithHeaderAllowList() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        Assert.assertTrue("The default should be an empty allow-list.",
                WebSettingsCompat.getRequestedWithHeaderOriginAllowList(settings).isEmpty());
        Set<String> allowList = new HashSet<>(
                Arrays.asList("https://*.google.com", "https://*.example"
                        + ".com:8443"));
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, allowList);
        assertEquals(
                "After setting an allow-list, it should be returned",
                allowList, WebSettingsCompat.getRequestedWithHeaderOriginAllowList(settings));

        // Check that the allow-list is respected, and the URL will get the expected header set.
        try (MockWebServer mockWebServer = new MockWebServer()) {
            HttpUrl url = mockWebServer.url("/");
            String requestUrl = url.toString();
            String requestOrigin = url.scheme() + "://" + url.host() + ":" + url.port();
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings,
                    Collections.singleton(requestOrigin));
            mWebViewOnUiThread.loadUrl(requestUrl);
            RecordedRequest recordedRequest = mockWebServer.takeRequest();
            String headerValue = recordedRequest.getHeader("X-Requested-With");
            Assert.assertEquals(TEST_APK_NAME, headerValue);
        }
    }
}
