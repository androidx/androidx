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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@ExperimentalTvMaterial3Api
object IconButtonDefaults {
    private val ContainerShape = CircleShape

    /** The size of a small icon inside [IconButton] */
    val SmallIconSize = 16.dp

    /** The size of a medium icon inside [IconButton] */
    val MediumIconSize = 20.dp

    /** The size of a large icon inside [IconButton] */
    val LargeIconSize = 28.dp

    /** The size of a small [IconButton] */
    val SmallButtonSize = 28.dp

    /** The size of a medium [IconButton]. */
    val MediumButtonSize = 40.dp

    /** The size of a large [IconButton]. */
    val LargeButtonSize = 56.dp

    /**
     * Creates a [ButtonShape] that represents the default container shapes used in a
     * IconButton.
     *
     * @param shape the shape used when the Button is enabled, and has no other [Interaction]s.
     * @param focusedShape the shape used when the Button is enabled and focused.
     * @param pressedShape the shape used when the Button is enabled pressed.
     * @param disabledShape the shape used when the Button is not enabled.
     * @param focusedDisabledShape the shape used when the Button is not enabled and focused.
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape
    ) = ButtonShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = disabledShape,
        focusedDisabledShape = focusedDisabledShape
    )

    /**
     * Creates a [ButtonColors] that represents the default colors used in a IconButton.
     *
     * @param containerColor the container color of this Button when enabled
     * @param contentColor the content color of this Button when enabled
     * @param focusedContainerColor the container color of this Button when enabled and focused
     * @param focusedContentColor the content color of this Button when enabled and focused
     * @param pressedContainerColor the container color of this Button when enabled and pressed
     * @param pressedContentColor the content color of this Button when enabled and pressed
     * @param disabledContainerColor the container color of this Button when not enabled
     * @param disabledContentColor the content color of this Button when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = focusedContentColor,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor: Color = contentColor,
    ) = ButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [ButtonScale] that represents the default scales used in a IconButton.
     * scales are used to modify the size of a composable in different [Interaction]
     * states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc.
     *
     * @param scale the scale to be used for this Button when enabled
     * @param focusedScale the scale to be used for this Button when focused
     * @param pressedScale the scale to be used for this Button when pressed
     * @param disabledScale the scale to be used for this Button when disabled
     * @param focusedDisabledScale the scale to be used for this Button when disabled and
     * focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) = ButtonScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = disabledScale,
        focusedDisabledScale = focusedDisabledScale
    )

    /**
     * Creates a [ButtonBorder] that represents the default [Border]s applied on a
     * IconButton in different [Interaction] states.
     *
     * @param border the [Border] to be used for this Button when enabled
     * @param focusedBorder the [Border] to be used for this Button when focused
     * @param pressedBorder the [Border] to be used for this Button when pressed
     * @param disabledBorder the [Border] to be used for this Button when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Button when disabled and
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
                color = MaterialTheme.colorScheme.border.copy(alpha = 0.2f)
            ),
            shape = ContainerShape
        )
    ) = ButtonBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = disabledBorder,
        focusedDisabledBorder = focusedDisabledBorder
    )

    /**
     * Creates a [ButtonGlow] that represents the default [Glow]s used in a IconButton.
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = ButtonGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}

@ExperimentalTvMaterial3Api
object OutlinedIconButtonDefaults {
    private val ContainerShape = CircleShape

    val SmallIconSize = 16.dp
    val MediumIconSize = 20.dp
    val LargeIconSize = 28.dp

    /** The size of a small [OutlinedIconButton] */
    val SmallButtonSize = 28.dp

    /** The size of a medium [OutlinedIconButton]. */
    val MediumButtonSize = 40.dp

    /** The size of a large [OutlinedIconButton]. */
    val LargeButtonSize = 56.dp

    /**
     * Creates a [ButtonShape] that represents the default container shapes used in an
     * OutlinedIconButton.
     *
     * @param shape the shape used when the Button is enabled, and has no other [Interaction]s.
     * @param focusedShape the shape used when the Button is enabled and focused.
     * @param pressedShape the shape used when the Button is enabled pressed.
     * @param disabledShape the shape used when the Button is not enabled.
     * @param focusedDisabledShape the shape used when the Button is not enabled and focused.
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape
    ) = ButtonShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = disabledShape,
        focusedDisabledShape = focusedDisabledShape
    )

    /**
     * Creates a [ButtonColors] that represents the default colors used in a OutlinedIconButton.
     *
     * @param containerColor the container color of this Button when enabled
     * @param contentColor the content color of this Button when enabled
     * @param focusedContainerColor the container color of this Button when enabled and focused
     * @param focusedContentColor the content color of this Button when enabled and focused
     * @param pressedContainerColor the container color of this Button when enabled and pressed
     * @param pressedContentColor the content color of this Button when enabled and pressed
     * @param disabledContainerColor the container color of this Button when not enabled
     * @param disabledContentColor the content color of this Button when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = focusedContentColor,
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = contentColor,
    ) = ButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [ButtonScale] that represents the default scales used in an
     * OutlinedIconButton.
     * scales are used to modify the size of a composable in different [Interaction]
     * states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc.
     *
     * @param scale the scale to be used for this Button when enabled
     * @param focusedScale the scale to be used for this Button when focused
     * @param pressedScale the scale to be used for this Button when pressed
     * @param disabledScale the scale to be used for this Button when disabled
     * @param focusedDisabledScale the scale to be used for this Button when disabled and
     * focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) = ButtonScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = disabledScale,
        focusedDisabledScale = focusedDisabledScale
    )

    /**
     * Creates a [ButtonBorder] that represents the default [Border]s applied on an
     * OutlinedIconButton in different [Interaction] states.
     *
     * @param border the [Border] to be used for this Button when enabled
     * @param focusedBorder the [Border] to be used for this Button when focused
     * @param pressedBorder the [Border] to be used for this Button when pressed
     * @param disabledBorder the [Border] to be used for this Button when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Button when disabled and
     * focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ),
            shape = ContainerShape
        ),
        focusedBorder: Border = Border(
            border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.onSurface),
            shape = ContainerShape
        ),
        pressedBorder: Border = Border(
            border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.onSurface),
            shape = ContainerShape
        ),
        disabledBorder: Border = Border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            ),
            shape = ContainerShape
        ),
        focusedDisabledBorder: Border = disabledBorder
    ) = ButtonBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = disabledBorder,
        focusedDisabledBorder = focusedDisabledBorder
    )

    /**
     * Creates a [ButtonGlow] that represents the default [Glow]s used in an OutlinedIconButton.
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = ButtonGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}
