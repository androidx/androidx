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
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Dialog
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.material.AlertDialogButtonLayout.SideBySide
import androidx.ui.material.AlertDialogButtonLayout.Stacked
import androidx.ui.material.surface.Surface
import androidx.ui.unit.dp

/**
 * Alert dialog is a [Dialog] which interrupts the user with urgent information, details or actions.
 *
 * There are two different layouts for showing the buttons inside the Alert dialog provided by
 * [AlertDialogButtonLayout].
 *
 * Sample of dialog with side by side buttons:
 *
 * @sample androidx.ui.material.samples.SideBySideAlertDialogSample
 *
 * Sample of dialog with stacked buttons:
 *
 * @sample androidx.ui.material.samples.StackedAlertDialogSample
 *
 * @param onCloseRequest Executes when the user tries to dismiss the Dialog by clicking outside
 * or pressing the back button.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.h6].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body1].
 * @param confirmButton A button which is meant to confirm a proposed action, thus resolving
 * what triggered the dialog.
 * @param dismissButton A button which is meant to dismiss the dialog.
 * @param buttonLayout An enum which specifies how the buttons are positioned inside the dialog:
 * SideBySide or Stacked.
 *
 */
@Composable
fun AlertDialog(
    onCloseRequest: () -> Unit,
    title: @Composable() (() -> Unit)? = null,
    text: @Composable() () -> Unit,
    confirmButton: @Composable() () -> Unit,
    dismissButton: @Composable() (() -> Unit)? = null,
    buttonLayout: AlertDialogButtonLayout = SideBySide
) {
    AlertDialog(
        onCloseRequest = onCloseRequest,
        title = title,
        text = text,
        buttons = {
            AlertDialogButtonLayout(
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                buttonLayout = buttonLayout
            )
        }
    )
}

/**
 * Alert dialog is a [Dialog] which interrupts the user with urgent information, details or actions.
 *
 * This function can be used to fully customize the button area, e.g. with:
 *
 * @sample androidx.ui.material.samples.CustomAlertDialogSample
 *
 * @param onCloseRequest Executes when the user tries to dismiss the Dialog by clicking outside
 * or pressing the back button.
 * @param title The title of the Dialog which should specify the purpose of the Dialog. The title
 * is not mandatory, because there may be sufficient information inside the [text]. Provided text
 * style will be [Typography.h6].
 * @param text The text which presents the details regarding the Dialog's purpose. Provided text
 * style will be [Typography.body1].
 * @param buttons Function that emits the layout with the buttons
 */
@Composable
fun AlertDialog(
    onCloseRequest: () -> Unit,
    title: (@Composable() () -> Unit)? = null,
    text: @Composable() () -> Unit,
    buttons: @Composable() () -> Unit
) {
    // TODO: Find a cleaner way to pass the properties of the MaterialTheme
    val currentColors = MaterialTheme.colors()
    val currentTypography = MaterialTheme.typography()
    Dialog(onCloseRequest = onCloseRequest) {
        MaterialTheme(colors = currentColors, typography = currentTypography) {
            Surface(modifier = LayoutWidth(AlertDialogWidth), shape = AlertDialogShape) {
                Column {
                    if (title != null) {
                        Box(modifier = TitlePadding + LayoutGravity.Start) {
                            val textStyle = MaterialTheme.typography().h6
                            CurrentTextStyleProvider(textStyle, title)
                        }
                    } else {
                        // TODO(b/138924683): Temporary until padding for the Text's
                        //  baseline
                        Spacer(LayoutHeight(NoTitleExtraHeight))
                    }

                    Box(modifier = TextPadding + LayoutGravity.Start) {
                        val textStyle = MaterialTheme.typography().body1
                        CurrentTextStyleProvider(textStyle, text)
                    }
                    Spacer(LayoutHeight(TextToButtonsHeight))
                    buttons()
                }
            }
        }
    }
}

// TODO(b/138925106): Add Auto mode when the flow layout is implemented
/**
 * An enum which specifies how the buttons are positioned inside the [AlertDialog]:
 *
 * [SideBySide] - positions the dismiss button on the left side of the confirm button.
 * [Stacked] - positions the dismiss button below the confirm button.
 */
enum class AlertDialogButtonLayout {
    SideBySide,
    Stacked
}

@Composable
private fun AlertDialogButtonLayout(
    confirmButton: @Composable() () -> Unit,
    dismissButton: @Composable() (() -> Unit)?,
    buttonLayout: AlertDialogButtonLayout
) {
    Box(LayoutWidth.Fill + ButtonsPadding, gravity = ContentGravity.CenterRight) {
        if (buttonLayout == SideBySide) {
            Row(arrangement = Arrangement.End) {
                if (dismissButton != null) {
                    dismissButton()
                    Spacer(LayoutWidth(ButtonsWidthSpace))
                }

                confirmButton()
            }
        } else {
            Column {
                confirmButton()

                if (dismissButton != null) {
                    Spacer(LayoutHeight(ButtonsHeightSpace))
                    dismissButton()
                }
            }
        }
    }
}

private val AlertDialogWidth = 280.dp
private val ButtonsPadding = LayoutPadding(all = 8.dp)
private val ButtonsWidthSpace = 8.dp
private val ButtonsHeightSpace = 12.dp
// TODO(b/138924683): Top padding should be actually be a distance between the Text baseline and
//  the Title baseline
private val TextPadding = LayoutPadding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 0.dp)
// TODO(b/138924683): Top padding should be actually be relative to the Text baseline
private val TitlePadding = LayoutPadding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 0.dp)
// The height difference of the padding between a Dialog with a title and one without a title
private val NoTitleExtraHeight = 2.dp
private val TextToButtonsHeight = 28.dp
// TODO: The corner radius should be customizable
private val AlertDialogShape = RoundedCornerShape(4.dp)