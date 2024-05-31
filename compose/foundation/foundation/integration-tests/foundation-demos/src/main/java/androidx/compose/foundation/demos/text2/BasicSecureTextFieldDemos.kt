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
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicSecureTextFieldDemos() {
    Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
        val clipboardManager = LocalClipboardManager.current
        Button(onClick = { clipboardManager.setText(AnnotatedString("\uD801\uDC37")) }) {
            Text("Copy surrogate pair \"\uD801\uDC37\"")
        }

        TagLine(tag = "Visible")
        BasicSecureTextFieldDemo(TextObfuscationMode.Visible)

        TagLine(tag = "RevealLastTyped")
        BasicSecureTextFieldDemo(TextObfuscationMode.RevealLastTyped)

        TagLine(tag = "Hidden")
        BasicSecureTextFieldDemo(TextObfuscationMode.Hidden)

        TagLine(tag = "Changing Mask")
        ChangingMaskDemo(TextObfuscationMode.RevealLastTyped)

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
fun ChangingMaskDemo(textObfuscationMode: TextObfuscationMode) {
    val maskState = rememberTextFieldState("\u2022")
    val passwordState = rememberTextFieldState("hunter2")
    Column {
        // single character TextField
        BasicTextField(
            state = maskState,
            modifier = demoTextFieldModifiers,
            inputTransformation = {
                // only handle single character insertion, reject everything else
                val isSingleCharacterInsertion =
                    changes.changeCount == 1 &&
                        changes.getRange(0).length == 1 &&
                        changes.getOriginalRange(0).length == 0

                if (!isSingleCharacterInsertion) {
                    revertAllChanges()
                } else {
                    replace(
                        start = 0,
                        end = length,
                        text = asCharSequence().substring(changes.getRange(0))
                    )
                }
            },
            outputTransformation = { insert(0, "Enter mask character: ") }
        )
    }
    BasicSecureTextField(
        state = passwordState,
        textObfuscationMode = textObfuscationMode,
        textObfuscationCharacter = maskState.text[0],
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberPasswordDemo() {
    val state = remember { TextFieldState() }
    BasicSecureTextField(
        state = state,
        inputTransformation = {
            if (!asCharSequence().isDigitsOnly()) {
                revertAllChanges()
            }
        },
        keyboardOptions =
            KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.NumberPassword),
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordToggleVisibilityDemo() {
    val state = remember { TextFieldState() }
    var visible by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicSecureTextField(
            state = state,
            textObfuscationMode =
                if (visible) {
                    TextObfuscationMode.Visible
                } else {
                    TextObfuscationMode.RevealLastTyped
                },
            modifier =
                Modifier.weight(1f)
                    .padding(6.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                    .padding(6.dp)
        )
        if (visible) {
            TextButton(onClick = { visible = false }) { Text("Hide") }
        } else {
            TextButton(onClick = { visible = true }) { Text("Show") }
        }
    }
}
