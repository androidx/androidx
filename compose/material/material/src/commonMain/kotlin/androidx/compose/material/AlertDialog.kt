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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

/**
 * <a href="https://material.io/components/dialogs#alert-dialog" class="external"
 * target="_blank">Material Design alert dialog</a>.
 *
 * Alert dialogs interrupt users with urgent information, details, or actions.
 *
 * ![Dialogs
 * image](https://developer.android.com/images/reference/androidx/compose/material/dialogs.png)
 *
 * The dialog will position its buttons based on the available space. By default it will try to
 * place them horizontally next to each other and fallback to horizontal placement if not enough
 * space is available. There is also another version of this composable that has a slot for buttons
 * to provide custom buttons layout.
 *
 * Sample of dialog:
 *
 * @sample androidx.compose.material.samples.AlertDialogSample
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving what
 *   triggered the dialog. The dialog does not set up any events for this button so they need to be
 *   set up by the caller.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog. The dialog does not set up
 *   any events for this button so they need to be set up by the caller.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title is
 *   not mandatory, because there may be sufficient information inside the [text]. Provided text
 *   style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 *   style will be [Typography.body2].
 * @param shape Defines the Dialog's shape
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param properties Typically platform specific properties to further configure the dialog.
 */
@Composable
expect fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
)

/**
 * <a href="https://material.io/components/dialogs#alert-dialog" class="external"
 * target="_blank">Material Design alert dialog</a>.
 *
 * Alert dialogs interrupt users with urgent information, details, or actions.
 *
 * ![Dialogs
 * image](https://developer.android.com/images/reference/androidx/compose/material/dialogs.png)
 *
 * This function can be used to fully customize the button area, e.g. with:
 *
 * @sample androidx.compose.material.samples.CustomAlertDialogSample
 * @param onDismissRequest Executes when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param buttons Function that emits the layout with the buttons.
 * @param modifier Modifier to be applied to the layout of the dialog.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title is
 *   not mandatory, because there may be sufficient information inside the [text]. Provided text
 *   style will be [Typography.subtitle1].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 *   style will be [Typography.body2].
 * @param shape Defines the Dialog's shape.
 * @param backgroundColor The background color of the dialog.
 * @param contentColor The preferred content color provided by this dialog to its children.
 * @param properties Typically platform specific properties to further configure the dialog.
 */
@Composable
expect fun AlertDialog(
    onDismissRequest: () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
)

@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun AlertDialogImpl(
    noinline onDismissRequest: () -> Unit,
    noinline confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    noinline dismissButton: @Composable (() -> Unit)?,
    noinline title: @Composable (() -> Unit)?,
    noinline text: @Composable (() -> Unit)?,
    shape: Shape,
    backgroundColor: Color,
    contentColor: Color,
    properties: DialogProperties
): Unit =
    AlertDialog(
        onDismissRequest = onDismissRequest,
        buttons = {
            // TODO: move the modifiers to FlowRow when it supports a modifier parameter
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
                AlertDialogFlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 12.dp) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        },
        modifier = modifier,
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )

@Suppress("NOTHING_TO_INLINE")
@Composable
internal inline fun AlertDialogImpl(
    noinline onDismissRequest: () -> Unit,
    noinline buttons: @Composable () -> Unit,
    modifier: Modifier,
    noinline title: (@Composable () -> Unit)?,
    noinline text: @Composable (() -> Unit)?,
    shape: Shape,
    backgroundColor: Color,
    contentColor: Color,
    properties: DialogProperties
): Unit =
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        AlertDialogContent(
            buttons = buttons,
            modifier = modifier,
            title = title,
            text = text,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

@Composable
internal fun AlertDialogContent(
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Column {
            AlertDialogBaselineLayout(
                title =
                    title?.let {
                        @Composable {
                            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                                val textStyle = MaterialTheme.typography.subtitle1
                                ProvideTextStyle(textStyle, title)
                            }
                        }
                    },
                text =
                    text?.let {
                        @Composable {
                            CompositionLocalProvider(
                                LocalContentAlpha provides ContentAlpha.medium
                            ) {
                                val textStyle = MaterialTheme.typography.body2
                                ProvideTextStyle(textStyle, text)
                            }
                        }
                    }
            )
            buttons()
        }
    }
}

/**
 * Layout that will add spacing between the top of the layout and [title]'s first baseline, and
 * [title]'s last baseline and [text]'s first baseline.
 *
 * If [title] and/or [text] do not have any baselines, the spacing will just be applied from the
 * edge of their layouts instead as a best effort implementation.
 */
@Composable
internal fun ColumnScope.AlertDialogBaselineLayout(
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?
) {
    Layout(
        {
            title?.let { title ->
                Box(TitlePadding.layoutId("title").align(Alignment.Start)) { title() }
            }
            text?.let { text ->
                Box(TextPadding.layoutId("text").align(Alignment.Start)) { text() }
            }
        },
        Modifier.weight(1f, false)
    ) { measurables, constraints ->
        // Measure with loose constraints for height as we don't want the text to take up more
        // space than it needs
        val titlePlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == "title" }
                ?.measure(constraints.copy(minHeight = 0))
        val textPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == "text" }
                ?.measure(constraints.copy(minHeight = 0))

        val layoutWidth = max(titlePlaceable?.width ?: 0, textPlaceable?.width ?: 0)

        val firstTitleBaseline =
            titlePlaceable?.get(FirstBaseline)?.let { baseline ->
                if (baseline == AlignmentLine.Unspecified) null else baseline
            } ?: 0
        val lastTitleBaseline =
            titlePlaceable?.get(LastBaseline)?.let { baseline ->
                if (baseline == AlignmentLine.Unspecified) null else baseline
            } ?: 0

        val titleOffset = TitleBaselineDistanceFromTop.roundToPx()

        // Place the title so that its first baseline is titleOffset from the top
        val titlePositionY = titleOffset - firstTitleBaseline

        val firstTextBaseline =
            textPlaceable?.get(FirstBaseline)?.let { baseline ->
                if (baseline == AlignmentLine.Unspecified) null else baseline
            } ?: 0

        val textOffset =
            if (titlePlaceable == null) {
                TextBaselineDistanceFromTop.roundToPx()
            } else {
                TextBaselineDistanceFromTitle.roundToPx()
            }

        // Combined height of title and spacing above
        val titleHeightWithSpacing = titlePlaceable?.let { it.height + titlePositionY } ?: 0

        // Align the bottom baseline of the text with the bottom baseline of the title, and then
        // add the offset
        val textPositionY =
            if (titlePlaceable == null) {
                // If there is no title, just place the text offset from the top of the dialog
                textOffset - firstTextBaseline
            } else {
                if (lastTitleBaseline == 0) {
                    // If `title` has no baseline, just place the text's baseline textOffset from
                    // the
                    // bottom of the title
                    titleHeightWithSpacing - firstTextBaseline + textOffset
                } else {
                    // Otherwise place the text's baseline textOffset from the title's last baseline
                    (titlePositionY + lastTitleBaseline) - firstTextBaseline + textOffset
                }
            }

        // Combined height of text and spacing above
        val textHeightWithSpacing =
            textPlaceable?.let {
                if (lastTitleBaseline == 0) {
                    textPlaceable.height + textOffset - firstTextBaseline
                } else {
                    textPlaceable.height + textOffset -
                        firstTextBaseline -
                        ((titlePlaceable?.height ?: 0) - lastTitleBaseline)
                }
            } ?: 0

        val layoutHeight = titleHeightWithSpacing + textHeightWithSpacing

        layout(layoutWidth, layoutHeight) {
            titlePlaceable?.place(0, titlePositionY)
            textPlaceable?.place(0, textPositionY)
        }
    }
}

/**
 * Simple clone of FlowRow that arranges its children in a horizontal flow with limited
 * customization.
 */
@Composable
internal fun AlertDialogFlowRow(
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    content: @Composable () -> Unit
) {
    Layout(content) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        val childConstraints = Constraints(maxWidth = constraints.maxWidth)

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() ||
                currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width <=
                    constraints.maxWidth

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            // Ensures that confirming actions appear above dismissive actions.
            @Suppress("ListIterator") sequences.add(0, currentSequence.toList())
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        measurables.fastForEach { measurable ->
            // Ask the child for its preferred size.
            val placeable = measurable.measure(childConstraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize =
            if (constraints.maxWidth != Constraints.Infinity) {
                constraints.maxWidth
            } else {
                max(mainAxisSpace, constraints.minWidth)
            }
        val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

        val layoutWidth = mainAxisLayoutSize

        val layoutHeight = crossAxisLayoutSize

        layout(layoutWidth, layoutHeight) {
            sequences.fastForEachIndexed { i, placeables ->
                val childrenMainAxisSizes =
                    IntArray(placeables.size) { j ->
                        placeables[j].width +
                            if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                    }
                val arrangement = Arrangement.Bottom
                // TODO(soboleva): rtl support
                // Handle vertical direction
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size)
                with(arrangement) {
                    arrange(mainAxisLayoutSize, childrenMainAxisSizes, mainAxisPositions)
                }
                placeables.fastForEachIndexed { j, placeable ->
                    placeable.place(x = mainAxisPositions[j], y = crossAxisPositions[i])
                }
            }
        }
    }
}

private val TitlePadding = Modifier.padding(start = 24.dp, end = 24.dp)
private val TextPadding = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
// Baseline distance from the first line of the title to the top of the dialog
private val TitleBaselineDistanceFromTop = 40.sp
// Baseline distance from the first line of the text to the last line of the title
private val TextBaselineDistanceFromTitle = 36.sp
// For dialogs with no title, baseline distance from the first line of the text to the top of the
// dialog
private val TextBaselineDistanceFromTop = 38.sp
