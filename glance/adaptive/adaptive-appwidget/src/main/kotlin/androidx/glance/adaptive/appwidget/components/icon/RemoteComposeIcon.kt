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

package androidx.glance.adaptive.appwidget.components.icon

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.RemoteVectorGlyph
import androidx.glance.adaptive.core.slots.Icon

/**
 * Draws the icon for the leading slot, [size] across — the RemoteCompose counterpart of
 * [Icon.buildRemoteViews].
 *
 * When [Icon.contained] is `true`, the glyph is drawn in a filled circle [size] across, tinted with
 * [RemoteComposePalette.onSecondaryContainer], as the design draws the dashboard header's brand
 * icon. When `false`, the icon is the glyph alone, tinted with [RemoteComposePalette.primary].
 *
 * @param size Edge length the matrix gives the icon.
 */
@RemoteComposable
@Composable
internal fun Icon.Content(size: Dp) {
    val description = contentDescription
    val semanticsModifier =
        if (description != null) {
            RemoteModifier.semantics {
                contentDescription = description.rs
                role = RemoteRole.Image
            }
        } else {
            RemoteModifier
        }

    if (!contained) {
        RemoteVectorGlyph(
            icon = icon,
            size = size,
            tint = RemoteComposePalette.primary,
            modifier = semanticsModifier,
        )
        return
    }

    RemoteBox(
        modifier =
            semanticsModifier
                .size(size.asRdp())
                .clip(RemoteCircleShape)
                .background(RemoteComposePalette.surfaceContainerHighest),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteVectorGlyph(
            icon = icon,
            size = size * IconDefaults.ContainedGlyphRatio,
            tint = RemoteComposePalette.onSecondaryContainer,
        )
    }
}
