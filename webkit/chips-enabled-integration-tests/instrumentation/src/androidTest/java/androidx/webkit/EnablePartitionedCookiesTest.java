/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context;
import android.webkit.CookieManager;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.test.common.TestWebMessageListener;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EnablePartitionedCookiesTest {
    private static final String TAG = "EnablePartitionedCookiesTest";
    private static final String LISTENER_NAME = "myListener";
    private static final String PAGE1 = "/page1";
    private static final String PAGE2 = "/page2";
    private static final String PARTITIONED_COOKIE = "test_partitioned_cookie=123";
    private static final String UNPARTITIONED_COOKIE = "unpartitioned_cookie=123";
    private static final TestWebMessageListener sListener = new TestWebMessageListener();

    private static final String IFRAME_HTML = String.join("\n",
            "<!DOCTYPE html><html>",
            "<head><link rel='shortcut icon' href='#'/></head>",
            "<body>",
            "    <script>",
            "        " + LISTENER_NAME + ".postMessage(document.cookie);",
            "    </script>",
            "<h1>iFrame Content</h1>",
            "</body></html>"
    );

    @BeforeClass
    public static void setUpClass() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        WebkitUtils.checkStartupFeature(ctx,
                WebViewFeature.STARTUP_FEATURE_CONFIGURE_PARTITIONED_COOKIES);
        // Note: only call ProcessGlobalConfig from @BeforeClass because this is only safe to call
        // once per process. If we call this from @Before or in the @Test method directly, then that
        // would prevent us from ever adding a second test case to this class.
        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setPartitionedCookiesEnabled(ctx, true);
        ProcessGlobalConfig.apply(config);
    }

    @Before
    public void setUp() {
        clearCookies();
        CookieManager.getInstance().setAcceptCookie(true);
    }

    @After
    public void tearDown() {
        clearCookies();
    }

    @Test
    public void testPartitionedCookiesEnabled() throws Exception {
        try (
                MockWebServer topLevelServer = new MockWebServer();
                MockWebServer iframeServer = new MockWebServer();
                WebViewOnUiThread webViewOnUiThread = new WebViewOnUiThread()) {
            webViewOnUiThread.getSettings().setJavaScriptEnabled(true);
            CookieManager.getInstance().setAcceptCookie(true);

            topLevelServer.start();
            iframeServer.start();
            String topLevelContentUrl = topLevelServer.url(PAGE1).toString();
            String iframeUrl = makeThirdPartyUrl(iframeServer.url(PAGE2).toString());
            webViewOnUiThread.addWebMessageListener(
                    LISTENER_NAME, /*allowedOriginRules*/ Collections.singleton("*"), sListener);

            final String partitionedCookieHeader =
                    PARTITIONED_COOKIE + "; Path=" + PAGE2 + "; SameSite=None; "
                            + "Secure; Partitioned";
            final String unpartitionedCookieHeader =
                    UNPARTITIONED_COOKIE + "; Path=" + PAGE2 + "; SameSite=None; "
                            + "Secure;";
            final MockResponse initialFrameResponse = new MockResponse()
                    .setBody(IFRAME_HTML)
                    .addHeader("Set-Cookie", unpartitionedCookieHeader)
                    .addHeader("Set-Cookie", partitionedCookieHeader);
            final MockResponse iframeResponse = new MockResponse()
                    .setBody(IFRAME_HTML);
            final MockResponse favIcoResponse = new MockResponse().setResponseCode(200);

            final String topLevelContent =
                    "<div><iframe src=\"" + iframeUrl + "\"></iframe></div>";
            final MockResponse topLevelResponse = new MockResponse()
                    .setResponseCode(200)
                    .setBody(topLevelContent);

            webViewOnUiThread.setAcceptThirdPartyCookies(true);

            iframeServer.enqueue(initialFrameResponse);
            iframeServer.enqueue(iframeResponse);
            webViewOnUiThread.loadUrl(iframeUrl);
            TestWebMessageListener.Data data = sListener.waitForOnPostMessage();

            iframeServer.enqueue(iframeResponse);
            iframeServer.enqueue(iframeResponse);
            webViewOnUiThread.loadUrl(iframeUrl);
            data = sListener.waitForOnPostMessage();
            Assert.assertTrue(
                    "The unpartitioned cookie should be present when loaded as top-level frame",
                    data.mMessage.getData().contains(UNPARTITIONED_COOKIE));
            Assert.assertTrue(
                    "The partitioned cookie should be present when loaded as top-level frame",
                    data.mMessage.getData().contains(PARTITIONED_COOKIE));

            topLevelServer.enqueue(topLevelResponse);
            iframeServer.enqueue(iframeResponse);
            iframeServer.enqueue(iframeResponse);
            webViewOnUiThread.loadUrl(topLevelContentUrl);
            data = sListener.waitForOnPostMessage();
            Assert.assertTrue(
                    "The unpartitioned cookie should be present when loaded as embedded frame",
                    data.mMessage.getData().contains(UNPARTITIONED_COOKIE));
            Assert.assertFalse(
                    "The partitioned cookie should not be present when loaded as embedded frame",
                    data.mMessage.getData().contains(PARTITIONED_COOKIE));
        }
    }

    private String makeThirdPartyUrl(String url) {
        return url.replaceAll("localhost", "127.0.0.1");
    }

    private void clearCookies() {
        final ResolvableFuture<Boolean> future = ResolvableFuture.create();
        WebkitUtils.onMainThread(() ->
                CookieManager.getInstance().removeAllCookies(future::set));
        WebkitUtils.waitForFuture(future);
    }
}
