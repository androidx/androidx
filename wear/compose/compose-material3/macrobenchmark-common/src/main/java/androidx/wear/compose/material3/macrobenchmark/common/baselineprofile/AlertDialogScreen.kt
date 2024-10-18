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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.macrobenchmark.common.FIND_OBJECT_TIMEOUT_MS
import androidx.wear.compose.material3.macrobenchmark.common.R

val AlertDialogScreen =
    object : BaselineProfileScreen {

        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AlertDialogWithConfirmAndDismiss()
                    AlertDialogWithEdgeButton()
                    AlertDialogWithContentGroups()
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                device.findObject(By.desc(OpenAlertDialogWithConfirmAndDismiss)).click()
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()

                device
                    .wait(
                        Until.findObject(By.desc(OpenAlertDialogWithEdgeButton)),
                        FIND_OBJECT_TIMEOUT_MS
                    )
                    .click()
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()

                device
                    .wait(
                        Until.findObject(By.desc(OpenAlertDialogWithContentGroups)),
                        FIND_OBJECT_TIMEOUT_MS
                    )
                    .click()
                device.waitForIdle()
                device.pressBack()
                device.waitForIdle()

                device.wait(
                    Until.findObject(By.desc(OpenAlertDialogWithConfirmAndDismiss)),
                    FIND_OBJECT_TIMEOUT_MS
                )
            }
    }

@Composable
private fun AlertDialogWithConfirmAndDismiss() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier =
                Modifier.align(Alignment.Center).semantics {
                    contentDescription = OpenAlertDialogWithConfirmAndDismiss
                },
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }
    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                painterResource(R.drawable.ic_favorite_rounded),
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Enable Battery Saver Mode?") },
        text = { Text("Your battery is low. Turn on battery saver.") },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    // Perform confirm action here
                    showDialog = false
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    // Perform dismiss action here
                    showDialog = false
                }
            )
        }
    )
}

@Composable
private fun AlertDialogWithEdgeButton() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier =
                Modifier.align(Alignment.Center).semantics {
                    contentDescription = OpenAlertDialogWithEdgeButton
                },
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }

    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                painterResource(R.drawable.ic_favorite_rounded),
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Mobile network is not currently available") },
        edgeButton = {
            AlertDialogDefaults.EdgeButton(
                onClick = {
                    // Perform confirm action here
                    showDialog = false
                }
            )
        }
    )
}

@Composable
private fun AlertDialogWithContentGroups() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier =
                Modifier.align(Alignment.Center).semantics {
                    contentDescription = OpenAlertDialogWithContentGroups
                },
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }
    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        title = { Text("Share your location") },
        text = { Text(" The following apps have asked you to share your location") },
    ) {
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text("Weather") }
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text("Calendar") }
            )
        }
        item { AlertDialogDefaults.GroupSeparator() }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text(modifier = Modifier.fillMaxWidth(), text = "Never share") }
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text(modifier = Modifier.fillMaxWidth(), text = "Share always") }
            )
        }
    }
}

private const val OpenAlertDialogWithConfirmAndDismiss = "OpenAlertDialogWithConfirmAndDismiss"
private const val OpenAlertDialogWithEdgeButton = "OpenAlertDialogWithEdgeButton"
private const val OpenAlertDialogWithContentGroups = "OpenAlertDialogWithContentGroups"
