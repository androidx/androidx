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

package androidx.compose.mpp.demo.components.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.textfield.android.loremIpsum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlertDialogExample() {
    var short by remember { mutableStateOf(false) }
    var long by remember { mutableStateOf(false) }
    Column(Modifier.padding(5.dp)) {
        Button(onClick = { short = true }) {
            Text("AlertDialog (short)")
        }
        Button(onClick = { long = true }) {
            Text("AlertDialog (long)")
        }
    }
    if (short) {
        AlertDialog(
            onDismissRequest = { short = false },
            confirmButton = {
                Button(onClick = { short = false }) {
                    Text("OK")
                }
            },
            text = { Text("Meow") },
        )
    }
    if (long) {
        AlertDialog(
            onDismissRequest = { long = false },
            confirmButton = {
                Button(onClick = { long = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { long = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Alert Dialog") },
            text = { Text(loremIpsum()) },
        )
    }
}
