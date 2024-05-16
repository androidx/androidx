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

import androidx.annotation.NonNull;
import androidx.webkit.JavaScriptReplyProxy;

import org.chromium.support_lib_boundary.JsReplyProxyBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * Internal implementation of {@link androidx.webkit.JavaScriptReplyProxy}.
 */
public class JavaScriptReplyProxyImpl extends JavaScriptReplyProxy {
    private final JsReplyProxyBoundaryInterface mBoundaryInterface;

    public JavaScriptReplyProxyImpl(@NonNull JsReplyProxyBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Get a support library JavaScriptReplyProxy object that is 1:1 with the AndroidX side object.
     */
    @NonNull
    public static JavaScriptReplyProxyImpl forInvocationHandler(
            @NonNull /* JsReplyProxy */ InvocationHandler invocationHandler) {
        final JsReplyProxyBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        JsReplyProxyBoundaryInterface.class, invocationHandler);
        return (JavaScriptReplyProxyImpl) boundaryInterface.getOrCreatePeer(
                () -> new JavaScriptReplyProxyImpl(boundaryInterface));
    }

    @Override
    public void postMessage(@NonNull final String message) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.WEB_MESSAGE_LISTENER;
        if (feature.isSupportedByWebView()) {
            mBoundaryInterface.postMessage(message);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void postMessage(@NonNull byte[] arrayBuffer) {
        // WebView cannot handle null ArrayBuffer as WebMessage.
        Objects.requireNonNull(arrayBuffer, "ArrayBuffer must be non-null");
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.WEB_MESSAGE_ARRAY_BUFFER;
        if (feature.isSupportedByWebView()) {
            mBoundaryInterface.postMessageWithPayload(BoundaryInterfaceReflectionUtil
                    .createInvocationHandlerFor(new WebMessagePayloadAdapter(arrayBuffer)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
