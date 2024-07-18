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

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Contains the default values used by a non-interactive [Surface]
 */
@ExperimentalTvMaterial3Api
object NonInteractiveSurfaceDefaults {
    /**
     * Represents the default shape used by a non-interactive [Surface]
     */
    val shape: Shape @ReadOnlyComposable @Composable get() = MaterialTheme.shapes.medium

    /**
     * Creates a [NonInteractiveSurfaceColors] that represents the default container & content
     * colors used by a non-interactive [Surface].
     *
     * @param containerColor the container color of this Surface
     * @param contentColor the content color of this Surface
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = contentColorFor(containerColor)
    ) = NonInteractiveSurfaceColors(
        containerColor = containerColor,
        contentColor = contentColor
    )

    /**
     * Represents the default border used by a non-interactive [Surface]
     */
    internal val border: Border = Border.None

    /**
     * Represents the default glow used by a non-interactive [Surface]
     */
    internal val glow: Glow = Glow.None
}

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

    internal fun containerColor(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        colors: ClickableSurfaceColors
    ): Color {
        return when {
            pressed && enabled -> colors.pressedContainerColor
            focused && enabled -> colors.focusedContainerColor
            enabled -> colors.containerColor
            else -> colors.disabledContainerColor
        }
    }

    internal fun contentColor(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        colors: ClickableSurfaceColors
    ): Color {
        return when {
            pressed && enabled -> colors.pressedContentColor
            focused && enabled -> colors.focusedContentColor
            enabled -> colors.contentColor
            else -> colors.disabledContentColor
        }
    }

    /**
     * Creates a [ClickableSurfaceColors] that represents the default container & content colors
     * used in a Surface.
     *
     * @param containerColor the container color of this Surface when enabled
     * @param contentColor the content color of this Surface when enabled
     * @param focusedContainerColor the container color of this Surface when enabled and focused
     * @param focusedContentColor the content color of this Surface when enabled and focused
     * @param pressedContainerColor the container color of this Surface when enabled and pressed
     * @param pressedContentColor the content color of this Surface when enabled and pressed
     * @param disabledContainerColor the container color of this Surface when not enabled
     * @param disabledContentColor the content color of this Surface when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = contentColorFor(containerColor),
        focusedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor: Color = contentColorFor(focusedContainerColor),
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = contentColorFor(pressedContainerColor),
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = DisabledContainerAlpha),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface
    ) = ClickableSurfaceColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

    internal fun scale(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        scale: ClickableSurfaceScale
    ): Float {
        return when {
            pressed && enabled -> scale.pressedScale
            focused && enabled -> scale.focusedScale
            focused && !enabled -> scale.focusedDisabledScale
            enabled -> scale.scale
            else -> scale.disabledScale
        }
    }

    /**
     * Creates a [ClickableSurfaceScale] that represents the default scales used in a
     * Surface. scales are used to modify the size of a composable in different [Interaction]
     * states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc.
     *
     * @param scale the scale to be used for this Surface when enabled
     * @param focusedScale the scale to be used for this Surface when focused
     * @param pressedScale the scale to be used for this Surface when pressed
     * @param disabledScale the scale to be used for this Surface when disabled
     * @param focusedDisabledScale the scale to be used for this Surface when disabled and
     * focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) = ClickableSurfaceScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = disabledScale,
        focusedDisabledScale = focusedDisabledScale
    )

    internal fun border(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        border: ClickableSurfaceBorder
    ): Border {
        return when {
            pressed && enabled -> border.pressedBorder
            focused && enabled -> border.focusedBorder
            focused && !enabled -> border.focusedDisabledBorder
            enabled -> border.border
            else -> border.disabledBorder
        }
    }

    /**
     * Creates a [ClickableSurfaceBorder] that represents the default [Border]s applied on a
     * Surface in different [Interaction] states.
     *
     * @param border the [Border] to be used for this Surface when enabled
     * @param focusedBorder the [Border] to be used for this Surface when focused
     * @param pressedBorder the [Border] to be used for this Surface when pressed
     * @param disabledBorder the [Border] to be used for this Surface when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Surface when disabled and
     * focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = border,
        pressedBorder: Border = focusedBorder,
        disabledBorder: Border = border,
        focusedDisabledBorder: Border = Border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.border
            ),
            inset = 0.dp,
            shape = ShapeDefaults.Small
        )
    ) = ClickableSurfaceBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = disabledBorder,
        focusedDisabledBorder = focusedDisabledBorder
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

/**
 * Contains the default values used by Toggleable Surface.
 */
@ExperimentalTvMaterial3Api
object ToggleableSurfaceDefaults {
    /**
     * Creates a [ToggleableSurfaceShape] that represents the default container shapes used in a
     * toggleable Surface.
     *
     * @param shape the shape used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedShape the shape used when the Surface is enabled and focused.
     * @param pressedShape the shape used when the Surface is enabled and pressed.
     * @param selectedShape the shape used when the Surface is enabled and selected.
     * @param disabledShape the shape used when the Surface is not enabled.
     * @param focusedSelectedShape the shape used when the Surface is enabled, focused and selected.
     * @param focusedDisabledShape the shape used when the Surface is not enabled and focused.
     * @param pressedSelectedShape the shape used when the Surface is enabled, pressed and selected.
     * @param selectedDisabledShape the shape used when the Surface is not enabled and selected.
     * @param focusedSelectedDisabledShape the shape used when the Surface is not enabled, focused
     * and selected.
     */
    @ReadOnlyComposable
    @Composable
    fun shape(
        shape: Shape = MaterialTheme.shapes.medium,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        selectedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedSelectedShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape,
        pressedSelectedShape: Shape = shape,
        selectedDisabledShape: Shape = disabledShape,
        focusedSelectedDisabledShape: Shape = disabledShape
    ) = ToggleableSurfaceShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
        selectedDisabledShape = selectedDisabledShape,
        focusedSelectedDisabledShape = focusedSelectedDisabledShape
    )

    /**
     * Creates a [ToggleableSurfaceColors] that represents the default container & content colors
     * used in a toggleable Surface.
     *
     * @param containerColor the container color used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param contentColor the content color used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedContainerColor the container color used when the Surface is enabled and
     * focused.
     * @param focusedContentColor the content color used when the Surface is enabled and
     * focused.
     * @param pressedContainerColor the container color used when the Surface is enabled and
     * pressed.
     * @param pressedContentColor the content color used when the Surface is enabled and
     * pressed.
     * @param selectedContainerColor the container color used when the Surface is enabled and
     * selected.
     * @param selectedContentColor the content color used when the Surface is enabled and
     * selected.
     * @param disabledContainerColor the container color used when the Surface is not enabled.
     * @param disabledContentColor the content color used when the Surface is not enabled.
     * @param focusedSelectedContainerColor the container color used when the Surface is enabled,
     * focused and selected.
     * @param focusedSelectedContentColor the content color used when the Surface is enabled,
     * focused and selected.
     * @param pressedSelectedContainerColor the container color used when the Surface is enabled,
     * pressed and selected.
     * @param pressedSelectedContentColor the content color used when the Surface is enabled,
     * pressed and selected.
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = contentColorFor(containerColor),
        focusedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor: Color = contentColorFor(focusedContainerColor),
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = contentColorFor(pressedContainerColor),
        selectedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface
            .copy(alpha = SelectedContainerAlpha),
        selectedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = DisabledContainerAlpha),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedSelectedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface
            .copy(alpha = SelectedContainerAlpha),
        focusedSelectedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedSelectedContainerColor: Color = focusedSelectedContainerColor,
        pressedSelectedContentColor: Color = focusedSelectedContentColor
    ) = ToggleableSurfaceColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor
    )

    /**
     * Creates a [ToggleableSurfaceScale] that represents the default scales used in a
     * toggleable Surface. scales are used to modify the size of a composable in different
     * [Interaction] states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc.
     *
     * @param scale the scale used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedScale the scale used when the Surface is enabled and focused.
     * @param pressedScale the scale used when the Surface is enabled and pressed.
     * @param selectedScale the scale used when the Surface is enabled and selected.
     * @param disabledScale the scale used when the Surface is not enabled.
     * @param focusedSelectedScale the scale used when the Surface is enabled, focused and
     * selected.
     * @param focusedDisabledScale the scale used when the Surface is not enabled and
     * focused.
     * @param pressedSelectedScale the scale used when the Surface is enabled, pressed and
     * selected.
     * @param selectedDisabledScale the scale used when the Surface is not enabled and
     * selected.
     * @param focusedSelectedDisabledScale the scale used when the Surface is not enabled,
     * focused and selected.
     */
    fun scale(
        scale: Float = 1f,
        focusedScale: Float = 1.1f,
        pressedScale: Float = scale,
        selectedScale: Float = scale,
        disabledScale: Float = scale,
        focusedSelectedScale: Float = focusedScale,
        focusedDisabledScale: Float = disabledScale,
        pressedSelectedScale: Float = scale,
        selectedDisabledScale: Float = disabledScale,
        focusedSelectedDisabledScale: Float = disabledScale
    ) = ToggleableSurfaceScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
        selectedDisabledScale = selectedDisabledScale,
        focusedSelectedDisabledScale = focusedSelectedDisabledScale
    )

    /**
     * Creates a [ToggleableSurfaceBorder] that represents the default [Border]s applied on a
     * toggleable Surface in different [Interaction] states.
     *
     * @param border the [Border] used when the Surface is enabled, and has no other
     * [Interaction]s.
     * @param focusedBorder the [Border] used when the Surface is enabled and focused.
     * @param pressedBorder the [Border] used when the Surface is enabled and pressed.
     * @param selectedBorder the [Border] used when the Surface is enabled and selected.
     * @param disabledBorder the [Border] used when the Surface is not enabled.
     * @param focusedSelectedBorder the [Border] used when the Surface is enabled, focused and
     * selected.
     * @param focusedDisabledBorder the [Border] used when the Surface is not enabled and focused.
     * @param pressedSelectedBorder the [Border] used when the Surface is enabled, pressed and
     * selected.
     * @param selectedDisabledBorder the [Border] used when the Surface is not enabled and
     * selected.
     * @param focusedSelectedDisabledBorder the [Border] used when the Surface is not enabled,
     * focused and selected.
     */
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = border,
        pressedBorder: Border = focusedBorder,
        selectedBorder: Border = border,
        disabledBorder: Border = border,
        focusedSelectedBorder: Border = focusedBorder,
        focusedDisabledBorder: Border = disabledBorder,
        pressedSelectedBorder: Border = border,
        selectedDisabledBorder: Border = disabledBorder,
        focusedSelectedDisabledBorder: Border = disabledBorder
    ) = ToggleableSurfaceBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
        selectedDisabledBorder = selectedDisabledBorder,
        focusedSelectedDisabledBorder = focusedSelectedDisabledBorder
    )

    /**
     * Creates a [ToggleableSurfaceGlow] that represents the default [Glow]s used in a
     * toggleable Surface.
     *
     * @param glow the [Glow] used when the Surface is enabled, and has no other [Interaction]s.
     * @param focusedGlow the [Glow] used when the Surface is enabled and focused.
     * @param pressedGlow the [Glow] used when the Surface is enabled and pressed.
     * @param selectedGlow the [Glow] used when the Surface is enabled and selected.
     * @param focusedSelectedGlow the [Glow] used when the Surface is enabled, focused and selected.
     * @param pressedSelectedGlow the [Glow] used when the Surface is enabled, pressed and selected.
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow,
        selectedGlow: Glow = glow,
        focusedSelectedGlow: Glow = focusedGlow,
        pressedSelectedGlow: Glow = glow
    ) = ToggleableSurfaceGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )

    internal fun shape(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        shape: ToggleableSurfaceShape
    ): Shape {
        return when {
            enabled && selected && pressed -> shape.pressedSelectedShape
            enabled && selected && focused -> shape.focusedSelectedShape
            enabled && selected -> shape.selectedShape
            enabled && pressed -> shape.pressedShape
            enabled && focused -> shape.focusedShape
            enabled -> shape.shape
            !enabled && selected && focused -> shape.focusedSelectedDisabledShape
            !enabled && selected -> shape.selectedDisabledShape
            !enabled && focused -> shape.focusedDisabledShape
            else -> shape.disabledShape
        }
    }

    internal fun containerColor(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        colors: ToggleableSurfaceColors
    ): Color {
        return when {
            enabled && selected && pressed -> colors.pressedSelectedContainerColor
            enabled && selected && focused -> colors.focusedSelectedContainerColor
            enabled && selected -> colors.selectedContainerColor
            enabled && pressed -> colors.pressedContainerColor
            enabled && focused -> colors.focusedContainerColor
            enabled -> colors.containerColor
            else -> colors.disabledContainerColor
        }
    }

    internal fun contentColor(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        colors: ToggleableSurfaceColors
    ): Color {
        return when {
            enabled && selected && pressed -> colors.pressedSelectedContentColor
            enabled && selected && focused -> colors.focusedSelectedContentColor
            enabled && selected -> colors.selectedContentColor
            enabled && pressed -> colors.pressedContentColor
            enabled && focused -> colors.focusedContentColor
            enabled -> colors.contentColor
            else -> colors.disabledContentColor
        }
    }

    internal fun scale(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        scale: ToggleableSurfaceScale
    ): Float {
        return when {
            enabled && selected && pressed -> scale.pressedSelectedScale
            enabled && selected && focused -> scale.focusedSelectedScale
            enabled && selected -> scale.selectedScale
            enabled && pressed -> scale.pressedScale
            enabled && focused -> scale.focusedScale
            enabled -> scale.scale
            !enabled && selected && focused -> scale.focusedSelectedDisabledScale
            !enabled && selected -> scale.selectedDisabledScale
            !enabled && focused -> scale.focusedDisabledScale
            else -> scale.disabledScale
        }
    }

    internal fun border(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        border: ToggleableSurfaceBorder
    ): Border {
        return when {
            enabled && selected && pressed -> border.pressedSelectedBorder
            enabled && selected && focused -> border.focusedSelectedBorder
            enabled && selected -> border.selectedBorder
            enabled && pressed -> border.pressedBorder
            enabled && focused -> border.focusedBorder
            enabled -> border.border
            !enabled && selected && focused -> border.focusedSelectedDisabledBorder
            !enabled && selected -> border.selectedDisabledBorder
            !enabled && focused -> border.focusedDisabledBorder
            else -> border.disabledBorder
        }
    }

    internal fun glow(
        enabled: Boolean,
        focused: Boolean,
        pressed: Boolean,
        selected: Boolean,
        glow: ToggleableSurfaceGlow
    ): Glow {
        return when {
            enabled && selected && pressed -> glow.pressedSelectedGlow
            enabled && selected && focused -> glow.focusedSelectedGlow
            enabled && selected -> glow.selectedGlow
            enabled && pressed -> glow.pressedGlow
            enabled && focused -> glow.focusedGlow
            enabled -> glow.glow
            else -> Glow.None
        }
    }
}

private const val DisabledContainerAlpha = 0.4f
private const val SelectedContainerAlpha = 0.5f
