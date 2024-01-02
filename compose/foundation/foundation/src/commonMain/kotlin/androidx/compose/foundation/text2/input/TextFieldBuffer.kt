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
import androidx.compose.foundation.text2.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text2.input.internal.ChangeTracker
import androidx.compose.foundation.text2.input.internal.PartialGapBuffer
import androidx.compose.ui.text.TextRange

/**
 * A text buffer that can be edited, similar to [StringBuilder].
 *
 * This class provides methods for changing the text, such as:
 *  - [replace]
 *  - [append]
 *  - [insert]
 *  - [delete]
 *
 * This class also stores and tracks the cursor position or selection range. The cursor position is
 * just a selection range with zero length. The cursor and selection can be changed using methods
 * such as:
 *  - [placeCursorAfterCodepointAt]
 *  - [placeCursorAfterCharAt]
 *  - [placeCursorBeforeCodepointAt]
 *  - [placeCursorBeforeCharAt]
 *  - [placeCursorAtEnd]
 *  - [selectAll]
 *
 * To get one of these, and for usage samples, see [TextFieldState.edit]. Every change to the buffer
 * is tracked in a [ChangeList] which you can access via the [changes] property.
 */
@ExperimentalFoundationApi
class TextFieldBuffer internal constructor(
    initialValue: TextFieldCharSequence,
    initialChanges: ChangeTracker? = null,
    /**
     * The value reverted to when [revertAllChanges] is called. This is not necessarily
     * [initialValue] since the initial value may have already have had some intermediate changes
     * applied to it.
     */
    private val sourceValue: TextFieldCharSequence = initialValue,
) : Appendable {

    private val buffer = PartialGapBuffer(initialValue)

    /**
     * Lazily-allocated [ChangeTracker], initialized on the first text change.
     */
    private var changeTracker: ChangeTracker? =
        initialChanges?.let { ChangeTracker(initialChanges) }

    /**
     * The number of characters in the text field. This will be equal to or greater than
     * [codepointLength].
     */
    val length: Int get() = buffer.length

    /**
     * The number of codepoints in the text field. This will be equal to or less than [length].
     */
    val codepointLength: Int get() = Character.codePointCount(buffer, 0, length)

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
     * True if the selection range has non-zero length. If this is false, then the selection
     * represents the cursor.
     *
     * @see selectionInChars
     */
    @get:JvmName("hasSelection")
    val hasSelection: Boolean
        get() = !selectionInChars.collapsed

    /**
     * The selected range of characters.
     *
     * @see selectionInCodepoints
     */
    var selectionInChars: TextRange = initialValue.selectionInChars
        private set

    /**
     * The selected range of codepoints.
     *
     * @see selectionInChars
     */
    val selectionInCodepoints: TextRange
        get() = charsToCodepoints(selectionInChars)

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
    fun replace(start: Int, end: Int, text: CharSequence) {
        replace(start, end, text, 0, text.length)
    }

    /**
     * Replaces the text between [start] (inclusive) and [end] (exclusive) in this value with
     * [text], and records the change in [changes].
     *
     * @param start The character offset of the first character to replace.
     * @param end The character offset of the first character after the text to replace.
     * @param text The text to replace the range `[start, end)` with.
     * @param textStart The character offset of the first character in [text] to copy.
     * @param textEnd The character offset after the last character in [text] to copy.
     *
     * @see append
     * @see insert
     * @see delete
     */
    internal fun replace(
        start: Int,
        end: Int,
        text: CharSequence,
        textStart: Int = 0,
        textEnd: Int = text.length
    ) {
        require(start <= end) { "Expected start=$start <= end=$end" }
        require(textStart <= textEnd) { "Expected textStart=$textStart <= textEnd=$textEnd" }
        onTextWillChange(start, end, textEnd - textStart)
        buffer.replace(start, end, text, textStart, textEnd)
    }

    /**
     * Similar to `replace(0, length, newText)` but only records a change if [newText] is actually
     * different from the current buffer value.
     */
    internal fun setTextIfChanged(newText: CharSequence) {
        findCommonPrefixAndSuffix(buffer, newText) { thisStart, thisEnd, newStart, newEnd ->
            replace(thisStart, thisEnd, newText, newStart, newEnd)
        }
    }

    // Doc inherited from Appendable.
    // This append overload should be first so it ends up being the target of links to this method.
    override fun append(text: CharSequence?): Appendable = apply {
        if (text != null) {
            onTextWillChange(length, length, text.length)
            buffer.replace(buffer.length, buffer.length, text)
        }
    }

    // Doc inherited from Appendable.
    override fun append(text: CharSequence?, start: Int, end: Int): Appendable = apply {
        if (text != null) {
            onTextWillChange(length, length, end - start)
            buffer.replace(buffer.length, buffer.length, text.subSequence(start, end))
        }
    }

    // Doc inherited from Appendable.
    override fun append(char: Char): Appendable = apply {
        onTextWillChange(length, length, 1)
        buffer.replace(buffer.length, buffer.length, char.toString())
    }

    /**
     * Called just before the text contents are about to change.
     *
     * @param replaceStart The first offset to be replaced (inclusive).
     * @param replaceEnd The last offset to be replaced (exclusive).
     * @param newLength The length of the replacement.
     */
    private fun onTextWillChange(replaceStart: Int, replaceEnd: Int, newLength: Int) {
        (changeTracker ?: ChangeTracker().also { changeTracker = it })
            .trackChange(replaceStart, replaceEnd, newLength)

        // Adjust selection.
        val start = minOf(replaceStart, replaceEnd)
        val end = maxOf(replaceStart, replaceEnd)
        var selStart = selectionInChars.min
        var selEnd = selectionInChars.max

        if (selEnd < start) {
            // The entire selection is before the insertion point – we don't have to adjust the
            // mark at all, so skip the math.
            return
        }

        if (selStart <= start && end <= selEnd) {
            // The insertion is entirely inside the selection, move the end only.
            val diff = newLength - (end - start)
            // Preserve "cursorness".
            if (selStart == selEnd) {
                selStart += diff
            }
            selEnd += diff
        } else if (selStart > start && selEnd < end) {
            // Selection is entirely inside replacement, move it to the end.
            selStart = start + newLength
            selEnd = start + newLength
        } else if (selStart >= end) {
            // The entire selection is after the insertion, so shift everything forward.
            val diff = newLength - (end - start)
            selStart += diff
            selEnd += diff
        } else if (start < selStart) {
            // Insertion is around start of selection, truncate start of selection.
            selStart = start + newLength
            selEnd += newLength - (end - start)
        } else {
            // Insertion is around end of selection, truncate end of selection.
            selEnd = start
        }
        selectionInChars = TextRange(selStart, selEnd)
    }

    /**
     * Returns the [Char] at [index] in this buffer.
     */
    fun charAt(index: Int): Char = buffer[index]

    override fun toString(): String = buffer.toString()

    /**
     * Returns a [CharSequence] backed by this buffer. Any subsequent changes to this buffer will
     * be visible in the returned sequence as well.
     */
    fun asCharSequence(): CharSequence = buffer

    private fun clearChangeList() {
        changeTracker?.clearChanges()
    }

    /**
     * Revert all changes made to this value since it was created.
     *
     * After calling this method, this object will be in the same state it was when it was initially
     * created, and [changes] will be empty.
     */
    fun revertAllChanges() {
        replace(0, length, sourceValue.toString())
        selectionInChars = sourceValue.selectionInChars
        clearChangeList()
    }

    /**
     * Places the cursor before the codepoint at the given index.
     *
     * If [index] is inside an invalid run, the cursor will be placed at the nearest earlier index.
     *
     * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the
     * end of the field, after the last character, pass index
     * [TextFieldBuffer.codepointLength] or call [placeCursorAtEnd].
     *
     * @param index Codepoint index to place cursor before, should be in range 0 to
     * [TextFieldBuffer.codepointLength], inclusive.
     *
     * @see placeCursorBeforeCharAt
     * @see placeCursorAfterCodepointAt
     */
    fun placeCursorBeforeCodepointAt(index: Int) {
        requireValidIndex(index, startExclusive = true, endExclusive = false, inCodepoints = true)
        val charIndex = codepointIndexToCharIndex(index)
        selectionInChars = TextRange(charIndex)
    }

    /**
     * Places the cursor before the character at the given index.
     *
     * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
     * nearest earlier index.
     *
     * To place the cursor at the beginning of the field, pass index 0. To place the cursor at the
     * end of the field, after the last character, pass index [TextFieldBuffer.length] or call
     * [placeCursorAtEnd].
     *
     * @param index Character index to place cursor before, should be in range 0 to
     * [TextFieldBuffer.length], inclusive.
     *
     * @see placeCursorBeforeCodepointAt
     * @see placeCursorAfterCharAt
     */
    fun placeCursorBeforeCharAt(index: Int) {
        requireValidIndex(index, startExclusive = true, endExclusive = false, inCodepoints = false)
        selectionInChars = TextRange(index)
    }

    /**
     * Places the cursor after the codepoint at the given index.
     *
     * If [index] is inside an invalid run, the cursor will be placed at the nearest later index.
     *
     * To place the cursor at the end of the field, after the last character, pass index
     * [TextFieldBuffer.codepointLength] or call [placeCursorAtEnd].
     *
     * @param index Codepoint index to place cursor after, should be in range 0 (inclusive) to
     * [TextFieldBuffer.codepointLength] (exclusive).
     *
     * @see placeCursorAfterCharAt
     * @see placeCursorBeforeCodepointAt
     */
    fun placeCursorAfterCodepointAt(index: Int) {
        requireValidIndex(index, startExclusive = false, endExclusive = true, inCodepoints = true)
        val charIndex = codepointIndexToCharIndex((index + 1).coerceAtMost(codepointLength))
        selectionInChars = TextRange(charIndex)
    }

    /**
     * Places the cursor after the character at the given index.
     *
     * If [index] is inside a surrogate pair or other invalid run, the cursor will be placed at the
     * nearest later index.
     *
     * To place the cursor at the end of the field, after the last character, pass index
     * [TextFieldBuffer.length] or call [placeCursorAtEnd].
     *
     * @param index Character index to place cursor after, should be in range 0 (inclusive) to
     * [TextFieldBuffer.length] (exclusive).
     *
     * @see placeCursorAfterCodepointAt
     * @see placeCursorBeforeCharAt
     */
    fun placeCursorAfterCharAt(index: Int) {
        requireValidIndex(index, startExclusive = false, endExclusive = true, inCodepoints = false)
        selectionInChars = TextRange((index + 1).coerceAtMost(length))
    }

    /**
     * Places the selection around the given [range] in codepoints.
     *
     * If the start or end of [range] fall inside invalid runs, the values will be adjusted to the
     * nearest earlier and later codepoints, respectively.
     *
     * To place the start of the selection at the beginning of the field, pass index 0. To place the
     * end of the selection at the end of the field, after the last codepoint, pass index
     * [TextFieldBuffer.codepointLength]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCodepointAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [TextFieldBuffer.codepointLength], inclusive.
     *
     * @see selectCharsIn
     */
    fun selectCodepointsIn(range: TextRange) {
        requireValidRange(range, inCodepoints = true)
        selectionInChars = codepointsToChars(range)
    }

    /**
     * Places the selection around the given [range] in characters.
     *
     * If the start or end of [range] fall inside surrogate pairs or other invalid runs, the values will
     * be adjusted to the nearest earlier and later characters, respectively.
     *
     * To place the start of the selection at the beginning of the field, pass index 0. To place the end
     * of the selection at the end of the field, after the last character, pass index
     * [TextFieldBuffer.length]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCharAt].
     *
     * @param range Codepoint range of the selection, should be in range 0 to
     * [TextFieldBuffer.length], inclusive.
     *
     * @see selectCodepointsIn
     */
    fun selectCharsIn(range: TextRange) {
        requireValidRange(range, inCodepoints = false)
        selectionInChars = range
    }

    internal fun toTextFieldCharSequence(
        composition: TextRange? = null
    ): TextFieldCharSequence = TextFieldCharSequence(
        buffer.toString(),
        selection = selectionInChars,
        composition = composition
    )

    private fun requireValidIndex(
        index: Int,
        startExclusive: Boolean,
        endExclusive: Boolean,
        inCodepoints: Boolean
    ) {
        var start = if (startExclusive) 0 else -1
        var end = if (endExclusive) length else length + 1

        // The "units" of the range in the error message should match the units passed in.
        // If the input was in codepoint indices, the output should be in codepoint indices.
        if (inCodepoints) {
            start = charIndexToCodepointIndex(start)
            end = charIndexToCodepointIndex(end)
        }

        require(index in start until end) {
            val unit = if (inCodepoints) "codepoints" else "chars"
            "Expected $index to be in [$start, $end) $unit"
        }
    }

    private fun requireValidRange(range: TextRange, inCodepoints: Boolean) {
        // The "units" of the range in the error message should match the units passed in.
        // If the input was in codepoint indices, the output should be in codepoint indices.
        val validRange = TextRange(0, length)
            .let { if (inCodepoints) charsToCodepoints(it) else it }
        require(range in validRange) {
            val unit = if (inCodepoints) "codepoints" else "chars"
            "Expected $range to be in $validRange ($unit)"
        }
    }

    private fun codepointsToChars(range: TextRange): TextRange = TextRange(
        codepointIndexToCharIndex(range.start),
        codepointIndexToCharIndex(range.end)
    )

    private fun charsToCodepoints(range: TextRange): TextRange = TextRange(
        charIndexToCodepointIndex(range.start),
        charIndexToCodepointIndex(range.end),
    )

    // TODO Support actual codepoints.
    private fun codepointIndexToCharIndex(index: Int): Int = index
    private fun charIndexToCodepointIndex(index: Int): Int = index

    /**
     * The ordered list of non-overlapping and discontinuous changes performed on a
     * [TextFieldBuffer] during the current [edit][TextFieldState.edit] or
     * [filter][InputTransformation.transformInput] operation. Changes are listed in the order they appear in the
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
 * Places the cursor at the end of the text.
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.placeCursorAtEnd() {
    placeCursorBeforeCharAt(length)
}

/**
 * Places the selection around all the text.
 */
@ExperimentalFoundationApi
fun TextFieldBuffer.selectAll() {
    selectCharsIn(TextRange(0, length))
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

/**
 * Finds the common prefix and suffix between [a] and [b] and then reports the ranges of each that
 * excludes those. The values are reported via an (inline) callback instead of a return value to
 * avoid having to allocate something to hold them. If the [CharSequence]s are identical, the
 * callback is not invoked.
 *
 * E.g. given `a="abcde"` and `b="abbbdefe"`, the middle diff for `a` is `"ab[cd]e"` and for `b` is
 * `ab[bbdef]e`, so reports `aMiddle=TextRange(2, 4)` and `bMiddle=TextRange(2, 7)`.
 */
internal inline fun findCommonPrefixAndSuffix(
    a: CharSequence,
    b: CharSequence,
    onFound: (aPrefixStart: Int, aSuffixStart: Int, bPrefixStart: Int, bSuffixStart: Int) -> Unit
) {
    var aStart = 0
    var aEnd = a.length
    var bStart = 0
    var bEnd = b.length

    // If either one is empty, the diff range is the entire non-empty one.
    if (a.isNotEmpty() && b.isNotEmpty()) {
        var prefixFound = false
        var suffixFound = false

        do {
            if (!prefixFound) {
                if (a[aStart] == b[bStart]) {
                    aStart += 1
                    bStart += 1
                } else {
                    prefixFound = true
                }
            }
            if (!suffixFound) {
                if (a[aEnd - 1] == b[bEnd - 1]) {
                    aEnd -= 1
                    bEnd -= 1
                } else {
                    suffixFound = true
                }
            }
        } while (
        // As soon as we've completely traversed one of the strings, if the other hasn't also
        // finished being traversed then we've found the diff region.
            aStart < aEnd && bStart < bEnd &&
            // If we've found the end of the common prefix and the start of the common suffix we're
            // done.
            !(prefixFound && suffixFound)
        )
    }

    if (aStart >= aEnd && bStart >= bEnd) {
        return
    }

    onFound(aStart, aEnd, bStart, bEnd)
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
