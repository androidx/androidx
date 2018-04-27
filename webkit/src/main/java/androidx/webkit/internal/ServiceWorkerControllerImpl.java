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
import android.webkit.ServiceWorkerController;

import androidx.annotation.RequiresApi;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.ServiceWorkerControllerCompat;
import androidx.webkit.ServiceWorkerWebSettingsCompat;

import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

/**
 * Implementation of {@link ServiceWorkerControllerCompat}.
 * This class uses either the framework, the WebView APK, or both, to implement
 * {@link ServiceWorkerControllerCompat} functionality.
 */
public class ServiceWorkerControllerImpl extends ServiceWorkerControllerCompat {
    private ServiceWorkerController mFrameworksImpl;
    private ServiceWorkerControllerBoundaryInterface mBoundaryInterface;
    private final ServiceWorkerWebSettingsCompat mWebSettings;

    @SuppressLint("NewApi")
    public ServiceWorkerControllerImpl() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            mFrameworksImpl = ServiceWorkerController.getInstance();
            // The current WebView APK might not be compatible with the support library, so set the
            // boundary interface to null for now.
            mBoundaryInterface = null;
            mWebSettings = new ServiceWorkerWebSettingsImpl(
                    mFrameworksImpl.getServiceWorkerWebSettings());
        } else if (feature.isSupportedByWebView()) {
            mFrameworksImpl = null;
            mBoundaryInterface = WebViewGlueCommunicator.getFactory().getServiceWorkerController();
            mWebSettings = new ServiceWorkerWebSettingsImpl(
                    mBoundaryInterface.getServiceWorkerWebSettings());
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @RequiresApi(24)
    private ServiceWorkerController getFrameworksImpl() {
        if (mFrameworksImpl == null) {
            mFrameworksImpl = ServiceWorkerController.getInstance();
        }
        return mFrameworksImpl;
    }

    private ServiceWorkerControllerBoundaryInterface getBoundaryInterface() {
        if (mBoundaryInterface == null) {
            mBoundaryInterface = WebViewGlueCommunicator.getFactory().getServiceWorkerController();
        }
        return mBoundaryInterface;
    }

    @Override
    public ServiceWorkerWebSettingsCompat getServiceWorkerWebSettings() {
        return mWebSettings;
    }

    @SuppressLint("NewApi")
    @Override
    public void setServiceWorkerClient(ServiceWorkerClientCompat client)  {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.SERVICE_WORKER_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            getFrameworksImpl().setServiceWorkerClient(new FrameworkServiceWorkerClient(client));
        } else if (feature.isSupportedByWebView()) {
            getBoundaryInterface().setServiceWorkerClient(
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new ServiceWorkerClientAdapter(client)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
