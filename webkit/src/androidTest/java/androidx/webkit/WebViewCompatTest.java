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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    private static final long TEST_TIMEOUT = 20000L;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @Test
    public void testVisualStateCallbackCalled() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK));

        final CountDownLatch callbackLatch = new CountDownLatch(1);
        final long kRequest = 100;

        mWebViewOnUiThread.loadUrl("about:blank");

        mWebViewOnUiThread.postVisualStateCallbackCompat(kRequest,
                new WebViewCompat.VisualStateCallback() {
                        public void onComplete(long requestId) {
                            assertEquals(kRequest, requestId);
                            callbackLatch.countDown();
                        }
                });

        assertTrue(callbackLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCheckThread() {
        // Skip this test if VisualStateCallback is not supported.
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK));
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

    @Test
    public void testStartSafeBrowsingUseApplicationContext() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING));

        final MockContext ctx =
                new MockContext(InstrumentationRegistry.getTargetContext().getApplicationContext());
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.startSafeBrowsing(ctx, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                assertTrue(ctx.wasGetApplicationContextCalled());
                resultLatch.countDown();
                return;
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testStartSafeBrowsingWithNullCallbackDoesntCrash() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING));

        WebViewCompat.startSafeBrowsing(InstrumentationRegistry.getTargetContext(), null);
    }

    @Test
    public void testStartSafeBrowsingInvokesCallback() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING));

        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.startSafeBrowsing(
                InstrumentationRegistry.getTargetContext().getApplicationContext(),
                new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        assertTrue(Looper.getMainLooper().isCurrentThread());
                        resultLatch.countDown();
                        return;
                    }
                });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetSafeBrowsingWhitelistWithMalformedList() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_WHITELIST));

        List whitelist = new ArrayList<String>();
        // Protocols are not supported in the whitelist
        whitelist.add("http://google.com");
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                assertFalse(success);
                resultLatch.countDown();
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetSafeBrowsingWhitelistWithValidList() throws Exception {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_WHITELIST));
        // This test relies on the onSafeBrowsingHit callback to verify correctness.
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_HIT));

        List whitelist = new ArrayList<String>();
        whitelist.add("safe-browsing");
        final CountDownLatch resultLatch = new CountDownLatch(1);
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                assertTrue(success);
                resultLatch.countDown();
            }
        });
        assertTrue(resultLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));

        final CountDownLatch resultLatch2 = new CountDownLatch(1);
        mWebViewOnUiThread.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                resultLatch2.countDown();
            }

            @Override
            public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType,
                    SafeBrowsingResponseCompat callback) {
                Assert.fail("Should not invoke onSafeBrowsingHit");
            }
        });

        mWebViewOnUiThread.loadUrl("chrome://safe-browsing/match?type=malware");

        // Wait until page load has completed
        assertTrue(resultLatch2.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetSafeBrowsingPrivacyPolicyUrl() throws Exception {
        assumeTrue(
                WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL));

        assertNotNull(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl());
        try {
            new URL(WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString());
        } catch (MalformedURLException e) {
            Assert.fail("The privacy policy URL should be a well-formed URL");
        }
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
