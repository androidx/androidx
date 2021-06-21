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

package androidx.webkit.internal;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewRenderProcess;
import androidx.webkit.WebViewRenderProcessClient;

import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Executor;

/**
 * Adapter for WebViewProviderBoundaryInterface providing the functionality expected of
 * WebViewCompat, this adapter is the support library version of
 * {@link android.webkit.WebViewProvider}.
 */
@SuppressWarnings("JavadocReference") // WebViewProvider is hidden.
public class WebViewProviderAdapter {
    WebViewProviderBoundaryInterface mImpl;

    public WebViewProviderAdapter(@NonNull WebViewProviderBoundaryInterface impl) {
        mImpl = impl;
    }

    /**
     * Adapter method WebViewCompat.insertVisualStateCallback().
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public void insertVisualStateCallback(
            long requestId, @NonNull WebViewCompat.VisualStateCallback callback) {
        mImpl.insertVisualStateCallback(requestId,
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new VisualStateCallbackAdapter(callback)));
    }

    /**
     * Adapter method for {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
    @NonNull
    public WebMessagePortCompat[] createWebMessageChannel() {
        InvocationHandler[] invocationHandlers = mImpl.createWebMessageChannel();
        WebMessagePortCompat[] messagePorts = new WebMessagePortCompat[invocationHandlers.length];
        for (int n = 0; n < invocationHandlers.length; n++) {
            messagePorts[n] = new WebMessagePortImpl(invocationHandlers[n]);
        }
        return messagePorts;
    }

    /**
     * Adapter method for {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public void postWebMessage(@NonNull WebMessageCompat message, @NonNull Uri targetOrigin) {
        mImpl.postMessageToMainFrame(
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebMessageAdapter(message)), targetOrigin);
    }

    /**
     * Adapter method for {@link WebViewCompat#addWebMessageListener(android.webkit.WebView,
     * String, List<String>, androidx.webkit.WebViewCompat.WebMessageListener)}.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public void addWebMessageListener(@NonNull String jsObjectName,
            @NonNull String[] allowedOriginRules,
            @NonNull WebViewCompat.WebMessageListener listener) {
        mImpl.addWebMessageListener(jsObjectName, allowedOriginRules,
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebMessageListenerAdapter(listener)));
    }

    /**
     * Adapter method for {@link WebViewCompat#addWebMessageListener(android.webkit.WebView,
     * String, Set)}
     */
    public @NonNull ScriptHandlerImpl addDocumentStartJavaScript(
            @NonNull String script, @NonNull String[] allowedOriginRules) {
        return ScriptHandlerImpl.toScriptHandler(
                mImpl.addDocumentStartJavaScript(script, allowedOriginRules));
    }

    /**
     * Adapter method for {@link WebViewCompat#removeWebMessageListener(String)}.
     */
    public void removeWebMessageListener(@NonNull String jsObjectName) {
        mImpl.removeWebMessageListener(jsObjectName);
    }

    /**
     * Adapter method for {@link WebViewCompat#getWebViewClient()}.
     */
    @NonNull
    public WebViewClient getWebViewClient() {
        return mImpl.getWebViewClient();
    }

    /**
     * Adapter method for {@link WebViewCompat#getWebChromeClient()}.
     */
    @Nullable
    public WebChromeClient getWebChromeClient() {
        return mImpl.getWebChromeClient();
    }

    /**
     * Adapter method for {@link WebViewCompat#getWebViewRenderer()}.
     */
    @Nullable
    public WebViewRenderProcess getWebViewRenderProcess() {
        return WebViewRenderProcessImpl.forInvocationHandler(mImpl.getWebViewRenderer());
    }

    /**
     * Adapter method for {@link WebViewCompat#getWebViewRendererClient()}.
     */
    @RequiresApi(19)
    @Nullable
    public WebViewRenderProcessClient getWebViewRenderProcessClient() {
        InvocationHandler handler = mImpl.getWebViewRendererClient();
        if (handler == null) return null;
        return ((WebViewRenderProcessClientAdapter)
                BoundaryInterfaceReflectionUtil.getDelegateFromInvocationHandler(
                        handler)).getWebViewRenderProcessClient();
    }

    /**
     * Adapter method for {@link WebViewCompat#setWebViewRendererClient(WebViewRendererClient)}.
     */
    // WebViewRenderProcessClient is a callback class, so it should be last. See
    // https://issuetracker.google.com/issues/139770271.
    @SuppressLint("LambdaLast")
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public void setWebViewRenderProcessClient(@Nullable Executor executor,
            @Nullable WebViewRenderProcessClient webViewRenderProcessClient) {
        InvocationHandler handler = webViewRenderProcessClient != null
                ? BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebViewRenderProcessClientAdapter(executor, webViewRenderProcessClient))
                : null;
        mImpl.setWebViewRendererClient(handler);
    }
}
