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

import androidx.compose.foundation.border
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.fontSize8
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Button
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun KeyboardOptionsDemos() {
    LazyColumn(Modifier.imePadding()) {
        item { Item(KeyboardType.Text) }
        item { Item(KeyboardType.Ascii) }
        item { Item(KeyboardType.Number) }
        item { Item(KeyboardType.Phone) }
        item { Item(KeyboardType.Uri) }
        item { Item(KeyboardType.Email) }
        item { Item(KeyboardType.Password) }
        item { Item(KeyboardType.NumberPassword) }
        item { ShowKeyboardOnFocus(true) }
        item { ShowKeyboardOnFocus(false) }
        item { HintLocaleDemo(LocaleList("de")) }
    }
}

@Composable
private fun Item(keyboardType: KeyboardType) {
    TagLine(tag = "Keyboard Type: $keyboardType")
    EditLine(keyboardType = keyboardType)
}

@Composable
private fun EditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    text: String = ""
) {
    val state = remember { TextFieldState(text) }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        state = state,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        textStyle = TextStyle(fontSize = fontSize8),
    )
}

@Composable
private fun ShowKeyboardOnFocus(showKeyboardOnFocus: Boolean) {
    Column {
        TagLine(tag = "showKeyboardOnFocus: $showKeyboardOnFocus")

        val state = remember { TextFieldState("") }
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            modifier = demoTextFieldModifiers.focusRequester(focusRequester),
            state = state,
            keyboardOptions = KeyboardOptions(showKeyboardOnFocus = showKeyboardOnFocus)
        )
        Button(onClick = { focusRequester.requestFocus() }) {
            BasicText("Focus me", style = LocalTextStyle.current)
        }
    }
}

@Composable
private fun HintLocaleDemo(localeList: LocaleList) {
    Column {
        TagLine(tag = "Hints IME Locale: $localeList")

        val state = remember { TextFieldState("") }
        BasicTextField(
            modifier = demoTextFieldModifiers,
            state = state,
            keyboardOptions = KeyboardOptions(hintLocales = localeList)
        )
    }
}

val demoTextFieldModifiers =
    Modifier.fillMaxWidth()
        .padding(6.dp)
        .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
        .padding(6.dp)
