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

import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Interface used to allow the remote surface (automotive, watch, etc...) to know if the connected
 * calling application supports the participant extension and optionally set up additional actions
 * for the [Participant]s in the call.
 *
 * Actions allow the remote surface to display additional optional state regarding the
 * [Participant]s in the call and send action requests to the calling application to modify the
 * state of supported actions.
 */
@ExperimentalAppActions
public interface ParticipantExtensionRemote {

    /**
     * Whether or not the participants extension is supported by the calling application.
     *
     * If `true`, then updates about [Participant]s in the call will be notified. If `false`, then
     * the remote doesn't support this extension and participants will not be notified to the caller
     * nor will associated actions receive state updates.
     *
     * Note: Must not be queried until after [CallExtensionScope.onConnected] is called.
     */
    public val isSupported: Boolean

    /**
     * Adds the "raise hand" action and provides the remote surface with the ability to display
     * which [Participant]s have their hands raised and an action to request to raise and lower
     * their own hand.
     *
     * ```
     * connectExtensions(call) {
     *     val participantExtension = addParticipantExtension(
     *         // consume participant changed events
     *     )
     *     // Initialize the raise hand action
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
     * Note: Must be called during initialization before [CallExtensionScope.onConnected] is called.
     *
     * @param onRaisedHandsChanged Called when the List of [Participant]s with their hands raised
     *   has changed, ordered from oldest raised hand to newest raised hand.
     * @return The action that is used to determine support of this action and send raise hand event
     *   requests to the calling application.
     */
    public fun addRaiseHandAction(
        onRaisedHandsChanged: suspend (List<Participant>) -> Unit
    ): RaiseHandAction

    /**
     * Adds the ability for the user to request to kick [Participant]s in the call.
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
    public fun addKickParticipantAction(): KickParticipantAction
}
