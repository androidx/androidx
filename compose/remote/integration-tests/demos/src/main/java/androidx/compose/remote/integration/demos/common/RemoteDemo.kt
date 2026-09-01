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

package androidx.compose.remote.integration.demos.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.settings.LocalPlayerType
import androidx.compose.remote.integration.demos.settings.PLAYER_TYPE_COMPOSE
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.platform.AndroidCustomContext
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
@Suppress("RestrictedApiAndroidX")
fun RemoteDemo(
    modifier: Modifier = Modifier,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    init: (RemoteComposePlayer) -> Unit = {},
    update: (RemoteComposePlayer) -> Unit = {},
    onNamedAction: (String, Any?, StateUpdater) -> Unit = { _, _, _ -> },
    bitmapLoader: BitmapLoader? = null,
    customSupport: AndroidCustomContext? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    var capturedBytes by remember { mutableStateOf<ByteArray?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        val creationDisplayInfo = createCreationDisplayInfo()
        LaunchedEffect(profile, content) {
            val captured =
                captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                    profile = profile,
                    content = content,
                )
            capturedBytes = captured.bytes
        }

        val playerType = LocalPlayerType.current

        val currentBytes = capturedBytes
        if (currentBytes != null) {
            key(playerType, currentBytes) {
                val remoteDoc = remember(playerType, currentBytes) { RemoteDocument(currentBytes) }
                val doc = remoteDoc.document
                if (playerType == PLAYER_TYPE_COMPOSE) {
                    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
                    RcPlayer(
                        document = doc,
                        modifier = modifier.fillMaxSize(),
                        onNamedAction = onNamedAction,
                    )
                } else {
                    val windowInfo = LocalWindowInfo.current
                    RemoteDocumentPlayer(
                        document = doc,
                        documentWidth = windowInfo.containerSize.width,
                        documentHeight = windowInfo.containerSize.height,
                        modifier = modifier.fillMaxSize(),
                        debugMode = 0,
                        init = init,
                        update = update,
                        onNamedAction = onNamedAction,
                        bitmapLoader = bitmapLoader,
                        customSupport = customSupport,
                    )
                }
            }
        }
    }
}
