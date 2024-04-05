/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

val FocusAndKeyInput = Screen.Example("FocusAndKeyInput") {
    var isDialogOpened by remember { mutableStateOf(true) }
    if (!isDialogOpened) {
        Button({ isDialogOpened = true }) {
            Text("Open Dialog")
        }
    }
    if (isDialogOpened) {
        Dialog(onDismissRequest = { isDialogOpened = false }) {
            var latestKeyPressed: Key? by remember { mutableStateOf(null) }
            Column(Modifier.onKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    latestKeyPressed = it.key
                }
                false
            }) {
                Text(latestKeyPressed?.toString() ?: "Change focus and type on keyboard")
                FocusableBox()
                FocusableBox()
            }
        }
    }
}

@Composable
private fun FocusableBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { Box(Modifier.size(100.dp)) },
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(if (isFocused.value) Color.Blue else Color.LightGray)
            .border(width = 2.dp, Color.Black)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocused.value = state.isFocused
            }
            .clickable { focusRequester.requestFocus() },
    ) {
        content()
    }
}
