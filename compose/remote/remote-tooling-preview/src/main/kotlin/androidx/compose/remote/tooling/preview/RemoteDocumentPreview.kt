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
package androidx.compose.remote.tooling.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Displays a [RemoteDocument] in the Android Studio Preview.
 *
 * This composable is intended for use within Android Studio to preview the content of a
 * [RemoteDocument]. It handles setting up the necessary Remote Compose player environment.
 *
 * @param remoteDocument The [RemoteDocument] containing the content to be displayed.
 * @param modifier The modifier to be applied to the box containing the preview.
 */
@Composable
public fun RemoteDocumentPreview(remoteDocument: RemoteDocument, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val windowInfo = LocalWindowInfo.current
        RemoteDocumentPlayer(
            document = remoteDocument.document,
            documentWidth = windowInfo.containerSize.width,
            documentHeight = windowInfo.containerSize.height,
            modifier = Modifier.fillMaxSize(),
            debugMode = 0,
            onNamedAction = { _, _, _ -> },
        )
    }
}

/**
 * Displays a [RemoteDocument] in the Android Studio Preview from a [ByteArray].
 *
 * This is a convenience overload that takes the raw document bytes directly.
 *
 * @param document The raw byte array representing the [RemoteDocument].
 * @param modifier The modifier to be applied to the box containing the preview.
 */
@Composable
public fun RemoteDocumentPreview(document: ByteArray, modifier: Modifier = Modifier): Unit =
    RemoteDocumentPreview(remoteDocument = RemoteDocument(document), modifier = modifier)
