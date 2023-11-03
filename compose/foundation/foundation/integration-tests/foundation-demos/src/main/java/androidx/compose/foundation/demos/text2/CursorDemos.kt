/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TextField2CursorNotBlinkingInUnfocusedWindowDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val textFieldDecoration = Modifier
            .border(2.dp, Color.DarkGray, RoundedCornerShape(5.dp))
            .padding(8.dp)

        val textState = rememberTextFieldState("hello")
        BasicTextField2(textState, textFieldDecoration)

        var showDialog by remember { mutableStateOf(false) }
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.focusProperties { canFocus = false }
        ) {
            Text("Open Dialog")
        }
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(elevation = 20.dp) {
                    val dialogFocusRequester = remember { FocusRequester() }
                    Text(
                        "Hello! This is a dialog.",
                        Modifier
                            .padding(20.dp)
                            .focusRequester(dialogFocusRequester)
                            .background(Color.DarkGray)
                    )
                    LaunchedEffect(Unit) {
                        dialogFocusRequester.requestFocus()
                    }
                }
            }
        }
    }
}
