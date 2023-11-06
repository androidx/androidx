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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

/**
 * This class represents the JavaScript object injected by {@link
 * WebViewCompat#addWebMessageListener(android.webkit.WebView, String, Set,
 * WebViewCompat.WebMessageListener) WebViewCompat#addWebMessageListener}. An instance will be given
 * by {@link WebViewCompat.WebMessageListener#onPostMessage(android.webkit.WebView,
 * WebMessageCompat, android.net.Uri, boolean, JavaScriptReplyProxy)
 * WebMessageListener#onPostMessage}. The app can use {@link #postMessage(String)} to talk to the
 * JavaScript context.
 *
 * <p>
 * There is a 1:1 relationship between this object and the JavaScript object in a frame.
 *
 * @see WebViewCompat#addWebMessageListener(android.webkit.WebView, String, Set,
 * WebViewCompat.WebMessageListener).
 */
public abstract class JavaScriptReplyProxy {
    /**
     * Post a String message to the injected JavaScript object which sent this {@link
     * JavaScriptReplyProxy}.
     *
     * @param message The String data to send to the JavaScript context.
     */
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void postMessage(@NonNull String message);

    /**
     * Post a ArrayBuffer message to the injected JavaScript object which sent this
     * {@link JavaScriptReplyProxy}. Be aware that large byte buffers can lead to out-of-memory
     * crashes on low-end devices.
     *
     * @param arrayBuffer The ArrayBuffer to send to the JavaScript context. An empty ArrayBuffer
     *                    is supported.
     */
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void postMessage(@NonNull byte[] arrayBuffer);

    /**
     * This class cannot be created by applications.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public JavaScriptReplyProxy() {}
}
