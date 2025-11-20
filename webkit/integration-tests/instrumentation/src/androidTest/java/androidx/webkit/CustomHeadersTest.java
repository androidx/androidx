/*
 * Copyright 2025 The Android Open Source Project
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


import android.os.Build;
import android.os.CancellationSignal;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Test for {@link Profile#addCustomHeader(CustomHeader)} and associated methods.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class CustomHeadersTest {

    private static final String SERVER_PATH = "/index.html";
    private static final int TIMEOUT_SECONDS = 5;
    private static final String OTHER_PROFILE_NAME = "CustomHeaderTestProfile";

    private WebViewOnUiThread mWebViewOnUiThread;
    private Profile mDefaultProfile;

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.CUSTOM_REQUEST_HEADERS);
        mWebViewOnUiThread = new WebViewOnUiThread();
        mDefaultProfile = WebkitUtils.onMainThreadSync(
                () -> ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME));
    }

    @After
    public void tearDown() throws Exception {
        if (mDefaultProfile != null) {
            clearAllCustomHeadersOnUiThread();
        }
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @Test
    public void canAddAndClearHeader() {
        CustomHeader header = new CustomHeader("X-ExtraHeader", "Value",
                Set.of("http://example.com"));
        addCustomHeaderOnUiThread(header);
        Assert.assertTrue(hasCustomHeaderOnUiThread("X-ExtraHeader"));
        Set<CustomHeader> customHeaders = getCustomHeadersOnUiThread();
        Assert.assertEquals(Set.of(header), customHeaders);

        clearCustomHeaderOnUiThread("X-ExtraHeader");
        Assert.assertFalse(hasCustomHeaderOnUiThread("X-ExtraHeader"));
        Assert.assertTrue(getCustomHeadersOnUiThread().isEmpty());
    }


    @Test
    public void canClearAllHeaders() {
        CustomHeader header = new CustomHeader("X-ExtraHeader", "Value",
                Set.of("http://example.com"));
        addCustomHeaderOnUiThread(header);
        Assert.assertTrue(hasCustomHeaderOnUiThread("X-ExtraHeader"));
        Set<CustomHeader> customHeaders = getCustomHeadersOnUiThread();
        Assert.assertEquals(Set.of(header), customHeaders);

        clearAllCustomHeadersOnUiThread();
        Assert.assertFalse(hasCustomHeaderOnUiThread("X-ExtraHeader"));
        Assert.assertTrue(getCustomHeadersOnUiThread().isEmpty());
    }

    @Test
    public void canAddMultipleValues() {
        Set<String> originRules = Set.of("http://example.com");
        CustomHeader header = new CustomHeader("X-ExtraHeader", "Value", originRules);
        CustomHeader otherHeader = new CustomHeader("X-ExtraHeader", "NewValue", originRules);
        addCustomHeaderOnUiThread(header);
        addCustomHeaderOnUiThread(otherHeader);
        Assert.assertTrue(hasCustomHeaderOnUiThread("X-ExtraHeader"));
        Assert.assertEquals(Set.of(header, otherHeader), getCustomHeadersOnUiThread());
    }

    @Test
    public void canGetHeadersFiltered() {
        Set<String> originRules = Set.of("http://example.com");

        final CustomHeader header1 = new CustomHeader("X-ExtraHeader", "Value", originRules);
        final CustomHeader header2 = new CustomHeader("X-ExtraHeader", "NewValue", originRules);
        final CustomHeader header3 = new CustomHeader("X-OtherHeader", "NewValue", originRules);

        addCustomHeaderOnUiThread(header1);
        addCustomHeaderOnUiThread(header2);
        addCustomHeaderOnUiThread(header3);

        Assert.assertEquals(Set.of(header1, header2, header3),
                getCustomHeadersOnUiThread());
        Assert.assertEquals(Set.of(header1, header2),
                getCustomHeadersOnUiThread("X-ExtraHeader"));
        Assert.assertEquals(Set.of(header2),
                getCustomHeadersOnUiThread("X-ExtraHeader", "NewValue"));
    }

    @Test
    public void canRemoveHeadersByName() {
        Set<String> originRules = Set.of("http://example.com");

        final CustomHeader header1 = new CustomHeader("X-ExtraHeader", "Value", originRules);
        final CustomHeader header2 = new CustomHeader("X-ExtraHeader", "NewValue", originRules);
        final CustomHeader header3 = new CustomHeader("X-OtherHeader", "NewValue", originRules);

        addCustomHeaderOnUiThread(header1);
        addCustomHeaderOnUiThread(header2);
        addCustomHeaderOnUiThread(header3);

        clearCustomHeaderOnUiThread("X-ExtraHeader");
        Assert.assertEquals(Set.of(header3), getCustomHeadersOnUiThread());
    }

    @Test
    public void canRemoveHeadersByNameAndValue() {
        Set<String> originRules = Set.of("http://example.com");

        final CustomHeader header1 = new CustomHeader("X-ExtraHeader", "Value", originRules);
        final CustomHeader header2 = new CustomHeader("X-ExtraHeader", "NewValue", originRules);
        final CustomHeader header3 = new CustomHeader("X-OtherHeader", "NewValue", originRules);

        addCustomHeaderOnUiThread(header1);
        addCustomHeaderOnUiThread(header2);
        addCustomHeaderOnUiThread(header3);

        clearCustomHeaderOnUiThread("X-ExtraHeader", "NewValue");

        Assert.assertEquals(Set.of(header1, header3), getCustomHeadersOnUiThread());
    }

    @Test
    public void canMergeOriginRules() {
        CustomHeader header = new CustomHeader("X-ExtraHeader", "Value",
                Set.of("http://example.com"));
        CustomHeader otherHeader = new CustomHeader("X-ExtraHeader", "Value",
                Set.of("http://example.test", "http://example.com"));

        addCustomHeaderOnUiThread(header);
        addCustomHeaderOnUiThread(otherHeader);

        Assert.assertEquals(Set.of(new CustomHeader("X-ExtraHeader", "Value",
                        Set.of("http://example.test", "http://example.com"))),
                getCustomHeadersOnUiThread());
    }

    @Test
    public void settingHeaderOnlyAppliesToProvidedProfile() {
        Profile otherProfile = WebkitUtils.onMainThreadSync(
                () -> ProfileStore.getInstance().getOrCreateProfile(OTHER_PROFILE_NAME));

        CustomHeader header = new CustomHeader("X-ExtraHeader", "Value",
                Set.of("http://example.com"));
        CustomHeader otherHeader = new CustomHeader("OtherHeader", "Value",
                Set.of("http://example.com"));

        addCustomHeaderOnUiThread(header);
        Assert.assertTrue(hasCustomHeaderOnUiThread("X-ExtraHeader"));

        WebkitUtils.onMainThreadSync(() -> {
            Assert.assertFalse(otherProfile.hasCustomHeader("X-ExtraHeader"));
            otherProfile.addCustomHeader(otherHeader);
            Assert.assertTrue(otherProfile.hasCustomHeader("OtherHeader"));
            Assert.assertFalse(
                    mDefaultProfile.hasCustomHeader("OtherHeader"));
            otherProfile.clearCustomHeader("OtherHeader");
            Assert.assertFalse(otherProfile.hasCustomHeader("OtherHeader"));
        });
    }


    @Test
    public void headerNotAttachedByDefault() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(getResponse());
            HttpUrl url = server.url(SERVER_PATH);
            mWebViewOnUiThread.loadUrl(url.toString());
            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertNull(request.getHeader("X-ExtraHeader"));
        }
    }

    @Test
    public void attachesHeaderToNetworkRequests() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(getResponse());
            HttpUrl url = server.url(SERVER_PATH);
            Assert.assertFalse(url.isHttps());
            Set<String> originRules = getOriginRules(url);
            addCustomHeaderOnUiThread(new CustomHeader("X-ExtraHeader", "Value", originRules));

            mWebViewOnUiThread.loadUrl(url.toString());
            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertEquals("Value", request.getHeader("X-ExtraHeader"));
        }
    }

    @Test
    public void headerNameValueIsNotHardCoded() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(getResponse());
            HttpUrl url = server.url(SERVER_PATH);
            Set<String> originRules = getOriginRules(url);
            addCustomHeaderOnUiThread(
                    new CustomHeader("X-DifferentHeader", "DifferentValue", originRules));

            mWebViewOnUiThread.loadUrl(url.toString());
            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertEquals("DifferentValue", request.getHeader("X-DifferentHeader"));
        }
    }

    @Test
    public void onlyAttachesToMappedOrigins() throws Exception {
        try (MockWebServer server1 = new MockWebServer();
                MockWebServer server2 = new MockWebServer()) {
            server1.start();
            server2.start();
            server1.enqueue(getResponse());
            server2.enqueue(getResponse());
            HttpUrl url1 = server1.url(SERVER_PATH);
            HttpUrl url2 = server2.url(SERVER_PATH);

            Set<String> originRules1 = getOriginRules(url1);
            addCustomHeaderOnUiThread(new CustomHeader("X-ExtraHeader", "active", originRules1));
            Set<String> originRules = getOriginRules(url2);
            addCustomHeaderOnUiThread(new CustomHeader("X-OtherHeader", "OtherValue", originRules));

            mWebViewOnUiThread.loadUrl(url1.toString());
            RecordedRequest request1 = server1.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request1);
            Assert.assertEquals("active", request1.getHeader("X-ExtraHeader"));
            Assert.assertNull(request1.getHeader("X-OtherHeader"));

            mWebViewOnUiThread.loadUrl(url2.toString());
            RecordedRequest request2 = server2.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request2);
            Assert.assertNull(request2.getHeader("X-ExtraHeader"));
            Assert.assertEquals("OtherValue", request2.getHeader("X-OtherHeader"));
        }
    }

    @Test
    public void headerVisibleOnShouldInterceptRequest() throws Exception {
        String url = "https://example.com/test.html";
        BlockingQueue<Map<String, String>> resultQueue = new LinkedBlockingQueue<>();

        mWebViewOnUiThread.setWebViewClient(
                new WebViewOnUiThread.WaitForLoadedClient(mWebViewOnUiThread) {
                    @Nullable
                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view,
                            WebResourceRequest request) {
                        if (url.equals(request.getUrl().toString())) {
                            resultQueue.add(request.getRequestHeaders());
                        }
                        return super.shouldInterceptRequest(view, request);
                    }
                });
        addCustomHeaderOnUiThread(
                new CustomHeader("X-ExtraHeader", "Value", Set.of("https://example.com")));
        mWebViewOnUiThread.loadUrl(url);
        Map<String, String> interceptedHeaders = resultQueue.take();
        Assert.assertEquals("Value", interceptedHeaders.get("X-ExtraHeader"));
    }

    @Test
    public void headerPresentOnPrefetchRequests() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_URL_PREFETCH);
        try (MockWebServer server = new MockWebServer()) {
            MockWebServerHttpsUtil.enableHttps(server);
            server.start();

            HttpUrl url = server.url(SERVER_PATH);
            Assert.assertTrue(url.isHttps());
            Set<String> originRules = getOriginRules(url);
            addCustomHeaderOnUiThread(new CustomHeader("X-ExtraHeader", "Value", originRules));

            mDefaultProfile.prefetchUrlAsync(url.toString(), new CancellationSignal(),
                    Runnable::run, ignored -> {
                    });

            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertEquals("Value", request.getHeader("X-ExtraHeader"));
        }
    }

    @NonNull
    private static MockResponse getResponse() {
        MockResponse response = new MockResponse();
        response.setBody("hello, world");
        response.setHeader("Content-Type", "text/text");
        return response;
    }

    @NonNull
    private static Set<String> getOriginRules(HttpUrl url) {
        return Set.of(url.scheme() + "://" + url.host() + ":" + url.port());
    }


    private void addCustomHeaderOnUiThread(CustomHeader header) {
        WebkitUtils.onMainThreadSync(() -> mDefaultProfile.addCustomHeader(header));
    }

    private boolean hasCustomHeaderOnUiThread(String headerName) {
        return WebkitUtils.onMainThreadSync(() -> mDefaultProfile.hasCustomHeader(headerName));
    }

    private void clearAllCustomHeadersOnUiThread() {
        WebkitUtils.onMainThreadSync(mDefaultProfile::clearAllCustomHeaders);
    }

    private void clearCustomHeaderOnUiThread(String headerName) {
        WebkitUtils.onMainThreadSync(() -> mDefaultProfile.clearCustomHeader(headerName));
    }

    private void clearCustomHeaderOnUiThread(String headerName, String headerValue) {
        WebkitUtils.onMainThreadSync(
                () -> mDefaultProfile.clearCustomHeader(headerName, headerValue));
    }

    private Set<CustomHeader> getCustomHeadersOnUiThread() {
        return WebkitUtils.onMainThreadSync(() -> mDefaultProfile.getCustomHeaders());
    }

    private Set<CustomHeader> getCustomHeadersOnUiThread(String name) {
        return WebkitUtils.onMainThreadSync(() -> mDefaultProfile.getCustomHeaders(name));
    }

    private Set<CustomHeader> getCustomHeadersOnUiThread(String name, String value) {
        return WebkitUtils.onMainThreadSync(() -> mDefaultProfile.getCustomHeaders(name, value));
    }
}
