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

import androidx.webkit.OutcomeReceiverCompat;
import androidx.webkit.PrefetchException;
import androidx.webkit.PrefetchNetworkException;
import androidx.webkit.Profile;

import org.chromium.support_lib_boundary.PrefetchOperationCallbackBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

public class PrefetchOperationCallbackAdapter {

    /**
     * Builds the PrefetchOperationCallback to send to the prefetch request.
     *
     * @param callback OutcomeReceiver to be triggered for the caller.
     * @return the built InvocationHandler
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull /* PrefetchOperationCallback */ InvocationHandler buildInvocationHandler(
            @NonNull OutcomeReceiverCompat<@Nullable Void, @NonNull PrefetchException> callback) {
        PrefetchOperationCallbackBoundaryInterface operationCallback =
                new PrefetchOperationCallbackBoundaryInterface() {
                    @Override
                    public void onSuccess() {
                        callback.onResult(null);
                    }

                    @Override
                    public void onFailure(@PrefetchExceptionTypeBoundaryInterface int type,
                            @NonNull String message, int networkErrorCode) {
                        if (type == PrefetchExceptionTypeBoundaryInterface.NETWORK) {
                            callback.onError(
                                    new PrefetchNetworkException(message, networkErrorCode));
                        } else {
                            callback.onError(new PrefetchException(message));
                        }
                    }
                };

        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                operationCallback);
    }
}
