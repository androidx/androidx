/*
 * Copyright 2023 The Android Open Source Project
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
import static org.junit.Assert.assertNotNull;

import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WebSettingsCompatUserAgentMetadataTest {
    private static final String[] USER_AGENT_CLIENT_HINTS = {"sec-ch-ua", "sec-ch-ua-arch",
            "sec-ch-ua-platform", "sec-ch-ua-model", "sec-ch-ua-mobile", "sec-ch-ua-full-version",
            "sec-ch-ua-platform-version", "sec-ch-ua-bitness", "sec-ch-ua-full-version-list",
            "sec-ch-ua-wow64", "sec-ch-ua-form-factors"};

    private static final String FIRST_URL = "/first.html";
    private static final String SECOND_URL = "/second.html";

    private static final String FIRST_RAW_HTML =
            "<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "  <body>\n"
                    + "    <iframe src=\"" + SECOND_URL + "\">\n"
                    + "    </iframe>\n"
                    + "  </body>\n"
                    + "</html>\n";

    private static final String SECOND_RAW_HTML =
            "<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "  <body>\n"
                    + "  </body>\n"
                    + "</html>\n";

    private WebViewOnUiThread mWebViewOnUiThread;
    private TestHttpsWebViewClient mTestHttpsWebViewClient;

    @Before
    public void setUp() throws Exception {
        mWebViewOnUiThread = new WebViewOnUiThread();
        mTestHttpsWebViewClient = new TestHttpsWebViewClient(mWebViewOnUiThread);
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
        CookieManager.getInstance().removeAllCookies(null);
    }

    /**
     * A WebViewClient to intercept the request to mock HTTPS response.
     */
    private static final class TestHttpsWebViewClient extends
            WebViewOnUiThread.WaitForLoadedClient {
        private final List<WebResourceRequest> mInterceptedRequests = new ArrayList<>();

        TestHttpsWebViewClient(WebViewOnUiThread webViewOnUiThread) throws Exception {
            super(webViewOnUiThread);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            // Only return content for FIRST_URL and SECOND_URL, deny all other requests.
            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", "text/html");
            responseHeaders.put("Accept-Ch", String.join(",", USER_AGENT_CLIENT_HINTS));

            if (request.getUrl().toString().endsWith(FIRST_URL)) {
                mInterceptedRequests.add(request);
                return new WebResourceResponse("text/html", "utf-8",
                        200, "OK", responseHeaders,
                        new ByteArrayInputStream(
                                FIRST_RAW_HTML.getBytes(StandardCharsets.UTF_8)));
            } else if (request.getUrl().toString().endsWith(SECOND_URL)) {
                mInterceptedRequests.add(request);
                return new WebResourceResponse("text/html", "utf-8",
                        200, "OK", responseHeaders,
                        new ByteArrayInputStream(
                                SECOND_RAW_HTML.getBytes(StandardCharsets.UTF_8)));
            }
            return new WebResourceResponse("text/html", "UTF-8", null);
        }

        public List<WebResourceRequest> getInterceptedRequests() {
            return mInterceptedRequests;
        }
    }

    @Test
    public void testSetUserAgentMetadataDefault() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        UserAgentMetadata defaultSetting = WebSettingsCompat.getUserAgentMetadata(
                settings);
        // Check brand version list.
        List<String> brands = new ArrayList<>();
        Assert.assertNotNull(defaultSetting.getBrandVersionList());
        for (UserAgentMetadata.BrandVersion bv : defaultSetting.getBrandVersionList()) {
            brands.add(bv.getBrand());
        }
        Assert.assertTrue("The default brand should contains Android WebView.",
                brands.contains("Android WebView"));
        // Check platform, bitness and wow64.
        assertEquals("The default platform is Android.", "Android",
                defaultSetting.getPlatform());
        assertEquals("The default bitness is 0.", UserAgentMetadata.BITNESS_DEFAULT,
                defaultSetting.getBitness());
        assertFalse("The default wow64 is false.", defaultSetting.isWow64());
    }

    @Test
    public void testSetUserAgentMetadataExplicitlyDefault() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                .setBrandVersionList(new ArrayList<>())
                .setArchitecture(null)
                .setFullVersion(null)
                .setPlatform(null)
                .setPlatformVersion(null)
                .setModel(null).build();
        WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);

        UserAgentMetadata userAgentMetadata = WebSettingsCompat.getUserAgentMetadata(settings);
        // Check brand version list.
        List<String> brands = new ArrayList<>();
        Assert.assertNotNull(userAgentMetadata.getBrandVersionList());
        for (UserAgentMetadata.BrandVersion bv : userAgentMetadata.getBrandVersionList()) {
            brands.add(bv.getBrand());
        }
        Assert.assertTrue("The default brand should contains Android WebView.",
                brands.contains("Android WebView"));
        assertEquals("The default platform is Android.", "Android",
                userAgentMetadata.getPlatform());
        assertNotNull(userAgentMetadata.getArchitecture());
        assertNotNull(userAgentMetadata.getFullVersion());
        assertNotNull(userAgentMetadata.getPlatform());
        assertNotNull(userAgentMetadata.getPlatformVersion());
        assertNotNull(userAgentMetadata.getModel());
    }

    @Test
    public void testSetUserAgentMetadataDefaultHttpHeader() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        mWebViewOnUiThread.setWebViewClient(mTestHttpsWebViewClient);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);

        // As WebViewOnUiThread clear cache doesn't work well, using different origin to avoid
        // client hints cache impacts other tests.
        String baseUrl = "https://example1.com";
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(baseUrl + FIRST_URL);
        List<WebResourceRequest> requests = mTestHttpsWebViewClient.getInterceptedRequests();
        Assert.assertEquals(2, requests.size());

        // Make sure the first request has low-entropy client hints.
        WebResourceRequest recordedRequest = requests.get(0);
        Assert.assertEquals(baseUrl + FIRST_URL, recordedRequest.getUrl().toString());
        Map<String, String> requestHeaders = recordedRequest.getRequestHeaders();
        Assert.assertTrue(
                requestHeaders.get("sec-ch-ua").contains("Android WebView"));
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-mobile").isEmpty());
        Assert.assertEquals("\"Android\"",
                requestHeaders.get("sec-ch-ua-platform"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-platform-version"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-arch"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-full-version-list"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-model"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-wow64"));

        // Verify all user-agent client hints on the second request.
        recordedRequest = requests.get(1);
        Assert.assertEquals(baseUrl + SECOND_URL, recordedRequest.getUrl().toString());
        requestHeaders = recordedRequest.getRequestHeaders();

        Assert.assertTrue(
                requestHeaders.get("sec-ch-ua").contains("Android WebView"));
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-mobile").isEmpty());
        Assert.assertEquals("\"Android\"",
                requestHeaders.get("sec-ch-ua-platform"));
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-platform-version").isEmpty());
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-arch").isEmpty());
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-full-version-list").isEmpty());
        Assert.assertEquals("\"\"", requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertFalse(requestHeaders.get("sec-ch-ua-model").isEmpty());
        Assert.assertEquals("?0", requestHeaders.get("sec-ch-ua-wow64"));
    }

    @Test
    public void testSetUserAgentMetadataFullOverrides() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        // Overrides user-agent metadata.
        UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion.Builder()
                .setBrand("myBrand").setMajorVersion("1").setFullVersion("1.1.1.1").build();
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(brandVersion))
                .setFullVersion("1.1.1.1")
                .setPlatform("myPlatform").setPlatformVersion("2.2.2.2").setArchitecture("myArch")
                .setMobile(true).setModel("myModel").setBitness(32)
                .setFormFactors(Collections.singletonList(UserAgentMetadata.FORM_FACTOR_XR))
                .build();

        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);
        assertEquals(
                "After override set the user-agent metadata, it should be returned",
                overrideSetting, WebSettingsCompat.getUserAgentMetadata(
                        settings));
    }

    @Test
    public void testSetUserAgentMetadataFullOverridesHttpHeader() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        mWebViewOnUiThread.setWebViewClient(mTestHttpsWebViewClient);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);

        // Overrides user-agent metadata.
        UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion.Builder()
                .setBrand("myBrand").setMajorVersion("1").setFullVersion("1.1.1.1").build();
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(brandVersion))
                .setFullVersion("1.1.1.1").setPlatform("myPlatform")
                .setPlatformVersion("2.2.2.2").setArchitecture("myArch")
                .setMobile(true).setModel("myModel").setBitness(32)
                .setWow64(false).build();
        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);

        // As WebViewOnUiThread clear cache doesn't work well, using different origin to avoid
        // client hints cache impacts other tests.
        String baseUrl = "https://example2.com";
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(baseUrl + FIRST_URL);
        List<WebResourceRequest> requests = mTestHttpsWebViewClient.getInterceptedRequests();
        Assert.assertEquals(2, requests.size());

        // Make sure the first request has low-entropy client hints.
        WebResourceRequest recordedRequest = requests.get(0);
        Assert.assertEquals(baseUrl + FIRST_URL, recordedRequest.getUrl().toString());
        Map<String, String> requestHeaders = recordedRequest.getRequestHeaders();
        Assert.assertEquals("\"myBrand\";v=\"1\"",
                requestHeaders.get("sec-ch-ua"));
        Assert.assertEquals("?1", requestHeaders.get("sec-ch-ua-mobile"));
        Assert.assertEquals("\"myPlatform\"",
                requestHeaders.get("sec-ch-ua-platform"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-platform-version"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-arch"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-full-version-list"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-model"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-wow64"));

        // Verify all user-agent client hints on the second request.
        recordedRequest = requests.get(1);
        Assert.assertEquals(baseUrl + SECOND_URL, recordedRequest.getUrl().toString());
        requestHeaders = recordedRequest.getRequestHeaders();

        Assert.assertEquals("\"myBrand\";v=\"1\"",
                requestHeaders.get("sec-ch-ua"));
        Assert.assertEquals("?1", requestHeaders.get("sec-ch-ua-mobile"));
        Assert.assertEquals("\"myPlatform\"",
                requestHeaders.get("sec-ch-ua-platform"));
        Assert.assertEquals("\"2.2.2.2\"",
                requestHeaders.get("sec-ch-ua-platform-version"));
        Assert.assertEquals("\"myArch\"",
                requestHeaders.get("sec-ch-ua-arch"));
        Assert.assertEquals("\"myBrand\";v=\"1.1.1.1\"",
                requestHeaders.get("sec-ch-ua-full-version-list"));
        Assert.assertEquals("\"32\"",
                requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertEquals("\"myModel\"",
                requestHeaders.get("sec-ch-ua-model"));
        Assert.assertEquals("?0", requestHeaders.get("sec-ch-ua-wow64"));
    }

    @Test
    public void testSetUserAgentMetadataPartialOverride() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        // Overrides without setting user-agent metadata platform and bitness.
        UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion.Builder()
                .setBrand("myBrand").setMajorVersion("1").setFullVersion("1.1.1.1").build();
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(brandVersion))
                .setFullVersion("1.1.1.1")
                .setPlatformVersion("2.2.2.2").setArchitecture("myArch").setMobile(true)
                .setModel("myModel").setWow64(false).build();

        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);
        UserAgentMetadata actualSetting = WebSettingsCompat.getUserAgentMetadata(
                settings);
        assertEquals("Platform should reset to system default if no overrides.",
                "Android", actualSetting.getPlatform());
        assertEquals("Bitness should reset to system default if no overrides.",
                UserAgentMetadata.BITNESS_DEFAULT, actualSetting.getBitness());
    }

    @Test
    public void testSetUserAgentMetadataPartialOverrideHttpHeader() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        mWebViewOnUiThread.setWebViewClient(mTestHttpsWebViewClient);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);

        // Overrides without setting user-agent metadata platform and bitness.
        UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion.Builder()
                .setBrand("myBrand").setMajorVersion("1").setFullVersion("1.1.1.1").build();
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setBrandVersionList(Collections.singletonList(brandVersion))
                .setFullVersion("1.1.1.1").setPlatformVersion("2.2.2.2")
                .setArchitecture("myArch").setMobile(true).setModel("myModel").setWow64(false)
                .build();
        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);

        // As WebViewOnUiThread clear cache doesn't work well, using different origin to avoid
        // client hints cache impacts other tests.
        String baseUrl = "https://example3.com";
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(baseUrl + FIRST_URL);
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(FIRST_URL);
        List<WebResourceRequest> requests = mTestHttpsWebViewClient.getInterceptedRequests();
        Assert.assertEquals(2, requests.size());

        // Make sure the first request has low-entropy client hints.
        WebResourceRequest recordedRequest = requests.get(0);
        Assert.assertEquals(baseUrl + FIRST_URL, recordedRequest.getUrl().toString());
        Map<String, String> requestHeaders = recordedRequest.getRequestHeaders();
        Assert.assertEquals("\"myBrand\";v=\"1\"",
                requestHeaders.get("sec-ch-ua"));
        Assert.assertEquals("?1", requestHeaders.get("sec-ch-ua-mobile"));
        Assert.assertEquals("\"Android\"",
                requestHeaders.get("sec-ch-ua-platform"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-platform-version"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-arch"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-full-version-list"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-model"));
        Assert.assertNull(requestHeaders.get("sec-ch-ua-wow64"));

        // Verify all user-agent client hints on the second request.
        recordedRequest = requests.get(1);
        Assert.assertEquals(baseUrl + SECOND_URL, recordedRequest.getUrl().toString());
        requestHeaders = recordedRequest.getRequestHeaders();

        Assert.assertEquals("\"myBrand\";v=\"1\"",
                requestHeaders.get("sec-ch-ua"));
        Assert.assertEquals("?1", requestHeaders.get("sec-ch-ua-mobile"));
        Assert.assertNotNull(requestHeaders.get("sec-ch-ua-platform-version"));
        Assert.assertNotNull(requestHeaders.get("sec-ch-ua-arch"));
        Assert.assertNotNull(requestHeaders.get("sec-ch-ua-full-version-list"));
        // The default bitness on HTTP header is empty string instead of 0.
        Assert.assertEquals("\"\"",
                requestHeaders.get("sec-ch-ua-bitness"));
        Assert.assertNotNull(requestHeaders.get("sec-ch-ua-model"));
        Assert.assertNotNull(requestHeaders.get("sec-ch-ua-wow64"));
    }

    @Test
    public void testSetUserAgentMetadataBlankBrandVersion() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion
                    .Builder().setBrand("myBrand").build();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setBrandVersionList(Collections.singletonList(brandVersion)).build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Brand name, major version and full version should not "
                    + "be null or blank.", e.getMessage());
        }

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion
                    .Builder().setBrand("").build();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setBrandVersionList(Collections.singletonList(brandVersion)).build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Brand should not be blank.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataBlankFullVersion() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setFullVersion("  ").build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Full version should not be blank.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataBlankPlatform() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA);

        try {
            WebSettings settings = mWebViewOnUiThread.getSettings();
            UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                    .setPlatform("  ").build();
            WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
            Assert.fail("Should have thrown exception.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Platform should not be blank.", e.getMessage());
        }
    }

    @Test
    public void testSetUserAgentMetadataEmptyFormFactors() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS);

        WebSettings settings = mWebViewOnUiThread.getSettings();
        UserAgentMetadata uaMetadata = new UserAgentMetadata.Builder()
                .setFormFactors(new ArrayList<>()).build();
        WebSettingsCompat.setUserAgentMetadata(settings, uaMetadata);
        UserAgentMetadata result = WebSettingsCompat.getUserAgentMetadata(settings);
        // Check form factors.
        List<String> formFactors = result.getFormFactors();
        assertEquals(1, formFactors.size());
        assertTrue("Default form factor should be Desktop or Mobile, actual: " + formFactors.get(0),
                formFactors.get(0).equals(UserAgentMetadata.FORM_FACTOR_DESKTOP)
                        || formFactors.get(0).equals(UserAgentMetadata.FORM_FACTOR_MOBILE));
    }

    @Test
    public void testSetUserAgentMetadataInvalidFormFactors() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> new UserAgentMetadata.Builder()
                .setFormFactors(Collections.singletonList("InvalidFormFactor")));

    }

    @Test
    public void testSetUserAgentMetadataFormFactorsHttpHeader() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS);

        mWebViewOnUiThread.setWebViewClient(mTestHttpsWebViewClient);
        WebSettings settings = mWebViewOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);

        // Overrides user-agent form factors.
        List<String> formFactors = new ArrayList<>();
        formFactors.add(UserAgentMetadata.FORM_FACTOR_XR);
        formFactors.add(UserAgentMetadata.FORM_FACTOR_DESKTOP);
        UserAgentMetadata overrideSetting = new UserAgentMetadata.Builder()
                .setFormFactors(formFactors).build();
        WebSettingsCompat.setUserAgentMetadata(settings, overrideSetting);

        // As WebViewOnUiThread clear cache doesn't work well, using different origin to avoid
        // client hints cache impacts other tests.
        String baseUrl = "https://example4.com";
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(baseUrl + FIRST_URL);
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(FIRST_URL);
        List<WebResourceRequest> requests = mTestHttpsWebViewClient.getInterceptedRequests();
        Assert.assertEquals(2, requests.size());

        // Make sure the first request is empty.
        WebResourceRequest recordedRequest = requests.get(0);
        Assert.assertEquals(baseUrl + FIRST_URL, recordedRequest.getUrl().toString());
        Map<String, String> requestHeaders = recordedRequest.getRequestHeaders();
        Assert.assertNull(requestHeaders.get("sec-ch-ua-form-factors"));

        // Verify form factors on the second request.
        recordedRequest = requests.get(1);
        Assert.assertEquals(baseUrl + SECOND_URL, recordedRequest.getUrl().toString());
        requestHeaders = recordedRequest.getRequestHeaders();
        Assert.assertEquals("\"" + String.join("\", \"", formFactors) + "\"",
                            requestHeaders.get("sec-ch-ua-form-factors"));
    }
}
