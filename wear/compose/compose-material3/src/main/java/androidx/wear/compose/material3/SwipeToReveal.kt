/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeDirection
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.createAnchors
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.tokens.SwipeToRevealTokens
import androidx.wear.compose.materialcore.screenWidthDp
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * [SwipeToReveal] Material composable. This adds the option to configure up to two additional
 * actions on a Composable: a mandatory [SwipeToRevealScope.primaryAction] and an optional
 * [SwipeToRevealScope.secondaryAction]. These actions are initially hidden and revealed only when
 * the [content] is swiped. These additional actions can be triggered by clicking on them after they
 * are revealed. [SwipeToRevealScope.primaryAction] will be triggered on full swipe of the
 * [content].
 *
 * For actions like "Delete", consider adding [SwipeToRevealScope.undoPrimaryAction] (displayed when
 * the [SwipeToRevealScope.primaryAction] is activated). Adding undo composables allow users to undo
 * the action that they just performed.
 *
 * [SwipeToReveal] composable adds the [CustomAccessibilityAction]s using the labels from primary
 * and secondary actions.
 *
 * Example of [SwipeToReveal] with primary and secondary actions
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSample
 *
 * Example of [SwipeToReveal] with a Card composable, it reveals a taller button.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSingleActionCardSample
 *
 * Example of [SwipeToReveal] that doesn't reveal the actions, instead it only executes them when
 * fully swiped or bounces back to its initial state.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealNonAnchoredSample
 * @param actions Actions of the [SwipeToReveal] composable, such as
 *   [SwipeToRevealScope.primaryAction]. [actions] should always include exactly one
 *   [SwipeToRevealScope.primaryAction]. [SwipeToRevealScope.secondaryAction],
 *   [SwipeToRevealScope.undoPrimaryAction] and [SwipeToRevealScope.undoSecondaryAction] are
 *   optional.
 * @param modifier [Modifier] to be applied on the composable
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param actionButtonHeight Desired height of the revealed action buttons. In case the content is a
 *   Button composable, it's suggested to use [SwipeToRevealDefaults.SmallActionButtonHeight], and
 *   for a Card composable, it's suggested to use [SwipeToRevealDefaults.LargeActionButtonHeight].
 * @param content The content that will be initially displayed over the other actions provided.
 * @see [androidx.wear.compose.foundation.SwipeToReveal]
 */
@Composable
fun SwipeToReveal(
    actions: SwipeToRevealScope.() -> Unit,
    modifier: Modifier = Modifier,
    revealState: RevealState = rememberRevealState(),
    actionButtonHeight: Dp = SwipeToRevealDefaults.SmallActionButtonHeight,
    content: @Composable () -> Unit,
) {
    val children = SwipeToRevealScope()
    with(children, actions)
    val primaryAction = children.primaryAction
    require(primaryAction != null) {
        "PrimaryAction should be provided in actions by calling the PrimaryAction method"
    }

    SwipeToReveal(
        modifier =
            modifier.fillMaxWidth().semantics {
                customActions = buildList {
                    add(
                        CustomAccessibilityAction(primaryAction.label) {
                            primaryAction.onClick()
                            true
                        }
                    )
                    children.secondaryAction?.let {
                        add(
                            CustomAccessibilityAction(it.label) {
                                it.onClick()
                                true
                            }
                        )
                    }
                }
            },
        primaryAction = {
            ActionButton(
                revealState,
                primaryAction,
                RevealActionType.PrimaryAction,
                actionButtonHeight,
                children.undoPrimaryAction != null,
            )
        },
        secondaryAction =
            children.secondaryAction?.let {
                {
                    ActionButton(
                        revealState,
                        it,
                        RevealActionType.SecondaryAction,
                        actionButtonHeight,
                        children.undoSecondaryAction != null,
                    )
                }
            },
        undoAction =
            when (revealState.lastActionType) {
                RevealActionType.SecondaryAction ->
                    children.undoSecondaryAction?.let {
                        {
                            ActionButton(
                                revealState,
                                it,
                                RevealActionType.UndoAction,
                                actionButtonHeight,
                            )
                        }
                    }
                else ->
                    children.undoPrimaryAction?.let {
                        {
                            ActionButton(
                                revealState,
                                it,
                                RevealActionType.UndoAction,
                                actionButtonHeight,
                            )
                        }
                    }
            },
        onFullSwipe = {
            // Full swipe triggers the main action, but does not set the click type.
            // Explicitly set the click type as main action when full swipe occurs.
            revealState.lastActionType = RevealActionType.PrimaryAction
            primaryAction.onClick()
        },
        state = revealState,
        content = content,
    )
}

/**
 * Scope for the actions of a [SwipeToReveal] composable. Used to define the primary, secondary,
 * undo primary and undo secondary actions.
 */
class SwipeToRevealScope {
    /**
     * Adds the primary action to a [SwipeToReveal]. This is required and exactly one primary action
     * should be specified. In case there are multiple, only the latest one will be displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a full swipe, or a
     *   button click.
     * @param icon Icon composable to be displayed for this action.
     * @param label Label for this action. Used to create a [CustomAccessibilityAction] for the
     *   [SwipeToReveal] component, and to display what the action is when the user fully swipes to
     *   execute the primary action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    fun primaryAction(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        primaryAction = SwipeToRevealAction(onClick, icon, label, containerColor, contentColor)
    }

    /**
     * Adds the secondary action to a [SwipeToReveal]. This is optional and at most one secondary
     * action should be specified. In case there are multiple, only the latest one will be
     * displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param icon Icon composable to be displayed for this action.
     * @param label Label for this action. Used to create a [CustomAccessibilityAction] for the
     *   [SwipeToReveal] component.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    fun secondaryAction(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        label: String,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        secondaryAction = SwipeToRevealAction(onClick, icon, label, containerColor, contentColor)
    }

    /**
     * Adds the undo action for the primary action to a [SwipeToReveal]. Displayed after the user
     * performs the primary action. This is optional and at most one undo primary action should be
     * specified. In case there are multiple, only the latest one will be displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param label Label for this action. Used to display what the undo action is after the user
     *   executes the primary action.
     * @param icon Optional Icon composable to be displayed for this action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    fun undoPrimaryAction(
        onClick: () -> Unit,
        label: String,
        icon: @Composable (() -> Unit)? = null,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        undoPrimaryAction = SwipeToRevealAction(onClick, icon, label, containerColor, contentColor)
    }

    /**
     * Adds the undo action for the secondary action to a [SwipeToReveal]. Displayed after the user
     * performs the secondary action.This is optional and at most one undo secondary action should
     * be specified. In case there are multiple, only the latest one will be displayed.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param label Label for this action. Used to display what the undo action is after the user
     *   executes the secondary action.
     * @param icon Optional Icon composable to be displayed for this action.
     * @param containerColor Container color for this action.
     * @param contentColor Content color for this action.
     */
    fun undoSecondaryAction(
        onClick: () -> Unit,
        label: String,
        icon: @Composable (() -> Unit)? = null,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        undoSecondaryAction =
            SwipeToRevealAction(onClick, icon, label, containerColor, contentColor)
    }

    internal var primaryAction: SwipeToRevealAction? = null
    internal var undoPrimaryAction: SwipeToRevealAction? = null
    internal var secondaryAction: SwipeToRevealAction? = null
    internal var undoSecondaryAction: SwipeToRevealAction? = null
}

/**
 * Creates a reveal state with Material3 specs.
 *
 * @param initialValue The initial value of the [RevealValue] for the [SwipeToReveal] composable.
 * @param anchorWidth Fraction of the screen revealed items should be displayed in. Ignored if
 *   [useAnchoredActions] is set to false, as the items won't be anchored to the screen. For a
 *   single action SwipeToReveal component, this should be
 *   [SwipeToRevealDefaults.SingleActionAnchorWidth], and for a double action SwipeToReveal,
 *   [SwipeToRevealDefaults.DoubleActionAnchorWidth] to be able to display both action buttons.
 * @param useAnchoredActions Whether the actions should stay revealed, or bounce back to hidden when
 *   the user stops swiping. This is relevant for SwipeToReveal components with a single action. If
 *   the developer wants a swipe to clear behaviour, this should be set to false.
 * @param swipeDirection Direction of the swipe to reveal the actions.
 */
@Composable
fun rememberRevealState(
    initialValue: RevealValue = RevealValue.Covered,
    anchorWidth: Dp = SwipeToRevealDefaults.SingleActionAnchorWidth,
    useAnchoredActions: Boolean = true,
    swipeDirection: SwipeDirection = SwipeDirection.RightToLeft,
): RevealState {
    val anchorFraction = anchorWidth.value / screenWidthDp()
    return androidx.wear.compose.foundation.rememberRevealState(
        initialValue = initialValue,
        animationSpec = spring(1f, Spring.StiffnessMedium),
        anchors =
            createAnchors(
                revealingAnchor = if (useAnchoredActions) anchorFraction else 0f,
                swipeDirection = swipeDirection,
            ),
    )
}

object SwipeToRevealDefaults {

    /** Width that's required to display both actions in a [SwipeToReveal] composable. */
    val DoubleActionAnchorWidth = 130.dp

    /** Width that's required to display a single action in a [SwipeToReveal] composable. */
    val SingleActionAnchorWidth = 64.dp

    /** Standard height for a small revealed action, such as when the swiped item is a Button. */
    val SmallActionButtonHeight = 52.dp

    /** Standard height for a large revealed action, such as when the swiped item is a Card. */
    val LargeActionButtonHeight = 84.dp

    internal val MinimumIconSize = 20.dp

    internal val IconSize = 26.dp

    internal val IconAndTextPadding = 6.dp

    internal val ActionButtonContentPadding = 4.dp

    internal val FullScreenPaddingFraction = 0.0625f
}

@Composable
internal fun ActionButton(
    revealState: RevealState,
    action: SwipeToRevealAction,
    revealActionType: RevealActionType,
    buttonHeight: Dp,
    hasUndo: Boolean = false,
) {
    val containerColor =
        action.containerColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContainerColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContainerColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.UndoActionContainerColor
                    )
                else -> Color.Unspecified
            }
        }
    val contentColor =
        action.contentColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContentColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContentColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(SwipeToRevealTokens.UndoActionContentColor)
                else -> Color.Unspecified
            }
        }
    val fullScreenPaddingDp = (screenWidthDp() * SwipeToRevealDefaults.FullScreenPaddingFraction).dp
    val startPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val endPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val coroutineScope = rememberCoroutineScope()
    Button(
        modifier =
            Modifier.height(buttonHeight)
                .padding(startPadding, 0.dp, endPadding, 0.dp)
                .fillMaxWidth(),
        onClick = {
            coroutineScope.launch {
                try {
                    if (revealActionType == RevealActionType.UndoAction) {
                        revealState.animateTo(RevealValue.Covered)
                    } else {
                        if (hasUndo || revealActionType == RevealActionType.PrimaryAction) {
                            revealState.lastActionType = revealActionType
                            revealState.animateTo(
                                if (revealState.offset > 0) {
                                    RevealValue.LeftRevealed
                                } else {
                                    RevealValue.RightRevealed
                                }
                            )
                        }
                    }
                } finally {
                    // Execute onClick even if the animation gets interrupted
                    action.onClick()
                }
            }
        },
        colors = buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(SwipeToRevealDefaults.ActionButtonContentPadding),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val density = LocalDensity.current
            val primaryActionTextRevealed = remember { mutableStateOf(false) }
            action.icon?.let { ActionIconWrapper(it) }
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    AnimatedVisibility(
                        visible = primaryActionTextRevealed.value,
                        enter = fadeIn() + expandHorizontally() + scaleIn(),
                        exit = fadeOut() + shrinkHorizontally() + scaleOut(),
                    ) {
                        ActionText(action, contentColor)
                    }
                RevealActionType.UndoAction -> ActionText(action, contentColor)
            }
            if (revealActionType == RevealActionType.PrimaryAction) {
                LaunchedEffect(revealState.offset) {
                    val minimumOffsetToRevealPx =
                        with(density) {
                            SwipeToRevealDefaults.DoubleActionAnchorWidth.toPx().toInt()
                        }
                    primaryActionTextRevealed.value =
                        abs(revealState.offset) > minimumOffsetToRevealPx &&
                            (revealState.targetValue == RevealValue.RightRevealed ||
                                revealState.targetValue == RevealValue.LeftRevealed)
                }
            }
        }
    }
}

@Composable
private fun ActionText(action: SwipeToRevealAction, contentColor: Color) {
    Text(
        modifier =
            Modifier.padding(
                start = action.icon?.let { SwipeToRevealDefaults.IconAndTextPadding } ?: 0.dp
            ),
        text = action.label,
        color = contentColor,
        maxLines = 1
    )
}

@Composable
private fun ActionIconWrapper(content: @Composable () -> Unit) {
    val iconAlpha = remember { mutableFloatStateOf(0f) }
    Box(
        modifier =
            Modifier.onGloballyPositioned { coordinates ->
                    val currentWidthDp = coordinates.size.width.dp.value
                    iconAlpha.floatValue =
                        ((currentWidthDp - SwipeToRevealDefaults.MinimumIconSize.value) /
                                (SwipeToRevealDefaults.IconSize.value -
                                    SwipeToRevealDefaults.MinimumIconSize.value))
                            .coerceIn(0.0f, 1.0f)
                }
                .size(SwipeToRevealDefaults.IconSize, Dp.Unspecified)
                .graphicsLayer { alpha = iconAlpha.floatValue }
    ) {
        content()
    }
}

/** Data class to define an action to be displayed in a [SwipeToReveal] composable. */
internal data class SwipeToRevealAction(
    /** Callback to be executed when the action is performed via a full swipe, or a button click. */
    val onClick: () -> Unit,

    /**
     * Icon composable to be displayed for this action. This accepts a scale parameter that should
     * be used to increase icon icon when an action is fully revealed.
     */
    val icon: @Composable (() -> Unit)?,

    /**
     * Label for this action. Used to create a [CustomAccessibilityAction] for the [SwipeToReveal]
     * component, display what the action is when the user fully swipes to execute the primary
     * action, or when the undo action is shown.
     */
    val label: String,

    /**
     * Color of the container, used for the background of the action button. This can be
     * [Color.Unspecified], and in case it is, needs to be replaced with a default.
     */
    val containerColor: Color,

    /**
     * Color of the content, used for the icon and text. This can be [Color.Unspecified], and in
     * case it is, needs to be replaced with a default.
     */
    val contentColor: Color,
)
