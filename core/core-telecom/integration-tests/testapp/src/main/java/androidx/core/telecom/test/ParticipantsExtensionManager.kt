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

package androidx.core.telecom.test

import androidx.core.telecom.extensions.Participant
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Manages the extension in a Call */
@OptIn(ExperimentalAppActions::class)
class ParticipantsExtensionManager {
    companion object {
        // Represents "self" in participants window, which allows raise hand state modification and
        // no kicking
        internal val SELF_PARTICIPANT =
            ParticipantState(
                "0",
                "Participant 0",
                isHandRaised = false,
                isActive = false,
                isSelf = true
            )
    }

    private val nextId = AtomicInteger(1)
    private val mParticipants: MutableStateFlow<List<ParticipantState>> =
        MutableStateFlow(listOf(SELF_PARTICIPANT))
    /** The current state of participants for the given call */
    val participants = mParticipants.asStateFlow()

    /** Adds a new Participant to the call. */
    fun addParticipant() {
        val id = nextId.getAndAdd(1)
        mParticipants.update {
            ArrayList(it).apply {
                add(
                    ParticipantState(
                        id = "$id",
                        name = "Participant $id",
                        isHandRaised = false,
                        isActive = false,
                        isSelf = false
                    )
                )
            }
        }
    }

    /** Removes the last participant in the List */
    fun removeParticipant() {
        mParticipants.update { participants ->
            if (participants.isEmpty()) return
            if (participants.last().isSelf) return
            ArrayList(participants).apply { remove(last()) }
        }
    }

    /** randomly change all Participant raise hand/active states one time */
    fun changeParticipantStates() {
        mParticipants.update { participants ->
            // Randomly choose a participant to make active & get hand raised
            val nextActive = Random.nextInt(0, participants.size + 1) - 1
            var raisedHandParticipant: ParticipantState? = null
            if (participants.size > 1) {
                val nextRaisedHand = Random.nextInt(0, participants.size)
                if (nextRaisedHand > 0) {
                    // self controls their own raised hand
                    raisedHandParticipant = participants.getOrNull(nextRaisedHand)
                }
            }
            val activeParticipant = participants.getOrNull(nextActive)

            participants.map { p ->
                ParticipantState(
                    id = p.id,
                    name = p.name,
                    isActive = activeParticipant?.id == p.id,
                    isHandRaised =
                        if (SELF_PARTICIPANT.id != p.id) {
                            raisedHandParticipant?.id == p.id
                        } else {
                            p.isHandRaised
                        },
                    isSelf = p.isSelf
                )
            }
        }
    }

    /** Change the raised hand state of the participant representing this user */
    fun onRaisedHandStateChanged(isHandRaised: Boolean) {
        mParticipants.update { state ->
            val newState = ArrayList<ParticipantState>()
            for (p in state) {
                if (p.id == SELF_PARTICIPANT.id) {
                    newState.add(
                        ParticipantState(
                            id = p.id,
                            name = p.name,
                            isActive = p.isActive,
                            isHandRaised = isHandRaised,
                            isSelf = p.isSelf
                        )
                    )
                } else {
                    newState.add(p)
                }
            }
            newState
        }
    }

    /** Kick a participant as long as it is not this user */
    fun onKickParticipant(participant: Participant) {
        mParticipants.update { state ->
            if (participant.id == SELF_PARTICIPANT.id) return
            val candidate = state.firstOrNull { it.id == participant.id }
            if (candidate == null) return
            ArrayList(state).apply { remove(candidate) }
        }
    }
}
