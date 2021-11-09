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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;

import org.chromium.support_lib_boundary.WebMessagePortBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

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

    public WebMessagePortImpl(WebMessagePort frameworksImpl) {
        mFrameworksImpl = frameworksImpl;
    }

    public WebMessagePortImpl(InvocationHandler invocationHandler) {
        mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebMessagePortBoundaryInterface.class, invocationHandler);
    }

    @RequiresApi(23)
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
        final WebViewFeatureInternal feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_POST_MESSAGE;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().postMessage(compatToFrameworkMessage(message));
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().postMessage(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageAdapter(message)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void close() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_CLOSE;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().close();
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().close();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(@NonNull final WebMessageCallbackCompat callback) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            frameworkMessageToCompat(message));
                }
            });
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(Handler handler,
            @NonNull final WebMessageCallbackCompat callback) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.CREATE_WEB_MESSAGE_CHANNEL;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
                @Override
                public void onMessage(WebMessagePort port, WebMessage message) {
                    callback.onMessage(new WebMessagePortImpl(port),
                            frameworkMessageToCompat(message));
                }
            }, handler);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)), handler);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @RequiresApi(23)
    @Override
    public WebMessagePort getFrameworkPort() {
        return getFrameworksImpl();
    }

    @Override
    public InvocationHandler getInvocationHandler() {
        return Proxy.getInvocationHandler(getBoundaryInterface());
    }

    /**
     * Convert an array of {@link WebMessagePort} objects into an array containing objects of the
     * corresponding support library class {@link WebMessagePortCompat}.
     */
    @Nullable
    public static WebMessagePortCompat[] portsToCompat(WebMessagePort[] ports) {
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
    @RequiresApi(23)
    @Nullable
    public static WebMessagePort[] compatToPorts(WebMessagePortCompat[] compatPorts) {
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
    @RequiresApi(23)
    @NonNull
    public static WebMessage compatToFrameworkMessage(WebMessageCompat message) {
        return new WebMessage(
                message.getData(),
                compatToPorts(message.getPorts()));
    }

    /**
     * Convert a {@link WebMessage} into the corresponding support library class
     * {@link WebMessageCompat}.
     */
    @RequiresApi(23)
    @NonNull
    public static WebMessageCompat frameworkMessageToCompat(WebMessage message) {
        return new WebMessageCompat(
                message.getData(),
                portsToCompat(message.getPorts()));
    }
}
