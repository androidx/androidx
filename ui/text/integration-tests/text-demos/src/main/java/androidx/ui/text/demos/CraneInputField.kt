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

package androidx.ui.text.demos

import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.EditorStyle
import androidx.ui.core.InputField
import androidx.ui.core.TextRange
import androidx.ui.input.EditorState
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.VerticalScroller
import androidx.ui.painting.TextStyle

@Composable
fun InputFieldDemo() {
    CraneWrapper {
        VerticalScroller {
            Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                TagLine(tag = "simple editing")
                EditLine()
            }
        }
    }
}

@Composable
fun EditLine() {
    val state = +state { EditorState(text = "Hello, Editor", selection = TextRange(2, 2)) }
    InputField(
        value = state.value,
        onValueChange = { state.value = it },
        editorStyle = EditorStyle(textStyle = TextStyle(fontSize = fontSize8))
    )
}
