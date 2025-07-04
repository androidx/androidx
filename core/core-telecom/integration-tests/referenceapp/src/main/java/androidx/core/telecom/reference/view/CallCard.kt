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

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.model.CallState
import androidx.core.telecom.reference.model.InCallItemUiState
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Displays the UI for a single call, including its state, display name, and action buttons.
 *
 * Shows information like display name and call state. Provides buttons for mute/unmute,
 * hold/unhold, ending the call, and changing the audio route via a dialog.
 */
@OptIn(ExperimentalAppActions::class)
@Composable
fun CallCard(
    uiState: InCallItemUiState,
    onMuteClick: (Boolean) -> Unit,
    onEndCallClick: (String) -> Unit,
    onHoldClick: (String) -> Unit,
    onUnholdClick: (String) -> Unit,
    onAudioRouteClick: (String, CallEndpointCompat) -> Unit,
    onLocalCallSilenceClick: (String, Boolean) -> Unit,
    onAddParticipantClick: (String) -> Unit,
    onRemoveParticipantClick: (String) -> Unit,
    onChangeCallIconClick: (String) -> Unit,
    defaultExpandedState: Boolean = false, // To set initial state
) {
    var isExpanded by remember { mutableStateOf(defaultExpandedState) }
    val expandedCardColor =
        when (isExpanded) {
            true -> MaterialTheme.colorScheme.surfaceContainerHigh
            false -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    val cardOuterPadding =
        when (isExpanded) {
            true -> 6.dp
            false -> 12.dp
        }

    var showAudioRouteDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = expandedCardColor),
        modifier =
            Modifier.animateContentSize()
                .fillMaxWidth()
                .padding(cardOuterPadding)
                .clickable(
                    enabled = uiState.isParticipantExtensionEnabled,
                    onClick = {
                        if (uiState.isParticipantExtensionEnabled) {
                            isExpanded = !isExpanded
                            Log.d(
                                "CallCard",
                                "[${uiState.callId}] Toggled extensions expansion to: $isExpanded",
                            )
                        } else {
                            Log.i("CallCard", "Extensions are not enabled for toggling.")
                        }
                    },
                ),
    ) {
        Column {
            // Section 1: Always visible content (CallerInfo + Main Action Buttons)
            Column(modifier = Modifier.padding(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Face,
                        modifier = Modifier.size(48.dp),
                        contentDescription = "Caller Icon",
                    )
                    Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                        Text(
                            text = uiState.attributes.displayName.toString(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = uiState.attributes.address.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            Text(
                                text =
                                    when (uiState.attributes.callType) {
                                        CallAttributesCompat.CALL_TYPE_AUDIO_CALL -> "Audio Call"
                                        CallAttributesCompat.CALL_TYPE_VIDEO_CALL -> "Video Call"
                                        else -> {
                                            "Unknown CallType"
                                        }
                                    }
                            )
                            VerticalDivider(modifier = Modifier.padding(horizontal = 6.dp))
                            Text(
                                text =
                                    when (uiState.attributes.direction) {
                                        CallAttributesCompat.DIRECTION_INCOMING -> "Incoming"
                                        CallAttributesCompat.DIRECTION_OUTGOING -> "Outgoing"
                                        else -> {
                                            "Unknown Direction"
                                        }
                                    }
                            )
                            VerticalDivider(modifier = Modifier.padding(horizontal = 6.dp))
                            Text(text = uiState.callState.toString())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    if (!uiState.isLocalCallSilenceEnabled) {
                        // Show global mute if local silence isn't active/supported
                        ActionIconButton(
                            icon =
                                if (uiState.isGloballyMuted) Icons.Filled.MicOff
                                else Icons.Filled.Mic,
                            contentDescription = if (uiState.isGloballyMuted) "Unmute" else "Mute",
                            onClick = { onMuteClick(!uiState.isGloballyMuted) },
                        )
                    }

                    if (uiState.currentEndpoint != null) {
                        ActionIconButton(
                            icon = getEndpointIcon(uiState.currentEndpoint),
                            contentDescription =
                                "Audio Route:" + " ${getEndpointName(uiState.currentEndpoint)}",
                            onClick = { showAudioRouteDialog = true },
                        )
                    }

                    if (uiState.callState == CallState.ACTIVE) {
                        ActionIconButton(
                            icon = Icons.Filled.Pause,
                            contentDescription = "Hold",
                            onClick = { onHoldClick(uiState.callId) },
                        )
                    }
                    // Assuming INACTIVE means locally held and can be unheld
                    if (uiState.callState == CallState.INACTIVE) {
                        ActionIconButton(
                            icon = Icons.Filled.PlayArrow,
                            contentDescription = "Unhold",
                            onClick = { onUnholdClick(uiState.callId) },
                        )
                    }
                    ActionIconButton(
                        icon = Icons.Filled.CallEnd,
                        contentDescription = "End Call",
                        onClick = { onEndCallClick(uiState.callId) },
                        tint = Color.Red,
                    )
                }
            }

            // Section 2: Expandable content (Extensions)
            AnimatedVisibility(visible = isExpanded && uiState.isParticipantExtensionEnabled) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .padding(bottom = 6.dp)
                                .padding(8.dp)
                    ) {
                        // Row for Call Icon & Local Call Silence
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top,
                        ) {
                            if (uiState.isCallIconExtensionEnabled) {
                                ExtensionItem(
                                    label = "Call Icon",
                                    onClick = { onChangeCallIconClick(uiState.callId) },
                                ) {
                                    if (uiState.callIconData?.bitmap != null) {
                                        Image(
                                            bitmap = uiState.callIconData.bitmap.asImageBitmap(),
                                            contentDescription = "Call Icon",
                                            modifier = Modifier.size(40.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Person,
                                            "Default Call Icon",
                                            modifier = Modifier.size(40.dp),
                                        )
                                    }
                                }
                            }

                            if (uiState.isLocalCallSilenceEnabled) {
                                ExtensionItem(
                                    label = "Local Silence",
                                    onClick = {
                                        onLocalCallSilenceClick(
                                            uiState.callId,
                                            !uiState.isLocallyMuted,
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector =
                                            if (uiState.isLocallyMuted) Icons.Filled.MicOff
                                            else Icons.Filled.Mic,
                                        contentDescription = "Local Call Silence",
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                            } else {
                                // Placeholder if local silence is not enabled but section is
                                // visible
                                Text(
                                    "Local Silence N/A",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Participant Section
                        ParticipantSection(
                            participants = uiState.participants,
                            onAddParticipant = { onAddParticipantClick(uiState.callId) },
                            onRemoveParticipant = { onRemoveParticipantClick(uiState.callId) },
                        )
                    }
                }
            }
        }
    }

    if (showAudioRouteDialog && uiState.availableEndpoints != null) {
        AlertDialog(
            onDismissRequest = { showAudioRouteDialog = false },
            title = { Text("Select Audio Route") },
            text = {
                Column {
                    uiState.availableEndpoints.forEach { endpoint ->
                        TextButton(
                            onClick = {
                                onAudioRouteClick(uiState.callId, endpoint)
                                showAudioRouteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = getEndpointIcon(endpoint),
                                contentDescription = null,
                                tint =
                                    if (endpoint == uiState.currentEndpoint)
                                        MaterialTheme.colorScheme.primary
                                    else LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getEndpointName(endpoint),
                                color =
                                    if (endpoint == uiState.currentEndpoint)
                                        MaterialTheme.colorScheme.primary
                                    else LocalContentColor.current,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioRouteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Call Card Active")
@Composable
fun CallCardActivePreview() {
    MaterialTheme {
        CallCard(
            uiState = sampleCallActive,
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

// Preview for a single CallCard (Muted Call)
@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Call Card Muted")
@Composable
fun CallCardMutedPreview() {
    MaterialTheme {
        CallCard(
            uiState = sampleCallMuted,
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

// Preview for a single CallCard (Inactive/Held Call)
@OptIn(ExperimentalAppActions::class)
@Preview(showBackground = true, name = "Call Card Inactive")
@Composable
fun CallCardInactivePreview() {
    MaterialTheme {
        CallCard(
            uiState = sampleCallInactive,
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

@Preview(showBackground = true, name = "Call Card Active w/ Participants")
@Composable
@OptIn(ExperimentalAppActions::class)
fun CallCardActiveParticipantsPreview() {
    MaterialTheme {
        CallCard(
            uiState = sampleCallActiveWithParticipants,
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
