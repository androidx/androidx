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

import android.webkit.WebView;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewStartUpConfig;

import org.chromium.support_lib_boundary.DropDataContentProviderBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.chromium.support_lib_boundary.ProxyControllerBoundaryInterface;
import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.StaticsBoundaryInterface;
import org.chromium.support_lib_boundary.TracingControllerBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewBuilderBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;
import org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

/**
 * Adapter for WebViewProviderFactoryBoundaryInterface providing static WebView functionality
 * similar to that provided by {@link android.webkit.WebViewFactoryProvider}.
 */
@SuppressWarnings("JavadocReference") // WebViewFactoryProvider and WebViewProvider are hidden.
public class WebViewProviderFactoryAdapter implements WebViewProviderFactory {
    final WebViewProviderFactoryBoundaryInterface mImpl;

    public WebViewProviderFactoryAdapter(@NonNull WebViewProviderFactoryBoundaryInterface impl) {
        mImpl = impl;
    }

    /**
     * Adapter method for fetching the support library class representing the WebViewBuilder
     * implementation details.
     */
    @Override
    public @NonNull WebViewBuilderBoundaryInterface getWebViewBuilder() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebViewBuilderBoundaryInterface.class, mImpl.getWebViewBuilder());
    }

    /**
     * Adapter method for creating a new support library version of {@link
     * android.webkit.WebViewProvider} - the class used to implement {@link
     * androidx.webkit.WebViewCompat}.
     */
    @Override
    public @NonNull WebViewProviderBoundaryInterface createWebView(@NonNull WebView webview) {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebViewProviderBoundaryInterface.class, mImpl.createWebView(webview));
    }

    /**
     * Adapter method for creating a new support library version of {@link
     * androidx.webkit.internal.WebkitToCompatConverter}, which converts android.webkit classes into
     * their corresponding support library classes.
     */
    @Override
    public @NonNull WebkitToCompatConverterBoundaryInterface getWebkitToCompatConverter() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebkitToCompatConverterBoundaryInterface.class, mImpl.getWebkitToCompatConverter());
    }

    /**
     * Adapter method for fetching the support library class representing {@link
     * android.webkit.WebViewFactoryProvider#Statics}.
     */
    @Override
    public @NonNull StaticsBoundaryInterface getStatics() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                StaticsBoundaryInterface.class, mImpl.getStatics());
    }

    /** Adapter method for fetching the features supported by the current WebView APK. */
    @Override
    public String @NonNull [] getWebViewFeatures() {
        return mImpl.getSupportedFeatures();
    }

    /**
     * Adapter method for fetching the support library class representing {@link
     * android.webkit.ServiceWorkerController}.
     */
    @Override
    public @NonNull ServiceWorkerControllerBoundaryInterface getServiceWorkerController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ServiceWorkerControllerBoundaryInterface.class, mImpl.getServiceWorkerController());
    }

    /**
     * Adapter method for fetching the support library class representing {@link
     * android.webkit.TracingController}.
     */
    @Override
    public @NonNull TracingControllerBoundaryInterface getTracingController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                TracingControllerBoundaryInterface.class, mImpl.getTracingController());
    }

    /**
     * Adapter method for fetching the support library class representing {@link
     * android.webkit.ProxyController}.
     */
    @Override
    public @NonNull ProxyControllerBoundaryInterface getProxyController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ProxyControllerBoundaryInterface.class, mImpl.getProxyController());
    }

    /**
     * Adapter method for fetching the support library class representing Drag drop Image
     * implementation.
     */
    @Override
    public @NonNull DropDataContentProviderBoundaryInterface getDropDataProvider() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                DropDataContentProviderBoundaryInterface.class, mImpl.getDropDataProvider());
    }

    @Override
    public @NonNull ProfileStoreBoundaryInterface getProfileStore() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ProfileStoreBoundaryInterface.class, mImpl.getProfileStore());
    }

    @WebViewCompat.ExperimentalAsyncStartUp
    @Override
    public void startUpWebView(
            @NonNull WebViewStartUpConfig config,
            WebViewCompat.@NonNull WebViewStartUpCallback callback) {
        mImpl.startUpWebView(
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebViewStartUpConfigAdapter(config)),
                BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                        new WebViewStartUpCallbackAdapter(callback)));
    }
}
