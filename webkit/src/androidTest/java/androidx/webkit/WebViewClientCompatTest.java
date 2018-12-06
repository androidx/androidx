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

import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewClientCompatTest {
    private WebViewOnUiThread mWebViewOnUiThread;

    private static final String TEST_URL = "http://www.example.com/";
    private static final String TEST_SAFE_BROWSING_URL =
            "chrome://safe-browsing/match?type=malware";

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread();
    }

    @After
    public void cleanUp() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testShouldOverrideUrlLoadingDefault. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    @SdkSuppress(minSdkVersion = 21) // to instantiate WebResourceRequest
    public void testShouldOverrideUrlLoadingDefault() {
        // This never calls into chromium, so we don't need to do any feature checks.

        final MockWebViewClient webViewClient = new MockWebViewClient();

        // Create any valid WebResourceRequest, the return values don't matter much.
        final WebResourceRequest resourceRequest = new WebResourceRequest() {
            @Override
            public Uri getUrl() {
                return Uri.parse(TEST_URL);
            }

            @Override
            public boolean isForMainFrame() {
                return false;
            }

            @Override
            public boolean isRedirect() {
                return false;
            }

            @Override
            public boolean hasGesture() {
                return false;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public Map<String, String> getRequestHeaders() {
                return new HashMap<>();
            }
        };

        Assert.assertFalse(webViewClient.shouldOverrideUrlLoading(
                mWebViewOnUiThread.getWebViewOnCurrentThread(), resourceRequest));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testShouldOverrideUrlLoading. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testShouldOverrideUrlLoading() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS);
        WebkitUtils.checkFeature(WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT);

        String data = "<html><body>"
                + "<a href=\"" + TEST_URL + "\" id=\"link\">new page</a>"
                + "</body></html>";
        mWebViewOnUiThread.loadDataAndWaitForCompletion(data, "text/html", null);
        final ResolvableFuture<Void> pageFinishedFuture = ResolvableFuture.create();
        final MockWebViewClient webViewClient = new MockWebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinishedFuture.set(null);
            }
        };
        mWebViewOnUiThread.setWebViewClient(webViewClient);
        mWebViewOnUiThread.getSettings().setJavaScriptEnabled(true);
        clickOnLinkUsingJs("link", mWebViewOnUiThread);
        WebkitUtils.waitForFuture(pageFinishedFuture);
        Assert.assertEquals(TEST_URL,
                webViewClient.getLastShouldOverrideResourceRequest().getUrl().toString());

        WebResourceRequest request = webViewClient.getLastShouldOverrideResourceRequest();
        Assert.assertNotNull(request);
        Assert.assertTrue(request.isForMainFrame());
        Assert.assertFalse(WebResourceRequestCompat.isRedirect(request));
        Assert.assertFalse(request.hasGesture());
    }

    private void clickOnLinkUsingJs(final String linkId, WebViewOnUiThread webViewOnUiThread)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ResolvableFuture<String> javascriptFuture = ResolvableFuture.create();
        ValueCallback<String> callback = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                javascriptFuture.set(value);
            }
        };
        webViewOnUiThread.evaluateJavascript(
                "document.getElementById('" + linkId + "').click();"
                        + "console.log('element with id [" + linkId + "] clicked');", callback);
        // TODO(ntfschr): consider asserting the value.
        WebkitUtils.waitForFuture(javascriptFuture);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testOnReceivedError. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOnReceivedError() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR);
        WebkitUtils.checkFeature(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE);

        final MockWebViewClient webViewClient = new MockWebViewClient();
        mWebViewOnUiThread.setWebViewClient(webViewClient);

        String wrongUri = "invalidscheme://some/resource";
        Assert.assertNull(webViewClient.getOnReceivedResourceError());
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(wrongUri);
        Assert.assertNotNull(webViewClient.getOnReceivedResourceError());
        Assert.assertEquals(WebViewClient.ERROR_UNSUPPORTED_SCHEME,
                webViewClient.getOnReceivedResourceError().getErrorCode());
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testOnReceivedErrorForSubresource. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOnReceivedErrorForSubresource() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR);

        final MockWebViewClient webViewClient = new MockWebViewClient();
        mWebViewOnUiThread.setWebViewClient(webViewClient);

        Assert.assertNull(webViewClient.getOnReceivedResourceError());
        String data = "<html>"
                + "  <body>"
                + "    <img src=\"invalidscheme://some/resource\" />"
                + "  </body>"
                + "</html>";

        mWebViewOnUiThread.loadDataAndWaitForCompletion(data, "text/html", null);
        Assert.assertNotNull(webViewClient.getOnReceivedResourceError());
        Assert.assertEquals(WebViewClient.ERROR_UNSUPPORTED_SCHEME,
                webViewClient.getOnReceivedResourceError().getErrorCode());
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testOnSafeBrowsingHitBackToSafety. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOnSafeBrowsingHitBackToSafety() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_HIT);
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY);
        WebkitUtils.checkFeature(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE);

        final SafeBrowsingBackToSafetyClient backToSafetyWebViewClient =
                new SafeBrowsingBackToSafetyClient();
        mWebViewOnUiThread.setWebViewClient(backToSafetyWebViewClient);
        WebSettingsCompat.setSafeBrowsingEnabled(mWebViewOnUiThread.getSettings(), true);

        // Load any page
        String data = "<html><body>some safe page</body></html>";
        mWebViewOnUiThread.loadDataAndWaitForCompletion(data, "text/html", null);
        final String originalUrl = mWebViewOnUiThread.getUrl();

        enableSafeBrowsingAndLoadUnsafePage(backToSafetyWebViewClient);

        // Back to safety should produce a network error
        Assert.assertNotNull(backToSafetyWebViewClient.getOnReceivedResourceError());
        Assert.assertEquals(WebViewClient.ERROR_UNSAFE_RESOURCE,
                backToSafetyWebViewClient.getOnReceivedResourceError().getErrorCode());

        // Check that we actually navigated backward
        Assert.assertEquals(originalUrl, mWebViewOnUiThread.getUrl());
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testOnSafeBrowsingHitProceed. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOnSafeBrowsingHitProceed() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_HIT);
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        WebkitUtils.checkFeature(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED);

        final SafeBrowsingProceedClient proceedWebViewClient = new SafeBrowsingProceedClient();
        mWebViewOnUiThread.setWebViewClient(proceedWebViewClient);
        WebSettingsCompat.setSafeBrowsingEnabled(mWebViewOnUiThread.getSettings(), true);

        // Load any page
        String data = "<html><body>some safe page</body></html>";
        mWebViewOnUiThread.loadDataAndWaitForCompletion(data, "text/html", null);

        enableSafeBrowsingAndLoadUnsafePage(proceedWebViewClient);

        // Check that we actually proceeded
        Assert.assertEquals(TEST_SAFE_BROWSING_URL, mWebViewOnUiThread.getUrl());
    }

    private void enableSafeBrowsingAndLoadUnsafePage(SafeBrowsingClient client) throws Throwable {
        // Note: Safe Browsing depends on user opt-in as well, so we can't assume it's actually
        // enabled. #getSafeBrowsingEnabled will tell us the true state of whether Safe Browsing is
        // enabled.
        boolean deviceSupportsSafeBrowsing =
                WebSettingsCompat.getSafeBrowsingEnabled(mWebViewOnUiThread.getSettings());
        final String msg = "The device should support Safe Browsing";
        Assume.assumeTrue(msg, deviceSupportsSafeBrowsing);

        Assert.assertNull(client.getOnReceivedResourceError());
        mWebViewOnUiThread.loadUrlAndWaitForCompletion(TEST_SAFE_BROWSING_URL);

        Assert.assertEquals(TEST_SAFE_BROWSING_URL,
                client.getOnSafeBrowsingHitRequest().getUrl().toString());
        Assert.assertTrue(client.getOnSafeBrowsingHitRequest().isForMainFrame());
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.WebViewClientTest#testOnPageCommitVisibleCalled. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    @Test
    public void testOnPageCommitVisibleCalled() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);

        final ResolvableFuture<String> pageCommitVisibleFuture = ResolvableFuture.create();
        mWebViewOnUiThread.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
                pageCommitVisibleFuture.set(url);
            }
        });

        mWebViewOnUiThread.loadUrl("about:blank");
        Assert.assertEquals(WebkitUtils.waitForFuture(pageCommitVisibleFuture), "about:blank");
    }

    @Test
    public void testResetClientToCompat() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.VISUAL_STATE_CALLBACK);

        final ResolvableFuture<String> pageCommitVisibleFuture = ResolvableFuture.create();
        WebViewClient nonCompatClient = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap bitmap) {
                pageCommitVisibleFuture.setException(new IllegalStateException(
                        "This client should not have callbacks invoked"));
            }
        };
        mWebViewOnUiThread.setWebViewClient(nonCompatClient);

        WebViewClientCompat compatClient = new WebViewClientCompat() {
            @Override
            public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
                pageCommitVisibleFuture.set(url);
            }
        };
        mWebViewOnUiThread.setWebViewClient(compatClient); // reset to the new client
        mWebViewOnUiThread.loadUrl("about:blank");
        Assert.assertEquals(WebkitUtils.waitForFuture(pageCommitVisibleFuture), "about:blank");
    }

    @Test
    public void testResetClientToRegular() throws Exception {
        WebViewClientCompat compatClient = new WebViewClientCompat() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap bitmap) {
                Assert.fail("This client should not have callbacks invoked");
            }
        };
        mWebViewOnUiThread.setWebViewClient(compatClient);

        final ResolvableFuture<String> pageFinishedFuture = ResolvableFuture.create();
        WebViewClient nonCompatClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinishedFuture.set(url);
            }
        };
        mWebViewOnUiThread.setWebViewClient(nonCompatClient); // reset to the new client
        mWebViewOnUiThread.loadUrl("about:blank");
        Assert.assertEquals(WebkitUtils.waitForFuture(pageFinishedFuture), "about:blank");
    }

    private class MockWebViewClient extends WebViewOnUiThread.WaitForLoadedClient {
        private boolean mOnPageStartedCalled;
        private boolean mOnPageFinishedCalled;
        private boolean mOnLoadResourceCalled;
        private WebResourceErrorCompat mOnReceivedResourceError;
        private WebResourceResponse mOnReceivedHttpError;
        private WebResourceRequest mLastShouldOverrideResourceRequest;

        MockWebViewClient() {
            super(mWebViewOnUiThread);
        }

        public WebResourceErrorCompat getOnReceivedResourceError() {
            return mOnReceivedResourceError;
        }

        public WebResourceResponse getOnReceivedHttpError() {
            return mOnReceivedHttpError;
        }

        public WebResourceRequest getLastShouldOverrideResourceRequest() {
            return mLastShouldOverrideResourceRequest;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mOnPageStartedCalled = true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Assert.assertTrue(mOnPageStartedCalled);
            Assert.assertTrue(mOnLoadResourceCalled);
            mOnPageFinishedCalled = true;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            mOnLoadResourceCalled = true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            // This can be called if a test runs for a WebView which does not support the {@link
            // WebViewFeature#RECEIVE_WEB_RESOURCE_ERROR} feature.
        }

        @Override
        public void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
                @NonNull WebResourceErrorCompat error) {
            mOnReceivedResourceError = error;
        }

        @Override
        public void onReceivedHttpError(@NonNull WebView view, @NonNull WebResourceRequest request,
                @NonNull WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            mOnReceivedHttpError = errorResponse;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // This can be called if a test runs for a WebView which does not support the {@link
            // WebViewFeature#SHOULD_OVERRIDE_WITH_REDIRECTS} feature.
            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view,
                @NonNull WebResourceRequest request) {
            mLastShouldOverrideResourceRequest = request;
            return false;
        }
    }

    private class SafeBrowsingClient extends MockWebViewClient {
        private WebResourceRequest mOnSafeBrowsingHitRequest;
        private int mOnSafeBrowsingHitThreatType;

        public WebResourceRequest getOnSafeBrowsingHitRequest() {
            return mOnSafeBrowsingHitRequest;
        }

        public void setOnSafeBrowsingHitRequest(WebResourceRequest request) {
            mOnSafeBrowsingHitRequest = request;
        }

        public int getOnSafeBrowsingHitThreatType() {
            return mOnSafeBrowsingHitThreatType;
        }

        public void setOnSafeBrowsingHitThreatType(int type) {
            mOnSafeBrowsingHitThreatType = type;
        }
    }

    private class SafeBrowsingBackToSafetyClient extends SafeBrowsingClient {
        @Override
        public void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
                int threatType, @NonNull SafeBrowsingResponseCompat response) {
            // Immediately go back to safety to return the network error code
            setOnSafeBrowsingHitRequest(request);
            setOnSafeBrowsingHitThreatType(threatType);
            response.backToSafety(/* report */ true);
        }
    }

    private class SafeBrowsingProceedClient extends SafeBrowsingClient {
        @Override
        public void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
                int threatType, @NonNull SafeBrowsingResponseCompat response) {
            // Proceed through Safe Browsing warnings
            setOnSafeBrowsingHitRequest(request);
            setOnSafeBrowsingHitThreatType(threatType);
            response.proceed(/* report */ true);
        }
    }
}
