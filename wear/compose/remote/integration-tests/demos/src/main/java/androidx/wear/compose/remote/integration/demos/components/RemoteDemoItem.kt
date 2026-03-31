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

package androidx.wear.compose.remote.integration.demos.components

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.Text

@Suppress("RestrictedApiAndroidX")
private val profileFeaturePaintMeasureDisabled =
    Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        RcProfiles.PROFILE_ANDROIDX,
        AndroidxRcPlatformServices(),
        {
            Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX)
                ?.keySet()
                .orEmpty() + setOf(Operations.CORE_TEXT)
        },
    ) { _, profile, _ ->
        RemoteComposeWriterAndroid(
            profile,
            RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile.operationsProfiles),
        )
    }

@Composable
@Suppress("RestrictedApiAndroidX")
fun RemoteDemoItem(
    modifier: Modifier = Modifier,
    documentWidth: Int? = null,
    documentHeight: Int? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val captured =
            captureSingleRemoteDocument(
                context = context,
                profile = profileFeaturePaintMeasureDisabled,
            ) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().padding(8.rdp),
                    contentAlignment = RemoteAlignment.Center,
                    content = content,
                )
            }
        documentState = RemoteDocument(captured.bytes)
    }

    if (documentState != null) {
        val windowInfo = LocalWindowInfo.current

        @Composable fun getDefaultHeight() = with(LocalDensity.current) { 50.dp.toPx() }.toInt()

        RemoteDocumentPlayer(
            document = documentState!!.document,
            documentWidth = documentWidth ?: windowInfo.containerSize.width,
            documentHeight = documentHeight ?: getDefaultHeight(),
            modifier = modifier,
            debugMode = 0,
            onNamedAction = { _, _, _ -> },
        )
    }
}

@Suppress("RestrictedApiAndroidX")
fun TransformingLazyColumnScope.remoteDemoItem(
    label: String,
    playerModifier: Modifier = Modifier,
    documentWidth: Int? = null,
    documentHeight: Int? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    item { ListSubHeader { Text(label) } }
    item {
        RemoteDemoItem(
            modifier = playerModifier,
            documentWidth = documentWidth,
            documentHeight = documentHeight,
            content = content,
        )
    }
}
