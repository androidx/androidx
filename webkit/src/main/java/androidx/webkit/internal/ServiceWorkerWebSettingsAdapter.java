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

import androidx.webkit.ServiceWorkerWebSettingsCompat;

import org.chromium.support_lib_boundary.ServiceWorkerWebSettingsBoundaryInterface;

/**
 * Adapter between {@link ServiceWorkerWebSettingsCompat} and
 * {@link ServiceWorkerWebSettingsBoundaryInterface} (the corresponding interface shared with the
 * support library glue in the WebView APK).
 */
public class ServiceWorkerWebSettingsAdapter extends ServiceWorkerWebSettingsCompat {
    private final ServiceWorkerWebSettingsBoundaryInterface mImpl;

    public ServiceWorkerWebSettingsAdapter(ServiceWorkerWebSettingsBoundaryInterface impl) {
        mImpl = impl;
    }

    @Override
    public void setCacheMode(int mode) {
        mImpl.setCacheMode(mode);
    }

    @Override
    public int getCacheMode() {
        return mImpl.getCacheMode();
    }

    @Override
    public void setAllowContentAccess(boolean allow) {
        mImpl.setAllowContentAccess(allow);
    }

    @Override
    public boolean getAllowContentAccess() {
        return mImpl.getAllowContentAccess();
    }

    @Override
    public void setAllowFileAccess(boolean allow) {
        mImpl.setAllowFileAccess(allow);
    }

    @Override
    public boolean getAllowFileAccess() {
        return mImpl.getAllowFileAccess();
    }

    @Override
    public void setBlockNetworkLoads(boolean flag) {
        mImpl.setBlockNetworkLoads(flag);
    }

    @Override
    public boolean getBlockNetworkLoads() {
        return mImpl.getBlockNetworkLoads();
    }
}
