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

package androidx.glance.wear.composable

import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.color
import androidx.glance.wear.isEmpty

// Forked from androidx.wear.compose.material3.ColorScheme.surfaceContainerLow
private val surfaceContainerLowColor: RemoteColor = Color(red = 39, green = 36, blue = 48).rc

/**
 * A container for a remote compose widget, applying standard styling.
 *
 * This container applies horizontal and vertical padding, as well as corners, to its content. It is
 * strongly recommended to explicitly define a non-transparent background. If the given [background]
 * is empty, a default surface color will be applied.
 */
@RemoteComposable
@Composable
internal fun WearWidgetContainer(
    horizontalPadding: RemoteDp,
    verticalPadding: RemoteDp,
    cornerRadius: RemoteDp,
    background: WearWidgetBrush,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val backgroundWithDefault =
        if (background.isEmpty()) {
            WearWidgetBrush.color(surfaceContainerLowColor)
        } else {
            background
        }
    RemoteBox(
        modifier =
            RemoteModifier.fillMaxSize().drawWithContent {
                val cornerRadiusOffset = RemoteOffset(cornerRadius.toPx(), cornerRadius.toPx())
                val paint = RemotePaint()
                backgroundWithDefault.foldIn(Unit) { _, element ->
                    with(element.brush) { applyTo(paint, size) }
                    drawRoundRect(paint = paint, cornerRadius = cornerRadiusOffset)
                }
                drawContent()
            }
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = content,
        )
    }
}
