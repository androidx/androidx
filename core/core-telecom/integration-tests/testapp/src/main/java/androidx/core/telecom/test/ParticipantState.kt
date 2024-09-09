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

/** The state of one participant in a call */
data class ParticipantState(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val isHandRaised: Boolean,
    val isSelf: Boolean
)

/** Control callback handler for adding/removing new participants in the Call via UI */
data class ParticipantControl(
    val onParticipantAdded: () -> Unit,
    val onParticipantRemoved: () -> Unit
)

@OptIn(ExperimentalAppActions::class)
fun ParticipantState.toParticipant(): Participant {
    return Participant(id, name)
}
