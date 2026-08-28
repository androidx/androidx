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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.xr.glimmer.AlertDialog
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text

@Sampled
@Composable
fun AlertDialogSample() {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { Button(onClick = { /* confirm action */ }) { Text("Confirm") } },
            dismissButton = { Button(onClick = { showDialog = false }) { Text("Dismiss") } },
            icon = { Icon(imageVector = FavoriteIcon, contentDescription = "Alert icon") },
            title = { Text("Title") },
            text = { Text("This is an alert dialog with options to confirm or dismiss.") },
        )
    }
}

@Sampled
@Composable
fun AlertDialogConfirmOnlySample() {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { Button(onClick = { showDialog = false }) { Text("OK") } },
            icon = {
                Icon(
                    imageVector = OutlinedFavoriteIcon,
                    contentDescription = "Favorite icon",
                )
            },
            title = { Text("Title") },
            text = { Text("This is an alert dialog with only an option to dismiss.") },
        )
    }
}
