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
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.BottomDrawerLayout
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.Button
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawerLayout
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ModalDrawerSample() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val appContentText =
        if (drawerState.isClosed) {
            ">>> Pull to open>>>"
        } else {
            "<<< Swipe to close <<<"
        }
    ModalDrawerLayout(
        drawerState = drawerState,
        drawerContent = {
            YourDrawerContent(onClose = { drawerState.close() })
        },
        bodyContent = {
            YourAppContent(appContentText, onOpen = { drawerState.open() })
        }
    )
}

@Sampled
@Composable
fun BottomDrawerSample() {
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    val appContentText =
        if (drawerState.isClosed) {
            "▲▲▲ Pull to open ▲▲▲"
        } else {
            "▼▼▼ Drag down to close ▼▼▼"
        }
    BottomDrawerLayout(
        drawerState = drawerState,
        drawerContent = {
            YourDrawerContent(onClose = { drawerState.close() })
        },
        bodyContent = {
            YourAppContent(appContentText, onOpen = { drawerState.open() })
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
