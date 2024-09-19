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
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallException.Companion.ERROR_CALL_IS_NOT_BEING_TRACKED
import androidx.core.telecom.extensions.KickParticipantAction
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.RaiseHandAction
import androidx.core.telecom.test.Compatibility
import androidx.core.telecom.test.ui.calling.CallStateTransition
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Track the kick participant support for this application */
@OptIn(ExperimentalAppActions::class)
class KickParticipantDataEmitter {
    companion object {
        private val unsupportedAction =
            object : KickParticipantAction {
                override var isSupported: Boolean = false

                override suspend fun requestKickParticipant(
                    participant: Participant
                ): CallControlResult {
                    return CallControlResult.Error(ERROR_CALL_IS_NOT_BEING_TRACKED)
                }
            }
        /** Implementation used when kicking participants is unsupported */
        val UNSUPPORTED = KickParticipantDataEmitter().collect(unsupportedAction)
    }

    /** Collect updates to [KickParticipantData] related to the call */
    fun collect(action: KickParticipantAction): Flow<KickParticipantData> {
        return flowOf(createKickParticipantData(action))
    }

    private fun createKickParticipantData(action: KickParticipantAction): KickParticipantData {
        return KickParticipantData(action)
    }
}

/** Track the raised hands state of participants in the call */
@OptIn(ExperimentalAppActions::class)
class RaiseHandDataEmitter {
    companion object {
        private val unsupportedAction =
            object : RaiseHandAction {
                override var isSupported: Boolean = false

                override suspend fun requestRaisedHandStateChange(
                    isRaised: Boolean
                ): CallControlResult {
                    return CallControlResult.Error(ERROR_CALL_IS_NOT_BEING_TRACKED)
                }
            }
        /** The implementation used when not supported */
        val UNSUPPORTED = RaiseHandDataEmitter().collect(unsupportedAction)
    }

    private val raisedHands: MutableStateFlow<List<Participant>> = MutableStateFlow(emptyList())

    /** The raised hands state of the participants has changed */
    fun onRaisedHandsChanged(newRaisedHands: List<Participant>) {
        raisedHands.value = newRaisedHands
    }

    /** Collect updates to the [RaiseHandData] related to this call */
    fun collect(action: RaiseHandAction): Flow<RaiseHandData> {
        return raisedHands.map { raisedHands -> createRaiseHandData(action, raisedHands) }
    }

    private fun createRaiseHandData(
        action: RaiseHandAction,
        raisedHands: List<Participant>
    ): RaiseHandData {
        return RaiseHandData(raisedHands, action)
    }
}

/**
 * Track and update listeners when the [ParticipantExtensionData] related to a call changes,
 * including the optional raise hand and kick participant extensions.
 */
@OptIn(ExperimentalAppActions::class)
class ParticipantExtensionDataEmitter {
    private val activeParticipant: MutableStateFlow<Participant?> = MutableStateFlow(null)
    private val participants: MutableStateFlow<Set<Participant>> = MutableStateFlow(emptySet())

    /** The participants in the call have changed */
    fun onParticipantsChanged(newParticipants: Set<Participant>) {
        participants.value = newParticipants
    }

    /** The active participant in the call has changed */
    fun onActiveParticipantChanged(participant: Participant?) {
        activeParticipant.value = participant
    }

    /**
     * Collect updates to the [ParticipantExtensionData] related to this call based on the support
     * state of this extension + actions
     */
    fun collect(
        isSupported: Boolean,
        raiseHandDataEmitter: Flow<RaiseHandData> = RaiseHandDataEmitter.UNSUPPORTED,
        kickParticipantDataEmitter: Flow<KickParticipantData> =
            KickParticipantDataEmitter.UNSUPPORTED
    ): Flow<ParticipantExtensionData> {
        return participants
            .combine(activeParticipant) { newParticipants, newActiveParticipant ->
                createExtensionData(isSupported, newActiveParticipant, newParticipants)
            }
            .combine(raiseHandDataEmitter) { data, rhData ->
                ParticipantExtensionData(
                    isSupported = data.isSupported,
                    activeParticipant = data.activeParticipant,
                    selfParticipant = data.selfParticipant,
                    participants = data.participants,
                    raiseHandData = rhData,
                    kickParticipantData = data.kickParticipantData
                )
            }
            .combine(kickParticipantDataEmitter) { data, kpData ->
                ParticipantExtensionData(
                    isSupported = data.isSupported,
                    activeParticipant = data.activeParticipant,
                    selfParticipant = data.selfParticipant,
                    participants = data.participants,
                    raiseHandData = data.raiseHandData,
                    kickParticipantData = kpData
                )
            }
    }

    private fun createExtensionData(
        isSupported: Boolean,
        activeParticipant: Participant? = null,
        participants: Set<Participant> = emptySet()
    ): ParticipantExtensionData {
        // For now, the first element is considered ourself
        val self = participants.firstOrNull()
        return ParticipantExtensionData(isSupported, activeParticipant, self, participants)
    }
}

/**
 * Track a [Call] and begin to stream [BaseCallData] using [collect] whenever the call data changes.
 */
class CallDataEmitter(val trackedCall: IcsCall) {
    private companion object {
        const val LOG_TAG = "CallDataProducer"
    }

    /** Collect on changes to the [BaseCallData] related to the [trackedCall] */
    fun collect(): Flow<BaseCallData> {
        return createCallDataFlow()
    }

    private fun createCallDataFlow(): Flow<BaseCallData> = callbackFlow {
        val callback =
            object : Call.Callback() {
                override fun onStateChanged(call: Call?, state: Int) {
                    if (call != trackedCall.call) return
                    val callData = createCallData(trackedCall)
                    Log.v(LOG_TAG, "onStateChanged: call ${trackedCall.id}: $callData")
                    trySendBlocking(callData)
                }

                override fun onDetailsChanged(call: Call?, details: Call.Details?) {
                    if (call != trackedCall.call) return
                    val callData = createCallData(trackedCall)
                    Log.v(LOG_TAG, "onDetailsChanged: call ${trackedCall.id}: $callData")
                    trySendBlocking(callData)
                }

                override fun onCallDestroyed(call: Call?) {
                    if (call != trackedCall.call) return
                    Log.v(LOG_TAG, "call ${trackedCall.id}: destroyed")
                    channel.close()
                }
            }
        if (trackedCall.call.details != null) {
            val callData = createCallData(trackedCall)
            Log.v(LOG_TAG, "call ${trackedCall.id}: $callData")
            trySendBlocking(callData)
        }
        trackedCall.call.registerCallback(callback)
        awaitClose { trackedCall.call.unregisterCallback(callback) }
    }

    private fun createCallData(icsCall: IcsCall): BaseCallData {
        return BaseCallData(
            id = icsCall.id,
            phoneAccountHandle = icsCall.call.details.accountHandle,
            name =
                when (icsCall.call.details.callerDisplayNamePresentation) {
                    TelecomManager.PRESENTATION_ALLOWED ->
                        icsCall.call.details.callerDisplayName ?: ""
                    TelecomManager.PRESENTATION_RESTRICTED -> "Restricted"
                    TelecomManager.PRESENTATION_UNKNOWN -> "Unknown"
                    else -> icsCall.call.details.callerDisplayName ?: ""
                },
            contactName = Compatibility.getContactDisplayName(icsCall.call.details),
            contactUri = Compatibility.getContactPhotoUri(icsCall.call.details),
            number = icsCall.call.details.handle ?: Uri.parse("unknown:UNKNOWN_ID_${icsCall.id}"),
            state = getState(Compatibility.getCallState(icsCall.call)),
            direction =
                when (icsCall.call.details.callDirection) {
                    Call.Details.DIRECTION_INCOMING -> Direction.INCOMING
                    else -> Direction.OUTGOING
                },
            callType =
                when (VideoProfile.isVideo(icsCall.call.details.videoState)) {
                    true -> CallType.VIDEO
                    false -> CallType.AUDIO
                },
            capabilities = getCapabilities(icsCall.call.details.callCapabilities),
            onStateChanged = ::onChangeCallState
        )
    }

    private fun onChangeCallState(transition: CallStateTransition) {
        when (transition) {
            CallStateTransition.HOLD -> trackedCall.call.hold()
            CallStateTransition.UNHOLD -> trackedCall.call.unhold()
            CallStateTransition.ANSWER -> trackedCall.call.answer(VideoProfile.STATE_AUDIO_ONLY)
            CallStateTransition.DISCONNECT -> trackedCall.call.disconnect()
            CallStateTransition.NONE -> {}
        }
    }

    private fun getState(telecomState: Int): CallState {
        return when (telecomState) {
            Call.STATE_RINGING -> CallState.INCOMING
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HELD
            Call.STATE_DISCONNECTING -> CallState.DISCONNECTING
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.UNKNOWN
        }
    }

    private fun getCapabilities(capabilities: Int): List<Capability> {
        val capabilitiesList = ArrayList<Capability>()
        if (canHold(capabilities)) {
            capabilitiesList.add(Capability.SUPPORTS_HOLD)
        }
        return capabilitiesList
    }

    private fun canHold(capabilities: Int): Boolean {
        return (Call.Details.CAPABILITY_HOLD and capabilities) > 0
    }
}
