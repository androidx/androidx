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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.selectCharsIn
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Sampled
fun BasicTextField2StateEditSample() {
    val state = TextFieldState(TextFieldValue("hello world"))
    state.edit {
        // Insert a comma after "hello".
        replace(5, 5, ",") // = "hello, world"

        // Delete "world".
        replace(7, 12, "") // = "hello, "

        // Add a different name.
        append("Compose") // = "hello, Compose"

        // Select the new name so the user can change it by just starting to type.
        selectCharsIn(TextRange(7, 14)) // "hello, ̲C̲o̲m̲p̲o̲s̲e"
    }
}