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

import androidx.annotation.NonNull;

import org.chromium.support_lib_boundary.DropDataContentProviderBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.chromium.support_lib_boundary.ProxyControllerBoundaryInterface;
import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.StaticsBoundaryInterface;
import org.chromium.support_lib_boundary.TracingControllerBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;
import org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

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
     * Adapter method for creating a new support library version of
     * {@link android.webkit.WebViewProvider} - the class used to implement
     * {@link androidx.webkit.WebViewCompat}.
     */
    @NonNull
    @Override
    public WebViewProviderBoundaryInterface createWebView(@NonNull WebView webview) {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebViewProviderBoundaryInterface.class, mImpl.createWebView(webview));
    }

    /**
     * Adapter method for creating a new support library version of
     * {@link androidx.webkit.internal.WebkitToCompatConverter}, which converts android.webkit
     * classes into their corresponding support library classes.
     */
    @NonNull
    @Override
    public WebkitToCompatConverterBoundaryInterface getWebkitToCompatConverter() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebkitToCompatConverterBoundaryInterface.class, mImpl.getWebkitToCompatConverter());
    }

    /**
     * Adapter method for fetching the support library class representing
     * {@link android.webkit.WebViewFactoryProvider#Statics}.
     */
    @NonNull
    @Override
    public StaticsBoundaryInterface getStatics() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                StaticsBoundaryInterface.class, mImpl.getStatics());
    }

    /**
     * Adapter method for fetching the features supported by the current WebView APK.
     */
    @NonNull
    @Override
    public String[] getWebViewFeatures() {
        return mImpl.getSupportedFeatures();
    }

    /**
     * Adapter method for fetching the support library class representing
     * {@link android.webkit.ServiceWorkerController}.
     */
    @Override
    @NonNull
    public ServiceWorkerControllerBoundaryInterface getServiceWorkerController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ServiceWorkerControllerBoundaryInterface.class, mImpl.getServiceWorkerController());
    }

    /**
     * Adapter method for fetching the support library class representing
     * {@link android.webkit.TracingController}.
     */
    @NonNull
    @Override
    public TracingControllerBoundaryInterface getTracingController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                TracingControllerBoundaryInterface.class, mImpl.getTracingController());
    }

    /**
     * Adapter method for fetching the support library class representing
     * {@link android.webkit.ProxyController}.
     */
    @NonNull
    @Override
    public ProxyControllerBoundaryInterface getProxyController() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ProxyControllerBoundaryInterface.class, mImpl.getProxyController());
    }

    /**
     * Adapter method for fetching the support library class representing Drag drop
     * Image implementation.
     */
    @NonNull
    @Override
    public DropDataContentProviderBoundaryInterface getDropDataProvider() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                DropDataContentProviderBoundaryInterface.class, mImpl.getDropDataProvider());
    }

    @NonNull
    @Override
    public ProfileStoreBoundaryInterface getProfileStore() {
        return BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ProfileStoreBoundaryInterface.class, mImpl.getProfileStore());
    }
}
