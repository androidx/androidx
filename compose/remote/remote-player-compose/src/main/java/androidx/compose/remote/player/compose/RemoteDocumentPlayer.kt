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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.player.compose

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags.isViewPlayerEnabled
import androidx.compose.remote.player.compose.impl.RemoteDocumentComposePlayer
import androidx.compose.remote.player.compose.impl.RemoteDocumentViewPlayer
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A player of a [CoreDocument] */
@OptIn(ExperimentalRemoteComposePlayerApi::class)
@Composable
public fun RemoteDocumentPlayer(
    document: CoreDocument,
    documentWidth: Int,
    documentHeight: Int,
    modifier: Modifier = Modifier,
    debugMode: Int = 0,
    init: (RemoteComposePlayer) -> Unit = {},
    update: (RemoteComposePlayer) -> Unit = {},
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    bitmapLoader: BitmapLoader? = null,
) {
    if (isViewPlayerEnabled) {
        RemoteDocumentViewPlayer(
            document = document,
            documentWidth = documentWidth,
            documentHeight = documentHeight,
            modifier = modifier,
            debugMode = debugMode,
            init = init,
            update = update,
            onAction = onAction,
            onNamedAction = onNamedAction,
            bitmapLoader = bitmapLoader,
        )
    } else {
        RemoteDocumentComposePlayer(
            document = document,
            documentWidth = documentWidth,
            documentHeight = documentHeight,
            modifier = modifier,
            debugMode = debugMode,
            onNamedAction = onNamedAction,
            bitmapLoader = bitmapLoader,
        )
    }
}
