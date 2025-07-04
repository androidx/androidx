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

import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallException

/**
 * Represents the UI-specific state for a single call item displayed on the In-Call screen.
 *
 * This data class transforms the raw call data (like [CallData]) into a format directly usable by
 * the UI composable responsible for rendering information and controls for one specific ongoing or
 * held call.
 */
data class InCallItemUiState(
    val callId: String,
    val attributes: CallAttributesCompat,
    val displayName: String,
    val callState: CallState,
    val isGloballyMuted: Boolean,
    val isSpeakerOn: Boolean = false,
    val isVideoCall: Boolean,
    val isCallActive: Boolean,
    val currentEndpoint: CallEndpointCompat?,
    val availableEndpoints: List<CallEndpointCompat>?,
    val hasCallException: CallException? = null,
    val isLoopbackActive: Boolean = false,
    val participants: List<ParticipantState> = emptyList(),
    val callIconData: IconData? = null,
    val isLocalCallSilenceEnabled: Boolean = false,
    val isLocallyMuted: Boolean = false,
    val isParticipantExtensionEnabled: Boolean = false,
    val isCallIconExtensionEnabled: Boolean = false,
)
