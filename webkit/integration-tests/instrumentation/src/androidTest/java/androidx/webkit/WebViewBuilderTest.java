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

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Choreographer;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.webkit.instrumentation.test.R;
import androidx.webkit.test.common.TestWebMessageListener;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
public class WebViewBuilderTest {
    @Rule
    public final ActivityScenarioRule<WebViewTestActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(WebViewTestActivity.class);

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V1);
    }

    @Test
    public void testConstructsWebView() {
        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);

        mActivityScenarioRule.getScenario().onActivity(activity -> {
            try {
                WebView webView = builder.build(activity);
                Assert.assertNotNull(webView);
                Assert.assertTrue(webView instanceof WebView);

                // We then destroy the WebView to avoid leaking into GC tests.
                webView.destroy();
            } catch (WebViewBuilderException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void testConstructsWebViewTwice() {
        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);

        mActivityScenarioRule.getScenario().onActivity(activity -> {
            try {
                WebView webView = builder.build(activity);
                WebView webView2 = builder.build(activity);
                // These were two different WebView objects created.
                Assert.assertTrue(webView != webView2);

                // We then destroy these WebViews to avoid leaking into GC tests.
                webView.destroy();
                webView2.destroy();
            } catch (WebViewBuilderException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void testApplyToUnusedWebView_succeedsOnce() {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2);

        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);
        WebViewBuilder builder2 = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);

        mActivityScenarioRule.getScenario().onActivity(activity -> {
            WebView webView = new WebView(activity);
            try {
                WebView outWebView = builder.applyTo(webView);
                // The argument should be returned as is.
                Assert.assertSame(webView, outWebView);

                // Only one applyTo call per WebView is allowed, regardless of builder.
                Assert.assertThrows(IllegalStateException.class, () -> {
                    builder.applyTo(webView);
                });
                Assert.assertThrows(IllegalStateException.class, () -> {
                    builder2.applyTo(webView);
                });

                // We then destroy the WebView to avoid leaking into GC tests.
                webView.destroy();
            } catch (WebViewBuilderException e) {
                throw new AssertionError(e);
            }
        });
    }

    /**
     * Asserts that IllegalStateException is thrown when calling
     * {@link WebViewBuilder#applyTo(WebView)} on a WebView that has first had a given action
     * performed on it.
     */
    private void assertBuilderApplicationThrowsForUsedWebView(Consumer<WebView> action) {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2);
        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            WebView webView = new WebView(activity);
            action.accept(webView);
            try {
                Assert.assertThrows(IllegalStateException.class,
                        () -> builder.applyTo(webView));

                // We then destroy the WebView to avoid leaking into GC tests.
                webView.destroy();
            } catch (WebViewBuilderException e) {
                throw new AssertionError(e);
            }
        });
    }

    // testApplyToUsedWebView_* test a non-exhaustive list of interesting APIs which should (or
    // should not) taint the WebView such that the builder cannot be applied. Some of the notable
    // things these tests cover include:
    // - Framework and SupportLib APIs
    // - APIs which do and don't go through AwContents.
    // - Getters and setters.
    // - Default and non-default profiles.
    // - Navigation.
    // - JavaScript interfaces.
    // - JavaScript evaluations.
    @Test
    public void testApplyToUsedWebView_getSettings_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(WebView::getSettings);
    }

    @Test
    // AndroidLintNewApi and ConstantValue lints fight over whether there should be an if statement.
    @SuppressWarnings("AndroidLintNewApi")
    public void testApplyToUsedWebView_getWebViewClient_illegalStateException() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        // Note that we specifically want to test the framework method here, not the compat one.
        // The compat version is tested separately.
        assertBuilderApplicationThrowsForUsedWebView(WebView::getWebViewClient);
    }

    @Test
    public void testApplyToUsedWebView_loadUrl_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView((webview) -> webview.loadUrl("about:blank"));
    }

    @Test
    public void testApplyToUsedWebView_saveState_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView((webview) -> webview.saveState(new Bundle()));
    }

    @Test
    public void testApplyToUsedWebView_restoreState_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> webview.restoreState(new Bundle()));
    }

    @Test
    public void testApplyToUsedWebView_addJavascriptInterface_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> webview.addJavascriptInterface(new Object(), "justTesting"));
    }

    @Test
    public void testApplyToUsedWebView_evaluateJavascript_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> webview.evaluateJavascript("1", null));
    }

    @Test
    public void testApplyToUsedWebView_setAcceptThirdPartyCookies_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true));
    }

    @Test
    public void testApplyToUsedWebView_compatGetWebViewClient_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_CLIENT);
        assertBuilderApplicationThrowsForUsedWebView(WebViewCompat::getWebViewClient);
    }

    @Test
    public void testApplyToUsedWebView_getProfile_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        assertBuilderApplicationThrowsForUsedWebView(WebViewCompat::getProfile);
    }

    @Test
    public void testApplyToUsedWebView_setDefaultProfile_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> WebViewCompat.setProfile(webview, Profile.DEFAULT_PROFILE_NAME));
    }

    @Test
    public void testApplyToUsedWebView_setNonDefaultProfile_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> WebViewCompat.setProfile(webview, "NonDefault"));
    }

    @Test
    public void testApplyToUsedWebView_compatSaveState_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.SAVE_STATE);
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> WebViewCompat.saveState(webview, new Bundle(), 1000000, true));
    }

    @Test
    public void testApplyToUsedWebView_addDocumentStartJavaScript_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.DOCUMENT_START_SCRIPT);
        assertBuilderApplicationThrowsForUsedWebView(
                (webview) -> WebViewCompat.addDocumentStartJavaScript(webview, "1", Set.of("*")));
    }

    @Test
    public void testApplyToUsedWebView_isAudioMuted_illegalStateException() {
        WebkitUtils.checkFeature(WebViewFeature.MUTE_AUDIO);
        assertBuilderApplicationThrowsForUsedWebView(WebViewCompat::isAudioMuted);
    }

    @Test
    public void testApplyToUsedWebView_onPause_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(WebView::onPause);
    }

    @Test
    public void testApplyToUsedWebView_onResume_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(WebView::onResume);
    }

    @Test
    public void testApplyToUsedWebView_pauseTimers_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(WebView::pauseTimers);
    }

    @Test
    public void testApplyToUsedWebView_resumeTimers_illegalStateException() {
        assertBuilderApplicationThrowsForUsedWebView(WebView::resumeTimers);
    }

    @Test
    public void testApplyToSubclassedWebView() {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2);

        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);

        class WebViewSubclass extends WebView {
            WebViewSubclass(Context context) {
                super(context);
            }
        }

        mActivityScenarioRule.getScenario().onActivity(activity -> {
            WebViewSubclass webView = new WebViewSubclass(activity);
            try {
                WebViewSubclass outWebView = builder.applyTo(webView);
                // The argument should be returned as is.
                Assert.assertSame(webView, outWebView);

                // We then destroy the WebView to avoid leaking into GC tests.
                webView.destroy();
            } catch (WebViewBuilderException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void testApplyToInflatedWebView() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2);

        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        mActivityScenarioRule.getScenario().onActivity(activity -> {
            // Inflate immediately.
            activity.setContentView(R.layout.inflated_webview);
            WebView webView = activity.findViewById(R.id.inflated_webview);

            // Wait a few frames for any natural View (etc.) interactions to happen, then call
            // applyTo. Note that this may not cover everything, e.g. input-related interactions.
            Choreographer.FrameCallback callback = new Choreographer.FrameCallback() {
                private int mRemainingFrames = 5;

                @Override
                public void doFrame(long frameTimeNanos) {
                    if (mRemainingFrames > 0) {
                        mRemainingFrames--;
                        Choreographer.getInstance().postFrameCallback(this);
                        return;
                    }

                    try {
                        WebView outWebView = builder.applyTo(webView);
                        // The argument should be returned as is.
                        Assert.assertSame(webView, outWebView);

                        // We then destroy the WebView to avoid leaking into GC tests.
                        webView.destroy();
                    } catch (WebViewBuilderException e) {
                        throw new AssertionError(e);
                    }
                    countDownLatch.countDown();
                }
            };

            Choreographer.getInstance().postFrameCallback(callback);
        });

        if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timed out waiting for applyTo to complete.");
        }
    }

    @Test
    public void testJavascriptInterface() throws Exception {
        try (MockWebServer server = new MockWebServer();
                MockWebServer nonEnabledServer = new MockWebServer()) {
            server.start();
            nonEnabledServer.start();

            server.enqueue(mockJavaScriptInterfaceResponse());
            nonEnabledServer.enqueue(mockJavaScriptInterfaceResponse());

            HttpUrl enabledUrl = server.url("");

            Set<String> originPatterns =
                    Set.of(
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
                            .addJavaScriptInterface(new TestInterface(1), "jsInterface")
                            .addJavaScriptInterface(new TestInterface(2), "jsInterface2")
                            .addJavaScriptInterface(new TestInterface(3), "jsInterface3")
                            .build();

            WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY)
                    .restrictJavaScriptInterfaces()
                    .addAllowlist(allowlist);

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
                new RestrictionAllowlist.Builder(Set.of("https://somesite.com"))
                        .addJavaScriptInterface(jsInterface, "jsInterface")
                        .build();

        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY)
                .addAllowlist(allowlist);

        // This builder did not call restrictJavaScriptInterfaces before allowlisting
        Assert.assertThrows(WebViewBuilderException.class, () -> build(builder));

        // After restricting JS, this should build fine:
        builder.restrictJavaScriptInterfaces();
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
                new RestrictionAllowlist.Builder(Set.of("https://someothersite.com"))
                        .addJavaScriptInterface(jsInterface, "jsInterface")
                        .build());
        Assert.assertThrows(WebViewBuilderException.class, () -> build(builder));

        // We then clean up this WebView to avoid leaking into the GC tests.
        WebViewOnUiThread.destroy(wv);
    }

    @Test
    public void testSetNoProfileUsesDefault() throws WebViewBuilderException {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);

        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY);
        WebView webView = build(builder);
        String profileName = WebkitUtils.onMainThreadSync(
                () -> WebViewCompat.getProfile(webView).getName());
        WebViewOnUiThread.destroy(webView);

        Assert.assertEquals(Profile.DEFAULT_PROFILE_NAME, profileName);
    }

    @Test
    public void testSetProfileName() throws WebViewBuilderException {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);

        String profileName = "NonDefault";
        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY)
                .setProfile(profileName);
        WebView webView = build(builder);
        String actualProfileName = WebkitUtils.onMainThreadSync(
                () -> WebViewCompat.getProfile(webView).getName());
        WebViewOnUiThread.destroy(webView);

        Assert.assertEquals(profileName, actualProfileName);
    }

    @Test
    public void testSetProfileNameWithApplyTo() throws WebViewBuilderException {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2);

        String profileName = "NonDefault";
        WebViewBuilder builder = new WebViewBuilder(WebViewBuilder.PRESET_LEGACY)
                .setProfile(profileName);
        String actualProfileName = WebkitUtils.onMainThreadSync(() -> {
            WebView webView =
                    builder.applyTo(new WebView(ApplicationProvider.getApplicationContext()));
            String result = WebViewCompat.getProfile(webView).getName();
            webView.destroy();
            return result;
        });

        Assert.assertEquals(profileName, actualProfileName);
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

    private MockResponse mockJavaScriptInterfaceResponse() {
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
