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

package androidx.xr.glimmer.demos.remote.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.xr.glimmer.remote.RemoteGlimmerTheme
import androidx.xr.glimmer.remote.RemoteIcon
import androidx.xr.glimmer.remote.surface

@Composable
internal fun RemoteIconDemo() {
    val documentBytes: ByteArray? by rememberRemoteDocumentState {
        RemoteGlimmerTheme { IconUsage() }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedRemoteDocumentPlayer(documentBytes = documentBytes, modifier = Modifier.weight(1f))
        SaveRemoteDocumentButton(fileName = "icons.rc", documentBytes = documentBytes)
    }
}

@Composable
@RemoteComposable
private fun IconUsage() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxWidth(),
        verticalArrangement =
            RemoteArrangement.spacedBy(RemoteGlimmerTheme.componentSpacingValues.extraLarge),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        IconSizes()
        // TODO: b/568385462 - Use the primary color for the surface once colors are customizable.
        RemoteIcon(
            FavoriteIcon,
            "Localized description".rs,
            RemoteModifier.surface(shape = RemoteCircleShape).padding(12.rdp),
        )
    }
}

@Composable
@RemoteComposable
private fun IconSizes() {
    val iconSizes = RemoteGlimmerTheme.iconSizes
    RemoteColumn(
        verticalArrangement = RemoteArrangement.spacedBy(20.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        RemoteIcon(FavoriteIcon, "Localized description".rs, RemoteModifier.size(iconSizes.small))
        // medium is also the default size, defining explicitly for clarity
        RemoteIcon(FavoriteIcon, "Localized description".rs, RemoteModifier.size(iconSizes.medium))
        RemoteIcon(FavoriteIcon, "Localized description".rs, RemoteModifier.size(iconSizes.large))
    }
}

/** Icon taken from material-icons-core */
private val FavoriteIcon: RemoteImageVector =
    RemoteImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.rdp,
            defaultHeight = 24.rdp,
            viewportWidth = 24f.rf,
            viewportHeight = 24f.rf,
            tintColor = Color.Unspecified.rc,
        )
        .apply {
            path(fill = RemoteBrush.solidColor(Color.White.rc)) {
                moveTo(12.0f.rf, 21.35f.rf)
                lineToRelative((-1.45f).rf, (-1.32f).rf)
                curveTo(5.4f.rf, 15.36f.rf, 2.0f.rf, 12.28f.rf, 2.0f.rf, 8.5f.rf)
                curveTo(2.0f.rf, 5.42f.rf, 4.42f.rf, 3.0f.rf, 7.5f.rf, 3.0f.rf)
                curveToRelative(1.74f.rf, 0.0f.rf, 3.41f.rf, 0.81f.rf, 4.5f.rf, 2.09f.rf)
                curveTo(13.09f.rf, 3.81f.rf, 14.76f.rf, 3.0f.rf, 16.5f.rf, 3.0f.rf)
                curveTo(19.58f.rf, 3.0f.rf, 22.0f.rf, 5.42f.rf, 22.0f.rf, 8.5f.rf)
                curveToRelative(0.0f.rf, 3.78f.rf, (-3.4f).rf, 6.86f.rf, (-8.55f).rf, 11.54f.rf)
                lineTo(12.0f.rf, 21.35f.rf)
                close()
            }
        }
        .build()
