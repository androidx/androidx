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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.internal.Icons
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.jvm.JvmInline

/**
 * <a href="https://m3.material.io/components/time-pickers/overview" class="external"
 * target="_blank">Material Design time picker dialog</a>.
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
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            shape = shape,
            tonalElevation = DialogTokens.ContainerElevation,
            modifier =
                modifier.width(IntrinsicSize.Min).background(shape = shape, color = containerColor),
        ) {
            Column(
                modifier = Modifier.padding(TimePickerDialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                title()
                content()
                Row(modifier = Modifier.height(TimePickerActionsHeight).fillMaxWidth()) {
                    modeToggleButton?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    confirmButton()
                    dismissButton?.invoke()
                }
            }
        }
    }
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
    val MinHeightForTimePicker: Dp = 400.dp

    /**
     * A button that toggles the display mode of the time picker between
     * [TimePickerDisplayMode.Picker] and [TimePickerDisplayMode.Input].
     *
     * @param onDisplayModeChange called when the button is clicked
     * @param displayMode the current display mode of the time picker
     * @param modifier the [Modifier] to be applied to this button
     */
    @Composable
    fun DisplayModeToggle(
        onDisplayModeChange: () -> Unit,
        displayMode: TimePickerDisplayMode,
        modifier: Modifier = Modifier,
    ) {
        IconButton(modifier = modifier, onClick = onDisplayModeChange) {
            val icon =
                if (displayMode == TimePickerDisplayMode.Picker) {
                    Icons.Outlined.Keyboard
                } else {
                    Icons.Outlined.Schedule
                }

            Icon(
                imageVector = icon,
                contentDescription =
                    getString(
                        if (displayMode == TimePickerDisplayMode.Picker) {
                            Strings.TimePickerToggleTouch
                        } else {
                            Strings.TimePickerToggleKeyboard
                        }
                    )
            )
        }
    }

    /**
     * The title of the time picker dialog.
     *
     * @param modifier the [Modifier] to be applied to this title
     * @param displayMode the current display mode of the time picker
     */
    @Composable
    fun Title(
        displayMode: TimePickerDisplayMode,
        modifier: Modifier = Modifier,
    ) {
        Text(
            modifier = modifier.fillMaxWidth().padding(bottom = 20.dp),
            style = MaterialTheme.typography.labelMedium,
            text =
                getString(
                    if (displayMode == TimePickerDisplayMode.Picker) {
                        Strings.TimePickerDialogTitle
                    } else {
                        Strings.TimeInputDialogTitle
                    }
                )
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

internal val TimePickerDialogPadding = 24.dp
internal val TimePickerActionsHeight = 40.dp
