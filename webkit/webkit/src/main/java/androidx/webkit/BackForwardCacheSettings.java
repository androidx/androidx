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

import org.jspecify.annotations.NonNull;

/**
 * A class for developers to configure the back-forward cache on a {@link android.webkit.WebView}.
 *
 * <p>The back-forward cache is a browser feature that improves the user experience by keeping pages
 * alive for a limited time after the user navigates away from them. If the user navigates back
 * or forward, the page is reused for a fast back navigation.
 *
 * <p>Example:
 *
 * <pre class="prettyprint">
 * WebSettingsCompat.setBackForwardCacheSettings(
 *         webView.getSettings(),
 *         new BackForwardCacheSettings.Builder()
 *                 .setTimeoutInSeconds(600)
 *                 .setMaxPagesInCache(10)
 *                 .build());
 * </pre>
 */
@WebSettingsCompat.ExperimentalBackForwardCacheSettings
public class BackForwardCacheSettings {
    private final long mTimeoutSeconds;
    private final int mMaxPagesInCache;

    // Default values
    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 600; // 10 minutes
    private static final int DEFAULT_MAX_PAGES_IN_CACHE = 6;

    private BackForwardCacheSettings(
            long timeoutSeconds,
            int maxPagesInCache) {
        mTimeoutSeconds = timeoutSeconds;
        mMaxPagesInCache = maxPagesInCache;
    }

    /** Returns the timeout for pages in the back-forward cache, in seconds. */
    public long getTimeoutSeconds() {
        return mTimeoutSeconds;
    }

    /** Returns the maximum number of pages that can be stored in the back-forward cache. */
    public int getMaxPagesInCache() {
        return mMaxPagesInCache;
    }

    /** Builder for {@link BackForwardCacheSettings}. */
    public static final class Builder {
        private long mTimeoutInSeconds = DEFAULT_TIMEOUT_IN_SECONDS;
        private int mMaxPagesInCache = DEFAULT_MAX_PAGES_IN_CACHE;

        /**
         * Sets the timeout for pages in the back-forward cache.
         *
         * @param timeoutInSeconds The timeout in seconds.
         * @return This builder.
         */
        @NonNull
        public Builder setTimeoutSeconds(long timeoutInSeconds) {
            mTimeoutInSeconds = timeoutInSeconds;
            return this;
        }

        /**
         * Sets the maximum number of pages that can be stored in the back-forward cache.
         *
         * @param maxPagesInCache The maximum number of pages.
         * @return This builder.
         */
        @NonNull
        public Builder setMaxPagesInCache(int maxPagesInCache) {
            mMaxPagesInCache = maxPagesInCache;
            return this;
        }

        /**
         * Builds the {@link BackForwardCacheSettings} object.
         *
         * @return The {@link BackForwardCacheSettings} object.
         */
        @NonNull
        public BackForwardCacheSettings build() {
            return new BackForwardCacheSettings(
                    mTimeoutInSeconds, mMaxPagesInCache);
        }
    }
}
