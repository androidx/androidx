/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.integration.macrobenchmark.target.material3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Confirmation
import androidx.wear.compose.material3.ConfirmationDefaults
import androidx.wear.compose.material3.FailureConfirmation
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SuccessConfirmation
import androidx.wear.compose.material3.Text

class DialogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val dialogType = remember { mutableStateOf(DialogType.NONE) }
                val showDialog = remember { mutableStateOf(true) }
                AppScaffold {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                dialogType.value = DialogType.ALERT
                                showDialog.value = true
                            },
                            modifier = Modifier.semantics { contentDescription = OPEN_ALERT_DIALOG }
                        ) {
                            Text("Open AlertDialog")
                        }
                        Button(
                            onClick = {
                                dialogType.value = DialogType.CONFIRM
                                showDialog.value = true
                            },
                            modifier =
                                Modifier.semantics { contentDescription = OPEN_CONFIRM_DIALOG }
                        ) {
                            Text("Open Confirmation")
                        }
                        Button(
                            onClick = {
                                dialogType.value = DialogType.SUCCESS
                                showDialog.value = true
                            },
                            modifier =
                                Modifier.semantics { contentDescription = OPEN_SUCCESS_DIALOG }
                        ) {
                            Text("Open Success")
                        }
                        Button(
                            onClick = {
                                dialogType.value = DialogType.FAILURE
                                showDialog.value = true
                            },
                            modifier =
                                Modifier.semantics { contentDescription = OPEN_FAILURE_DIALOG }
                        ) {
                            Text("Open Failure")
                        }
                    }
                    CustomDialog(dialogType, showDialog)
                }
            }
        }
    }

    @Composable
    fun CustomDialog(dialogType: MutableState<DialogType>, showDialog: MutableState<Boolean>) {
        when (dialogType.value) {
            DialogType.NONE -> {}
            DialogType.ALERT -> {
                AlertDialog(
                    show = showDialog.value,
                    onDismissRequest = { showDialog.value = false },
                    title = { Text("Title") },
                    confirmButton = {
                        Button(
                            modifier = Modifier.semantics { contentDescription = DIALOG_CONFIRM },
                            onClick = { showDialog.value = false },
                            content = { Text("Confirm") }
                        )
                    }
                )
            }
            DialogType.CONFIRM -> {
                Confirmation(
                    show = showDialog.value,
                    onDismissRequest = { showDialog.value = false },
                    durationMillis = 2000,
                    curvedText = ConfirmationDefaults.curvedText("Text"),
                    content = ConfirmationDefaults.SuccessIcon
                )
            }
            DialogType.SUCCESS -> {
                SuccessConfirmation(
                    show = showDialog.value,
                    onDismissRequest = { showDialog.value = false },
                    durationMillis = 2000
                )
            }
            DialogType.FAILURE -> {
                FailureConfirmation(
                    show = showDialog.value,
                    onDismissRequest = { showDialog.value = false },
                    durationMillis = 2000
                )
            }
        }
    }

    enum class DialogType {
        NONE,
        ALERT,
        CONFIRM,
        SUCCESS,
        FAILURE
    }
}

private const val OPEN_ALERT_DIALOG = "OPEN_ALERT_DIALOG"
private const val OPEN_CONFIRM_DIALOG = "OPEN_CONFIRM_DIALOG"
private const val OPEN_SUCCESS_DIALOG = "OPEN_SUCCESS_DIALOG"
private const val OPEN_FAILURE_DIALOG = "OPEN_FAILURE_DIALOG"

private const val DIALOG_CONFIRM = "DIALOG_CONFIRM"
