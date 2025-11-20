/*
 * Copyright 2025 The Android Open Source Project
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

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.RequiresFeature;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.WebViewProviderFactoryBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility version of {@link WebResourceResponse}. This class can be used as a direct
 * replacement of the framework class.
 *
 * @see WebResourceResponse
 */
public class WebResourceResponseCompat {
    private final @NonNull WebResourceResponse mWrapped;
    private @NonNull List<String> mCookies = Collections.emptyList();

    /**
     * Convert a {@link WebResourceResponse} to the compat version.
     */
    @NonNull
    public static WebResourceResponseCompat toWebResourceResponseCompat(
            @NonNull WebResourceResponse response) {

        int statusCode = response.getStatusCode();
        String reasonPhrase = response.getReasonPhrase();
        if (statusCode < 100) {
            statusCode = 200;
        }
        if (reasonPhrase == null) {
            reasonPhrase = "OK";
        }
        return new WebResourceResponseCompat(response.getMimeType(), response.getEncoding(),
                statusCode, reasonPhrase, response.getResponseHeaders(), response.getData());
    }

    /**
     * Constructor matching
     * {@link WebResourceResponse#WebResourceResponse(String, String, InputStream)}
     */
    public WebResourceResponseCompat(@NonNull String mimeType, @Nullable String encoding,
            @Nullable InputStream data) {
        mWrapped = new WebResourceResponse(mimeType, encoding, data);
        mWrapped.setResponseHeaders(Map.of());
    }

    /**
     * Constructor matching
     * {@link WebResourceResponse#WebResourceResponse(String, String, int, String, Map, InputStream)}
     */
    public WebResourceResponseCompat(@NonNull String mimeType, @Nullable String encoding,
            int statusCode, @NonNull String reasonPhrase,
            @Nullable Map<String, String> responseHeaders, @Nullable InputStream data) {
        mWrapped = new WebResourceResponse(mimeType, encoding, statusCode, reasonPhrase,
                responseHeaders != null ? responseHeaders : Map.of(), data);
    }

    /**
     * Convert this object to a {@link WebResourceResponse} that can be used as return value in
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(WebView, WebResourceRequest)}
     * and {@link android.webkit.ServiceWorkerClient#shouldInterceptRequest(WebResourceRequest)}.
     *
     * <p>The cookie headers set through {@link #setCookies(List)} will be encoded as part of the
     * {@link WebResourceResponse#getResponseHeaders()} header map to use by the WebView
     * implementation.
     */
    @NonNull
    public WebResourceResponse toWebResourceResponse() {
        Map<String, String> headers = mWrapped.getResponseHeaders();
        Map<String, String> mergedHeaders;
        if (headers != null) {
            mergedHeaders = new HashMap<>(headers);
        } else {
            mergedHeaders = new HashMap<>();
        }
        if (!mCookies.isEmpty()) {
            mergedHeaders.put(WebViewProviderFactoryBoundaryInterface.MULTI_COOKIE_HEADER_NAME,
                    serializeMultiCookieHeader(mCookies));
        }
        // The constructor checks if statusCode and reasonPhrase are valid, even though it is
        // possible to construct invalid objects with the short constructor.
        // We set the values to 200 OK as default values if no other values are set.
        int statusCode = mWrapped.getStatusCode();
        String reasonPhrase = mWrapped.getReasonPhrase();
        if (statusCode < 100) {
            statusCode = 200;
            reasonPhrase = "OK";
        }
        return new WebResourceResponse(mWrapped.getMimeType(), mWrapped.getEncoding(), statusCode,
                reasonPhrase, mergedHeaders, mWrapped.getData());
    }

    /**
     * @see WebResourceResponse#setMimeType(String)
     */
    public void setMimeType(@NonNull String mimeType) {
        mWrapped.setMimeType(mimeType);
    }

    /**
     * @see WebResourceResponse#getMimeType()
     */
    @NonNull
    public String getMimeType() {
        return mWrapped.getMimeType();
    }

    /**
     * @see WebResourceResponse#setEncoding(String)
     */
    public void setEncoding(@Nullable String encoding) {
        mWrapped.setEncoding(encoding);
    }

    /**
     * @see WebResourceResponse#getEncoding()
     */
    @Nullable
    public String getEncoding() {
        return mWrapped.getEncoding();
    }

    /**
     * @see WebResourceResponse#setStatusCodeAndReasonPhrase(int, String)
     */
    public void setStatusCodeAndReasonPhrase(int statusCode, @NonNull String reasonPhrase) {
        mWrapped.setStatusCodeAndReasonPhrase(statusCode, reasonPhrase);
    }

    /**
     * @see WebResourceResponse#getStatusCode()
     */
    public int getStatusCode() {
        return mWrapped.getStatusCode();
    }

    /**
     * @see WebResourceResponse#getReasonPhrase()
     */
    @Nullable
    public String getReasonPhrase() {
        return mWrapped.getReasonPhrase();
    }

    /**
     * @see WebResourceResponse#setResponseHeaders(Map)
     */
    public void setResponseHeaders(@NonNull Map<String,  String> headers) {
        mWrapped.setResponseHeaders(headers);
    }

    /**
     * @see WebResourceResponse#getResponseHeaders()
     */
    @NonNull
    public Map<String, String> getResponseHeaders() {
        return mWrapped.getResponseHeaders();
    }

    /**
     * @see WebResourceResponse#setData(InputStream)
     */
    public void setData(@Nullable InputStream data) {
        mWrapped.setData(data);
    }

    /**
     * @see WebResourceResponse#getData()
     */
    @Nullable
    public InputStream getData() {
        return mWrapped.getData();
    }

    /**
     * Set the list of {@code Set-Cookie} header values applicable to this response.
     *
     * <p>Note that these values will only be used by WebView if
     * {@link WebSettingsCompat#areCookiesIncludedInShouldInterceptRequest(WebSettings)} is
     * {@code true}, and by service workers if
     * {@link ServiceWorkerWebSettingsCompat#isIncludeCookiesOnShouldInterceptRequestEnabled()}
     * is {@code true}. Otherwise the values will be ignored.
     *
     * <p>It is safe to use this method even if the map of response headers provided in the
     * constructor or through {@link #setResponseHeaders(Map)} already contains a
     * {@code Set-Cookie} value. In such cases, all values will be applied.
     * However, it is recommended to only use this method to supply {@code Set-Cookie} header
     * values. A {@code Set-Cookie} value in the header map will also only be used according to the
     * restrictions mentioned above.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns true for
     * {@link WebViewFeature#COOKIE_INTERCEPT}.
     *
     * @param cookies List of valid {@code Set-Cookie} header values
     */
    @RequiresFeature(name = WebViewFeature.COOKIE_INTERCEPT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setCookies(@NonNull List<String> cookies) {
        final ApiFeature.NoFramework feature = WebViewFeatureInternal.COOKIE_INTERCEPT;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        mCookies = cookies;
    }

    /**
     * Get the list of cookies set by {@link #setCookies(List)} or an empty list.
     */
    @RequiresFeature(name = WebViewFeature.COOKIE_INTERCEPT, enforcement = "androidx.webkit"
            + ".WebViewFeature#isFeatureSupported")
    public @NonNull List<String> getCookies() {
        return mCookies;
    }

    /**
     * Serializes the {@code cookieValues} list into a string that can be unpacked by WebView code.
     *
     * @param cookieValues List of values to serialize
     */
    @NonNull
    private String serializeMultiCookieHeader(@NonNull List<String> cookieValues) {
        if (cookieValues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String cookieValue : cookieValues) {
            if (!cookieValue.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(WebViewProviderFactoryBoundaryInterface.MULTI_COOKIE_VALUE_SEPARATOR);
                }
                sb.append(cookieValue.trim());
            }
        }
        return sb.toString();
    }
}
