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

import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebViewRenderProcessClientTest {
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

    private static class JSBlocker {
        // A CoundDownLatch is used here, instead of a Future, because that makes it
        // easier to support requiring variable numbers of releaseBlock() calls
        // to unblock.
        private CountDownLatch mLatch;
        private ResolvableFuture<Void> mBecameBlocked;
        JSBlocker(int requiredReleaseCount) {
            mLatch = new CountDownLatch(requiredReleaseCount);
            mBecameBlocked = ResolvableFuture.create();
        }

        JSBlocker() {
            this(1);
        }

        public void releaseBlock() {
            mLatch.countDown();
        }

        @JavascriptInterface
        public void block() throws Exception {
            // This blocks indefinitely (until signalled) on a background thread.
            // The actual test timeout is not determined by this wait, but by other
            // code waiting for the onRenderProcessUnresponsive() call.
            mBecameBlocked.set(null);
            mLatch.await();
        }

        public void waitForBlocked() {
            WebkitUtils.waitForFuture(mBecameBlocked);
        }
    }

    private void blockRenderProcess(final JSBlocker blocker) {
        WebkitUtils.onMainThreadSync(() -> {
            WebView webView = mWebViewOnUiThread.getWebViewOnCurrentThread();
            webView.evaluateJavascript("blocker.block();", null);
            blocker.waitForBlocked();
            // Sending an input event that does not get acknowledged will cause
            // the unresponsive renderer event to fire.
            webView.dispatchKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        });
    }

    private void addJsBlockerInterface(final JSBlocker blocker) {
        WebkitUtils.onMainThreadSync(() -> {
            WebView webView = mWebViewOnUiThread.getWebViewOnCurrentThread();
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(blocker, "blocker");
        });
    }

    private void testWebViewRenderProcessClientOnExecutor(Executor executor) throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);
        final JSBlocker blocker = new JSBlocker();
        final ResolvableFuture<Void> rendererUnblocked = ResolvableFuture.create();

        WebViewRenderProcessClient client = new WebViewRenderProcessClient() {
            @Override
            public void onRenderProcessUnresponsive(WebView view, WebViewRenderProcess renderer) {
                // Let the renderer unblock.
                blocker.releaseBlock();
            }

            @Override
            public void onRenderProcessResponsive(WebView view, WebViewRenderProcess renderer) {
                // Notify that the renderer has been unblocked.
                rendererUnblocked.set(null);
            }
        };
        if (executor == null) {
            mWebViewOnUiThread.setWebViewRenderProcessClient(client);
        } else {
            mWebViewOnUiThread.setWebViewRenderProcessClient(executor, client);
        }

        addJsBlockerInterface(blocker);
        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        blockRenderProcess(blocker);
        WebkitUtils.waitForFuture(rendererUnblocked);
    }

    @Test
    public void testWebViewRenderProcessClientWithoutExecutor() throws Throwable {
        testWebViewRenderProcessClientOnExecutor(null);
    }

    @Test
    public void testWebViewRenderProcessClientWithExecutor() throws Throwable {
        final AtomicInteger executorCount = new AtomicInteger();
        testWebViewRenderProcessClientOnExecutor(new Executor() {
            @Override
            public void execute(Runnable r) {
                executorCount.incrementAndGet();
                r.run();
            }
        });
        Assert.assertEquals(2, executorCount.get());
    }

    @Test
    public void testSetWebViewRenderProcessClient() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);

        Assert.assertNull("Initially the renderer client should be null",
                mWebViewOnUiThread.getWebViewRenderProcessClient());

        final WebViewRenderProcessClient client = new WebViewRenderProcessClient() {
            @Override
            public void onRenderProcessUnresponsive(WebView view, WebViewRenderProcess renderer) {}

            @Override
            public void onRenderProcessResponsive(WebView view, WebViewRenderProcess renderer) {}
        };
        mWebViewOnUiThread.setWebViewRenderProcessClient(client);

        Assert.assertSame(
                "After the renderer client is set, getting it should return the same object",
                client, mWebViewOnUiThread.getWebViewRenderProcessClient());
    }
}
