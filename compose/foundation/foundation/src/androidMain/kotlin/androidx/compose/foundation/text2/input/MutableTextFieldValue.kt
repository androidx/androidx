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
 * A mutable version of [TextFieldValue], similar to [StringBuilder].
 *
 * This class provides methods for changing the text, such as [replace] and [append].
 *
 * To get one of these, and for usage samples, see [TextFieldState.edit].
 */
@ExperimentalFoundationApi
class MutableTextFieldValue internal constructor(internal val value: TextFieldValue) : Appendable {

    private val buffer = StringBuffer(value.text)

    /**
     * The number of characters in the text field. This will be equal to or greater than
     * [codepointLength].
     */
    val length: Int get() = buffer.length

    /**
     * The number of codepoints in the text field. This will be equal to or less than [length].
     */
    val codepointLength: Int get() = charIndexToCodepointIndex(length)

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

    internal fun codepointsToChars(range: TextRange): TextRange = TextRange(
        codepointIndexToCharIndex(range.start),
        codepointIndexToCharIndex(range.end)
    )

    internal fun charsToCodepoints(range: TextRange): TextRange = TextRange(
        charIndexToCodepointIndex(range.start),
        charIndexToCodepointIndex(range.end),
    )

    // TODO Support actual codepoints.
    private fun codepointIndexToCharIndex(index: Int): Int = index
    private fun charIndexToCodepointIndex(index: Int): Int = index
}