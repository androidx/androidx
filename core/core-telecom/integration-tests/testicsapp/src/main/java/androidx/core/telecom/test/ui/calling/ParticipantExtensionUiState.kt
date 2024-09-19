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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.telecom.CallControlResult

/** Provide a preview of [ParticipantExtensionUiState] for helping with UI rendering of state. */
class ParticipantExtensionProvider : PreviewParameterProvider<ParticipantExtensionUiState> {
    override val values =
        sequenceOf(
            ParticipantExtensionUiState(
                isRaiseHandSupported = true,
                isKickParticipantSupported = true,
                onRaiseHandStateChanged = { CallControlResult.Success() },
                listOf(
                    ParticipantUiState(
                        "Abraham Lincoln",
                        false,
                        isHandRaised = false,
                        isSelf = true,
                        onKickParticipant = { CallControlResult.Success() }
                    ),
                    ParticipantUiState(
                        "Betty Lapone",
                        true,
                        isHandRaised = true,
                        isSelf = false,
                        onKickParticipant = { CallControlResult.Success() }
                    )
                )
            )
        )
}

/** UI state and actions related to the extensions on a call */
data class ParticipantExtensionUiState(
    val isRaiseHandSupported: Boolean,
    val isKickParticipantSupported: Boolean,
    val onRaiseHandStateChanged: suspend (Boolean) -> Unit,
    val participants: List<ParticipantUiState>
)

/** UI state and actions associated with a participant */
data class ParticipantUiState(
    val name: String,
    val isActive: Boolean,
    val isSelf: Boolean,
    val isHandRaised: Boolean,
    val onKickParticipant: suspend () -> CallControlResult,
)
