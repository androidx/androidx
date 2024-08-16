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
import androidx.core.telecom.internal.ParticipantActionsRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

/**
 * Implements the ability for the user to raise/lower their hand as well as allow the user to listen
 * to the hand raised states of all other participants
 *
 * @param participants The StateFlow containing the current set of participants in the call at any
 *   given time.
 * @param onRaisedHandsChanged The callback that allows the user to listen to the state of
 *   participants that have their hand raised
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAppActions::class)
internal class RaiseHandActionImpl(
    private val participants: StateFlow<Set<Participant>>,
    private val onRaisedHandsChanged: suspend (List<Participant>) -> Unit
) : RaiseHandAction {
    companion object {
        const val TAG = CallExtensionScopeImpl.TAG + "(RHCA)"
    }

    // Contains the remote Binder interface used to notify the remote application of events
    private var remoteActions: ParticipantActionsRemote? = null
    // Contains the current state of participant ids that have their hands raised
    private val raisedHandIdsState = MutableStateFlow<List<String>>(emptyList())

    override var isSupported by Delegates.notNull<Boolean>()

    override suspend fun requestRaisedHandStateChange(isRaised: Boolean): CallControlResult {
        Log.d(TAG, "setRaisedHandState: isRaised=$isRaised")
        if (remoteActions == null) {
            Log.w(TAG, "setRaisedHandState: no binder, isSupported=$isSupported")
            // TODO: This needs to have its own CallException result
            return CallControlResult.Error(CallException.ERROR_UNKNOWN)
        }
        val cb = ActionsResultCallback()
        remoteActions?.setHandRaised(isRaised, cb)
        val result = cb.waitForResponse()
        Log.d(TAG, "setRaisedHandState: isRaised=$isRaised, result=$result")
        return result
    }

    /** Called when the remote application has changed the raised hands state */
    private suspend fun raisedHandIdsStateChanged(raisedHands: List<String>) {
        Log.d(TAG, "raisedHandsStateChanged to $raisedHands")
        raisedHandIdsState.emit(raisedHands)
    }

    /** Called when capability exchange has completed and we should setup the action */
    internal fun initialize(
        callScope: CoroutineScope,
        isSupported: Boolean,
        callbacks: ParticipantStateCallbackRepository
    ) {
        Log.d(TAG, "initialize, isSupported=$isSupported")
        this.isSupported = isSupported
        if (!isSupported) return
        callbacks.raisedHandIdsStateCallback = ::raisedHandIdsStateChanged
        participants
            .combine(raisedHandIdsState) { p, rhs ->
                rhs.mapNotNull { rh -> p.firstOrNull { it.id == rh } }
            }
            .distinctUntilChanged()
            .onEach { filtered -> onRaisedHandsChanged(filtered) }
            .onCompletion { Log.d(TAG, "raised hands flow complete") }
            .launchIn(callScope)
    }

    /** Called when the remote has connected for Actions and events are available */
    internal fun connect(remote: ParticipantActionsRemote?) {
        Log.d(TAG, "connect: remote is null=${remote == null}")
        remoteActions = remote
    }
}
