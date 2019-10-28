/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.webkit.internal;

import android.annotation.SuppressLint;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewRenderProcess;
import androidx.webkit.WebViewRenderProcessClient;

import org.chromium.support_lib_boundary.WebViewRendererClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Executor;

/**
 * Adapter class to pass a {@link WebViewRenderProcessClient} over to chromium.
 */
public class WebViewRenderProcessClientAdapter implements WebViewRendererClientBoundaryInterface {
    private static final String[] sSupportedFeatures = new String[] {
            Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
    };

    private final Executor mExecutor;
    private final WebViewRenderProcessClient mWebViewRenderProcessClient;

    // WebViewRenderProcessClient is a callback class, so it should be last. See
    // https://issuetracker.google.com/issues/139770271.
    @SuppressLint("LambdaLast")
    public WebViewRenderProcessClientAdapter(
            @Nullable Executor executor,
            @Nullable WebViewRenderProcessClient webViewRenderProcessClient) {
        mExecutor = executor;
        mWebViewRenderProcessClient = webViewRenderProcessClient;
    }

    @Nullable
    public WebViewRenderProcessClient getWebViewRenderProcessClient() {
        return mWebViewRenderProcessClient;
    }

    /**
     * Returns the list of features this client supports. This feature list should always be a
     * subset of the Features declared in WebViewFeature.
     */
    @Override
    @NonNull
    public final String[] getSupportedFeatures() {
        return sSupportedFeatures;
    }

    /**
     * Invoked by chromium with arguments that need to be wrapped by support library adapter
     * objects.
     */
    @Override
    public final void onRendererUnresponsive(
            @NonNull final WebView view,
            @NonNull /* WebViewRenderer */ final InvocationHandler renderer) {
        final WebViewRenderProcess rendererObject =
                WebViewRenderProcessImpl.forInvocationHandler(renderer);
        final WebViewRenderProcessClient client = mWebViewRenderProcessClient;
        if (mExecutor == null) {
            client.onRenderProcessUnresponsive(view, rendererObject);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    client.onRenderProcessUnresponsive(view, rendererObject);
                }
            });
        }
    }

    /**
     * Invoked by chromium with arguments that need to be wrapped by support library adapter
     * objects.
     */
    @Override
    public final void onRendererResponsive(
            @NonNull final WebView view,
            @NonNull /* WebViewRenderer */ InvocationHandler renderer) {
        final WebViewRenderProcess rendererObject =
                WebViewRenderProcessImpl.forInvocationHandler(renderer);
        final WebViewRenderProcessClient client = mWebViewRenderProcessClient;
        if (mExecutor == null) {
            client.onRenderProcessResponsive(view, rendererObject);
        } else {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    client.onRenderProcessResponsive(view, rendererObject);
                }
            });
        }
    }
}
