/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.tokens.SnackbarTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Material Design snackbar.
 *
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience,
 * and they don’t require user input to disappear.
 *
 * A Snackbar can contain a single action. "Dismiss" or "cancel" actions are optional.
 *
 * Snackbars with an action should not timeout or self-dismiss until the user performs another
 * action. Here, moving the keyboard focus indicator to navigate through interactive elements in a
 * page is not considered an action.
 *
 * This component provides only the visuals of the Snackbar. If you need to show a Snackbar
 * with defaults on the screen, use [SnackbarHostState.showSnackbar]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the Snackbar, you can pass your own version as a child
 * of the [SnackbarHost] to the [Scaffold]:
 * @sample androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
 *
 * @param modifier modifiers for the Snackbar layout
 * @param action action / button component to add as an action to the snackbar. Consider using
 * [ColorScheme.inversePrimary] as the color for the action, if you do not have a predefined color
 * you wish to use instead.
 * @param dismissAction action / button component to add as an additional close affordance action
 * when a snackbar is non self-dismissive. Consider using [ColorScheme.inverseOnSurface] as the
 * color for the action, if you do not have a predefined color you wish to use instead.
 * @param actionOnNewLine whether or not action should be put on the separate line. Recommended
 * for action with long action text
 * @param shape defines the Snackbar's shape (as well as its shadow when using `shadowElevation`)
 * @param containerColor background color of the Snackbar
 * @param contentColor the preferred color for content inside this Snackbar. Also see
 * [LocalContentColor] which is used by [Text] and [Icon] by default.
 * @param content content to show information about a process that an app has performed or will
 * perform
 */
@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    dismissAction: @Composable (() -> Unit)? = null,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarTokens.ContainerShape,
    containerColor: Color = SnackbarTokens.ContainerColor.toColor(),
    contentColor: Color = SnackbarTokens.SupportingTextColor.toColor(),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = SnackbarTokens.ContainerElevation
    ) {
        val textStyle = MaterialTheme.typography.fromToken(SnackbarTokens.SupportingTextFont)
        CompositionLocalProvider(LocalTextStyle provides textStyle) {
            when {
                action == null -> OneRowSnackbar(
                    text = content,
                    action = null,
                    dismissAction = dismissAction
                )
                actionOnNewLine -> NewLineButtonSnackbar(content, action, dismissAction)
                else -> OneRowSnackbar(
                    text = content,
                    action = action,
                    dismissAction = dismissAction
                )
            }
        }
    }
}

/**
 * Material Design snackbar.
 *
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience,
 * and they don’t require user input to disappear.
 *
 * A Snackbar can contain a single action. "Dismiss" or "cancel" actions are optional.
 *
 * Snackbars with an action should not timeout or self-dismiss until the user performs another
 * action. Here, moving the keyboard focus indicator to navigate through interactive elements in a
 * page is not considered an action.
 *
 * This version of snackbar is designed to work with [SnackbarData] provided by the
 * [SnackbarHost], which is usually used inside of the [Scaffold].
 *
 * This components provides only the visuals of the Snackbar. If you need to show a Snackbar
 * with defaults on the screen, use [SnackbarHostState.showSnackbar]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
 *
 * If you want to customize appearance of the Snackbar, you can pass your own version as a child
 * of the [SnackbarHost] to the [Scaffold]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
 *
 * When a [SnackbarData.visuals] sets the Snackbar's duration as [SnackbarDuration.Indefinite], it's
 * recommended to display an additional close affordance action.
 * See [SnackbarVisuals.withDismissAction]:
 *
 * @sample androidx.compose.material3.samples.ScaffoldWithIndefiniteSnackbar
 *
 * @param snackbarData data about the current snackbar showing via [SnackbarHostState]
 * @param modifier modifiers for the Snackbar layout
 * @param actionOnNewLine whether or not the Snackbar's action should be put on the separate line
 * (recommended for action with long action text)
 * @param shape defines the Snackbar's shape (as well as its shadow when using `shadowElevation`)
 * @param containerColor background color of the Snackbar
 * @param contentColor the preferred color for content inside this Snackbar. Also see
 * [LocalContentColor] which is used by [Text] and [Icon] by default.
 * @param actionColor color of the Snackbar's action
 */
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = SnackbarTokens.ContainerShape,
    containerColor: Color = SnackbarTokens.ContainerColor.toColor(),
    contentColor: Color = SnackbarTokens.SupportingTextColor.toColor(),
    actionColor: Color = SnackbarTokens.ActionLabelTextColor.toColor()
) {
    val actionLabel = snackbarData.visuals.actionLabel
    val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
        @Composable {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                onClick = { snackbarData.performAction() },
                content = { Text(actionLabel) }
            )
        }
    } else {
        null
    }
    val dismissActionComposable: (@Composable () -> Unit)? =
        if (snackbarData.visuals.withDismissAction) {
            @Composable {
                IconButton(
                    onClick = { snackbarData.dismiss() },
                    content = {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = null, // TODO add "Dismiss Snackbar" to Strings.
                        )
                    }
                )
            }
        } else {
            null
        }
    Snackbar(
        modifier = modifier.padding(12.dp),
        action = actionComposable,
        dismissAction = dismissActionComposable,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        content = { Text(snackbarData.visuals.message) }
    )
}

@Composable
private fun NewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit,
    dismissAction: @Composable (() -> Unit)?
) {
    Column(
        modifier = Modifier
            // Fill max width, up to ContainerMaxWidth.
            .widthIn(max = ContainerMaxWidth)
            .fillMaxWidth()
            .padding(
                start = HorizontalSpacing,
                bottom = SeparateButtonExtraY
            )
    ) {
        Box(
            Modifier.paddingFromBaseline(HeightToFirstLine, LongButtonVerticalOffset)
                .padding(end = HorizontalSpacingButtonSide)
        ) { text() }

        Box(
            Modifier.align(Alignment.End)
                .padding(end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp)
        ) {
            Row {
                val actionTextColor = SnackbarTokens.ActionLabelTextColor.toColor()
                val actionTextStyle =
                    MaterialTheme.typography.fromToken(SnackbarTokens.ActionLabelTextFont)
                CompositionLocalProvider(
                    LocalContentColor provides actionTextColor,
                    LocalTextStyle provides actionTextStyle,
                    content = action
                )

                if (dismissAction != null) {
                    val dismissActionColor = SnackbarTokens.IconColor.toColor()
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction
                    )
                }
            }
        }
    }
}

@Composable
private fun OneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)?,
    dismissAction: @Composable (() -> Unit)?
) {
    val textTag = "text"
    val actionTag = "action"
    val dismissActionTag = "dismissAction"
    Layout(
        {
            Box(Modifier.layoutId(textTag).padding(vertical = SnackbarVerticalPadding)) { text() }
            if (action != null) {
                Box(Modifier.layoutId(actionTag)) {
                    val actionTextColor = SnackbarTokens.ActionLabelTextColor.toColor()
                    val actionTextStyle =
                        MaterialTheme.typography.fromToken(SnackbarTokens.ActionLabelTextFont)
                    CompositionLocalProvider(
                        LocalContentColor provides actionTextColor,
                        LocalTextStyle provides actionTextStyle,
                        content = action
                    )
                }
            }
            if (dismissAction != null) {
                Box(Modifier.layoutId(dismissActionTag)) {
                    val dismissActionColor = SnackbarTokens.IconColor.toColor()
                    CompositionLocalProvider(
                        LocalContentColor provides dismissActionColor,
                        content = dismissAction
                    )
                }
            }
        },
        modifier = Modifier.padding(
            start = HorizontalSpacing,
            end = if (dismissAction == null) HorizontalSpacingButtonSide else 0.dp
        )
    ) { measurables, constraints ->
        val containerWidth = min(constraints.maxWidth, ContainerMaxWidth.roundToPx())
        val actionButtonPlaceable =
            measurables.firstOrNull { it.layoutId == actionTag }?.measure(constraints)
        val dismissButtonPlaceable =
            measurables.firstOrNull { it.layoutId == dismissActionTag }?.measure(constraints)
        val actionButtonWidth = actionButtonPlaceable?.width ?: 0
        val actionButtonHeight = actionButtonPlaceable?.height ?: 0
        val dismissButtonWidth = dismissButtonPlaceable?.width ?: 0
        val dismissButtonHeight = dismissButtonPlaceable?.height ?: 0
        val extraSpacingWidth = if (dismissButtonWidth == 0) TextEndExtraSpacing.roundToPx() else 0
        val textMaxWidth =
            (containerWidth - actionButtonWidth - dismissButtonWidth - extraSpacingWidth)
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable = measurables.first { it.layoutId == textTag }.measure(
            constraints.copy(minHeight = 0, maxWidth = textMaxWidth)
        )

        val firstTextBaseline = textPlaceable[FirstBaseline]
        require(firstTextBaseline != AlignmentLine.Unspecified) { "No baselines for text" }
        val lastTextBaseline = textPlaceable[LastBaseline]
        require(lastTextBaseline != AlignmentLine.Unspecified) { "No baselines for text" }
        val isOneLine = firstTextBaseline == lastTextBaseline
        val dismissButtonPlaceX = containerWidth - dismissButtonWidth
        val actionButtonPlaceX = dismissButtonPlaceX - actionButtonWidth

        val textPlaceY: Int
        val containerHeight: Int
        val actionButtonPlaceY: Int
        if (isOneLine) {
            val minContainerHeight = SnackbarTokens.SingleLineContainerHeight.roundToPx()
            val contentHeight = max(actionButtonHeight, dismissButtonHeight)
            containerHeight = max(minContainerHeight, contentHeight)
            textPlaceY = (containerHeight - textPlaceable.height) / 2
            actionButtonPlaceY = if (actionButtonPlaceable != null) {
                actionButtonPlaceable[FirstBaseline].let {
                    if (it != AlignmentLine.Unspecified) {
                        textPlaceY + firstTextBaseline - it
                    } else {
                        0
                    }
                }
            } else {
                0
            }
        } else {
            val baselineOffset = HeightToFirstLine.roundToPx()
            textPlaceY = baselineOffset - firstTextBaseline
            val minContainerHeight = SnackbarTokens.TwoLinesContainerHeight.roundToPx()
            val contentHeight = textPlaceY + textPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            actionButtonPlaceY = if (actionButtonPlaceable != null) {
                (containerHeight - actionButtonPlaceable.height) / 2
            } else {
                0
            }
        }
        val dismissButtonPlaceY = if (dismissButtonPlaceable != null) {
            (containerHeight - dismissButtonPlaceable.height) / 2
        } else {
            0
        }

        layout(containerWidth, containerHeight) {
            textPlaceable.placeRelative(0, textPlaceY)
            dismissButtonPlaceable?.placeRelative(dismissButtonPlaceX, dismissButtonPlaceY)
            actionButtonPlaceable?.placeRelative(actionButtonPlaceX, actionButtonPlaceY)
        }
    }
}

private val ContainerMaxWidth = 600.dp
private val HeightToFirstLine = 30.dp
private val HorizontalSpacing = 16.dp
private val HorizontalSpacingButtonSide = 8.dp
private val SeparateButtonExtraY = 2.dp
private val SnackbarVerticalPadding = 6.dp
private val TextEndExtraSpacing = 8.dp
private val LongButtonVerticalOffset = 12.dp
