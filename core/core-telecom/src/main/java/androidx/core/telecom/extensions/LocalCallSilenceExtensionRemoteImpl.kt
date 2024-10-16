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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallException
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.LocalCallSilenceActionsRemote
import androidx.core.telecom.internal.LocalCallSilenceStateListener
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.resume
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAppActions::class)
internal class LocalCallSilenceExtensionRemoteImpl(
    private val callScope: CoroutineScope,
    private val onLocalSilenceStateUpdated: suspend (Boolean) -> Unit
) : LocalCallSilenceExtensionRemote {

    companion object {
        val TAG: String = LocalCallSilenceExtensionRemoteImpl::class.java.simpleName
    }

    override var isSupported: Boolean by Delegates.notNull()
    private val isLocallySilenced = MutableStateFlow(false)
    private var remoteActions: ILocalSilenceActions? = null

    /**
     * This method is used by the InCallService to update the VoIP applications local call silence
     * state.
     */
    override suspend fun requestLocalCallSilenceUpdate(isSilenced: Boolean): CallControlResult {
        if (remoteActions == null) {
            Log.i(TAG, "requestLocalCallSilenceState: remoteActions are null")
            return CallControlResult.Error(CallException.ERROR_UNKNOWN)
        }
        val cb = ActionsResultCallback()
        // this remote impl --> VoIP  / Callback
        remoteActions?.setIsLocallySilenced(isSilenced, cb)
        val result = cb.waitForResponse()
        Log.i(TAG, "requestLocalCallSilenceState: isSilenced= $isSilenced, result=$result")
        return result
    }

    // NOTE: There are NO actions! Therefore there is no need to add action support OR register!
    internal val actions
        get() = IntArray(0)

    internal suspend fun onExchangeComplete(
        negotiatedCapability: Capability?,
        remote: CapabilityExchangeListenerRemote?
    ) {
        if (negotiatedCapability == null || remote == null) {
            Log.i(TAG, "onNegotiated: remote is not capable")
            isSupported = false
            return
        }
        isSupported = true
        Log.i(TAG, "onExchangeComplete: isSupported=[true]")
        isLocallySilenced
            .drop(1) // ignore the first default value
            .onEach {
                // This updates external extension block that the InCallService implements.
                // see [CallExtensionScopeImpl#addLocalCallSilenceExtension] for more.
                onLocalSilenceStateUpdated(it)
            }
            .launchIn(callScope)

        remoteActions = connectToRemote(negotiatedCapability, remote)
    }

    private suspend fun connectToRemote(
        negotiatedCapability: Capability,
        remote: CapabilityExchangeListenerRemote
    ): LocalCallSilenceActionsRemote? = suspendCancellableCoroutine { continuation ->
        val stateListener =
            LocalCallSilenceStateListener(
                updateLocalCallSilence = {
                    callScope.launch {
                        //  This is the first entry point when the VoIP app updates this
                        // remote impl. It is called when:
                        // - the initial sync is started
                        // - any update the VoIP app sends to this remote impl.

                        // This updates external extension block that the InCallService implements.
                        // see [CallExtensionScopeImpl#addLocalCallSilenceExtension] for more.
                        Log.i(TAG, "LCS_SL: updateLocalCallSilence: isSilenced=[$it]")
                        isLocallySilenced.emit(it)
                    }
                },
                finishSync = { remoteBinder ->
                    callScope.launch { continuation.resume(remoteBinder) }
                }
            )
        remote.onCreateLocalCallSilenceExtension(
            negotiatedCapability.featureVersion,
            negotiatedCapability.supportedActions,
            stateListener
        )
    }
}
