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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.os.BuildCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WebViewCompatTest {
    WebViewOnUiThread mWebViewOnUiThread;

    private static final long TEST_TIMEOUT = 20000L;

    @Before
    public void setUp() {
        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
    }

    @MediumTest
    @Test
    public void testVisualStateCallbackCalled() throws Exception {
        // TODO(gsennton) activate this test for pre-P devices when we can pre-install a WebView APK
        // containing support for the WebView Support Library, see b/73454652.
        if (!BuildCompat.isAtLeastP()) return;

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

    @MediumTest
    @Test
    public void testCheckThread() {
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
}
