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
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.ActionsResultCallbackRemote
import androidx.core.telecom.internal.ParticipantActions
import androidx.core.telecom.internal.ParticipantStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Adds the participant extension to a call, which provides the ability to specify participant
 * related information.
 *
 * @param initialParticipants The initial participants in the call
 * @param initialActiveParticipant The initial participant that is active in the call
 * @return The interface used to update the participant state to remote InCallServices
 */
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
fun ExtensionInitializationScope.addParticipantExtension(
    initialParticipants: Set<Participant> = emptySet(),
    initialActiveParticipant: Participant? = null
): ParticipantExtension {
    val extension = ParticipantExtension(initialParticipants, initialActiveParticipant)
    registerExtension("Participants") {
        ExtensionCreationDelegate(
            Capability().apply {
                featureId = CallsManager.PARTICIPANT
                featureVersion = 1
                supportedActions = extension.actions.keys.toIntArray()
            },
            onCreateParticipantExtension = extension::onCreateParticipantExtension
        )
    }
    return extension
}

/**
 * Holds the callbacks that are called when the remote InCallService sends a request to perform an
 * action.
 */
@ExperimentalAppActions
internal class ParticipantActionCallbackRepository {
    /**
     * The callback that is called when the remote InCallService changes the raised hand state of
     * this user.
     */
    var raiseHandStateCallback: (suspend (Boolean) -> Unit)? = null

    /** The callback that is called when the remote InCallService requests to kick a participant. */
    var kickParticipantCallback: (suspend (Participant) -> Unit)? = null

    /**
     * Call the callback that is registered to listen for when the raised hand state changed request
     * is fired from the remote InCallService.
     *
     * @param newState The new raised hand state of this user
     * @param remote The result callback that must be called at the end of this operation to signal
     *   to the remote side that the operation completed.
     */
    suspend fun raiseHandStateChanged(newState: Boolean, remote: ActionsResultCallbackRemote) {
        raiseHandStateCallback?.invoke(newState)
        remote.onSuccess()
    }

    /**
     * Call the callback that is registered to listen for when the remote InCallService requests to
     * kick a participant.
     *
     * @param participant the participant that the remote InCallService is requesting to kick
     * @param remote the result callback that must be called once this operation is complete.
     */
    suspend fun kickParticipant(participant: Participant, remote: ActionsResultCallbackRemote) {
        kickParticipantCallback?.invoke(participant)
        remote.onSuccess()
    }
}

/**
 * The participant extension that manages the state of Participants associated with this call as
 * well as allowing participant related actions to register themselves with this extension.
 *
 * @param initialParticipants The initial set of Participants that are associated with this call.
 * @param initialActiveParticipant The initial active Participant that is associated with this call.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(VERSION_CODES.O)
class ParticipantExtension(
    initialParticipants: Set<Participant>,
    initialActiveParticipant: Participant?
) {
    companion object {
        const val LOG_TAG = CallsManagerExtensions.LOG_TAG + "(PE)"
    }

    /**
     * Maps an Action to a creation function that allows the caller to register for action callbacks
     * using [ParticipantActionCallbackRepository] and the [CoroutineScope] used to launch tasks
     * associated with handling remote actions as well as the [ParticipantStateListenerRemote] used
     * to communicate with the remote InCallService.
     */
    internal val actions:
        HashMap<
            Int,
            suspend (ParticipantActionCallbackRepository.(
                CoroutineScope, ParticipantStateListenerRemote
            ) -> Unit)
        > =
        HashMap()

    /** StateFlow of the current set of Participants associated with the call */
    internal val participants: MutableStateFlow<Set<Participant>> =
        MutableStateFlow(initialParticipants)

    /** StateFlow containing the active participant of the call if it exists */
    private val activeParticipant: MutableStateFlow<Participant?> =
        MutableStateFlow(initialActiveParticipant)

    /**
     * The repository of callbacks that are associated with actions that have registered themselves
     * with this extension.
     */
    private val actionRepository: ParticipantActionCallbackRepository =
        ParticipantActionCallbackRepository()

    /**
     * Update all remote listeners that the Participants of this call have changed
     *
     * @param newParticipants The new set of [Participant]s associated with this call
     */
    suspend fun updateParticipants(newParticipants: Set<Participant>) {
        participants.emit(newParticipants)
    }

    /**
     * The active participant associated with this call, if it exists
     *
     * @param participant the participant that is marked as active or `null` if there is no active
     *   participant
     */
    suspend fun updateActiveParticipant(participant: Participant?) {
        activeParticipant.emit(participant)
    }

    /**
     * Register an action that is associated with this Participant
     *
     * @param id The unique identifier of the action, which is shared to the remote InCallService to
     *   identify this action
     * @param action The action creation function that provides a
     *   [ParticipantActionCallbackRepository] for the action to listen to action requests from the
     *   remote as well as a [CoroutineScope] to run any tasks associated with processing actions
     *   and the [ParticipantStateListenerRemote] interface to communicate with the remote
     *   InCallService
     */
    internal fun registerAction(
        id: Int,
        action:
            suspend ParticipantActionCallbackRepository.(
                CoroutineScope, ParticipantStateListenerRemote
            ) -> Unit
    ) {
        actions[id] = action
    }

    /**
     * Function registered to [ExtensionInitializationScope] in order to handle the creation of the
     * participant extenson.
     *
     * @param coroutineScope the CoroutineScope used to launch tasks associated with participants
     * @param remoteActions the actions reported as supported from the remote InCallService side
     * @param binder the interface used to communicate with the remote InCallService.
     */
    internal fun onCreateParticipantExtension(
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
            binder.updateActiveParticipant(CallsManager.NULL_PARTICIPANT_ID)
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
                binder.updateActiveParticipant(it?.id ?: CallsManager.NULL_PARTICIPANT_ID)
            }
            .launchIn(coroutineScope)
        Log.d(LOG_TAG, "onCreatePE: finished state update")

        // Set up actions (only where the remote side supports it)
        val filteredActions = actions.filter { entry -> remoteActions.contains(entry.key) }
        coroutineScope.launch {
            Log.d(LOG_TAG, "onCreatePE: init filtered actions, actions=$filteredActions")
            filteredActions.forEach { it.value(actionRepository, coroutineScope, binder) }
            Log.d(LOG_TAG, "onCreatePE: calling finishSync")
            binder.finishSync(
                ParticipantActions(
                    setHandRaised = { value, cb ->
                        coroutineScope.launch {
                            Log.i(LOG_TAG, "from remote: raiseHandStateChanged=$value")
                            actionRepository.raiseHandStateChanged(value, cb)
                        }
                    },
                    kickParticipant = { participant, cb ->
                        coroutineScope.launch {
                            Log.i(LOG_TAG, "from remote: kickParticipant=$participant")
                            actionRepository.kickParticipant(participant, cb)
                        }
                    }
                )
            )
        }
    }
}

/** Action interface implemented by the listener of the RaiseHandAction */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
fun interface RaiseHandAction {
    /**
     * The remote InCallService has requested that the raised hand state of the user be changed
     *
     * @param raisedHandState the new raised hand state that the user has requested. If true, the
     *   user wants to raise their hand, false if they want to lower their hand.
     */
    suspend fun onHandRaisedChanged(raisedHandState: Boolean)
}

/**
 * Interface used to communicate with remote InCallServices in order to update the current raised
 * hand state of all Participants
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
fun interface RaiseHandActionState {
    /**
     * Notify remote listeners of which Participants have their hand raised
     *
     * @param raisedHands the set of Participants that have their hands raised.
     */
    suspend fun updateRaisedHands(raisedHands: Set<Participant>)
}

/**
 * Add an action to notify remote InCallServices of the raised hand state of all Participants in the
 * call and listen for changes to this user's hand raised state.
 *
 * @param action Implemented by this application to receive raised hand state change requests from
 *   this user.
 * @return The interface used to update the current raised hand state of all participants in the
 *   call.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(VERSION_CODES.O)
fun ParticipantExtension.addRaiseHandAction(action: RaiseHandAction): RaiseHandActionState {
    val raiseHandRemote = RaiseHandActionRemote(participants, action)
    registerAction(CallsManager.RAISE_HAND_ACTION) { scope, remote ->
        Log.d(CallsManagerExtensions.LOG_TAG, "init addRaiseHandAction")
        raiseHandStateCallback = raiseHandRemote::raiseHandStateChanged
        raiseHandRemote.connect(scope, remote)
    }
    return raiseHandRemote
}

/**
 * Tracks the current raised hand state of all of the Participants of this call and notifies the
 * listener if the user requests to change their raised hand state.
 *
 * @param participants The StateFlow containing the current set of Participants in the call
 * @param action The action to perform when the remote InCallService requests to change this user's
 *   raised hand state.
 */
@ExperimentalAppActions
internal class RaiseHandActionRemote(
    val participants: StateFlow<Set<Participant>>,
    private val action: RaiseHandAction
) : RaiseHandActionState {
    companion object {
        const val LOG_TAG = CallsManagerExtensions.LOG_TAG + "(RHAR)"
    }

    private val raisedHandsState: MutableStateFlow<Set<Participant>> = MutableStateFlow(emptySet())

    /**
     * Notify the remote InCallService of an update to the participants that have their hands raised
     *
     * @param raisedHands The new set of Participants that have their hands raised.
     */
    override suspend fun updateRaisedHands(raisedHands: Set<Participant>) {
        raisedHandsState.emit(raisedHands)
    }

    /**
     * Registered to be called when the remote InCallService has requested to change the raised hand
     * state of the user.
     *
     * @param state The new raised hand state, true if hand is raised, false if it is not.
     */
    suspend fun raiseHandStateChanged(state: Boolean) {
        Log.d(LOG_TAG, "raisedHandStateChanged: updated state: $state")
        action.onHandRaisedChanged(state)
    }

    fun connect(scope: CoroutineScope, remote: ParticipantStateListenerRemote) {
        Log.i(LOG_TAG, "connect: sync state")
        remote.updateRaisedHandsAction(raisedHandsState.value.map { it.id }.toIntArray())
        participants
            .combine(raisedHandsState) { p, rhs -> p.intersect(rhs) }
            .distinctUntilChanged()
            .onEach {
                Log.i(LOG_TAG, "to remote: updateRaisedHands=$it")
                remote.updateRaisedHandsAction(it.map { p -> p.id }.toIntArray())
            }
            .launchIn(scope)
    }
}

/**
 * Interface used to notify callers when the remote InCallService has requested to kick a
 * participant.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
fun interface KickParticipantAction {
    /**
     * The user has requested to kick a participant
     *
     * @param participant the participant to kick
     */
    suspend fun onKickParticipant(participant: Participant)
}

/**
 * Adds the action to support the user kicking participants.
 *
 * @param action The action to perform when the user requests to kick a participant
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(VERSION_CODES.O)
fun ParticipantExtension.addKickParticipantAction(action: KickParticipantAction) {
    val kickAction = KickParticipantActionRemote(participants, action)
    registerAction(CallsManager.KICK_PARTICIPANT_ACTION) { _, _ ->
        Log.d(CallsManagerExtensions.LOG_TAG, "init addKickParticipantAction")
        kickParticipantCallback = kickAction::kickParticipant
    }
}

/**
 * Tracks requests to kick participants from a remote InCallService and invokes the supplied action
 * when a request comes in.
 *
 * @param participants A StateFlow containing the set of Participants in the call, which is used to
 *   validate the participant to kick is valid.
 * @param action The action to perform when a request comes in from the remote InCallService to kick
 *   a participant.
 */
@ExperimentalAppActions
internal class KickParticipantActionRemote(
    val participants: StateFlow<Set<Participant>>,
    private val action: KickParticipantAction
) {
    companion object {
        const val LOG_TAG = CallsManagerExtensions.LOG_TAG + "(KPAR)"
    }

    /**
     * Registered to be called when the remote InCallService has requested to kick a Participant.
     *
     * @param participant The participant to kick
     */
    suspend fun kickParticipant(participant: Participant) {
        if (!participants.value.contains(participant)) {
            Log.w(LOG_TAG, "kickParticipant: $participant can not be found")
            return
        }
        Log.d(LOG_TAG, "kickParticipant: kicking $participant")
        action.onKickParticipant(participant)
    }
}
