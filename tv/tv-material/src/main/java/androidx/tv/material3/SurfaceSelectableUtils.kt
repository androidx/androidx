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

package androidx.tv.material3

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

internal fun Modifier.tvSelectable(
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource,
) =
    handleDPadEnter(
            enabled = enabled,
            interactionSource = interactionSource,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        // We are not using "selectable" modifier here because if we set "enabled" to false
        // then the Surface won't be focusable as well. But, in TV use case, a disabled surface
        // should be focusable
        .focusable(interactionSource = interactionSource)
        .semantics(mergeDescendants = true) {
            this.selected = selected
            onClick {
                onClick()
                true
            }
            onLongClick {
                onLongClick?.let { nnOnLongClick ->
                    nnOnLongClick()
                    return@onLongClick true
                }
                false
            }
            if (!enabled) {
                disabled()
            }
        }

internal fun SelectableSurfaceShape.currentShape(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Shape {
    return when {
        enabled && selected && pressed -> pressedSelectedShape
        enabled && selected && focused -> focusedSelectedShape
        enabled && selected -> selectedShape
        enabled && pressed -> pressedShape
        enabled && focused -> focusedShape
        enabled -> shape
        !enabled && selected && focused -> focusedSelectedDisabledShape
        !enabled && selected -> selectedDisabledShape
        !enabled && focused -> focusedDisabledShape
        else -> disabledShape
    }
}

internal fun SelectableSurfaceColors.currentContainerColor(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Color {
    return when {
        enabled && selected && pressed -> pressedSelectedContainerColor
        enabled && selected && focused -> focusedSelectedContainerColor
        enabled && selected -> selectedContainerColor
        enabled && pressed -> pressedContainerColor
        enabled && focused -> focusedContainerColor
        enabled -> containerColor
        else -> disabledContainerColor
    }
}

internal fun SelectableSurfaceColors.currentContentColor(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Color {
    return when {
        enabled && selected && pressed -> pressedSelectedContentColor
        enabled && selected && focused -> focusedSelectedContentColor
        enabled && selected -> selectedContentColor
        enabled && pressed -> pressedContentColor
        enabled && focused -> focusedContentColor
        enabled -> contentColor
        else -> disabledContentColor
    }
}

internal fun SelectableSurfaceScale.currentScale(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Float {
    return when {
        enabled && selected && pressed -> pressedSelectedScale
        enabled && selected && focused -> focusedSelectedScale
        enabled && selected -> selectedScale
        enabled && pressed -> pressedScale
        enabled && focused -> focusedScale
        enabled -> scale
        !enabled && selected && focused -> focusedSelectedDisabledScale
        !enabled && selected -> selectedDisabledScale
        !enabled && focused -> focusedDisabledScale
        else -> disabledScale
    }
}

internal fun SelectableSurfaceBorder.currentBorder(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Border {
    return when {
        enabled && selected && pressed -> pressedSelectedBorder
        enabled && selected && focused -> focusedSelectedBorder
        enabled && selected -> selectedBorder
        enabled && pressed -> pressedBorder
        enabled && focused -> focusedBorder
        enabled -> border
        !enabled && selected && focused -> focusedSelectedDisabledBorder
        !enabled && selected -> selectedDisabledBorder
        !enabled && focused -> focusedDisabledBorder
        else -> disabledBorder
    }
}

internal fun SelectableSurfaceGlow.currentGlow(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Glow {
    return when {
        enabled && selected && pressed -> pressedSelectedGlow
        enabled && selected && focused -> focusedSelectedGlow
        enabled && selected -> selectedGlow
        enabled && pressed -> pressedGlow
        enabled && focused -> focusedGlow
        enabled -> glow
        else -> Glow.None
    }
}
