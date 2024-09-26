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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealScope
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.SwipeToReveal
import kotlin.math.abs

/**
 * [SwipeToReveal] Material composable for [Chip]s. This adds the option to configure up to two
 * additional actions on the [Chip]: a mandatory [primaryAction] and an optional [secondaryAction].
 * These actions are initially hidden and revealed only when the [content] is swiped. These
 * additional actions can be triggered by clicking on them after they are revealed. It is
 * recommended to trigger [primaryAction] on full swipe of the [content].
 *
 * For actions like "Delete", consider adding [undoPrimaryAction] (displayed when the
 * [primaryAction] is activated) and/or [undoSecondaryAction] (displayed when the [secondaryAction]
 * is activated). Adding undo composables allow users to undo the action that they just performed.
 *
 * Example of [SwipeToRevealChip] with primary and secondary actions
 *
 * @sample androidx.wear.compose.material.samples.SwipeToRevealChipSample
 * @param primaryAction A composable to describe the primary action when swiping. The action will be
 *   triggered on clicking the action. See [SwipeToRevealPrimaryAction].
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param onFullSwipe A lambda which will be triggered on full swipe from either of the anchors. We
 *   recommend to keep this similar to primary action click action. This sets the
 *   [RevealState.lastActionType] to [RevealActionType.PrimaryAction].
 * @param modifier [Modifier] to be applied on the composable
 * @param secondaryAction A composable to describe the contents of secondary action. The action will
 *   be triggered on clicking the action. See [SwipeToRevealSecondaryAction]
 * @param undoPrimaryAction A composable to describe the contents of undo action when the primary
 *   action was triggered. See [SwipeToRevealUndoAction]
 * @param undoSecondaryAction composable to describe the contents of undo action when secondary
 *   action was triggered. See [SwipeToRevealUndoAction]
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 *   [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of primary and secondary action composables. Recommended shape for chips
 *   is [Shapes.small].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeToRevealChip(
    primaryAction: @Composable RevealScope.() -> Unit,
    revealState: RevealState,
    onFullSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: @Composable (RevealScope.() -> Unit)? = null,
    undoPrimaryAction: @Composable (RevealScope.() -> Unit)? = null,
    undoSecondaryAction: @Composable (RevealScope.() -> Unit)? = null,
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
        onFullSwipe = onFullSwipe,
        content = content
    )
}

/**
 * [SwipeToReveal] Material composable for [Card]s. This adds the option to configure up to two
 * additional actions on the [Card]: a mandatory [primaryAction] and an optional [secondaryAction].
 * These actions are initially hidden and revealed only when the [content] is swiped. These
 * additional actions can be triggered by clicking on them after they are revealed. It is
 * recommended to trigger [primaryAction] on full swipe of the [content].
 *
 * For actions like "Delete", consider adding [undoPrimaryAction] (displayed when the
 * [primaryAction] is activated) and/or [undoSecondaryAction] (displayed when the [secondaryAction]
 * is activated). Adding undo composables allow users to undo the action that they just performed.
 *
 * Example of [SwipeToRevealCard] with primary and secondary actions
 *
 * @sample androidx.wear.compose.material.samples.SwipeToRevealCardSample
 * @param primaryAction A composable to describe the primary action when swiping. The action will be
 *   triggered on clicking the action. See [SwipeToRevealPrimaryAction].
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param onFullSwipe A lambda which will be triggered on full swipe from either of the anchors. We
 *   recommend to keep this similar to primary action click action. This sets the
 *   [RevealState.lastActionType] to [RevealActionType.PrimaryAction].
 * @param modifier [Modifier] to be applied on the composable
 * @param secondaryAction A composable to describe the contents of secondary action.The action will
 *   be triggered on clicking the action. See [SwipeToRevealSecondaryAction]
 * @param undoPrimaryAction A composable to describe the contents of undo action when the primary
 *   action was triggered. See [SwipeToRevealUndoAction]
 * @param undoSecondaryAction A composable to describe the contents of undo action when secondary
 *   action was triggered. See [SwipeToRevealUndoAction]
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 *   [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of primary and secondary action composables. Recommended shape for cards
 *   is [SwipeToRevealDefaults.CardActionShape].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeToRevealCard(
    primaryAction: @Composable RevealScope.() -> Unit,
    revealState: RevealState,
    onFullSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: @Composable (RevealScope.() -> Unit)? = null,
    undoPrimaryAction: @Composable (RevealScope.() -> Unit)? = null,
    undoSecondaryAction: @Composable (RevealScope.() -> Unit)? = null,
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
        onFullSwipe = onFullSwipe,
        content = content
    )
}

/**
 * A composable which can be used for setting the primary action of material [SwipeToRevealCard] and
 * [SwipeToRevealChip].
 *
 * @param revealState The [RevealState] of the [SwipeToReveal] where this action is used.
 * @param onClick A lambda which gets triggered when the action is clicked.
 * @param icon The icon which will be displayed initially on the action
 * @param label The label which will be displayed on the expanded action
 * @param modifier [Modifier] to be applied on the action
 * @param interactionSource The [MutableInteractionSource] representing the stream of interactions
 *   with this action.
 */
@ExperimentalWearMaterialApi
@Composable
public fun RevealScope.SwipeToRevealPrimaryAction(
    revealState: RevealState,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) =
    ActionCommon(
        revealState = revealState,
        actionType = RevealActionType.PrimaryAction,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        icon = icon,
        label = label
    )

/**
 * A composable which can be used for setting the secondary action of material [SwipeToRevealCard]
 * and [SwipeToRevealChip].
 *
 * @param revealState The [RevealState] of the [SwipeToReveal] where this action is used.
 * @param onClick A lambda which gets triggered when the action is clicked.
 * @param modifier [Modifier] to be applied on the action
 * @param interactionSource The [MutableInteractionSource] representing the stream of interactions
 *   with this action.
 * @param content The composable which will be displayed on the action. It is recommended to keep
 *   this content as an [Icon] composable.
 */
@ExperimentalWearMaterialApi
@Composable
public fun RevealScope.SwipeToRevealSecondaryAction(
    revealState: RevealState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) =
    ActionCommon(
        revealState = revealState,
        actionType = RevealActionType.SecondaryAction,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        icon = content,
        label = null
    )

/**
 * A composable which can be used for setting the undo action of material [SwipeToRevealCard] and
 * [SwipeToRevealChip].
 *
 * @param revealState The [RevealState] of the [SwipeToReveal] where this action is used.
 * @param onClick A lambda which gets triggered when the action is clicked.
 * @param modifier [Modifier] to be applied on the action
 * @param interactionSource The [MutableInteractionSource] representing the stream of interactions
 *   with this action.
 * @param icon An optional icon which will be displayed on the action
 * @param label An optional label which will be displayed on the action. We strongly recommend to
 *   set [icon] and/or [label] for the action.
 */
@ExperimentalWearMaterialApi
@Composable
public fun RevealScope.SwipeToRevealUndoAction(
    revealState: RevealState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    icon: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = {
                    revealState.lastActionType = RevealActionType.UndoAction
                    onClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke()
        Spacer(Modifier.size(5.dp))
        label?.invoke()
    }
}

/** Defaults for Material [SwipeToReveal]. */
@ExperimentalWearMaterialApi
public object SwipeToRevealDefaults {
    /** Recommended shape for [SwipeToReveal] actions when used with [Card]. */
    public val CardActionShape = RoundedCornerShape(40.dp)

    /**
     * The recommended colors used to display the contents of the primary, secondary and undo
     * actions in [SwipeToReveal].
     *
     * @param primaryActionBackgroundColor The background color (color of the shape) of the primary
     *   action
     * @param primaryActionContentColor The content color (text and icon) of the primary action
     * @param secondaryActionBackgroundColor The background color (color of the shape) of the
     *   secondary action
     * @param secondaryActionContentColor The content color (text and icon) of the secondary action
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

    /** [ImageVector] for delete icon, often used for the primary action. */
    public val Delete = Icons.Outlined.Delete

    /** [ImageVector] for more options icon, often used for the secondary action. */
    public val MoreOptions = Icons.Outlined.MoreVert

    internal val UndoButtonHorizontalPadding = 14.dp
    internal val UndoButtonVerticalPadding = 6.dp
    internal val ActionMaxHeight = 84.dp
}

/**
 * A class representing the colors applied in [SwipeToReveal] actions. See
 * [SwipeToRevealDefaults.actionColors].
 *
 * @param primaryActionBackgroundColor Color of the shape (background) of primary action
 * @param primaryActionContentColor Color of icon or text used in the primary action
 * @param secondaryActionBackgroundColor Color of the secondary action shape (background)
 * @param secondaryActionContentColor Color of the icon or text used in the secondary action
 * @param undoActionBackgroundColor Color of the undo action shape (background)
 * @param undoActionContentColor Color of the icon or text used in the undo action
 */
@ExperimentalWearMaterialApi
public class SwipeToRevealActionColors
constructor(
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

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealComponent(
    primaryAction: @Composable RevealScope.() -> Unit,
    revealState: RevealState,
    modifier: Modifier,
    secondaryAction: @Composable (RevealScope.() -> Unit)?,
    undoPrimaryAction: @Composable (RevealScope.() -> Unit)?,
    undoSecondaryAction: @Composable (RevealScope.() -> Unit)?,
    colors: SwipeToRevealActionColors,
    shape: Shape,
    onFullSwipe: () -> Unit,
    content: @Composable () -> Unit
) {
    SwipeToReveal(
        state = revealState,
        modifier = modifier,
        onFullSwipe = {
            // Full swipe triggers the main action, but does not set the click type.
            // Explicitly set the click type as main action when full swipe occurs.
            revealState.lastActionType = RevealActionType.PrimaryAction
            onFullSwipe()
        },
        primaryAction = {
            ActionWrapper(
                revealState = revealState,
                backgroundColor = colors.primaryActionBackgroundColor,
                contentColor = colors.primaryActionContentColor,
                shape = shape,
                content = primaryAction
            )
        },
        secondaryAction =
            secondaryAction?.let {
                {
                    ActionWrapper(
                        revealState = revealState,
                        backgroundColor = colors.secondaryActionBackgroundColor,
                        contentColor = colors.secondaryActionContentColor,
                        shape = shape,
                        content = secondaryAction
                    )
                }
            },
        undoAction =
            when (revealState.lastActionType) {
                RevealActionType.SecondaryAction ->
                    undoSecondaryAction?.let {
                        { UndoActionWrapper(colors = colors, content = undoSecondaryAction) }
                    }
                // With manual swiping the last click action type will be none, show undo action
                RevealActionType.PrimaryAction,
                RevealActionType.None ->
                    undoPrimaryAction?.let {
                        { UndoActionWrapper(colors = colors, content = undoPrimaryAction) }
                    }
                else -> null
            },
        content = content
    )
}

/** Action composables for [SwipeToReveal]. */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun RevealScope.ActionWrapper(
    revealState: RevealState,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape,
    content: @Composable RevealScope.() -> Unit,
) {
    // Change opacity of shape from 0% to 100% between 10% and 20% of the progress
    val shapeAlpha =
        if (revealOffset > 0)
            ((-revealState.offset - revealOffset * 0.1f) / (0.1f * revealOffset)).coerceIn(
                0.0f,
                1.0f
            )
        else 1f
    Box(
        modifier =
            Modifier.graphicsLayer { alpha = shapeAlpha }
                .background(backgroundColor, shape)
                // Limit the incoming constraints to max height
                .heightIn(min = 0.dp, max = SwipeToRevealDefaults.ActionMaxHeight)
                // Then, fill the max height based on incoming constraints
                .fillMaxSize()
                .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun RevealScope.UndoActionWrapper(
    colors: SwipeToRevealActionColors,
    content: @Composable RevealScope.() -> Unit
) {
    Box(
        modifier =
            Modifier.clip(MaterialTheme.shapes.small)
                .defaultMinSize(minHeight = 52.dp)
                .background(color = colors.undoActionBackgroundColor)
                .padding(
                    horizontal = SwipeToRevealDefaults.UndoButtonHorizontalPadding,
                    vertical = SwipeToRevealDefaults.UndoButtonVerticalPadding
                ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.undoActionContentColor) {
            content()
        }
    }
}

@Composable
private fun RevealScope.ActionCommon(
    revealState: RevealState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionType: RevealActionType = RevealActionType.UndoAction,
    interactionSource: MutableInteractionSource? = null,
    icon: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = {
                        revealState.lastActionType = actionType
                        onClick()
                    }
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            ActionIcon(revealState = revealState, content = icon)
        }
        if (label != null) {
            ActionLabel(revealState = revealState, content = label)
        }
    }
}

@Composable
private fun RevealScope.ActionIcon(revealState: RevealState, content: @Composable () -> Unit) {
    // Change opacity of icons from 0% to 100% between 50% to 75% of the progress
    val iconAlpha =
        if (revealOffset > 0)
            ((-revealState.offset - revealOffset * 0.5f) / (revealOffset * 0.25f)).coerceIn(
                0.0f,
                1.0f
            )
        else 1f
    // Scale icons from 70% to 100% between 50% and 100% of the progress
    val iconScale =
        if (revealOffset > 0)
            lerp(
                start = 0.7f,
                stop = 1.0f,
                fraction = (-revealState.offset - revealOffset * 0.5f) / revealOffset + 0.5f
            )
        else 1f
    Box(
        modifier =
            Modifier.graphicsLayer {
                alpha = iconAlpha
                scaleX = iconScale
                scaleY = iconScale
            }
    ) {
        content()
    }
}

@Composable
private fun RevealScope.ActionLabel(revealState: RevealState, content: @Composable () -> Unit) {
    val labelAlpha =
        animateFloatAsState(
            targetValue = if (abs(revealState.offset) > revealOffset) 1f else 0f,
            animationSpec = tween(durationMillis = RAPID, delayMillis = RAPID),
            label = "ActionLabelAlpha"
        )
    AnimatedVisibility(
        visible = abs(revealState.offset) > revealOffset,
        enter = expandHorizontally(animationSpec = tween(durationMillis = RAPID)),
        exit = ExitTransition.None
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha = labelAlpha.value }) {
            Spacer(Modifier.size(5.dp))
            content.invoke()
        }
    }
}
