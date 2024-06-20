/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.tokens.ElevatedCardTokens
import androidx.compose.material3.tokens.FilledCardTokens
import androidx.compose.material3.tokens.OutlinedCardTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design filled card</a>.
 *
 * Cards contain contain content and actions that relate information about a subject. Filled cards
 * provide subtle separation from the background. This has less emphasis than elevated or outlined
 * cards.
 *
 * This Card does not handle input events - see the other Card overloads if you want a clickable or
 * selectable Card.
 *
 * ![Filled card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-card.png)
 *
 * Card sample:
 *
 * @sample androidx.compose.material3.samples.CardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the colors used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param content The content displayed on the card
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor(enabled = true),
        contentColor = colors.contentColor(enabled = true),
        shadowElevation = elevation.shadowElevation(enabled = true, interactionSource = null).value,
        border = border,
    ) {
        Column(content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design filled card</a>.
 *
 * Cards contain contain content and actions that relate information about a subject. Filled cards
 * provide subtle separation from the background. This has less emphasis than elevated or outlined
 * cards.
 *
 * This Card handles click events, calling its [onClick] lambda.
 *
 * ![Filled card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-card.png)
 *
 * Clickable card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.cardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled),
        contentColor = colors.contentColor(enabled),
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design elevated card</a>.
 *
 * Elevated cards contain content and actions that relate information about a subject. They have a
 * drop shadow, providing more separation from the background than filled cards, but less than
 * outlined cards.
 *
 * This ElevatedCard does not handle input events - see the other ElevatedCard overloads if you want
 * a clickable or selectable ElevatedCard.
 *
 * ![Elevated card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-card.png)
 *
 * Elevated card sample:
 *
 * @sample androidx.compose.material3.samples.ElevatedCardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container and shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.elevatedCardElevation].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param content The content displayed on the card
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) =
    Card(
        modifier = modifier,
        shape = shape,
        border = null,
        elevation = elevation,
        colors = colors,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design elevated card</a>.
 *
 * Elevated cards contain content and actions that relate information about a subject. They have a
 * drop shadow, providing more separation from the background than filled cards, but less than
 * outlined cards.
 *
 * This ElevatedCard handles click events, calling its [onClick] lambda.
 *
 * ![Elevated card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-card.png)
 *
 * Clickable elevated card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableElevatedCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container and shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.elevatedCardElevation].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) =
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design outlined card</a>.
 *
 * Outlined cards contain content and actions that relate information about a subject. They have a
 * visual boundary around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard does not handle input events - see the other OutlinedCard overloads if you want
 * a clickable or selectable OutlinedCard.
 *
 * ![Outlined card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-card.png)
 *
 * Outlined card sample:
 *
 * @sample androidx.compose.material3.samples.OutlinedCardSample
 * @param modifier the [Modifier] to be applied to this card
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.outlinedCardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param content The content displayed on the card
 */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(),
    content: @Composable ColumnScope.() -> Unit
) =
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external"
 * target="_blank">Material Design outlined card</a>.
 *
 * Outlined cards contain content and actions that relate information about a subject. They have a
 * visual boundary around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard handles click events, calling its [onClick] lambda.
 *
 * ![Outlined card
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-card.png)
 *
 * Clickable outlined card sample:
 *
 * @sample androidx.compose.material3.samples.ClickableOutlinedCardSample
 * @param onClick called when this card is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param enabled controls the enabled state of this card. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this card's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [CardColors] that will be used to resolve the color(s) used for this card in
 *   different states. See [CardDefaults.outlinedCardColors].
 * @param elevation [CardElevation] used to resolve the elevation for this card in different states.
 *   This controls the size of the shadow below the card. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   also: [Surface].
 * @param border the border to draw around the container of this card
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content The content displayed on the card
 */
@Composable
fun OutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.outlinedShape,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = CardDefaults.outlinedCardBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) =
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content
    )

/** Contains the default values used by all card types. */
object CardDefaults {
    // shape Defaults
    /** Default shape for a card. */
    val shape: Shape
        @Composable get() = FilledCardTokens.ContainerShape.value

    /** Default shape for an elevated card. */
    val elevatedShape: Shape
        @Composable get() = ElevatedCardTokens.ContainerShape.value

    /** Default shape for an outlined card. */
    val outlinedShape: Shape
        @Composable get() = OutlinedCardTokens.ContainerShape.value

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for a [Card].
     *
     * @param defaultElevation the elevation used when the [Card] is has no other [Interaction]s.
     * @param pressedElevation the elevation used when the [Card] is pressed.
     * @param focusedElevation the elevation used when the [Card] is focused.
     * @param hoveredElevation the elevation used when the [Card] is hovered.
     * @param draggedElevation the elevation used when the [Card] is dragged.
     * @param disabledElevation the elevation used when the [Card] is disabled.
     */
    @Composable
    fun cardElevation(
        defaultElevation: Dp = FilledCardTokens.ContainerElevation,
        pressedElevation: Dp = FilledCardTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledCardTokens.HoverContainerElevation,
        draggedElevation: Dp = FilledCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = FilledCardTokens.DisabledContainerElevation
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation
        )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [ElevatedCard].
     *
     * @param defaultElevation the elevation used when the [ElevatedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [ElevatedCard] is pressed.
     * @param focusedElevation the elevation used when the [ElevatedCard] is focused.
     * @param hoveredElevation the elevation used when the [ElevatedCard] is hovered.
     * @param draggedElevation the elevation used when the [ElevatedCard] is dragged.
     * @param disabledElevation the elevation used when the [Card] is disabled.
     */
    @Composable
    fun elevatedCardElevation(
        defaultElevation: Dp = ElevatedCardTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedCardTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = ElevatedCardTokens.HoverContainerElevation,
        draggedElevation: Dp = ElevatedCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = ElevatedCardTokens.DisabledContainerElevation
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation
        )

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [OutlinedCard].
     *
     * @param defaultElevation the elevation used when the [OutlinedCard] is has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when the [OutlinedCard] is pressed.
     * @param focusedElevation the elevation used when the [OutlinedCard] is focused.
     * @param hoveredElevation the elevation used when the [OutlinedCard] is hovered.
     * @param draggedElevation the elevation used when the [OutlinedCard] is dragged.
     */
    @Composable
    fun outlinedCardElevation(
        defaultElevation: Dp = OutlinedCardTokens.ContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = OutlinedCardTokens.DraggedContainerElevation,
        disabledElevation: Dp = OutlinedCardTokens.DisabledContainerElevation
    ): CardElevation =
        CardElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation
        )

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     */
    @Composable fun cardColors() = MaterialTheme.colorScheme.defaultCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in a
     * [Card].
     *
     * @param containerColor the container color of this [Card] when enabled.
     * @param contentColor the content color of this [Card] when enabled.
     * @param disabledContainerColor the container color of this [Card] when not enabled.
     * @param disabledContentColor the content color of this [Card] when not enabled.
     */
    @Composable
    fun cardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultCardColors: CardColors
        get() {
            return defaultCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(FilledCardTokens.ContainerColor),
                        contentColor = contentColorFor(fromToken(FilledCardTokens.ContainerColor)),
                        disabledContainerColor =
                            fromToken(FilledCardTokens.DisabledContainerColor)
                                .copy(alpha = FilledCardTokens.DisabledContainerOpacity)
                                .compositeOver(fromToken(FilledCardTokens.ContainerColor)),
                        disabledContentColor =
                            contentColorFor(fromToken(FilledCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultCardColorsCached = it }
        }

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     */
    @Composable fun elevatedCardColors() = MaterialTheme.colorScheme.defaultElevatedCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [ElevatedCard].
     *
     * @param containerColor the container color of this [ElevatedCard] when enabled.
     * @param contentColor the content color of this [ElevatedCard] when enabled.
     * @param disabledContainerColor the container color of this [ElevatedCard] when not enabled.
     * @param disabledContentColor the content color of this [ElevatedCard] when not enabled.
     */
    @Composable
    fun elevatedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColor.copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultElevatedCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultElevatedCardColors: CardColors
        get() {
            return defaultElevatedCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(ElevatedCardTokens.ContainerColor),
                        contentColor =
                            contentColorFor(fromToken(ElevatedCardTokens.ContainerColor)),
                        disabledContainerColor =
                            fromToken(ElevatedCardTokens.DisabledContainerColor)
                                .copy(alpha = ElevatedCardTokens.DisabledContainerOpacity)
                                .compositeOver(
                                    fromToken(ElevatedCardTokens.DisabledContainerColor)
                                ),
                        disabledContentColor =
                            contentColorFor(fromToken(ElevatedCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultElevatedCardColorsCached = it }
        }

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     */
    @Composable fun outlinedCardColors() = MaterialTheme.colorScheme.defaultOutlinedCardColors

    /**
     * Creates a [CardColors] that represents the default container and content colors used in an
     * [OutlinedCard].
     *
     * @param containerColor the container color of this [OutlinedCard] when enabled.
     * @param contentColor the content color of this [OutlinedCard] when enabled.
     * @param disabledContainerColor the container color of this [OutlinedCard] when not enabled.
     * @param disabledContentColor the content color of this [OutlinedCard] when not enabled.
     */
    @Composable
    fun outlinedCardColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = contentColorFor(containerColor).copy(DisabledAlpha),
    ): CardColors =
        MaterialTheme.colorScheme.defaultOutlinedCardColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultOutlinedCardColors: CardColors
        get() {
            return defaultOutlinedCardColorsCached
                ?: CardColors(
                        containerColor = fromToken(OutlinedCardTokens.ContainerColor),
                        contentColor =
                            contentColorFor(fromToken(OutlinedCardTokens.ContainerColor)),
                        disabledContainerColor = fromToken(OutlinedCardTokens.ContainerColor),
                        disabledContentColor =
                            contentColorFor(fromToken(OutlinedCardTokens.ContainerColor))
                                .copy(DisabledAlpha),
                    )
                    .also { defaultOutlinedCardColorsCached = it }
        }

    /**
     * Creates a [BorderStroke] that represents the default border used in [OutlinedCard].
     *
     * @param enabled whether the card is enabled
     */
    @Composable
    fun outlinedCardBorder(enabled: Boolean = true): BorderStroke {
        val color =
            if (enabled) {
                OutlinedCardTokens.OutlineColor.value
            } else {
                OutlinedCardTokens.DisabledOutlineColor.value
                    .copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
                    .compositeOver(ElevatedCardTokens.ContainerColor.value)
            }
        return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
    }
}

/**
 * Represents the elevation for a card in different states.
 * - See [CardDefaults.cardElevation] for the default elevation used in a [Card].
 * - See [CardDefaults.elevatedCardElevation] for the default elevation used in an [ElevatedCard].
 * - See [CardDefaults.outlinedCardElevation] for the default elevation used in an [OutlinedCard].
 */
@Immutable
class CardElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp
) {
    /**
     * Represents the shadow elevation used in a card, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the card to give it higher emphasis.
     *
     * @param enabled whether the card is enabled
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource?
    ): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    is DragInteraction.Start -> draggedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state.
                    animatable.snapTo(target)
                } else {
                    val lastInteraction =
                        when (animatable.targetValue) {
                            pressedElevation -> PressInteraction.Press(Offset.Zero)
                            hoveredElevation -> HoverInteraction.Enter()
                            focusedElevation -> FocusInteraction.Focus()
                            draggedElevation -> DragInteraction.Start()
                            else -> null
                        }
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target
                    )
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Represents the container and content colors used in a card in different states.
 *
 * @param containerColor the container color of this [Card] when enabled.
 * @param contentColor the content color of this [Card] when enabled.
 * @param disabledContainerColor the container color of this [Card] when not enabled.
 * @param disabledContentColor the content color of this [Card] when not enabled.
 * @constructor create an instance with arbitrary colors.
 * - See [CardDefaults.cardColors] for the default colors used in a [Card].
 * - See [CardDefaults.elevatedCardColors] for the default colors used in a [ElevatedCard].
 * - See [CardDefaults.outlinedCardColors] for the default colors used in a [OutlinedCard].
 */
@Immutable
class CardColors
constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Returns a copy of this CardColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor
    ) =
        CardColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
        )

    /**
     * Represents the container color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the content color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean) =
        if (enabled) contentColor else disabledContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}
