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
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.TestWebMessageListener;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class WebViewBuilderTest {
    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER);
    }

    @Test
    public void testConstructsWebView() {
        WebViewBuilder builder = new WebViewBuilder();

        try (ActivityScenario<WebViewTestActivity> scenario =
                ActivityScenario.launch(WebViewTestActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        try {
                            WebView webView = builder.build(activity);
                            Assert.assertNotNull(webView);
                            Assert.assertTrue(webView instanceof WebView);

                            // We then destroy the WebView to avoid leaking into GC tests.
                            webView.destroy();
                        } catch (WebViewBuilderException e) {
                            Assert.fail(e.toString());
                        }
                    });
        }
    }

    @Test
    public void testConstructsWebViewTwice() {
        WebViewBuilder builder = new WebViewBuilder();

        try (ActivityScenario<WebViewTestActivity> scenario =
                ActivityScenario.launch(WebViewTestActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        try {
                            WebView webView = builder.build(activity);
                            WebView webView2 = builder.build(activity);
                            // These were two different WebView objects created.
                            Assert.assertTrue(webView != webView2);

                            // We then destroy these WebViews to avoid leaking into GC tests.
                            webView.destroy();
                            webView2.destroy();
                        } catch (WebViewBuilderException e) {
                            Assert.fail(e.toString());
                        }
                    });
        }
    }

    @Test
    public void testJavascriptInterface() throws Exception {
        try (MockWebServer server = new MockWebServer();
                MockWebServer nonEnabledServer = new MockWebServer()) {
            server.start();
            nonEnabledServer.start();

            server.enqueue(mockJavascriptInterfaceResponse());
            nonEnabledServer.enqueue(mockJavascriptInterfaceResponse());

            HttpUrl enabledUrl = server.url("");

            List<String> originPatterns =
                    List.of(
                            enabledUrl.scheme()
                                    + "://"
                                    + enabledUrl.host()
                                    + ":"
                                    + enabledUrl.port());

            class TestInterface {
                TestInterface(int value) {
                    mValue = value;
                }

                @JavascriptInterface
                public int getNum() {
                    return mValue;
                }

                private int mValue;
            }

            RestrictionAllowlist allowlist =
                    new RestrictionAllowlist.Builder(originPatterns)
                            .javascriptInterface(new TestInterface(1), "jsInterface")
                            .javascriptInterface(new TestInterface(2), "jsInterface2")
                            .javascriptInterface(new TestInterface(3), "jsInterface3")
                            .build();

            WebViewBuilder builder =
                    new WebViewBuilder().restrictJavascriptInterface().addAllowlist(allowlist);

            WebView webview = build(builder);

            TestWebMessageListener listener = new TestWebMessageListener();
            configureListener(listener, webview);
            loadUrlAndWaitForLoad(webview, enabledUrl.toString(), listener);
            Assert.assertEquals("1", evaluateJavascript(webview, "jsInterface.getNum()"));
            Assert.assertEquals("2", evaluateJavascript(webview, "jsInterface2.getNum()"));
            Assert.assertEquals("3", evaluateJavascript(webview, "jsInterface3.getNum()"));

            loadUrlAndWaitForLoad(webview, nonEnabledServer.url("/").toString(), listener);
            Assert.assertEquals("null", evaluateJavascript(webview, "jsInterface.getNum()"));
            Assert.assertEquals("null", evaluateJavascript(webview, "jsInterface2.getNum()"));
            Assert.assertEquals("null", evaluateJavascript(webview, "jsInterface3.getNum()"));

            // We then clean up this WebView to avoid leaking into the GC tests.
            WebViewOnUiThread.destroy(webview);
        }
    }

    @Test
    public void testJavascriptInterface_validation() throws Exception {
        Object jsInterface = new Object();

        RestrictionAllowlist allowlist =
                new RestrictionAllowlist.Builder(List.of("https://somesite.com"))
                        .javascriptInterface(jsInterface, "jsInterface")
                        .build();

        WebViewBuilder builder = new WebViewBuilder().addAllowlist(allowlist);

        // This builder did not call restrictJavascriptInterface before allowlisting
        Assert.assertThrows(WebViewBuilderException.class, () -> build(builder));

        // After restricting JS, this should build fine:
        builder.restrictJavascriptInterface();
        WebView wv = build(builder);
        Assert.assertNotNull(wv);

        // This WebView should not be allowed to add a JavaScript interface
        // because it is restricted.
        Assert.assertThrows(
                IllegalStateException.class,
                () ->
                        WebkitUtils.onMainThreadSync(
                                () -> {
                                    wv.addJavascriptInterface(jsInterface, "blah");
                                    return wv;
                                }));

        // If we try to build with the same javascript interface name,
        // we should also get a validation failure.
        builder.addAllowlist(
                new RestrictionAllowlist.Builder(List.of("https://someothersite.com"))
                        .javascriptInterface(jsInterface, "jsInterface")
                        .build());
        Assert.assertThrows(WebViewBuilderException.class, () -> build(builder));

        // We then clean up this WebView to avoid leaking into the GC tests.
        WebViewOnUiThread.destroy(wv);
    }

    private WebView build(final WebViewBuilder builder) throws WebViewBuilderException {
        return WebkitUtils.onMainThreadSync(
                () -> {
                    WebView wv = builder.build(ApplicationProvider.getApplicationContext());
                    wv.getSettings().setJavaScriptEnabled(true);
                    return wv;
                });
    }

    private void configureListener(final TestWebMessageListener listener, final WebView webview) {
        WebkitUtils.onMainThreadSync(
                () -> {
                    WebViewCompat.addWebMessageListener(webview, "listener", Set.of("*"), listener);
                });
    }

    private void loadUrlAndWaitForLoad(
            final WebView webview, final String url, final TestWebMessageListener listener)
            throws Exception {
        WebkitUtils.onMainThread(() -> webview.loadUrl(url));
        WebMessageCompat message = listener.waitForOnPostMessage().mMessage;
        Assert.assertEquals("loaded", message.getData());
    }

    private String evaluateJavascript(final WebView webview, String script) {
        CompletableFuture<String> future = new CompletableFuture<String>();
        WebkitUtils.onMainThread(
                () ->
                        webview.evaluateJavascript(
                                script,
                                (result) -> {
                                    future.complete(result);
                                }));
        return WebkitUtils.waitForFuture(future);
    }

    private MockResponse mockJavascriptInterfaceResponse() {
        MockResponse response = new MockResponse();
        response.addHeader("Content-Type", "text/html");
        response.setBody(
                "<!DOCTYPE html>"
                        + "<html><body><script>"
                        + "listener.postMessage('loaded');"
                        + "</script></body></html>");
        return response;
    }
}
