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

package androidx.appactions.interaction.capabilities.core.impl;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.proto.FulfillmentResponse;

/** An interface for receiving the result of action. */
public interface CallbackInternal {

    /** Invoke to set an action result upon success. */
    void onSuccess(@NonNull FulfillmentResponse fulfillmentResponse);

    default void onSuccess() {
        onSuccess(FulfillmentResponse.getDefaultInstance());
    }

    /** Invokes to set an error status for the action. */
    void onError(@NonNull ErrorStatusInternal errorStatus);
}
