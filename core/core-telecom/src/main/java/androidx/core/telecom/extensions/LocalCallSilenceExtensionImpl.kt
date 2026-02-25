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

package androidx.core.telecom.extensions

import android.content.Context
import android.media.AudioManager
import android.os.RemoteException
import android.util.Log
import androidx.core.telecom.internal.CallStateEvent
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.internal.LocalCallSilenceCallbackRepository
import androidx.core.telecom.internal.LocalCallSilenceStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalAppActions::class)
internal class LocalCallSilenceExtensionImpl(
    context: Context,
    private val callStateFlow: MutableSharedFlow<CallStateEvent>,
    private val initialSilenceState: Boolean,
    private val initialCanUserUpdateSilenceState: Boolean,
    private val onLocalSilenceUpdate: suspend (Boolean) -> Unit,
) : LocalCallSilenceExtension {
    private val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mIsGloballyMuted: Boolean = false
    private var mCallState: CallStateEvent = CallStateEvent.NEW
    private val TAG = LocalCallSilenceExtensionImpl::class.java.simpleName

    private fun isFocus(): Boolean {
        return mCallState.isFocusState()
    }

    private fun isInactive(): Boolean {
        return mCallState.isInactiveState()
    }

    private fun isGloballyMuted(): Boolean {
        return mIsGloballyMuted
    }

    private fun maybeUpdateGlobalMuteState(state: CallStateEvent) {
        if (state.isGlobalMuteState()) {
            mIsGloballyMuted = state.isMuted()
        }
    }

    private fun maybeUpdateCallControlState(state: CallStateEvent) {
        if (state.isCallControlState()) {
            mCallState = state
        }
    }

    companion object {
        internal const val VERSION = 2
        internal const val AUTHORITATIVE_MUTE_MIN_VERSION = 2
        val TAG: String = LocalCallSilenceExtensionImpl::class.java.simpleName
    }

    internal val isLocallySilenced: MutableStateFlow<Boolean> =
        MutableStateFlow(initialSilenceState)
    private val canUserUpdateSilenceState = MutableStateFlow(initialCanUserUpdateSilenceState)

    /**
     * This method is called by the VoIP application whenever the VoIP application wants to update
     * all the remote surfaces
     */
    override suspend fun updateIsLocallySilenced(isSilenced: Boolean) {
        Log.i(TAG, "updateIsLocallySilenced: isSilenced=[$isSilenced]")
        isLocallySilenced.emit(isSilenced)
    }

    override suspend fun updateCanUserUpdateSilence(canUserUpdateSilence: Boolean) {
        Log.i(TAG, "updateCanUserUpdateSilence: canUserUpdateSilence=[$canUserUpdateSilence]")
        canUserUpdateSilenceState.emit(canUserUpdateSilence)
    }

    internal fun onExchangeStarted(callbacks: CapabilityExchangeRepository): Capability {
        callbacks.onCreateLocalCallSilenceExtension = ::onCreateLocalSilenceExtension
        return Capability().apply {
            featureId = Extensions.LOCAL_CALL_SILENCE
            featureVersion = VERSION
            supportedActions = IntArray(0)
        }
    }

    private fun onCreateLocalSilenceExtension(
        coroutineScope: CoroutineScope,
        version: Int,
        remoteActions: Set<Int>,
        binder: LocalCallSilenceStateListenerRemote,
    ) {
        Log.d(TAG, "onCreateLocalSilenceExtension: version=[$version], actions=$remoteActions")
        startGlobalMuteMonitoring(coroutineScope)
        // Setup listeners for changes to state
        isLocallySilenced
            .onEach {
                // send all updates to the remote surfaces
                // VoIP --> ICS
                binder.updateIsLocallySilenced(it)
            }
            .launchIn(coroutineScope)
        canUserUpdateSilenceState
            .onEach { canUserUpdateSilenceState ->
                try {
                    Log.w(
                        TAG,
                        "onCreateLocalSilenceExtension: sending" +
                            " canUserUpdateSilenceState=[$canUserUpdateSilenceState]",
                    )
                    if (version >= AUTHORITATIVE_MUTE_MIN_VERSION) {
                        binder.updateCanUserUpdateSilence(canUserUpdateSilenceState)
                    }
                } catch (e: RemoteException) {
                    // Remote (ICS) is likely on an older version, swallow exception
                    Log.w(
                        TAG,
                        "onCreateLocalSilenceExtension: Failed to update " +
                            "canUserUpdateSilenceState",
                        e,
                    )
                }
            }
            .launchIn(coroutineScope)
        // hook up the callbacks so the remote ICS can update this impl
        val callbackRepository = LocalCallSilenceCallbackRepository(coroutineScope)
        callbackRepository.localCallSilenceCallback = ::localCallSilenceStateChanged
        binder.finishSync(callbackRepository.eventListener)
    }

    /**
     * Helper function containing the logic previously found in the init block. Triggered only when
     * the capability exchange happens.
     */
    private fun startGlobalMuteMonitoring(coroutineScope: CoroutineScope) {
        var shouldRemute = false
        coroutineScope.launch {
            callStateFlow.collect {
                maybeUpdateCallControlState(state = it)
                maybeUpdateGlobalMuteState(state = it)
                if (isFocus() && isGloballyMuted()) {
                    Log.i(TAG, "UNMUTING the mic globally in favor of a local call silence")
                    mAudioManager.isMicrophoneMute = false
                    shouldRemute = true
                } else if (isInactive() && shouldRemute) {
                    Log.i(
                        TAG,
                        "MUTING the mic globally to put the device back in its original state",
                    )
                    mAudioManager.isMicrophoneMute = true
                    shouldRemute = false
                }
            }
        }
    }

    /**
     * This method is the entry point when the remote surface wants to update this impl. This
     * updates the block in the VoIP app where the extension was added.
     */
    private suspend fun localCallSilenceStateChanged(isSilenced: Boolean) {
        Log.i(TAG, "localCallSilenceStateChanged: isSilenced=[$isSilenced]")
        // notify the voip application of the remote InCallService update
        onLocalSilenceUpdate(isSilenced)
    }
}
