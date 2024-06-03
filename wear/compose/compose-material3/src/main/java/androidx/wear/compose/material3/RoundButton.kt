/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * This is a copy of RoundButton from materialcore, with additional onLongClick callback and usage
 * of combinedClickable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean,
    backgroundColor: @Composable (enabled: Boolean) -> Color,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    border: @Composable (enabled: Boolean) -> BorderStroke?,
    buttonSize: Dp,
    ripple: Indication,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderStroke = border(enabled)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .semantics { role = Role.Button }
                .size(buttonSize)
                .clip(shape) // Clip for the touch area (e.g. for Ripple).
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onLongClickLabel = onLongClickLabel,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple,
                )
                .then(
                    if (borderStroke != null) Modifier.border(border = borderStroke, shape = shape)
                    else Modifier
                )
                .background(color = backgroundColor(enabled), shape = shape),
        content = content
    )
}
