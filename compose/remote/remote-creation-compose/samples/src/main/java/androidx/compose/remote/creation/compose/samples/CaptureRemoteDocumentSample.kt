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

package androidx.compose.remote.creation.compose.samples

import android.content.Context
import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

@Sampled
suspend fun CaptureSingleRemoteDocumentSample(context: Context): ByteArray {
    val displayInfo = createCreationDisplayInfo(context, Size(width = 400f, height = 200f))
    val capturedDocument =
        captureSingleRemoteDocument(context = context, creationDisplayInfo = displayInfo) {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color(0xFF1E88E5).rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "Hello Remote Compose".rs, color = Color.White.rc)
            }
        }
    return capturedDocument.bytes
}

@Sampled
suspend fun CaptureRemoteDocumentSample(
    context: Context,
    titleFlow: Flow<String>,
    onDocumentUpdated: (ByteArray) -> Unit,
) {
    val displayInfo = createCreationDisplayInfo(context, Size(width = 400f, height = 200f))
    captureRemoteDocument(context = context, creationDisplayInfo = displayInfo) {
            val title by titleFlow.collectAsState(initial = "Loading...")
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize().background(Color(0xFF1E88E5).rc).padding(16.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = title.rs, color = Color.White.rc)
            }
        }
        .collect { documentBytes -> onDocumentUpdated(documentBytes) }
}
