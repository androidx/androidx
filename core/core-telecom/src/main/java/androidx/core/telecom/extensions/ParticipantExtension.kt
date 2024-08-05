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

import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.internal.ParticipantActionCallbackRepository
import androidx.core.telecom.internal.ParticipantStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Called when a new remove connection to an action is being established. The
 * [ParticipantStateListenerRemote] contains the remote interface used to send both the initial and
 * ongoing updates to the state tracked by the action. Any collection of flows related to updating
 * the remote session should use the provided [CoroutineScope]. For event callbacks from the remote,
 * [ParticipantActionCallbackRepository] should be used to register the callbacks that the action
 * should handle.
 */
@OptIn(ExperimentalAppActions::class)
internal typealias ActionConnector =
    (CoroutineScope, ParticipantActionCallbackRepository, ParticipantStateListenerRemote) -> Unit

/**
 * The participant extension that manages the state of Participants associated with this call as
 * well as allowing participant related actions to register themselves with this extension.
 *
 * Along with updating the participants in a call to remote surfaces, this extension also allows the
 * following optional actions to be supported:
 * - [addRaiseHandSupport] - Support for allowing a remote surface to show which participants have
 *   their hands raised to the user as well as update the raised hand state of the user.
 * - [addKickParticipantSupport] = Support for allowing a user on a remote surface to kick a
 *   participant.
 *
 * @param initialParticipants The initial set of Participants that are associated with this call.
 * @param initialActiveParticipant The initial active Participant that is associated with this call.
 */
// TODO: Refactor to Public API
@ExperimentalAppActions
@RequiresApi(VERSION_CODES.O)
internal class ParticipantExtension(
    initialParticipants: Set<Participant>,
    initialActiveParticipant: Participant?
) {
    public companion object {
        /**
         * The version of this ParticipantExtension used for capability exchange. Should be updated
         * whenever there is an API change to this extension or an existing action.
         */
        internal const val VERSION = 1

        /**
         * Constants used to denote the type of action supported by the [Capability] being
         * registered.
         */
        @Target(AnnotationTarget.TYPE)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(RAISE_HAND_ACTION, KICK_PARTICIPANT_ACTION)
        annotation class ExtensionActions

        /** Identifier for the raise hand action */
        internal const val RAISE_HAND_ACTION = 1
        /** Identifier for the kick participant action */
        internal const val KICK_PARTICIPANT_ACTION = 2

        private const val LOG_TAG = Extensions.LOG_TAG + "(PE)"
    }

    /** StateFlow of the current set of Participants associated with the call */
    internal val participants: MutableStateFlow<Set<Participant>> =
        MutableStateFlow(initialParticipants)

    /** StateFlow containing the active participant of the call if it exists */
    private val activeParticipant: MutableStateFlow<Participant?> =
        MutableStateFlow(initialActiveParticipant)

    /** Maps an action to its [ActionConnector], which will be called during capability exchange */
    private val actionRemoteConnector: HashMap<Int, ActionConnector> = HashMap()

    /**
     * Update all remote listeners that the Participants of this call have changed
     *
     * @param newParticipants The new set of [Participant]s associated with this call
     */
    public suspend fun updateParticipants(newParticipants: Set<Participant>) {
        participants.emit(newParticipants)
    }

    /**
     * The active participant associated with this call, if it exists
     *
     * @param participant the participant that is marked as active or `null` if there is no active
     *   participant
     */
    public suspend fun updateActiveParticipant(participant: Participant?) {
        activeParticipant.emit(participant)
    }

    /**
     * Adds support for notifying remote InCallServices of the raised hand state of all Participants
     * in the call and listening for changes to this user's hand raised state.
     *
     * @param onHandRaisedChanged Called when the raised hand state of this user has changed. If
     *   `true`, the user has raised their hand. If `false`, the user has lowered their hand.
     * @return The interface used to update the current raised hand state of all participants in the
     *   call.
     */
    fun addRaiseHandSupport(onHandRaisedChanged: suspend (Boolean) -> Unit): RaiseHandState {
        val state = RaiseHandState(participants, onHandRaisedChanged)
        registerAction(RAISE_HAND_ACTION, connector = state::connect)
        return state
    }

    /**
     * Adds support for allowing the user to kick participants in the call.
     *
     * @param onKickParticipant The action to perform when the user requests to kick a participant
     * @return The interface used to update the state related to this action. This action contains
     *   no state today, but is included for forward compatibility
     */
    fun addKickParticipantSupport(onKickParticipant: suspend (Participant) -> Unit) {
        val state = KickParticipantState(participants, onKickParticipant)
        registerAction(KICK_PARTICIPANT_ACTION) { _, repo, _ -> state.connect(repo) }
    }

    /**
     * Setup the participant extension creation callback receiver and return the Capability of this
     * extension to be shared with the remote.
     */
    internal fun onExchangeStarted(callbacks: CapabilityExchangeRepository): Capability {
        callbacks.onCreateParticipantExtension = ::onCreateParticipantExtension
        return Capability().apply {
            featureId = Extensions.PARTICIPANT
            featureVersion = VERSION
            supportedActions = actionRemoteConnector.keys.toIntArray()
        }
    }

    /**
     * Register an action to this extension
     *
     * @param action The identifier of the action, which will be shared with the remote
     * @param connector The method that is called every time a new remote connects to the action in
     *   order to facilitate connecting this action to the remote.
     */
    private fun registerAction(action: Int, connector: ActionConnector) {
        actionRemoteConnector[action] = connector
    }

    /**
     * Function registered to [ExtensionInitializationScope] in order to handle the creation of the
     * participant extension.
     *
     * @param coroutineScope the CoroutineScope used to launch tasks associated with participants
     * @param remoteActions the actions reported as supported from the remote InCallService side
     * @param binder the interface used to communicate with the remote InCallService.
     */
    private fun onCreateParticipantExtension(
        coroutineScope: CoroutineScope,
        remoteActions: Set<Int>,
        binder: ParticipantStateListenerRemote
    ) {
        Log.i(LOG_TAG, "onCreatePE: actions=$remoteActions")

        // Synchronize initial state with remote
        val initParticipants = participants.value.toTypedArray()
        val initActiveParticipant = activeParticipant.value
        binder.updateParticipants(initParticipants)
        if (initActiveParticipant != null && initParticipants.contains(initActiveParticipant)) {
            binder.updateActiveParticipant(initActiveParticipant.id)
        } else {
            binder.updateActiveParticipant(Extensions.NULL_PARTICIPANT_ID)
        }

        // Setup listeners for changes to state
        participants
            .onEach { updatedParticipants ->
                Log.i(LOG_TAG, "to remote: updateParticipants: $updatedParticipants")
                binder.updateParticipants(updatedParticipants.toTypedArray())
            }
            .combine(activeParticipant) { p, a ->
                val result = if (a != null && p.contains(a)) a else null
                Log.d(LOG_TAG, "combine: $p + $a = $result")
                result
            }
            .distinctUntilChanged()
            .onEach {
                Log.d(LOG_TAG, "to remote: updateActiveParticipant=$it")
                binder.updateActiveParticipant(it?.id ?: Extensions.NULL_PARTICIPANT_ID)
            }
            .launchIn(coroutineScope)
        Log.d(LOG_TAG, "onCreatePE: finished state update")

        // Setup actions
        coroutineScope.launch {
            // Setup one callback repository per connection to remote
            val callbackRepository = ParticipantActionCallbackRepository(this)
            // Set up actions (only where the remote side supports it)
            actionRemoteConnector
                .filter { entry -> remoteActions.contains(entry.key) }
                .map { entry -> entry.value }
                .forEach { initializer -> initializer(this, callbackRepository, binder) }
            Log.d(LOG_TAG, "onCreatePE: calling finishSync")
            binder.finishSync(callbackRepository.eventListener)
        }
    }
}
