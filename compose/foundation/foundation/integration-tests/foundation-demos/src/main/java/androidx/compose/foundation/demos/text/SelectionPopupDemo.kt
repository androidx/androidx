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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

private val boxColor = Color.Gray
private val textBackgroundColor = Color.White
private val columnColor = lerp(boxColor, textBackgroundColor, 0.5f)

private val text = loremIpsum(wordCount = 50)

private val modifier = Modifier.background(textBackgroundColor)

@Composable
fun SelectionPopupDemo() {
    Box(modifier = Modifier.fillMaxSize().background(boxColor)) {
        Popup(alignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(columnColor)
            ) {
                BasicText("SelectionContainer")
                SelectionContainer { BasicText(text = text, modifier = modifier) }

                BasicText("BTF1")
                var tfv by remember { mutableStateOf(TextFieldValue(text)) }
                BasicTextField(tfv, { tfv = it }, modifier = modifier)

                BasicText("BTF2")
                BasicTextField(rememberTextFieldState(text), modifier = modifier)
            }
        }
    }
}
