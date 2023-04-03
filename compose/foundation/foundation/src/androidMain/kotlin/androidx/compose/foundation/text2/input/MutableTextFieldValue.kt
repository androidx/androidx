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
open class MutableTextFieldValue internal constructor(
    internal val value: TextFieldValue
) : CharSequence,
    Appendable {

    private val buffer = StringBuffer(value.text)

    /**
     * The number of characters in the text field. This will be equal to or greater than
     * [codepointLength].
     */
    override val length: Int get() = buffer.length

    /**
     * The number of codepoints in the text field. This will be equal to or less than [length].
     */
    val codepointLength: Int get() = buffer.codePointCount(0, length)

    /**
     * Replaces the text between [start] (inclusive) and [end] (exclusive) in this value with
     * [text].
     *
     * @see insert
     */
    fun replace(start: Int, end: Int, text: String) {
        onTextWillChange(TextRange(start, end), text.length)
        replaceWithoutNotifying(start, end, text)
    }

    override fun append(char: Char): Appendable = apply {
        onTextWillChange(TextRange(length), 1)
        buffer.append(char)
    }

    override fun append(text: CharSequence?): Appendable = apply {
        if (text != null) {
            onTextWillChange(TextRange(length), text.length)
            buffer.append(text)
        }
    }

    override fun append(text: CharSequence?, start: Int, end: Int): Appendable = apply {
        if (text != null) {
            onTextWillChange(TextRange(length), end - start)
            buffer.append(text, start, end)
        }
    }

    protected fun replaceWithoutNotifying(start: Int, end: Int, text: String) {
        buffer.replace(start, end, text)
    }

    /**
     * Called just before the text contents are about to change.
     *
     * @param rangeToBeReplaced The range in the current text that's about to be replaced.
     * @param newLength The length of the replacement.
     */
    protected open fun onTextWillChange(rangeToBeReplaced: TextRange, newLength: Int) {
        // Noop.
    }

    override operator fun get(index: Int): Char = buffer[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        buffer.subSequence(startIndex, endIndex)

    override fun toString(): String = buffer.toString()

    internal fun toTextFieldValue(
        selection: TextRange,
        composition: TextRange? = null
    ): TextFieldValue = TextFieldValue(
        buffer.toString(),
        selection = selection,
        composition = composition
    )

    internal fun requireValidIndex(index: Int, inCodepoints: Boolean) {
        // The "units" of the range in the error message should match the units passed in.
        // If the input was in codepoint indices, the output should be in codepoint indices.
        val validRange = TextRange(0, length)
            .let { if (inCodepoints) charsToCodepoints(it) else it }
        require(index in validRange) {
            val unit = if (inCodepoints) "codepoints" else "chars"
            "Expected $index to be in $validRange ($unit)"
        }
    }

    internal fun requireValidRange(range: TextRange, inCodepoints: Boolean) {
        // The "units" of the range in the error message should match the units passed in.
        // If the input was in codepoint indices, the output should be in codepoint indices.
        val validRange = TextRange(0, length)
            .let { if (inCodepoints) charsToCodepoints(it) else it }
        require(range in validRange) {
            val unit = if (inCodepoints) "codepoints" else "chars"
            "Expected $range to be in $validRange ($unit)"
        }
    }

    internal fun codepointsToChars(range: TextRange): TextRange = TextRange(
        codepointIndexToCharIndex(range.start),
        codepointIndexToCharIndex(range.end)
    )

    internal fun charsToCodepoints(range: TextRange): TextRange = TextRange(
        charIndexToCodepointIndex(range.start),
        charIndexToCodepointIndex(range.end),
    )

    // TODO Support actual codepoints.
    internal fun codepointIndexToCharIndex(index: Int): Int = index
    private fun charIndexToCodepointIndex(index: Int): Int = index
}

/**
 * Insert [text] at the given [index] in this value.
 *
 * @see MutableTextFieldValue.replace
 */
@ExperimentalFoundationApi
fun MutableTextFieldValue.insert(index: Int, text: String) {
    replace(index, index, text)
}