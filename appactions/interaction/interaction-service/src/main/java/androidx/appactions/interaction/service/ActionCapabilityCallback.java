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

package androidx.appactions.interaction.service;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;

/** Service implementation of the capability library execution callback. */
final class ActionCapabilityCallback implements CallbackInternal {

    private final Completer<FulfillmentResponse> mCompleter;

    ActionCapabilityCallback(@NonNull Completer<FulfillmentResponse> mCompleter) {
        this.mCompleter = mCompleter;
    }

    @Override
    public void onSuccess(@NonNull FulfillmentResponse fulfillmentResponse) {
        mCompleter.set(fulfillmentResponse);
    }

    @Override
    public void onError(@NonNull ErrorStatusInternal errorStatus) {
        mCompleter.setException(
                new CapabilityExecutionException(errorStatus, "Error executing action capability"));
    }
}
