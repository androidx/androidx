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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType

val IOSDynamicKeyboardType = Screen.Example("IOSDynamicKeyboardType") {
    //Issue https://github.com/JetBrains/compose-multiplatform/issues/3885
    var keyboardOptions by remember { mutableStateOf(KeyboardOptions.Default) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Button(
                onClick = {
                    keyboardOptions =
                        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
                }
            ) {
                Text("Phone")
            }
            Button(
                onClick = {
                    keyboardOptions =
                        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                }
            ) {
                Text("Number")
            }
            Button(
                onClick = {
                    keyboardOptions =
                        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
                }
            ) {
                Text("Email")
            }
        }
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                .background(color = Color.LightGray),
            value = "",
            onValueChange = {},
            singleLine = true,
            keyboardOptions = keyboardOptions,
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
