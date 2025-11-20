/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.reference.view

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.telecom.reference.model.ParticipantState
import androidx.core.telecom.util.ExperimentalAppActions

@Composable
fun ParticipantRow(participant: ParticipantState) {
    ListItem(
        headlineContent = { Text(participant.name + if (participant.isSelf) " (You)" else "") },
        leadingContent = {
            Icon(
                imageVector =
                    if (participant.isActive) Icons.Filled.Person else Icons.Outlined.Person,
                contentDescription =
                    if (participant.isActive) "Active Participant" else "Participant",
                tint =
                    if (participant.isActive) MaterialTheme.colorScheme.primary
                    else LocalContentColor.current,
            )
        },
        trailingContent = {
            Row {
                // Raise Hand Button (only enable for self)
                IconButton(onClick = {}, enabled = participant.isSelf) {
                    Icon(
                        imageVector = Icons.Filled.PanTool,
                        contentDescription =
                            if (participant.isHandRaised) "Lower Hand" else "Raise Hand",
                        tint =
                            if (participant.isHandRaised) MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current.copy(
                                    alpha = if (participant.isSelf) 1f else 0.3f
                                ),
                    )
                }
                // Kick Button (only enable if NOT self)
                IconButton(onClick = {}, enabled = !participant.isSelf) {
                    Icon(
                        imageVector = Icons.Filled.PersonRemove,
                        contentDescription = "Kick Participant",
                        tint =
                            if (!participant.isSelf) Color.Red.copy(alpha = 0.8f)
                            else LocalContentColor.current.copy(alpha = 0.3f),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Participant Row Self")
@Composable
fun ParticipantRowPreview() {
    MaterialTheme { ParticipantRow(sampleSelf) }
}

@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Participant Row Self")
@Composable
fun ParticipantRowAlicePreview() {
    MaterialTheme { ParticipantRow(sampleP1) }
}

@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Participant Row Self")
@Composable
fun ParticipantBobPreview() {
    MaterialTheme { ParticipantRow(sampleP2) }
}
