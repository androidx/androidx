/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.internal

import androidx.core.telecom.CallEndpointCompat

/**
 * Represents all possible events that can modify the list of available call endpoints. This is an
 * internal implementation detail for the state management of the pre-call endpoint Flow.
 */
internal sealed class EndpointAction {
    data class Add(val endpoints: List<CallEndpointCompat>) : EndpointAction()

    data class Remove(val endpoints: List<CallEndpointCompat>) : EndpointAction()
}
