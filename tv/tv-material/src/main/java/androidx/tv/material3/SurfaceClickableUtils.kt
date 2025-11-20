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
import androidx.compose.ui.semantics.semantics

internal fun Modifier.tvClickable(
    enabled: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource,
) =
    handleDPadEnter(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        // We are not using "clickable" modifier here because if we set "enabled" to false
        // then the Surface won't be focusable as well. But, in TV use case, a disabled surface
        // should be focusable
        .focusable(interactionSource = interactionSource)
        .semantics(mergeDescendants = true) {
            onClick {
                onClick?.let { nnOnClick ->
                    nnOnClick()
                    return@onClick true
                }
                false
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

internal fun ClickableSurfaceShape.currentShape(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Shape {
    return when {
        pressed && enabled -> pressedShape
        focused && enabled -> focusedShape
        focused && !enabled -> focusedDisabledShape
        enabled -> shape
        else -> disabledShape
    }
}

internal fun ClickableSurfaceColors.currentContainerColor(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Color {
    return when {
        pressed && enabled -> pressedContainerColor
        focused && enabled -> focusedContainerColor
        enabled -> containerColor
        else -> disabledContainerColor
    }
}

internal fun ClickableSurfaceColors.currentContentColor(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Color {
    return when {
        pressed && enabled -> pressedContentColor
        focused && enabled -> focusedContentColor
        enabled -> contentColor
        else -> disabledContentColor
    }
}

internal fun ClickableSurfaceScale.currentScale(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Float {
    return when {
        pressed && enabled -> pressedScale
        focused && enabled -> focusedScale
        focused && !enabled -> focusedDisabledScale
        enabled -> scale
        else -> disabledScale
    }
}

internal fun ClickableSurfaceBorder.currentBorder(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Border {
    return when {
        pressed && enabled -> pressedBorder
        focused && enabled -> focusedBorder
        focused && !enabled -> focusedDisabledBorder
        enabled -> border
        else -> disabledBorder
    }
}

internal fun ClickableSurfaceGlow.currentGlow(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
): Glow {
    return if (enabled) {
        when {
            pressed -> pressedGlow
            focused -> focusedGlow
            else -> glow
        }
    } else {
        Glow.None
    }
}
