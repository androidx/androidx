/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file -> except in compliance with the License.
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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A preview-friendly/experimental wrapper of [RcPlayer] that accepts a [RemoteDocument] instead of
 * the raw [androidx.compose.remote.core.CoreDocument].
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
public fun ExperimentalRemoteDocumentPlayer(
    document: RemoteDocument,
    modifier: Modifier = Modifier,
    autoUpdate: Boolean = true,
    namedColorOverrides: ObjectIntMap<String> = emptyObjectIntMap(),
    imageLoader: RcImageLoader? = null,
    isShaderValid: (shaderSource: String) -> Boolean = { true },
    onAction: (actionId: Int, value: String?) -> Unit = { _, _ -> },
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
) {
    RcPlayer(
        document = document.document,
        modifier = modifier,
        autoUpdate = autoUpdate,
        namedColorOverrides = namedColorOverrides,
        imageLoader = imageLoader,
        isShaderValid = isShaderValid,
        onAction = onAction,
        onNamedAction = onNamedAction,
    )
}
