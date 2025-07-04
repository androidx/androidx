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

package androidx.core.telecom.reference.viewModel

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.AudioLoopbackManager
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.model.CallData
import androidx.core.telecom.reference.model.CallState
import androidx.core.telecom.reference.model.InCallItemUiState
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalAppActions::class)
class InCallViewModel(
    private val context: Context,
    private val callRepository: CallRepository = CallRepository(),
) : ViewModel() {

    private val _activeLoopbackCallId = MutableStateFlow<String?>(null)
    private val audioLoopbackManager = AudioLoopbackManager(context)
    private val audioManager: AudioManager by lazy {
        context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    init {
        observeCallStateForLoopback()
    }

    val uiState: StateFlow<List<InCallItemUiState>> =
        callRepository.callDataFlow
            .combine(_activeLoopbackCallId) { calls, activeLoopbackId ->
                calls.map { call ->
                    val currentCallId = call.callId.toString()
                    InCallItemUiState(
                        callId = currentCallId,
                        attributes = call.attributes,
                        displayName = call.attributes.displayName.toString(),
                        callState = call.callState,
                        isGloballyMuted = call.isGloballyMuted,
                        isVideoCall = call.attributes.callType == CALL_TYPE_VIDEO_CALL,
                        isCallActive = call.callState == CallState.ACTIVE,
                        currentEndpoint = call.currentEndpoint,
                        availableEndpoints = call.availableEndpoints,
                        hasCallException = call.callException,
                        isLoopbackActive = currentCallId == activeLoopbackId,
                        isParticipantExtensionEnabled = call.isParticipantExtensionEnabled,
                        participants = call.participants,
                        isLocalCallSilenceEnabled = call.isLocalCallSilenceEnabled,
                        isCallIconExtensionEnabled = call.isCallIconExtensionEnabled,
                        callIconData = call.iconData,
                        isLocallyMuted = call.isLocallyMuted,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private fun observeCallStateForLoopback() {
        viewModelScope.launch {
            callRepository.callDataFlow.collect { calls ->
                val currentLoopbackCallId = _activeLoopbackCallId.value
                val eligibleCall: CallData? =
                    calls.firstOrNull {
                        it.callId.toString() ==
                            currentLoopbackCallId || // Prioritize current loopback call
                            (currentLoopbackCallId == null &&
                                it.callState == CallState.ACTIVE) // Or first active
                    }
                        ?: calls.firstOrNull {
                            it.callState == CallState.ACTIVE
                        } // Fallback to any active

                var effectiveMuteForLoopback =
                    audioManager.isMicrophoneMute // Start with global mute

                if (eligibleCall != null) {
                    // If the specific call eligible for loopback is locally silenced,
                    // then for loopback purposes, it's considered muted.
                    effectiveMuteForLoopback =
                        effectiveMuteForLoopback || eligibleCall.isLocallyMuted
                }

                Log.v(
                    TAG,
                    "Loopback Observer: Calls (${calls.size}), " +
                        "EligibleCall: ${eligibleCall?.callId}, " +
                        "GlobalMute: ${audioManager.isMicrophoneMute}, " +
                        "EligibleCallLocallyMuted: ${eligibleCall?.isLocallyMuted}, " +
                        "EffectiveMuteForLoopback: $effectiveMuteForLoopback",
                )

                val desiredLoopbackCallId: String? =
                    if (
                        eligibleCall != null &&
                            eligibleCall.callState == CallState.ACTIVE &&
                            !effectiveMuteForLoopback
                    ) {
                        eligibleCall.callId.toString() // Use toString() for consistency
                    } else {
                        null
                    }
                Log.v(
                    TAG,
                    "Loopback Observer: Desired ID: $desiredLoopbackCallId, Current ID: $currentLoopbackCallId",
                )

                if (
                    desiredLoopbackCallId != null && currentLoopbackCallId != desiredLoopbackCallId
                ) {
                    Log.d(
                        TAG,
                        "Loopback Observer: Condition met to START for $desiredLoopbackCallId",
                    )
                    if (currentLoopbackCallId != null) {
                        stopAudioLoopbackInternal()
                    }
                    // Permission check (assuming it's handled by DialerActivity on app start)
                    if (hasRecordAudioPermission()) {
                        startAudioLoopbackInternal(desiredLoopbackCallId)
                    } else {
                        Log.e(
                            TAG,
                            "Loopback: RECORD_AUDIO permission missing for $desiredLoopbackCallId! Should be granted on app start.",
                        )
                    }
                } else if (desiredLoopbackCallId == null && currentLoopbackCallId != null) {
                    Log.d(
                        TAG,
                        "Loopback Observer: Condition met to STOP for $currentLoopbackCallId",
                    )
                    stopAudioLoopbackInternal()
                }
            }
        }
    }

    fun holdCall(callId: String) {
        viewModelScope.launch { callRepository.setCallInactive(callId) }
    }

    fun endCall(callId: String) {
        viewModelScope.launch { callRepository.endCall(callId) }
    }

    fun setCallActive(callId: String) {
        viewModelScope.launch { callRepository.setCallActive(callId) }
    }

    fun toggleGlobalMute(isMuted: Boolean) {
        Log.d(TAG, "Requesting toggleGlobalMute: isMuted = $isMuted")
        maybeStopAudioLoopback(isMuted)
        callRepository.toggleGlobalMute(isMuted)
    }

    fun toggleLocalCallSilence(callId: String, isMuted: Boolean) {
        Log.d(TAG, "Requesting toggleLocalMute: isMuted = $isMuted")
        maybeStopAudioLoopback(isMuted)
        callRepository.toggleLocalCallSilence(callId, isMuted)
    }

    fun maybeStopAudioLoopback(isMuted: Boolean) {
        // Immediately stop loopback if muting
        if (isMuted && _activeLoopbackCallId.value != null) {
            Log.i(TAG, "Mute toggled ON while loopback active. " + "Stopping loopback immediately.")
            stopAudioLoopbackInternal()
        }
    }

    fun onAudioRouteClicked(callId: String, endpoint: CallEndpointCompat) {
        viewModelScope.launch { callRepository.switchCallEndpoint(callId, endpoint) }
    }

    fun addParticipant(callId: String) {
        Log.d(TAG, "[$callId] ViewModel: addParticipant")
        callRepository.addParticipant(callId)
    }

    fun removeParticipant(callId: String) {
        Log.d(TAG, "[$callId] ViewModel: removeParticipant")
        callRepository.removeParticipant(callId)
    }

    fun changeCallIcon(callId: String) {
        callRepository.changeCallIcon(callId)
    }

    // --- Permission Handling ---

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startAudioLoopbackInternal(callId: String) {
        if (audioManager.isMicrophoneMute) {
            Log.w(
                TAG,
                "[$callId] Internal start check: Cannot start loopback, global mute is active.",
            )
            _activeLoopbackCallId.value = null
            return
        }
        if (_activeLoopbackCallId.value == callId) {
            Log.w(
                TAG,
                "[$callId] startAudioLoopbackInternal called but already active for this ID.",
            )
            return
        }
        Log.d(TAG, "[$callId] Attempting to start loopback via manager...")
        if (audioLoopbackManager.isLoopbackRunning()) {
            Log.w(TAG, "[$callId] Manager was already running unexpectedly, stopping first.")
            audioLoopbackManager.stopLoopback() // Request stop first
        }
        val started = audioLoopbackManager.startLoopback(viewModelScope)
        if (started) {
            _activeLoopbackCallId.value = callId
            Log.i(TAG, "[$callId] Loopback reported as started by manager.")
        } else {
            _activeLoopbackCallId.value = null
            Log.e(TAG, "[$callId] Failed to start loopback (manager returned false).")
        }
    }

    private fun stopAudioLoopbackInternal() {
        val currentCallId = _activeLoopbackCallId.value
        if (currentCallId != null) {
            Log.d(TAG, "[$currentCallId] Stopping loopback...")
            audioLoopbackManager.stopLoopback()
            _activeLoopbackCallId.value = null // Update state *after* requesting stop
            Log.i(TAG, "[$currentCallId] Loopback stop requested/completed.")
        } else if (audioLoopbackManager.isLoopbackRunning()) {
            // Safety check: Stop manager if it's running without a tracked ID
            Log.w(TAG, "Stopping loopback manager which was running without a tracked ID.")
            audioLoopbackManager.stopLoopback()
        }
    }
}
