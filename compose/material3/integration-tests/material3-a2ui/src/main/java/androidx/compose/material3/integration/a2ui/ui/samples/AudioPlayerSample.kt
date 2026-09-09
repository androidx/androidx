/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.ui.samples

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AudioPlayerSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable { mutableStateOf(DefaultAudioUrl) }
    var description by rememberSaveable { mutableStateOf(DefaultAudioDescription) }

    LaunchedEffect(Unit) {
        updatePayload(url = url, description = description, onPayloadUpdated = onPayloadUpdated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Track Description", subtitle = "Title or summary of the audio track") {
            TextInputControl(
                value = description,
                onValueChange = {
                    description = it
                    updatePayload(url = url, description = it, onPayloadUpdated = onPayloadUpdated)
                },
                label = "Description",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ControlCard(title = "Audio URL", subtitle = "Resource locator for audio stream") {
            TextInputControl(
                value = url,
                onValueChange = {
                    url = it
                    updatePayload(
                        url = it,
                        description = description,
                        onPayloadUpdated = onPayloadUpdated,
                    )
                },
                label = "URL",
            )
        }
    }
}

private fun updatePayload(
    url: String,
    description: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "AudioPlayer",
            properties =
                mapOf(
                    "url" to url,
                    "description" to description,
                ),
        )
    onPayloadUpdated(listOf(component), emptyMap())
}

private const val DefaultAudioUrl =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
private const val DefaultAudioDescription = "Big Buck Bunny Soundtrack"
