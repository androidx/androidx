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
 * The extension interface used to support notifying remote surfaces (automotive, watch, etc...) of
 * state related to the [Participant]s in the call.
 *
 * This interface allows an application to notify remote surfaces of changes to [Participant] state.
 * Additionally, this interface allows the application to support optional actions that use the
 * participant state. These actions provide remote surfaces with the ability to request participant
 * state updates based on user input and provide additional information about the state of specific
 * participants.
 *
 * @see ExtensionInitializationScope.addParticipantExtension
 */
@ExperimentalAppActions
public interface ParticipantExtension {
    /**
     * Update all of the remote surfaces that the [Participant]s of this call have changed.
     *
     * @param newParticipants The new set of [Participant]s associated with this call.
     */
    public suspend fun updateParticipants(newParticipants: Set<Participant>)

    /**
     * Update all of the remote surfaces that the active participant associated with this call has
     * changed, if it exists.
     *
     * The "active" participant is the participant that is currently taking focus and should be
     * marked in UX as active or take a more prominent view to the user.
     *
     * @param participant the [Participant] that is marked as the active participant or `null` if
     *   there is no active participant
     */
    public suspend fun updateActiveParticipant(participant: Participant?)

    /**
     * Adds support for notifying remote surfaces of the "raised hand" state of all [Participant]s
     * in the call.
     *
     * @param initialRaisedHands The initial List of [Participant]s whose hands are raised, ordered
     *   from earliest raised hand to newest raised hand.
     * @param onHandRaisedChanged This is called when the user has requested to change their "raised
     *   hand" state on a remote surface. If `true`, this user has raised their hand. If `false`,
     *   this user has lowered their hand. This operation should not return until the request has
     *   been processed.
     * @return The interface used to update the current raised hand state of all [Participant]s in
     *   the call.
     */
    public fun addRaiseHandSupport(
        initialRaisedHands: List<Participant> = emptyList(),
        onHandRaisedChanged: suspend (Boolean) -> Unit
    ): RaiseHandState

    /**
     * Adds support for allowing the user to kick participants in the call using the remote surface.
     *
     * @param onKickParticipant The action to perform when the user requests to kick a participant.
     *   This operation should not return until the request has been processed.
     */
    public fun addKickParticipantSupport(onKickParticipant: suspend (Participant) -> Unit)
}
