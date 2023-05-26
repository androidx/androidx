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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text2.BasicSecureTextField
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicSecureTextFieldDemos() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine(tag = "Visible")
        BasicSecureTextFieldDemo(TextObfuscationMode.Visible)

        TagLine(tag = "RevealLastTyped")
        BasicSecureTextFieldDemo(TextObfuscationMode.RevealLastTyped)

        TagLine(tag = "Hidden")
        BasicSecureTextFieldDemo(TextObfuscationMode.Hidden)

        TagLine(tag = "Number Password")
        NumberPasswordDemo()

        TagLine(tag = "Password Toggle Visibility")
        PasswordToggleVisibilityDemo()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicSecureTextFieldDemo(textObfuscationMode: TextObfuscationMode) {
    val state = remember { TextFieldState() }
    BasicSecureTextField(
        state = state,
        textObfuscationMode = textObfuscationMode,
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPasswordDemo() {
    val state = remember { TextFieldState() }
    BasicSecureTextField(
        state = state,
        filter = { _, new ->
            if (!new.isDigitsOnly()) {
                new.revertAllChanges()
            }
        },
        keyboardType = KeyboardType.NumberPassword,
        imeAction = ImeAction.Default,
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordToggleVisibilityDemo() {
    val state = remember { TextFieldState() }
    var visible by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        BasicSecureTextField(
            state = state,
            textObfuscationMode = if (visible) {
                TextObfuscationMode.Visible
            } else {
                TextObfuscationMode.RevealLastTyped
            },
            modifier = Modifier
                .weight(1f)
                .padding(6.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                .padding(6.dp)
        )
        IconToggleButton(checked = visible, onCheckedChange = { visible = it }) {
            if (visible) {
                Icon(Icons.Default.Warning, "")
            } else {
                Icon(Icons.Default.Info, "")
            }
        }
    }
}