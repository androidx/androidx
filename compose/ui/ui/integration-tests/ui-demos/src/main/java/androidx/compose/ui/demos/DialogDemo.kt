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

package androidx.compose.ui.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private var expandedWidthInit = false
private var expandedHeightInit = false
private var usePlatformDefaultWidthInit = false

@Composable
fun DialogDemo() {
    val roundedRectangleShape = RoundedCornerShape(percent = 15)
    var shape by remember { mutableStateOf(RectangleShape) }
    var elevation by remember { mutableStateOf(8.dp) }
    var openDialog by remember { mutableStateOf(true) }
    var expandedWidth by remember { mutableStateOf(expandedWidthInit) }
    var expandedHeight by remember { mutableStateOf(expandedHeightInit) }
    var usePlatformDefaultWidth by remember { mutableStateOf(usePlatformDefaultWidthInit) }

    TextButton(onClick = { openDialog = !openDialog }) {
        Text("Tap anywhere to reopen dialog")
    }
    if (openDialog) {
        Dialog(
            onDismissRequest = { openDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)
        ) {
            val width = if (expandedWidth) { 1500.dp } else { 300.dp }
            val height = if (expandedHeight) { 600.dp } else { 400.dp }
            Card(
                modifier = Modifier.size(width, height).padding(10.dp),
                elevation = elevation,
                shape = shape
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Dialog")
                    TextButton(
                        onClick = {
                            shape = if (shape == roundedRectangleShape) {
                                RectangleShape
                            } else {
                                roundedRectangleShape
                            }
                        }
                    ) {
                        Text("Toggle corners")
                    }
                    TextButton(
                        onClick = {
                            expandedWidth = !expandedWidth
                            expandedWidthInit = expandedWidth
                        }
                    ) {
                        Text("Toggle width")
                    }
                    TextButton(
                        onClick = {
                            expandedHeight = !expandedHeight
                            expandedHeightInit = expandedHeight
                        }
                    ) {
                        Text("Toggle height")
                    }
                    TextButton(
                        onClick = {
                            usePlatformDefaultWidth = !usePlatformDefaultWidth
                            usePlatformDefaultWidthInit = usePlatformDefaultWidthInit
                        }
                    ) {
                        Text("Toggle widthlock")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { elevation -= 1.dp }) {
                            Text("-1")
                        }
                        Text("Elevation: $elevation")
                        TextButton(onClick = { elevation += 1.dp }) {
                            Text("+1")
                        }
                    }
                    Text("Current size: [$width, $height]")
                    Text("usePlatformDefaultWidth = $usePlatformDefaultWidth")
                }
            }
        }
    }
}
