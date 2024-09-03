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

import androidx.core.telecom.CallControlScope
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * The scope used to initialize extensions on a call as well as manage initialized extensions
 * associated with the call once the call has been set up.
 *
 * Extensions contain state and optional actions that are used to support additional features on a
 * call, such as information about the participants in the call.
 *
 * Supported Extensions:
 * - The ability to describe meeting participant information as well as actions on those
 *   participants using [addParticipantExtension]
 *
 * For example, to add participant support, the participant extension can be created during
 * initialization and then used as part of [onCall] to update participant state and listen to action
 * requests from remote surfaces:
 * ```
 * scope.launch {
 *         mCallsManager.addCallWithExtensions(attributes,
 *             onAnswerLambda,
 *             onDisconnectLambda,
 *             onSetActiveLambda,
 *             onSetInactiveLambda) {
 *                 // Initialize extensions ...
 *                 // Example: add participants support & associated actions
 *                 val participantExtension = addParticipantExtension(initialParticipants)
 *                 val raiseHandState = participantExtension.addRaiseHandSupport(
 *                         initialRaisedHands) { onHandRaisedStateChanged ->
 *                     // handle raised hand state changed
 *                 }
 *                 participantExtension.addKickParticipantSupport {
 *                         participant ->
 *                     // handle kicking the requested participant
 *                 }
 *                 // Call has been set up, perform in-call actions
 *                 onCall {
 *                     // Example: collect call state updates
 *                     callStateFlow.onEach { newState ->
 *                         // handle call state updates
 *                     }.launchIn(this)
 *                     // update participant extensions
 *                     participantsFlow.onEach { newParticipants ->
 *                         participantExtension.updateParticipants(newParticipants)
 *                     }.launchIn(this)
 *                     raisedHandsFlow.onEach { newRaisedHands ->
 *                         raiseHandState.updateRaisedHands(newRaisedHands)
 *                     }.launchIn(this)
 *                 }
 *             }
 *         }
 * }
 * ```
 */
@ExperimentalAppActions
public interface ExtensionInitializationScope {

    /**
     * User provided callback implementation that is run when the call is ready using the provided
     * [CallControlScope].
     *
     * @param onCall callback invoked when the call has been notified to the framework and the call
     *   is ready
     */
    public fun onCall(onCall: suspend CallControlScope.() -> Unit)

    /**
     * Adds the participant extension to a call, which provides the ability for this application to
     * specify participant related information, which will be shared with remote surfaces that
     * support displaying that information (automotive, watch, etc...).
     *
     * @param initialParticipants The initial [Set] of [Participant]s in the call
     * @param initialActiveParticipant The initial [Participant] that is active in the call or
     *   `null` if there is no active participant.
     * @return The interface used by this application to further update the participant extension
     *   state to remote surfaces
     */
    public fun addParticipantExtension(
        initialParticipants: Set<Participant> = emptySet(),
        initialActiveParticipant: Participant? = null
    ): ParticipantExtension

    /**
     * Adds the local call silence extension to a call, which provides the ability for this
     * application to signal to the local call silence state to other surfaces (e.g. Android Auto)
     *
     * Local Call Silence means that the call should be silenced at the application layer (local
     * silence) instead of the hardware layer (global silence). Using a local call silence over
     * global silence is advantageous when the application wants to still receive the audio input
     * data while not transmitting audio input data to remote users.
     *
     * @param initialCallSilenceState The initial call silence value at the start of the call. True,
     *   signals silence the user and do not transmit audio data to the remote users. False signals
     *   the mic is transmitting audio data at the application layer.
     * @param onLocalSilenceUpdate This is called when the user has requested to change their
     *   silence state on a remote surface. If true, this user has requested to silence the
     *   microphone. If false, this user has unsilenced the microphone. This operation should not
     *   return until the request has been processed.
     * @return The interface used by this application to further update the local call silence
     *   extension state to remote surfaces
     */
    public fun addLocalSilenceExtension(
        initialCallSilenceState: Boolean,
        onLocalSilenceUpdate: (suspend (Boolean) -> Unit),
    ): LocalCallSilenceExtension
}
