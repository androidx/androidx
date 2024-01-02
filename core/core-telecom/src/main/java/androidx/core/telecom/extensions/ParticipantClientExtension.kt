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

package androidx.core.telecom.extensions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CallCompat
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.flow.StateFlow

// Adds extensions onto CallCompat to include participants support
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal object ParticipantClientExtension {
    internal fun createCapability(actions: Set<Int>): Capability {
        // Create the capability based on the user provided set of actions:
        // Todo: preprocess the actions for potential user inputted errors. This can be done by
        //  leveraging CapabilityExchangeUtils#preprocessSupportedActions
        val participantCapability = Capability()
        participantCapability.featureId = CallsManager.PARTICIPANT
        participantCapability.supportedActions = actions.toIntArray()

        return participantCapability
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal fun CallCompat.getParticipantActions(): Result<ParticipantClientActions> {
    return getParticipantClientActions()
}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal fun CallCompat.addParticipantsSupport(
    supportedActions: Set<Int>,
    onInitializationComplete: (ParticipantClientActions) -> Unit
) {
    addCapability(ParticipantClientExtension.createCapability(supportedActions))
    // pass onInitializationComplete through to be called once everything is fully setup:
    addExtension(onInitializationComplete)
}

// Allows the InCallService implementer to inspect state and perform requests to update state
internal interface ParticipantClientActions {
    val negotiatedActions: Set<Int>
    val isParticipantExtensionSupported: Boolean

    // incoming information from the voip app:
    val participantsStateFlow: StateFlow<Set<Participant>>
    val raisedHandsStateFlow: StateFlow<Set<Int>>
    val activeParticipantStateFlow: StateFlow<Int>

    // outgoing information to the voip app:
    suspend fun toggleHandRaised(isHandRaised: Boolean): CallControlResult
    suspend fun kickParticipant(participantId: Int): CallControlResult
}
