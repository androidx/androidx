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

package androidx.xr.compose.material3.integration.testapp

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.SpatialDialog

@Composable
internal fun DetailPaneDialog() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SimpleDialog()
        MaterialAlertDialog()
        XrElevatedDialog()
    }
}

@Composable
private fun SimpleDialog() {
    val shouldEnableButton = !LocalSpatialCapabilities.current.isSpatialUiEnabled
    DialogWithShowButton(enabled = shouldEnableButton, text = "Compose UI Dialog") { showDialog ->
        Dialog(onDismissRequest = { showDialog.value = false }) {
            Card(Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.5f)) {
                Text("This is a simple Dialog with a Material Card inside.")
            }
        }
    }
}

@Composable
private fun MaterialAlertDialog() {
    val context = LocalContext.current
    DialogWithShowButton(text = "Material Alert Dialog") { showDialog ->
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("AlertDialog") },
            text = { Text("This is a Material AlertDialog") },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Confirm button clicked", Toast.LENGTH_LONG).show()
                        showDialog.value = false
                    }
                ) {
                    Text("Confirm")
                }
            },
        )
    }
}

@Composable
private fun XrElevatedDialog() {
    DialogWithShowButton(text = "XR ElevatedDialog") { showDialog ->
        SpatialDialog(onDismissRequest = { showDialog.value = false }) {
            Card { Text("This is an XR ElevatedDialog with a Material Card inside.") }
        }
    }
}

@Composable
private fun DialogWithShowButton(
    enabled: Boolean = true,
    text: String,
    content: @Composable (MutableState<Boolean>) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    Button(onClick = { showDialog.value = true }, enabled = enabled) { Text(text) }
    if (showDialog.value) {
        content(showDialog)
    }
}
