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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.material3.integration.a2ui.ui.TextInputControl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun VideoSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable { mutableStateOf(DefaultVideoUrl) }

    LaunchedEffect(Unit) { updatePayload(url = url, onPayloadUpdated = onPayloadUpdated) }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Video URL", subtitle = "Resource locator for video playback") {
            TextInputControl(
                value = url,
                onValueChange = {
                    url = it
                    updatePayload(url = it, onPayloadUpdated = onPayloadUpdated)
                },
                label = "URL",
            )
        }
    }
}

private fun updatePayload(
    url: String,
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
) {
    val component =
        A2uiComponentPayload(
            id = "root",
            type = "Video",
            properties = mapOf("url" to url),
        )
    onPayloadUpdated(listOf(component), emptyMap())
}

private const val DefaultVideoUrl =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
