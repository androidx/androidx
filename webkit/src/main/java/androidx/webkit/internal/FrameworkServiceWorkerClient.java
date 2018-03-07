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
import android.webkit.ServiceWorkerClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.RequiresApi;
import androidx.webkit.ServiceWorkerClientCompat;

/**
 * A shim class that implements {@link ServiceWorkerClient} by delegating to a
 * {@link ServiceWorkerClientCompat}.
 * This class is used on up-to-date devices to avoid using reflection to call into WebView APK code.
 */
@RequiresApi(Build.VERSION_CODES.N)
public class FrameworkServiceWorkerClient extends ServiceWorkerClient {
    private final ServiceWorkerClientCompat mImpl;

    public FrameworkServiceWorkerClient(ServiceWorkerClientCompat impl) {
        mImpl = impl;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        return mImpl.shouldInterceptRequest(request);
    }
}
