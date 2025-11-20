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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.reference.model.ParticipantState
import androidx.core.telecom.util.ExperimentalAppActions
import kotlin.collections.forEach

@OptIn(ExperimentalAppActions::class)
@Composable
fun ParticipantSection(
    participants: List<ParticipantState>,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onAddParticipant) {
                Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
            Button(onClick = onRemoveParticipant, enabled = participants.any { !it.isSelf }) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Remove Last")
            }
        }
        Spacer(Modifier.height(16.dp))

        Column {
            if (participants.isEmpty()) {
                Text(
                    "No participants",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                )
            } else {
                participants.forEach { participant -> ParticipantRow(participant = participant) }
            }
        }
    }
}

@Preview(showBackground = true, name = "Participant Dialog")
@Composable
@OptIn(ExperimentalAppActions::class)
fun ParticipantDialogPreview() {
    MaterialTheme {
        ParticipantSection(
            participants = sampleParticipants,
            onAddParticipant = {},
            onRemoveParticipant = {},
        )
    }
}
