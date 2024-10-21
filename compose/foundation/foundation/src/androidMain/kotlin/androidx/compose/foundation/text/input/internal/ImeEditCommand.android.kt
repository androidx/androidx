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

package androidx.compose.foundation.text.input.internal

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.text.input.PlacedAnnotation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.adjustTextRange
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastCoerceIn

/**
 * IME ideally knows about the visual/presented text and not the raw state of [TextFieldState], so
 * the edit commands are sent according to what's seen by the user. This scope enables edit appliers
 * to convert text ranges from transformed space to original space before applying them. It is sort
 * of a reverse output transformation.
 *
 * This scope also carries batch editing capabilities so any edit op called on this scope can be
 * wrapped in a batch for various purposes.
 */
internal interface ImeEditCommandScope {
    /**
     * Transforms given [range] from transformed space to original space. Please note that this
     * function runs in the current TextFieldState. This means that the ongoing edits are not
     * visible to this transform function yet.
     */
    fun mapFromTransformed(range: TextRange): TextRange

    /**
     * Transforms given [range] from original space to transformed space. Please note that this
     * function runs in the current TextFieldState. This means that the ongoing edits are not
     * visible to this transform function yet.
     */
    fun mapToTransformed(range: TextRange): TextRange

    /**
     * Start a batch edit. All [edit] calls coming after [beginBatchEdit] are only executed after
     * corresponding [endBatchEdit] call comes. Returns true if a successful new batch is started.
     */
    fun beginBatchEdit(): Boolean

    /**
     * Add edit op to internal list with wrapping batch edit. It's not guaranteed by IME that batch
     * editing will be used for every operation. Instead, [ImeEditCommandScope] should create its
     * own mini batch for every edit op. These batches are only applied when batch depth reaches 0,
     * meaning that artificial batches won't be applied until the real batches are completed.
     */
    fun edit(block: TextFieldBuffer.() -> Unit)

    /** End an ongoing batch edit. Returns true if there's still a batch ongoing. */
    fun endBatchEdit(): Boolean
}

internal class DefaultImeEditCommandScope(
    private val transformedTextFieldState: TransformedTextFieldState
) : ImeEditCommandScope {

    /**
     * The depth of the batch session. 0 means no session.
     *
     * Sometimes InputConnection does not call begin/endBatchEdit functions before calling other
     * edit functions like commitText or setComposingText. StatelessInputConnection starts and
     * finishes a new artificial batch for every EditCommand to make sure that there is always an
     * ongoing batch. EditCommands are only applied when batchDepth reaches 0.
     */
    private var batchDepth: Int = 0

    /**
     * Transforms given [range] from transformed space to original space. Please note that this
     * function runs in the current TextFieldState. This means that the ongoing edits are not
     * visible to this transform function yet.
     */
    override fun mapFromTransformed(range: TextRange) =
        transformedTextFieldState.mapFromTransformed(range)

    /**
     * Transforms given [range] from original space to transformed space. Please note that this
     * function runs in the current TextFieldState. This means that the ongoing edits are not
     * visible to this transform function yet.
     */
    override fun mapToTransformed(range: TextRange) =
        transformedTextFieldState.mapToTransformed(range)

    private val editCommands = mutableVectorOf<TextFieldBuffer.() -> Unit>()

    override fun beginBatchEdit(): Boolean {
        batchDepth++
        return true
    }

    override fun edit(block: TextFieldBuffer.() -> Unit) {
        beginBatchEdit()
        editCommands.add(block)
        endBatchEdit()
    }

    override fun endBatchEdit(): Boolean {
        batchDepth--
        if (batchDepth == 0 && editCommands.isNotEmpty()) {
            // apply the changes to active input session in order.
            transformedTextFieldState.editUntransformedTextAsUser(
                restartImeIfContentChanges = false
            ) {
                editCommands.forEach { it.invoke(this) }
            }
            editCommands.clear()
        }
        return batchDepth > 0
    }
}

/**
 * Commit final [text] to the text box and set the new cursor position.
 *
 * See
 * [`commitText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)).
 *
 * @param text The text to commit.
 * @param newCursorPosition The cursor position after inserted text.
 */
internal fun ImeEditCommandScope.commitText(text: String, newCursorPosition: Int) = edit {
    // API description says to replace the ongoing composition text if there is any. Then, if
    // there is no composition text, insert text into cursor position or replace selection.
    val compositionRange = composition
    if (compositionRange != null) {
        imeReplace(compositionRange.start, compositionRange.end, text)
    } else {
        // In this editing buffer, insert into cursor or replace selection are equivalent.
        imeReplace(selection.start, selection.end, text)
    }

    // After replace function is called, the buffer places the cursor at the end of the
    // modified range.
    val newCursor = selection.start

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer =
        if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

    selection = TextRange(newCursorInBuffer.coerceIn(0, length))
}

/**
 * Mark a certain region of text as composing text.
 *
 * See
 * [`setComposingRegion`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingRegion(int,%2520int)).
 *
 * @param start The inclusive start offset of the composing region.
 * @param end The exclusive end offset of the composing region
 */
internal fun ImeEditCommandScope.setComposingRegion(start: Int, end: Int) = edit {
    // The API description says, different from SetComposingText, SetComposingRegion must
    // preserve the ongoing composition text and set new composition.
    if (hasComposition()) {
        commitComposition()
    }

    // Sanitize the input: reverse if reversed, clamped into valid range, ignore empty range.
    val clampedStart = start.coerceIn(0, length)
    val clampedEnd = end.coerceIn(0, length)
    if (clampedStart == clampedEnd) {
        // do nothing. empty composition range is not allowed.
    } else if (clampedStart < clampedEnd) {
        setComposition(clampedStart, clampedEnd)
    } else {
        setComposition(clampedEnd, clampedStart)
    }
}

/**
 * Replace the currently composing text with the given text, and set the new cursor position. Any
 * composing text set previously will be removed automatically.
 *
 * See
 * [`setComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingText(java.lang.CharSequence,%2520int)).
 *
 * @param text The composing text.
 * @param newCursorPosition The cursor position after setting composing text.
 * @param annotations Text annotations that IME attaches to the composing region. e.g. background
 *   color or underline styling.
 */
internal fun ImeEditCommandScope.setComposingText(
    text: String,
    newCursorPosition: Int,
    annotations: List<PlacedAnnotation>? = null
) = edit {
    val compositionRange = composition
    if (compositionRange != null) {
        // API doc says, if there is ongoing composing text, replace it with new text.
        imeReplace(compositionRange.start, compositionRange.end, text)
        if (text.isNotEmpty()) {
            setComposition(
                compositionRange.start,
                compositionRange.start + text.length,
                annotations
            )
        }
    } else {
        // If there is no composing text, insert composing text into cursor position with
        // removing selected text if any.
        val initialSelectionStart = selection.start
        imeReplace(initialSelectionStart, selection.end, text)
        if (text.isNotEmpty()) {
            setComposition(initialSelectionStart, initialSelectionStart + text.length, annotations)
        }
    }

    // After replace function is called, the editing buffer places the cursor at the end of the
    // modified range.
    val newCursor = selection.start

    // See above API description for the meaning of newCursorPosition.
    val newCursorInBuffer =
        if (newCursorPosition > 0) {
            newCursor + newCursorPosition - 1
        } else {
            newCursor + newCursorPosition - text.length
        }

    selection = TextRange(newCursorInBuffer.coerceIn(0, length))
}

/**
 * Delete [lengthBeforeCursor] characters of text before the current cursor position, and delete
 * [lengthAfterCursor] characters of text after the current cursor position, excluding the
 * selection.
 *
 * Before and after refer to the order of the characters in the string, not to their visual
 * representation.
 *
 * See
 * [`deleteSurroundingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingText(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in UTF-16 before the cursor to be deleted.
 *   Must be non-negative.
 * @param lengthAfterCursor The number of characters in UTF-16 after the cursor to be deleted. Must
 *   be non-negative.
 */
internal fun ImeEditCommandScope.deleteSurroundingText(
    lengthBeforeCursor: Int,
    lengthAfterCursor: Int
) = edit {
    requirePrecondition(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
        "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
            "$lengthBeforeCursor and $lengthAfterCursor respectively."
    }

    // calculate the end with safe addition since lengthAfterCursor can be set to e.g. Int.MAX
    // by the input
    val end = selection.end.addExactOrElse(lengthAfterCursor) { length }
    imeDelete(selection.end, minOf(end, length))

    // calculate the start with safe subtraction since lengthBeforeCursor can be set to e.g.
    // Int.MAX by the input
    val start = selection.start.subtractExactOrElse(lengthBeforeCursor) { 0 }
    imeDelete(maxOf(0, start), selection.start)
}

/**
 * A variant of [deleteSurroundingText]. The difference is that
 * * The lengths are supplied in code points, not in chars.
 * * This command does nothing if there are one or more invalid surrogate pairs in the requested
 *   range.
 *
 * See
 * [`deleteSurroundingTextInCodePoints`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingTextInCodePoints(int,%2520int)).
 *
 * @param lengthBeforeCursor The number of characters in Unicode code points before the cursor to be
 *   deleted. Must be non-negative.
 * @param lengthAfterCursor The number of characters in Unicode code points after the cursor to be
 *   deleted. Must be non-negative.
 */
internal fun ImeEditCommandScope.deleteSurroundingTextInCodePoints(
    lengthBeforeCursor: Int,
    lengthAfterCursor: Int
) = edit {
    requirePrecondition(lengthBeforeCursor >= 0 && lengthAfterCursor >= 0) {
        "Expected lengthBeforeCursor and lengthAfterCursor to be non-negative, were " +
            "$lengthBeforeCursor and $lengthAfterCursor respectively."
    }

    // Convert code point length into character length. Then call the common logic of the
    // DeleteSurroundingTextEditOp
    var beforeLenInChars = 0
    for (i in 0 until lengthBeforeCursor) {
        beforeLenInChars++
        if (selection.start > beforeLenInChars) {
            val lead = asCharSequence()[selection.start - beforeLenInChars - 1]
            val trail = asCharSequence()[selection.start - beforeLenInChars]

            if (isSurrogatePair(lead, trail)) {
                beforeLenInChars++
            }
        } else {
            // overflowing
            beforeLenInChars = selection.start
            break
        }
    }

    var afterLenInChars = 0
    for (i in 0 until lengthAfterCursor) {
        afterLenInChars++
        if (selection.end + afterLenInChars < length) {
            val lead = asCharSequence()[selection.end + afterLenInChars - 1]
            val trail = asCharSequence()[selection.end + afterLenInChars]

            if (isSurrogatePair(lead, trail)) {
                afterLenInChars++
            }
        } else {
            // overflowing
            afterLenInChars = length - selection.end
            break
        }
    }

    imeDelete(selection.end, selection.end + afterLenInChars)
    imeDelete(selection.start - beforeLenInChars, selection.start)
}

/**
 * Finishes the composing text that is currently active. This simply leaves the text as-is, removing
 * any special composing styling or other state that was around it. The cursor position remains
 * unchanged.
 *
 * See
 * [`finishComposingText`](https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#finishComposingText()).
 */
internal fun ImeEditCommandScope.finishComposingText() = edit { commitComposition() }

/**
 * Sets selection while converting the given range from transformed space to original space and
 * coercing to the buffer range.
 */
internal fun ImeEditCommandScope.setSelection(start: Int, end: Int) = edit {
    // First we need to find the length of transformed space so we can clamp [start] and [end]
    val transformedSpaceLength = mapToTransformed(TextRange(0, length))

    val clampedStart = start.fastCoerceIn(transformedSpaceLength.min, transformedSpaceLength.max)
    val clampedEnd = end.fastCoerceIn(transformedSpaceLength.min, transformedSpaceLength.max)
    selection = mapFromTransformed(TextRange(clampedStart, clampedEnd))
}

/**
 * Helper function that returns true when [high] is a Unicode high-surrogate code unit and [low] is
 * a Unicode low-surrogate code unit.
 */
private fun isSurrogatePair(high: Char, low: Char): Boolean =
    high.isHighSurrogate() && low.isLowSurrogate()

/**
 * Replace function wrapper to be called by edit commands that are originated from IME.
 *
 * IME editing is usually done in batches to maintain a composing range that helps with auto-correct
 * and auto-suggest. For example when typing a word through software keyboard, the IME doesn't send
 * a character typed event, instead it sends a replace command that swaps the currently composing
 * word with a character added version, i.e. replace("worl" -> "world").
 *
 * This unfortunately confuses the change trackers. Therefore, we coerce the changes from both sides
 * to find the absolute minimum version of the replace call so that a change tracker only reports
 * the actual intended edit, instead of what the IME sends as a command. For example when IME tells
 * us to replace "worl" with "world", this function converts this into "insert d".
 *
 * [imeReplace] also uses a different selection adjustment than regular [replace]. The selection is
 * always placed as a cursor at the end of the change.
 */
@VisibleForTesting
internal fun TextFieldBuffer.imeReplace(start: Int, end: Int, text: CharSequence) {
    val min = minOf(start, end)
    val max = maxOf(start, end)

    // coerce the replacement bounds before tracking change. This is mostly necessary for
    // composition based typing when each keystroke may trigger a replace function that looks
    // like "abcd" => "abcde".

    // b(351165334)
    // Since we are starting from the left hand side to compare the strings, when "abc" is
    // replaced with "aabc", it will be reported as an `a` is inserted at `TextRange(1)` instead
    // of the more logical possibility; `TextRange(0)`. This replace call cannot differentiate
    // between the two possible cases because we have no way of really knowing what was the
    // intention of the user beyond this replace call. We prefer to choose the more logical
    // explanation for right hand side since it's the more common direction of typing. This is
    // guaranteed by the fact that we start our coercion from left hand side, and finally apply
    // the right hand side.

    // coerce min
    var i = 0
    var cMin = min
    while (cMin < max && i < text.length && text[i] == asCharSequence()[cMin]) {
        i++
        cMin++
    }
    // coerce max
    var j = text.length
    var cMax = max
    while (cMax > cMin && j > i && text[j - 1] == asCharSequence()[cMax - 1]) {
        j--
        cMax--
    }

    if (cMin != cMax || i != j) {
        replace(start = cMin, end = cMax, text = text.subSequence(i, j))
    }

    // IME replace calls should always place the selection at the end of replaced region.
    // Also default replace behavior for composition is to cancel it. This is again true for
    // imeReplace. So we don't readjust the composition here.
    selection = TextRange(min + text.length)
}

/**
 * Similar to regular [TextFieldBuffer.delete] function but also maintains the composing region
 * instead of fully wiping it.
 */
@VisibleForTesting
internal fun TextFieldBuffer.imeDelete(start: Int, end: Int) {
    val initialComposition = composition

    val min = minOf(start, end)
    val max = maxOf(start, end)
    delete(min, max)

    // composition is lost by calling delete but we should restore it for delete calls that
    // originate from the IME
    initialComposition?.let {
        val adjustedComposition = adjustTextRange(initialComposition, min, max, 0)
        if (adjustedComposition.collapsed) {
            commitComposition()
        } else {
            setComposition(adjustedComposition.min, adjustedComposition.max)
        }
    }
}
