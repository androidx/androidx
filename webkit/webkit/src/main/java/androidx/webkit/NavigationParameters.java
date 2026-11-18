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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters used for the {@link WebViewCompat#navigate} API. Use the
 * {@link NavigationParameters.Builder} to construct.
 */
@RequiresFeature(name = WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@WebViewCompat.ExperimentalNavigate
public final class NavigationParameters {

    private final boolean mShouldReplaceCurrentEntry;
    private final @NonNull Map<String, String> mAdditionalHeaders;
    private final @Nullable String mReferrer;

    private NavigationParameters(@NonNull Builder builder) {
        mShouldReplaceCurrentEntry = builder.mShouldReplaceCurrentEntry;
        mAdditionalHeaders = builder.mAdditionalHeaders;
        mReferrer = builder.mReferrer;
    }

    /**
     * @return Setting for whether a navigation will replace the current entry,
     * set using {@link NavigationParameters.Builder}.
     */
    public boolean shouldReplaceCurrentEntry() {
        return mShouldReplaceCurrentEntry;
    }

    /**
     * @return The map of the additional headers added using {@link NavigationParameters.Builder}.
     */
    public @NonNull Map<String, String> getAdditionalHeaders() {
        return mAdditionalHeaders;
    }

    /**
     * @return The referrer set using {@link NavigationParameters.Builder#setReferrer(String)}, or
     * {@code null} if it wasn't set.
     */
    public @Nullable String getReferrer() {
        return mReferrer;
    }

    /**
     * A Builder for {@link NavigationParameters}.
     */
    public static final class Builder {
        private boolean mShouldReplaceCurrentEntry;

        private final @NonNull Map<String, String> mAdditionalHeaders;

        private @Nullable String mReferrer;

        /**
         * Constructor for NavigationParameters builder which sets parameter defaults.
         */
        public Builder() {
            mShouldReplaceCurrentEntry = false;
            mAdditionalHeaders = new HashMap<>();
        }

        /**
         * If true, the navigation will replace the current entry in the
         * WebView's history, meaning that the current entry will not be
         * reachable through WebView#goBack. Defaults to {@code false}.
         */
        public @NonNull Builder setShouldReplaceCurrentEntry(boolean shouldReplaceCurrentEntry) {
            mShouldReplaceCurrentEntry = shouldReplaceCurrentEntry;
            return this;
        }

        /**
         * Adds an additional HTTP header to the request. Note that if the
         * header is set by default by this WebView, such as those
         * controlling caching, accept types or the User-Agent, it
         * may be overridden by this WebView's default.
         *
         * <p>
         * Unlike headers passed to
         * {@link android.webkit.WebView#loadUrl(String, Map)},
         * additional headers passed here are tied to the navigation,
         * meaning that subsequent user triggered navigations to the
         * same URL will not have those headers applied. If called
         * multiple times for the same key, the latest value will be used.
         *
         * <p>
         * Header keys must be RFC 2616-compliant.
         */
        public @NonNull Builder addAdditionalHeader(
                @NonNull String key, @NonNull String value) {
            mAdditionalHeaders.put(key, value);
            return this;
        }

        /**
         * Adds multiple additional HTTP headers to the request, as if
         * {@link NavigationParameters.Builder#addAdditionalHeader(String, String)}
         * were called multiple times with each key-value pair of the map. See
         * {@link NavigationParameters.Builder#addAdditionalHeader(String, String)}
         * for more information.
         */
        public @NonNull Builder addAdditionalHeaders(
                @NonNull Map<String, String> additionalHeaders) {
            mAdditionalHeaders.putAll(additionalHeaders);
            return this;
        }

        /**
         * Sets the referrer of the navigation.
         *
         * <p>
         * The referrer is sent to the server in the {@code Referer} header and exposed to the
         * page as {@code document.referrer}, subject to the default referrer policy,
         * {@code strict-origin-when-cross-origin}: only its origin is sent for a cross-origin
         * navigation, and nothing is sent if it's an {@code https} URL and the navigation is to
         * an {@code http} URL.
         *
         * <p>
         * Defaults to {@code null}, meaning the navigation has no referrer. A non-null referrer
         * must be a valid {@code http} or {@code https} URL, otherwise
         * {@link WebViewCompat#navigate} throws an {@link IllegalArgumentException}.
         *
         * <p>
         * Use this rather than adding a {@code Referer} header with
         * {@link #addAdditionalHeader(String, String)}, which is ignored.
         *
         * <p>
         * This method is a no-op if {@link WebViewFeature#isFeatureSupported(String)} returns
         * {@code false} for {@link WebViewFeature#WEBVIEW_NAVIGATE_REFERRER}.
         */
        public @NonNull Builder setReferrer(@Nullable String referrer) {
            mReferrer = referrer;
            return this;
        }

        /**
         * Use to finish building the NavigationParameters.
         *
         * @return built NavigationParameters object.
         * @throws UnsupportedOperationException if the
         *     {@link WebViewFeature#WEBVIEW_NAVIGATE_EXPERIMENTAL_V1} feature is not supported.
         *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
         */
        @RequiresFeature(name = WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1,
                enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
        public @NonNull NavigationParameters build() {
            return new NavigationParameters(this);
        }
    }
}
