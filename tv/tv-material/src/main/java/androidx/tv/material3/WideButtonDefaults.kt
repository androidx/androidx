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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

internal object BaseWideButtonDefaults {
    const val SubtitleAlpha = 0.8f
    val MinWidth = 240.dp
    val MinHeight = 48.dp
    val MinHeightWithSubtitle = 64.dp
    val HorizontalContentGap = 12.dp
    val VerticalContentGap = 4.dp
}

object WideButtonDefaults {
    private val HorizontalPadding = 16.dp
    private val VerticalPadding = 10.dp

    /** The default content padding used by [WideButton] */
    internal val ContentPadding =
        PaddingValues(
            start = HorizontalPadding,
            top = VerticalPadding,
            end = HorizontalPadding,
            bottom = VerticalPadding
        )

    private val ContainerShape = RoundedCornerShape(12.dp)

    /** Default background for a [WideButton] */
    @Composable
    fun Background(
        enabled: Boolean,
        interactionSource: MutableInteractionSource,
    ) {
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isPressed = interactionSource.collectIsPressedAsState().value

        val backgroundColor =
            when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                isPressed -> MaterialTheme.colorScheme.onSurface
                isFocused -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }

        Box(modifier = Modifier.fillMaxSize().background(backgroundColor))
    }

    /**
     * Creates a [ButtonShape] that represents the default container shapes used in a [WideButton]
     *
     * @param shape the shape used when the Button is enabled, and has no other [Interaction]s
     * @param focusedShape the shape used when the Button is enabled and focused
     * @param pressedShape the shape used when the Button is enabled pressed
     * @param disabledShape the shape used when the Button is not enabled
     * @param focusedDisabledShape the shape used when the Button is not enabled and focused
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
     * Creates a [WideButtonContentColor] that represents the default content colors used in a
     * [WideButton]
     *
     * @param color the content color of this Button when enabled
     * @param focusedColor the content color of this Button when enabled and focused
     * @param pressedColor the content color of this Button when enabled and pressed
     * @param disabledColor the content color of this Button when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun contentColor(
        color: Color = MaterialTheme.colorScheme.onSurface,
        focusedColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedColor: Color = focusedColor,
        disabledColor: Color = color
    ) =
        WideButtonContentColor(
            contentColor = color,
            focusedContentColor = focusedColor,
            pressedContentColor = pressedColor,
            disabledContentColor = disabledColor
        )

    /**
     * Creates a [ButtonScale] that represents the default scales used in a [WideButton]. Scale is
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
     * Creates a [ButtonBorder] that represents the default [Border]s applied on a [WideButton] in
     * different [Interaction] states
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
                border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.border),
                inset = 0.dp,
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
     * Creates a [ButtonGlow] that represents the default [Glow]s used in a [WideButton]
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(glow: Glow = Glow.None, focusedGlow: Glow = glow, pressedGlow: Glow = glow) =
        ButtonGlow(glow = glow, focusedGlow = focusedGlow, pressedGlow = pressedGlow)
}
