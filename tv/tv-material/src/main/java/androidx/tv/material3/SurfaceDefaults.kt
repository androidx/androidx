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
            enabled -> shape.defaultShape
            else -> shape.disabledShape
        }
    }

    /**
     * Creates a [ClickableSurfaceShape] that represents the default container shapes used in a
     * Surface.
     *
     * @param defaultShape the shape used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedShape the shape used when the Surface is enabled and focused.
     * @param pressedShape the shape used when the Surface is enabled pressed.
     * @param disabledShape the shape used when the Surface is not enabled.
     * @param focusedDisabledShape the shape used when the Surface is not enabled and focused.
     */
    @ReadOnlyComposable
    @Composable
    fun shape(
        defaultShape: Shape = MaterialTheme.shapes.medium,
        focusedShape: Shape = defaultShape,
        pressedShape: Shape = defaultShape,
        disabledShape: Shape = defaultShape,
        focusedDisabledShape: Shape = disabledShape
    ) = ClickableSurfaceShape(
        defaultShape = defaultShape,
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
            enabled -> color.defaultColor
            else -> color.disabledColor
        }
    }

    /**
     * Creates a [ClickableSurfaceColor] that represents the default container colors used in a
     * Surface.
     *
     * @param defaultColor the container color of this Surface when enabled
     * @param focusedColor the container color of this Surface when enabled and focused
     * @param pressedColor the container color of this Surface when enabled and pressed
     * @param disabledColor the container color of this Surface when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun color(
        defaultColor: Color = MaterialTheme.colorScheme.surface,
        focusedColor: Color = MaterialTheme.colorScheme.inverseSurface,
        pressedColor: Color = MaterialTheme.colorScheme.inverseSurface,
        disabledColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundAlpha
        )
    ) = ClickableSurfaceColor(
        defaultColor = defaultColor,
        focusedColor = focusedColor,
        pressedColor = pressedColor,
        disabledColor = disabledColor
    )

    /**
     * Creates a [ClickableSurfaceColor] that represents the default content colors used in a
     * Surface.
     *
     * @param defaultColor the content color of this Surface when enabled
     * @param focusedColor the content color of this Surface when enabled and focused
     * @param pressedColor the content color of this Surface when enabled and pressed
     * @param disabledColor the content color of this Surface when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun contentColor(
        defaultColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        disabledColor: Color = MaterialTheme.colorScheme.onSurface
    ) = ClickableSurfaceColor(
        defaultColor = defaultColor,
        focusedColor = focusedColor,
        pressedColor = pressedColor,
        disabledColor = disabledColor
    )
}

private const val DisabledBackgroundAlpha = 0.4f
