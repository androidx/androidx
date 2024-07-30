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
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.ParticipantActionsRemote
import androidx.core.telecom.internal.ParticipantStateListener
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@ExperimentalAppActions
internal fun interface ParticipantsUpdate {
    /**
     * The participants in the call have been updated.
     *
     * @param participants The new Set of Participants
     */
    suspend fun onParticipantsUpdated(participants: Set<Participant>)
}

@ExperimentalAppActions
internal fun interface ActiveParticipantsUpdate {
    /**
     * The active participant in the call has changed
     *
     * @param participant The Participant that is active in the call or `null` if there is no active
     *   participant
     */
    suspend fun onActiveParticipantChanged(participant: Participant?)
}

@ExperimentalAppActions
internal fun interface RaisedHandsUpdate {
    /**
     * The participants in the call with their hands raised has changed.
     *
     * @param participants The Set of Participants with their hands raised.
     */
    suspend fun onRaisedHandsChanged(participants: Set<Participant>)
}

/**
 * Add support for representing Participants in this call.
 *
 * ```
 * connectExtensions(call) {
 *     val participantExtension = addParticipantExtension(
 *         // consume participant changed events
 *     )
 *     onConnected {
 *         // extensions have been negotiated and actions are ready to be used
 *     }
 * }
 * ```
 */
// TODO: Refactor to Public API
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal fun CallExtensionsScope.addParticipantExtension(
    activeParticipantsUpdate: ActiveParticipantsUpdate,
    participantsUpdate: ParticipantsUpdate
): ParticipantClientExtension {
    val extension =
        ParticipantClientExtension(callScope, activeParticipantsUpdate, participantsUpdate)
    registerExtension {
        CallExtensionCreationDelegate(
            capability =
                Capability().apply {
                    featureId = CallsManager.PARTICIPANT
                    featureVersion = 1
                    supportedActions = extension.actions.keys.toIntArray()
                },
            receiver = extension::onNegotiated
        )
    }
    return extension
}

/** Repository containing the callbacks associated with the Participant extension state changes */
@ExperimentalAppActions
internal class ParticipantStateCallbackRepository {
    var raisedHandsStateCallback: (suspend (Set<Int>) -> Unit)? = null

    /** Called by the remote application when this state changes */
    suspend fun raisedHandsStateChanged(newState: Set<Int>) {
        raisedHandsStateCallback?.invoke(newState)
    }
}

/**
 * Initializer allowing an action to first register callbacks to
 * [ParticipantStateCallbackRepository] as part of initialization as well as provides a
 * [CoroutineScope] to attach callbacks from the remote party to and whether or not the action is
 * supported.
 */
@OptIn(ExperimentalAppActions::class)
internal typealias ActionInitializer =
    ParticipantStateCallbackRepository.(CoroutineScope, Boolean) -> Unit

/**
 * Called with the remote interface that will be used to notify the remote application of when an
 * there is an event to send related to action.
 */
@OptIn(ExperimentalAppActions::class)
internal typealias ActionConnectedCallback = (ParticipantActionsRemote?) -> Unit

/**
 * Contains the callbacks used by Actions during creation. [onInitialization] is called when
 * capability exchange has completed and [onActionConnected] is called when the remote has connected
 * and is ready to handle Participant action updates.
 */
@ExperimentalAppActions
internal data class ActionExchangeResult(
    val onInitialization: ActionInitializer,
    val onActionConnected: ActionConnectedCallback
)

/**
 * Implements the Participant extension and provides a method for actions to use to register
 * themselves.
 *
 * @param callScope The CoroutineScope of the underlying call
 * @param activeParticipantsUpdate The update callback used whenever the active participants change
 * @param participantsUpdate The update callback used whenever the participants in the call change
 */
// TODO: Refactor to Public API
// TODO: Remove old version of ParticipantClientExtension in a follow up CL with this impl.
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalAppActions
internal class ParticipantClientExtension(
    private val callScope: CoroutineScope,
    private val activeParticipantsUpdate: ActiveParticipantsUpdate,
    private val participantsUpdate: ParticipantsUpdate
) {
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
    // Maps a Capability to a receiver that allows the action to register itself with a listener
    // and then return a Receiver that gets called when Cap exchange completes.
    internal val actions = HashMap<Int, ActionExchangeResult>()
    // Manages callbacks that are applicable to sub-actions of the Participants
    private val callbacks = ParticipantStateCallbackRepository()

    // Participant specific state
    internal val participants = MutableStateFlow<Set<Participant>>(emptySet())
    private val activeParticipant = MutableStateFlow<Int?>(null)

    // Holds the remote Binder interface that is used to send action events back to the remote
    // application.
    private val remoteBinderListener = MutableSharedFlow<ParticipantActionsRemote?>()
    // Leapfrogs from the AIDL callback to scheduling delivery of these updates via the callScope
    // coroutine
    private val participantStateListener =
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
                    callbacks.raisedHandsStateChanged(newRaisedHands)
                }
            },
            finishSync = { remoteBinder ->
                callScope.launch {
                    Log.v(TAG, "finishSync complete, isNull=${remoteBinder == null}")
                    remoteBinderListener.emit(remoteBinder)
                }
            }
        )

    companion object {
        const val TAG = CallExtensionsScope.TAG + "(PCE)"
    }

    /**
     * Register an Action on the Participants extension.
     *
     * @param action An `Int` describing the action
     * @param onRemoteConnected The callback called when the remote has connected and action events
     *   can be sent to the remote via [ParticipantActionsRemote]
     * @param onInitialization The Action initializer, which allows the action to setup callbacks
     *   via [ParticipantStateCallbackRepository] and determine if the action is supported.
     */
    fun registerAction(
        action: Int,
        onRemoteConnected: ActionConnectedCallback,
        onInitialization: ActionInitializer
    ) {
        actions[action] = ActionExchangeResult(onInitialization, onRemoteConnected)
    }

    /**
     * The Participant extension has been negotiated
     *
     * @param negotiatedCapability The negotiated Participant capability or null if the remote
     *   doesn't support this capability.
     * @param remote The remote interface to use to create the Participant extension on the remote
     *   side using the negotiated capability.
     */
    suspend fun onNegotiated(
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
        initializeActions(negotiatedCapability)
        remote.onCreateParticipantExtension(
            negotiatedCapability.featureVersion,
            negotiatedCapability.supportedActions,
            participantStateListener
        )
        val remoteBinder = remoteBinderListener.firstOrNull()
        actions.forEach { connector -> connector.value.onActionConnected(remoteBinder) }
    }

    /** Setup callback updates when [participants] or [activeParticipant] changes */
    private fun initializeParticipantUpdates() {
        participants
            .onEach { participantsState ->
                participantsUpdate.onParticipantsUpdated(participantsState)
            }
            .combine(activeParticipant) { p, ap ->
                ap?.let { p.firstOrNull { participant -> participant.id == ap } }
            }
            .distinctUntilChanged()
            .onEach { activeParticipant ->
                activeParticipantsUpdate.onActiveParticipantChanged(activeParticipant)
            }
            .onCompletion { Log.d(TAG, "participant flow complete") }
            .launchIn(callScope)
    }

    /**
     * Call the [ActionInitializer] callback on each action to initialize the action with whether or
     * not the action is supported and provide the ability for the action to register for remote
     * state callbacks.
     */
    private fun initializeActions(negotiatedCapability: Capability) {
        for (action in actions) {
            Log.d(TAG, "initializeActions: setup action=${action.key}")
            if (negotiatedCapability.supportedActions.contains(action.key)) {
                Log.d(TAG, "initializeActions: action=${action.key} supported")
                action.value.onInitialization(callbacks, callScope, true)
            } else {
                Log.d(TAG, "initializeActions: action=${action.key} not supported")
                action.value.onInitialization(callbacks, callScope, false)
            }
        }
    }

    /**
     * In the case that participants are not supported, notify all actions that they are also not
     * supported.
     */
    private fun initializeNotSupportedActions() {
        Log.d(TAG, "initializeActions: no actions supported")
        for (action in actions) {
            action.value.onInitialization(callbacks, callScope, false)
        }
    }
}
