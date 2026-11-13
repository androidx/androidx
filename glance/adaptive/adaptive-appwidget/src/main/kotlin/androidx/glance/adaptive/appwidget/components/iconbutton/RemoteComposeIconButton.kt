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

package androidx.glance.adaptive.appwidget.components.iconbutton

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
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.components.RemoteComposeActions
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.RemoteVectorGlyph
import androidx.glance.adaptive.core.slots.IconButton

/**
 * Draws the icon button as a circle [diameter] across, or as a pill [width] wide and [diameter]
 * tall when [width] is larger, with its glyph [glyphSize] across at the center — the RemoteCompose
 * counterpart of [IconButton.buildRemoteViews].
 *
 * @param taps Where the button's tap goes.
 * @param diameter Height of the button, and its width when it is a circle.
 * @param glyphSize Edge length of the glyph.
 * @param width Width of the button; a pill when larger than [diameter].
 */
@RemoteComposable
@Composable
internal fun IconButton.Content(
    taps: RemoteComposeActions,
    diameter: Dp,
    glyphSize: Dp,
    width: Dp = diameter,
) {
    val shape =
        if (width > diameter) {
            RemoteRoundedCornerShape((diameter.value / 2f).rdp)
        } else {
            RemoteCircleShape
        }
    val tappable = taps.isTappable(action)
    val description = contentDescription

    RemoteBox(
        modifier =
            RemoteModifier.size(width = width.asRdp(), height = diameter.asRdp())
                .clip(shape)
                .background(RemoteComposePalette.secondaryContainer)
                .semantics {
                    contentDescription = description.rs
                    if (tappable) {
                        role = RemoteRole.Button
                    }
                }
                .run { with(taps) { widgetClickable(action) } },
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteVectorGlyph(
            icon = icon,
            size = glyphSize,
            tint = RemoteComposePalette.onSecondaryContainer,
        )
    }
}
