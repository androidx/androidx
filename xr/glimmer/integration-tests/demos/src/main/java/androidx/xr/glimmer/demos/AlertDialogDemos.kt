/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.xr.glimmer.AlertDialog
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.GlimmerLazyColumn

private enum class DialogType {
    None,
    TwoButtons,
    ConfirmOnly,
}

@Composable
internal fun AlertDialogDemo() {
    var activeDialog by remember { mutableStateOf(DialogType.None) }

    GlimmerLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListItem(onClick = { activeDialog = DialogType.TwoButtons }) {
                Text("Two Button Alert Dialog")
            }
        }
        item {
            ListItem(onClick = { activeDialog = DialogType.ConfirmOnly }) {
                Text("Confirm-Only Alert Dialog")
            }
        }
    }

    val dismissDialog = { activeDialog = DialogType.None }
    when (activeDialog) {
        DialogType.TwoButtons -> {
            AlertDialog(
                onDismissRequest = dismissDialog,
                confirmButton = { Button(onClick = dismissDialog) { Text("Confirm") } },
                dismissButton = { Button(onClick = dismissDialog) { Text("Dismiss") } },
                title = { Text("Title") },
                text = { Text("This is an alert dialog with options to confirm or dismiss.") },
            )
        }
        DialogType.ConfirmOnly -> {
            AlertDialog(
                onDismissRequest = dismissDialog,
                confirmButton = { Button(onClick = dismissDialog) { Text("OK") } },
                title = { Text("Title") },
                text = { Text("This is an alert dialog with only an option to dismiss.") },
            )
        }
        DialogType.None -> {}
    }
}
