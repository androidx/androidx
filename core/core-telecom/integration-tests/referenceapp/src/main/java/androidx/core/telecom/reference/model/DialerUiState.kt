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

package androidx.core.telecom.reference.model

import androidx.core.telecom.CallEndpointCompat

/**
 * Represents the observable state of the Dialer user interface.
 *
 * This data class holds all the information needed by the UI composables to render the dialer
 * screen, including user input, call options, and any potential error messages to display.
 */
data class DialerUiState(
    val phoneNumber: String = "",
    val displayName: String = "",
    val isVideoCall: Boolean = false,
    val canHold: Boolean = true,
    val errorMessage: String? = null,
    val availableEndpoints: List<CallEndpointCompat> = emptyList(),
    val selectedEndpoint: CallEndpointCompat? = null,
    val isFetchingEndpoints: Boolean = false,
)
