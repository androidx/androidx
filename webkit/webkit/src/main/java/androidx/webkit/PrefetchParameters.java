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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters for customizing the prefetch. Use the {@link Builder} to
 * construct.
 */
@RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@Profile.ExperimentalUrlPrefetch
public final class PrefetchParameters {
    private final @NonNull Map<String, String> mAdditionalHeaders;
    private final @Nullable NoVarySearchData mExpectedNoVarySearchData;

    private PrefetchParameters(@NonNull Map<String, String> additionalHeaders,
            @Nullable NoVarySearchData noVarySearchData) {
        this.mAdditionalHeaders = additionalHeaders;
        this.mExpectedNoVarySearchData = noVarySearchData;
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
    public @Nullable NoVarySearchData getExpectedNoVarySearchData() {
        return mExpectedNoVarySearchData;
    }

    /**
     * A builder class to use to construct the {@link PrefetchParameters}.
     */
    public static final class Builder {
        private final @NonNull Map<String, String> mAdditionalHeaders;
        private @Nullable NoVarySearchData mExpectedNoVarySearchData;

        public Builder() {
            mAdditionalHeaders = new HashMap<>();
            mExpectedNoVarySearchData = null;
        }

        /**
         * Use to finish building the PrefetchParams
         *
         * @return built PrefetchParams object.
         */
        @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
                enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
        @Profile.ExperimentalUrlPrefetch
        @NonNull
        public PrefetchParameters build() {
            return new PrefetchParameters(mAdditionalHeaders, mExpectedNoVarySearchData);
        }

        /**
         * Sets the header value for the given key. If called multiple times
         * for the same key, the latest value will be used.
         * <p>
         * Header keys must be RFC 2616-compliant.
         */
        @Profile.ExperimentalUrlPrefetch
        @NonNull
        public Builder addAdditionalHeader(@NonNull String key, @NonNull String value) {
            mAdditionalHeaders.put(key, value);
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
        @NonNull
        public Builder setExpectedNoVarySearchData(
                @NonNull NoVarySearchData expectedNoVarySearchData) {
            this.mExpectedNoVarySearchData = expectedNoVarySearchData;
            return this;
        }
    }
}
