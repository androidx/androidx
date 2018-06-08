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

import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.ServiceWorkerControllerCompat;
import androidx.webkit.ServiceWorkerWebSettingsCompat;

import org.chromium.support_lib_boundary.ServiceWorkerControllerBoundaryInterface;
import org.chromium.support_lib_boundary.ServiceWorkerWebSettingsBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

/**
 * Adapter between {@link ServiceWorkerControllerCompat} and
 * {@link ServiceWorkerControllerBoundaryInterface} (the corresponding interface shared with the
 * support library glue in the WebView APK).
 */
public class ServiceWorkerControllerAdapter extends ServiceWorkerControllerCompat {
    private final ServiceWorkerControllerBoundaryInterface mImpl;
    private final ServiceWorkerWebSettingsCompat mWebSettings;

    public ServiceWorkerControllerAdapter(ServiceWorkerControllerBoundaryInterface impl) {
        mImpl = impl;
        mWebSettings = new ServiceWorkerWebSettingsAdapter(
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        ServiceWorkerWebSettingsBoundaryInterface.class,
                        mImpl.getServiceWorkerWebSettings()));
    }

    @Override
    public ServiceWorkerWebSettingsCompat getServiceWorkerWebSettings() {
        return mWebSettings;
    }

    @Override
    public void setServiceWorkerClient(ServiceWorkerClientCompat client) {
        mImpl.setServiceWorkerClient(BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                new ServiceWorkerClientAdapter(client)));
    }
}
