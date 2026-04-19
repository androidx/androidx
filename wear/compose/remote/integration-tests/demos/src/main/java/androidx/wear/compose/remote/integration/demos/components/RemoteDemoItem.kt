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
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.remote.material3.RemoteMaterialTheme

internal val LocalUseDynamicColor = compositionLocalOf { true }

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
    useDynamicColor: Boolean = LocalUseDynamicColor.current,
    content: @Composable @RemoteComposable () -> Unit,
) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val dynamicColors =
        if (useDynamicColor) {
            remember(configuration) { dynamicColorScheme(context) }
        } else {
            null
        }
    LaunchedEffect(Unit) {
        val captured =
            captureSingleRemoteDocument(
                context = context,
                profile = profileFeaturePaintMeasureDisabled,
            ) {
                RemoteMaterialTheme {
                    RemoteBox(
                        modifier = RemoteModifier.fillMaxWidth().padding(8.rdp),
                        contentAlignment = RemoteAlignment.Center,
                        content = content,
                    )
                }
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
            update = { player -> setDynamicColors(dynamicColors, player) },
            onNamedAction = { _, _, _ -> },
        )
    }
}

@Suppress("RestrictedApiAndroidX")
private fun setDynamicColors(dynamicColors: ColorScheme?, player: RemoteComposePlayer) {
    dynamicColors?.let { colors ->
        player.setUserLocalColor("WearM3.primary", colors.primary.toArgb())
        player.setUserLocalColor("WearM3.primaryDim", colors.primaryDim.toArgb())
        player.setUserLocalColor("WearM3.primaryContainer", colors.primaryContainer.toArgb())
        player.setUserLocalColor("WearM3.onPrimary", colors.onPrimary.toArgb())
        player.setUserLocalColor("WearM3.onPrimaryContainer", colors.onPrimaryContainer.toArgb())
        player.setUserLocalColor("WearM3.secondary", colors.secondary.toArgb())
        player.setUserLocalColor("WearM3.secondaryDim", colors.secondaryDim.toArgb())
        player.setUserLocalColor("WearM3.secondaryContainer", colors.secondaryContainer.toArgb())
        player.setUserLocalColor("WearM3.onSecondary", colors.onSecondary.toArgb())
        player.setUserLocalColor(
            "WearM3.onSecondaryContainer",
            colors.onSecondaryContainer.toArgb(),
        )
        player.setUserLocalColor("WearM3.tertiary", colors.tertiary.toArgb())
        player.setUserLocalColor("WearM3.tertiaryDim", colors.tertiaryDim.toArgb())
        player.setUserLocalColor("WearM3.tertiaryContainer", colors.tertiaryContainer.toArgb())
        player.setUserLocalColor("WearM3.onTertiary", colors.onTertiary.toArgb())
        player.setUserLocalColor("WearM3.onTertiaryContainer", colors.onTertiaryContainer.toArgb())
        player.setUserLocalColor("WearM3.surfaceContainerLow", colors.surfaceContainerLow.toArgb())
        player.setUserLocalColor("WearM3.surfaceContainer", colors.surfaceContainer.toArgb())
        player.setUserLocalColor(
            "WearM3.surfaceContainerHigh",
            colors.surfaceContainerHigh.toArgb(),
        )
        player.setUserLocalColor("WearM3.onSurface", colors.onSurface.toArgb())
        player.setUserLocalColor("WearM3.onSurfaceVariant", colors.onSurfaceVariant.toArgb())
        player.setUserLocalColor("WearM3.outline", colors.outline.toArgb())
        player.setUserLocalColor("WearM3.outlineVariant", colors.outlineVariant.toArgb())
        player.setUserLocalColor("WearM3.background", colors.background.toArgb())
        player.setUserLocalColor("WearM3.onBackground", colors.onBackground.toArgb())
        player.setUserLocalColor("WearM3.error", colors.error.toArgb())
        player.setUserLocalColor("WearM3.errorDim", colors.errorDim.toArgb())
        player.setUserLocalColor("WearM3.errorContainer", colors.errorContainer.toArgb())
        player.setUserLocalColor("WearM3.onError", colors.onError.toArgb())
        player.setUserLocalColor("WearM3.onErrorContainer", colors.onErrorContainer.toArgb())
    }
}

@Suppress("RestrictedApiAndroidX")
fun TransformingLazyColumnScope.remoteDemoItem(
    label: String,
    playerModifier: Modifier = Modifier,
    documentWidth: Int? = null,
    documentHeight: Int? = null,
    useDynamicColor: Boolean = true,
    content: @Composable @RemoteComposable () -> Unit,
) {
    item { ListSubHeader { Text(label) } }
    item {
        RemoteDemoItem(
            modifier = playerModifier,
            documentWidth = documentWidth,
            documentHeight = documentHeight,
            useDynamicColor = useDynamicColor,
            content = content,
        )
    }
}
