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

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material.SwipeToRevealDefaults.RevealingRatio
import androidx.wear.compose.material.SwipeToRevealDefaults.createRevealAnchors
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import androidx.wear.compose.materialcore.SwipeableV2State
import androidx.wear.compose.materialcore.swipeAnchors
import androidx.wear.compose.materialcore.swipeableV2
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
 * @param content The initial content shown prior to the swipe-to-reveal gesture. Custom
 *   accessibility actions should always be added to the content using [Modifier.semantics] -
 *   examples are shown in the code samples.
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeToRevealChip(
    primaryAction: @Composable () -> Unit,
    revealState: RevealState,
    onFullSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: @Composable (() -> Unit)? = null,
    undoPrimaryAction: @Composable (() -> Unit)? = null,
    undoSecondaryAction: @Composable (() -> Unit)? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable () -> Unit,
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
        content = content,
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
 * @param content The initial content shown prior to the swipe-to-reveal gesture. Custom
 *   accessibility actions should always be added to the content using [Modifier.semantics] -
 *   examples are shown in the code samples.
 * @see [SwipeToReveal]
 */
@ExperimentalWearMaterialApi
@Composable
public fun SwipeToRevealCard(
    primaryAction: @Composable () -> Unit,
    revealState: RevealState,
    onFullSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: @Composable (() -> Unit)? = null,
    undoPrimaryAction: @Composable (() -> Unit)? = null,
    undoSecondaryAction: @Composable (() -> Unit)? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = SwipeToRevealDefaults.CardActionShape,
    content: @Composable () -> Unit,
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
        content = content,
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
public fun SwipeToRevealPrimaryAction(
    revealState: RevealState,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
): Unit =
    ActionCommon(
        revealState = revealState,
        actionType = RevealActionType.PrimaryAction,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        icon = icon,
        label = label,
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
public fun SwipeToRevealSecondaryAction(
    revealState: RevealState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
): Unit =
    ActionCommon(
        revealState = revealState,
        actionType = RevealActionType.SecondaryAction,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        icon = content,
        label = null,
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
public fun SwipeToRevealUndoAction(
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
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
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
    public val CardActionShape: RoundedCornerShape = RoundedCornerShape(40.dp)

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
        undoActionContentColor: Color = MaterialTheme.colors.onSurface,
    ): SwipeToRevealActionColors {
        return SwipeToRevealActionColors(
            primaryActionBackgroundColor = primaryActionBackgroundColor,
            primaryActionContentColor = primaryActionContentColor,
            secondaryActionBackgroundColor = secondaryActionBackgroundColor,
            secondaryActionContentColor = secondaryActionContentColor,
            undoActionBackgroundColor = undoActionBackgroundColor,
            undoActionContentColor = undoActionContentColor,
        )
    }

    /** [ImageVector] for delete icon, often used for the primary action. */
    public val Delete: ImageVector = Icons.Outlined.Delete

    /** [ImageVector] for more options icon, often used for the secondary action. */
    public val MoreOptions: ImageVector = Icons.Outlined.MoreVert

    /** Default animation spec used when moving between states. */
    internal val AnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = RAPID_ANIMATION, easing = FastOutSlowInEasing)

    /** Default padding space between action slots. */
    internal val Padding = 4.dp

    /**
     * Default ratio of the content displayed when in [RevealValue.RightRevealing] state, i.e. all
     * the actions are revealed and the top content is not being swiped. For example, a value of 0.7
     * means that 70% of the width is used to place the actions.
     */
    public val RevealingRatio: Float = 0.7f

    /**
     * Default position threshold that needs to be swiped in order to transition to the next state.
     * Used in conjunction with [RevealingRatio]; for example, a threshold of 0.5 with a revealing
     * ratio of 0.7 means that the user needs to swipe at least 35% (0.5 * 0.7) of the component
     * width to go from [RevealValue.Covered] to [RevealValue.RightRevealing] and at least 85%
     * (0.7 + 0.5 * (1 - 0.7)) of the component width to go from [RevealValue.RightRevealing] to
     * [RevealValue.RightRevealed].
     */
    public val PositionalThreshold: (totalDistance: Float) -> Float = { totalDistance: Float ->
        totalDistance * 0.5f
    }

    /**
     * Creates the required anchors to which the top content can be swiped, to reveal the actions.
     * Each value should be in the range [0..1], where 0 represents right most end and 1 represents
     * the full width of the top content starting from right and ending on left.
     *
     * @param coveredAnchor Anchor for the [RevealValue.Covered] value
     * @param revealingAnchor Anchor for the [RevealValue.LeftRevealing] or
     *   [RevealValue.RightRevealing] value
     * @param revealedAnchor Anchor for the [RevealValue.LeftRevealed] or
     *   [RevealValue.RightRevealed] value
     * @param revealDirection The direction in which the content can be swiped. It's strongly
     *   advised to keep the default [RevealDirection.RightToLeft] in order to preserve
     *   compatibility with the system wide swipe to dismiss gesture.
     */
    @SuppressWarnings("PrimitiveInCollection")
    public fun createRevealAnchors(
        coveredAnchor: Float = 0f,
        revealingAnchor: Float = RevealingRatio,
        revealedAnchor: Float = 1f,
        revealDirection: RevealDirection = RevealDirection.RightToLeft,
    ): Map<RevealValue, Float> {
        if (revealDirection == RevealDirection.Both) {
            return mapOf(
                RevealValue.LeftRevealed to -revealedAnchor,
                RevealValue.LeftRevealing to -revealingAnchor,
                RevealValue.Covered to coveredAnchor,
                RevealValue.RightRevealing to revealingAnchor,
                RevealValue.RightRevealed to revealedAnchor,
            )
        }
        return mapOf(
            RevealValue.Covered to coveredAnchor,
            RevealValue.RightRevealing to revealingAnchor,
            RevealValue.RightRevealed to revealedAnchor,
        )
    }

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
    public val primaryActionBackgroundColor: Color,
    public val primaryActionContentColor: Color,
    public val secondaryActionBackgroundColor: Color,
    public val secondaryActionContentColor: Color,
    public val undoActionBackgroundColor: Color,
    public val undoActionContentColor: Color,
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
    primaryAction: @Composable () -> Unit,
    revealState: RevealState,
    modifier: Modifier,
    secondaryAction: @Composable (() -> Unit)?,
    undoPrimaryAction: @Composable (() -> Unit)?,
    undoSecondaryAction: @Composable (() -> Unit)?,
    colors: SwipeToRevealActionColors,
    shape: Shape,
    onFullSwipe: () -> Unit,
    content: @Composable () -> Unit,
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
                content = primaryAction,
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
                        content = secondaryAction,
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
        content = content,
    )
}

/** Action composables for [SwipeToReveal]. */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun ActionWrapper(
    revealState: RevealState,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    // Change opacity of shape from 0% to 100% between 10% and 20% of the progress
    val shapeAlpha =
        if (revealState.revealThreshold > 0)
            ((abs(revealState.offset) - revealState.revealThreshold * 0.1f) /
                    (0.1f * revealState.revealThreshold))
                .coerceIn(0.0f, 1.0f)
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
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun UndoActionWrapper(colors: SwipeToRevealActionColors, content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier.clip(MaterialTheme.shapes.small)
                .defaultMinSize(minHeight = 52.dp)
                .background(color = colors.undoActionBackgroundColor)
                .padding(
                    horizontal = SwipeToRevealDefaults.UndoButtonHorizontalPadding,
                    vertical = SwipeToRevealDefaults.UndoButtonVerticalPadding,
                ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.undoActionContentColor,
            content = content,
        )
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun ActionCommon(
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
                    },
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ActionIcon(revealState = revealState, content = icon)
        }
        if (label != null) {
            ActionLabel(revealState = revealState, content = label)
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun ActionIcon(revealState: RevealState, content: @Composable () -> Unit) {
    // Change opacity of icons from 0% to 100% between 50% to 75% of the progress
    val iconAlpha =
        if (revealState.revealThreshold > 0)
            ((abs(revealState.offset) - revealState.revealThreshold * 0.5f) /
                    (revealState.revealThreshold * 0.25f))
                .coerceIn(0.0f, 1.0f)
        else 1f
    // Scale icons from 70% to 100% between 50% and 100% of the progress
    val iconScale =
        if (revealState.revealThreshold > 0)
            lerp(
                start = 0.7f,
                stop = 1.0f,
                fraction =
                    ((abs(revealState.offset) - revealState.revealThreshold * 0.5f) /
                            (revealState.revealThreshold * 0.5f))
                        .coerceIn(0.0f, 1.0f),
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

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun ActionLabel(revealState: RevealState, content: @Composable () -> Unit) {
    val labelAlpha =
        animateFloatAsState(
            targetValue = if (abs(revealState.offset) > revealState.revealThreshold) 1f else 0f,
            animationSpec = tween(durationMillis = RAPID, delayMillis = RAPID),
            label = "ActionLabelAlpha",
        )
    AnimatedVisibility(
        visible = abs(revealState.offset) > revealState.revealThreshold,
        enter = expandHorizontally(animationSpec = tween(durationMillis = RAPID)),
        exit = ExitTransition.None,
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha = labelAlpha.value }) {
            Spacer(Modifier.size(5.dp))
            content.invoke()
        }
    }
}

/**
 * Different values that determine the state of the [SwipeToReveal] composable, reflected in
 * [RevealState.currentValue]. [RevealValue.Covered] is considered the default state where none of
 * the actions are revealed yet.
 *
 * [SwipeToReveal] direction is not localised, with the default being [RevealDirection.RightToLeft],
 * and [RevealValue.RightRevealing] and [RevealValue.RightRevealed] correspond to the actions
 * getting revealed from the right side of the screen. In case swipe direction is set to
 * [RevealDirection.Both], actions can also get revealed from the left side of the screen, and in
 * that case [RevealValue.LeftRevealing] and [RevealValue.LeftRevealed] are used.
 *
 * @see [RevealDirection]
 */
@ExperimentalWearMaterialApi
@JvmInline
public value class RevealValue private constructor(public val value: Int) {
    public companion object {
        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and they are displayed on the left side of the screen. This also represents the
         * state in which one of the actions has been triggered/performed.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Both], and the user
         * swipes from the left side of the screen.
         */
        public val LeftRevealed: RevealValue = RevealValue(-2)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the left side of the screen.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Both], and the user
         * swipes from the left side of the screen.
         */
        public val LeftRevealing: RevealValue = RevealValue(-1)

        /**
         * The default first value which generally represents the state where the revealable actions
         * have not been revealed yet. In this state, none of the actions have been triggered or
         * performed yet.
         */
        public val Covered: RevealValue = RevealValue(0)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the right side of the screen.
         */
        public val RightRevealing: RevealValue = RevealValue(1)

        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and the actions are revealed on the right side of the screen. This also
         * represents the state in which one of the actions has been triggered/performed.
         */
        public val RightRevealed: RevealValue = RevealValue(2)
    }
}

/**
 * Different values [SwipeToReveal] composable can reveal the actions from.
 *
 * [RevealDirection] is not localised, with the default being [RevealDirection.RightToLeft] to
 * prevent conflict with the system-wide swipe to dismiss gesture in an activity, so it's strongly
 * advised to respect the default value to avoid conflicting gestures.
 */
@ExperimentalWearMaterialApi
@JvmInline
public value class RevealDirection private constructor(public val value: Int) {
    public companion object {
        /**
         * The default value which allows the user to swipe right to left to reveal or execute the
         * actions. It's strongly advised to respect the default behavior to avoid conflict with the
         * swipe-to-dismiss gesture.
         */
        public val RightToLeft: RevealDirection = RevealDirection(0)

        /**
         * The value which allows the user to swipe in either direction to reveal or execute the
         * actions. This should not be used if the component is used in an activity as the gesture
         * might conflict with the swipe-to-dismiss gesture and could be confusing for the users.
         * This is only supported for rare cases where the current screen does not support swipe to
         * dismiss.
         */
        public val Both: RevealDirection = RevealDirection(1)
    }
}

/**
 * Different values which can trigger the state change from one [RevealValue] to another. These are
 * not set by themselves and need to be set appropriately with [RevealState.snapTo] and
 * [RevealState.animateTo].
 */
@ExperimentalWearMaterialApi
@JvmInline
public value class RevealActionType private constructor(public val value: Int) {
    public companion object {
        /**
         * Represents the primary action composable of [SwipeToReveal]. This corresponds to the
         * mandatory `primaryAction` parameter of [SwipeToReveal].
         */
        public val PrimaryAction: RevealActionType = RevealActionType(0)

        /**
         * Represents the secondary action composable of [SwipeToReveal]. This corresponds to the
         * optional `secondaryAction` composable of [SwipeToReveal].
         */
        public val SecondaryAction: RevealActionType = RevealActionType(1)

        /**
         * Represents the undo action composable of [SwipeToReveal]. This corresponds to the
         * `undoAction` composable of [SwipeToReveal] which is shown once an action is performed.
         */
        public val UndoAction: RevealActionType = RevealActionType(2)

        /** Default value when none of the above are applicable. */
        public val None: RevealActionType = RevealActionType(-1)
    }
}

/**
 * A class to keep track of the state of the composable. It can be used to customise the behavior
 * and state of the composable.
 *
 * @constructor Create a [RevealState].
 */
@SuppressLint("PrimitiveInCollection")
@ExperimentalWearMaterialApi
public class RevealState
internal constructor(
    initialValue: RevealValue,
    animationSpec: AnimationSpec<Float>,
    confirmValueChange: (RevealValue) -> Boolean,
    positionalThreshold: (totalDistance: Float) -> Float,
    internal val anchors: Map<RevealValue, Float>,
    internal val coroutineScope: CoroutineScope,
    internal val nestedScrollDispatcher: NestedScrollDispatcher,
) {
    /** [SwipeableV2State] internal instance for the state. */
    internal val swipeableState =
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = { revealValue ->
                confirmValueChangeAndReset(confirmValueChange, revealValue)
            },
            positionalThreshold = { totalDistance -> positionalThreshold(totalDistance) },
            nestedScrollDispatcher = nestedScrollDispatcher,
        )

    public var lastActionType: RevealActionType by mutableStateOf(RevealActionType.None)

    /**
     * The current [RevealValue] based on the status of the component.
     *
     * @see Modifier.swipeableV2
     */
    public val currentValue: RevealValue
        get() = swipeableState.currentValue

    /**
     * The target [RevealValue] based on the status of the component. This will be equal to the
     * [currentValue] if there is no animation running or swiping has stopped. Otherwise, this
     * returns the next [RevealValue] based on the animation/swipe direction.
     *
     * @see Modifier.swipeableV2
     */
    public val targetValue: RevealValue
        get() = swipeableState.targetValue

    /**
     * Returns whether the animation is running or not.
     *
     * @see Modifier.swipeableV2
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    /**
     * The current amount by which the revealable content has been revealed by.
     *
     * @see Modifier.swipeableV2
     */
    public val offset: Float
        get() = swipeableState.offset ?: 0f

    /**
     * Defines the anchors for revealable content. These anchors are used to determine the width at
     * which the revealable content can be revealed to and stopped without requiring any input from
     * the user.
     *
     * @see Modifier.swipeableV2
     */
    public val swipeAnchors: Map<RevealValue, Float>
        get() = anchors

    /**
     * The threshold, in pixels, where the revealed actions are fully visible but the existing
     * content would be left in place if the reveal action was stopped. This threshold is used to
     * create the anchor for [RevealValue.RightRevealing]. If there is no such anchor defined for
     * [RevealValue.RightRevealing], it returns 0.0f.
     */
    /* @FloatRange(from = 0.0) */
    public val revealThreshold: Float
        get() = width.floatValue * (swipeAnchors[RevealValue.RightRevealing] ?: 0.0f)

    /**
     * The total width of the component in pixels. Initialise to zero, updated when the width
     * changes.
     */
    public val width: MutableFloatState = mutableFloatStateOf(0.0f)

    /**
     * Snaps to the [targetValue] without any animation.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will be changed to.
     * @see Modifier.swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        swipeableState.snapTo(targetValue)
    }

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animate to.
     * @throws IllegalStateException if the target [RevealValue] is not valid for current
     *   [RevealState] instance.
     */
    public suspend fun animateTo(targetValue: RevealValue) {
        checkNotNull(anchors[targetValue]) {
            "The RevealValue you're targeting isn't supported by current RevealState instance. " +
                "Ensure the RevealState was created with an anchor map that contains " +
                "the target RevealValue."
        }
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        try {
            swipeableState.animateTo(targetValue)
        } finally {
            if (targetValue == RevealValue.Covered) {
                lastActionType = RevealActionType.None
            }
        }
    }

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()

    private fun confirmValueChangeAndReset(
        confirmValueChange: Predicate<RevealValue>,
        revealValue: RevealValue,
    ): Boolean {
        val canChangeValue = confirmValueChange.test(revealValue)
        val currentState = this
        // Update the state if the reveal value is changing to a different value than Covered.
        if (canChangeValue && revealValue != RevealValue.Covered) {
            coroutineScope.launch { resetLastState(currentState) }
        }
        return canChangeValue
    }

    /**
     * Resets last state if a different SwipeToReveal is being moved to new anchor and the last
     * state is in either [RevealValue.RightRevealing] or [RevealValue.LeftRevealing] mode which
     * represents no action has been performed yet. In [RevealValue.RightRevealed] or
     * [RevealValue.LeftRevealed], the action has been performed and it will not be reset.
     */
    private suspend fun resetLastState(currentState: RevealState) {
        val oldState = SingleSwipeCoordinator.lastUpdatedState.getAndSet(currentState)
        if (
            currentState != oldState &&
                (oldState?.currentValue == RevealValue.RightRevealing ||
                    oldState?.currentValue == RevealValue.LeftRevealing)
        ) {
            oldState.animateTo(RevealValue.Covered)
        }
    }

    /** A singleton instance to keep track of the [RevealState] which was modified the last time. */
    private object SingleSwipeCoordinator {
        var lastUpdatedState: AtomicReference<RevealState?> = AtomicReference(null)
    }
}

/**
 * Create and [remember] a [RevealState].
 *
 * @param initialValue The initial value of the [RevealValue].
 * @param animationSpec The animation which will be applied on the top content.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 *   while the reveal is in progress and when settling after the revealing ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param anchors A map of [RevealValue] to the fraction where the content can be revealed to reach
 *   that value. Each anchor should be between [0..1] which will be adjusted based on total width.
 */
@SuppressLint("PrimitiveInCollection")
@ExperimentalWearMaterialApi
@Composable
public fun rememberRevealState(
    initialValue: RevealValue = RevealValue.Covered,
    animationSpec: AnimationSpec<Float> = SwipeToRevealDefaults.AnimationSpec,
    confirmValueChange: (RevealValue) -> Boolean = { true },
    positionalThreshold: (totalDistance: Float) -> Float =
        SwipeToRevealDefaults.PositionalThreshold,
    anchors: Map<RevealValue, Float> = createRevealAnchors(),
): RevealState {
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    return remember(initialValue, animationSpec) {
        RevealState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            anchors = anchors,
            coroutineScope = coroutineScope,
            nestedScrollDispatcher = nestedScrollDispatcher,
        )
    }
}

/**
 * A composable that can be used to add extra actions to a composable (up to two) which will be
 * revealed when the original composable is swiped to the left. This composable requires a primary
 * swipe/click action, a secondary optional click action can also be provided.
 *
 * When the composable reaches the state where all the actions are revealed and the swipe continues
 * beyond the positional threshold defined in [RevealState], the primary action is automatically
 * triggered.
 *
 * An optional undo action can also be added. This undo action will be visible to users once the
 * [RevealValue] becomes [RevealValue.RightRevealed].
 *
 * It is strongly recommended to have icons represent the actions and maybe a text and icon for the
 * undo action.
 *
 * @param primaryAction The primary action that will be triggered in the event of a completed swipe.
 *   We also strongly recommend to trigger the action when it is clicked.
 * @param modifier Optional [Modifier] for this component.
 * @param onFullSwipe An optional lambda which will be triggered when a full swipe from either of
 *   the anchors is performed.
 * @param state The [RevealState] of this component. It can be used to customise the anchors and
 *   threshold config of the swipeable modifier which is applied.
 * @param secondaryAction An optional action that can be added to the component. We strongly
 *   recommend triggering the action when it is clicked.
 * @param undoAction The optional undo action that will be applied to the component once the
 *   [RevealState.currentValue] becomes [RevealValue.RightRevealed].
 * @param content The content that will be initially displayed over the other actions provided.
 */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
internal fun SwipeToReveal(
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onFullSwipe: () -> Unit = {},
    state: RevealState = rememberRevealState(),
    secondaryAction: (@Composable () -> Unit)? = null,
    undoAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // A no-op NestedScrollConnection which does not consume scroll/fling events
    val noOpNestedScrollConnection = remember { object : NestedScrollConnection {} }

    var globalPosition by remember { mutableStateOf<LayoutCoordinates?>(null) }

    CustomTouchSlopProvider(
        newTouchSlop = LocalViewConfiguration.current.touchSlop * CustomTouchSlopMultiplier
    ) {
        Box(
            modifier =
                modifier
                    .onGloballyPositioned { layoutCoordinates ->
                        globalPosition = layoutCoordinates
                    }
                    .swipeableV2(
                        state = state.swipeableState,
                        orientation = Orientation.Horizontal,
                        enabled =
                            state.currentValue != RevealValue.LeftRevealed &&
                                state.currentValue != RevealValue.RightRevealed,
                    )
                    .swipeAnchors(
                        state = state.swipeableState,
                        possibleValues = state.swipeAnchors.keys,
                    ) { value, layoutSize ->
                        val swipeableWidth = layoutSize.width.toFloat()
                        // Update the total width which will be used to calculate the anchors
                        state.width.floatValue = swipeableWidth
                        // Multiply the anchor with -1f to get the actual swipeable anchor
                        -state.swipeAnchors[value]!! * swipeableWidth
                    }
                    // NestedScrollDispatcher sends the scroll/fling events from the node to its
                    // parent
                    // and onwards including the modifier chain. Apply it in the end to let nested
                    // scroll
                    // connection applied before this modifier consume the scroll/fling events.
                    .nestedScroll(noOpNestedScrollConnection, state.nestedScrollDispatcher)
        ) {
            val swipeCompleted =
                state.currentValue == RevealValue.RightRevealed ||
                    state.currentValue == RevealValue.LeftRevealed
            val lastActionIsSecondary = state.lastActionType == RevealActionType.SecondaryAction
            val isWithinRevealOffset by remember {
                derivedStateOf { abs(state.offset) <= state.revealThreshold }
            }
            val canSwipeRight =
                (state.swipeAnchors.minOfOrNull { (_, offset) -> offset } ?: 0f) < 0f

            // Determines whether the secondary action will be visible based on the current
            // reveal offset
            val showSecondaryAction = isWithinRevealOffset || lastActionIsSecondary

            // Determines whether both primary and secondary action should be hidden, usually the
            // case
            // when secondary action is clicked
            val hideActions = !isWithinRevealOffset && lastActionIsSecondary

            val swipingRight by remember { derivedStateOf { state.offset > 0 } }

            // Don't draw actions on the left side if the user cannot swipe right, and they are
            // currently swiping right
            val shouldDrawActions by remember {
                derivedStateOf { abs(state.offset) > 0 && (canSwipeRight || !swipingRight) }
            }

            // Draw the buttons only when offset is greater than zero.
            if (shouldDrawActions) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment =
                        if (swipingRight) AbsoluteAlignment.CenterLeft
                        else AbsoluteAlignment.CenterRight,
                ) {
                    AnimatedContent(
                        targetState = swipeCompleted && undoAction != null,
                        transitionSpec = {
                            if (targetState) { // Fade in the Undo composable and fade out actions
                                fadeInUndo()
                            } else { // Fade in the actions and fade out the undo composable
                                fadeOutUndo()
                            }
                        },
                        label = "AnimatedContentS2R",
                    ) { displayUndo ->
                        if (displayUndo && undoAction != null) {
                            val undoActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 1f else 0f,
                                    animationSpec =
                                        tween(
                                            durationMillis = RAPID_ANIMATION,
                                            delayMillis = FLASH_ANIMATION,
                                            easing = STANDARD_IN_OUT,
                                        ),
                                    label = "UndoActionAlpha",
                                )
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = undoActionAlpha.value }
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                ActionSlot(content = undoAction)
                            }
                        } else {
                            // Animate weight for secondary action slot.
                            val secondaryActionWeight =
                                animateFloatAsState(
                                    targetValue = if (showSecondaryAction) 1f else 0f,
                                    animationSpec = tween(durationMillis = QUICK_ANIMATION),
                                    label = "SecondaryActionAnimationSpec",
                                )
                            val secondaryActionAlpha =
                                animateFloatAsState(
                                    targetValue =
                                        if (!showSecondaryAction || hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = QUICK_ANIMATION,
                                            easing = LinearEasing,
                                        ),
                                    label = "SecondaryActionAlpha",
                                )
                            val primaryActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(durationMillis = 100, easing = LinearEasing),
                                    label = "PrimaryActionAlpha",
                                )
                            val revealedContentAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = FLASH_ANIMATION,
                                            easing = LinearEasing,
                                        ),
                                    label = "RevealedContentAlpha",
                                )
                            var revealedContentHeight by remember { mutableIntStateOf(0) }
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = revealedContentAlpha.value }
                                        .onSizeChanged { revealedContentHeight = it.height }
                                        .layout { measurable, constraints ->
                                            val placeable =
                                                measurable.measure(
                                                    constraints.copy(
                                                        maxWidth =
                                                            if (hideActions) {
                                                                    state.revealThreshold
                                                                } else {
                                                                    abs(state.offset)
                                                                }
                                                                .roundToInt()
                                                    )
                                                )
                                            layout(placeable.width, placeable.height) {
                                                placeable.placeRelative(
                                                    0,
                                                    calculateVerticalOffsetBasedOnScreenPosition(
                                                        revealedContentHeight,
                                                        globalPosition,
                                                    ),
                                                )
                                            }
                                        },
                                horizontalArrangement = Arrangement.Absolute.Right,
                            ) {
                                if (!swipingRight) {
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                    }
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha,
                                    )
                                } else {
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha,
                                    )
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier =
                    Modifier.absoluteOffset {
                        val xOffset = state.requireOffset().roundToInt()
                        IntOffset(
                            x = if (canSwipeRight) xOffset else xOffset.coerceAtMost(0),
                            y = 0,
                        )
                    }
            ) {
                content()
            }
            LaunchedEffect(state.currentValue) {
                if (
                    (state.currentValue == RevealValue.LeftRevealed ||
                        state.currentValue == RevealValue.RightRevealed) &&
                        state.lastActionType == RevealActionType.None
                ) {
                    onFullSwipe()
                }
            }
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun RowScope.ActionSlot(
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    opacity: State<Float> = mutableFloatStateOf(1f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.weight(weight).graphicsLayer { alpha = opacity.value },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun fadeInUndo(): ContentTransform =
    ContentTransform(
        // animation spec for the fading in undo action (fadeIn + scaleIn)
        targetContentEnter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = RAPID_ANIMATION,
                        delayMillis = FLASH_ANIMATION,
                        easing = LinearEasing,
                    )
            ) +
                scaleIn(
                    initialScale = 1.2f,
                    animationSpec =
                        tween(
                            durationMillis = RAPID_ANIMATION,
                            delayMillis = FLASH_ANIMATION,
                            easing = STANDARD_IN_OUT,
                        ),
                ),
        // animation spec for the fading out content and actions (fadeOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = FLASH_ANIMATION, easing = LinearEasing)),
    )

private fun fadeOutUndo(): ContentTransform =
    ContentTransform(
        // No animation, fade-in in 0 milliseconds since enter transition is mandatory
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = 0, delayMillis = SHORT_ANIMATION)),

        // animation spec for the fading out undo action (fadeOut + scaleOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = SHORT_ANIMATION, easing = LinearEasing)),
    )

private fun calculateVerticalOffsetBasedOnScreenPosition(
    childHeight: Int,
    globalPosition: LayoutCoordinates?,
): Int {
    if (globalPosition == null || !globalPosition.positionOnScreen().isSpecified) {
        return 0
    }
    val positionOnScreen = globalPosition.positionOnScreen()
    val boundsInWindow = globalPosition.boundsInWindow()
    val parentTop = positionOnScreen.y.toInt()
    val parentHeight = globalPosition.size.height
    val parentBottom = parentTop + parentHeight
    if (parentTop >= boundsInWindow.top && parentBottom <= boundsInWindow.bottom) {
        // Don't offset if the item is fully on screen
        return 0
    }

    // Avoid going outside parent bounds
    val minCenter = parentTop + childHeight / 2
    val maxCenter = parentTop + parentHeight - childHeight / 2
    val desiredCenter = boundsInWindow.center.y.toInt().coerceIn(minCenter, maxCenter)
    val actualCenter = parentTop + parentHeight / 2
    return desiredCenter - actualCenter
}

internal const val CustomTouchSlopMultiplier = 1.20f

/** Short animation in milliseconds. */
private const val SHORT_ANIMATION = 50

/** Flash animation length in milliseconds. */
private const val FLASH_ANIMATION = 100

/** Rapid animation length in milliseconds. */
private const val RAPID_ANIMATION = 200

/** Quick animation length in milliseconds. */
private const val QUICK_ANIMATION = 250

/** Standard easing for Swipe To Reveal. */
private val STANDARD_IN_OUT = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.00f)
