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
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.util.fastForEach
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
    internal val changeTracker: ChangeTracker
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

    // region selection

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
     * Places the selection around the given range in characters.
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
            highlight = null
        }

    // endregion

    // region composition

    /**
     * Returns the composition information as TextRange. Returns null if no composition is set.
     *
     * Evaluates to null if it is set to a collapsed TextRange. Clears [composingAnnotations] when
     * set to null, including collapsed TextRange.
     */
    internal var composition: TextRange? = initialValue.composition
        private set(value) {
            // collapsed composition region is equivalent to no composition
            if (value == null || value.collapsed) {
                field = null
                // Do not deallocate an existing list. We will probably use it again.
                composingAnnotations?.clear()
            } else {
                field = value
            }
        }

    /**
     * List of annotations that are attached to the composing region. These are usually styling cues
     * like underline or different background colors.
     */
    internal var composingAnnotations:
        MutableVector<AnnotatedString.Range<AnnotatedString.Annotation>>? =
        if (!initialValue.composingAnnotations.isNullOrEmpty()) {
            MutableVector(initialValue.composingAnnotations.size) {
                initialValue.composingAnnotations[it]
            }
        } else {
            null
        }
        private set

    /** Helper function that returns true if the buffer has composing region */
    internal fun hasComposition(): Boolean = composition != null

    /** Clears current composition. */
    internal fun commitComposition() {
        composition = null
    }

    /**
     * Mark the specified area of the text as composition text.
     *
     * The empty range or reversed range is not allowed. Use [commitComposition] in case if you want
     * to clear composition.
     *
     * @param start the inclusive start offset of the composition
     * @param end the exclusive end offset of the composition
     * @param annotations Annotations that are attached to the composing region of text. This
     *   function does not check whether the given annotations are inside the composing region. It
     *   simply adds them to the current buffer while adjusting their range according to where the
     *   new composition region is set.
     * @throws IndexOutOfBoundsException if start or end offset is outside of current buffer
     * @throws IllegalArgumentException if start is larger than or equal to end. (reversed or
     *   collapsed range)
     */
    internal fun setComposition(start: Int, end: Int, annotations: List<PlacedAnnotation>? = null) {
        if (start < 0 || start > buffer.length) {
            throw IndexOutOfBoundsException(
                "start ($start) offset is outside of text region ${buffer.length}"
            )
        }
        if (end < 0 || end > buffer.length) {
            throw IndexOutOfBoundsException(
                "end ($end) offset is outside of text region ${buffer.length}"
            )
        }
        if (start >= end) {
            throw IllegalArgumentException("Do not set reversed or empty range: $start > $end")
        }

        composition = TextRange(start, end)

        this.composingAnnotations?.clear()
        if (!annotations.isNullOrEmpty()) {
            if (this.composingAnnotations == null) {
                this.composingAnnotations = mutableVectorOf()
            }
            annotations.fastForEach {
                // place the annotations at the correct indices in the buffer.
                this.composingAnnotations?.add(
                    it.copy(start = it.start + start, end = it.end + start)
                )
            }
        }
    }

    // endregion

    // region highlight

    /**
     * A highlighted range of text. This may be used to display handwriting gesture previews from
     * the IME.
     */
    internal var highlight: Pair<TextHighlightType, TextRange>? = null
        private set

    /**
     * Mark a range of text to be highlighted. This may be used to display handwriting gesture
     * previews from the IME.
     *
     * An empty or reversed range is not allowed.
     *
     * @param type the highlight type
     * @param start the inclusive start offset of the highlight
     * @param end the exclusive end offset of the highlight
     */
    internal fun setHighlight(type: TextHighlightType, start: Int, end: Int) {
        if (start >= end) {
            throw IllegalArgumentException("Do not set reversed or empty range: $start > $end")
        }
        val clampedStart = start.coerceIn(0, length)
        val clampedEnd = end.coerceIn(0, length)

        highlight = Pair(type, TextRange(clampedStart, clampedEnd))
    }

    /** Clear the highlighted text range. */
    internal fun clearHighlight() {
        highlight = null
    }

    // endregion

    // region editing

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

        commitComposition()
        highlight = null
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
        // On Android, IME calls are usually followed with an explicit change to selection.
        // Therefore it might seem unnecessary to adjust the selection here. However, this sort of
        // behavior is not expected for edits that are coming from the developer programmatically
        // or desktop APIs. So, we make sure that the selection is placed at a reasonable place
        // after any kind of edit.
        selectionInChars = adjustTextRange(selection, replaceStart, replaceEnd, newLength)
    }

    // endregion

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
     *   value is this buffer's current composition.
     */
    internal fun toTextFieldCharSequence(
        selection: TextRange = this.selection,
        composition: TextRange? = this.composition,
        composingAnnotations: List<PlacedAnnotation>? =
            this.composingAnnotations?.asMutableList()?.takeIf { it.isNotEmpty() },
    ): TextFieldCharSequence =
        TextFieldCharSequence(
            text = buffer.toString(),
            selection = selection,
            composition = composition,
            composingAnnotations = composingAnnotations
        )

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
 * Given [originalRange], calculates its new placement in the buffer after a region starting from
 * [replaceStart] (inclusive) ending at [replaceEnd] (exclusive) is deleted and [insertedTextLength]
 * number of characters are inserted at [replaceStart]. The rules of the adjustment are as follows;
 * - '||'; denotes the [originalRange]
 * - '\/'; denotes the [replaceStart], [replaceEnd]
 *
 * If the [originalRange]
 * - is before the replaced region, it remains in the same place.
 *     - abcd|efg|hijk\lmno/pqrs => abcd|efg|hijkxyzpqrs
 *     - TextRange(4, 7) => TextRange(4, 7)
 * - is after the replaced region, it is moved by the difference in length after replacement,
 *   essentially corresponding to the same part of the text.
 *     - abcd\efg/hijk|lmno|pqrs => abcdxyzxyzxyzhijk|lmno|pqrs
 *     - TextRange(11, 15) => TextRange(17, 21)
 * - fully wraps the replaced region, only the end is adjusted.
 *     - ab|cd\efg/hijklmno|pqrs => ab|cdxyzxyzxyzhijklmno|pqrs
 *     - TextRange(2, 15) => TextRange(2, 21)
 * - is inside the replaced region, range is collapsed and moved to the end of the replaced region.
 *     - ab\cd|efg|hijklmno/pqrs => abxyzxyz|pqrs
 *     - TextRange(4, 7) => TextRange(8, 8)
 * - collides with the replaced region at the start or at the end, it is adjusted so that the
 *   colliding range is not included anymore.
 *     - abcd|efg\hijk|lm/nopqrs => abcd|efg|xyzxyznopqrs
 *     - TextRange(4, 11) => TextRange(4, 7)
 */
internal fun adjustTextRange(
    originalRange: TextRange,
    replaceStart: Int,
    replaceEnd: Int,
    insertedTextLength: Int
): TextRange {
    var selStart = originalRange.min
    var selEnd = originalRange.max

    if (selEnd < replaceStart) {
        // The entire originalRange is before the insertion point – we don't have to adjust
        // the mark at all, so skip the math.
        return originalRange
    }

    if (selStart <= replaceStart && replaceEnd <= selEnd) {
        // The insertion is entirely inside the originalRange, move the end only.
        val diff = insertedTextLength - (replaceEnd - replaceStart)
        // Preserve "cursorness".
        if (selStart == selEnd) {
            selStart += diff
        }
        selEnd += diff
    } else if (selStart > replaceStart && selEnd < replaceEnd) {
        // originalRange is entirely inside replacement, move it to the end.
        selStart = replaceStart + insertedTextLength
        selEnd = replaceStart + insertedTextLength
    } else if (selStart >= replaceEnd) {
        // The entire originalRange is after the insertion, so shift everything forward.
        val diff = insertedTextLength - (replaceEnd - replaceStart)
        selStart += diff
        selEnd += diff
    } else if (replaceStart < selStart) {
        // Insertion is around start of originalRange, truncate start of originalRange.
        selStart = replaceStart + insertedTextLength
        selEnd += insertedTextLength - (replaceEnd - replaceStart)
    } else {
        // Insertion is around end of originalRange, truncate end of originalRange.
        selEnd = replaceStart
    }
    // should not validate
    return TextRange(selStart, selEnd)
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

/**
 * Normally [TextFieldBuffer] throws an [IllegalArgumentException] when an invalid selection change
 * is attempted. However internally and especially for selection ranges coming from the IME we
 * coerce the given numbers to a valid range to not crash. Also, IMEs sometimes send values like
 * `Int.MAX_VALUE` to move selection to end.
 */
internal fun TextFieldBuffer.setSelectionCoerced(start: Int, end: Int = start) {
    selection = TextRange(start.coerceIn(0, length), end.coerceIn(0, length))
}
