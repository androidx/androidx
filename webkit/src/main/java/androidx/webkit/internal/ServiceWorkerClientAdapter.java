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

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.webkit.ServiceWorkerClientCompat;

import org.chromium.support_lib_boundary.ServiceWorkerClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

/**
 * Adapter between {@link ServiceWorkerClientCompat} and
 * {@link ServiceWorkerClientBoundaryInterface} (the corresponding interface shared with the support
 * library glue in the WebView APK).
 */
public class ServiceWorkerClientAdapter implements ServiceWorkerClientBoundaryInterface {
    private final ServiceWorkerClientCompat mClient;

    public ServiceWorkerClientAdapter(ServiceWorkerClientCompat client) {
        mClient = client;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        return mClient.shouldInterceptRequest(request);
    }

    @Override
    public String[] getSupportedFeatures() {
        return new String[] { Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST };
    }
}
