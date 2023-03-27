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

package androidx.appactions.interaction.capabilities.core.task.impl;

import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;

import com.google.auto.value.AutoValue;

/** Represents a fulfillment request coming from Assistant. */
@AutoValue
abstract class AssistantUpdateRequest {

    static AssistantUpdateRequest create(
            ArgumentsWrapper argumentsWrapper, CallbackInternal callbackInternal) {
        return new AutoValue_AssistantUpdateRequest(argumentsWrapper, callbackInternal);
    }

    /** The fulfillment request data. */
    abstract ArgumentsWrapper argumentsWrapper();

    /*
     * The callback to be report results from handling this request.
     */
    abstract CallbackInternal callbackInternal();
}
