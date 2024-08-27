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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun AlertDialogWithConfirmAndDismissSample() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }
    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                Icons.Rounded.AccountCircle,
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

@Sampled
@Composable
fun AlertDialogWithBottomButtonSample() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }

    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                Icons.Rounded.AccountCircle,
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Mobile network is not currently available") },
        bottomButton = {
            AlertDialogDefaults.BottomButton(
                onClick = {
                    // Perform confirm action here
                    showDialog = false
                }
            )
        }
    )
}

@Sampled
@Composable
fun AlertDialogWithContentGroupsSample() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }
    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        title = { Text("Share your location") },
        text = { Text(" The following apps have asked you to share your location") },
        bottomButton = {
            AlertDialogDefaults.BottomButton(
                onClick = {
                    // Perform confirm action here
                    showDialog = false
                }
            ) {
                Text("Share once")
            }
        }
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
