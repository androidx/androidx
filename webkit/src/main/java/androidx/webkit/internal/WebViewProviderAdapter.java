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

import android.net.Uri;
import android.webkit.WebView;

import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;

import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;

/**
 * Adapter for WebViewProviderBoundaryInterface providing the functionality expected of
 * WebViewCompat, this adapter is the support library version of
 * {@link android.webkit.WebViewProvider}.
 */
public class WebViewProviderAdapter {
    WebViewProviderBoundaryInterface mImpl;

    public WebViewProviderAdapter(WebViewProviderBoundaryInterface impl) {
        mImpl = impl;
    }

    /**
     * Adapter method WebViewCompat.insertVisualStateCallback().
     */
    public void insertVisualStateCallback(long requestId,
            WebViewCompat.VisualStateCallback callback) {
        mImpl.insertVisualStateCallback(requestId,
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new VisualStateCallbackAdapter(callback)));
    }

    /**
     * Adapter method for {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
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
    public void postWebMessage(WebMessageCompat message, Uri targetOrigin) {
        mImpl.postMessageToMainFrame(
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebMessageAdapter(message)), targetOrigin);
    }
}
