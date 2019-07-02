/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.material.BottomDrawer
import androidx.ui.material.Button
import androidx.ui.material.DrawerState
import androidx.ui.material.ModalDrawer
import androidx.ui.material.StaticDrawer

@Sampled
@Composable
fun StaticDrawerSample() {
    Row {
        StaticDrawer {
            Center {
                Text("Drawer Content")
            }
        }
        ColoredRect(Color.Black, width = 1.dp)
        Text("Rest of App")
    }
}

@Sampled
@Composable
fun ModalDrawerSample() {
    val (state, onStateChange) = +state { DrawerState.Closed }
    val appContentText =
        if (state == DrawerState.Closed) ">>> Pull to open >>>" else "<<< Swipe to close <<<"
    Stack {
        aligned(Alignment.Center) {
            // your app content goes there
            YourAppContent(appContentText, onStateChange)
        }
        aligned(Alignment.CenterLeft) {
            ModalDrawer(state, onStateChange) {
                Column {
                    Text(text = "Drawer Content")
                    HeightSpacer(20.dp)
                    Button(
                        text = "Close Drawer",
                        onClick = { onStateChange(DrawerState.Closed) })
                }
            }
        }
    }
}

@Sampled
@Composable
fun BottomDrawerSample() {
    val (state, onStateChange) = +state { DrawerState.Closed }
    val appContentText =
        if (state == DrawerState.Closed) "▲▲▲ Pull to open ▲▲▲" else "▼▼▼ Drag down to close ▼▼▼"
    Stack {
        aligned(Alignment.Center) {
            // your app content goes there
            YourAppContent(appContentText, onStateChange)
        }
        aligned(Alignment.BottomCenter) {
            BottomDrawer(state, onStateChange) {
                Column {
                    Text(text = "Drawer Content")
                    HeightSpacer(20.dp)
                    Button(
                        text = "Close Drawer",
                        onClick = { onStateChange(DrawerState.Closed) })
                }
            }
        }
    }
}

@Composable
private fun YourAppContent(text: String, onDrawerStateChange: (DrawerState) -> Unit) {
    Column {
        Text(text = text)
        HeightSpacer(20.dp)
        Button(
            text = "Click to open",
            onClick = { onDrawerStateChange(DrawerState.Opened) }
        )
    }
}
