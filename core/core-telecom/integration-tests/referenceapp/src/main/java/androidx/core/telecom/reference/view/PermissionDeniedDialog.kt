/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.reference.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PermissionDeniedDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Users usually can't easily dismiss permission prompts */ },
        title = { Text("Permissions Required") },
        text = {
            Text(
                "This app requires certain permissions (Microphone," +
                    " Notifications, Nearby Devices) to function correctly. Please grant them" +
                    " in App Settings."
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Open Settings") } },
    )
}
