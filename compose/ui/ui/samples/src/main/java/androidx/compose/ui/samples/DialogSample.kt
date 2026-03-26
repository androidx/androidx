/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.os.IBinder
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Sampled
@Composable
fun DialogSample() {
    val openDialog = remember { mutableStateOf(true) }
    val dialogWidth = 200.dp
    val dialogHeight = 50.dp

    if (openDialog.value) {
        Dialog(onDismissRequest = { openDialog.value = false }) {
            // Draw a rectangle shape with rounded corners inside the dialog
            Box(Modifier.size(dialogWidth, dialogHeight).background(Color.White))
        }
    }
}

@Sampled
@Composable
fun DialogFromServiceSample() {
    // In a real Service scenario, appWindowToken would be received via IPC
    // from the main application process. This sample simulates having the token.
    val appWindowToken: IBinder? = null // Provided via IPC

    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) { Text("Show Dialog From Service") }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties =
                DialogProperties(
                    // Pass the application's window token
                    windowToken = appWindowToken
                ),
        ) {
            Box(Modifier.size(250.dp, 150.dp).background(Color.DarkGray)) {
                Text("Dialog Content (Service)", color = Color.White)
            }
        }
    }
}
