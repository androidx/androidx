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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.text.input.TextFieldBuffer.ChangeList
import androidx.compose.foundation.text.input.internal.ChangeTracker
import androidx.compose.foundation.text.input.internal.OffsetMappingCalculator
import androidx.compose.foundation.text.input.internal.PartialGapBuffer
import androidx.compose.ui.text.TextRange
import kotlin.jvm.JvmName

/**
 * A text buffer that can be edited, similar to [StringBuilder].
 *
 * This class provides methods for changing the text, such as:
 * - [replace]
 * - [append]
 * - [insert]
 * - [delete]
 *
 * This class also stores and tracks the cursor position or selection range. The cursor position is
 * just a selection range with zero length. The cursor and selection can be changed using methods
 * such as:
 * - [placeCursorAfterCharAt]
 * - [placeCursorBeforeCharAt]
 * - [placeCursorAtEnd]
 * - [selectAll]
 *
 * To get one of these, and for usage samples, see [TextFieldState.edit]. Every change to the buffer
 * is tracked in a [ChangeList] which you can access via the [changes] property.
 */
class TextFieldBuffer
internal constructor(
    initialValue: TextFieldCharSequence,
    initialChanges: ChangeTracker? = null,
    internal val originalValue: TextFieldCharSequence = initialValue,
    private val offsetMappingCalculator: OffsetMappingCalculator? = null,
) : Appendable {

    private val buffer = PartialGapBuffer(initialValue)

    private var backingChangeTracker: ChangeTracker? =
        initialChanges?.let { ChangeTracker(initialChanges) }

    /** Lazily-allocated [ChangeTracker], initialized on the first access. */
    private val changeTracker: ChangeTracker
        get() = backingChangeTracker ?: ChangeTracker().also { backingChangeTracker = it }

    /** The number of characters in the text field. */
    val length: Int
        get() = buffer.length

    /**
     * Original text content of the buffer before any changes were applied. Calling
     * [revertAllChanges] will set the contents of this buffer to this value.
     */
    val originalText: CharSequence
        get() = originalValue.text

    /**
     * Original selection before the changes. Calling [revertAllChanges] will set the selection to
     * this value.
     */
    val originalSelection: TextRange
        get() = originalValue.selection

    /**
     * The [ChangeList] represents the changes made to this value and is inherently mutable. This
     * means that the returned [ChangeList] always reflects the complete list of changes made to
     * this value at any given time, even those made after reading this property.
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldChangeIterationSample
     * @sample androidx.compose.foundation.samples.BasicTextFieldChangeReverseIterationSample
     */
    @ExperimentalFoundationApi
    val changes: ChangeList
        get() = changeTracker

    /**
     * True if the selection range has non-zero length. If this is false, then the selection
     * represents the cursor.
     *
     * @see selection
     */
    @get:JvmName("hasSelection")
    val hasSelection: Boolean
        get() = !selection.collapsed

    /**
     * Backing TextRange for [selection]. Each method that updates selection has its own validation.
     * This backing field does not further validate its own state.
     */
    private var selectionInChars: TextRange = initialValue.selection

    /**
     * The selected range of characters.
     *
     * Places the selection around the given [range] in characters.
     *
     * If the start or end of TextRange fall inside surrogate pairs or other invalid runs, the
     * values will be adjusted to the nearest earlier and later characters, respectively.
     *
     * To place the start of the selection at the beginning of the field, set this value to
     * [TextRange.Zero]. To place the end of the selection at the end of the field, after the last
     * character, pass [TextFieldBuffer.length]. Passing a zero-length range is the same as calling
     * [placeCursorBeforeCharAt].
     */
    var selection: TextRange
        get() = selectionInChars
        set(value) {
            requireValidRange(value)
            selectionInChars = value
        }

    /**
     * Replaces the text between [start] (inclusive) and [end] (exclusive) in this value with
     * [text], and records the change in [changes].
     *
     * @param start The character offset of the first character to replace.
     * @param end The character offset of the first character after the text to replace.
     * @param text The text to replace the range `[start, end)` with.
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
        requirePrecondition(start <= end) { "Expected start=$start <= end=$end" }
        requirePrecondition(textStart <= textEnd) {
            "Expected textStart=$textStart <= textEnd=$textEnd"
        }
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
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun append(text: CharSequence?): Appendable = apply {
        if (text != null) {
            onTextWillChange(length, length, text.length)
            buffer.replace(buffer.length, buffer.length, text)
        }
    }

    // Doc inherited from Appendable.
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun append(text: CharSequence?, start: Int, end: Int): Appendable = apply {
        if (text != null) {
            onTextWillChange(length, length, end - start)
            buffer.replace(buffer.length, buffer.length, text.subSequence(start, end))
        }
    }

    // Doc inherited from Appendable.
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
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
        changeTracker.trackChange(replaceStart, replaceEnd, newLength)
        offsetMappingCalculator?.recordEditOperation(replaceStart, replaceEnd, newLength)

        // Adjust selection.
        val start = minOf(replaceStart, replaceEnd)
        val end = maxOf(replaceStart, replaceEnd)
        var selStart = selection.min
        var selEnd = selection.max

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
        // should not validate
        selectionInChars = TextRange(selStart, selEnd)
    }

    /** Returns the [Char] at [index] in this buffer. */
    fun charAt(index: Int): Char = buffer[index]

    override fun toString(): String = buffer.toString()

    /**
     * Returns a [CharSequence] backed by this buffer. Any subsequent changes to this buffer will be
     * visible in the returned sequence as well.
     */
    fun asCharSequence(): CharSequence = buffer

    private fun clearChangeList() {
        changeTracker.clearChanges()
    }

    /**
     * Revert all changes made to this value since it was created.
     *
     * After calling this method, this object will be in the same state it was when it was initially
     * created, and [changes] will be empty.
     */
    fun revertAllChanges() {
        replace(0, length, originalValue.toString())
        selection = originalValue.selection
        clearChangeList()
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
     *   [TextFieldBuffer.length], inclusive.
     * @see placeCursorAfterCharAt
     */
    fun placeCursorBeforeCharAt(index: Int) {
        requireValidIndex(index, startExclusive = true, endExclusive = false)
        // skip further validation
        selectionInChars = TextRange(index)
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
     *   [TextFieldBuffer.length] (exclusive).
     * @see placeCursorBeforeCharAt
     */
    fun placeCursorAfterCharAt(index: Int) {
        requireValidIndex(index, startExclusive = false, endExclusive = true)
        // skip further validation
        selectionInChars = TextRange((index + 1).coerceAtMost(length))
    }

    /**
     * Returns an immutable [TextFieldCharSequence] that has the same contents of this buffer.
     *
     * @param selection The selection for the returned [TextFieldCharSequence]. Default value is
     *   this buffer's selection. Passing a different value in here _only_ affects the return value,
     *   it does not change the current selection in the buffer.
     * @param composition The composition range for the returned [TextFieldCharSequence]. Default
     *   value is no composition (null).
     */
    internal fun toTextFieldCharSequence(
        selection: TextRange = this.selection,
        composition: TextRange? = null
    ): TextFieldCharSequence =
        TextFieldCharSequence(buffer.toString(), selection = selection, composition = composition)

    private fun requireValidIndex(index: Int, startExclusive: Boolean, endExclusive: Boolean) {
        val start = if (startExclusive) 0 else -1
        val end = if (endExclusive) length else length + 1

        requirePrecondition(index in start until end) { "Expected $index to be in [$start, $end)" }
    }

    private fun requireValidRange(range: TextRange) {
        val validRange = TextRange(0, length)
        requirePrecondition(range in validRange) { "Expected $range to be in $validRange" }
    }

    /**
     * The ordered list of non-overlapping and discontinuous changes performed on a
     * [TextFieldBuffer] during the current [edit][TextFieldState.edit] or
     * [filter][InputTransformation.transformInput] operation. Changes are listed in the order they
     * appear in the text, not the order in which they were made. Overlapping changes are
     * represented as a single change.
     */
    interface ChangeList {
        /** The number of changes that have been performed. */
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
 * @see TextFieldBuffer.replace
 * @see TextFieldBuffer.append
 * @see TextFieldBuffer.delete
 */
fun TextFieldBuffer.insert(index: Int, text: String) {
    replace(index, index, text)
}

/**
 * Delete the text between [start] (inclusive) and [end] (exclusive). Pass 0 as [start] and
 * [TextFieldBuffer.length] as [end] to delete everything in this buffer.
 *
 * @param start The character offset of the first character to delete.
 * @param end The character offset of the first character after the deleted range.
 * @see TextFieldBuffer.replace
 * @see TextFieldBuffer.append
 * @see TextFieldBuffer.insert
 */
fun TextFieldBuffer.delete(start: Int, end: Int) {
    replace(start, end, "")
}

/** Places the cursor at the end of the text. */
fun TextFieldBuffer.placeCursorAtEnd() {
    placeCursorBeforeCharAt(length)
}

/** Places the selection around all the text. */
fun TextFieldBuffer.selectAll() {
    selection = TextRange(0, length)
}

/**
 * Iterates over all the changes in this [ChangeList].
 *
 * Changes are iterated by index, so any changes made by [block] after the current one will be
 * visited by [block]. [block] should not make any new changes _before_ the current one or changes
 * will be visited more than once. If you need to make changes, consider using
 * [forEachChangeReversed].
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldChangeIterationSample
 * @see forEachChangeReversed
 */
@ExperimentalFoundationApi
inline fun ChangeList.forEachChange(block: (range: TextRange, originalRange: TextRange) -> Unit) {
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
 * @sample androidx.compose.foundation.samples.BasicTextFieldChangeReverseIterationSample
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
 * E.g. given `a="abcde"` and `b="abbbdefe"`, the middle diff for `a` is `"ab|cd|e"` and for `b` is
 * `ab|bbdef|e`, so reports `aMiddle=TextRange(2, 4)` and `bMiddle=TextRange(2, 7)`.
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
            aStart < aEnd &&
                bStart < bEnd &&
                // If we've found the end of the common prefix and the start of the common suffix
                // we're
                // done.
                !(prefixFound && suffixFound)
        )
    }

    if (aStart >= aEnd && bStart >= bEnd) {
        return
    }

    onFound(aStart, aEnd, bStart, bEnd)
}
