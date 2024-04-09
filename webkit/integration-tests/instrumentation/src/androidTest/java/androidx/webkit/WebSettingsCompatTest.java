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

import static androidx.webkit.WebViewMediaIntegrityApiStatusConfig.WEBVIEW_MEDIA_INTEGRITY_API_DISABLED;
import static androidx.webkit.WebViewMediaIntegrityApiStatusConfig.WEBVIEW_MEDIA_INTEGRITY_API_ENABLED;
import static androidx.webkit.WebViewMediaIntegrityApiStatusConfig.WEBVIEW_MEDIA_INTEGRITY_API_ENABLED_WITHOUT_APP_IDENTITY;

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

    @Test
    public void testAttributionRegistrationBehaviorChange() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.ATTRIBUTION_REGISTRATION_BEHAVIOR);
        WebSettings settings = mWebViewOnUiThread.getSettings();

        Assert.assertEquals("App Source and Web Trigger is the expected default",
                WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_WEB_TRIGGER,
                WebSettingsCompat.getAttributionRegistrationBehavior(settings));

        WebSettingsCompat.setAttributionRegistrationBehavior(settings,
                WebSettingsCompat.ATTRIBUTION_BEHAVIOR_DISABLED);
        Assert.assertEquals(WebSettingsCompat.ATTRIBUTION_BEHAVIOR_DISABLED,
                WebSettingsCompat.getAttributionRegistrationBehavior(settings));

        WebSettingsCompat.setAttributionRegistrationBehavior(settings,
                WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_WEB_TRIGGER);
        Assert.assertEquals(WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_WEB_TRIGGER,
                WebSettingsCompat.getAttributionRegistrationBehavior(settings));

        WebSettingsCompat.setAttributionRegistrationBehavior(settings,
                WebSettingsCompat.ATTRIBUTION_BEHAVIOR_WEB_SOURCE_AND_WEB_TRIGGER);
        Assert.assertEquals(WebSettingsCompat.ATTRIBUTION_BEHAVIOR_WEB_SOURCE_AND_WEB_TRIGGER,
                WebSettingsCompat.getAttributionRegistrationBehavior(settings));

        WebSettingsCompat.setAttributionRegistrationBehavior(settings,
                WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_APP_TRIGGER);
        Assert.assertEquals(WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_APP_TRIGGER,
                WebSettingsCompat.getAttributionRegistrationBehavior(settings));

    }

    @Test
    public void testWebViewMediaIntegrityApiDefaultStatus() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        Assert.assertEquals(WEBVIEW_MEDIA_INTEGRITY_API_ENABLED,
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings).getDefaultStatus());
        Assert.assertTrue(
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getOverrideRules().isEmpty());
    }

    @Test
    public void testSetWebViewMediaIntegrityApiWithNoRules() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);
        WebSettings settings = mWebViewOnUiThread.getSettings();

        WebViewMediaIntegrityApiStatusConfig config =
                new WebViewMediaIntegrityApiStatusConfig.Builder(
                        WEBVIEW_MEDIA_INTEGRITY_API_DISABLED)
                        .build();
        WebSettingsCompat.setWebViewMediaIntegrityApiStatus(settings, config);
        Assert.assertEquals(
                WEBVIEW_MEDIA_INTEGRITY_API_DISABLED,
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getDefaultStatus());
        Assert.assertTrue(
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getOverrideRules().isEmpty());
    }

    @Test
    public void testSetWebViewMediaIntegrityApiWithRules() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);
        WebSettings settings = mWebViewOnUiThread.getSettings();

        WebViewMediaIntegrityApiStatusConfig config =
                new WebViewMediaIntegrityApiStatusConfig.Builder(
                        WEBVIEW_MEDIA_INTEGRITY_API_ENABLED_WITHOUT_APP_IDENTITY)
                        .addOverrideRule("http://*.example.com",
                                WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
                        .build();
        WebSettingsCompat.setWebViewMediaIntegrityApiStatus(settings, config);
        Assert.assertEquals(
                WEBVIEW_MEDIA_INTEGRITY_API_ENABLED_WITHOUT_APP_IDENTITY,
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings).getDefaultStatus());
        Assert.assertEquals(1,
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getOverrideRules().size());
    }

    @Test
    public void testSetWebViewMediaIntegrityApiWithInvalidStatus() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        int invalidStatus = 15;

        WebViewMediaIntegrityApiStatusConfig config =
                new WebViewMediaIntegrityApiStatusConfig.Builder(invalidStatus).build();
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> WebSettingsCompat.setWebViewMediaIntegrityApiStatus(settings, config));
        Assert.assertTrue(
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getOverrideRules().isEmpty());
    }

    @Test
    public void testSetWebViewMediaIntegrityApiWithInvalidRules() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        String validRule = "http://*.example.com";
        String invalidRule1 = "http://xyz.*.com";
        String invalidRule2 = "customscheme://xyz";

        WebViewMediaIntegrityApiStatusConfig config =
                new WebViewMediaIntegrityApiStatusConfig
                        .Builder(WEBVIEW_MEDIA_INTEGRITY_API_DISABLED)
                        .addOverrideRule(validRule, WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
                        .addOverrideRule(invalidRule1, WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
                        .addOverrideRule(invalidRule2, WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
                        .build();
        Exception error = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> WebSettingsCompat.setWebViewMediaIntegrityApiStatus(settings, config));
        Assert.assertTrue(error.getMessage().contains(invalidRule1));
        Assert.assertTrue(error.getMessage().contains(invalidRule2));
        Assert.assertTrue(
                WebSettingsCompat.getWebViewMediaIntegrityApiStatus(settings)
                        .getOverrideRules().isEmpty());
    }

    @Test
    public void testWebauthnSupport() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEB_AUTHENTICATION);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        mWebViewOnUiThread.setCleanupTask(
                () -> WebSettingsCompat.setWebAuthenticationSupport(settings,
                        WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_NONE));

        Assert.assertEquals("NONE is the expected default",
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_NONE,
                WebSettingsCompat.getWebAuthenticationSupport(settings));

        WebSettingsCompat.setWebAuthenticationSupport(settings,
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP);
        Assert.assertEquals(WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP,
                WebSettingsCompat.getWebAuthenticationSupport(settings));

        WebSettingsCompat.setWebAuthenticationSupport(settings,
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER);
        Assert.assertEquals(WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER,
                WebSettingsCompat.getWebAuthenticationSupport(settings));
    }
}
