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

import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.AnyThread;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.PrefetchOperationCallbackAdapter;
import androidx.webkit.internal.SpeculativeLoadingParametersAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.concurrent.Executor;


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
     *
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
     *
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


    /**
     * Starts a URL prefetch request.
     * <p>
     * All WebViews associated with this Profile will use a URL request
     * matching algorithm during execution of all variants of
     * {@link android.webkit.WebView#loadUrl(String)} for determining if there
     * was already a prefetch request executed for the provided URL. This
     * includes prefetches that are "in progress". If a prefetch is matched,
     * WebView will leverage that for handling the URL, otherwise the URL
     * will be handled normally (i.e. through a network request).
     * <p>
     * Applications will still be responsible for calling
     * {@link android.webkit.WebView#loadUrl(String)} to display web contents
     * in a WebView.
     * <p>
     * NOTE: Additional headers passed to
     * {@link android.webkit.WebView#loadUrl(String, Map)} are not considered
     * in the matching algorithm for determining whether or not to serve a
     * prefetched response to a navigation.
     * <p>
     * For max latency saving benefits, it is recommended to call this method
     * as early as possible (i.e. before any WebView associated with this
     * profile is created).
     * <p>
     * Only supports HTTPS scheme.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PROFILE_URL_PREFETCH}.
     *
     * @param url                the url associated with the prefetch request.
     * @param cancellationSignal will make the best effort to cancel an
     *                           in-flight prefetch request, However cancellation is not
     *                           guaranteed.
     * @param callbackExecutor   the executor to resolve the callback with. If {@code null},
     *                           the callback will be executed on the main thread.
     * @param outcomeReceiver    callbacks for reporting result back to application.
     * @throws IllegalArgumentException      if the url or callback is null.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PROFILE_URL_PREFETCH}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @AnyThread
    @Profile.ExperimentalUrlPrefetch
    public void prefetchUrlAsync(
            @NonNull String url,
            @Nullable CancellationSignal cancellationSignal,
            @Nullable Executor callbackExecutor,
            @NonNull WebViewOutcomeReceiver<@Nullable Void, PrefetchException> outcomeReceiver) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PROFILE_URL_PREFETCH;
        if (feature.isSupportedByWebView()) {
            if (callbackExecutor == null) {
                callbackExecutor = new Handler(Looper.getMainLooper())::post;
            }
            mProfileImpl.prefetchUrl(url, cancellationSignal, callbackExecutor,
                    PrefetchOperationCallbackAdapter.buildInvocationHandler(outcomeReceiver));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Starts a URL prefetch request.
     * <p>
     * All WebViews associated with this Profile will use a URL request
     * matching algorithm during execution of all variants of
     * {@link android.webkit.WebView#loadUrl(String)} for determining if there
     * was already a prefetch request executed for the provided URL. This
     * includes prefetches that are "in progress". If a prefetch is matched,
     * WebView will leverage that for handling the URL, otherwise the URL
     * will be handled normally (i.e. through a network request).
     * <p>
     * Applications will still be responsible for calling
     * {@link android.webkit.WebView#loadUrl(String)} to display web contents
     * in a WebView.
     * <p>
     * NOTE: Additional headers passed to
     * {@link android.webkit.WebView#loadUrl(String, Map)} are not considered
     * in the matching algorithm for determining whether or not to serve a
     * prefetched response to a navigation.
     * <p>
     * For max latency saving benefits, it is recommended to call this method
     * as early as possible (i.e. before any WebView associated with this
     * profile is created).
     * <p>
     * Only supports HTTPS scheme.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#PROFILE_URL_PREFETCH}.
     *
     * @param url                the url associated with the prefetch request.
     * @param cancellationSignal will make the best effort to cancel an
     *                           in-flight prefetch request, However cancellation is not
     *                           guaranteed.
     * @param callbackExecutor   the executor to resolve the callback with. If
     *                           {@code null}, the callback will be executed on the
     *                           main thread.
     * @param prefetchParameters parameters to customize the prefetch request.
     * @param outcomeReceiver    callbacks for reporting result back to application.
     * @throws IllegalArgumentException      if the url or callback is null.
     * @throws UnsupportedOperationException if the {@link WebViewFeature#PROFILE_URL_PREFETCH}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @AnyThread
    @Profile.ExperimentalUrlPrefetch
    public void prefetchUrlAsync(
            @NonNull String url,
            @Nullable CancellationSignal cancellationSignal,
            @Nullable Executor callbackExecutor,
            @NonNull PrefetchParameters prefetchParameters,
            @NonNull WebViewOutcomeReceiver<@Nullable Void, PrefetchException> outcomeReceiver) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PROFILE_URL_PREFETCH;
        if (feature.isSupportedByWebView()) {
            if (callbackExecutor == null) {
                callbackExecutor = new Handler(Looper.getMainLooper())::post;
            }

            SpeculativeLoadingParameters params = new SpeculativeLoadingParameters(
                    prefetchParameters);

            InvocationHandler paramsBoundaryInterface =
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new SpeculativeLoadingParametersAdapter(params));

            mProfileImpl.prefetchUrl(url, cancellationSignal, callbackExecutor,
                    paramsBoundaryInterface,
                    PrefetchOperationCallbackAdapter.buildInvocationHandler(outcomeReceiver));

        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
