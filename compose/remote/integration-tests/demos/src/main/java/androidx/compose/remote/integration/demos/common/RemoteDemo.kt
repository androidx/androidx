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
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
@Suppress("RestrictedApiAndroidX")
fun RemoteDemo(
    modifier: Modifier = Modifier,
    init: (RemoteComposePlayer) -> Unit = {},
    update: (RemoteComposePlayer) -> Unit = {},
    content: @Composable @RemoteComposable () -> Unit,
) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        val creationDisplayInfo = createCreationDisplayInfo()
        // TODO(b/495316956): pass LayoutDirection to captureSingleRemoteDocumentV2
        LaunchedEffect(Unit) {
            val captured =
                captureSingleRemoteDocumentV2(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                    content = content,
                )
            documentState = RemoteDocument(captured.bytes)
        }

        if (documentState != null) {
            val windowInfo = LocalWindowInfo.current
            RemoteDocumentPlayer(
                document = documentState!!.document,
                windowInfo.containerSize.width,
                windowInfo.containerSize.height,
                modifier = modifier.fillMaxSize(),
                debugMode = 0,
                init = init,
                update = update,
                onNamedAction = { _, _, _ -> },
            )
        }
    }
}
