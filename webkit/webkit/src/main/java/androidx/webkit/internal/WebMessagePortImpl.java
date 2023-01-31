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

    public WebMessagePortImpl(@NonNull WebMessagePort frameworksImpl) {
        mFrameworksImpl = frameworksImpl;
    }

    public WebMessagePortImpl(@NonNull InvocationHandler invocationHandler) {
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
        final ApiFeature.M feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_POST_MESSAGE;
        // Only String type is supported by framework.
        if (feature.isSupportedByFramework() && message.getType() == WebMessageCompat.TYPE_STRING) {
            ApiHelperForM.postMessage(getFrameworksImpl(), compatToFrameworkMessage(message));
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
        final ApiFeature.M feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_CLOSE;
        if (feature.isSupportedByFramework()) {
            ApiHelperForM.close(getFrameworksImpl());
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().close();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(@NonNull final WebMessageCallbackCompat callback) {
        final ApiFeature.M feature = WebViewFeatureInternal.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK;
        if (feature.isSupportedByWebView()) {
            // We prefer use WebView impl, since the impl in framework does not support
            // WebMessageCompat types other than String.
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)));
        } else if (feature.isSupportedByFramework()) {
            ApiHelperForM.setWebMessageCallback(getFrameworksImpl(), callback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setWebMessageCallback(@Nullable Handler handler,
            @NonNull final WebMessageCallbackCompat callback) {
        final ApiFeature.M feature = WebViewFeatureInternal.CREATE_WEB_MESSAGE_CHANNEL;
        if (feature.isSupportedByWebView()) {
            // We prefer use WebView impl, since the impl in framework does not support
            // WebMessageCompat types other than String.
            getBoundaryInterface().setWebMessageCallback(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new WebMessageCallbackAdapter(callback)), handler);
        } else if (feature.isSupportedByFramework()) {
            ApiHelperForM.setWebMessageCallback(getFrameworksImpl(), callback, handler);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @NonNull
    @RequiresApi(23)
    @Override
    public WebMessagePort getFrameworkPort() {
        return getFrameworksImpl();
    }

    @NonNull
    @Override
    public InvocationHandler getInvocationHandler() {
        return Proxy.getInvocationHandler(getBoundaryInterface());
    }

    /**
     * Convert an array of {@link WebMessagePort} objects into an array containing objects of the
     * corresponding support library class {@link WebMessagePortCompat}.
     */
    @Nullable
    public static WebMessagePortCompat[] portsToCompat(@Nullable WebMessagePort[] ports) {
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
    public static WebMessagePort[] compatToPorts(@Nullable WebMessagePortCompat[] compatPorts) {
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
    public static WebMessage compatToFrameworkMessage(@NonNull WebMessageCompat message) {
        return ApiHelperForM.createWebMessage(message);
    }

    /**
     * Convert a {@link WebMessage} into the corresponding support library class
     * {@link WebMessageCompat}.
     */
    @RequiresApi(23)
    @NonNull
    public static WebMessageCompat frameworkMessageToCompat(@NonNull WebMessage message) {
        return ApiHelperForM.createWebMessageCompat(message);
    }
}
