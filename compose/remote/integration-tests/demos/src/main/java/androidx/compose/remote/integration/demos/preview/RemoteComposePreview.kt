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

package androidx.compose.remote.integration.demos.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.RememberRemoteDocumentInline
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.compose.ExperimentalRemoteComposePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteComposeDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo

/** Display a Remote Compose Composable in the Android Studio Preview. */
@OptIn(ExperimentalRemoteComposePlayerApi::class)
@Composable
@Suppress("RestrictedApiAndroidX")
fun RemoteComposePreview(content: @RemoteComposable @Composable () -> Unit) {
    RemoteComposePlayerFlags.isViewPlayerEnabled = false
    var documentState by remember { mutableStateOf<RemoteComposeDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        RememberRemoteDocumentInline(
            onDocument = { doc ->
                println("Document generated: $doc")
                if (documentState == null) {
                    // Generate seems to get called again with a partial document
                    // Essentially re-recording but with existing state, so document is incomplete
                    documentState = RemoteComposeDocument(doc)
                }
            },
            content = content,
        )

        if (documentState != null) {
            val windowInfo = LocalWindowInfo.current
            RemoteDocumentPlayer(
                document = documentState!!.document,
                windowInfo.containerSize.width,
                windowInfo.containerSize.height,
                modifier = Modifier.fillMaxSize(),
                0,
                onNamedAction = { _, _, _ -> },
            )
        }
    }
}
