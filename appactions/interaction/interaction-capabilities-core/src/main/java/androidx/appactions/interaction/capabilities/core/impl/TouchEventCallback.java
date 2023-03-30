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
import androidx.appactions.interaction.proto.TouchEventMetadata;

/**
 * An internal interface to allow the AppInteraction SDKs to be notified of results from processing
 * touch events.
 */
public interface TouchEventCallback {

    /** Results from a successful touch event invocation. */
    void onSuccess(
            @NonNull FulfillmentResponse fulfillmentResponse,
            @NonNull TouchEventMetadata touchEventMetadata);

    /** Results from an unsuccessful touch event invocation. */
    void onError(@NonNull ErrorStatusInternal errorStatus);
}
