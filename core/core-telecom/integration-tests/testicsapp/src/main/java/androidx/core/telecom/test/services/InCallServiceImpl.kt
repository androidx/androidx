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

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.util.Log
import androidx.core.telecom.InCallServiceCompat
import androidx.core.telecom.test.Compatibility
import androidx.core.telecom.util.ExperimentalAppActions
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Implements the InCallService for this application as well as a local ICS binder for activities to
 * bind to this service locally and receive state changes.
 */
class InCallServiceImpl : LocalIcsBinder, InCallServiceCompat() {
    private companion object {
        const val LOG_TAG = "InCallServiceImpl"
    }

    private val localBinder =
        object : LocalIcsBinder.Connector, Binder() {
            override fun getService(): LocalIcsBinder {
                return this@InCallServiceImpl
            }
        }

    private val currId = AtomicInteger(1)
    private val mCallDataAggregator = CallDataAggregator()
    override val callData: StateFlow<List<CallData>> = mCallDataAggregator.callDataState
    private val mMuteStateResolver = MuteStateResolver()

    @Suppress("DEPRECATION")
    private val mCallAudioRouteResolver =
        CallAudioRouteResolver(
            lifecycleScope,
            callData,
            ::setAudioRoute,
            ::requestBluetoothAudio,
            onRequestEndpointChange = { ep, e, or ->
                Compatibility.requestCallEndpointChange(this@InCallServiceImpl, ep, e, or)
            }
        )
    override val isMuted: StateFlow<Boolean> = mMuteStateResolver.muteState
    override val currentAudioEndpoint: StateFlow<CallAudioEndpoint?> =
        mCallAudioRouteResolver.currentEndpoint
    override val availableAudioEndpoints: StateFlow<List<CallAudioEndpoint>> =
        mCallAudioRouteResolver.availableEndpoints

    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            Log.w(LOG_TAG, "onBind: null intent, returning")
            return null
        }
        if (SERVICE_INTERFACE == intent.action) {
            Log.d(LOG_TAG, "onBind: Received telecom interface.")
            return super.onBind(intent)
        }
        Log.d(LOG_TAG, "onBind: Received bind request from ${intent.`package`}")
        return localBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "onUnbind: Received unbind request from $intent")
        // work around a stupid bug where InCallService assumes that the unbind request can only
        // come from telecom
        if (intent?.action != null) {
            return super.onUnbind(intent)
        }
        return false
    }

    override fun onChangeMuteState(isMuted: Boolean) {
        setMuted(isMuted)
    }

    override suspend fun onChangeAudioRoute(id: String) {
        mCallAudioRouteResolver.onChangeAudioRoute(id)
    }

    @OptIn(ExperimentalAppActions::class)
    override fun onCallAdded(call: Call?) {
        if (call == null) return
        var callJob: Job? = null
        callJob =
            lifecycleScope.launch {
                connectExtensions(call) {
                    val participantsEmitter = ParticipantExtensionDataEmitter()
                    val participantExtension =
                        addParticipantExtension(
                            onActiveParticipantChanged =
                                participantsEmitter::onActiveParticipantChanged,
                            onParticipantsUpdated = participantsEmitter::onParticipantsChanged
                        )

                    val kickParticipantDataEmitter = KickParticipantDataEmitter()
                    val kickParticipantAction = participantExtension.addKickParticipantAction()

                    val raiseHandDataEmitter = RaiseHandDataEmitter()
                    val raiseHandAction =
                        participantExtension.addRaiseHandAction(
                            raiseHandDataEmitter::onRaisedHandsChanged
                        )
                    onConnected {
                        val callDataEmitter = CallDataEmitter(IcsCall(currId.getAndAdd(1), call))
                        val participantData =
                            participantsEmitter.collect(
                                participantExtension.isSupported,
                                raiseHandDataEmitter.collect(raiseHandAction),
                                kickParticipantDataEmitter.collect(kickParticipantAction)
                            )
                        val fullData =
                            callDataEmitter.collect().combine(participantData) { callData, partData
                                ->
                                CallData(callData, partData)
                            }
                        mCallDataAggregator.watch(this@launch, fullData)
                    }
                }
                callJob?.cancel("Call Disconnected")
                Log.d(LOG_TAG, "onCallAdded: connectedExtensions complete")
            }
    }

    @Deprecated("Deprecated in API 34")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        mMuteStateResolver.onCallAudioStateChanged(audioState)
        mCallAudioRouteResolver.onCallAudioStateChanged(audioState)
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        mMuteStateResolver.onMuteStateChanged(isMuted)
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        mCallAudioRouteResolver.onCallEndpointChanged(callEndpoint)
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        mCallAudioRouteResolver.onAvailableCallEndpointsChanged(availableEndpoints)
    }
}
