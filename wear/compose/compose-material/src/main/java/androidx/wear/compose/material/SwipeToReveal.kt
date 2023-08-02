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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.wear.compose.foundation.RevealScope
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [SwipeToReveal] Material composable for Chips. This provides the default style for consistency.
 *
 * @see [SwipeToReveal]
 * @param action An [SwipeToRevealAction] object to describe the primary action when swiping. See
 * [SwipeToRevealDefaults.action].
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param modifier [Modifier] to be applied on the composable
 * @param additionalAction An [SwipeToRevealAction] object to describe the contents of addition action.
 * See [SwipeToRevealDefaults.additionalAction]
 * @param undoAction An [SwipeToRevealAction] object to describe the contents of undo action. See
 * [SwipeToRevealDefaults.undoAction].
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 * [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of action and additional action composables. Recommended shape for chips
 * is [Shapes.small].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
public fun SwipeToRevealChip(
    action: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier = Modifier,
    additionalAction: SwipeToRevealAction? = null,
    undoAction: SwipeToRevealAction? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable () -> Unit
) {
    SwipeToRevealComponent(
        action = action,
        revealState = revealState,
        modifier = modifier,
        additionalAction = additionalAction,
        undoAction = undoAction,
        colors = colors,
        shape = shape,
        content = content
    )
}

/**
 * [SwipeToReveal] Material composable for Cards. This provides the default style for consistency.
 *
 * @see [SwipeToReveal]
 * @param action An [SwipeToRevealAction] object to describe the primary action when swiping. See
 * [SwipeToRevealDefaults.action]
 * @param revealState [RevealState] of the [SwipeToReveal]
 * @param modifier [Modifier] to be applied on the composable
 * @param additionalAction An [SwipeToRevealAction] object to describe the contents of addition action
 * @param undoAction An [SwipeToRevealAction] object to describe the contents of undo action
 * @param colors An instance of [SwipeToRevealActionColors] to describe the colors of actions. See
 * [SwipeToRevealDefaults.actionColors].
 * @param shape The shape of action and additional action composables. Recommended shape for cards
 * is [SwipeToRevealDefaults.CardActionShape].
 * @param content The initial content shown prior to the swipe-to-reveal gesture.
 */
@ExperimentalWearMaterialApi
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
public fun SwipeToRevealCard(
    action: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier = Modifier,
    additionalAction: SwipeToRevealAction? = null,
    undoAction: SwipeToRevealAction? = null,
    colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
    shape: Shape = SwipeToRevealDefaults.CardActionShape,
    content: @Composable () -> Unit
) {
    SwipeToRevealComponent(
        action = action,
        revealState = revealState,
        modifier = modifier,
        additionalAction = additionalAction,
        undoAction = undoAction,
        colors = colors,
        shape = shape,
        content = content
    )
}

/**
 * Defaults for Material [SwipeToReveal].
 */
@ExperimentalWearMaterialApi
public object SwipeToRevealDefaults {
    /**
     * Recommended shape for [SwipeToReveal] actions when used with [Card].
     */
    public val CardActionShape = RoundedCornerShape(40.dp)

    /**
     * Colors to be used with different actions in [SwipeToReveal].
     *
     * @param actionBackgroundColor The background color (color of the shape) of the action
     * @param actionContentColor The content color (text and icon) of the action
     * @param additionalActionBackgroundColor The background color (color of the shape) of the
     * additional action
     * @param additionalActionContentColor The content color (text and icon) of the additional
     * action
     * @param undoActionBackgroundColor The background color (color of the shape) of the undo action
     * @param undoActionContentColor The content color (text) of the undo action
     */
    @Composable
    public fun actionColors(
        actionBackgroundColor: Color = MaterialTheme.colors.error,
        actionContentColor: Color = MaterialTheme.colors.onError,
        additionalActionBackgroundColor: Color = MaterialTheme.colors.surface,
        additionalActionContentColor: Color = MaterialTheme.colors.onSurface,
        undoActionBackgroundColor: Color = MaterialTheme.colors.surface,
        undoActionContentColor: Color = MaterialTheme.colors.onSurface
    ): SwipeToRevealActionColors {
        return SwipeToRevealActionColors(
            actionBackgroundColor = actionBackgroundColor,
            actionContentColor = actionContentColor,
            additionalActionBackgroundColor = additionalActionBackgroundColor,
            additionalActionContentColor = additionalActionContentColor,
            undoActionBackgroundColor = undoActionBackgroundColor,
            undoActionContentColor = undoActionContentColor
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the primary action parameter of [SwipeToRevealChip] and
     * [SwipeToRevealCard]. The default behaviour of this action is to animate to
     * [RevealValue.Revealed] and execute the [onClick] parameter. To override, consider using
     * [Modifier.clickable].
     *
     * @param icon The icon that will be displayed initially on the action
     * @param label The text that will be displayed on the expanded action
     * @param modifier [Modifier] to be applied on this action composable
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun action(
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = icon,
            label = label,
            modifier = modifier,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the additional action of [SwipeToRevealChip] and
     * [SwipeToRevealCard]. The default behaviour of this action is to animate to
     * [RevealValue.Revealed] and execute the [onClick] parameter. To override, consider using
     * [Modifier.clickable].
     *
     * @param icon The icon that will be displayed initially on the screen
     * @param modifier [Modifier] to be applied on this action composable
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun additionalAction(
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = icon,
            label = null,
            modifier = modifier,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * Creates a new [SwipeToRevealAction] instance for the undo action of [SwipeToRevealChip] and
     * [SwipeToRevealCard]. The default behaviour of this action is to snap to
     * [RevealValue.Covered] and execute the [onClick] parameter. To override, consider using
     * [Modifier.clickable].
     *
     * @param label The text that will be displayed on the undo action composable
     * @param modifier [Modifier] to be applied on this action composable
     * @param interactionSource The [MutableInteractionSource] representing the stream of
     * interactions with this action.
     * @param onClick Called when this action is clicked.
     */
    @Composable
    public fun undoAction(
        label: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        onClick: () -> Unit = {}
    ): SwipeToRevealAction {
        return SwipeToRevealAction(
            icon = null,
            label = label,
            modifier = modifier,
            interactionSource = interactionSource,
            onClick = onClick
        )
    }

    /**
     * [ImageVector] for delete icon, often used for the primary action.
     */
    public val Delete = Icons.Outlined.Delete

    /**
     * [ImageVector] for more options icon, often used for the additional action.
     */
    public val MoreOptions = Icons.Outlined.MoreVert

    internal val UndoButtonHorizontalPadding = 14.dp
    internal val UndoButtonVerticalPadding = 6.dp
    internal val ActionMaxHeight = 84.dp
}

/**
 * A class representing the colors applied in [SwipeToReveal] actions.
 *
 * @param actionBackgroundColor Color of the shape (background)
 * @param actionContentColor Color of icon or text used in the action
 * @param additionalActionBackgroundColor Color of the additional action shape (background)
 * @param additionalActionContentColor Color of the icon or text used in the additional action
 * @param undoActionBackgroundColor Color of the undo action shape (background)
 * @param undoActionContentColor Color of the icon or text used in the undo action
 */
@ExperimentalWearMaterialApi
public class SwipeToRevealActionColors constructor(
    val actionBackgroundColor: Color,
    val actionContentColor: Color,
    val additionalActionBackgroundColor: Color,
    val additionalActionContentColor: Color,
    val undoActionBackgroundColor: Color,
    val undoActionContentColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SwipeToRevealActionColors

        if (actionBackgroundColor != other.actionBackgroundColor) return false
        if (actionContentColor != other.actionContentColor) return false
        if (additionalActionBackgroundColor != other.additionalActionBackgroundColor) return false
        if (additionalActionContentColor != other.additionalActionContentColor) return false
        if (undoActionBackgroundColor != other.undoActionBackgroundColor) return false
        if (undoActionContentColor != other.undoActionContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actionBackgroundColor.hashCode()
        result = 31 * result + actionContentColor.hashCode()
        result = 31 * result + additionalActionBackgroundColor.hashCode()
        result = 31 * result + additionalActionContentColor.hashCode()
        result = 31 * result + undoActionBackgroundColor.hashCode()
        result = 31 * result + undoActionContentColor.hashCode()
        return result
    }
}

/**
 * A class containing the details required for describing the content of an action composable.
 *
 * @param icon A slot for providing the icon for this [SwipeToReveal] action
 * @param label A slot for providing a text label for this [SwipeToRevealAction] action. The
 * content provided here will be used in different perspective based on the action type
 * (action, additional action or undo action).
 * @param modifier The [Modifier] to be applied on the action.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * interactions with this action.
 * @param onClick Called when the user clicks the action.
 */
@ExperimentalWearMaterialApi
public class SwipeToRevealAction constructor(
    val icon: (@Composable () -> Unit)?,
    val label: (@Composable () -> Unit)?,
    val modifier: Modifier,
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
        if (interactionSource != other.interactionSource) return false
        if (onClick != other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = icon.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + modifier.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
private fun SwipeToRevealComponent(
    action: SwipeToRevealAction,
    revealState: RevealState,
    modifier: Modifier,
    additionalAction: SwipeToRevealAction?,
    undoAction: SwipeToRevealAction?,
    colors: SwipeToRevealActionColors,
    shape: Shape,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        state = revealState,
        modifier = modifier,
        onFullSwipe = action.onClick,
        action = {
            SwipeToRevealAction(
                revealState = revealState,
                coroutineScope = coroutineScope,
                action = action,
                backgroundColor = colors.actionBackgroundColor,
                contentColor = colors.actionContentColor,
                shape = shape,
                animateTo = RevealValue.Revealed
            )
        },
        additionalAction =
        additionalAction?.let {
            {
                SwipeToRevealAction(
                    revealState = revealState,
                    coroutineScope = coroutineScope,
                    action = additionalAction,
                    backgroundColor = colors.additionalActionBackgroundColor,
                    contentColor = colors.additionalActionContentColor,
                    shape = shape,
                    animateTo = RevealValue.Revealed
                )
            }
        },
        undoAction =
        undoAction?.label?.let {
            {
                Box(
                    modifier = undoAction.modifier
                        .clickable(
                            interactionSource = undoAction.interactionSource,
                            indication = rememberRipple(),
                            role = Role.Button,
                            onClick = {
                                coroutineScope.launch { revealState.animateTo(RevealValue.Covered) }
                                undoAction.onClick()
                            }
                        )
                        .clip(CircleShape)
                        .background(color = colors.undoActionBackgroundColor)
                        .padding(
                            horizontal = SwipeToRevealDefaults.UndoButtonHorizontalPadding,
                            vertical = SwipeToRevealDefaults.UndoButtonVerticalPadding
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.undoActionContentColor
                    ) {
                        undoAction.label.invoke()
                    }
                }
            }
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
    coroutineScope: CoroutineScope,
    action: SwipeToRevealAction,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape,
    animateTo: RevealValue
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
            .clickable(
                interactionSource = action.interactionSource,
                indication = rememberRipple(),
                role = Role.Button,
                onClick = {
                    coroutineScope.launch { revealState.animateTo(animateTo) }
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
