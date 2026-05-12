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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters for customizing the prefetch. Use the {@link Builder} to
 * construct.
 */
@Profile.ExperimentalUrlPrefetch
public final class PrefetchParameters {
    private final @NonNull Map<String, String> mAdditionalHeaders;
    private final @Nullable NoVarySearchHeader mExpectedNoVarySearchHeader;
    private final boolean mIsJavaScriptEnabled;
    private final @Nullable Integer mVariationsId;

    private PrefetchParameters(@NonNull Map<String, String> additionalHeaders,
            @Nullable NoVarySearchHeader noVarySearchHeader, boolean isJavaScriptEnabled,
            @Nullable Integer variationsId) {
        mAdditionalHeaders = additionalHeaders;
        mExpectedNoVarySearchHeader = noVarySearchHeader;
        mIsJavaScriptEnabled = isJavaScriptEnabled;
        mVariationsId = variationsId;
    }

    /**
     * Returns the map of additional HTTP headers to be sent with the prefetch request.
     */
    public @NonNull Map<String, String> getAdditionalHeaders() {
        return mAdditionalHeaders;
    }

    /**
     * Returns the expected No-Vary-Search header data used to match subsequent navigations
     * to this prefetch.
     */
    public @Nullable NoVarySearchHeader getExpectedNoVarySearchData() {
        return mExpectedNoVarySearchHeader;
    }

    /**
     * Returns whether JavaScript is enabled for the prefetch, which influences the sending
     * of client hints.
     */
    public boolean isJavaScriptEnabled() {
        return mIsJavaScriptEnabled;
    }

    /**
     * Returns the variations ID associated with this prefetch request, if any.
     */
    @SuppressWarnings("AutoBoxing") // Integer is intentional here.
    public @Nullable Integer getVariationsId() {
        return mVariationsId;
    }

    /**
     * A builder class for constructing {@link PrefetchParameters} instances.
     */
    public static final class Builder {
        private final @NonNull Map<String, String> mAdditionalHeaders = new HashMap<>();
        private @Nullable NoVarySearchHeader mExpectedNoVarySearchHeader;
        private boolean mIsJavaScriptEnabled;
        private @Nullable Integer mVariationsId;

        /** Construct a new Builder. */
        public Builder() {
        }

        /**
         * Build the {@link PrefetchParameters}.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull PrefetchParameters build() {
            return new PrefetchParameters(mAdditionalHeaders, mExpectedNoVarySearchHeader,
                    mIsJavaScriptEnabled, mVariationsId);
        }

        /**
         * Sets the header value for the given key. If called multiple times
         * for the same key, the last value will be used.
         * <p>
         * Header keys must be <a href="https://datatracker.ietf.org/doc/html/rfc2616">RFC 2616</a>
         * compliant.
         * <p>
         * The logic for handling additional header isn't guaranteed to match the
         * {@link android.webkit.WebView#loadUrl(String, Map)}'s logic and is subject to change
         * in the future.
         *
         * @param key The header key.
         * @param value The header value.
         * @return This builder instance for chaining.
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
         * Header keys must be <a href="https://datatracker.ietf.org/doc/html/rfc2616">RFC 2616</a>
         * -compliant.
         *
         * @param additionalHeaders A map of additional headers to set.
         * @return This builder instance for chaining.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder addAdditionalHeaders(@NonNull Map<String, String>
                additionalHeaders) {
            mAdditionalHeaders.putAll(additionalHeaders);
            return this;
        }

        /**
         * Sets the
         * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/No-Vary-Search">No-Vary-Search</a>
         * data that's expected to be returned via the header in the prefetch's response.
         * <p>
         * This is used to help determine if
         * WebView#loadUrl should either use an in-flight prefetch response to
         * render the web contents or handle the URL as it typically does
         * (i.e. start a network request).
         *
         * @param expectedNoVarySearchHeader The No-Vary-Search data expected to be returned in
         *                                   the prefetch's response.
         * @return This builder instance for chaining.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder setExpectedNoVarySearchData(
                @NonNull NoVarySearchHeader expectedNoVarySearchHeader) {
            mExpectedNoVarySearchHeader = expectedNoVarySearchHeader;
            return this;
        }

        /**
         * Set whether the page that is loaded will have JavaScript enabled.
         * {@code true} if the WebView's that will be loading the prefetched
         * response will have javascript enabled. This affects whether
         * client hints header is sent with the prefetch request.
         *
         * @param javaScriptEnabled {@code true} if the WebView that will be loading the
         *                          prefetched response will have JavaScript enabled.
         * @return This builder instance for chaining.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder setJavaScriptEnabled(boolean javaScriptEnabled) {
            mIsJavaScriptEnabled = javaScriptEnabled;
            return this;
        }

        /**
         * Sets an optional variations ID to associate with this prefetch request.
         *
         * @param variationsId An Integer ID for this prefetch configuration, or {@code null}
         *                     if no specific variations ID is applicable.
         * @return This builder instance for chaining.
         */
        @Profile.ExperimentalUrlPrefetch
        public @NonNull Builder setVariationsId(
                @SuppressWarnings("AutoBoxing") @Nullable Integer variationsId) {
            mVariationsId = variationsId;
            return this;
        }

    }
}
