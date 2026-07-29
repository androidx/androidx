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

package androidx.compose.remote.integration.demos.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun DragPropagationDemo() {
    val outerScrollState = rememberScrollState()

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(outerScrollState)
                .padding(16.dp)
    ) {
        Text(
            text = "Drag Propagation Demo",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Instructions & Expected Behavior:",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        "1. Try dragging / scrolling vertically inside the blue outlined box (Remote Compose Player containing a scrollable list).\n" +
                            "2. Expected: Dragging inside the Remote Compose UI should scroll the Remote Compose list and consume the gesture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Remote Compose Scrollable Player (Blue Box):",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(280.dp)
                    .border(2.dp, Color(0xFF1976D2))
                    .background(Color.White)
        ) {
            RemoteDemo(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberRemoteScrollState()
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxSize().verticalScroll(scrollState).padding(8.rdp)
                ) {
                    RemoteText(
                        text = "--- Remote Compose Scrollable Start ---".rs,
                        color = Color.Blue.rc,
                    )
                    for (i in 1..25) {
                        val itemColor = if (i % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFBBDEFB)
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxWidth()
                                    .height(40.rdp)
                                    .padding(vertical = 4.rdp)
                                    .background(itemColor.rc)
                        ) {
                            RemoteText(
                                text = "Remote Compose Scroll Item #$i".rs,
                                color = Color.Black.rc,
                            )
                        }
                    }
                    RemoteText(
                        text = "--- Remote Compose Scrollable End ---".rs,
                        color = Color.Blue.rc,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Outer Host Extra Content (Scroll Down):",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
        )

        for (i in 1..15) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text(
                    text = "Outer Host Page Item #$i",
                    modifier = Modifier.padding(12.dp),
                    color = Color.Black,
                )
            }
        }
    }
}
