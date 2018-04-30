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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ServiceWorkerControllerImpl;

/**
 * Manages Service Workers used by WebView.
 *
 * <p>Example usage:
 * <pre class="prettyprint">
 * ServiceWorkerControllerCompat swController = ServiceWorkerControllerCompat.getInstance();
 * swController.setServiceWorkerClient(new ServiceWorkerClientCompat() {
 *   {@literal @}Override
 *   public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
 *     // Capture request here and generate response or allow pass-through
 *     // by returning null.
 *     return null;
 *   }
 * });
 * </pre>
 */
public abstract class ServiceWorkerControllerCompat {
    /**
     *
     * @hide Don't allow apps to sub-class this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ServiceWorkerControllerCompat() {}

    /**
     * Returns the default ServiceWorkerController instance. At present there is
     * only one ServiceWorkerController instance for all WebView instances,
     * however this restriction may be relaxed in the future.
     *
     * @return the default ServiceWorkerController instance
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.SERVICE_WORKER_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static ServiceWorkerControllerCompat getInstance() {
        return LAZY_HOLDER.INSTANCE;
    }

    private static class LAZY_HOLDER {
        static final ServiceWorkerControllerCompat INSTANCE = new ServiceWorkerControllerImpl();
    }

    /**
     *
     * Gets the settings for all service workers.
     *
     * @return the current {@link ServiceWorkerWebSettingsCompat}
     *
     */
    @NonNull
    public abstract ServiceWorkerWebSettingsCompat getServiceWorkerWebSettings();

    /**
     *
     * Sets the client to capture service worker related callbacks.
     *
     * A {@link ServiceWorkerClientCompat} should be set before any service workers are
     * active, e.g. a safe place is before any WebView instances are created or
     * pages loaded.
     *
     */
    public abstract void setServiceWorkerClient(@Nullable ServiceWorkerClientCompat client);
}
