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

import android.webkit.TracingController;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.chromium.support_lib_boundary.DropDataContentProviderBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.chromium.support_lib_boundary.ProxyControllerBoundaryInterface;
import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.StaticsBoundaryInterface;
import org.chromium.support_lib_boundary.TracingControllerBoundaryInterface;
import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;
import org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface;

/**
 * Interface representing {@link android.webkit.WebViewFactoryProvider}.
 * On device with a compatible WebView APK this interface is implemented by a class defined in the
 * WebView APK itself.
 * On devices without a compatible WebView APK this interface is implemented by a stub class
 * {@link androidx.webkit.internal.IncompatibleApkWebViewProviderFactory}.
 */
@SuppressWarnings("JavadocReference") // WebViewFactoryProvider and WebViewProvider are hidden.
public interface WebViewProviderFactory {
    /**
     * Create a support library version of {@link android.webkit.WebViewProvider}.
     */
    @NonNull
    WebViewProviderBoundaryInterface createWebView(@NonNull WebView webview);

    /**
     * Create the boundary interface for {@link WebkitToCompatConverter}
     * which converts android.webkit classes into their corresponding support library classes.
     */
    @NonNull
    WebkitToCompatConverterBoundaryInterface getWebkitToCompatConverter();

    /**
     * Fetch the boundary interface representing
     * {@link android.webkit.WebViewFactoryProvider#Statics}.
     */
    @NonNull
    StaticsBoundaryInterface getStatics();

    /**
     * Fetch the features supported by the current WebView APK.
     */
    @NonNull
    String[] getWebViewFeatures();

    /**
     * Fetch the boundary interface representing {@link android.webkit.ServiceWorkerController}.
     */
    @NonNull
    ServiceWorkerControllerBoundaryInterface getServiceWorkerController();

    /**
     * Fetch the boundary interface representing {@link TracingController}.
     */
    @NonNull
    TracingControllerBoundaryInterface getTracingController();

    /**
     * Fetch the boundary interface representing {@link android.webkit.ProxyController}.
     */
    @NonNull
    ProxyControllerBoundaryInterface getProxyController();

    /**
     * Fetch the boundary interface representing image drag drop implementation.
     */
    @NonNull
    DropDataContentProviderBoundaryInterface getDropDataProvider();

    /**
     * Fetch the boundary interface representing profile store for Multi-Profile.
     */
    @NonNull
    ProfileStoreBoundaryInterface getProfileStore();


}
