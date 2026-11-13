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
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Forked from androidx.wear.compose.material3.ColorScheme.surfaceContainerLow
private val surfaceContainerLowColor: RemoteColor = Color(red = 39, green = 36, blue = 48).rc

/**
 * A container for a remote compose widget, applying standard styling.
 *
 * This container clips both [background] and [content] to [cornerRadius] and applies
 * [horizontalPadding] and [verticalPadding] to [content]. If [background] is `null`, a default
 * surface color is applied as the container background; otherwise [background] is drawn behind
 * [content]. It is strongly recommended to explicitly define a non-transparent [background].
 */
@RemoteComposable
@Composable
internal fun WearWidgetContainer(
    horizontalPadding: RemoteDp,
    verticalPadding: RemoteDp,
    cornerRadius: RemoteDp,
    background: (@RemoteComposable @Composable () -> Unit)?,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val baseModifier = RemoteModifier.fillMaxSize().clip(RemoteRoundedCornerShape(cornerRadius))
    RemoteBox(
        modifier =
            if (background == null) {
                baseModifier.background(surfaceContainerLowColor)
            } else {
                baseModifier
            }
    ) {
        background?.invoke()
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            content = { content() },
        )
    }
}
