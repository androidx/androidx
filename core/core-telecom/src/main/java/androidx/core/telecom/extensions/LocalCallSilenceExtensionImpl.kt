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

import android.util.Log
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.internal.LocalCallSilenceCallbackRepository
import androidx.core.telecom.internal.LocalCallSilenceStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalAppActions::class)
internal class LocalCallSilenceExtensionImpl(
    private val initialSilenceState: Boolean,
    private val onLocalSilenceUpdate: suspend (Boolean) -> Unit
) : LocalCallSilenceExtension {
    companion object {
        internal const val VERSION = 1
        val TAG: String = LocalCallSilenceExtensionImpl::class.java.simpleName
    }

    internal val isLocallySilenced: MutableStateFlow<Boolean> =
        MutableStateFlow(initialSilenceState)

    /**
     * This method is called by the VoIP application whenever the VoIP application wants to update
     * all the remote surfaces
     */
    override suspend fun updateIsLocallySilenced(isSilenced: Boolean) {
        Log.i(TAG, "updateIsLocallySilenced: isSilenced=[$isSilenced]")
        isLocallySilenced.emit(isSilenced)
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
        remoteActions: Set<Int>,
        binder: LocalCallSilenceStateListenerRemote
    ) {
        Log.d(TAG, "onCreateLocalSilenceExtension: actions=$remoteActions")
        // Synchronize initial state with remote
        binder.updateIsLocallySilenced(initialSilenceState)
        // Setup listeners for changes to state
        isLocallySilenced
            .drop(1) // drop the first value since the sync was already sent out
            .onEach {
                // send all updates to the remote surfaces
                // VoIP --> ICS
                binder.updateIsLocallySilenced(it)
            }
            .launchIn(coroutineScope)
        // hook up the callbacks so the remote ICS can update this impl
        val callbackRepository = LocalCallSilenceCallbackRepository(coroutineScope)
        callbackRepository.localCallSilenceCallback = ::localCallSilenceStateChanged
        binder.finishSync(callbackRepository.eventListener)
    }

    /**
     * This method is the entry point when the remote surface wants to update this impl. This
     * updates the block in the VoIP app where the extension was added.
     */
    private suspend fun localCallSilenceStateChanged(isSilenced: Boolean) {
        Log.i(TAG, "localCallSilenceStateChanged: isSilenced=[$isSilenced]")
        onLocalSilenceUpdate(isSilenced)
    }
}
