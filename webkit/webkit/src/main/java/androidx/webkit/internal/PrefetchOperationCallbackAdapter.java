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

package androidx.webkit.internal;

import androidx.webkit.PrefetchException;
import androidx.webkit.PrefetchNetworkException;
import androidx.webkit.Profile;
import androidx.webkit.WebViewOutcomeReceiver;

import org.chromium.support_lib_boundary.PrefetchOperationCallbackBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.chromium.support_lib_boundary.util.Features;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

@Profile.ExperimentalUrlPrefetch
public class PrefetchOperationCallbackAdapter implements
        PrefetchOperationCallbackBoundaryInterface {
    private final @NonNull WebViewOutcomeReceiver<@Nullable Void, @NonNull PrefetchException>
            mCallback;

    /**
     * @param callback OutcomeReceiver to be triggered for
     *                 {@link #buildInvocationHandler(WebViewOutcomeReceiver)}
     */
    private PrefetchOperationCallbackAdapter(@NonNull WebViewOutcomeReceiver<
            @Nullable Void, @NonNull PrefetchException> callback) {
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
            @NonNull WebViewOutcomeReceiver<@Nullable Void, @NonNull PrefetchException> callback
    ) {
        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                new PrefetchOperationCallbackAdapter(callback));
    }

    /**
     * Please use {@link #onResult(int)} instead.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onSuccess() {
        mCallback.onResult(null);
    }

    @Override
    public void onResult(
            @PrefetchResultTypeBoundaryInterface int type) {
        switch (type) {
            case PrefetchResultTypeBoundaryInterface.SUCCESS:
                mCallback.onResult(null);
                break;
            case PrefetchResultTypeBoundaryInterface.DUPLICATE:
                /*
                 * On earlier versions of the Chromium and AndroidX library,
                 * duplicate requests were reported as errors instead of success
                 * with a PrefetchException and "Duplicate prefetch request" here:
                 * https://chromium-review.googlesource.com/c/chromium/src/+/7664079
                 * aosp/3989873
                 */
                mCallback.onError(new PrefetchNetworkException("Duplicate prefetch request"));
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
    public void onFailure(
            @PrefetchExceptionTypeBoundaryInterface int type,
            @NonNull String message, int networkErrorCode) {
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
