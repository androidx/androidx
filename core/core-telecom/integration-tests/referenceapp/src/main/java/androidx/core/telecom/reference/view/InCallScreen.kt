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

import android.net.Uri
import android.os.ParcelUuid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.model.CallState
import androidx.core.telecom.reference.model.IconData
import androidx.core.telecom.reference.model.InCallItemUiState
import androidx.core.telecom.reference.model.ParticipantState
import androidx.core.telecom.reference.viewModel.InCallViewModel
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.UUID

/**
 * The main composable function for the In-Call screen.
 *
 * Observes the list of current calls from the [InCallViewModel] and displays them using
 * [InCallListContent]. It also handles displaying an [AlertDialog] if any call enters an error
 * state (indicated by [InCallItemUiState.hasCallException]).
 *
 * @param inCallViewModel The ViewModel providing the state for the in-call screen.
 */
@OptIn(ExperimentalAppActions::class)
@Composable
fun InCallScreen(inCallViewModel: InCallViewModel) {
    val uiState by inCallViewModel.uiState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var displayedErrorCallId by remember { mutableStateOf<String?>(null) }

    uiState.forEach { call ->
        if (call.hasCallException != null && displayedErrorCallId != call.callId) {
            LaunchedEffect(call.callId) {
                if (displayedErrorCallId == null || displayedErrorCallId != call.callId) {
                    showErrorDialog = true
                    displayedErrorCallId = call.callId
                }
            }
        }
    }

    if (showErrorDialog) {
        val errorCall = uiState.firstOrNull { it.callId == displayedErrorCallId }
        errorCall?.let {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Call Error") },
                text = { Text(it.hasCallException?.message ?: "An unknown error occurred.") },
                confirmButton = { Button(onClick = { showErrorDialog = false }) { Text("OK") } },
            )
        }
    }

    InCallListContent(
        calls = uiState,
        onMuteClick = inCallViewModel::toggleGlobalMute,
        onEndCallClick = inCallViewModel::endCall,
        onHoldClick = inCallViewModel::holdCall,
        onUnholdClick = inCallViewModel::setCallActive,
        onAudioRouteClick = inCallViewModel::onAudioRouteClicked,
        onAddParticipantClick = inCallViewModel::addParticipant,
        onRemoveParticipantClick = inCallViewModel::removeParticipant,
        onChangeCallIconClick = inCallViewModel::changeCallIcon,
        onLocalCallSilenceClick = inCallViewModel::toggleLocalCallSilence,
    )
}

// --- Previews Data Starts Here ---

fun createSampleEndpoint(type: Int, name: String): CallEndpointCompat {
    return CallEndpointCompat(name, type, ParcelUuid.fromString(UUID.randomUUID().toString()))
}

// Sample data for previews
val sampleEarpiece = createSampleEndpoint(CallEndpointCompat.TYPE_EARPIECE, "Earpiece")
val sampleSpeaker = createSampleEndpoint(CallEndpointCompat.TYPE_SPEAKER, "Speaker")
val sampleBluetooth = createSampleEndpoint(CallEndpointCompat.TYPE_BLUETOOTH, "Bluetooth Headset")
val sampleEndpoints = listOf(sampleEarpiece, sampleSpeaker, sampleBluetooth)

val sampleCallActive =
    InCallItemUiState(
        callId = "call_1",
        displayName = "Alice Wonderland",
        callState = CallState.ACTIVE,
        isGloballyMuted = false,
        currentEndpoint = sampleEarpiece,
        availableEndpoints = sampleEndpoints,
        hasCallException = null,
        isSpeakerOn = false,
        isVideoCall = false,
        isCallActive = true,
        attributes =
            CallAttributesCompat(
                "Alice Wonderland",
                Uri.parse("123"),
                CallAttributesCompat.DIRECTION_INCOMING,
            ),
    )

val sampleCallMuted =
    InCallItemUiState(
        callId = "call_2",
        displayName = "Bob The Builder",
        callState = CallState.ACTIVE,
        isGloballyMuted = true,
        currentEndpoint = sampleSpeaker,
        availableEndpoints = sampleEndpoints,
        hasCallException = null,
        isSpeakerOn = false,
        isVideoCall = true,
        isCallActive = false,
        attributes =
            CallAttributesCompat(
                "Bob The Builder",
                Uri.parse("456"),
                CallAttributesCompat.DIRECTION_OUTGOING,
            ),
    )

val sampleCallInactive =
    InCallItemUiState(
        callId = "call_3",
        displayName = "Charlie Chaplin",
        callState = CallState.INACTIVE, // Or CallState.HOLDING if that's used
        isGloballyMuted = false,
        currentEndpoint = sampleBluetooth,
        availableEndpoints = sampleEndpoints,
        hasCallException = null,
        isSpeakerOn = false,
        isVideoCall = false,
        isCallActive = false,
        attributes =
            CallAttributesCompat(
                "Charlie Chaplin",
                Uri.parse("789"),
                CallAttributesCompat.DIRECTION_OUTGOING,
            ),
    )

val sampleCallDialing =
    InCallItemUiState(
        callId = "call_4",
        displayName = "Diana Prince",
        callState = CallState.DIALING,
        isGloballyMuted = false,
        currentEndpoint = sampleEarpiece,
        availableEndpoints = sampleEndpoints,
        hasCallException = null,
        isSpeakerOn = true,
        isVideoCall = true,
        isCallActive = false,
        attributes =
            CallAttributesCompat(
                "Diana Prince",
                Uri.parse("345"),
                CallAttributesCompat.DIRECTION_OUTGOING,
            ),
    )

val sampleSelf = ParticipantState("p_self", "Me", false, false, true)
val sampleP1 = ParticipantState("p_1", "Alice", true, false, false)
val sampleP2 = ParticipantState("p_2", "Bob", false, true, false)
val sampleParticipants = listOf(sampleSelf, sampleP1, sampleP2)

val sampleCallActiveWithParticipants =
    sampleCallActive.copy(
        participants = sampleParticipants,
        isParticipantExtensionEnabled = true,
        callIconData =
            IconData(
                Uri.parse(
                    "android.resource://androidx.core.telecom.reference/drawable/ic_launcher_foreground"
                ),
                null,
                "",
            ),
        isCallIconExtensionEnabled = true,
    )
