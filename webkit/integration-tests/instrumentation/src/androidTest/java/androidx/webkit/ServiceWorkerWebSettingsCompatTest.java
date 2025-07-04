/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.webkit.WebViewFeature.isFeatureSupported;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ServiceWorkerWebSettingsCompatTest {

    public static final String TEST_APK_NAME = "androidx.webkit.instrumentation.test";
    private ServiceWorkerWebSettingsCompat mSettings;

    private static final long POLL_TIMEOUT_DURATION_MS = 5000;
    private static final long POLL_INTERVAL_MS = 10;

    private static final String INDEX_HTML_PATH = "/";
    // Website which installs a service worker and sends an empty message to it once it's ready.
    // Once the serviceworker responds to the message, the website then unregisters any installed
    // serviceworkers again to clean up.
    private static final String INDEX_HTML_DOCUMENT =
            "<!DOCTYPE html>\n"
                    + "<link rel=\"icon\" href=\"data:;base64,=\">\n"
                    + "<script>\n"
                    + "window.done=false;\n"
                    + "function swReady(sw) {\n"
                    + "   sw.postMessage({});\n"
                    + "}\n"
                    + "navigator.serviceWorker.register('sw.js')\n"
                    + "    .then(sw_reg => {\n"
                    + "        let sw = sw_reg.installing || sw_reg.waiting || sw_reg.active;\n"
                    + "        if (sw.state == 'activated') {\n"
                    + "            swReady(sw);\n"
                    + "        } else {\n"
                    + "            sw.addEventListener('statechange', e => {\n"
                    + "                if(e.target.state == 'activated') swReady(e.target); \n"
                    + "            });\n"
                    + "        }\n"
                    + "    });\n"
                    + "navigator.serviceWorker.addEventListener('message', _ => {\n"
                    + "    navigator.serviceWorker.getRegistrations()\n"
                    + "        .then(registrations => {\n"
                    + "            registrations.forEach(reg => reg.unregister());\n"
                    + "            window.done=true;\n"
                    + "        }\n"
                    + "    );\n"
                    + "});\n"
                    + "</script>";

    private static final String SERVICE_WORKER_PATH = "/sw.js";
    // ServiceWorker which registers a message event listener that fetches a file and then sends
    // an empty response back to the requester.
    private static final String SERVICE_WORKER_JAVASCRIPT =
            "self.addEventListener('message', async event => {\n"
                    + "    await fetch('content.txt');\n"
                    + "    event.source.postMessage({});\n"
                    + "});\n";

    private static final String TEXT_CONTENT_PATH = "/content.txt";
    private static final String TEXT_CONTENT = "fetch_ok";


    /**
     * Class to hold the default values of the ServiceWorkerWebSettings while we run the test so
     * we can restore them afterwards.
     */
    private static class ServiceWorkerWebSettingsCompatCache {
        private int mCacheMode;
        private boolean mAllowContentAccess;
        private boolean mAllowFileAccess;
        private boolean mBlockNetworkLoads;
        private Set<String> mRequestedHeaderOriginAllowList;
        private boolean mInterceptCookies;

        ServiceWorkerWebSettingsCompatCache(ServiceWorkerWebSettingsCompat settingsCompat) {
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_CACHE_MODE)) {
                mCacheMode = settingsCompat.getCacheMode();
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS)) {
                mAllowContentAccess = settingsCompat.getAllowContentAccess();
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS)) {
                mAllowFileAccess = settingsCompat.getAllowFileAccess();
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS)) {
                mBlockNetworkLoads = settingsCompat.getBlockNetworkLoads();
            }
            if (isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                mRequestedHeaderOriginAllowList =
                        settingsCompat.getRequestedWithHeaderOriginAllowList();
            }
            if (isFeatureSupported(WebViewFeature.COOKIE_INTERCEPT)) {
                mInterceptCookies =
                        settingsCompat.isIncludeCookiesOnShouldInterceptRequestEnabled();
            }
        }

        void restoreSavedValues(ServiceWorkerWebSettingsCompat mSettings) {
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_CACHE_MODE)) {
                mSettings.setCacheMode(mCacheMode);
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS)) {
                mSettings.setAllowContentAccess(mAllowContentAccess);
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS)) {
                mSettings.setAllowFileAccess(mAllowFileAccess);
            }
            if (isFeatureSupported(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS)) {
                mSettings.setBlockNetworkLoads(mBlockNetworkLoads);
            }
            if (isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                mSettings.setRequestedWithHeaderOriginAllowList(mRequestedHeaderOriginAllowList);
            }
            if (isFeatureSupported(WebViewFeature.COOKIE_INTERCEPT)) {
                mSettings.setIncludeCookiesOnShouldInterceptRequestEnabled(mInterceptCookies);
            }
        }
    }

    private ServiceWorkerWebSettingsCompatCache mSavedDefaults;

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BASIC_USAGE);
        mSettings = ServiceWorkerControllerCompat.getInstance().getServiceWorkerWebSettings();
        // Remember to update this constructor when adding new settings to this test case
        mSavedDefaults = new ServiceWorkerWebSettingsCompatCache(mSettings);
    }

    @After
    public void tearDown() {
        // Remember to update the restore method when adding new settings to this test case
        if (mSavedDefaults != null) {
            mSavedDefaults.restoreSavedValues(mSettings);
        }
        WebkitUtils.onMainThreadSync(() -> CookieManager.getInstance().removeAllCookies(value -> {
        }));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testCacheMode. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testCacheMode() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_CACHE_MODE);

        int i = WebSettings.LOAD_DEFAULT;
        Assert.assertEquals(i, mSettings.getCacheMode());
        for (; i <= WebSettings.LOAD_CACHE_ONLY; i++) {
            mSettings.setCacheMode(i);
            Assert.assertEquals(i, mSettings.getCacheMode());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testAllowContentAccess. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testAllowContentAccess() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS);

        Assert.assertTrue(mSettings.getAllowContentAccess());
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setAllowContentAccess(b);
            Assert.assertEquals(b, mSettings.getAllowContentAccess());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testAllowFileAccess. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testAllowFileAccess() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_FILE_ACCESS);

        Assert.assertTrue(mSettings.getAllowFileAccess());
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setAllowFileAccess(b);
            Assert.assertEquals(b, mSettings.getAllowFileAccess());
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.ServiceWorkerWebSettingsTest#testBlockNetworkLoads. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testBlockNetworkLoads() {
        WebkitUtils.checkFeature(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS);

        // Note: we cannot test this setter unless we provide the INTERNET permission, otherwise we
        // get a SecurityException when we pass 'false'.
        final boolean hasInternetPermission = true;

        Assert.assertEquals(mSettings.getBlockNetworkLoads(), !hasInternetPermission);
        for (boolean b : new boolean[]{false, true}) {
            mSettings.setBlockNetworkLoads(b);
            Assert.assertEquals(b, mSettings.getBlockNetworkLoads());
        }
    }


    @Test
    public void testSetAppPackageNameXRequestedWithHeaderAllowList() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST);

        Assert.assertTrue("The allow-list should be empty by default",
                mSettings.getRequestedWithHeaderOriginAllowList().isEmpty());

        try (MockWebServer server = getServiceWorkerMockServer();
             WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {

            webViewOnUiThread.getSettings().setJavaScriptEnabled(true);

            HttpUrl url = server.url(INDEX_HTML_PATH);
            String requestOrigin = url.scheme() + "://" + url.host() + ":" + url.port();
            Set<String> allowList = Collections.singleton(requestOrigin);
            mSettings.setRequestedWithHeaderOriginAllowList(allowList);

            Assert.assertEquals("The allow-list should be returned once set", allowList,
                    mSettings.getRequestedWithHeaderOriginAllowList());

            String requestUrl = url.toString();
            webViewOnUiThread.loadUrl(requestUrl);

            RecordedRequest request;
            do {
                // Wait until we get the request for the text content
                request = server.takeRequest(5, TimeUnit.SECONDS);
            } while (request != null && !TEXT_CONTENT_PATH.equals(request.getPath()));
            Assert.assertNotNull("Test timed out while waiting for expected request", request);
            Assert.assertEquals(TEST_APK_NAME, request.getHeader("X-Requested-With"));
            webViewOnUiThread.setCleanupTask(() -> waitForServiceWorkerDone(webViewOnUiThread));
        }
    }

    /**
     * Create, configure and start a MockWebServer to test the X-Requested-With header for
     * ServiceWorkers.
     */
    static MockWebServer getServiceWorkerMockServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                MockResponse response = new MockResponse();
                switch (request.getPath()) {
                    case INDEX_HTML_PATH:
                        response.setResponseCode(200);
                        response.setHeader("Content-Type", "text/html");
                        response.setBody(INDEX_HTML_DOCUMENT);
                        break;
                    case SERVICE_WORKER_PATH:
                        response.setResponseCode(200);
                        response.setHeader("Content-Type", "text/javascript");
                        response.setBody(SERVICE_WORKER_JAVASCRIPT);
                        break;
                    case TEXT_CONTENT_PATH:
                        response.setResponseCode(200);
                        response.setHeader("Content-Type", "text/text");
                        response.setBody(TEXT_CONTENT);
                        break;
                    default:
                        response.setResponseCode(404);
                        break;
                }
                return response;
            }
        });
        server.start();
        return server;
    }

    /**
     * Wait for the done boolean to be set, to indicate that serviceworkers have been unregistered.
     * This is crucial to clean up any remaining renderer processes that otherwise cause problems
     * for other tests.
     * See b/230078824.
     */
    @SuppressLint("BanThreadSleep")
    private static void waitForServiceWorkerDone(final WebViewOnUiThread webViewOnUiThread) {
        long timeout = SystemClock.uptimeMillis() + POLL_TIMEOUT_DURATION_MS;
        while (SystemClock.uptimeMillis() < timeout && !"true".equals(
                webViewOnUiThread.evaluateJavascriptSync("window.done"))) {
            try {
                //noinspection BusyWait We want to wait, to let the WebView finish the test
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                // If we haven't reached our timeout yet, keep waiting
            }
        }
        Assert.assertEquals("true", webViewOnUiThread.evaluateJavascriptSync("window.done"));
    }

    @Test
    public void testCookieInterceptReceivesHeader() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        mSettings.setIncludeCookiesOnShouldInterceptRequestEnabled(true);

        BlockingQueue<Map<String, String>> interceptInfo = new LinkedBlockingQueue<>();

        String interceptUrl = runCookieInterceptServiceWorkerLoad(interceptInfo);

        Map<String, String> requestHeaders = interceptInfo.take();
        Assert.assertTrue(requestHeaders.containsKey("Cookie"));
        Assert.assertEquals("foo=bar", requestHeaders.get("Cookie"));

        CookieManager cookieManager = CookieManager.getInstance();

        Set<String> cookies = new HashSet<>(
                CookieManagerCompat.getCookieInfo(cookieManager, interceptUrl));
        Assert.assertEquals(Set.of(
                "foo=bar; domain=localhost; path=/",
                "bar=baz; domain=localhost; path=/",
                "baz=foo; domain=localhost; path=/"), cookies);
    }

    @Test
    public void testCookieInterceptNoHeadersIfDisabled() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.COOKIE_INTERCEPT);
        mSettings.setIncludeCookiesOnShouldInterceptRequestEnabled(false);

        BlockingQueue<Map<String, String>> interceptInfo = new LinkedBlockingQueue<>();
        String interceptUrl = runCookieInterceptServiceWorkerLoad(interceptInfo);

        Map<String, String> requestHeaders = interceptInfo.take();
        Assert.assertFalse(requestHeaders.containsKey("Cookie"));

        CookieManager cookieManager = CookieManager.getInstance();
        Set<String> cookies = new HashSet<>(
                CookieManagerCompat.getCookieInfo(cookieManager, interceptUrl));
        Assert.assertEquals(Set.of("foo=bar; domain=localhost; path=/"), cookies);
    }

    /**
     * Starts a test web server that serves a service worker, ensures a cookie is present for the
     * URL fetched by the service worker, loads the page, intercepts the response, adds a number
     * of set-cookie header values and returns the URL that was used.
     */
    private static @NonNull String runCookieInterceptServiceWorkerLoad(
            BlockingQueue<Map<String, String>> interceptInfo) throws Exception {
        CookieManager cookieManager = CookieManager.getInstance();
        try (MockWebServer server = getServiceWorkerMockServer();
                WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            webViewOnUiThread.getSettings().setJavaScriptEnabled(true);
            final String interceptUrl = server.url(TEXT_CONTENT_PATH).toString();
            cookieManager.setCookie(interceptUrl, "foo=bar");

            ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(
                    new ServiceWorkerClientCompat() {
                        @Override
                        public @Nullable WebResourceResponse
                        shouldInterceptRequest(
                                @NonNull WebResourceRequest request) {
                            if (request.getUrl().toString().equals(interceptUrl)) {
                                interceptInfo.add(request.getRequestHeaders());
                                WebResourceResponseCompat response = new WebResourceResponseCompat(
                                        "text/text", "utf-8", 200, "OK",
                                        Collections.emptyMap(),
                                        new ByteArrayInputStream(
                                                TEXT_CONTENT.getBytes(StandardCharsets.UTF_8)));
                                response.setCookies(
                                        List.of("bar=baz", "baz=foo"));
                                return response.toWebResourceResponse();
                            }
                            return null;
                        }
                    });

            String requestUrl = server.url(INDEX_HTML_PATH).toString();
            webViewOnUiThread.loadUrl(requestUrl);
            waitForServiceWorkerDone(webViewOnUiThread);
            return interceptUrl;
        }
    }


}

