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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interactionBarrier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun InteractionBarrierSample() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // Background button on the main screen (opens dialog, blocked while dialog is visible)
        Button(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.Center)) {
            Text("Open Dialog")
        }

        // Custom modal dialog overlay using Modifier.interactionBarrier()
        if (showDialog) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .interactionBarrier(),
                contentAlignment = Alignment.Center,
            ) {
                Card(modifier = Modifier.padding(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Custom Dialog", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Background interaction is blocked by the barrier.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showDialog = false }) { Text("Close Dialog") }
                    }
                }
            }
        }
    }
}
