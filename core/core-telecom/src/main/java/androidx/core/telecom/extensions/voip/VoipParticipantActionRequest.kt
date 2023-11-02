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

package androidx.core.telecom.extensions.voip

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.IActionsResultCallback
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * This class helps handle callback action requests which will be queued up to a channel
 * so that order of the requests will be preserved.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
internal class VoipParticipantActionRequest(
    private val session: CallControlScope,
    private val action: @CallsManager.Companion.ExtensionSupportedActions Int,
    private val cb: IActionsResultCallback?,
    private val participantId: Int = CapabilityExchangeUtils.NULL_PARTICIPANT_ID
) {

    companion object {
        private val TAG = VoipParticipantActionRequest::class.simpleName
    }

    /**
     * Process the passed in action from the "queue".
     */
    internal fun processAction() {
        when (action) {
            CallsManager.RAISE_HAND_ACTION -> {
                processToggleHandRaised()
            }
            CallsManager.KICK_PARTICIPANT_ACTION -> {
                processKickParticipant()
            }
            else -> Log.i(TAG, "$action action is not supported (ignoring request).")
        }
    }

    /***********************************************************************************************
     *                           Private Helpers
     *********************************************************************************************/

    /**
     * Process toggle hand raised action. If the state is not defined on the VOIP side or the
     * participant cannot be found, a failure result will be propagated to the ICS.
     */
    private fun processToggleHandRaised() {
        // VOIP side hasn't defined (or doesn't support) a raised hands state for the participants.
        if (session.raisedHandParticipants == null) {
            cb?.onFailure(CapabilityExchangeUtils.VOIP_SERVER_ERROR,
                "Unexpected error on VOIP side. The associated state is not defined.")
            return
        }

        val participantToToggleRaiseHand = findParticipant()
        // Unable to find participant.
        if (participantToToggleRaiseHand == null) {
            cb?.onFailure(CapabilityExchangeUtils.PARTICIPANT_NOT_FOUND_ERROR,
                "Unable to raise hand for non-existent participant with id: $participantId")
            return
        }

        // Toggle raise hand state for the given participant.
        session.raisedHandParticipants!!.value = toggleRaiseHand(
            session.raisedHandParticipants!!.value.toMutableSet(),
            participantToToggleRaiseHand)
        cb?.onSuccess()
    }

    private fun toggleRaiseHand(
        raisedHandsState: MutableSet<Participant>,
        participant: Participant
    ): MutableSet<Participant> {
        if (raisedHandsState.contains(participant)) {
            raisedHandsState.remove(participant)
        } else {
            raisedHandsState.add(participant)
        }
        return raisedHandsState
    }

    /**
     * Process kick participant action. If the state is not defined on the VOIP side or the
     * participant cannot be found, a failure result will be propagated to the ICS.
     */
    private fun processKickParticipant() {
        // VOIP side hasn't defined (or doesn't support) a raised hands state for the participants.
        if (session.participants == null) {
            cb?.onFailure(CapabilityExchangeUtils.VOIP_SERVER_ERROR,
                "Unexpected error on VOIP side. Participants state is not defined.")
            return
        }

        val participantToKick = findParticipant()
        // Unable to find participant.
        if (participantToKick == null) {
            cb?.onFailure(CapabilityExchangeUtils.PARTICIPANT_NOT_FOUND_ERROR,
                "Unable to raise hand for non-existent participant with id: $participantId")
            return
        }

        // Kick the given participant.
        val currentParticipantsState = session.participants!!.value.toMutableSet()
        // This will always be true.
        currentParticipantsState.remove(participantToKick)
        session.participants!!.value = currentParticipantsState
        cb?.onSuccess()
    }

    /**
     * Find the participant in the participants flow pointing to participantId, or null, if not
     * found.
     */
    private fun findParticipant(): Participant? {
        session.participants?.let {
            for (participant in it.value) {
                if (participant.id == participantId) {
                    return participant
                }
            }
        }
        return null
    }
}
