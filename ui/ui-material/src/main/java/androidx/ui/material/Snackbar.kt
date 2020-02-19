/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.Composable
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.FirstBaseline
import androidx.ui.core.LastBaseline
import androidx.ui.core.Layout
import androidx.ui.core.LayoutTagParentData
import androidx.ui.core.Modifier
import androidx.ui.core.ParentData
import androidx.ui.core.Text
import androidx.ui.core.tag
import androidx.ui.foundation.DrawBackground
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.AlignmentLineOffset
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.material.surface.Surface
import androidx.ui.unit.IntPx
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max

/**
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience,
 * and they don’t require user input to disappear.
 *
 * A snackbar can contain a single action. Because they disappear automatically, the action
 * shouldn’t be “Dismiss” or “Cancel.”
 *
 * @sample androidx.ui.material.samples.SimpleSnackbar
 *
 * @param text information about a process that an app has performed or will perform
 * @param actionText action name in the snackbar. If null, there will be text label with no
 * action button
 * @param onActionClick lambda to be invoked when the action is clicked
 * @param modifier modifiers for the the Snackbar layout
 * @param actionOnNewLine whether or not action should be put on the separate line. Recommended
 * for action with long action text
 */
@Composable
fun Snackbar(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier.None,
    actionOnNewLine: Boolean = false
) {
    val actionSlot: @Composable() (() -> Unit)? =
        if (actionText != null) {
            @Composable {
                TextButton(
                    onClick = onActionClick,
                    // TODO: remove this when primary light variant is figured out
                    contentColor = makePrimaryVariantLight(MaterialTheme.colors().primary)
                ) {
                    Text(actionText)
                }
            }
        } else {
            null
        }
    Snackbar(
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        text = { Text(text, maxLines = TextMaxLines) },
        action = actionSlot
    )
}

/**
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience,
 * and they don’t require user input to disappear.
 *
 * A snackbar can contain a single action. Because they disappear automatically, the action
 * shouldn’t be “Dismiss” or “Cancel.”
 *
 * This version provides more granular control over the content of the Snackbar. Use it if you
 * want to customize the content inside.
 *
 * @sample androidx.ui.material.samples.SlotsSnackbar
 *
 * @param text text component to show information about a process that an app has performed or
 * will perform
 * @param action action / button component to add as an action to the snackbar
 * @param modifier modifiers for the the Snackbar layout
 * @param actionOnNewLine whether or not action should be put on the separate line. Recommended
 * for action with long action text
 */
@Composable
fun Snackbar(
    text: @Composable() () -> Unit,
    action: @Composable() (() -> Unit)? = null,
    modifier: Modifier = Modifier.None,
    actionOnNewLine: Boolean = false
) {
    val colors = MaterialTheme.colors()
    Surface(
        modifier = modifier,
        shape = SnackbarShape,
        elevation = SnackbarElevation,
        color = colors.surface
    ) {
        val textStyle = MaterialTheme.typography().body2.copy(color = colors.surface)
        val additionalBackground = DrawBackground(
            color = colors.onSurface.copy(alpha = SnackbarOverlayAlpha),
            shape = SnackbarShape
        )
        CurrentTextStyleProvider(value = textStyle) {
            when {
                action == null -> TextOnlySnackbar(additionalBackground, text)
                actionOnNewLine -> NewLineButtonSnackbar(additionalBackground, text, action)
                else -> OneRowSnackbar(additionalBackground, text, action)
            }
        }
    }
}

@Composable
private fun TextOnlySnackbar(modifier: Modifier = Modifier.None, text: @Composable() () -> Unit) {
    Layout(
        text,
        modifier = modifier + LayoutPadding(start = HorizontalSpacing, end = HorizontalSpacing)
    ) { measurables, constraints ->
        require(measurables.size == 1) {
            "text for Snackbar expected to have exactly only one child"
        }
        val textPlaceable = measurables.first().measure(constraints)
        val firstBaseline = requireNotNull(textPlaceable[FirstBaseline]) { "No baselines for text" }
        val lastBaseline = requireNotNull(textPlaceable[LastBaseline]) { "No baselines for text" }

        val minHeight = if (firstBaseline == lastBaseline) MinHeightOneLine else MinHeightTwoLines
        layout(constraints.maxWidth, max(minHeight.toIntPx(), textPlaceable.height)) {
            val textPlaceY = HeightToFirstLine.toIntPx() - firstBaseline
            textPlaceable.place(0.ipx, textPlaceY)
        }
    }
}

@Composable
private fun NewLineButtonSnackbar(
    modifier: Modifier = Modifier.None,
    text: @Composable() () -> Unit,
    button: @Composable() () -> Unit
) {
    Column(
        modifier = modifier + LayoutWidth.Fill + LayoutPadding(
            start = HorizontalSpacing,
            end = HorizontalSpacingButtonSide,
            bottom = SeparateButtonExtraY
        )
    ) {
        AlignmentLineOffset(alignmentLine = LastBaseline, after = LongButtonVerticalOffset) {
            AlignmentLineOffset(alignmentLine = FirstBaseline, before = HeightToFirstLine) {
                Container(LayoutPadding(end = HorizontalSpacingButtonSide), children = text)
            }
        }
        Container(modifier = LayoutGravity.End, children = button)
    }
}

@Composable
private fun OneRowSnackbar(
    modifier: Modifier = Modifier.None,
    text: @Composable() () -> Unit,
    button: @Composable() () -> Unit
) {
    Layout(
        {
            ParentData(
                object : LayoutTagParentData {
                    override val tag: Any = "text"
                },
                text
            )
            ParentData(
                object : LayoutTagParentData {
                    override val tag: Any = "button"
                },
                button
            )
        },
        modifier = modifier +
                LayoutPadding(start = HorizontalSpacing, end = HorizontalSpacingButtonSide)
    ) { measurables, constraints ->
        val buttonPlaceable = measurables.first { it.tag == "button" }.measure(constraints)
        val textMaxWidth =
            (constraints.maxWidth - buttonPlaceable.width - TextEndExtraSpacing.toIntPx())
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable = measurables.first { it.tag == "text" }.measure(
            constraints.copy(minHeight = IntPx.Zero, maxWidth = textMaxWidth)
        )

        val firstTextBaseline =
            requireNotNull(textPlaceable[FirstBaseline]) { "No baselines for text" }
        val lastTextBaseline =
            requireNotNull(textPlaceable[LastBaseline]) { "No baselines for text" }
        val baselineOffset = HeightToFirstLine.toIntPx()
        val isOneLine = firstTextBaseline == lastTextBaseline
        val textPlaceY = baselineOffset - firstTextBaseline
        val buttonPlaceX = constraints.maxWidth - buttonPlaceable.width

        val containerHeight: IntPx
        val buttonPlaceY: IntPx
        if (isOneLine) {
            val minContainerHeight = MinHeightOneLine.toIntPx()
            val contentHeight = buttonPlaceable.height + SingleTextYPadding.toIntPx() * 2
            containerHeight = max(minContainerHeight, contentHeight)
            val buttonBaseline = buttonPlaceable.get(FirstBaseline)
            buttonPlaceY =
                buttonBaseline?.let { baselineOffset - it } ?: SingleTextYPadding.toIntPx()
        } else {
            val minContainerHeight = MinHeightTwoLines.toIntPx()
            val contentHeight = textPlaceY + textPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            buttonPlaceY = (containerHeight - buttonPlaceable.height) / 2
        }

        layout(constraints.maxWidth, containerHeight) {
            textPlaceable.place(0.ipx, textPlaceY)
            buttonPlaceable.place(buttonPlaceX, buttonPlaceY)
        }
    }
}

// TODO: remove this when primary light variant is figured out in MaterialTheme
private fun makePrimaryVariantLight(primary: Color): Color {
    val blendColor = Color.White.copy(alpha = 0.6f)
    return Color(
        red = blendColor.red * blendColor.alpha + primary.red * (1 - blendColor.alpha),
        green = blendColor.green * blendColor.alpha + primary.green * (1 - blendColor.alpha),
        blue = blendColor.blue * blendColor.alpha + primary.blue * (1 - blendColor.alpha)
    )
}

private val TextMaxLines = 2
private val SnackbarOverlayAlpha = 0.8f
private val SnackbarShape = RoundedCornerShape(4.dp)
private val SnackbarElevation = 6.dp

private val MinHeightOneLine = 48.dp
private val MinHeightTwoLines = 68.dp
private val HeightToFirstLine = 30.dp
private val HorizontalSpacing = 16.dp
private val HorizontalSpacingButtonSide = 8.dp
private val SeparateButtonExtraY = 8.dp
private val SingleTextYPadding = 6.dp
private val TextEndExtraSpacing = 8.dp
private val LongButtonVerticalOffset = 18.dp