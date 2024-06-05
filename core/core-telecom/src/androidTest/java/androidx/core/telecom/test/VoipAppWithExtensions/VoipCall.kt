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

package androidx.core.telecom.test.VoipAppWithExtensions

import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.ExtensionInitializationScope
import androidx.core.telecom.extensions.ParticipantExtension
import androidx.core.telecom.extensions.RaiseHandActionState
import androidx.core.telecom.extensions.addCallWithExtensions
import androidx.core.telecom.extensions.addKickParticipantAction
import androidx.core.telecom.extensions.addParticipantExtension
import androidx.core.telecom.extensions.addRaiseHandAction
import androidx.core.telecom.test.ITestAppControlCallback
import androidx.core.telecom.util.ExperimentalAppActions

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
class VoipCall(
    private val callsManager: CallsManager,
    private val callback: ITestAppControlCallback?,
    private val capabilities: List<Capability>
) {
    private lateinit var callId: String
    // Participant state updaters
    var participantStateUpdater: ParticipantExtension? = null
    var raiseHandStateUpdater: RaiseHandActionState? = null

    suspend fun addCall(
        callAttributes: CallAttributesCompat,
        onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
        onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        onSetActive: suspend () -> Unit,
        onSetInactive: suspend () -> Unit,
        init: suspend CallControlScope.() -> Unit
    ) {
        callsManager.addCallWithExtensions(
            callAttributes,
            onAnswer,
            onDisconnect,
            onSetActive,
            onSetInactive
        ) {
            createExtensions()
            onCall {
                callId = getCallId().toString()
                init.invoke(this)
            }
        }
    }

    private fun ExtensionInitializationScope.createExtensions() {
        for (capability in capabilities) {
            when (capability.featureId) {
                CallsManager.PARTICIPANT -> {
                    participantStateUpdater = addParticipantExtension()
                    participantStateUpdater!!.initializeActions(capability)
                }
                CallsManager.CALL_ICON -> {
                    TODO("Implement call icons once implemented")
                }
                CallsManager.CALL_SILENCE -> {
                    TODO("implement call silence once implemented")
                }
            }
        }
    }

    private fun ParticipantExtension.initializeActions(capability: Capability) {
        for (action in capability.supportedActions) {
            when (action) {
                CallsManager.RAISE_HAND_ACTION -> {
                    raiseHandStateUpdater = addRaiseHandAction {
                        callback?.raiseHandStateAction(callId, it)
                    }
                }
                CallsManager.KICK_PARTICIPANT_ACTION -> {
                    addKickParticipantAction { callback?.kickParticipantAction(callId, it) }
                }
            }
        }
    }
}
