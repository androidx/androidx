/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.webkit.internal;

import android.os.CancellationSignal;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerController;
import android.webkit.WebStorage;

import androidx.webkit.CustomHeader;
import androidx.webkit.PrefetchCache;
import androidx.webkit.PrefetchException;
import androidx.webkit.Profile;
import androidx.webkit.SpeculativeLoadingConfig;
import androidx.webkit.SpeculativeLoadingParameters;
import androidx.webkit.WebViewOutcomeReceiver;

import org.chromium.support_lib_boundary.OriginMatchedHeaderBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;


/**
 * Internal implementation of Profile.
 */
public class ProfileImpl implements Profile {

    private final @NonNull ProfileBoundaryInterface mProfileImpl;

    ProfileImpl(@NonNull ProfileBoundaryInterface profileImpl) {
        mProfileImpl = profileImpl;
    }

    @Override
    public @NonNull String getName() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getName();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull CookieManager getCookieManager() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getCookieManager();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull WebStorage getWebStorage() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getWebStorage();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
    public @NonNull PrefetchCache getPrefetchCache() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PREFETCH_CACHE;
        if (feature.isSupportedByWebView()) {
            return new PrefetchCache(mProfileImpl);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull GeolocationPermissions getGeolocationPermissions()
            throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getGeoLocationPermissions();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull ServiceWorkerController getServiceWorkerController()
            throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getServiceWorkerController();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
    public void prefetchUrlAsync(@NonNull String url,
            @Nullable CancellationSignal cancellationSignal, @NonNull Executor callbackExecutor,
            @NonNull SpeculativeLoadingParameters params,
            @NonNull WebViewOutcomeReceiver<Void, PrefetchException> callback) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PROFILE_URL_PREFETCH;
        if (feature.isSupportedByWebView()) {
            InvocationHandler paramsBoundaryInterface =
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new SpeculativeLoadingParametersAdapter(params));

            mProfileImpl.prefetchUrl(url, cancellationSignal, callbackExecutor,
                    paramsBoundaryInterface,
                    PrefetchOperationCallbackAdapter.buildInvocationHandler(callback));

        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
    public void prefetchUrlAsync(@NonNull String url,
            @Nullable CancellationSignal cancellationSignal, @NonNull Executor callbackExecutor,
            @NonNull WebViewOutcomeReceiver<Void, PrefetchException> callback) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PROFILE_URL_PREFETCH;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.prefetchUrl(url, cancellationSignal, callbackExecutor,
                    PrefetchOperationCallbackAdapter.buildInvocationHandler(callback));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
    @SuppressWarnings("removal")
    public void setSpeculativeLoadingConfig(
            @NonNull SpeculativeLoadingConfig speculativeLoadingConfig) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.SPECULATIVE_LOADING_CONFIG;
        if (feature.isSupportedByWebView()) {
            InvocationHandler configInvocation =
                    BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                            new SpeculativeLoadingConfigAdapter(speculativeLoadingConfig));
            mProfileImpl.setSpeculativeLoadingConfig(configInvocation);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
    public void setMaxPrerenders(@Nullable Integer maxPrerenders) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.SET_MAX_PRERENDERS;
        if (feature.isSupportedByWebView()) {
            if (maxPrerenders != null && maxPrerenders < 1) {
                throw new IllegalArgumentException(
                        "maxPrerenders should be greater than or equal to 1");
            }
            mProfileImpl.setMaxPrerenders(maxPrerenders);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalWarmUpRendererProcess
    public void warmUpRendererProcess() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.WARM_UP_RENDERER_PROCESS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.warmUpRendererProcess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalOriginMatchedHeader
    @SuppressWarnings("deprecation")
    public void setOriginMatchedHeader(@NonNull String headerName, @NonNull String headerValue,
            @NonNull Set<String> originRules) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.setOriginMatchedHeader(headerName, headerValue, originRules);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalOriginMatchedHeader
    public boolean hasOriginMatchedHeader(@NonNull String headerName) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.hasOriginMatchedHeader(headerName);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalOriginMatchedHeader
    @SuppressWarnings("deprecation")
    public void clearOriginMatchedHeader(@NonNull String headerName) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearOriginMatchedHeader(headerName);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalOriginMatchedHeader
    public void clearAllOriginMatchedHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearAllOriginMatchedHeaders();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void addCustomHeader(@NonNull CustomHeader header) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.addOriginMatchedHeader(header.getName(), header.getValue(),
                    header.getRules());
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public boolean hasCustomHeader(@NonNull String headerName) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.hasOriginMatchedHeader(headerName);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull Set<CustomHeader> getCustomHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            return getCustomHeadersInternal(null, null);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull Set<CustomHeader> getCustomHeaders(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            return getCustomHeadersInternal(name, null);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public @NonNull Set<CustomHeader> getCustomHeaders(@NonNull String name,
            @NonNull String value) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            return getCustomHeadersInternal(name, value);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private @NonNull Set<CustomHeader> getCustomHeadersInternal(@Nullable String name,
            @Nullable String value) {
        HashSet<CustomHeader> headers = new HashSet<>();
        List<InvocationHandler> originMatchedHeaders =
                mProfileImpl.getOriginMatchedHeaders(name, value);
        for (InvocationHandler handler : originMatchedHeaders) {
            OriginMatchedHeaderBoundaryInterface boundaryObject =
                    BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                            OriginMatchedHeaderBoundaryInterface.class, handler);
            assert boundaryObject != null;
            headers.add(new CustomHeader(boundaryObject.getName(), boundaryObject.getValue(),
                    boundaryObject.getRules()));
        }
        return headers;
    }

    @Override
    public void clearCustomHeader(@NonNull String headerName) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearOriginMatchedHeader(headerName, null);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void clearCustomHeader(@NonNull String headerName, @NonNull String headerValue) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearOriginMatchedHeader(headerName, headerValue);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void clearAllCustomHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.CUSTOM_REQUEST_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearAllOriginMatchedHeaders();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalPreconnect
    public void preconnect(@NonNull String url) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PRECONNECT;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.preconnect(url);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @ExperimentalAddQuicHints
    public void addQuicHints(@NonNull Set<String> urls) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ADD_QUIC_HINTS_V1;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.addQuicHints(urls);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
