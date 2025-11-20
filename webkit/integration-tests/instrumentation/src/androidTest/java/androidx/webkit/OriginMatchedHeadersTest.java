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


import android.os.CancellationSignal;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
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
 * Test for {@link Profile#setOriginMatchedHeader(String, String, Set)} and associated methods.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class OriginMatchedHeadersTest {

    private static final String HEADER_NAME = "X-ExtraHeader";
    private static final String SERVER_PATH = "/index.html";
    private static final String EXPECTED_HEADER_VALUE = "active";
    private static final int TIMEOUT_SECONDS = 5;
    private static final String OTHER_PROFILE_NAME = "OriginMatchedHeaderTestProfile";

    private WebViewOnUiThread mWebViewOnUiThread;
    private Profile mDefaultProfile;

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.ORIGIN_MATCHED_HEADERS);
        mWebViewOnUiThread = new WebViewOnUiThread();
        mDefaultProfile = WebkitUtils.onMainThreadSync(
                () -> ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME));
    }

    @After
    public void tearDown() throws Exception {
        if (mDefaultProfile != null) {
            clearContentFilterHeaderForOriginsOnMainThread();
        }
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @Test
    public void canSetAndClearHeader() {
        Set<String> originRules = Set.of("http://example.com");
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile.setOriginMatchedHeader(HEADER_NAME, EXPECTED_HEADER_VALUE, originRules);
            Assert.assertTrue(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
            mDefaultProfile.clearOriginMatchedHeader(HEADER_NAME);
            Assert.assertFalse(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
        });
    }

    @Test
    public void canSetAndClearAllHeaders() {
        Set<String> originRules = Set.of("http://example.com");
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile.setOriginMatchedHeader(HEADER_NAME, EXPECTED_HEADER_VALUE, originRules);
            Assert.assertTrue(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
            mDefaultProfile.clearAllOriginMatchedHeaders();
            Assert.assertFalse(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
        });
    }

    @Test
    public void canOverwriteHeader() {
        Set<String> originRules = Set.of("http://example.com");
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile.setOriginMatchedHeader(HEADER_NAME, EXPECTED_HEADER_VALUE, originRules);
            mDefaultProfile.setOriginMatchedHeader(HEADER_NAME, "NewValue", originRules);
            Assert.assertTrue(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
        });
    }

    @Test
    public void settingHeaderOnlyAppliesToProvidedProfile() {
        Set<String> originRules = Set.of("http://example.com");
        WebkitUtils.onMainThreadSync(() -> {
            Profile otherProfile = ProfileStore.getInstance().getOrCreateProfile(
                    OTHER_PROFILE_NAME);
            Assert.assertNotNull(otherProfile);

            mDefaultProfile.setOriginMatchedHeader(HEADER_NAME, EXPECTED_HEADER_VALUE, originRules);
            Assert.assertTrue(mDefaultProfile.hasOriginMatchedHeader(HEADER_NAME));
            Assert.assertFalse(otherProfile.hasOriginMatchedHeader(HEADER_NAME));

            String otherHeaderName = "OtherHeaderName";
            otherProfile.setOriginMatchedHeader(otherHeaderName, "Value", originRules);
            Assert.assertTrue(otherProfile.hasOriginMatchedHeader(otherHeaderName));
            Assert.assertFalse(mDefaultProfile.hasOriginMatchedHeader(otherHeaderName));
            otherProfile.clearOriginMatchedHeader(otherHeaderName);
            Assert.assertFalse(otherProfile.hasOriginMatchedHeader(otherHeaderName));
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
            Assert.assertNull(request.getHeader(HEADER_NAME));
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
            setContentFilterHeaderForOriginsOnMainThread(HEADER_NAME, EXPECTED_HEADER_VALUE,
                    originRules);

            mWebViewOnUiThread.loadUrl(url.toString());
            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertEquals(EXPECTED_HEADER_VALUE, request.getHeader(HEADER_NAME));
        }
    }

    @Test
    public void headerNameValueIsNotHardCoded() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(getResponse());
            HttpUrl url = server.url(SERVER_PATH);
            Set<String> originRules = getOriginRules(url);
            setContentFilterHeaderForOriginsOnMainThread("X-DifferentHeader", "DifferentValue",
                    originRules);

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

            setContentFilterHeaderForOriginsOnMainThread("X-ExtraHeader", "active",
                    getOriginRules(url1));
            setContentFilterHeaderForOriginsOnMainThread("X-OtherHeader", "OtherValue",
                    getOriginRules(url2));

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
        setContentFilterHeaderForOriginsOnMainThread(HEADER_NAME, EXPECTED_HEADER_VALUE,
                Set.of("https://example.com"));
        mWebViewOnUiThread.loadUrl(url);
        Map<String, String> interceptedHeaders = resultQueue.take();
        Assert.assertEquals(EXPECTED_HEADER_VALUE, interceptedHeaders.get(HEADER_NAME));
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
            setContentFilterHeaderForOriginsOnMainThread(HEADER_NAME, EXPECTED_HEADER_VALUE,
                    originRules);

            WebkitUtils.onMainThread(
                    () -> mDefaultProfile.prefetchUrlAsync(url.toString(), new CancellationSignal(),
                            Runnable::run, ignored -> {
                            }));

            RecordedRequest request = server.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.assertNotNull(request);
            Assert.assertEquals(SERVER_PATH, request.getPath());
            Assert.assertEquals(EXPECTED_HEADER_VALUE, request.getHeader(HEADER_NAME));
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

    private void clearContentFilterHeaderForOriginsOnMainThread() {
        WebkitUtils.onMainThreadSync(mDefaultProfile::clearAllOriginMatchedHeaders);
    }

    private void setContentFilterHeaderForOriginsOnMainThread(String headerName, String headerValue,
            Set<String> originRules) {
        WebkitUtils.onMainThreadSync(
                () -> mDefaultProfile.setOriginMatchedHeader(headerName, headerValue, originRules));
    }
}
