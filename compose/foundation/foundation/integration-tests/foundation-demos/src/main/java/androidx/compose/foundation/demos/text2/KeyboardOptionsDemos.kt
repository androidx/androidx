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
import androidx.compose.foundation.border
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.fontSize8
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun KeyboardOptionsDemos() {
    LazyColumn {
        item { Item(KeyboardType.Text) }
        item { Item(KeyboardType.Ascii) }
        item { Item(KeyboardType.Number) }
        item { Item(KeyboardType.Phone) }
        item { Item(KeyboardType.Uri) }
        item { Item(KeyboardType.Email) }
        item { Item(KeyboardType.Password) }
        item { Item(KeyboardType.NumberPassword) }
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
    BasicTextField2(
        modifier = demoTextFieldModifiers,
        state = state,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        textStyle = TextStyle(fontSize = fontSize8),
    )
}

val demoTextFieldModifiers = Modifier
    .fillMaxWidth()
    .padding(6.dp)
    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
    .padding(6.dp)