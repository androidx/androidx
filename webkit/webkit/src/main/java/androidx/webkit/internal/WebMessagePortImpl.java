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

import android.os.Handler;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;

import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;

import org.chromium.support_lib_boundary.WebMessagePortBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Implementation of {@link WebMessagePortCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link WebMessagePortCompat} functionality.
 */
public class WebMessagePortImpl extends WebMessagePortCompat {
    private WebMessagePort mFrameworksImpl;
    private WebMessagePortBoundaryInterface mBoundaryInterface;

    public WebMessagePortImpl(@NonNull WebMessagePort frameworksImpl) {
        mFrameworksImpl = frameworksImpl;
    }

    public WebMessagePortImpl(@NonNull InvocationHandler invocationHandler) {
        mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebMessagePortBoundaryInterface.class, invocationHandler);
    }

    private WebMessagePort getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl = WebViewGlueCommunicator.getCompatConverter().convertWebMessagePort(
                    Proxy.getInvocationHandler(mBoundaryInterface));
        }
        return mFrameworksImpl;
    }

    private WebMessagePortBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    WebMessagePortBoundaryInterface.class,
                    WebViewGlueCommunicator.getCompatConverter().convertWebMessagePort(
                            mFrameworksImpl));
        }
        return mBoundaryInterface;
    }

    @Override
    public void postMessage(@NonNull WebMessageCompat message) {
        final ApiFeature.M feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_POST_MESSAGE;
        // Only String type is supported by framework.
        if (message.getType() == WebMessageCompat.TYPE_STRING) {
            getFrameworksImpl().postMessage(compatToFrameworkMessage(message));
        } else if (feature.isSupportedByWebView()
                && WebMessageAdapter.isMessagePayloadTypeSupportedByWebView(message.getType())) {
            getBoundaryInterface().postMessage(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageAdapter(message)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void close() {
        getFrameworksImpl().close();
    }

    @Override
    public void setWebMessageCallback(final @NonNull WebMessageCallbackCompat callback) {
        final ApiFeature.M feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK;
        if (feature.isSupportedByWebView()) {
            // We prefer use WebView impl, since the impl in framework does not support
            // WebMessageCompat types other than String.
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)));
        } else {
            getFrameworksImpl().setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            WebMessagePortImpl.frameworkMessageToCompat(message));
                }
            });
        }
    }

    @Override
    public void setWebMessageCallback(@Nullable Handler handler,
            final @NonNull WebMessageCallbackCompat callback) {
        final ApiFeature.M feature = WebViewFeatureInternal.CREATE_WEB_MESSAGE_CHANNEL;
        if (feature.isSupportedByWebView()) {
            // We prefer use WebView impl, since the impl in framework does not support
            // WebMessageCompat types other than String.
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)), handler);
        } else {
            getFrameworksImpl().setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            WebMessagePortImpl.frameworkMessageToCompat(message));
                }
            }, handler);
        }
    }

    @Override
    public @NonNull WebMessagePort getFrameworkPort() {
        return getFrameworksImpl();
    }

    @Override
    public @NonNull InvocationHandler getInvocationHandler() {
        return Proxy.getInvocationHandler(getBoundaryInterface());
    }

    /**
     * Convert an array of {@link WebMessagePort} objects into an array containing objects of the
     * corresponding support library class {@link WebMessagePortCompat}.
     */
    public static WebMessagePortCompat @Nullable [] portsToCompat(
            WebMessagePort @Nullable [] ports) {
        if (ports == null) return null;
        WebMessagePortCompat[] compatPorts = new WebMessagePortCompat[ports.length];
        for (int n = 0; n < ports.length; n++) {
            compatPorts[n] = new WebMessagePortImpl(ports[n]);
        }
        return compatPorts;
    }

    /**
     * Convert an array of {@link WebMessagePortCompat} objects into an array containing objects of
     * the corresponding framework class {@link WebMessagePort}.
     */
    public static WebMessagePort @Nullable [] compatToPorts(
            WebMessagePortCompat @Nullable [] compatPorts) {
        if (compatPorts == null) return null;
        WebMessagePort[] ports = new WebMessagePort[compatPorts.length];
        for (int n = 0; n < ports.length; n++) {
            ports[n] = compatPorts[n].getFrameworkPort();
        }
        return ports;
    }

    /**
     * Convert a {@link WebMessageCompat} into the corresponding framework class {@link WebMessage}.
     */
    public static @NonNull WebMessage compatToFrameworkMessage(@NonNull WebMessageCompat message) {
        return new WebMessage(message.getData(),
                WebMessagePortImpl.compatToPorts(message.getPorts()));
    }

    /**
     * Convert a {@link WebMessage} into the corresponding support library class
     * {@link WebMessageCompat}.
     */
    public static @NonNull WebMessageCompat frameworkMessageToCompat(@NonNull WebMessage message) {
        return new WebMessageCompat(message.getData(),
                WebMessagePortImpl.portsToCompat(message.getPorts()));
    }
}
