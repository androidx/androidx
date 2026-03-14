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
import org.jspecify.annotations.Nullable;

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
     * These configurations will be applied to any prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * These configurations will be applied to WebViews that are associated with the
     * {@link Profile} that owns this {@link PrefetchCache}.
     *
     * @param maxPrefetches the maximum number of prefetches to allow. Setting this value to
     *                      {@code null} will use the default value.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void setMaxPrefetches(
            @Nullable @IntRange(from = 1) @SuppressWarnings("AutoBoxing") Integer maxPrefetches) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            if (maxPrefetches != null && maxPrefetches < 1) {
                throw new IllegalArgumentException(
                        "maxPrefetches should be greater than or equal to 1");
            }
            mProfileImpl.setMaxPrefetches(maxPrefetches);
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
     * @param prefetchTtlSeconds the TTL in seconds. Setting this value to {@code null}
     *                           will use the default value.
     */
    @RequiresFeature(name = WebViewFeature.PREFETCH_CACHE_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @Profile.ExperimentalUrlPrefetch
    public void setPrefetchTtlSeconds(
            @Nullable @IntRange(from = 1) @SuppressWarnings("AutoBoxing")
            Integer prefetchTtlSeconds) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            if (prefetchTtlSeconds != null && prefetchTtlSeconds < 1) {
                throw new IllegalArgumentException(
                        "prefetchTtlSeconds should be greater than or equal to 1");
            }
            mProfileImpl.setPrefetchTtlSeconds(prefetchTtlSeconds);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
