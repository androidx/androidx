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

import androidx.annotation.IntRange;

import org.jspecify.annotations.NonNull;

/**
 * Represents a configuration for speculative loading in a {@link Profile} instance. This should
 * be set using {@link Profile#setSpeculativeLoadingConfig(SpeculativeLoadingConfig)}
 */
@Profile.ExperimentalUrlPrefetch
public class SpeculativeLoadingConfig {

    private final int mPrefetchTTLSeconds;
    private final int mMaxPrefetches;
    private final int mMaxPrerenders;

    /**
     * Private constructors, the application will need to use
     * {@link Builder} for constructing instances of
     * this class.
     */
    private SpeculativeLoadingConfig(int ttlSecs, int maxPrefetches, int maxPrerenders) {
        mPrefetchTTLSeconds = ttlSecs;
        mMaxPrefetches = maxPrefetches;
        mMaxPrerenders = maxPrerenders;
    }

    /**
     * The "time to live" for a prefetch inside of the prefetch cache.
     * This is representative of the maximum time that a prefetch is considered
     * valid and can be served to a navigation. WebView will choose the appropriate default value.
     */
    @IntRange(from = 1)
    public int getPrefetchTtlSeconds() {
        return mPrefetchTTLSeconds;
    }

    /**
     * The max amount of prefetches that can live in the cache. WebView will choose the
     * appropriate default value.
     */
    @IntRange(from = 1)
    public int getMaxPrefetches() {
        return mMaxPrefetches;
    }

    /**
     * The max amount of prerenders that can live in the cache of any WebView associated with
     * this {@link Profile}. WebView will choose the appropriate default value.
     */
    @IntRange(from = 1)
    @Profile.ExperimentalUrlPrefetch
    public int getMaxPrerenders() {
        return mMaxPrerenders;
    }

    @Profile.ExperimentalUrlPrefetch
    public static final class Builder {
        private int mPrefetchTTLSeconds;
        private int mMaxPrefetches;
        private int mMaxPrerenders;

        public Builder() {
        }

        /**
         * Sets the Time-to-Live (TTL) in seconds for prefetched data.
         * <p>
         * This value determines how long prefetched data will be considered valid before it is
         * refreshed.
         *
         * @param ttlSeconds The TTL value in seconds. Must be a positive integer.
         * @return This builder instance for method chaining.
         * @throws IllegalArgumentException If {@code ttlSeconds} is less than 1.
         * @see Builder#build()
         */
        @NonNull
        public Builder setPrefetchTtlSeconds(@IntRange(from = 1) int ttlSeconds) {
            if (ttlSeconds <= 0) {
                throw new IllegalArgumentException("Prefetch TTL must be greater than 0");
            }
            mPrefetchTTLSeconds = ttlSeconds;
            return this;
        }

        /**
         * Sets the maximum number of allowed prefetches. This value limits the number of
         * prefetch data that can live in the cache. The WebView's internal maximum limit may
         * override this value.
         *
         * @param max The maximum number of prefetches. Must be a positive integer.
         * @return This builder instance for method chaining.
         * @throws IllegalArgumentException If {@code max} is less than 1.
         * @see Builder#build()
         */
        @NonNull
        public Builder setMaxPrefetches(@IntRange(from = 1) int max) {
            if (max < 1) {
                throw new IllegalArgumentException("Max prefetches must be greater than 0");
            }
            mMaxPrefetches = max;
            return this;
        }

        /**
         * Sets the maximum number of allowed prerenders. The WebView's internal maximum limit
         * may override this value.
         *
         * @param max The maximum number of prerenders. Must be a positive integer.
         * @return This builder instance for method chaining.
         * @throws IllegalArgumentException If {@code max} is less than 1.
         * @see Builder#build()
         */
        @NonNull
        public Builder setMaxPrerenders(@IntRange(from = 1) int max) {
            if (max < 1) {
                throw new IllegalArgumentException("Max prerenders must be greater than 0");
            }
            mMaxPrerenders = max;
            return this;
        }

        /**
         * Builds a new {@link SpeculativeLoadingConfig} instance.
         * <p>
         * This method creates a new {@link SpeculativeLoadingConfig} object using the parameters
         * that have been set in this builder.
         *
         * @return A new {@link SpeculativeLoadingConfig} instance.
         */
        @Profile.ExperimentalUrlPrefetch
        @NonNull
        public SpeculativeLoadingConfig build() {
            return new SpeculativeLoadingConfig(mPrefetchTTLSeconds, mMaxPrefetches,
                    mMaxPrerenders);
        }
    }
}
