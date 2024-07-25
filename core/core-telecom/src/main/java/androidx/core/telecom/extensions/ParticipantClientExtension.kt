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
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.ParticipantActionsRemote
import androidx.core.telecom.internal.ParticipantStateListener
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Repository containing the callbacks associated with the Participant extension state changes */
@ExperimentalAppActions
internal class ParticipantStateCallbackRepository {
    var raisedHandsStateCallback: (suspend (Set<Int>) -> Unit)? = null
}

/**
 * Contains the callbacks used by Actions during creation. [onInitialization] is called when
 * capability exchange has completed and Actions should be initialized and [onRemoteConnected] is
 * called when the remote has connected, finished sending initial state, and is ready to handle
 * Participant action updates.
 */
@ExperimentalAppActions
internal data class ActionExchangeResult(
    val onInitialization: (Boolean) -> Unit,
    val onRemoteConnected: (ParticipantActionsRemote?) -> Unit
)

/**
 * Implements the Participant extension and provides a method for actions to use to register
 * themselves.
 *
 * @param callScope The CoroutineScope of the underlying call
 * @param onActiveParticipantChanged The update callback used whenever the active participants
 *   change
 * @param onParticipantsUpdated The update callback used whenever the participants in the call
 *   change
 */
// TODO: Refactor to Public API
// TODO: Remove old version of ParticipantClientExtension in a follow up CL with this impl.
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal class ParticipantClientExtension(
    private val callScope: CoroutineScope,
    private val onActiveParticipantChanged: suspend (Participant?) -> Unit,
    private val onParticipantsUpdated: suspend (Set<Participant>) -> Unit
) {
    companion object {
        internal const val TAG = CallExtensionsScope.TAG + "(PCE)"
    }

    /**
     * Whether or not the participants extension is supported by the remote.
     *
     * if `true`, then updates about call participants will be notified. If `false`, then the remote
     * doesn't support this extension and participants will not be notified to the caller nor will
     * associated actions receive state updates.
     *
     * Should not be queried until [CallExtensionsScope.onConnected] is called.
     */
    var isSupported by Delegates.notNull<Boolean>()

    /** The actions that are registered with the Participant extension */
    internal val actions
        get() = actionInitializers.keys.toIntArray()

    // Maps a Capability to a receiver that allows the action to register itself with a listener
    // and then return a Receiver that gets called when Cap exchange completes.
    private val actionInitializers = HashMap<Int, ActionExchangeResult>()
    // Manages callbacks that are applicable to sub-actions of the Participants
    private val callbacks = ParticipantStateCallbackRepository()

    // Participant specific state
    private val participants = MutableStateFlow<Set<Participant>>(emptySet())
    private val activeParticipant = MutableStateFlow<Int?>(null)

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
     *
     * @param onRaisedHandsChanged Called when the Set of Participants with their hands raised has
     *   changed.
     * @return The action that is used to send raise hand event requests to the remote Call.
     */
    fun addRaiseHandAction(
        onRaisedHandsChanged: suspend (Set<Participant>) -> Unit
    ): RaiseHandClientAction {
        val action = RaiseHandClientAction(participants, onRaisedHandsChanged)
        registerAction(
            ParticipantExtension.RAISE_HAND_ACTION,
            onRemoteConnected = action::connect
        ) { isSupported ->
            action.initialize(callScope, isSupported, callbacks)
        }
        return action
    }

    /**
     * Adds the ability for the user to kick participants.
     *
     * ```
     * connectExtensions(call) {
     *     val participantExtension = addParticipantExtension(
     *         // consume participant changed events
     *     )
     *     val kickParticipantAction = participantExtension.addKickParticipantAction()
     *
     *     onConnected {
     *         // extensions have been negotiated and actions are ready to be used
     *         ...
     *         // kick a participant
     *         val kickResult = kickParticipantAction.kickParticipant(participant)
     *     }
     * }
     * ```
     *
     * @return The action that is used to send kick Participant event requests to the remote Call.
     */
    fun addKickParticipantAction(): KickParticipantClientAction {
        val action = KickParticipantClientAction(participants)
        registerAction(
            ParticipantExtension.KICK_PARTICIPANT_ACTION,
            onRemoteConnected = action::connect,
            onInitialization = action::initialize
        )
        return action
    }

    /**
     * Register an Action on the Participant extension that will be initialized and connected if the
     * action is supported by the remote Call before [CallExtensionsScope.onConnected] is called.
     *
     * @param action A unique identifier for the action that will be used by the remote side to
     *   identify this action.
     * @param onRemoteConnected The callback called when the remote has connected and action events
     *   can be sent to the remote via [ParticipantActionsRemote].
     * @param onInitialization The Action initializer, which allows the action to setup callbacks.
     *   via [ParticipantStateCallbackRepository] and determine if the action is supported by the
     *   remote Call.
     */
    private fun registerAction(
        action: Int,
        onRemoteConnected: (ParticipantActionsRemote?) -> Unit,
        onInitialization: (Boolean) -> Unit
    ) {
        actionInitializers[action] = ActionExchangeResult(onInitialization, onRemoteConnected)
    }

    /**
     * Capability exchange has completed and the [Capability] of the Participant extension has been
     * negotiated with the remote call.
     *
     * @param negotiatedCapability The negotiated Participant capability or null if the remote
     *   doesn't support this capability.
     * @param remote The remote interface which must be used by this extension to create the
     *   Participant extension on the remote side using the negotiated capability.
     */
    internal suspend fun onExchangeComplete(
        negotiatedCapability: Capability?,
        remote: CapabilityExchangeListenerRemote?
    ) {
        if (negotiatedCapability == null || remote == null) {
            Log.i(TAG, "onNegotiated: remote is not capable")
            isSupported = false
            initializeNotSupportedActions()
            return
        }
        Log.d(TAG, "onNegotiated: setup updates")
        initializeParticipantUpdates()
        initializeActionsLocally(negotiatedCapability)
        val remoteBinder = connectActionsToRemote(negotiatedCapability, remote)
        actionInitializers.forEach { connector -> connector.value.onRemoteConnected(remoteBinder) }
    }

    /**
     * Connect Participant action Flows to the remote interface so we can start receiving changes to
     * the Participant and associated action state.
     *
     * When [CapabilityExchangeListenerRemote.onCreateParticipantExtension] is called, the remote
     * will send the initial state of each of the supported actions and then call
     * [ParticipantStateListener.finishSync], which will provide us an interface to allow us to send
     * participant action event requests.
     *
     * @param negotiatedCapability The negotiated Participant capability that contains a negotiated
     *   version and actions supported by both the local and remote Call.
     * @param remote The interface used by the local call to create the Participant extension with
     *   the remote party if supported and allow for Participant state updates.
     * @return The interface used by the local Call to send Participant action event requests.
     */
    private suspend fun connectActionsToRemote(
        negotiatedCapability: Capability,
        remote: CapabilityExchangeListenerRemote
    ): ParticipantActionsRemote? = suspendCoroutine { continuation ->
        val participantStateListener =
            ParticipantStateListener(
                updateParticipants = { newParticipants ->
                    callScope.launch {
                        Log.v(TAG, "updateParticipants: $newParticipants")
                        participants.emit(newParticipants)
                    }
                },
                updateActiveParticipant = { newActiveParticipant ->
                    callScope.launch {
                        Log.v(TAG, "activeParticipant=$newActiveParticipant")
                        activeParticipant.emit(newActiveParticipant)
                    }
                },
                updateRaisedHands = { newRaisedHands ->
                    callScope.launch {
                        Log.v(TAG, "raisedHands=$newRaisedHands")
                        callbacks.raisedHandsStateCallback?.invoke(newRaisedHands)
                    }
                },
                finishSync = { remoteBinder ->
                    callScope.launch {
                        Log.v(TAG, "finishSync complete, isNull=${remoteBinder == null}")
                        continuation.resume(remoteBinder)
                    }
                }
            )
        remote.onCreateParticipantExtension(
            negotiatedCapability.featureVersion,
            negotiatedCapability.supportedActions,
            participantStateListener
        )
    }

    /** Setup callback updates when [participants] or [activeParticipant] changes */
    private fun initializeParticipantUpdates() {
        participants
            .onEach { participantsState -> onParticipantsUpdated(participantsState) }
            .combine(activeParticipant) { p, ap ->
                ap?.let { p.firstOrNull { participant -> participant.id == ap } }
            }
            .distinctUntilChanged()
            .onEach { activeParticipant -> onActiveParticipantChanged(activeParticipant) }
            .onCompletion { Log.d(TAG, "participant flow complete") }
            .launchIn(callScope)
    }

    /**
     * Calls the [ActionExchangeResult.onInitialization] callback on each registered action
     * (registered via [registerAction]) to initialize. Initialization uses the negotiated
     * [Capability] to determine whether or not the registered action is supported by the remote and
     * provides the ability for the action to register for remote state callbacks.
     *
     * @param negotiatedCapability The negotiated Participant [Capability] containing the
     *   Participant extension version and actions supported by both the local and remote Call.
     */
    private fun initializeActionsLocally(negotiatedCapability: Capability) {
        for (action in actionInitializers) {
            Log.d(TAG, "initializeActions: setup action=${action.key}")
            if (negotiatedCapability.supportedActions.contains(action.key)) {
                Log.d(TAG, "initializeActions: action=${action.key} supported")
                action.value.onInitialization(true)
            } else {
                Log.d(TAG, "initializeActions: action=${action.key} not supported")
                action.value.onInitialization(false)
            }
        }
    }

    /**
     * In the case that participants are not supported, notify all actions that they are also not
     * supported.
     */
    private fun initializeNotSupportedActions() {
        Log.d(TAG, "initializeActions: no actions supported")
        for (action in actionInitializers) {
            action.value.onInitialization(false)
        }
    }
}
