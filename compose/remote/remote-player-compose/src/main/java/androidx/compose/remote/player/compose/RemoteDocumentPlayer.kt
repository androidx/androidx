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
package androidx.compose.remote.player.compose

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.player.view.RemoteComposeDocument
import androidx.compose.remote.player.view.state.StateUpdater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A player of a [CoreDocument] */
@Composable
public fun RemoteDocumentPlayer(
    document: CoreDocument,
    documentWidth: Int,
    documentHeight: Int,
    modifier: Modifier = Modifier,
    debugMode: Int = 0,
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
) {
    var inDarkTheme by remember { mutableStateOf(false) }
    var playbackTheme by remember { mutableIntStateOf(Theme.UNSPECIFIED) }

    val remoteDoc = remember(document) { RemoteComposeDocument(document) }

    inDarkTheme =
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> isSystemInDarkTheme()
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> isSystemInDarkTheme()
            else -> {
                false
            }
        }

    playbackTheme =
        if (inDarkTheme) {
            Theme.DARK
        } else {
            Theme.LIGHT
        }

    RemoteComposePlayer(
        document = remoteDoc,
        modifier = modifier.size(documentWidth.dp, documentHeight.dp),
        theme = playbackTheme,
        debugMode = debugMode,
        onNamedAction = onNamedAction,
    )
}
