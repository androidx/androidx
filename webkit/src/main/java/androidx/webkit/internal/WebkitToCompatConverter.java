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

import android.webkit.ServiceWorkerWebSettings;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;

import androidx.annotation.RequiresApi;
import androidx.webkit.WebResourceErrorCompat;

import org.chromium.support_lib_boundary.ServiceWorkerWebSettingsBoundaryInterface;
import org.chromium.support_lib_boundary.WebResourceRequestBoundaryInterface;
import org.chromium.support_lib_boundary.WebSettingsBoundaryInterface;
import org.chromium.support_lib_boundary.WebkitToCompatConverterBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;

/**
 * A class providing functionality for converting android.webkit classes into support library
 * classes.
 */
public class WebkitToCompatConverter {
    private final WebkitToCompatConverterBoundaryInterface mImpl;

    public WebkitToCompatConverter(WebkitToCompatConverterBoundaryInterface impl) {
        mImpl = impl;
    }

    /**
     * Return a WebSettingsAdapter linked to webSettings such that calls on either of those
     * objects affect the other object. That WebSettingsAdapter can be used to implement
     * {@link androidx.webkit.WebSettingsCompat}.
     */
    public WebSettingsAdapter convertSettings(WebSettings webSettings) {
        return new WebSettingsAdapter(BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebSettingsBoundaryInterface.class, mImpl.convertSettings(webSettings)));
    }

    /**
     * Return a {@link WebResourceRequestAdapter} linked to the given {@link WebResourceRequest} so
     * that calls on either of those objects affect the other object.
     */
    public WebResourceRequestAdapter convertWebResourceRequest(WebResourceRequest request) {
        return new WebResourceRequestAdapter(BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                WebResourceRequestBoundaryInterface.class,
                mImpl.convertWebResourceRequest(request)));
    }

    /**
     * Return a {@link ServiceWorkerWebSettingsBoundaryInterface} linked to the given
     * {@link ServiceWorkerWebSettings} such that calls on either of those objects affect the other
     * object.
     */
    public InvocationHandler convertServiceWorkerSettings(
            ServiceWorkerWebSettings settings) {
        return mImpl.convertServiceWorkerSettings(settings);
    }

    /**
     * Convert from an {@link InvocationHandler} representing an
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat} into a
     * {@link ServiceWorkerWebSettings}.
     */
    @RequiresApi(24)
    public ServiceWorkerWebSettings convertServiceWorkerSettings(
            /* SupportLibServiceWorkerSettings */ InvocationHandler serviceWorkerSettings) {
        return (ServiceWorkerWebSettings) mImpl.convertServiceWorkerSettings(serviceWorkerSettings);
    }

    /**
     * Return a {@link InvocationHandler} linked to the given
     * {@link WebResourceError}such that calls on either of those objects affect the other
     * object.
     */
    InvocationHandler convertWebResourceError(WebResourceError webResourceError) {
        return mImpl.convertWebResourceError(webResourceError);
    }


    /**
     * Convert from an {@link InvocationHandler} representing a {@link WebResourceErrorCompat} into
     * a {@link WebResourceError}.
     */
    @RequiresApi(23)
    WebResourceError convertWebResourceError(
            /* SupportLibWebResourceError */ InvocationHandler webResourceError) {
        return (WebResourceError) mImpl.convertWebResourceError(webResourceError);
    }
}
