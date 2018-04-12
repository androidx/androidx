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

import android.annotation.SuppressLint;
import android.webkit.WebResourceRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.webkit.internal.WebResourceRequestAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

// TODO(gsennton) add a test for this class

/**
 * Compatibility version of {@link WebResourceRequest}.
 */
public class WebResourceRequestCompat {

    // Developers should not be able to instantiate this class.
    private WebResourceRequestCompat() {}

    /**
     * Gets whether the request was a result of a server-side redirect.
     *
     * @return whether the request was a result of a server-side redirect.
     */
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean isRedirect(@NonNull WebResourceRequest request) {
        WebViewFeatureInternal feature = WebViewFeatureInternal.WEB_RESOURCE_REQUEST_IS_REDIRECT;
        if (feature.isSupportedByFramework()) {
            return request.isRedirect();
        } else if (feature.isSupportedByWebView()) {
            return getAdapter(request).isRedirect();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private static WebResourceRequestAdapter getAdapter(WebResourceRequest request) {
        return WebViewGlueCommunicator.getCompatConverter().convertWebResourceRequest(request);
    }
}
