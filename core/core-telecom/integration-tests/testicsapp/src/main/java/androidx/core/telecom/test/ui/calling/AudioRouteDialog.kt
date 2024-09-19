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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.telecom.test.R
import androidx.core.telecom.test.services.AudioRoute
import androidx.core.telecom.test.ui.calling.OngoingCallsViewModel.Companion.UnknownAudioUiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * The dialog that pops up on the screen when the user tries to change the audio route of the
 * device.
 */
@Composable
fun AudioRoutePickerDialog(
    ongoingCallsViewModel: OngoingCallsViewModel,
    onDismissDialog: () -> Unit,
    onChangeAudioRoute: suspend (String) -> Unit
) {
    val currentAudioRoute: AudioEndpointUiState by
        ongoingCallsViewModel
            .streamCurrentEndpointAudioData()
            .collectAsStateWithLifecycle(UnknownAudioUiState)
    val availableAudioRoutes: List<AudioEndpointUiState> by
        ongoingCallsViewModel
            .streamAvailableEndpointAudioData()
            .collectAsStateWithLifecycle(emptyList())
    Dialog(onDismissRequest = onDismissDialog) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text("Current Audio Route")
                Spacer(modifier = Modifier.padding(vertical = 3.dp))
                OutlinedCard { AudioRouteContent(currentAudioRoute) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text("Available Audio Routes")
                val available = availableAudioRoutes.filter { it.id != currentAudioRoute.id }
                if (available.isEmpty()) {
                    Text(modifier = Modifier.padding(6.dp), text = "<None Available>")
                } else {
                    available.forEach { route ->
                        ClickableAudioRouteContent(route, onChangeAudioRoute)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Composable
fun ClickableAudioRouteContent(
    @PreviewParameter(UserPreviewEndpointProvider::class) audioRoute: AudioEndpointUiState,
    onChangeAudioRoute: suspend (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading: Boolean by remember { mutableStateOf(false) }
    ElevatedCard(
        enabled = !isLoading,
        onClick = {
            coroutineScope.launch {
                isLoading = true
                onChangeAudioRoute(audioRoute.id)
                isLoading = false
            }
        }
    ) {
        AudioRouteContent(audioRoute)
    }
}

@Composable
fun AudioRouteContent(audioRoute: AudioEndpointUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(getResourceForAudioRoute(audioRoute.audioRoute)),
            contentDescription = "audio route details"
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(audioRoute.name)
    }
}

fun getResourceForAudioRoute(audioRoute: AudioRoute): Int {
    return when (audioRoute) {
        AudioRoute.UNKNOWN -> R.drawable.phone_in_talk_24px
        AudioRoute.EARPIECE -> R.drawable.phone_in_talk_24px
        AudioRoute.SPEAKER -> R.drawable.speaker_phone_24px
        AudioRoute.BLUETOOTH -> R.drawable.bluetooth_24px
        AudioRoute.HEADSET -> R.drawable.headset_mic_24px
        AudioRoute.STREAMING -> R.drawable.cast_24px
    }
}
