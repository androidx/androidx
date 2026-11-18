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

import android.view.ViewGroup
import androidx.activity.compose.LocalFullyDrawnReporterOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcImageLoader
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.player.compose.embedded.rememberRcPlayerState
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnPreDraw

enum class PlayerVariant {
    Embedded,
    View,
    DocumentPlayer,
}

fun playerVariantFromMode(playerMode: String): Pair<PlayerVariant, Boolean> =
    when (playerMode) {
        "embedded",
        MODE_EMBEDDED_PLAYER -> PlayerVariant.Embedded to true
        "view",
        MODE_JAVA_PLAYER,
        MODE_VIEW_PLAYER -> PlayerVariant.View to false
        "documentPlayer_embedded",
        MODE_DOCUMENT_PLAYER_EMBEDDED -> PlayerVariant.DocumentPlayer to true
        "documentPlayer_view",
        MODE_DOCUMENT_PLAYER_VIEW -> PlayerVariant.DocumentPlayer to false
        else -> throw IllegalArgumentException("Unknown playerMode: $playerMode")
    }

/**
 * Shared host composable for rendering a Remote Compose document using either the Compose-native
 * embedded player ([RcPlayer]), the View-based player ([RemoteComposePlayer]) directly, or
 * [RemoteDocumentPlayer].
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
    onEmbeddedStateReady: ((RcPlayerState) -> Unit)? = null,
) {
    val (variant, embeddedFlag) = playerVariantFromMode(playerMode)
    RemotePlayerHost(
        variant = variant,
        embeddedFlag = embeddedFlag,
        remoteDocumentBytes = remoteDocumentBytes,
        modifier = modifier,
        imageLoader = imageLoader,
        bitmapLoader = bitmapLoader,
        onJavaPlayerReady = onJavaPlayerReady,
        onEmbeddedDocReady = onEmbeddedDocReady,
        onEmbeddedStateReady = onEmbeddedStateReady,
    )
}

@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
fun RemotePlayerHost(
    variant: PlayerVariant,
    embeddedFlag: Boolean,
    remoteDocumentBytes: ByteArray,
    modifier: Modifier = Modifier,
    imageLoader: RcImageLoader? = null,
    bitmapLoader: BitmapLoader? = null,
    onJavaPlayerReady: ((RemoteComposePlayer) -> Unit)? = null,
    onEmbeddedDocReady: ((CoreDocument) -> Unit)? = null,
    onEmbeddedStateReady: ((RcPlayerState) -> Unit)? = null,
) {
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = embeddedFlag

    val remoteDocument =
        remember(remoteDocumentBytes) {
            trace("CreateRemoteDocument:parsing") { RemoteDocument(remoteDocumentBytes) }
        }

    val playerModifier = Modifier.fillMaxSize().then(modifier)

    when (variant) {
        PlayerVariant.Embedded -> {
            val state = rememberRcPlayerState(remoteDocument.document)
            DisposableEffect(state) {
                onEmbeddedDocReady?.invoke(state.document)
                onEmbeddedStateReady?.invoke(state)
                onDispose {}
            }
            RcPlayer(
                state = state,
                modifier = playerModifier,
                imageLoader = imageLoader,
            )
        }
        PlayerVariant.View -> {
            val playbackTheme = if (isSystemInDarkTheme()) Theme.DARK else Theme.LIGHT
            val fullyDrawnReporter = LocalFullyDrawnReporterOwner.current?.fullyDrawnReporter
            DisposableEffect(fullyDrawnReporter) {
                fullyDrawnReporter?.addReporter()
                onDispose { fullyDrawnReporter?.removeReporter() }
            }
            AndroidView(
                modifier = playerModifier,
                factory = { context ->
                    RemoteComposePlayer(context).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        doOnPreDraw { fullyDrawnReporter?.removeReporter() }
                        bitmapLoader?.let(::setBitmapLoader)
                    }
                },
                update = { player ->
                    player.setTheme(playbackTheme)
                    player.setDocument(remoteDocument)
                    player.setDebug(0)
                    onJavaPlayerReady?.invoke(player)
                },
            )
        }
        PlayerVariant.DocumentPlayer -> {
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
}
