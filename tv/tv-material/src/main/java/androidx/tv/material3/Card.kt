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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Cards contain content and actions that relate information about a subject.
 *
 * This Card handles click events, calling its [onClick] lambda.
 *
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param shape [CardShape] defines the shape of this card's container in different interaction
 * states. See [CardDefaults.shape].
 * @param colors [CardColors] defines the background & content colors used in this card for
 * different interaction states. See [CardDefaults.colors].
 * @param scale [CardScale] defines size of the card relative to its original size for different
 * interaction states. See [CardDefaults.scale].
 * @param border [CardBorder] defines a border around the card for different interaction states.
 * See [CardDefaults.border].
 * @param glow [CardGlow] defines a shadow to be shown behind the card for different interaction
 * states. See [CardDefaults.glow].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this card. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this card in different states.
 * @param content defines the [Composable] content inside the Card.
 */
@ExperimentalTvMaterial3Api
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    glow: CardGlow = CardDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape.toClickableSurfaceShape(),
        color = colors.toClickableSurfaceContainerColor(),
        contentColor = colors.toClickableSurfaceContentColor(),
        scale = scale.toClickableSurfaceScale(),
        border = border.toClickableSurfaceBorder(),
        glow = glow.toClickableSurfaceGlow(),
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}

/**
 * Contains the default values used by all card types.
 */
@ExperimentalTvMaterial3Api
object CardDefaults {
    /**
    * The default [Shape] used by Cards.
    */
    private val ContainerShape = RoundedCornerShape(8.dp)

    /**
     * Creates a [CardShape] that represents the default container shapes used in a Card.
     *
     * @param shape the default shape used when the Card has no other [Interaction]s.
     * @param focusedShape the shape used when the Card is focused.
     * @param pressedShape the shape used when the Card is pressed.
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape
    ) = CardShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape
    )

    /**
     * Creates [CardColors] that represents the default container & content colors used in a Card.
     *
     * @param containerColor the default container color of this Card.
     * @param contentColor the default content color of this Card.
     * @param focusedContainerColor the container color of this Card when focused.
     * @param focusedContentColor the content color of this Card when focused.
     * @param pressedContainerColor the container color of this Card when pressed.
     * @param pressedContentColor the content color of this Card when pressed.
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = contentColorFor(containerColor),
        focusedContainerColor: Color = containerColor,
        focusedContentColor: Color = contentColorFor(focusedContainerColor),
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = contentColorFor(pressedContainerColor)
    ) = CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor
    )

    /**
     * Creates a [CardScale] that represents the default scales used in a Card.
     * Scales are used to modify the size of a composable in different [Interaction] states
     * e.g. 1f (original) in default state, 1.1f (scaled up) in focused state, 0.8f (scaled down)
     * in pressed state, etc.
     *
     * @param scale the default scale to be used for this Card.
     * @param focusedScale the scale to be used for this Card when focused.
     * @param pressedScale the scale to be used for this Card when pressed.
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale
    ) = CardScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale
    )

    /**
     * Creates a [CardBorder] that represents the border [Border]s applied on a Card in
     * different [Interaction] states.
     *
     * @param border the default [Border] to be used for this Card.
     * @param focusedBorder the [Border] to be used for this Card when focused.
     * @param pressedBorder the [Border] to be used for this Card when pressed.
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = Border(
            border = BorderStroke(
                width = 3.dp,
                color = MaterialTheme.colorScheme.border
            ),
            shape = ContainerShape
        ),
        pressedBorder: Border = focusedBorder
    ) = CardBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder
    )

    /**
     * Creates a [CardGlow] that represents the default [Glow]s used in a card.
     *
     * @param glow the default [Glow] behind this Card.
     * @param focusedGlow the [Glow] behind this Card when focused.
     * @param pressedGlow the [Glow] behind this Card when pressed.
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = CardGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardColors.toClickableSurfaceContainerColor() =
    ClickableSurfaceColor(
        color = containerColor,
        focusedColor = focusedContainerColor,
        pressedColor = pressedContainerColor,
        disabledColor = containerColor
    )

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardColors.toClickableSurfaceContentColor() =
    ClickableSurfaceColor(
        color = contentColor,
        focusedColor = focusedContentColor,
        pressedColor = pressedContentColor,
        disabledColor = contentColor
    )

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardShape.toClickableSurfaceShape() =
    ClickableSurfaceShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = shape,
        focusedDisabledShape = shape
    )

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardScale.toClickableSurfaceScale() =
    ClickableSurfaceScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = scale,
        focusedDisabledScale = scale
    )

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardBorder.toClickableSurfaceBorder() =
    ClickableSurfaceBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = border,
        focusedDisabledBorder = border
    )

@OptIn(ExperimentalTvMaterial3Api::class)
private fun CardGlow.toClickableSurfaceGlow() =
    ClickableSurfaceGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
