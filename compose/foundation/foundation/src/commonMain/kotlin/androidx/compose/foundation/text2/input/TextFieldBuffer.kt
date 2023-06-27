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

import androidx.annotation.CallSuper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text2.input.internal.ChangeTracker
import androidx.compose.ui.text.TextRange

/**
 * A text buffer that can be edited, similar to [StringBuilder].
 *
 * This class provides methods for changing the text, such as [replace], [append], [insert], and
 * [delete].
 *
 * To get one of these, and for usage samples, see [TextFieldState.edit]. Every change to the buffer
 * is tracked in a [ChangeList] which you can access via the [changes] property.
 *
 * [TextFieldBufferWithSelection] is a special type of buffer that has an associated cursor position
 * or selection range.
 */
@ExperimentalFoundationApi
open class TextFieldBuffer internal constructor(
    internal val value: TextFieldCharSequence,
    initialChanges: ChangeTracker? = null
) : CharSequence,
    Appendable {

    private val buffer = StringBuffer(value)

    /**
     * Lazily-allocated [ChangeTracker], initialized on the first text change.
     */
    private var changeTracker: ChangeTracker? =
        initialChanges?.let { ChangeTracker(initialChanges) }

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
     * The [ChangeList] represents the changes made to this value and is inherently mutable. This
     * means that the returned [ChangeList] always reflects the complete list of changes made to
     * this value at any given time, even those made after reading this property.
     *
     * @sample androidx.compose.foundation.samples.BasicTextField2ChangeIterationSample
     * @sample androidx.compose.foundation.samples.BasicTextField2ChangeReverseIterationSample
     */
    val changes: ChangeList get() = changeTracker ?: EmptyChangeList

    /**
     * Replaces the text between [start] (inclusive) and [end] (exclusive) in this value with
     * [text], and records the change in [changes].
     *
     * @param start The character offset of the first character to replace.
     * @param end The character offset of the first character after the text to replace.
     * @param text The text to replace the range `[start, end)` with.
     *
     * @see append
     * @see insert
     * @see delete
     */
    fun replace(start: Int, end: Int, text: String) {
        onTextWillChange(TextRange(start, end), text.length)
        buffer.replace(start, end, text)
    }

    // Doc inherited from Appendable.
    // This append overload should be first so it ends up being the target of links to this method.
    override fun append(text: CharSequence?): Appendable = apply {
        if (text != null) {
            onTextWillChange(TextRange(length), text.length)
            buffer.append(text)
        }
    }

    // Doc inherited from Appendable.
    override fun append(text: CharSequence?, start: Int, end: Int): Appendable = apply {
        if (text != null) {
            onTextWillChange(TextRange(length), end - start)
            buffer.append(text, start, end)
        }
    }

    // Doc inherited from Appendable.
    override fun append(char: Char): Appendable = apply {
        onTextWillChange(TextRange(length), 1)
        buffer.append(char)
    }

    /**
     * Called just before the text contents are about to change.
     *
     * @param rangeToBeReplaced The range in the current text that's about to be replaced.
     * @param newLength The length of the replacement.
     */
    @CallSuper
    protected open fun onTextWillChange(rangeToBeReplaced: TextRange, newLength: Int) {
        (changeTracker ?: ChangeTracker().also { changeTracker = it })
            .trackChange(rangeToBeReplaced, newLength)
    }

    override operator fun get(index: Int): Char = buffer[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        buffer.subSequence(startIndex, endIndex)

    override fun toString(): String = buffer.toString()

    internal fun clearChangeList() {
        changeTracker?.clearChanges()
    }

    internal fun toTextFieldCharSequence(
        selection: TextRange,
        composition: TextRange? = null
    ): TextFieldCharSequence = TextFieldCharSequence(
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

    internal fun toTextFieldCharSequence(selection: TextRange): TextFieldCharSequence =
        TextFieldCharSequence(buffer.toString(), selection = selection)

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

    /**
     * The ordered list of non-overlapping and discontinuous changes performed on a
     * [TextFieldBuffer] during the current [edit][TextFieldState.edit] or
     * [filter][TextEditFilter.filter] operation. Changes are listed in the order they appear in the
     * text, not the order in which they were made. Overlapping changes are represented as a single
     * change.
     */
    @ExperimentalFoundationApi
    interface ChangeList {
        /**
         * The number of changes that have been performed.
         */
        val changeCount: Int

        /**
         * Returns the range in the [TextFieldBuffer] that was changed.
         *
         * @throws IndexOutOfBoundsException If [changeIndex] is not in [0, [changeCount]).
         */
        fun getRange(changeIndex: Int): TextRange

        /**
         * Returns the range in the original text that was replaced.
         *
         * @throws IndexOutOfBoundsException If [changeIndex] is not in [0, [changeCount]).
         */
        fun getOriginalRange(changeIndex: Int): TextRange
    }
}

/**
 * Insert [text] at the given [index] in this value. Pass 0 to insert [text] at the beginning of
 * this buffer, and pass [TextFieldBuffer.length] to insert [text] at the end of this buffer.
 *
 * This is equivalent to calling `replace(index, index, text)`.
 *
 * @param index The character offset at which to insert [text].
 * @param text The text to insert.
 *
 * @see TextFieldBuffer.replace
 * @see TextFieldBuffer.append
 * @see TextFieldBuffer.delete
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.insert(index: Int, text: String) {
    replace(index, index, text)
}

/**
 * Delete the text between [start] (inclusive) and [end] (exclusive). Pass 0 as [start] and
 * [TextFieldBuffer.length] as [end] to delete everything in this buffer.
 *
 * @param start The character offset of the first character to delete.
 * @param end The character offset of the first character after the deleted range.
 *
 * @see TextFieldBuffer.replace
 * @see TextFieldBuffer.append
 * @see TextFieldBuffer.insert
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.delete(start: Int, end: Int) {
    replace(start, end, "")
}

/**
 * Iterates over all the changes in this [ChangeList].
 *
 * Changes are iterated by index, so any changes made by [block] after the current one will be
 * visited by [block]. [block] should not make any new changes _before_ the current one or changes
 * will be visited more than once. If you need to make changes, consider using
 * [forEachChangeReversed].
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2ChangeIterationSample
 *
 * @see forEachChangeReversed
 */
@ExperimentalFoundationApi
inline fun ChangeList.forEachChange(
    block: (range: TextRange, originalRange: TextRange) -> Unit
) {
    var i = 0
    // Check the size every iteration in case more changes were performed.
    while (i < changeCount) {
        block(getRange(i), getOriginalRange(i))
        i++
    }
}

/**
 * Iterates over all the changes in this [ChangeList] in reverse order.
 *
 * Changes are iterated by index, so [block] should not perform any new changes before the current
 * one or changes may be skipped. [block] may make non-overlapping changes after the current one
 * safely, such changes will not be visited.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2ChangeReverseIterationSample
 *
 * @see forEachChange
 */
@ExperimentalFoundationApi
inline fun ChangeList.forEachChangeReversed(
    block: (range: TextRange, originalRange: TextRange) -> Unit
) {
    var i = changeCount - 1
    while (i >= 0) {
        block(getRange(i), getOriginalRange(i))
        i--
    }
}

@OptIn(ExperimentalFoundationApi::class)
private object EmptyChangeList : ChangeList {
    override val changeCount: Int
        get() = 0

    override fun getRange(changeIndex: Int): TextRange {
        throw IndexOutOfBoundsException()
    }

    override fun getOriginalRange(changeIndex: Int): TextRange {
        throw IndexOutOfBoundsException()
    }
}