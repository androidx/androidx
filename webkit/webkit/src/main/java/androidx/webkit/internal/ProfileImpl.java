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

import androidx.webkit.OutcomeReceiverCompat;
import androidx.webkit.PrefetchException;
import androidx.webkit.Profile;
import androidx.webkit.SpeculativeLoadingConfig;
import androidx.webkit.SpeculativeLoadingParameters;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
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
            @Nullable CancellationSignal cancellationSignal,
            @NonNull Executor callbackExecutor,
            @NonNull SpeculativeLoadingParameters params,
            @NonNull OutcomeReceiverCompat<Void, PrefetchException> callback) {
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
            @Nullable CancellationSignal cancellationSignal,
            @NonNull Executor callbackExecutor,
            @NonNull OutcomeReceiverCompat<Void, PrefetchException> callback) {
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
    public void clearPrefetchAsync(@NonNull String url,
            @NonNull Executor callbackExecutor,
            @NonNull OutcomeReceiverCompat<Void, PrefetchException> callback) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PROFILE_URL_PREFETCH;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearPrefetch(url, callbackExecutor,
                    PrefetchOperationCallbackAdapter.buildInvocationHandler(callback));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Profile.ExperimentalUrlPrefetch
    @Override
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
    public void setOriginMatchedHeader(@NonNull String headerName,
            @NonNull String headerValue, @NonNull Set<String> originRules) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            if (mProfileImpl.hasOriginMatchedHeader(headerName)) {
                throw new IllegalStateException(
                        "Profile " + mProfileImpl.getName() + " already has origin-matched header: "
                                + headerName);
            }
            mProfileImpl.setOriginMatchedHeader(headerName, headerValue, originRules);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void clearOriginMatchedHeader(@NonNull String headerName) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearOriginMatchedHeader(headerName);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    public void clearAllOriginMatchedHeaders() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.ORIGIN_MATCHED_HEADERS;
        if (feature.isSupportedByWebView()) {
            mProfileImpl.clearAllOriginMatchedHeaders();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
