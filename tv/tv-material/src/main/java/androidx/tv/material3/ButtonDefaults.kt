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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

internal object BaseButtonDefaults {
    val MinWidth = 58.dp
    val MinHeight = 40.dp
}

object ButtonDefaults {
    private val ContainerShape = CircleShape
    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 10.dp
    private val ButtonWithIconHorizontalStartPadding = 12.dp

    val ContentPadding =
        PaddingValues(
            start = ButtonHorizontalPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    val ButtonWithIconContentPadding =
        PaddingValues(
            start = ButtonWithIconHorizontalStartPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    /** The default size of the icon when used inside any button. */
    val IconSize = 20.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside any button.
     */
    val IconSpacing = 8.dp

    /**
     * Creates a [ButtonShape] that represents the default container shapes used in a FilledButton.
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
    ) =
        ButtonShape(
            shape = shape,
            focusedShape = focusedShape,
            pressedShape = pressedShape,
            disabledShape = disabledShape,
            focusedDisabledShape = focusedDisabledShape
        )

    /**
     * Creates a [ButtonColors] that represents the default colors used in a FilledButton.
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
        contentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = focusedContentColor,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    ) =
        ButtonColors(
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
     * Creates a [ButtonScale] that represents the default scales used in a FilledButton. scales are
     * used to modify the size of a composable in different [Interaction] states e.g. 1f (original)
     * in default state, 1.2f (scaled up) in focused state, 0.8f (scaled down) in pressed state,
     * etc.
     *
     * @param scale the scale to be used for this Button when enabled
     * @param focusedScale the scale to be used for this Button when focused
     * @param pressedScale the scale to be used for this Button when pressed
     * @param disabledScale the scale to be used for this Button when disabled
     * @param focusedDisabledScale the scale to be used for this Button when disabled and focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) =
        ButtonScale(
            scale = scale,
            focusedScale = focusedScale,
            pressedScale = pressedScale,
            disabledScale = disabledScale,
            focusedDisabledScale = focusedDisabledScale
        )

    /**
     * Creates a [ButtonBorder] that represents the default [Border]s applied on a FilledButton in
     * different [Interaction] states.
     *
     * @param border the [Border] to be used for this Button when enabled
     * @param focusedBorder the [Border] to be used for this Button when focused
     * @param pressedBorder the [Border] to be used for this Button when pressed
     * @param disabledBorder the [Border] to be used for this Button when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Button when disabled and
     *   focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = border,
        pressedBorder: Border = focusedBorder,
        disabledBorder: Border = border,
        focusedDisabledBorder: Border =
            Border(
                border =
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    ),
                shape = ContainerShape
            )
    ) =
        ButtonBorder(
            border = border,
            focusedBorder = focusedBorder,
            pressedBorder = pressedBorder,
            disabledBorder = disabledBorder,
            focusedDisabledBorder = focusedDisabledBorder
        )

    /**
     * Creates a [ButtonGlow] that represents the default [Glow]s used in a FilledButton.
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(glow: Glow = Glow.None, focusedGlow: Glow = glow, pressedGlow: Glow = glow) =
        ButtonGlow(glow = glow, focusedGlow = focusedGlow, pressedGlow = pressedGlow)
}

object OutlinedButtonDefaults {
    private val ContainerShape = CircleShape
    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 10.dp
    private val ButtonWithIconHorizontalStartPadding = 12.dp

    val ContentPadding =
        PaddingValues(
            start = ButtonHorizontalPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    /** The default size of the icon when used inside any button. */
    val IconSize = 20.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside any button.
     */
    val IconSpacing = 8.dp

    /** The default content padding used by [OutlinedButton] that contains an [Icon]. */
    val ButtonWithIconContentPadding =
        PaddingValues(
            start = ButtonWithIconHorizontalStartPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    /**
     * Creates a [ButtonShape] that represents the default container shapes used in an
     * OutlinedButton.
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
    ) =
        ButtonShape(
            shape = shape,
            focusedShape = focusedShape,
            pressedShape = pressedShape,
            disabledShape = disabledShape,
            focusedDisabledShape = focusedDisabledShape
        )

    /**
     * Creates a [ButtonColors] that represents the default colors used in a OutlinedButton.
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
        contentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = focusedContentColor,
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    ) =
        ButtonColors(
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
     * Creates a [ButtonScale] that represents the default scales used in an OutlinedButton. scales
     * are used to modify the size of a composable in different [Interaction] states e.g. 1f
     * (original) in default state, 1.2f (scaled up) in focused state, 0.8f (scaled down) in pressed
     * state, etc.
     *
     * @param scale the scale to be used for this Button when enabled
     * @param focusedScale the scale to be used for this Button when focused
     * @param pressedScale the scale to be used for this Button when pressed
     * @param disabledScale the scale to be used for this Button when disabled
     * @param focusedDisabledScale the scale to be used for this Button when disabled and focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) =
        ButtonScale(
            scale = scale,
            focusedScale = focusedScale,
            pressedScale = pressedScale,
            disabledScale = disabledScale,
            focusedDisabledScale = focusedDisabledScale
        )

    /**
     * Creates a [ButtonBorder] that represents the default [Border]s applied on an OutlinedButton
     * in different [Interaction] states.
     *
     * @param border the [Border] to be used for this Button when enabled
     * @param focusedBorder the [Border] to be used for this Button when focused
     * @param pressedBorder the [Border] to be used for this Button when pressed
     * @param disabledBorder the [Border] to be used for this Button when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Button when disabled and
     *   focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border =
            Border(
                border =
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ),
                shape = ContainerShape
            ),
        focusedBorder: Border =
            Border(
                border =
                    BorderStroke(
                        width = 1.65.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                shape = ContainerShape
            ),
        pressedBorder: Border =
            Border(
                border =
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                shape = ContainerShape
            ),
        disabledBorder: Border =
            Border(
                border =
                    BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    ),
                shape = ContainerShape
            ),
        focusedDisabledBorder: Border = disabledBorder
    ) =
        ButtonBorder(
            border = border,
            focusedBorder = focusedBorder,
            pressedBorder = pressedBorder,
            disabledBorder = disabledBorder,
            focusedDisabledBorder = focusedDisabledBorder
        )

    /**
     * Creates a [ButtonGlow] that represents the default [Glow]s used in an OutlinedButton.
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(glow: Glow = Glow.None, focusedGlow: Glow = glow, pressedGlow: Glow = glow) =
        ButtonGlow(glow = glow, focusedGlow = focusedGlow, pressedGlow = pressedGlow)
}
