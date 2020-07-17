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
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredHeight
import androidx.ui.material.BottomDrawerLayout
import androidx.ui.material.BottomDrawerState
import androidx.ui.material.Button
import androidx.ui.material.DrawerState
import androidx.ui.material.ModalDrawerLayout
import androidx.ui.unit.dp

@Sampled
@Composable
fun ModalDrawerSample() {
    val (state, onStateChange) = state { DrawerState.Closed }
    val appContentText =
        if (state == DrawerState.Closed) {
            ">>> Pull to open >>>"
        } else {
            "<<< Swipe to close <<<"
        }
    ModalDrawerLayout(
        drawerState = state,
        onStateChange = onStateChange,
        drawerContent = {
            YourDrawerContent(onClose = { onStateChange(DrawerState.Closed) })
        },
        bodyContent = {
            YourAppContent(appContentText, onOpen = { onStateChange(DrawerState.Opened) })
        }
    )
}

@Sampled
@Composable
fun BottomDrawerSample() {
    val (state, onStateChange) = state { BottomDrawerState.Closed }
    val appContentText =
        if (state == BottomDrawerState.Closed) {
            "▲▲▲ Pull to open ▲▲▲"
        } else {
            "▼▼▼ Drag down to close ▼▼▼"
        }
    BottomDrawerLayout(
        drawerState = state,
        onStateChange = onStateChange,
        drawerContent = {
            YourDrawerContent(onClose = { onStateChange(BottomDrawerState.Closed) })
        },
        bodyContent = {
            YourAppContent(appContentText, onOpen = { onStateChange(BottomDrawerState.Opened) })
        }
    )
}

@Composable
private fun YourDrawerContent(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), gravity = ContentGravity.Center) {
        Column(Modifier.fillMaxHeight()) {
            Text(text = "Drawer Content")
            Spacer(Modifier.preferredHeight(20.dp))
            Button(onClick = onClose) {
                Text("Close Drawer")
            }
        }
    }
}

@Composable
private fun YourAppContent(text: String, onOpen: () -> Unit) {
    Column(Modifier.fillMaxHeight()) {
        Text(text = text)
        Spacer(Modifier.preferredHeight(20.dp))
        Button(onClick = onOpen) {
            Text("Click to open")
        }
    }
}
