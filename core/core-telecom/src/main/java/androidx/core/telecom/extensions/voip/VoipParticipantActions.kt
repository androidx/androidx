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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.IActionsResultCallback
import androidx.core.telecom.extensions.IParticipantActions
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Define Participant action callbacks for ICS to send requests back to the VOIP app.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
internal class VoipParticipantActions(
    private val session: CallControlScope,
    private val callChannels: CallChannels,
    private val voipSupportedActions: Set<@CallsManager.Companion.ExtensionSupportedActions Int>,
    // To be set by ICS for handling hand raise functionality.
    private var participantId: Int = CapabilityExchangeUtils.NULL_PARTICIPANT_ID
) : IParticipantActions.Stub() {

    // Todo: For each of the participant actions provided to the ICS, we need to make sure the version
    // is accounted to update the right methods/channel.
    override fun toggleHandRaised(cb: IActionsResultCallback?) {
        if (voipSupportedActions.contains(CallsManager.RAISE_HAND_ACTION)) {
            val actionRequest = VoipParticipantActionRequest(session,
                CallsManager.RAISE_HAND_ACTION, cb, participantId)
            callChannels.voipParticipantActionRequestsChannel.trySend(actionRequest)
        } else {
            cb?.onFailure(CapabilityExchangeUtils.VOIP_ACTION_NOT_SUPPORTED_ERROR,
                "VOIP app does not support raise hand action")
        }
    }

    override fun kickParticipant(participantIdToKick: Int, cb: IActionsResultCallback?) {
        if (voipSupportedActions.contains(CallsManager.KICK_PARTICIPANT_ACTION)) {
            val actionRequest = VoipParticipantActionRequest(session,
                CallsManager.KICK_PARTICIPANT_ACTION, cb, participantIdToKick)
            callChannels.voipParticipantActionRequestsChannel.trySend(actionRequest)
        } else {
            cb?.onFailure(CapabilityExchangeUtils.VOIP_ACTION_NOT_SUPPORTED_ERROR,
                "VOIP app does not support raise hand action")
        }
    }
}
