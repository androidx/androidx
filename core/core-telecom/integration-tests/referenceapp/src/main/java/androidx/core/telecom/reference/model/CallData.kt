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
import androidx.core.telecom.extensions.CallIconExtension
import androidx.core.telecom.extensions.LocalCallSilenceExtension
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Represents the complete state and attributes of a single ongoing or recently completed call.
 *
 * This data class holds both the static information defined when the call was initiated (like
 * attributes) and the dynamic, mutable state that changes during the call's lifecycle (like call
 * state, mute status, endpoints, and potential errors).
 */
data class CallData
@OptIn(ExperimentalAppActions::class)
constructor(
    val callId: String,
    val attributes: CallAttributesCompat,
    var callState: CallState,
    val isGloballyMuted: Boolean,
    val currentEndpoint: CallEndpointCompat?,
    val availableEndpoints: List<CallEndpointCompat>?,
    val callException: CallException?,
    /* Participant Extension - This data is all optional and provides info on participants
     * in a call or meeting with the optional ability to raise hands or kick them */
    val isParticipantExtensionEnabled: Boolean = false,
    val participants: List<ParticipantState> = emptyList(),
    val participantExtension: ParticipantControl? = null,
    /* Local Call Silence Extension - This data is all optional and provides info on a per call
     * local silence that can silence the call at the app level instead of the platform level.
     * This allows apps to process audio data to do stuff like inform the user they are talking
     * into the mic when it is muted. */
    val isLocalCallSilenceEnabled: Boolean = false,
    val isLocallyMuted: Boolean = false,
    val localCallSilenceExtension: LocalCallSilenceExtension? = null,
    /* Call Icon Extension - This data is all optional and allows voip apps to share a call
     * icon with remote surfaces (e.g. watch face, auto, etc.) */
    val isCallIconExtensionEnabled: Boolean = false,
    val iconData: IconData? = null,
    val callIconExtension: CallIconExtension? = null,
)
