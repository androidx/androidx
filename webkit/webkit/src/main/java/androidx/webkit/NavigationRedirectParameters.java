/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.NavigationRedirectParametersBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.util.Collections;
import java.util.Map;

/**
 * A class which exposes response headers for
 * //TODO: put jdoc here
 * in a way which is backwards compatible for older WebView versions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class NavigationRedirectParameters {

    private final @NonNull NavigationRedirectParametersBoundaryInterface mBoundaryInterface;

    /**
     * Factory method that returns the NavigationRedirectParams associated with the given
     * invocationHandler.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull NavigationRedirectParameters forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        NavigationRedirectParametersBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        NavigationRedirectParametersBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return new NavigationRedirectParameters(boundaryInterface);
    }

    private NavigationRedirectParameters(
            @NonNull NavigationRedirectParametersBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Returns the HTTP response headers of this redirect response (for example {@code Location}).
     * <p>
     * The {@code Set-Cookie} header is never included.
     *
     * @return An unmodifiable {@link Map} from header name to header value.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#NAVIGATION_GET_RESPONSE_HEADERS} feature is not supported.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_RESPONSE_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @NonNull Map<String, String> getResponseHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_RESPONSE_HEADERS;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        Map<String, String> responseHeaders =
                mBoundaryInterface.getResponseHeaders();
        return Collections.unmodifiableMap(responseHeaders);
    }

    /**
     * Returns the status code received by the navigation for the current redirect callback.
     *
     * @return The HTTP status code.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#NAVIGATION_GET_RESPONSE_HEADERS} feature is not supported.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.NAVIGATION_GET_RESPONSE_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public int getStatusCode() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.NAVIGATION_GET_RESPONSE_HEADERS;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        return mBoundaryInterface.getStatusCode();
    }

}
