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

package androidx.wear.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealScope
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.SwipeToReveal
import kotlin.math.abs

/**
 * [SwipeToReveal] Material composable for Chips. This provides the default style for consistency.
 *
 * Example of [SwipeToRevealChip] with primary and secondary actions
 * @sample androidx.wear.compose.material.samples.SwipeToRevealChipSample
 *
 * @param primaryAction A [SwipeToRevealAction] instance to describe the primary action when
 * swiping. See [SwipeToRevealDefaults.primaryAction]. The action will be triggered on click or a
 * full swipe.
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param modifier [Modifier] to be applied on the composable
 * @param secondaryAction A [SwipeToRevealAction] instance to describe the contents of secondary
 * action. See [SwipeToRevealDefaults.secondaryAction]. The action will be triggered on clicking the
 * action.
 * @param undoPrimaryAction A [SwipeToRevealAction] instance to describe the contents of undo action
 * when the primary action was triggered. See [SwipeToRevealDefaults.undoAction].
 * @param undoSecondaryAction [SwipeToRevealAction] instance to describe the contents of undo
 * action when secondary action was triggered. See [SwipeToRevealDefaults.undoAction].
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 * [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of primary and secondary action composables. Recommended shape for chips
 * is [Shapes.small].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 *
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
public fun SwipeToRevealChip(
    primaryAction: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier = Modifier,
    secondaryAction: SwipeToRevealAction? = null,
    undoPrimaryAction: SwipeToRevealAction? = null,
    undoSecondaryAction: SwipeToRevealAction? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable () -> Unit
) {
    SwipeToRevealComponent(
        primaryAction = primaryAction,
        revealState = revealState,
        modifier = modifier,
        secondaryAction = secondaryAction,
        undoPrimaryAction = undoPrimaryAction,
        undoSecondaryAction = undoSecondaryAction,
        colors = colors,
        shape = shape,
        content = content
    )
}

/**
 * [SwipeToReveal] Material composable for Cards. This provides the default style for consistency.
 *
 * Example of [SwipeToRevealCard] with primary and secondary actions
 * @sample androidx.wear.compose.material.samples.SwipeToRevealCardSample
 *
 * @param primaryAction A [SwipeToRevealAction] instance to describe the primary action when
 * swiping. See [SwipeToRevealDefaults.primaryAction]. The action will be triggered on click or a
 * full swipe.
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param modifier [Modifier] to be applied on the composable
 * @param secondaryAction A [SwipeToRevealAction] instance to describe the contents of secondary
 * action. See [SwipeToRevealDefaults.secondaryAction]. The action will be triggered on clicking the
 * action.
 * @param undoPrimaryAction A [SwipeToRevealAction] instance to describe the contents of undo action
 * when the primary action was triggered. See [SwipeToRevealDefaults.undoAction].
 * @param undoSecondaryAction [SwipeToRevealAction] instance to describe the contents of undo
 * action when secondary action was triggered. See [SwipeToRevealDefaults.undoAction].
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 * [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of primary and secondary action composables. Recommended shape for cards
 * is [SwipeToRevealDefaults.CardActionShape].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 *
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
public fun SwipeToRevealCard(
    primaryAction: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier = Modifier,
    secondaryAction: SwipeToRevealAction? = null,
    undoPrimaryAction: SwipeToRevealAction? = null,
    undoSecondaryAction: SwipeToRevealAction? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = SwipeToRevealDefaults.CardActionShape,
    content: @Composable () -> Unit
) {
    SwipeToRevealComponent(
        primaryAction = primaryAction,
        revealState = revealState,
        modifier = modifier,
        secondaryAction = secondaryAction,
        undoPrimaryAction = undoPrimaryAction,
        undoSecondaryAction = undoSecondaryAction,
        colors = colors,
        shape = shape,
        content = content
    )
}

/**
 * Defaults for Material [SwipeToReveal].
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
public object SwipeToRevealDefaults {
    /**
     * Recommended shape for [SwipeToReveal] actions when used with [Card].
     */
    public val CardActionShape = RoundedCornerShape(40.dp)

    /**
     * Colors to be used with different actions in [SwipeToReveal].
     *
     * @param primaryActionBackgroundColor The background color (color of the shape) of the primary
     * action
     * @param primaryActionContentColor The content color (text and icon) of the primary action
     * @param secondaryActionBackgroundColor The background color (color of the shape) of the
     * secondary action
     * @param secondaryActionContentColor The content color (text and icon) of the secondary
     * action
     * @param undoActionBackgroundColor The background color (color of the shape) of the undo action
     * @param undoActionContentColor The content color (text) of the undo action
     */
    @Composable
    public fun actionColors(
        primaryActionBackgroundColor: Color = MaterialTheme.colors.error,
        primaryActionContentColor: Color = MaterialTheme.colors.onError,
        secondaryActionBackgroundColor: Color = MaterialTheme.colors.surface,
        secondaryActionContentColor: Color = MaterialTheme.colors.onSurface,
        undoActionBackgroundColor: Color = MaterialTheme.colors.surface,
        undoActionContentColor: Color = MaterialTheme.colors.onSurface
    ): SwipeToRevealActionColors {
        return SwipeToRevealActionColors(
            primaryActionBackgroundColor = primaryActionBackgroundColor,
            primaryActionContentColor = primaryActionContentColor,
            secondaryActionBackgroundColor = secondaryActionBackgroundColor,
            secondaryActionContentColor = secondaryActionContentColor,
            undoActionBackgroundColor = undoActionBackgroundColor,
            undoActionContentColor = undoActionContentColor
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the primary action parameter of
     * [SwipeToRevealChip] and [SwipeToRevealCard].
     *
     * @param icon The icon that will be displayed initially on the action
     * @param label The text that will be displayed on the expanded action
     * @param modifier [Modifier] to be applied on this action composable
     * @param actionType The [RevealActionType] that is set in [RevealState.lastActionType] when
     * this action is clicked.
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun primaryAction(
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        actionType: RevealActionType = RevealActionType.PrimaryAction,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = icon,
            label = label,
            modifier = modifier,
            actionType = actionType,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the secondary action of [SwipeToRevealChip]
     * and [SwipeToRevealCard].
     *
     * @param icon The icon that will be displayed initially on the screen
     * @param modifier [Modifier] to be applied on this action composable
     * @param actionType The [RevealActionType] that is set in [RevealState.lastActionType] when
     * this action is clicked.
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun secondaryAction(
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        actionType: RevealActionType = RevealActionType.SecondaryAction,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = icon,
            label = null,
            modifier = modifier,
            actionType = actionType,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the undo action of [SwipeToRevealChip] and
     * [SwipeToRevealCard].
     *
     * @param label The text that will be displayed on the undo action composable
     * @param modifier [Modifier] to be applied on this action composable
     * @param actionType The [RevealActionType] that is set in [RevealState.lastActionType] when
     * this action is clicked.
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun undoAction(
        label: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        actionType: RevealActionType = RevealActionType.UndoAction,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = null,
            label = label,
            modifier = modifier,
            actionType = actionType,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * [ImageVector] for delete icon, often used for the primary action.
     */
    public val Delete = Icons.Outlined.Delete

    /**
     * [ImageVector] for more options icon, often used for the secondary action.
     */
    public val MoreOptions = Icons.Outlined.MoreVert

    internal val UndoButtonHorizontalPadding = 14.dp
    internal val UndoButtonVerticalPadding = 6.dp
    internal val ActionMaxHeight = 84.dp
}

/**
 * A class representing the colors applied in [SwipeToReveal] actions.
 *
 * @param primaryActionBackgroundColor Color of the shape (background) of primary action
 * @param primaryActionContentColor Color of icon or text used in the primary action
 * @param secondaryActionBackgroundColor Color of the secondary action shape (background)
 * @param secondaryActionContentColor Color of the icon or text used in the secondary action
 * @param undoActionBackgroundColor Color of the undo action shape (background)
 * @param undoActionContentColor Color of the icon or text used in the undo action
 */
@ExperimentalWearMaterialApi
public class SwipeToRevealActionColors constructor(
    val primaryActionBackgroundColor: Color,
    val primaryActionContentColor: Color,
    val secondaryActionBackgroundColor: Color,
    val secondaryActionContentColor: Color,
    val undoActionBackgroundColor: Color,
    val undoActionContentColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SwipeToRevealActionColors

        if (primaryActionBackgroundColor != other.primaryActionBackgroundColor) return false
        if (primaryActionContentColor != other.primaryActionContentColor) return false
        if (secondaryActionBackgroundColor != other.secondaryActionBackgroundColor) return false
        if (secondaryActionContentColor != other.secondaryActionContentColor) return false
        if (undoActionBackgroundColor != other.undoActionBackgroundColor) return false
        if (undoActionContentColor != other.undoActionContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryActionBackgroundColor.hashCode()
        result = 31 * result + primaryActionContentColor.hashCode()
        result = 31 * result + secondaryActionBackgroundColor.hashCode()
        result = 31 * result + secondaryActionContentColor.hashCode()
        result = 31 * result + undoActionBackgroundColor.hashCode()
        result = 31 * result + undoActionContentColor.hashCode()
        return result
    }
}

/**
 * A class containing the details required for describing the content of an action composable.
 * Both composables, [icon] and [label] are optional, however it is expected that at least one is
 * provided.
 *
 * @param icon A slot for providing the icon for this [SwipeToReveal] action
 * @param label A slot for providing a text label for this [SwipeToRevealAction] action. The
 * content provided here will be used in different perspective based on the action type
 * (primary action, secondary action or undo action).
 * @param modifier The [Modifier] to be applied on the action.
 * @param actionType The [RevealActionType] that gets applied to [RevealState.lastActionType] when
 * this action is clicked.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * interactions with this action.
 * @param onClick Called when the user clicks the action.
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
public class SwipeToRevealAction constructor(
    val icon: (@Composable () -> Unit)?,
    val label: (@Composable () -> Unit)?,
    val modifier: Modifier,
    val actionType: RevealActionType,
    val interactionSource: MutableInteractionSource,
    val onClick: () -> Unit
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SwipeToRevealAction

        if (icon != other.icon) return false
        if (label != other.label) return false
        if (modifier != other.modifier) return false
        if (actionType != other.actionType) return false
        if (interactionSource != other.interactionSource) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = icon.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + actionType.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
private fun SwipeToRevealComponent(
    primaryAction: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier,
    secondaryAction: SwipeToRevealAction?,
    undoPrimaryAction: SwipeToRevealAction?,
    undoSecondaryAction: SwipeToRevealAction?,
    colors: SwipeToRevealActionColors,
    shape: Shape,
    content: @Composable () -> Unit
) {
    SwipeToReveal(
        state = revealState,
        modifier = modifier,
        onFullSwipe = {
            // Full swipe triggers the main action, but does not set the click type.
            // Explicitly set the click type as main action when full swipe occurs.
            revealState.lastActionType = RevealActionType.PrimaryAction
            primaryAction.onClick()
        },
        primaryAction = {
            SwipeToRevealAction(
                revealState = revealState,
                action = primaryAction,
                backgroundColor = colors.primaryActionBackgroundColor,
                contentColor = colors.primaryActionContentColor,
                shape = shape,
            )
        },
        secondaryAction =
        secondaryAction?.let {
            {
                SwipeToRevealAction(
                    revealState = revealState,
                    action = secondaryAction,
                    backgroundColor = colors.secondaryActionBackgroundColor,
                    contentColor = colors.secondaryActionContentColor,
                    shape = shape,
                )
            }
        },
        undoAction =
        when (revealState.lastActionType) {
            RevealActionType.SecondaryAction -> undoSecondaryAction?.let {
                {
                    UndoAction(
                        revealState = revealState,
                        undoAction = undoSecondaryAction,
                        colors = colors
                    )
                }
            }
            // With manual swiping the last click action type will be none, show undo action
            RevealActionType.PrimaryAction, RevealActionType.None -> undoPrimaryAction?.let {
                {
                    UndoAction(
                        revealState = revealState,
                        undoAction = undoPrimaryAction,
                        colors = colors
                    )
                }
            }
            else -> null
        },
        content = content
    )
}

/**
 * Action composables for [SwipeToReveal].
 */
@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
private fun RevealScope.SwipeToRevealAction(
    revealState: RevealState,
    action: SwipeToRevealAction,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape,
) {
    // Change opacity of shape from 0% to 100% between 10% and 20% of the progress
    val shapeAlpha =
        if (revealOffset > 0)
            ((-revealState.offset - revealOffset * 0.1f) / (0.1f * revealOffset))
                .coerceIn(0.0f, 1.0f)
        else 1f
    Row(
        modifier = action.modifier
            .graphicsLayer { alpha = shapeAlpha }
            .background(backgroundColor, shape)
            // Limit the incoming constraints to max height
            .heightIn(min = 0.dp, max = SwipeToRevealDefaults.ActionMaxHeight)
            // Then, fill the max height based on incoming constraints
            .fillMaxSize()
            .clip(shape)
            .clickable(
                interactionSource = action.interactionSource,
                indication = rememberRipple(),
                role = Role.Button,
                onClick = {
                    revealState.lastActionType = action.actionType
                    action.onClick()
                }
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            if (action.icon != null) {
                ActionIcon(
                    revealState = revealState,
                    content = action.icon
                )
            }
            if (abs(revealState.offset) > revealOffset && action.label != null) {
                Spacer(Modifier.size(5.dp))
                action.label.invoke()
            }
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
private fun RevealScope.UndoAction(
    revealState: RevealState,
    undoAction: SwipeToRevealAction,
    colors: SwipeToRevealActionColors
) {
    Row(
        modifier = undoAction.modifier
            .clip(MaterialTheme.shapes.small)
            .defaultMinSize(minHeight = 52.dp)
            .background(color = colors.undoActionBackgroundColor)
            .padding(
                horizontal = SwipeToRevealDefaults.UndoButtonHorizontalPadding,
                vertical = SwipeToRevealDefaults.UndoButtonVerticalPadding
            )
            .clickable(
                interactionSource = undoAction.interactionSource,
                indication = rememberRipple(),
                role = Role.Button,
                onClick = {
                    revealState.lastActionType = undoAction.actionType
                    undoAction.onClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.undoActionContentColor
        ) {
            undoAction.icon?.invoke()
            Spacer(Modifier.size(5.dp))
            undoAction.label?.invoke()
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun RevealScope.ActionIcon(
    revealState: RevealState,
    content: @Composable () -> Unit
) {
    // Change opacity of icons from 0% to 100% between 50% to 75% of the progress
    val iconAlpha =
        if (revealOffset > 0)
            ((-revealState.offset - revealOffset * 0.5f) / (revealOffset * 0.25f))
                .coerceIn(0.0f, 1.0f)
        else 1f
    // Scale icons from 50% to 100% between 50% and 100% of the progress
    val iconScale =
        if (revealOffset > 0)
            ((-revealState.offset - revealOffset * 0.5f) / revealOffset)
                .coerceIn(0.0f, 0.5f) + 0.5f
        else 1f
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = iconAlpha
            scaleX = iconScale
            scaleY = iconScale
        }
    ) {
        content()
    }
}
