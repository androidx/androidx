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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.core.telecom.test.R
import kotlinx.coroutines.launch

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Composable
fun ExtensionsContent(
    @PreviewParameter(ParticipantExtensionProvider::class)
    participantUiState: ParticipantExtensionUiState
) {
    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
        Text("Participants")
        if (participantUiState.participants.isEmpty()) {
            Text(modifier = Modifier.padding(horizontal = 6.dp), text = "<No Participants>")
        } else {
            Column(
                modifier =
                    Modifier.height(150.dp)
                        .fillMaxWidth()
                        .padding(6.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                participantUiState.participants.forEach {
                    if (it.isActive) {
                        ActiveParticipantContent(
                            participantUiState.isKickParticipantSupported,
                            participantUiState.isRaiseHandSupported,
                            onRaiseHandStateChanged = participantUiState.onRaiseHandStateChanged,
                            it
                        )
                    } else {
                        NonActiveParticipantContent(
                            participantUiState.isKickParticipantSupported,
                            participantUiState.isRaiseHandSupported,
                            onRaiseHandStateChanged = participantUiState.onRaiseHandStateChanged,
                            it
                        )
                    }
                    Spacer(Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
fun NonActiveParticipantContent(
    isKickSupported: Boolean,
    isRaiseHandSupported: Boolean,
    onRaiseHandStateChanged: suspend (Boolean) -> Unit,
    participant: ParticipantUiState
) {
    ElevatedCard {
        ParticipantContent(
            isKickSupported,
            isRaiseHandSupported,
            onRaiseHandStateChanged,
            participant
        )
    }
}

@Composable
fun ActiveParticipantContent(
    isKickSupported: Boolean,
    isRaiseHandSupported: Boolean,
    onRaiseHandStateChanged: suspend (Boolean) -> Unit,
    participant: ParticipantUiState
) {
    OutlinedCard(
        border = BorderStroke(3.dp, Color.Black),
    ) {
        ParticipantContent(
            isKickSupported,
            isRaiseHandSupported,
            onRaiseHandStateChanged,
            participant
        )
    }
}

@Composable
fun ParticipantContent(
    isKickSupported: Boolean,
    isRaiseHandSupported: Boolean,
    onRaiseHandStateChanged: suspend (Boolean) -> Unit,
    participant: ParticipantUiState
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = rememberCoroutineScope()
        Icon(
            Icons.Rounded.Face,
            modifier = Modifier.size(48.dp),
            contentDescription = "Caller Icon"
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(participant.name)
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        if (isRaiseHandSupported) {
            if (participant.isHandRaised) {
                Icon(
                    painter = painterResource(R.drawable.waving_hand_24px),
                    contentDescription = "hand raised"
                )
            }
        }
        Spacer(modifier = Modifier.padding(horizontal = 6.dp).weight(1f))
        if (participant.isSelf && isRaiseHandSupported) {
            var isRaiseHandEnabled by remember { mutableStateOf(true) }
            if (participant.isHandRaised) {
                Button(
                    enabled = isRaiseHandEnabled,
                    onClick = {
                        scope.launch {
                            isRaiseHandEnabled = false
                            onRaiseHandStateChanged(false)
                            isRaiseHandEnabled = true
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.waving_hand_24px),
                        contentDescription = "lower hand request"
                    )
                }
            } else {
                ElevatedButton(
                    enabled = isRaiseHandEnabled,
                    onClick = {
                        scope.launch {
                            isRaiseHandEnabled = false
                            onRaiseHandStateChanged(true)
                            isRaiseHandEnabled = true
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.waving_hand_24px),
                        contentDescription = "raise hand request"
                    )
                }
            }
        }

        if (!participant.isSelf && isKickSupported) {
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            var isKickEnabled by remember { mutableStateOf(true) }
            ElevatedButton(
                enabled = isKickEnabled,
                onClick = {
                    scope.launch {
                        isKickEnabled = false
                        participant.onKickParticipant()
                        isKickEnabled = true
                    }
                }
            ) {
                Text("Kick")
            }
        }
    }
}
