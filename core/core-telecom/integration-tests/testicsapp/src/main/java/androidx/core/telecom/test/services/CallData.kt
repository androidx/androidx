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

package androidx.core.telecom.test.services

import android.net.Uri
import android.telecom.PhoneAccountHandle
import androidx.core.telecom.extensions.KickParticipantAction
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.RaiseHandAction
import androidx.core.telecom.test.ui.calling.CallStateTransition
import androidx.core.telecom.util.ExperimentalAppActions

enum class CallState {
    INCOMING,
    DIALING,
    ACTIVE,
    HELD,
    DISCONNECTING,
    DISCONNECTED,
    UNKNOWN
}

enum class Direction {
    INCOMING,
    OUTGOING
}

enum class AudioRoute {
    UNKNOWN,
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    HEADSET,
    STREAMING
}

enum class CallType {
    AUDIO,
    VIDEO
}

enum class Capability {
    SUPPORTS_HOLD
}

/** Base relevant call data */
data class BaseCallData(
    val id: Int,
    val phoneAccountHandle: PhoneAccountHandle,
    val name: String,
    val contactName: String?,
    val contactUri: Uri?,
    val number: Uri,
    val state: CallState,
    val direction: Direction,
    val callType: CallType,
    val capabilities: List<Capability>,
    val onStateChanged: (transition: CallStateTransition) -> Unit
)

/** Represents a call endpoint from the application's perspective */
data class CallAudioEndpoint(
    val id: String,
    val audioRoute: AudioRoute,
    val frameworkName: String? = null
)

/** data related to the extensions to the call */
@OptIn(ExperimentalAppActions::class)
data class ParticipantExtensionData(
    val isSupported: Boolean,
    val activeParticipant: Participant?,
    val selfParticipant: Participant?,
    val participants: Set<Participant>,
    val raiseHandData: RaiseHandData? = null,
    val kickParticipantData: KickParticipantData? = null
)

@OptIn(ExperimentalAppActions::class)
data class RaiseHandData(val raisedHands: List<Participant>, val raiseHandAction: RaiseHandAction)

@OptIn(ExperimentalAppActions::class)
data class KickParticipantData(val kickParticipantAction: KickParticipantAction)

/** Combined call data including extensions. */
data class CallData(
    val callData: BaseCallData,
    val participantExtensionData: ParticipantExtensionData?
)
