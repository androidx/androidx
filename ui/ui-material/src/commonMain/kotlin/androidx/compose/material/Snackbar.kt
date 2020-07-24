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

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.id
import androidx.ui.core.layoutId
import androidx.compose.foundation.Box
import androidx.compose.foundation.ProvideTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.relativePaddingFrom
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.foundation.text.LastBaseline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Snackbars provide brief messages about app processes at the bottom of the screen.
 *
 * Snackbars inform users of a process that an app has performed or will perform. They appear
 * temporarily, towards the bottom of the screen. They shouldn’t interrupt the user experience,
 * and they don’t require user input to disappear.
 *
 * A Snackbar can contain a single action. Because they disappear automatically, the action
 * shouldn't be "Dismiss" or "Cancel".
 *
 * @sample androidx.compose.material.samples.SimpleSnackbar
 *
 * @param text text component to show information about a process that an app has performed or
 * will perform
 * @param action action / button component to add as an action to the snackbar. Consider using
 * [snackbarPrimaryColorFor] as the color for the action, if you do not have a predefined color
 * you wish to use instead.
 * @param modifier modifiers for the the Snackbar layout
 * @param actionOnNewLine whether or not action should be put on the separate line. Recommended
 * for action with long action text
 * @param shape Defines the Snackbar's shape as well as its shadow
 * @param elevation The z-coordinate at which to place the SnackBar. This controls the size
 * of the shadow below the SnackBar
 */
@Composable
fun Snackbar(
    text: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    elevation: Dp = 6.dp
) {
    val colors = MaterialTheme.colors
    // Snackbar has a background color of onSurface with an alpha applied blended on top of surface
    val snackbarOverlayColor = colors.onSurface.copy(alpha = SnackbarOverlayAlpha)
    val snackbarColor = snackbarOverlayColor.compositeOver(colors.surface)
    Surface(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        color = snackbarColor,
        contentColor = colors.surface
    ) {
        ProvideEmphasis(EmphasisAmbient.current.high) {
            val textStyle = MaterialTheme.typography.body2
            ProvideTextStyle(value = textStyle) {
                when {
                    action == null -> TextOnlySnackbar(text)
                    actionOnNewLine -> NewLineButtonSnackbar(text, action)
                    else -> OneRowSnackbar(text, action)
                }
            }
        }
    }
}

@Composable
private fun TextOnlySnackbar(text: @Composable () -> Unit) {
    Layout(
        text,
        modifier = Modifier.padding(
            start = HorizontalSpacing,
            end = HorizontalSpacing,
            top = SnackbarVerticalPadding,
            bottom = SnackbarVerticalPadding
        )
    ) { measurables, constraints ->
        require(measurables.size == 1) {
            "text for Snackbar expected to have exactly only one child"
        }
        val textPlaceable = measurables.first().measure(constraints)
        val firstBaseline = textPlaceable[FirstBaseline]
        val lastBaseline = textPlaceable[LastBaseline]
        require(firstBaseline != AlignmentLine.Unspecified) { "No baselines for text" }
        require(lastBaseline != AlignmentLine.Unspecified) { "No baselines for text" }

        val minHeight =
            if (firstBaseline == lastBaseline) {
                SnackbarMinHeightOneLine
            } else {
                SnackbarMinHeightTwoLines
            }
        val containerHeight = max(minHeight.toIntPx(), textPlaceable.height)
        layout(constraints.maxWidth, containerHeight) {
            val textPlaceY = (containerHeight - textPlaceable.height) / 2
            textPlaceable.place(0, textPlaceY)
        }
    }
}

@Composable
private fun NewLineButtonSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(
                start = HorizontalSpacing,
                end = HorizontalSpacingButtonSide,
                bottom = SeparateButtonExtraY
            )
    ) {
        Box(
            Modifier
                .relativePaddingFrom(LastBaseline, after = LongButtonVerticalOffset)
                .relativePaddingFrom(FirstBaseline, before = HeightToFirstLine)
                .padding(end = HorizontalSpacingButtonSide),
            children = text
        )
        Box(Modifier.gravity(Alignment.End), children = action)
    }
}

@Composable
private fun OneRowSnackbar(
    text: @Composable () -> Unit,
    action: @Composable () -> Unit
) {
    val textTag = "text"
    val actionTag = "action"
    Layout(
        {
            Box(Modifier.layoutId(textTag), children = text)
            Box(Modifier.layoutId(actionTag), children = action)
        },
        modifier = Modifier.padding(
            start = HorizontalSpacing,
            end = HorizontalSpacingButtonSide,
            top = SnackbarVerticalPadding,
            bottom = SnackbarVerticalPadding
        )
    ) { measurables, constraints ->
        val buttonPlaceable = measurables.first { it.id == actionTag }.measure(constraints)
        val textMaxWidth =
            (constraints.maxWidth - buttonPlaceable.width - TextEndExtraSpacing.toIntPx())
                .coerceAtLeast(constraints.minWidth)
        val textPlaceable = measurables.first { it.id == textTag }.measure(
            constraints.copy(minHeight = 0, maxWidth = textMaxWidth)
        )

        val firstTextBaseline = textPlaceable[FirstBaseline]
        require(firstTextBaseline != AlignmentLine.Unspecified) { "No baselines for text" }
        val lastTextBaseline = textPlaceable[LastBaseline]
        require(lastTextBaseline != AlignmentLine.Unspecified) { "No baselines for text" }
        val isOneLine = firstTextBaseline == lastTextBaseline
        val buttonPlaceX = constraints.maxWidth - buttonPlaceable.width

        val textPlaceY: Int
        val containerHeight: Int
        val buttonPlaceY: Int
        if (isOneLine) {
            val minContainerHeight = SnackbarMinHeightOneLine.toIntPx()
            val contentHeight = buttonPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            textPlaceY = (containerHeight - textPlaceable.height) / 2
            val buttonBaseline = buttonPlaceable[FirstBaseline]
            buttonPlaceY = buttonBaseline.let {
                if (it != AlignmentLine.Unspecified) {
                    textPlaceY + firstTextBaseline - it
                } else {
                    0
                }
            }
        } else {
            val baselineOffset = HeightToFirstLine.toIntPx()
            textPlaceY = baselineOffset - firstTextBaseline - SnackbarVerticalPadding.toIntPx()
            val minContainerHeight = SnackbarMinHeightTwoLines.toIntPx()
            val contentHeight = textPlaceY + textPlaceable.height
            containerHeight = max(minContainerHeight, contentHeight)
            buttonPlaceY = (containerHeight - buttonPlaceable.height) / 2
        }

        layout(constraints.maxWidth, containerHeight) {
            textPlaceable.place(0, textPlaceY)
            buttonPlaceable.place(buttonPlaceX, buttonPlaceY)
        }
    }
}

/**
 * Provides a best-effort 'primary' color to be used as the primary color inside a [Snackbar].
 * Given that [Snackbar]s have an 'inverted' theme, i.e in a light theme they appear dark, and
 * in a dark theme they appear light, just using [ColorPalette.primary] will not work, and has
 * incorrect contrast.
 *
 * If your light theme has a corresponding dark theme, you should instead directly use
 * [ColorPalette.primary] from the dark theme when in a light theme, and use
 * [ColorPalette.primaryVariant] from the dark theme when in a dark theme.
 *
 * When in a light theme, this function applies a color overlay to [ColorPalette.primary] from
 * [colors] to attempt to reduce the contrast, and when in a dark theme this function uses
 * [ColorPalette.primaryVariant].
 *
 * @param colors the [ColorPalette] to calculate the Snackbar primary color for
 */
fun snackbarPrimaryColorFor(colors: ColorPalette): Color {
    return if (colors.isLight) {
        val primary = colors.primary
        val overlayColor = colors.surface.copy(alpha = 0.6f)

        overlayColor.compositeOver(primary)
    } else {
        colors.primaryVariant
    }
}

private const val SnackbarOverlayAlpha = 0.8f

private val HeightToFirstLine = 30.dp
private val HorizontalSpacing = 16.dp
private val HorizontalSpacingButtonSide = 8.dp
private val SeparateButtonExtraY = 8.dp
private val SnackbarVerticalPadding = 6.dp
private val TextEndExtraSpacing = 8.dp
private val LongButtonVerticalOffset = 18.dp
private val SnackbarMinHeightOneLine = 48.dp - SnackbarVerticalPadding * 2
private val SnackbarMinHeightTwoLines = 68.dp - SnackbarVerticalPadding * 2