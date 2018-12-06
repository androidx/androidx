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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewCompatTest {
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
     * android.webkit.cts.WebViewTest#testVisualStateCallbackCalled. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testVisualStateCallbackCalled() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);

        final long kRequest = 100;

        mWebViewOnUiThread.loadUrl("about:blank");

        final ResolvableFuture<Long> visualStateFuture = ResolvableFuture.create();
        mWebViewOnUiThread.postVisualStateCallbackCompat(kRequest,
                new WebViewCompat.VisualStateCallback() {
                        public void onComplete(long requestId) {
                            visualStateFuture.set(requestId);
                        }
                });

        assertEquals(kRequest, (long) WebkitUtils.waitForFuture(visualStateFuture));
    }

    @Test
    public void testCheckThread() {
        // Skip this test if VisualStateCallback is not supported.
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);
        try {
            WebViewCompat.postVisualStateCallback(mWebViewOnUiThread.getWebViewOnCurrentThread(), 5,
                    new WebViewCompat.VisualStateCallback() {
                        @Override
                        public void onComplete(long requestId) {
                        }
                    });
        } catch (RuntimeException e) {
            return;
        }
        fail("Calling a WebViewCompat method on the wrong thread must cause a run-time exception");
    }

    private static class MockContext extends ContextWrapper {
        private boolean mGetApplicationContextWasCalled;

        MockContext(Context context) {
            super(context);
        }

        public Context getApplicationContext() {
            mGetApplicationContextWasCalled = true;
            return super.getApplicationContext();
        }

        public boolean wasGetApplicationContextCalled() {
            return mGetApplicationContextWasCalled;
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingUseApplicationContext. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingUseApplicationContext() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final MockContext ctx =
                new MockContext(InstrumentationRegistry.getTargetContext().getApplicationContext());
        final ResolvableFuture<Boolean> startSafeBrowsingFuture = ResolvableFuture.create();
        WebViewCompat.startSafeBrowsing(ctx, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                startSafeBrowsingFuture.set(ctx.wasGetApplicationContextCalled());
            }
        });
        assertTrue(WebkitUtils.waitForFuture(startSafeBrowsingFuture));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingWithNullCallbackDoesntCrash.
     * Modifications to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingWithNullCallbackDoesntCrash() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        WebViewCompat.startSafeBrowsing(InstrumentationRegistry.getTargetContext(), null);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testStartSafeBrowsingInvokesCallback. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testStartSafeBrowsingInvokesCallback() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.START_SAFE_BROWSING);

        final ResolvableFuture<Boolean> startSafeBrowsingFuture = ResolvableFuture.create();
        WebViewCompat.startSafeBrowsing(
                InstrumentationRegistry.getTargetContext().getApplicationContext(),
                new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        startSafeBrowsingFuture.set(Looper.getMainLooper().isCurrentThread());
                    }
                });
        assertTrue(WebkitUtils.waitForFuture(startSafeBrowsingFuture));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingWhitelistWithMalformedList. Modifications
     * to this test should be reflected in that test as necessary. See
     * http://go/modifying-webview-cts.
     */
    @Test
    public void testSetSafeBrowsingWhitelistWithMalformedList() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_WHITELIST);

        List<String> whitelist = new ArrayList<>();
        // Protocols are not supported in the whitelist
        whitelist.add("http://google.com");
        final ResolvableFuture<Boolean> safeBrowsingWhitelistFuture = ResolvableFuture.create();
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                safeBrowsingWhitelistFuture.set(success);
            }
        });
        assertFalse(WebkitUtils.waitForFuture(safeBrowsingWhitelistFuture));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testSetSafeBrowsingWhitelistWithValidList. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @FlakyTest(bugId = 111690396)
    @Test
    public void testSetSafeBrowsingWhitelistWithValidList() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_WHITELIST);
        // This test relies on the onSafeBrowsingHit callback to verify correctness.
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_HIT);

        List<String> whitelist = new ArrayList<>();
        whitelist.add("safe-browsing");
        final ResolvableFuture<Boolean> safeBrowsingWhitelistFuture = ResolvableFuture.create();
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                safeBrowsingWhitelistFuture.set(success);
            }
        });
        assertTrue(WebkitUtils.waitForFuture(safeBrowsingWhitelistFuture));

        final ResolvableFuture<Void> pageFinishedFuture = ResolvableFuture.create();
        mWebViewOnUiThread.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinishedFuture.set(null);
            }

            @Override
            public void onSafeBrowsingHit(@NonNull WebView view,
                    @NonNull WebResourceRequest request, int threatType,
                    @NonNull SafeBrowsingResponseCompat callback) {
                pageFinishedFuture.setException(new IllegalStateException(
                        "Should not invoke onSafeBrowsingHit"));
            }
        });

        mWebViewOnUiThread.loadUrl("chrome://safe-browsing/match?type=malware");

        // Wait until page load has completed
        WebkitUtils.waitForFuture(pageFinishedFuture);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetSafeBrowsingPrivacyPolicyUrl. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetSafeBrowsingPrivacyPolicyUrl() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL);

        assertNotNull(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl());
        try {
            new URL(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString());
        } catch (MalformedURLException e) {
            Assert.fail("The privacy policy URL should be a well-formed URL");
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebViewClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebViewClient() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebViewClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebViewClient should return a default WebViewClient if it hasn't been set yet
        WebViewClient client = WebViewOnUiThread.getWebViewClient(webView);
        assertNotNull(client);
        assertTrue(client instanceof WebViewClient);

        // getWebViewClient should return the client after it has been set
        WebViewClient client2 = new WebViewClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebViewClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebViewClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewTest#testGetWebChromeClient. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testGetWebChromeClient() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_CHROME_CLIENT);

        // Create a new WebView because WebViewOnUiThread sets a WebChromeClient during
        // construction.
        WebView webView = WebViewOnUiThread.createWebView();

        // getWebChromeClient should return null if the client hasn't been set yet
        WebChromeClient client = WebViewOnUiThread.getWebChromeClient(webView);
        assertNull(client);

        // getWebChromeClient should return the client after it has been set
        WebChromeClient client2 = new WebChromeClient();
        assertNotSame(client, client2);
        WebViewOnUiThread.setWebChromeClient(webView, client2);
        assertSame(client2, WebViewOnUiThread.getWebChromeClient(webView));

        WebViewOnUiThread.destroy(webView);
    }

    /**
     * WebViewCompat.getCurrentWebViewPackage should be null on pre-L devices.
     * On L+ devices WebViewCompat.getCurrentWebViewPackage should be null only in exceptional
     * circumstances - like when the WebView APK is being updated, or for Wear devices. The L+
     * devices used in support library testing should have a non-null WebView package.
     */
    @Test
    public void testGetCurrentWebViewPackage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            assertNull(WebViewCompat.getCurrentWebViewPackage(
                    InstrumentationRegistry.getTargetContext()));
        } else {
            assertNotNull(
                    WebViewCompat.getCurrentWebViewPackage(
                            InstrumentationRegistry.getTargetContext()));
        }
    }
}
