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

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewRendererTest {
    private <T> ListenableFuture<T> onMainThread(final Callable<T> callable)  {
        final ResolvableFuture<T> future = ResolvableFuture.create();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    private ListenableFuture<Boolean> terminateRendererOnUiThread(
            final WebViewRenderer renderer) {
        return onMainThread(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return renderer.terminate();
            }
        });
    }

    ListenableFuture<WebViewRenderer> getRendererOnUiThread(final WebView webView) {
        return onMainThread(new Callable<WebViewRenderer>() {
            @Override
            public WebViewRenderer call() {
                return WebViewCompat.getWebViewRenderer(webView);
            }
        });
    }

    private ListenableFuture<WebViewRenderer> startAndGetRenderer(
            final WebView webView) throws Throwable {
        final ResolvableFuture<WebViewRenderer> future = ResolvableFuture.create();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        WebViewRenderer result = WebViewCompat.getWebViewRenderer(webView);
                        future.set(result);
                    }
                });
                webView.loadUrl("about:blank");
            }
        });

        return future;
    }

    ListenableFuture<Boolean> catchRendererTermination(final WebView webView) {
        final ResolvableFuture<Boolean> future = ResolvableFuture.create();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean onRenderProcessGone(
                            WebView view,
                            RenderProcessGoneDetail detail) {
                        view.destroy();
                        future.set(true);
                        return true;
                    }
                });
            }
        });

        return future;
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    public void testGetWebViewRendererPreO() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);

        // It should not be possible to get a renderer pre-O
        WebView webView = WebViewOnUiThread.createWebView();
        final WebViewRenderer renderer = startAndGetRenderer(webView).get();
        Assert.assertNull(renderer);

        WebViewOnUiThread.destroy(webView);
    }

    @LargeTest
    @Test
    @SuppressLint("NewApi")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testGetWebViewRenderer() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);
        // TODO(tobiasjs) some O devices are not multiprocess, and multiprocess can also be disabled
        // manually. This test should handle those scenarios.

        final WebView webView = WebViewOnUiThread.createWebView();

        final WebViewRenderer preStartRenderer = getRendererOnUiThread(webView).get();
        Assert.assertNotNull(
                "Should be possible to obtain a renderer handle before the renderer has started.",
                preStartRenderer);
        Assert.assertFalse(
                "Should not be able to terminate an unstarted renderer.",
                terminateRendererOnUiThread(preStartRenderer).get());

        final WebViewRenderer renderer = startAndGetRenderer(webView).get();
        Assert.assertSame(
                "The pre- and post-start renderer handles should be the same object.",
                renderer, preStartRenderer);

        Assert.assertSame(
                "When getWebViewRender is called a second time, it should return the same object.",
                renderer, startAndGetRenderer(webView).get());

        ListenableFuture<Boolean> terminationFuture = catchRendererTermination(webView);
        Assert.assertTrue(
                "A started renderer should be able to be terminated.",
                terminateRendererOnUiThread(renderer).get());
        Assert.assertTrue(
                "Terminating a renderer should result in onRenderProcessGone being called.",
                terminationFuture.get());

        Assert.assertFalse(
                "It should not be possible to terminate a renderer that has already terminated.",
                terminateRendererOnUiThread(renderer).get());

        final WebView webView2 = WebViewOnUiThread.createWebView();
        Assert.assertNotSame(
                "After a renderer restart, the new renderer handle object should be different.",
                renderer, startAndGetRenderer(webView2).get());

        // Ensure that we clean up webView2. webView has been destroyed by the WebViewClient
        // installed by catchRendererTermination
        WebViewOnUiThread.destroy(webView2);
    }
}
