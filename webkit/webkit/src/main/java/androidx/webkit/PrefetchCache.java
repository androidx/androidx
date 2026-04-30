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

import androidx.annotation.IntRange;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.jspecify.annotations.NonNull;

/**
 * PrefetchCache manages the configuration of the prefetch cache for a {@link Profile}.
 * <p>
 * It allows applications to tune the behavior of prefetch cache by setting limits such as
 * the maximum number of prefetches and the time-to-live for prefetched responses. These
 * configurations are applied to all WebViews associated with the profile.
 * <p>
 * Use {@link Profile#getPrefetchCache()} to obtain the PrefetchCache instance for a specific
 * profile.
 */
@Profile.ExperimentalUrlPrefetch
public final class PrefetchCache {

    private final @NonNull ProfileBoundaryInterface mProfileImpl;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PrefetchCache(@NonNull ProfileBoundaryInterface profileImpl) {
        mProfileImpl = profileImpl;
    }

    /**
     * Sets the maximum number of prefetches for the current browsing session.
     * <p>
     * This configuration will be applied to any prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * This configuration will be applied to WebViews that are associated with the
     * {@link Profile} that owns this {@link PrefetchCache}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     *
     * @param maxPrefetches the maximum number of prefetches to allow.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void setMaxPrefetches(@IntRange(from = 1) int maxPrefetches) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            if (maxPrefetches < 1) {
                throw new IllegalArgumentException(
                        "maxPrefetches should be greater than or equal to 1");
            }
            mProfileImpl.setMaxPrefetches(maxPrefetches);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns maximum prefetches set for this Profile.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public int getMaxPrefetches() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getMaxPrefetches();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Resets the maximum number of prefetches for the current browsing session to system default.
     * <p>
     * This configuration will be applied to any prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * This configuration will be applied to WebViews that are associated with the
     * {@link Profile} that owns this {@link PrefetchCache}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     *
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void clearMaxPrefetches() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearMaxPrefetches();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the maximum prefetch Time-to-Live (TTL) in seconds for the current browsing session.
     * <p>
     * These configurations will be applied to any prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * These configurations will be applied to WebViews that are associated with the
     * {@link Profile} that owns this {@link PrefetchCache}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     *
     * @param prefetchTtlSeconds the TTL in seconds.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void setPrefetchTtlSeconds(@IntRange(from = 1) int prefetchTtlSeconds) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            if (prefetchTtlSeconds < 1) {
                throw new IllegalArgumentException(
                        "prefetchTtlSeconds should be greater than or equal to 1");
            }
            mProfileImpl.setPrefetchTtlSeconds(prefetchTtlSeconds);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns Prefetch TTL in Seconds set for this Profile.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public int getPrefetchTtlSeconds() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getPrefetchTtlSeconds();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Resets the maximum prefetch Time-to-Live (TTL) in seconds for the current browsing session
     * to the system default.
     * <p>
     * This configuration will be applied to any prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * This configuration will be applied to WebViews that are associated with the
     * {@link Profile} that owns this {@link PrefetchCache}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PREFETCH_CACHE_V1}.
     *
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PREFETCH_CACHE_V1}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void clearPrefetchTtl() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearPrefetchTtl();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
