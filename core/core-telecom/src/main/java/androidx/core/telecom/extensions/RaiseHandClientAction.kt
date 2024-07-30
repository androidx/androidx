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
import androidx.core.telecom.CallsManager
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
 * Adds the ability for participants to raise their hands.
 *
 * ```
 * connectExtensions(call) {
 *     val participantExtension = addParticipantExtension(
 *         // consume participant changed events
 *     )
 *     val raiseHandAction = participantExtension.addRaiseHandAction { raisedHands ->
 *         // consume changes of participants with their hands raised
 *     }
 *     onConnected {
 *         // extensions have been negotiated and actions are ready to be used
 *         ...
 *         // notify the remote that this user has changed their hand raised state
 *         val raisedHandResult = raiseHandAction.setRaisedHandState(userHandRaisedState)
 *     }
 * }
 * ```
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal fun ParticipantClientExtension.addRaiseHandAction(
    stateUpdate: RaisedHandsUpdate
): RaiseHandClientAction {
    val action = RaiseHandClientAction(participants, stateUpdate)
    registerAction(CallsManager.RAISE_HAND_ACTION, action::connect) { scope, isSupported ->
        Log.d(ParticipantClientExtension.TAG, "addRaiseHandAction: initialize")
        raisedHandsStateCallback = action::raisedHandsStateChanged
        action.initialize(scope, isSupported)
    }
    return action
}

/**
 * Implements the ability for the user to raise/lower their hand as well as allow the user to listen
 * to the hand raised states of all other participants
 *
 * @param participants The StateFlow containing the current set of participants in the call at any
 *   given time.
 * @param stateUpdate The callback that allows the user to listen to the state of participants that
 *   have their hand raised
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal class RaiseHandClientAction(
    private val participants: StateFlow<Set<Participant>>,
    private val stateUpdate: RaisedHandsUpdate
) {
    companion object {
        const val TAG = CallExtensionsScope.TAG + "(RHCA)"
    }

    /**
     * Whether or not raising/lowering hands is supported by the remote.
     *
     * if `true`, then updates about raised hands will be notified. If `false`, then the remote
     * doesn't support this action this state will not be notified to the caller.
     *
     * Should not be queried until [CallExtensionsScope.onConnected] is called.
     */
    var isSupported by Delegates.notNull<Boolean>()

    // Contains the remote Binder interface used to notify the remote application of events
    private var remoteActions: ParticipantActionsRemote? = null
    // Contains the current state of participants that have their hands raised
    private val raisedHandState = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * Request the remote application to raise or lower this user's hand.
     *
     * Note: This operation succeeding does not mean that the raised hand state of the user has
     * changed. It only means that the request was received by the remote application.
     *
     * @param isRaised `true` if this user has raised their hand, `false` if they have lowered their
     *   hand
     * @return Whether or not the remote application received this event. This does not mean that
     *   the operation succeeded, but rather the remote received the event successfully.
     */
    suspend fun requestRaisedHandStateChange(isRaised: Boolean): CallControlResult {
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
    internal suspend fun raisedHandsStateChanged(raisedHands: Set<Int>) {
        Log.d(TAG, "raisedHandsStateChanged to $raisedHands")
        raisedHandState.emit(raisedHands)
    }

    /** Called when capability exchange has completed and we should setup the action */
    internal fun initialize(callScope: CoroutineScope, isSupported: Boolean) {
        Log.d(TAG, "initialize, isSupported=$isSupported")
        this.isSupported = isSupported
        if (isSupported) {
            participants
                .combine(raisedHandState) { p, rhs -> p.filter { rhs.contains(it.id) } }
                .distinctUntilChanged()
                .onEach { filtered -> stateUpdate.onRaisedHandsChanged(filtered.toSet()) }
                .onCompletion { Log.d(TAG, "raised hands flow complete") }
                .launchIn(callScope)
        }
    }

    /** Called when the remote has connected for Actions and events are available */
    internal fun connect(remote: ParticipantActionsRemote?) {
        Log.d(TAG, "connect: remote is null=${remote == null}")
        remoteActions = remote
    }
}
