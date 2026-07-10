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

package androidx.webkit.internal;

import android.os.CancellationSignal;

import androidx.webkit.PrefetchException;
import androidx.webkit.PrefetchNetworkException;
import androidx.webkit.PrefetchResult;
import androidx.webkit.Profile;
import androidx.webkit.SpeculativeLoadingParameters;
import androidx.webkit.WebViewOutcomeReceiver;

import org.chromium.support_lib_boundary.PrefetchOperationCallbackBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.Executor;

/**
 * This Adapter is different from {@link PrefetchOperationCallbackAdapter} because we have added
 * {@link PrefetchResult} to the callback types, and we can not overload the {@code
 * buildInvocationHandler} with two different types.
 *
 * <p>
 * This new adapter is used for the newer versions and the older is kept for deprecated
 * {@link Profile#prefetchUrlAsync(String, CancellationSignal, Executor, WebViewOutcomeReceiver)},
 * {@link Profile#prefetchUrlAsync(String, CancellationSignal, Executor, SpeculativeLoadingParameters, WebViewOutcomeReceiver)} APIs
 */
@Profile.ExperimentalUrlPrefetch
public class PrefetchOperationCallbackWithResultAdapter implements
        PrefetchOperationCallbackBoundaryInterface {
    private final @NonNull WebViewOutcomeReceiver<@NonNull PrefetchResult,
            @NonNull PrefetchException>
            mCallback;

    /**
     * @param callback OutcomeReceiver to be triggered for
     *                 {@link #buildInvocationHandler(WebViewOutcomeReceiver)}
     */
    private PrefetchOperationCallbackWithResultAdapter(@NonNull WebViewOutcomeReceiver<
            @NonNull PrefetchResult, @NonNull PrefetchException> callback) {
        mCallback = callback;
    }

    /**
     * Builds the PrefetchOperationCallback to send to the prefetch request.
     *
     * @param callback the callback object used for prefetch operation.
     * @return the built InvocationHandler
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull /* PrefetchOperationCallbackBoundaryInterface
    */ InvocationHandler buildInvocationHandler(
            @NonNull WebViewOutcomeReceiver<
                    @NonNull PrefetchResult, @NonNull PrefetchException> callback) {
        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                new PrefetchOperationCallbackWithResultAdapter(callback));
    }

    /**
     * Maintained for compatibility with older WebViews
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onSuccess() {
        mCallback.onResult(new PrefetchResult(/* wasDuplicate: */ false));
    }

    @Override
    public void onResult(@PrefetchResultTypeBoundaryInterface int type) {
        switch (type) {
            case PrefetchResultTypeBoundaryInterface.SUCCESS:
                mCallback.onResult(new PrefetchResult(/* wasDuplicate: */ false));
                break;
            case PrefetchResultTypeBoundaryInterface.DUPLICATE:
                mCallback.onResult(new PrefetchResult(/* wasDuplicate: */ true));
                break;
            default:
                throw new IllegalArgumentException("Given type isn't defined.");
        }
    }

    @Override
    public String @NonNull [] getSupportedFeatures() {
        return new String[]{Features.PREFETCH_WITH_CALLBACK_RESULT_V1};
    }

    @Override
    public void onFailure(@PrefetchExceptionTypeBoundaryInterface int type,
            @NonNull String message, int networkErrorCode) {

        /*
         * On earlier versions of WebView, duplicate requests were reported as
         * errors instead of success, with this specific error message:
         * https://chromium-review.googlesource.com/c/chromium/src/+/7664079
         */
        ApiFeature.NoFrameworkInternal feature =
                WebViewFeatureInternal.PREFETCH_WITH_CALLBACK_RESULT;
        if (!feature.isSupportedByWebView()
                && "Duplicate prefetch request".equals(message)) {
            mCallback.onResult(new PrefetchResult(/* wasDuplicate: */ true));
            return;
        }

        switch (type) {
            case PrefetchExceptionTypeBoundaryInterface.NETWORK:
                mCallback.onError(new PrefetchNetworkException(message, networkErrorCode));
                break;
            default:
                mCallback.onError(new PrefetchException(message));
                break;
        }
    }
}
