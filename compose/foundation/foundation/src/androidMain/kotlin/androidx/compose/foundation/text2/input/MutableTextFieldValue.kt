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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A mutable version of [TextFieldValue]. The text in the buffer can be changed by calling
 * [replace].
 */
@ExperimentalFoundationApi
class MutableTextFieldValue internal constructor(internal val value: TextFieldValue) : Appendable {

    private val buffer = StringBuffer(value.text)

    val length: Int get() = buffer.length

    /**
     * Replaces the text between [start] (inclusive) and [end] (exclusive) in this value with
     * [text].
     */
    fun replace(start: Int, end: Int, text: String) {
        buffer.replace(start, end, text)
    }

    override fun append(char: Char): Appendable = apply {
        buffer.append(char)
    }

    override fun append(text: CharSequence?): Appendable = apply {
        buffer.append(text)
    }

    override fun append(text: CharSequence?, start: Int, end: Int): Appendable = apply {
        buffer.append(text, start, end)
    }

    override fun toString(): String = buffer.toString()

    internal fun toTextFieldValue(selection: TextRange): TextFieldValue =
        TextFieldValue(buffer.toString(), selection = selection)
}