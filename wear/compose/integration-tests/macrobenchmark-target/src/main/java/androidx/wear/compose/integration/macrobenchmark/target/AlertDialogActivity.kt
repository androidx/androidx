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

package androidx.wear.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog

class AlertDialogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showDialog by remember { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Chip(
                        onClick = { showDialog = true },
                        label = { Text("Open") },
                        modifier = Modifier.semantics { contentDescription = OPEN_ALERT_DIALOG },
                    )
                }
                val scrollState = rememberScalingLazyListState()
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                    scrollState = scrollState,
                ) {
                    Alert(
                        title = {
                            Text(
                                text = "Power off",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onBackground,
                            )
                        },
                        negativeButton = {
                            Button(
                                onClick = { showDialog = false },
                                colors = ButtonDefaults.secondaryButtonColors(),
                            ) {
                                Text("No")
                            }
                        },
                        positiveButton = {
                            Button(
                                onClick = { showDialog = false },
                                colors = ButtonDefaults.primaryButtonColors(),
                                modifier =
                                    Modifier.semantics { contentDescription = DIALOG_CONFIRM },
                            ) {
                                Text("Yes")
                            }
                        },
                        scrollState = scrollState,
                    ) {
                        Text(
                            text = "Are you sure?",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            }
        }
    }
}

private const val OPEN_ALERT_DIALOG = "OPEN_ALERT_DIALOG"
private const val DIALOG_CONFIRM = "DIALOG_CONFIRM"
