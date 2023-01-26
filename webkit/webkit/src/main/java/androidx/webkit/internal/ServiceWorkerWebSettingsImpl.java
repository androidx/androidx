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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.ServiceWorkerWebSettingsCompat;

import org.chromium.support_lib_boundary.ServiceWorkerWebSettingsBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Implementation of {@link ServiceWorkerWebSettingsCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link ServiceWorkerWebSettingsCompat} functionality.
 */
public class ServiceWorkerWebSettingsImpl extends ServiceWorkerWebSettingsCompat {
    private ServiceWorkerWebSettings mFrameworksImpl;
    private ServiceWorkerWebSettingsBoundaryInterface mBoundaryInterface;

    /**
     * This class handles three different scenarios:
     * 1. The Android version on the device is high enough to support all APIs used.
     * 2. The Android version on the device is too low to support any ServiceWorkerWebSettings APIs
     * so we use the support library glue instead through
     * {@link ServiceWorkerWebSettingsBoundaryInterface}.
     * 3. The Android version on the device is high enough to support some ServiceWorkerWebSettings
     * APIs, so we call into them using {@link android.webkit.ServiceWorkerWebSettings}, but the
     * rest of the APIs are only supported by the support library glue, so whenever we call such an
     * API we fetch a {@link ServiceWorkerWebSettingsBoundaryInterface} corresponding to our
     * {@link android.webkit.ServiceWorkerWebSettings}.
     */
    public ServiceWorkerWebSettingsImpl(@NonNull ServiceWorkerWebSettings settings) {
        mFrameworksImpl = settings;
    }

    public ServiceWorkerWebSettingsImpl(@NonNull InvocationHandler invocationHandler) {
        mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ServiceWorkerWebSettingsBoundaryInterface.class, invocationHandler);
    }

    @RequiresApi(24)
    private ServiceWorkerWebSettings getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl =
                    WebViewGlueCommunicator.getCompatConverter().convertServiceWorkerSettings(
                            Proxy.getInvocationHandler(mBoundaryInterface));
        }
        return mFrameworksImpl;
    }

    private ServiceWorkerWebSettingsBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            // If the boundary interface is null we must have a working frameworks implementation to
            // convert into a boundary interface.
            // The case of the boundary interface being null here only occurs if we created the
            // ServiceWorkerWebSettingsImpl using a frameworks API, but now want to call an API on
            // the ServiceWorkerWebSettingsImpl that is only supported by the support library glue.
            // This could happen for example if we introduce a new ServiceWorkerWebSettings API in
            // level 30 and we run the support library on an N device (whose framework supports
            // ServiceWorkerWebSettings).
            mBoundaryInterface = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    ServiceWorkerWebSettingsBoundaryInterface.class,
                    WebViewGlueCommunicator.getCompatConverter().convertServiceWorkerSettings(
                            mFrameworksImpl));
        }
        return mBoundaryInterface;
    }

    @Override
    public void setCacheMode(int mode) {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_CACHE_MODE;
        if (feature.isSupportedByFramework()) {
            ApiHelperForN.setCacheMode(getFrameworksImpl(), mode);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setCacheMode(mode);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public int getCacheMode() {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_CACHE_MODE;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForN.getCacheMode(getFrameworksImpl());
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getCacheMode();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setAllowContentAccess(boolean allow) {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_CONTENT_ACCESS;
        if (feature.isSupportedByFramework()) {
            ApiHelperForN.setAllowContentAccess(getFrameworksImpl(), allow);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setAllowContentAccess(allow);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public boolean getAllowContentAccess() {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_CONTENT_ACCESS;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForN.getAllowContentAccess(getFrameworksImpl());
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getAllowContentAccess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setAllowFileAccess(boolean allow) {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_FILE_ACCESS;
        if (feature.isSupportedByFramework()) {
            ApiHelperForN.setAllowFileAccess(getFrameworksImpl(), allow);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setAllowFileAccess(allow);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public boolean getAllowFileAccess() {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_FILE_ACCESS;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForN.getAllowFileAccess(getFrameworksImpl());
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getAllowFileAccess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setBlockNetworkLoads(boolean flag) {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_BLOCK_NETWORK_LOADS;
        if (feature.isSupportedByFramework()) {
            ApiHelperForN.setBlockNetworkLoads(getFrameworksImpl(), flag);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setBlockNetworkLoads(flag);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public boolean getBlockNetworkLoads() {
        final ApiFeature.N feature = WebViewFeatureInternal.SERVICE_WORKER_BLOCK_NETWORK_LOADS;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForN.getBlockNetworkLoads(getFrameworksImpl());
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getBlockNetworkLoads();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @NonNull
    @Override
    public Set<String> getRequestedWithHeaderOriginAllowList() {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.REQUESTED_WITH_HEADER_ALLOW_LIST;
        if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getRequestedWithHeaderOriginAllowList();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void setRequestedWithHeaderOriginAllowList(@NonNull Set<String> allowList) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.REQUESTED_WITH_HEADER_ALLOW_LIST;
        if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setRequestedWithHeaderOriginAllowList(allowList);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
