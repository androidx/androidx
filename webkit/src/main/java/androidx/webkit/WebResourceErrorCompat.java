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

import android.webkit.WebResourceError;
import android.webkit.WebViewClient;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Compatibility version of {@link WebResourceError}.
 */
public abstract class WebResourceErrorCompat {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            WebViewClient.ERROR_UNKNOWN,
            WebViewClient.ERROR_HOST_LOOKUP,
            WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME,
            WebViewClient.ERROR_AUTHENTICATION,
            WebViewClient.ERROR_PROXY_AUTHENTICATION,
            WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_IO,
            WebViewClient.ERROR_TIMEOUT,
            WebViewClient.ERROR_REDIRECT_LOOP,
            WebViewClient.ERROR_UNSUPPORTED_SCHEME,
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE,
            WebViewClient.ERROR_BAD_URL,
            WebViewClient.ERROR_FILE,
            WebViewClient.ERROR_FILE_NOT_FOUND,
            WebViewClient.ERROR_TOO_MANY_REQUESTS,
            WebViewClient.ERROR_UNSAFE_RESOURCE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetErrorCode {}

    /**
     * Gets the error code of the error. The code corresponds to one
     * of the {@code ERROR_*} constants in {@link WebViewClient}.
     *
     * @return The error code of the error
     */
    @RequiresFeature(name = WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract @NetErrorCode int getErrorCode();

    /**
     * Gets the string describing the error. Descriptions are localized,
     * and thus can be used for communicating the problem to the user.
     *
     * @return The description of the error
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract CharSequence getDescription();

    /**
     * This class cannot be created by applications.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WebResourceErrorCompat() {
    }
}
