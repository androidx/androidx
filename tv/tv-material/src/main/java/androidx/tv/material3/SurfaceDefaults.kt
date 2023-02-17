/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Contains the default values used by clickable Surface.
 */
@ExperimentalTvMaterial3Api
object ClickableSurfaceDefaults {
    internal fun shape(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        shape: ClickableSurfaceShape
    ): Shape {
        return when {
            pressed && enabled -> shape.pressedShape
            focused && enabled -> shape.focusedShape
            focused && !enabled -> shape.focusedDisabledShape
            enabled -> shape.shape
            else -> shape.disabledShape
        }
    }

    /**
     * Creates a [ClickableSurfaceShape] that represents the default container shapes used in a
     * Surface.
     *
     * @param shape the shape used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedShape the shape used when the Surface is enabled and focused.
     * @param pressedShape the shape used when the Surface is enabled pressed.
     * @param disabledShape the shape used when the Surface is not enabled.
     * @param focusedDisabledShape the shape used when the Surface is not enabled and focused.
     */
    @ReadOnlyComposable
    @Composable
    fun shape(
        shape: Shape = MaterialTheme.shapes.medium,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape
    ) = ClickableSurfaceShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = disabledShape,
        focusedDisabledShape = focusedDisabledShape
    )

    internal fun color(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        color: ClickableSurfaceColor
    ): Color {
        return when {
            pressed && enabled -> color.pressedColor
            focused && enabled -> color.focusedColor
            enabled -> color.color
            else -> color.disabledColor
        }
    }

    /**
     * Creates a [ClickableSurfaceColor] that represents the default container colors used in a
     * Surface.
     *
     * @param color the container color of this Surface when enabled
     * @param focusedColor the container color of this Surface when enabled and focused
     * @param pressedColor the container color of this Surface when enabled and pressed
     * @param disabledColor the container color of this Surface when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun color(
        color: Color = MaterialTheme.colorScheme.surface,
        focusedColor: Color = MaterialTheme.colorScheme.inverseSurface,
        pressedColor: Color = MaterialTheme.colorScheme.inverseSurface,
        disabledColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundAlpha
        )
    ) = ClickableSurfaceColor(
        color = color,
        focusedColor = focusedColor,
        pressedColor = pressedColor,
        disabledColor = disabledColor
    )

    /**
     * Creates a [ClickableSurfaceColor] that represents the default content colors used in a
     * Surface.
     *
     * @param color the content color of this Surface when enabled
     * @param focusedColor the content color of this Surface when enabled and focused
     * @param pressedColor the content color of this Surface when enabled and pressed
     * @param disabledColor the content color of this Surface when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun contentColor(
        color: Color = MaterialTheme.colorScheme.onSurface,
        focusedColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        disabledColor: Color = MaterialTheme.colorScheme.onSurface
    ) = ClickableSurfaceColor(
        color = color,
        focusedColor = focusedColor,
        pressedColor = pressedColor,
        disabledColor = disabledColor
    )

    internal fun glow(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        glow: ClickableSurfaceGlow
    ): Glow {
        return if (enabled) {
            when {
                pressed -> glow.pressedGlow
                focused -> glow.focusedGlow
                else -> glow.glow
            }
        } else {
            Glow.None
        }
    }

    /**
     * Creates a [ClickableSurfaceGlow] that represents the default [Glow]s used in a
     * Surface.
     *
     * @param glow the Glow behind this Surface when enabled
     * @param focusedGlow the Glow behind this Surface when focused
     * @param pressedGlow the Glow behind this Surface when pressed
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = ClickableSurfaceGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}

private const val DisabledBackgroundAlpha = 0.4f
