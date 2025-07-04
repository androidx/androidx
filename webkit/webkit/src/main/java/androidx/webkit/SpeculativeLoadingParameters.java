/*
 * Copyright 2024 The Android Open Source Project
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

import android.webkit.WebSettings;

import androidx.annotation.RequiresFeature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters for customizing the prefetch. Use the {@link Builder} to
 * construct.
 */
@RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@Profile.ExperimentalUrlPrefetch
public final class SpeculativeLoadingParameters {
    private final @NonNull Map<String, String> mAdditionalHeaders;
    private final @Nullable NoVarySearchHeader mExpectedNoVarySearchHeader;
    private final boolean mIsJavaScriptEnabled;

    private SpeculativeLoadingParameters(@NonNull Map<String, String> additionalHeaders,
            @Nullable NoVarySearchHeader noVarySearchHeader, boolean isJavaScriptEnabled) {
        mAdditionalHeaders = additionalHeaders;
        mExpectedNoVarySearchHeader = noVarySearchHeader;
        mIsJavaScriptEnabled = isJavaScriptEnabled;
    }

    /**
     * @return The map of the additional headers built using {@link Builder}.
     */
    public @NonNull Map<String, String> getAdditionalHeaders() {
        return mAdditionalHeaders;
    }

    /**
     * @return The noVarySearch model built using {@link Builder}.
     */
    public @Nullable NoVarySearchHeader getExpectedNoVarySearchData() {
        return mExpectedNoVarySearchHeader;
    }

    /**
     * @return The javascript enabled setting built using {@link Builder}.
     */
    public boolean isJavaScriptEnabled() {
        return mIsJavaScriptEnabled;
    }

    /**
     * A builder class to use to construct the {@link SpeculativeLoadingParameters}.
     */
    public static final class Builder {
        private final @NonNull Map<String, String> mAdditionalHeaders;
        private @Nullable NoVarySearchHeader mExpectedNoVarySearchHeader;
        private boolean mIsJavaScriptEnabled;

        public Builder() {
            mAdditionalHeaders = new HashMap<>();
            mExpectedNoVarySearchHeader = null;
            mIsJavaScriptEnabled = false;
        }

        /**
         * Use to finish building the PrefetchParams
         *
         * @return built PrefetchParams object.
         */
        @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
                enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
        @Profile.ExperimentalUrlPrefetch
        public @NonNull SpeculativeLoadingParameters build() {
            return new SpeculativeLoadingParameters(mAdditionalHeaders, mExpectedNoVarySearchHeader,
                    mIsJavaScriptEnabled);
        }

        /**
         * Sets the header value for the given key. If called multiple times
         * for the same key, the latest value will be used.
         * <p>
         * Header keys must be RFC 2616-compliant.
         * <p>
         * The logic for handling additional header isn't guaranteed to match the
         * {@link android.webkit.WebView#loadUrl(String, Map)}'s logic and is subject to change
         * in the future.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder addAdditionalHeader(@NonNull String key, @NonNull String value) {
            mAdditionalHeaders.put(key, value);
            return this;
        }

        /**
         * Sets multiple headers at once. The headers passed in here will
         * be merged with any that have been previously set (duplicate keys
         * will be overridden).
         * <p>
         * Header keys must be RFC 2616-compliant.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder addAdditionalHeaders(@NonNull Map<String, String>
                additionalHeaders) {
            mAdditionalHeaders.putAll(additionalHeaders);
            return this;
        }

        /**
         * Sets the "No-Vary-Search data that's expected to be returned via. the
         * header in the prefetch's response. This is used to help determine if
         * WebView#loadUrl should either use an in-flight prefetch response to
         * render the web contents or handle the URL as it typically does
         * (i.e. start a network request).
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder setExpectedNoVarySearchData(
                @NonNull NoVarySearchHeader expectedNoVarySearchHeader) {
            mExpectedNoVarySearchHeader = expectedNoVarySearchHeader;
            return this;
        }

        /**
         * {@code true} if the WebView's that will be loading the prefetched
         * response will have javascript enabled. This affects whether or not
         * client hints header is sent with the prefetch request.
         * <p>
         * Note: This flag is ignored for prefetches initiated by the
         * prerendering API. The value from
         * {@link WebSettings#getJavaScriptEnabled()} will be used instead.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder setJavaScriptEnabled(boolean javaScriptEnabled) {
            mIsJavaScriptEnabled = javaScriptEnabled;
            return this;
        }

    }
}
