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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.foundation.Text
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Row
import androidx.ui.material.AlertDialog
import androidx.ui.material.AlertDialogButtonLayout
import androidx.ui.material.Button

@Sampled
@Composable
fun SideBySideAlertDialogSample() {
    val openDialog = state { true }

    if (openDialog.value) {
        AlertDialog(
            onCloseRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onCloseRequest.
                openDialog.value = false
            },
            title = {
                Text(text = "Title")
            },
            text = {
                Text("This area typically contains the supportive text" +
                        " which presents the details regarding the Dialog's purpose.")
            },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Dismiss")
                }
            },
            buttonLayout = AlertDialogButtonLayout.SideBySide
        )
    }
}

@Sampled
@Composable
fun StackedAlertDialogSample() {
    val openDialog = state { true }

    if (openDialog.value) {
        AlertDialog(
            onCloseRequest = {
                // In this example we allow the dialog to be closed by other actions
                // such as taping outside or pressing the back button.
                openDialog.value = false
            },
            title = {
                Text(text = "Title")
            },
            text = {
                Text("This area typically contains the supportive text" +
                        " which presents the details regarding the Dialog's purpose.")
            },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Long Confirm Button")
                }
            },
            dismissButton = {
                Button(onClick = {
                    openDialog.value = false
                }) {
                    Text("Long Dismiss Button")
                }
            },
            buttonLayout = AlertDialogButtonLayout.Stacked
        )
    }
}

@Sampled
@Composable
fun CustomAlertDialogSample() {
    val openDialog = state { true }

    if (openDialog.value) {
        AlertDialog(
            onCloseRequest = {
                openDialog.value = false
            },
            title = {
                Text(text = "Title")
            },
            text = {
                Text("This area typically contains the supportive text" +
                        " which presents the details regarding the Dialog's purpose.")
            },
            buttons = {
                Row(horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { openDialog.value = false }) {
                        Text("Button")
                    }
                }
            }
        )
    }
}