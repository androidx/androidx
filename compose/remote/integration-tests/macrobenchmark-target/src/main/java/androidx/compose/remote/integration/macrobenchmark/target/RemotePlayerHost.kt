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

package androidx.compose.remote.integration.macrobenchmark.target

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcImageLoader
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.util.trace

/**
 * Shared host composable for rendering a Remote Compose document using either the Compose-native
 * embedded player ([RcPlayer]) or the View-based Java player ([RemoteDocumentPlayer]).
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
fun RemotePlayerHost(
    playerMode: String,
    remoteDocumentBytes: ByteArray,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    bitmapLoader: BitmapLoader? = null,
    onJavaPlayerReady: ((RemoteComposePlayer) -> Unit)? = null,
    onEmbeddedDocReady: ((CoreDocument) -> Unit)? = null,
) {
    val remoteDocument =
        remember(remoteDocumentBytes) {
            trace("CreateRemoteDocument:parsing") { RemoteDocument(remoteDocumentBytes) }
        }

    val playerModifier = Modifier.fillMaxSize().then(modifier)

    if (playerMode == MODE_EMBEDDED_PLAYER) {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
        val coreDoc = remoteDocument.document
        DisposableEffect(coreDoc) {
            onEmbeddedDocReady?.invoke(coreDoc)
            onDispose {}
        }
        RcPlayer(
            document = coreDoc,
            modifier = playerModifier,
            imageLoader = imageLoader,
        )
    } else {
        val windowInfo = LocalWindowInfo.current
        RemoteDocumentPlayer(
            document = remoteDocument.document,
            documentWidth = windowInfo.containerSize.width,
            documentHeight = windowInfo.containerSize.height,
            modifier = playerModifier,
            debugMode = 0,
            bitmapLoader = bitmapLoader,
            init = { player -> onJavaPlayerReady?.invoke(player) },
            update = { player -> onJavaPlayerReady?.invoke(player) },
        )
    }
}
