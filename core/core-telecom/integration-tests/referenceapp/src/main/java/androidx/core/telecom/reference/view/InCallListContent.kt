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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.model.InCallItemUiState
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Displays the list of active/held calls using a [LazyColumn].
 *
 * If the list of calls is empty, it displays a "No active calls" message. Otherwise, it renders a
 * [CallCard] for each call in the list.
 */
@OptIn(ExperimentalAppActions::class)
@Composable
fun InCallListContent(
    calls: List<InCallItemUiState>,
    onMuteClick: (Boolean) -> Unit,
    onEndCallClick: (String) -> Unit,
    onHoldClick: (String) -> Unit,
    onUnholdClick: (String) -> Unit,
    onAudioRouteClick: (String, CallEndpointCompat) -> Unit,
    onAddParticipantClick: (String) -> Unit,
    onRemoveParticipantClick: (String) -> Unit,
    onChangeCallIconClick: (String) -> Unit,
    onLocalCallSilenceClick: (String, Boolean) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize().padding(16.dp).background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(calls, key = { it.callId }) { call ->
            CallCard(
                uiState = call,
                onMuteClick = onMuteClick,
                onEndCallClick = onEndCallClick,
                onHoldClick = onHoldClick,
                onUnholdClick = onUnholdClick,
                onAudioRouteClick = onAudioRouteClick,
                onAddParticipantClick = onAddParticipantClick,
                onRemoveParticipantClick = onRemoveParticipantClick,
                onChangeCallIconClick = onChangeCallIconClick,
                onLocalCallSilenceClick = onLocalCallSilenceClick,
            )
        }
        if (calls.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No active calls", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// Preview for the list content with multiple calls
@Preview(showBackground = true, name = "InCall List Content")
@Composable
@OptIn(ExperimentalAppActions::class)
fun InCallListContentPreview() {
    MaterialTheme {
        InCallListContent(
            calls =
                listOf(sampleCallActive, sampleCallMuted, sampleCallInactive, sampleCallDialing),
            // Provide dummy lambdas for all actions
            onMuteClick = {},
            onEndCallClick = {},
            onHoldClick = {},
            onUnholdClick = {},
            onAudioRouteClick = { _, _ -> },
            onAddParticipantClick = {},
            onRemoveParticipantClick = {},
            onChangeCallIconClick = {},
            onLocalCallSilenceClick = { _, _ -> },
        )
    }
}

// Preview for the list content when empty
@Preview(showBackground = true, name = "InCall List Empty")
@Composable
@OptIn(ExperimentalAppActions::class)
fun InCallListContentEmptyPreview() {
    MaterialTheme {
        InCallListContent(
            calls = emptyList(),
            // Provide dummy lambdas for all actions
            onMuteClick = {},
            onEndCallClick = {},
            onHoldClick = {},
            onUnholdClick = {},
            onAudioRouteClick = { _, _ -> },
            onAddParticipantClick = {},
            onRemoveParticipantClick = {},
            onChangeCallIconClick = {},
            onLocalCallSilenceClick = { _, _ -> },
        )
    }
}
