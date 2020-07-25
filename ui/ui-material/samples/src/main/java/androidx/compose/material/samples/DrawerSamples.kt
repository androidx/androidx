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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.ui.core.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.BottomDrawerLayout
import androidx.compose.material.BottomDrawerState
import androidx.compose.material.Button
import androidx.compose.material.DrawerState
import androidx.compose.material.ModalDrawerLayout
import androidx.compose.ui.unit.dp

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
