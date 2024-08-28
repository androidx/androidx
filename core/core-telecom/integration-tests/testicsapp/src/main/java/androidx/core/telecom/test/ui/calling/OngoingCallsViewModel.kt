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

package androidx.core.telecom.test.ui.calling

import android.content.Context
import android.net.Uri
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallException
import androidx.core.telecom.test.services.AudioRoute
import androidx.core.telecom.test.services.CallAudioEndpoint
import androidx.core.telecom.test.services.CallData
import androidx.core.telecom.test.services.CallState
import androidx.core.telecom.test.services.Capability
import androidx.core.telecom.test.services.ParticipantExtensionData
import androidx.core.telecom.test.services.RemoteCallProvider
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.ViewModel
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ViewModel responsible for maintaining the connection to the [RemoteCallProvider] as well as
 * converting call/extension state to the associated UI specific state.
 */
class OngoingCallsViewModel(private val callProvider: RemoteCallProvider = RemoteCallProvider()) :
    ViewModel() {
    companion object {
        val UnknownAudioEndpoint =
            CallAudioEndpoint(id = "UNKNOWN", audioRoute = AudioRoute.UNKNOWN)
        val UnknownAudioUiState =
            AudioEndpointUiState(id = "UNKNOWN", name = "UNKNOWN", audioRoute = AudioRoute.UNKNOWN)
    }

    /** connect to the remote call provider with the given context */
    fun connectService(context: Context) {
        callProvider.connectService(context)
    }

    /** disconnect from the remote call provider */
    fun disconnectService() {
        callProvider.disconnectService()
    }

    /**
     * stream the [CallData] from the [RemoteCallProvider] when the service is connected and map
     * that data to associated [CallUiState].
     */
    fun streamCallData(context: Context): Flow<List<CallUiState>> {
        return callProvider.streamCallData().map { dataState ->
            dataState.map { mapToUiState(context, it) }
        }
    }

    /** Stream the global mute state of the device as long as the service is connected. */
    fun streamMuteData(): Flow<Boolean> {
        return callProvider.streamMuteData()
    }

    /**
     * Stream the current audio endpoint of the device as long as the service is connected and we
     * are in call.
     */
    fun streamCurrentEndpointAudioData(): Flow<AudioEndpointUiState> {
        return callProvider
            .streamCurrentEndpointData()
            .map { it ?: UnknownAudioEndpoint }
            .map(::mapToUiAudioState)
    }

    /**
     * Stream the available endpoints of the device as long as the service is connected and we are
     * in call.
     */
    fun streamAvailableEndpointAudioData(): Flow<List<AudioEndpointUiState>> {
        return callProvider
            .streamAvailableEndpointData()
            .map { it.map(::mapToUiAudioState) }
            .map { endpoints -> endpoints.sortedWith(compareBy({ it.audioRoute }, { it.name })) }
    }

    /**
     * Change the global mute state of the device
     *
     * @param isMuted true if the device should be muted, false otherwise
     */
    fun onChangeMuteState(isMuted: Boolean) {
        callProvider.onChangeMuteState(isMuted)
    }

    /**
     * Change the audio route of the active call
     *
     * @param id The ID of the endpoint from [AudioEndpointUiState.id]
     */
    suspend fun onChangeAudioRoute(id: String) {
        callProvider.onChangeAudioRoute(id)
    }

    /** Perform a map operation from [CallData] to [CallUiState] */
    private fun mapToUiState(context: Context, fullCallData: CallData): CallUiState {
        return CallUiState(
            id = fullCallData.callData.id,
            name = fullCallData.callData.contactName ?: fullCallData.callData.name,
            photo = fullCallData.callData.contactUri,
            number =
                formatPhoneNumber(
                    context,
                    fullCallData.callData.phoneAccountHandle,
                    fullCallData.callData.number
                ),
            state = fullCallData.callData.state,
            validTransition =
                getValidTransition(fullCallData.callData.state, fullCallData.callData.capabilities),
            direction = fullCallData.callData.direction,
            callType = fullCallData.callData.callType,
            onStateChanged = { fullCallData.callData.onStateChanged(it) },
            participantUiState = mapToUiParticipantExtension(fullCallData.participantExtensionData)
        )
    }

    /** Perform a map ooperation from [ParticipantExtensionData] to [ParticipantExtensionUiState] */
    @OptIn(ExperimentalAppActions::class)
    private fun mapToUiParticipantExtension(
        participantExtensionData: ParticipantExtensionData?
    ): ParticipantExtensionUiState? {
        if (participantExtensionData == null || !participantExtensionData.isSupported) return null
        return ParticipantExtensionUiState(
            isRaiseHandSupported =
                participantExtensionData.raiseHandData?.raiseHandAction?.isSupported ?: false,
            isKickParticipantSupported =
                participantExtensionData.kickParticipantData?.kickParticipantAction?.isSupported
                    ?: false,
            onRaiseHandStateChanged = {
                participantExtensionData.raiseHandData
                    ?.raiseHandAction
                    ?.requestRaisedHandStateChange(it)
            },
            participants = mapUiParticipants(participantExtensionData)
        )
    }

    /** map [ParticipantExtensionData] to [ParticipantExtensionUiState] */
    @OptIn(ExperimentalAppActions::class)
    private fun mapUiParticipants(
        participantExtensionData: ParticipantExtensionData
    ): List<ParticipantUiState> {
        return participantExtensionData.participants.map { p ->
            ParticipantUiState(
                name = p.name.toString(),
                isActive = participantExtensionData.activeParticipant == p,
                isSelf = participantExtensionData.selfParticipant?.id == p.id,
                isHandRaised =
                    participantExtensionData.raiseHandData?.raisedHands?.contains(p) ?: false,
                onKickParticipant = {
                    participantExtensionData.kickParticipantData
                        ?.kickParticipantAction
                        ?.requestKickParticipant(p)
                        ?: CallControlResult.Error(CallException.ERROR_CALL_IS_NOT_BEING_TRACKED)
                }
            )
        }
    }

    /** format the phone number to a user friendly form */
    private fun formatPhoneNumber(
        context: Context,
        phoneAccountHandle: PhoneAccountHandle,
        number: Uri
    ): String {
        val isTel = PhoneAccount.SCHEME_TEL == number.scheme
        if (!isTel) return number.schemeSpecificPart
        val tm: TelephonyManager? =
            context
                .getSystemService<TelephonyManager>()
                ?.createForPhoneAccountHandle(phoneAccountHandle)
        val iso = tm?.networkCountryIso ?: Locale.getDefault().country
        return PhoneNumberUtils.formatNumber(number.schemeSpecificPart, iso)
    }

    /** Determine the valid [CallStateTransition] based on [CallState] and call [Capability] */
    private fun getValidTransition(
        state: CallState,
        capabilities: List<Capability>
    ): CallStateTransition {
        return when (state) {
            CallState.INCOMING -> CallStateTransition.ANSWER
            CallState.DIALING -> CallStateTransition.NONE
            CallState.ACTIVE -> {
                if (capabilities.contains(Capability.SUPPORTS_HOLD)) {
                    CallStateTransition.HOLD
                } else {
                    CallStateTransition.NONE
                }
            }
            CallState.HELD -> CallStateTransition.UNHOLD
            CallState.DISCONNECTING -> CallStateTransition.NONE
            CallState.DISCONNECTED -> CallStateTransition.NONE
            CallState.UNKNOWN -> CallStateTransition.NONE
        }
    }

    /** Map from [CallAudioEndpoint] to [AudioEndpointUiState] */
    private fun mapToUiAudioState(endpoint: CallAudioEndpoint): AudioEndpointUiState {
        return AudioEndpointUiState(
            id = endpoint.id,
            name = endpoint.frameworkName ?: getAudioEndpointRouteName(endpoint.audioRoute),
            audioRoute = endpoint.audioRoute
        )
    }

    /** Get the user friendly endpoint route name */
    private fun getAudioEndpointRouteName(audioState: AudioRoute): String {
        return when (audioState) {
            AudioRoute.EARPIECE -> "Earpiece"
            AudioRoute.SPEAKER -> "Speaker"
            AudioRoute.HEADSET -> "Headset"
            AudioRoute.BLUETOOTH -> "Bluetooth"
            AudioRoute.STREAMING -> "Streaming"
            AudioRoute.UNKNOWN -> "Unknown"
        }
    }
}
