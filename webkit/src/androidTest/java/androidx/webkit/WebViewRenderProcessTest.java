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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewRenderProcessTest {
    private boolean terminateRenderProcessOnUiThread(
            final WebViewRenderProcess renderer) {
        return WebkitUtils.onMainThreadSync(() -> renderer.terminate());
    }

    WebViewRenderProcess getRenderProcessOnUiThread(final WebView webView) {
        return WebkitUtils.onMainThreadSync(() -> WebViewCompat.getWebViewRenderProcess(webView));
    }

    private ListenableFuture<WebViewRenderProcess> startAndGetRenderProcess(
            final WebView webView) throws Throwable {
        final ResolvableFuture<WebViewRenderProcess> future = ResolvableFuture.create();

        WebkitUtils.onMainThread(() -> {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    WebViewRenderProcess result =
                            WebViewCompat.getWebViewRenderProcess(webView);
                    future.set(result);
                }
            });
            webView.loadUrl("about:blank");
        });

        return future;
    }

    ListenableFuture<Boolean> catchRenderProcessTermination(final WebView webView) {
        final ResolvableFuture<Boolean> future = ResolvableFuture.create();

        WebkitUtils.onMainThread(() -> {
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
        });

        return future;
    }

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER);

        // Ensure that any existing renderer still alive after a previous test is terminated.
        // TODO(tobiasjs): This assumes that WebView uses at most one renderer, which is true
        // for now but may not remain so in future.
        final WebView webView = WebViewOnUiThread.createWebView();
        final WebViewRenderProcess renderProcess = getRenderProcessOnUiThread(webView);
        WebViewOnUiThread.destroy(webView);
        if (renderProcess != null) {
            terminateRenderProcessOnUiThread(renderProcess);
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    public void testGetWebViewRenderProcessPreO() throws Throwable {
        // It should not be possible to get a renderer pre-O
        WebView webView = WebViewOnUiThread.createWebView();
        final WebViewRenderProcess renderer = startAndGetRenderProcess(webView).get();
        Assert.assertNull(renderer);

        WebViewOnUiThread.destroy(webView);
    }

    @LargeTest
    @Test
    @SuppressLint("NewApi")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void testGetWebViewRenderProcess() throws Throwable {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS_QUERY)
                && !WebViewCompat.isMultiProcessEnabled()) {
            return;
        }

        final WebView webView = WebViewOnUiThread.createWebView();

        final WebViewRenderProcess preStartRenderProcess = getRenderProcessOnUiThread(webView);
        Assert.assertNotNull(
                "Should be possible to obtain a renderer handle before the renderer has started.",
                preStartRenderProcess);
        Assert.assertFalse(
                "Should not be able to terminate an unstarted renderer.",
                terminateRenderProcessOnUiThread(preStartRenderProcess));

        final WebViewRenderProcess renderer = startAndGetRenderProcess(webView).get();
        Assert.assertSame(
                "The pre- and post-start renderer handles should be the same object.",
                renderer, preStartRenderProcess);

        Assert.assertSame(
                "When getWebViewRender is called a second time, it should return the same object.",
                renderer, startAndGetRenderProcess(webView).get());

        ListenableFuture<Boolean> terminationFuture = catchRenderProcessTermination(webView);
        Assert.assertTrue(
                "A started renderer should be able to be terminated.",
                terminateRenderProcessOnUiThread(renderer));
        Assert.assertTrue(
                "Terminating a renderer should result in onRenderProcessGone being called.",
                terminationFuture.get());

        Assert.assertFalse(
                "It should not be possible to terminate a renderer that has already terminated.",
                terminateRenderProcessOnUiThread(renderer));

        final WebView webView2 = WebViewOnUiThread.createWebView();
        Assert.assertNotSame(
                "After a renderer restart, the new renderer handle object should be different.",
                renderer, startAndGetRenderProcess(webView2).get());

        // Ensure that we clean up webView2. webView has been destroyed by the WebViewClient
        // installed by catchRenderProcessTermination
        WebViewOnUiThread.destroy(webView2);
    }
}
