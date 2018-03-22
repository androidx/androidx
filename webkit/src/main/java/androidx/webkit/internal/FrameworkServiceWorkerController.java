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

import android.os.Build;
import android.webkit.ServiceWorkerController;

import androidx.annotation.RequiresApi;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.ServiceWorkerControllerCompat;
import androidx.webkit.ServiceWorkerWebSettingsCompat;

/**
 * Implementation of {@link ServiceWorkerControllerCompat} meant for use on up-to-date platforms.
 * This class does not use reflection to bypass framework APIs - instead it uses android.webkit
 * APIs.
 */
@RequiresApi(Build.VERSION_CODES.N)
public class FrameworkServiceWorkerController extends ServiceWorkerControllerCompat {
    private final ServiceWorkerController mImpl;
    private ServiceWorkerWebSettingsCompat mSettings;

    public FrameworkServiceWorkerController(ServiceWorkerController impl) {
        mImpl = impl;
    }

    @Override
    public ServiceWorkerWebSettingsCompat getServiceWorkerWebSettings() {
        if (mSettings == null) {
            mSettings = new FrameworksServiceWorkerWebSettings(mImpl.getServiceWorkerWebSettings());
        }
        return mSettings;
    }

    @Override
    public void setServiceWorkerClient(ServiceWorkerClientCompat client) {
        mImpl.setServiceWorkerClient(new FrameworkServiceWorkerClient(client));
    }
}
