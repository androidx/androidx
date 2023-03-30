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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.internal.EditProcessor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@ExperimentalFoundationApi
class TextFieldState(
    initialValue: TextFieldValue = TextFieldValue(),
    filter: TextEditFilter = TextEditFilter.Default
) {

    internal var editProcessor = EditProcessor(initialValue, filter)

    val value: TextFieldValue
        get() = editProcessor.value
}

@ExperimentalFoundationApi
fun interface TextEditFilter {

    fun filter(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue

    companion object {
        val Default = TextEditFilter { _, new -> new }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldState.deselect() {
    if (!value.selection.collapsed) {
        editProcessor.reset(value.copy(selection = TextRange.Zero, composition = TextRange.Zero))
    }
}