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
import androidx.core.telecom.internal.MeetingSummaryStateListenerRemote
import androidx.core.telecom.internal.ParticipantActionCallbackRepository
import androidx.core.telecom.internal.ParticipantStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
 * @param initialParticipants The initial list of Participants that are associated with this call.
 * @param initialActiveParticipant The initial active Participant that is associated with this call.
 */
@OptIn(ExperimentalAppActions::class)
@RequiresApi(VERSION_CODES.O)
internal class ParticipantExtensionImpl(
    initialParticipants: List<Participant>,
    initialActiveParticipant: Participant?,
) : ParticipantExtension {
    companion object {
        /**
         * The version of this ParticipantExtension used for capability exchange. Should be updated
         * whenever there is an API change to this extension or an existing action.
         */
        internal const val VERSION = 1
        /**
         * The version of this MeetingSummaryExtension used for capability exchange. Should be
         * updated whenever there is an API change to this extension or an existing action.
         */
        internal const val MEETING_SUMMARY_VERSION = 1

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
    internal val participants: MutableStateFlow<List<Participant>> =
        MutableStateFlow(initialParticipants)

    /** StateFlow containing the active participant of the call if it exists */
    private val activeParticipant: MutableStateFlow<Participant?> =
        MutableStateFlow(initialActiveParticipant)

    /** Maps an action to its [ActionConnector], which will be called during capability exchange */
    private val actionRemoteConnector: HashMap<Int, ActionConnector> = HashMap()

    override suspend fun updateParticipants(newParticipants: List<Participant>) {
        participants.emit(newParticipants.distinct())
    }

    override suspend fun updateActiveParticipant(participant: Participant?) {
        activeParticipant.emit(participant)
    }

    override fun addRaiseHandSupport(
        initialRaisedHands: List<Participant>,
        onHandRaisedChanged: suspend (Boolean) -> Unit,
    ): RaiseHandState {
        val state = RaiseHandStateImpl(participants, initialRaisedHands, onHandRaisedChanged)
        registerAction(RAISE_HAND_ACTION, connector = state::connect)
        return state
    }

    override fun addKickParticipantSupport(onKickParticipant: suspend (Participant) -> Unit) {
        val state = KickParticipantState(participants, onKickParticipant)
        registerAction(KICK_PARTICIPANT_ACTION) { _, repo, _ -> state.connect(repo) }
    }

    /**
     * Setup the participant extension creation callback receiver and return the Capability of this
     * extension to be shared with the remote.
     */
    internal fun onParticipantExchangeStarted(callbacks: CapabilityExchangeRepository): Capability {
        callbacks.onCreateParticipantExtension = ::onCreateParticipantExtension
        return Capability().apply {
            featureId = Extensions.PARTICIPANT
            featureVersion = VERSION
            supportedActions = actionRemoteConnector.keys.toIntArray()
        }
    }

    /**
     * Setup the Meeting Summary extension creation callback receiver and return the Capability of
     * this extension to be shared with the remote.
     */
    internal fun onMeetingSummaryExchangeStarted(
        callbacks: CapabilityExchangeRepository
    ): Capability {
        callbacks.onMeetingSummaryExtension = ::onCreateMeetingSummaryExtension
        return Capability().apply {
            featureId = Extensions.MEETING_SUMMARY
            featureVersion = MEETING_SUMMARY_VERSION
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
     * Creates and initializes the meeting summary extension.
     *
     * This function is responsible for setting up the meeting summary extension, synchronizing the
     * initial state with the remote, and establishing listeners for changes to the participant
     * count and the current speaker.
     *
     * The process involves:
     * 1. **Initial State Synchronization:** Retrieves the initial values of the `participants` list
     *    and the `activeParticipant` from their respective `StateFlow`s. It then sends these
     *    initial values to the remote side using the provided `binder`.
     * 2. **Setting up Flow Listeners:** Creates `Flow` pipelines using `onEach`, `combine`, and
     *    `distinctUntilChanged` to observe changes to both the `participants` list and the
     *    `activeParticipant`.
     *     - `participants.onEach`: This listener triggers whenever the `participants` list changes.
     *       It sends the updated participant count to the remote.
     *     - `combine(activeParticipant)`: This operator combines the latest values from the
     *       `participants` flow and the `activeParticipant` flow. It emits a new value whenever
     *       *either* flow emits. The lambda function checks if the `activeParticipant` is still
     *       present in the `participants` list. If not, it emits `null`.
     *     - `distinctUntilChanged`: This operator ensures that the downstream flow only receives
     *       updates when the value emitted by `combine` actually changes. This prevents redundant
     *       updates to the remote.
     *     - `onEach`: This listener triggers whenever the combined and filtered value changes. It
     *       sends the updated current speaker (or null) to the remote.
     *     - `launchIn(coroutineScope)`: This terminal operator launches the entire flow pipeline in
     *       the provided `coroutineScope`. This means the listeners will remain active as long as
     *       the `coroutineScope` is active.
     * 3. **Finishing Synchronization:** After setting up the listeners, it calls
     *    `binder.finishSync()` to signal to the remote side that the initial synchronization is
     *    complete.
     *
     * @param coroutineScope The [CoroutineScope] in which the flow listeners will be launched. This
     *   scope should be tied to the lifecycle of the component managing the meeting summary
     *   extension to ensure that the listeners are automatically cancelled when the component is
     *   destroyed.
     * @param binder The [MeetingSummaryStateListenerRemote] instance used to communicate with the
     *   remote side. This binder provides methods for updating the participant count and current
     *   speaker.
     */
    private fun onCreateMeetingSummaryExtension(
        coroutineScope: CoroutineScope,
        binder: MeetingSummaryStateListenerRemote,
    ) {
        Log.i(LOG_TAG, "onCreateMeetingSummaryExtension")
        // sync state
        val initParticipants = participants.value
        val initActiveParticipant = activeParticipant.value?.name.toString()
        binder.updateParticipantCount(initParticipants.size)
        binder.updateCurrentSpeaker(initActiveParticipant)
        // Setup listeners for changes to state
        participants
            .onEach { updatedParticipants ->
                Log.i(LOG_TAG, "to remote: updateParticipantCount: ${updatedParticipants.size}")
                binder.updateParticipantCount(updatedParticipants.size)
            }
            .combine(activeParticipant) { p, a ->
                val result = if (a != null && p.contains(a)) a else null
                Log.v(LOG_TAG, "combine: $p + $a = $result")
                result
            }
            .distinctUntilChanged()
            .onEach {
                Log.i(LOG_TAG, "to remote: updateCurrentSpeaker=${it?.name.toString()}")
                binder.updateCurrentSpeaker(it?.name.toString())
            }
            .launchIn(coroutineScope)
        binder.finishSync()
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
        binder: ParticipantStateListenerRemote,
    ) {
        Log.i(LOG_TAG, "onCreatePE: actions=$remoteActions")

        // Synchronize initial state with remote
        val initParticipants = participants.value.distinct()
        val initActiveParticipant = activeParticipant.value
        binder.updateParticipants(initParticipants)
        if (initActiveParticipant != null && initParticipants.contains(initActiveParticipant)) {
            binder.updateActiveParticipant(initActiveParticipant)
        } else {
            binder.updateActiveParticipant(null)
        }

        // Setup listeners for changes to state
        participants
            .onEach { updatedParticipants ->
                Log.i(LOG_TAG, "to remote: updateParticipants: $updatedParticipants")
                binder.updateParticipants(updatedParticipants)
            }
            .combine(activeParticipant) { p, a ->
                val result = if (a != null && p.contains(a)) a else null
                Log.d(LOG_TAG, "combine: $p + $a = $result")
                result
            }
            .distinctUntilChanged()
            .onEach {
                Log.d(LOG_TAG, "to remote: updateActiveParticipant=$it")
                binder.updateActiveParticipant(it)
            }
            .launchIn(coroutineScope)
        Log.d(LOG_TAG, "onCreatePE: finished state update")

        // Setup one callback repository per connection to remote
        val callbackRepository = ParticipantActionCallbackRepository(coroutineScope)
        // Set up actions (only where the remote side supports it)
        actionRemoteConnector
            .filter { entry -> remoteActions.contains(entry.key) }
            .map { entry -> entry.value }
            .forEach { initializer -> initializer(coroutineScope, callbackRepository, binder) }
        Log.d(LOG_TAG, "onCreatePE: calling finishSync")
        binder.finishSync(callbackRepository.eventListener)
    }
}
