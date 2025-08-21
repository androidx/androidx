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

package androidx.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.tokens.DatePickerModalTokens
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.DialogProperties

/**
 * [Material Design date picker dialog](https://m3.material.io/components/date-pickers/overview)
 *
 * A dialog for displaying a [DatePicker]. Date pickers let people select a date.
 *
 * A sample for displaying a [DatePicker] in a dialog:
 *
 * @sample androidx.compose.material3.samples.DatePickerDialogSample
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param confirmButton button which is meant to confirm a proposed action, thus resolving what
 *   triggered the dialog. The dialog does not set up any events for this button, nor does it
 *   control its enablement, so those need to be set up by the caller.
 * @param modifier the [Modifier] to be applied to this dialog's content.
 * @param dismissButton button which is meant to dismiss the dialog. The dialog does not set up any
 *   events for this button so they need to be set up by the caller.
 * @param shape defines the dialog's surface shape as well its shadow
 * @param tonalElevation when [DatePickerColors.containerColor] is [ColorScheme.surface], a higher
 *   the elevation will result in a darker color in light theme and lighter color in dark theme
 * @param colors [DatePickerColors] that will be used to resolve the colors used for this date
 *   picker in different states. See [DatePickerDefaults.colors].
 * @param properties typically platform specific properties to further configure the dialog
 * @param content the content of the dialog (i.e. a [DatePicker], for example)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    shape: Shape,
    tonalElevation: Dp,
    colors: DatePickerColors,
    properties: DialogProperties,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.wrapContentHeight(),
        properties = properties,
    ) {
        Surface(
            modifier =
                Modifier.requiredWidth(DatePickerModalTokens.ContainerWidth)
                    .heightIn(max = DatePickerModalTokens.ContainerHeight),
            shape = shape,
            color = colors.containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                // Wrap the content with a Box and Modifier.weight(1f) to ensure that any "confirm"
                // and "dismiss" buttons are not pushed out of view when running on small screens,
                // or when nesting a DateRangePicker.
                // Fill is false to support collapsing the dialog's height when switching to input
                // mode.
                Box(Modifier.weight(1f, fill = false)) { this@Column.content() }
                // Buttons
                Box(modifier = Modifier.align(Alignment.End).padding(DialogButtonsPadding)) {
                    ProvideContentColorTextStyle(
                        contentColor = DialogTokens.ActionLabelTextColor.value,
                        textStyle = DialogTokens.ActionLabelTextFont.value,
                    ) {
                        val buttonPaddingFromMICS =
                            LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                                ButtonDefaults.MinHeight
                        AlertDialogFlowRow(
                            mainAxisSpacing = DialogButtonsMainAxisSpacing,
                            crossAxisSpacing =
                                (DialogButtonsCrossAxisSpacing - buttonPaddingFromMICS).coerceIn(
                                    0.dp,
                                    DialogButtonsCrossAxisSpacing,
                                ),
                        ) {
                            confirmButton()
                            dismissButton?.invoke()
                        }
                    }
                }
            }
        }
    }
}

private val DialogButtonsPadding = PaddingValues(bottom = 8.dp, end = 6.dp)
private val DialogButtonsMainAxisSpacing = 8.dp
private val DialogButtonsCrossAxisSpacing = 8.dp
