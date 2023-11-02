/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.extensions.voip

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import androidx.core.telecom.extensions.Capability
import androidx.core.telecom.extensions.IParticipantStateListener
import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.extensions.voip.VoipExtensionManager.Companion.isActionSupportedByIcs
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils
import androidx.core.telecom.internal.utils.CapabilityExchangeUtils.Companion.preprocessSupportedActions
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * VOIP side manager for handling the Participant extension. The manager is set up if the VOIP app
 * supports the extension and allows ICS to subscribe to these updates. Internally, the VOIP app
 * does version handling to ensure backwards compatibility.
 */
@ExperimentalAppActions
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.O)
internal class VoipParticipantExtensionManager(
    private val session: CallControlScope,
    private val coroutineContext: CoroutineContext,
    private val callChannels: CallChannels,
    voipCapability: Capability
) {
    // List of actions supported by the VOIP app (sanitized for potential user input error).
    private val voipSupportedActions: Set<@CallsManager.Companion.ExtensionSupportedActions Int> =
        preprocessSupportedActions(
            voipCapability.featureId, voipCapability.supportedActions)

    // Keep track of ICS subscribers. This contains the listener which the VOIP app can use to send
    // updates to the ICS, the negotiated actions between the ICS and VOIP app, as well as the
    // version, to handle backwards compatibility.
    private val activeSubscribers: MutableMap<Int, Triple<IParticipantStateListener,
        IntArray, Int>> = HashMap()

    // Singleton that references the delegates that will be used to support the kotlin extensions
    // being added into CallControlScope for supporting capabilities.
    private val extensionSingleton = CallControlScopeExtensionSingleton.getInstance()

    // Jobs that are run to handle updates to the participants state. The lifecycle needs to be
    // managed explicitly so that these jobs are cancelled when the call session is terminated.
    internal lateinit var participantUpdateJob: Job
    internal lateinit var activeParticipantsJob: Job
    internal lateinit var raisedHandParticipantsJob: Job
    internal lateinit var callbackActionsJob: Job

    companion object {
        private val TAG = VoipParticipantExtensionManager::class.simpleName

        /**
         * Convert list of participants into a list of participant ids.
         */
        internal fun resolveIdsFromParticipants(participants: Set<Participant>): IntArray {
            val participantIds = IntArray(participants.size)
            participants.forEachIndexed { index, participant ->
                participantIds[index] = participant.id
            }
            return participantIds
        }
    }

    init {
        // Set up MutableFlowStates with initial values so that it can be accessed by the VOIP app
        // via CallControlScope and be used as the source of truth for representing the current
        // Participant state.
        registerParticipantExtension()
        startHandlingUpdates()
    }

    /***********************************************************************************************
     *                           Internal Functions
     *********************************************************************************************/

    /**
     * Subscribe ICS side for updates. This is done when the VOIP app is notified that the ICS
     * supports Participant extensions via
     * [CapabilityExchangeListener.onCreateParticipantExtension], which passes along the listener
     * that the VOIP app can use to send updates to the ICS as well as the negotiated actions
     * resolved from the ICS side.
     *
     * @param icsId id for the ICS that is subscribing to updates.
     * @param participantStateListener listener provided by the ICS that the VOIP uses to send
     *                                 participant state updates.
     * @param icsSupportedActions actions supported by the ICS.
     * @param version supported by the ICS.
     */
    internal fun subscribeToVoipUpdates(
        icsId: Int,
        participantStateListener: IParticipantStateListener,
        icsSupportedActions: IntArray,
        version: Int
    ) {
        Log.i(TAG, "Subscribing ICS $icsId to receive participant extension updates.")
        activeSubscribers[icsId] = Triple(participantStateListener, icsSupportedActions, version)
        var currentActiveParticipantId = CapabilityExchangeUtils.NULL_PARTICIPANT_ID
        session.activeParticipant!!.value?.let {
            currentActiveParticipantId = it.id
        }

        // Todo: Version handling
        // Send initial states via ParticipantStateListener. Flows should already be instantiated
        // at this point.
        try {
            participantStateListener.updateParticipants(session.participants!!.value.toTypedArray())
            participantStateListener.updateActiveParticipant(currentActiveParticipantId)
            participantStateListener.updateRaisedHandsAction(
                resolveIdsFromParticipants(session.raisedHandParticipants!!.value)
            )

            // Notify the ICS that sync has been completed, providing a callback that it can
            // invoke actions on.
            participantStateListener.finishSync(
                VoipParticipantActions(
                    session, callChannels, voipSupportedActions
                )
            )
        } catch (e: Exception) {
            CapabilityExchangeUtils.handleVoipSideUpdateExceptions(TAG!!, "subscribeToVoipUpdates",
                CapabilityExchangeUtils.PARTICIPANT_TAG, e)
            // If an exception was thrown before or during finishSync(), there's no reason to
            // consider the ICS as an active subscriber.
            unsubscribeFromUpdates(icsId)
        }
    }

    /**
     * Unsubscribes the ICS side from receiving updates around call detail extensions. This would
     * be invoked when the ICS signals the VOIP side via
     * [CapabilityExchangeListener.onRemoveExtensions].
     *
     * @param icsId indicating which ICS to unsubscribe.
     */
    internal fun unsubscribeFromUpdates(icsId: Int): Boolean {
        Log.i(TAG, "Unsubscribing ICS $icsId from receiving participant extension updates.");
        return activeSubscribers.remove(icsId) != null
    }

    /**
     * Tear down manager to stop providing updates and clear delegate mapping.
     */
    internal fun tearDown() {
        Log.i(TAG, "Tearing down participants extension.");
        // Cancel jobs providing updates.
        cancelJobs()
        // Remove delegate mapping to CallControlScope extensions defined for this session.
        extensionSingleton.PARTICIPANT_DELEGATE.remove(session.getCallId())
        // Remove delegate mapping to extensions for this session.
        activeSubscribers.clear()
        // Close channel that receives Participant action requests from the ICS side.
        callChannels.voipParticipantActionRequestsChannel.close()
    }

    /***********************************************************************************************
     *                           Private Helpers
     *********************************************************************************************/

    /**
     * Register participant extension support on the VOIP side. This sets up the flows with the
     * initial empty states.
     *
     * Note: The CallControlScope extension properties would be undefined until this function is
     * invoked.
     */
    private fun registerParticipantExtension() {
        Log.i(TAG, "Registering participant extension for VOIP app.")
        // Create initial states and set up flows on the VOIP side for tracking updates.
        extensionSingleton.PARTICIPANT_DELEGATE[session.getCallId()] = ParticipantApiDelegate(
            MutableStateFlow(setOf()), MutableStateFlow(null), MutableStateFlow(setOf()))
    }

    /**
     * Begin setting up coroutines to handle updates for the participant state as well as the
     * incoming requests from the ICS to perform an action.
     */
    private fun startHandlingUpdates() {
        // Handle participants state updates and propagate to active ICS subscribers.
        processStateUpdates()
        // List of requests to be processed for invoking actions from ICS side.
        processActionRequests()
    }

    /**
     * Sets up CallControlScope extensions to collect updates from the specified flows so that the
     * VOIP side can propagate the updates to the ICS side.
     */
    private fun processStateUpdates() {
        participantUpdateJob = CoroutineScope(coroutineContext).launch {
            // For each ICS, send update via respective channels.
            session.participants?.collect {
                for ((icsId, subscriber) in activeSubscribers.entries) {
                    Log.i(TAG, "Updating participants state for ICS $icsId.")
                    val participantStateListener = subscriber.first
                    try {
                        participantStateListener.updateParticipants(it.toTypedArray())
                    } catch (e: Exception) {
                        CapabilityExchangeUtils.handleVoipSideUpdateExceptions(TAG!!,
                            "participantsUpdate", CapabilityExchangeUtils.PARTICIPANT_TAG, e)
                    }
                }
            }
        }

        activeParticipantsJob = CoroutineScope(coroutineContext).launch {
            // For each ICS, send update via respective channels.
            session.activeParticipant?.collect { participant ->
                var participantId = CapabilityExchangeUtils.NULL_PARTICIPANT_ID
                participant?.let {
                    participantId = it.id
                }
                // Send updates to all active ICS subscribers.
                for ((icsId, subscriber) in activeSubscribers.entries) {
                    Log.i(TAG, "Updating active participant state for ICS $icsId.")
                    val participantStateListener = subscriber.first
                    try {
                        participantStateListener.updateActiveParticipant(participantId)
                    } catch (e: Exception) {
                        CapabilityExchangeUtils.handleVoipSideUpdateExceptions(TAG!!,
                            "activeParticipantsUpdate", CapabilityExchangeUtils.PARTICIPANT_TAG, e)
                    }
                }
            }
        }

        raisedHandParticipantsJob = CoroutineScope(coroutineContext).launch {
            // For each ICS, send update via respective channels. If the action isn't supported by
            // the VOIP app, the flow will be null and no updates will be sent to the ICS side.
            session.raisedHandParticipants?.collect {
                val participantIds: IntArray = resolveIdsFromParticipants(it)
                for ((icsId, subscriber) in activeSubscribers.entries) {
                    val icsSupportedActions = subscriber.second
                    // Only send updates to ICS that support the raise hand action.
                    if (isActionSupportedByIcs(icsSupportedActions,
                            CallsManager.RAISE_HAND_ACTION)) {
                        Log.i(TAG, "Updating raise hand state for ICS $icsId.")
                        val participantStateListener = subscriber.first
                        try {
                            participantStateListener.updateRaisedHandsAction(participantIds)
                        } catch (e: Exception) {
                            CapabilityExchangeUtils.handleVoipSideUpdateExceptions(TAG!!,
                                "raisedHandsUpdate", CapabilityExchangeUtils.PARTICIPANT_TAG, e)
                        }
                    }
                }
            }
        }
    }

    /**
     * The requests for the callbacks are queued to the voipParticipantActionRequestsChannel to be
     * processed sequentially by the VOIP side.
     */
    private fun processActionRequests() {
        callbackActionsJob = CoroutineScope(coroutineContext).launch {
            callChannels.voipParticipantActionRequestsChannel.consumeEach {
                it.processAction()
            }
        }
    }

    /**
     * Manages the lifecycle of the launched coroutines to ensure that they are properly tore down
     * when the call session is terminated.
     */
    private fun cancelJobs() {
        participantUpdateJob.cancel()
        activeParticipantsJob.cancel()
        raisedHandParticipantsJob.cancel()
        callbackActionsJob.cancel()
    }

    /***********************************************************************************************
     *                          Internal Class Helpers
     *********************************************************************************************/

    /**
     * Encapsulates the participant extension states for V1 (participants, activeParticipant, and
     * which participants have their hand raised).
     */
    internal class ParticipantApiDelegate(
        internal val participantsFlow: MutableStateFlow<Set<Participant>>,
        internal val activeParticipantFlow: MutableStateFlow<Participant?>,
        internal val raisedHandParticipantsFlow: MutableStateFlow<Set<Participant>>
    )
}

/***********************************************************************************************
 *                          CallControlScope Extensions
 *********************************************************************************************/

/**
 * Extension properties/ functions to be supported for voip actions. Extension properties cannot
 * be translated to backing fields so, internally, we need to resolve the APIs for each
 * CallControlScope using a delegate to mimic this behavior.
 */
internal val CallControlScope.activeParticipant: MutableStateFlow<Participant?>?
    @ExperimentalAppActions
    @RequiresApi(Build.VERSION_CODES.O)
    get() = CallControlScopeExtensionSingleton.getInstance()
        .PARTICIPANT_DELEGATE[getCallId()]?.activeParticipantFlow

internal val CallControlScope.participants: MutableStateFlow<Set<Participant>>?
    @ExperimentalAppActions
    @RequiresApi(Build.VERSION_CODES.O)
    get() = CallControlScopeExtensionSingleton.getInstance()
        .PARTICIPANT_DELEGATE[getCallId()]?.participantsFlow

internal val CallControlScope.raisedHandParticipants: MutableStateFlow<Set<Participant>>?
    @ExperimentalAppActions
    @RequiresApi(Build.VERSION_CODES.O)
    get() = CallControlScopeExtensionSingleton.getInstance()
        .PARTICIPANT_DELEGATE[getCallId()]?.raisedHandParticipantsFlow
