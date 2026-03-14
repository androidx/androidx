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

import androidx.webkit.JavaScriptExecutionException;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebViewOutcomeReceiver;

import org.chromium.support_lib_boundary.ExecuteJavaScriptCallbackBoundaryInterface;
import org.chromium.support_lib_boundary.ExecuteJavaScriptCallbackBoundaryInterface.ExecuteJavaScriptExceptionTypeBoundaryInterface;
import org.chromium.support_lib_boundary.JsReplyProxyBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    public static @NonNull JavaScriptReplyProxyImpl forInvocationHandler(
            /* JsReplyProxy */ @NonNull InvocationHandler invocationHandler) {
        final JsReplyProxyBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        JsReplyProxyBoundaryInterface.class, invocationHandler);
        return (JavaScriptReplyProxyImpl) boundaryInterface.getOrCreatePeer(
                () -> new JavaScriptReplyProxyImpl(boundaryInterface));
    }

    @Override
    public void postMessage(final @NonNull String message) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.WEB_MESSAGE_LISTENER;
        if (feature.isSupportedByWebView()) {
            mBoundaryInterface.postMessage(message);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void postMessage(byte @NonNull [] arrayBuffer) {
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

    @Override
    public void executeJavaScript(
            @NonNull String script,
            final @Nullable WebViewOutcomeReceiver<String, JavaScriptExecutionException> receiver) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.JS_INJECTION_IN_FRAME_AND_WORLD;
        if (feature.isSupportedByWebView()) {
            mBoundaryInterface.executeJavaScript(
                    script,
                    receiver == null ? null :
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new ExecuteJavaScriptCallbackBoundaryInterface() {
                                @Override
                                public void onSuccess(@NonNull String result) {
                                    receiver.onResult(result);
                                }

                                @Override
                                public void onFailure(
                                        @ExecuteJavaScriptExceptionTypeBoundaryInterface int type,
                                        @Nullable String message) {
                                    receiver.onError(
                                            new JavaScriptExecutionException(toErrorType(type),
                                                    message));
                                }
                            }));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private @JavaScriptExecutionException.ErrorType int toErrorType(
            @ExecuteJavaScriptExceptionTypeBoundaryInterface int type) {
        switch (type) {
            case ExecuteJavaScriptExceptionTypeBoundaryInterface.GENERIC:
                return JavaScriptExecutionException.ERROR_GENERIC;
            case ExecuteJavaScriptExceptionTypeBoundaryInterface.FRAME_DESTROYED:
                return JavaScriptExecutionException.ERROR_FRAME_DESTROYED;
        }
        return JavaScriptExecutionException.ERROR_GENERIC;
    }
}
