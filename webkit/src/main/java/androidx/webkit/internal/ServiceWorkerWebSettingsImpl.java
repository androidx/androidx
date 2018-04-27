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

import android.annotation.SuppressLint;
import android.webkit.ServiceWorkerWebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.webkit.ServiceWorkerWebSettingsCompat;

import org.chromium.support_lib_boundary.ServiceWorkerWebSettingsBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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

    @SuppressLint("NewApi")
    @Override
    public void setCacheMode(int mode) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_CACHE_MODE;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setCacheMode(mode);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setCacheMode(mode);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public int getCacheMode() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_CACHE_MODE;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().getCacheMode();
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getCacheMode();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void setAllowContentAccess(boolean allow) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_CONTENT_ACCESS;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setAllowContentAccess(allow);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setAllowContentAccess(allow);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean getAllowContentAccess() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_CONTENT_ACCESS;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().getAllowContentAccess();
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getAllowContentAccess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void setAllowFileAccess(boolean allow) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_FILE_ACCESS;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setAllowFileAccess(allow);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setAllowFileAccess(allow);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean getAllowFileAccess() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_FILE_ACCESS;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().getAllowFileAccess();
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getAllowFileAccess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void setBlockNetworkLoads(boolean flag) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.SERVICE_WORKER_BLOCK_NETWORK_LOADS;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setBlockNetworkLoads(flag);
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setBlockNetworkLoads(flag);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean getBlockNetworkLoads() {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.SERVICE_WORKER_BLOCK_NETWORK_LOADS;
        if (feature.isSupportedByFramework()) {
            return getFrameworksImpl().getBlockNetworkLoads();
        } else if (feature.isSupportedByWebView()) {
            return getBoundaryInterface().getBlockNetworkLoads();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
