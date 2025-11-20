/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.internal.Icons
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.jvm.JvmInline
import kotlin.math.truncate

/**
 * [Material Design time picker dialog](https://m3.material.io/components/time-pickers/overview)
 *
 * A dialog for displaying a [TimePicker]. Time pickers let people select a time.
 *
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton button which is meant to confirm a proposed action, thus resolving what
 *   triggered the dialog. The dialog does not set up any events for this button, nor does it
 *   control its enablement, so those need to be set up by the caller.
 * @param title The title to be displayed on top of the dialog.
 * @param modifier the [Modifier] to be applied to this dialog's content.
 * @param properties typically platform specific properties to further configure the dialog
 * @param modeToggleButton Optional toggle to switch between clock and text input modes.
 * @param dismissButton button which is meant to dismiss the dialog. The dialog does not set up any
 *   events for this button so they need to be set up by the caller.
 * @param shape defines the dialog's surface shape as well its shadow
 * @param containerColor the color of the dialog's container
 * @param content the content of the dialog (i.e. a [TimePicker], for example)
 */
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    modeToggleButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = TimePickerDialogDefaults.shape,
    containerColor: Color = TimePickerDialogDefaults.containerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        TimePickerDialogLayout(
            shape = shape,
            modifier = modifier,
            containerColor = containerColor,
            title = title,
            content = content,
            modeToggleButton = modeToggleButton,
            dismissButton = dismissButton,
            confirmButton = confirmButton,
        )
    }
}

@Composable
internal fun TimePickerDialogLayout(
    confirmButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    modeToggleButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = TimePickerDialogDefaults.shape,
    containerColor: Color = TimePickerDialogDefaults.containerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = shape,
        tonalElevation = DialogTokens.ContainerElevation,
        modifier = modifier,
        color = containerColor,
    ) {
        TimePickerCustomLayout(
            title = title,
            actions = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    modeToggleButton?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            },
            content = content,
        )
    }
}

@Composable
internal fun TimePickerCustomLayout(
    title: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val content =
        @Composable {
            Box(modifier = Modifier.layoutId("title")) { title() }
            Box(modifier = Modifier.layoutId("actions")) { actions() }
            Column(modifier = Modifier.layoutId("timePickerContent"), content = content)
        }

    val measurePolicy = MeasurePolicy { measurables, constraints ->
        val titleMeasurable = measurables.fastFirst { it.layoutId == "title" }
        val contentMeasurable = measurables.fastFirst { it.layoutId == "timePickerContent" }
        val actionsMeasurable = measurables.fastFirst { it.layoutId == "actions" }

        val contentPadding = 24.dp.roundToPx()
        val landMaxDialogHeight = 384.dp.roundToPx()
        val landTitleTopPadding = 24.dp.roundToPx()
        val landContentTopPadding = 16.dp.roundToPx()
        val landContentActionsPadding = 4.dp.roundToPx()
        val landActionsBottomPadding = 8.dp.roundToPx()

        val portTitleTopPadding = 24.dp.roundToPx()
        val portActionsBottomPadding = 24.dp.roundToPx()

        val contentPlaceable = contentMeasurable.measure(constraints.copy(minHeight = 0))

        // Input mode will be smaller than the smallest TimePickerContent (currently 200.dp)
        // But will always use portrait layout for correctness.
        val isLandscape =
            contentPlaceable.width > contentPlaceable.height &&
                contentPlaceable.height >= truncate(ClockDialMinContainerSize.toPx())

        val dialogWidth =
            if (isLandscape) {
                contentPlaceable.width + contentPadding * 2
            } else {
                contentPlaceable.width + contentPadding * 2
            }

        val actionsPlaceable =
            actionsMeasurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentPlaceable.width)
            )

        val titlePlaceable =
            titleMeasurable.measure(
                constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentPlaceable.width)
            )

        val layoutHeight =
            if (isLandscape) {
                val contentTotalHeight =
                    contentPlaceable.height +
                        actionsPlaceable.height +
                        landActionsBottomPadding +
                        landContentTopPadding +
                        landContentActionsPadding
                if (constraints.hasBoundedHeight) constraints.maxHeight else contentTotalHeight
            } else {
                portTitleTopPadding +
                    titlePlaceable.height +
                    contentPlaceable.height +
                    actionsPlaceable.height +
                    portActionsBottomPadding
            }

        layout(width = dialogWidth, height = layoutHeight) {
            if (isLandscape) {
                val contentHeight =
                    landContentTopPadding +
                        contentPlaceable.height +
                        landContentActionsPadding +
                        actionsPlaceable.height +
                        landActionsBottomPadding
                val remainingSpace = layoutHeight - contentHeight
                val adjustedActionsBottomPadding =
                    if (layoutHeight >= landMaxDialogHeight) {
                        16.dp.roundToPx()
                    } else {
                        0
                    }

                titlePlaceable.place(x = landTitleTopPadding, y = landTitleTopPadding)
                val timePickerContentX = contentPadding
                val timePickerContentY = landContentTopPadding + remainingSpace / 2
                contentPlaceable.place(x = timePickerContentX, y = timePickerContentY)
                val actionsY =
                    timePickerContentY + contentPlaceable.height + landContentActionsPadding -
                        adjustedActionsBottomPadding + remainingSpace / 2
                actionsPlaceable.place(x = timePickerContentX, y = actionsY)
            } else {
                val titleX = landTitleTopPadding
                titlePlaceable.place(x = titleX, y = portTitleTopPadding)

                val contentX = (dialogWidth - contentPlaceable.width) / 2
                val contentY = portTitleTopPadding + titlePlaceable.height
                contentPlaceable.place(x = contentX, y = contentY)

                val actionsX = (dialogWidth - actionsPlaceable.width) / 2
                val actionsY = contentY + contentPlaceable.height
                actionsPlaceable.place(x = actionsX, y = actionsY)
            }
        }
    }

    Layout(content = content, measurePolicy = measurePolicy)
}

/** Default properties for a [TimePickerDialog] */
object TimePickerDialogDefaults {

    /** Container color for [TimePickerDialog] */
    val containerColor
        @Composable get() = DialogTokens.ContainerColor.value

    /** Shape color for [TimePickerDialog] */
    val shape
        @Composable get() = DialogTokens.ContainerShape.value

    /** Min Screen Height required to display a TimePicker in Picker in mode */
    val MinHeightForTimePicker: Dp = 300.dp

    /**
     * A button that toggles the display mode of the time picker between
     * [TimePickerDisplayMode.Picker] and [TimePickerDisplayMode.Input].
     *
     * @param onDisplayModeChange called when the button is clicked
     * @param displayMode the current display mode of the time picker
     * @param modifier the [Modifier] to be applied to this button
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DisplayModeToggle(
        onDisplayModeChange: () -> Unit,
        displayMode: TimePickerDisplayMode,
        modifier: Modifier = Modifier,
    ) {
        val contentDescription =
            getString(
                if (displayMode == TimePickerDisplayMode.Picker) {
                    Strings.TimePickerToggleKeyboard
                } else {
                    Strings.TimePickerToggleTouch
                }
            )
        TooltipBox(
            positionProvider =
                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(contentDescription) } },
            state = rememberTooltipState(),
        ) {
            IconButton(modifier = modifier, onClick = onDisplayModeChange) {
                val icon =
                    if (displayMode == TimePickerDisplayMode.Picker) {
                        Icons.Outlined.Keyboard
                    } else {
                        Icons.Outlined.Schedule
                    }
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
        }
    }

    /**
     * The title of the time picker dialog.
     *
     * @param modifier the [Modifier] to be applied to this title
     * @param displayMode the current display mode of the time picker
     */
    @Composable
    fun Title(displayMode: TimePickerDisplayMode, modifier: Modifier = Modifier) {
        Text(
            modifier = modifier.padding(bottom = 20.dp),
            style = MaterialTheme.typography.labelMedium,
            text =
                getString(
                    if (displayMode == TimePickerDisplayMode.Picker) {
                        Strings.TimePickerDialogTitle
                    } else {
                        Strings.TimeInputDialogTitle
                    }
                ),
        )
    }
}

/**
 * Represents the display mode for the content of a [TimePickerDialog].
 *
 * Defines the different ways a user can interact with the time picker, such as using a visual
 * clock-like picker or entering the time via text input.
 */
@Immutable
@JvmInline
value class TimePickerDisplayMode internal constructor(internal val value: Int) {

    companion object {
        /** Time picker input mode */
        val Picker = TimePickerDisplayMode(0)

        /** Time text input mode */
        val Input = TimePickerDisplayMode(1)
    }

    override fun toString() =
        when (this) {
            Picker -> "Picker"
            Input -> "Input"
            else -> "Unknown"
        }
}
