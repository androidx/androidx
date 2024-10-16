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

package androidx.core.telecom.test.ui.calling

import android.net.Uri
import androidx.core.telecom.test.services.CallState
import androidx.core.telecom.test.services.CallType
import androidx.core.telecom.test.services.Direction

/** Defines valid call state transitions */
enum class CallStateTransition {
    ANSWER,
    HOLD,
    UNHOLD,
    NONE,
    DISCONNECT
}

/** UI state and callback container for a Call */
data class CallUiState(
    val id: Int,
    val name: String,
    val photo: Uri?,
    val number: String,
    val state: CallState,
    val validTransition: CallStateTransition,
    val direction: Direction,
    val callType: CallType,
    val onStateChanged: (transition: CallStateTransition) -> Unit,
    val participantUiState: ParticipantExtensionUiState?,
    val localCallSilenceUiState: LocalCallSilenceExtensionUiState?
)
