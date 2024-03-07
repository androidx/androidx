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
import androidx.compose.foundation.text.input.internal.toCharArray
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn

/**
 * An immutable snapshot of the contents of a [TextFieldState].
 *
 * This class is a [CharSequence] and directly represents the text being edited. It also stores
 * the current [selection] of the field, which may either represent the cursor (if the
 * selection is [collapsed][TextRange.collapsed]) or the selection range.
 *
 * This class also may contain the range being composed by the IME, if any, although this is not
 * exposed.
 *
 * @see TextFieldBuffer
 */
sealed interface TextFieldCharSequence : CharSequence {
    /**
     * The selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    @ExperimentalFoundationApi
    val selection: TextRange

    /**
     * Composition range created by IME. If null, there is no composition range.
     *
     * Input service composition is an instance of text produced by IME. An example visual for the
     * composition is that the currently composed word is visually separated from others with
     * underline, or text background. For description of composition please check
     * [W3C IME Composition](https://www.w3.org/TR/ime-api/#ime-composition)
     *
     * Composition can only be set by the system.
     */
    @ExperimentalFoundationApi
    val composition: TextRange?

    /**
     * Returns true if the text in this object is equal to the text in [other], disregarding any
     * other properties of this (such as selection) or [other].
     */
    fun contentEquals(other: CharSequence): Boolean

    abstract override fun toString(): String
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

fun TextFieldCharSequence(
    text: String = "",
    selection: TextRange = TextRange.Zero
): TextFieldCharSequence = TextFieldCharSequenceWrapper(text, selection, composition = null)

internal fun TextFieldCharSequence(
    text: CharSequence,
    selection: TextRange,
    composition: TextRange? = null
): TextFieldCharSequence = TextFieldCharSequenceWrapper(text, selection, composition)

/**
 * Returns the backing CharSequence object that this TextFieldCharSequence is wrapping. This is
 * useful for external equality comparisons that cannot use [TextFieldCharSequence.contentEquals].
 */
internal fun TextFieldCharSequence.getBackingCharSequence(): CharSequence {
    return when (this) {
        is TextFieldCharSequenceWrapper -> this.text
    }
}

/**
 * Copies the contents of this sequence from [[sourceStartIndex], [sourceEndIndex]) into
 * [destination] starting at [destinationOffset].
 */
internal fun TextFieldCharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    sourceStartIndex: Int,
    sourceEndIndex: Int
) = (this as TextFieldCharSequenceWrapper).toCharArray(
    destination,
    destinationOffset,
    sourceStartIndex,
    sourceEndIndex
)

@OptIn(ExperimentalFoundationApi::class)
private class TextFieldCharSequenceWrapper(
    text: CharSequence,
    selection: TextRange,
    composition: TextRange?
) : TextFieldCharSequence {

    /**
     * If this TextFieldCharSequence is actually a copy of another, make sure to use the backing
     * CharSequence object to stop unnecessary nesting and logic that depends on exact equality of
     * CharSequence comparison that's using [CharSequence.equals].
     */
    val text: CharSequence = if (text is TextFieldCharSequenceWrapper) {
        text.text
    } else {
        text
    }

    override val length: Int
        get() = text.length

    override val selection: TextRange = selection.coerceIn(0, text.length)

    override val composition: TextRange? = composition?.coerceIn(0, text.length)

    override operator fun get(index: Int): Char = text[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        text.subSequence(startIndex, endIndex)

    override fun toString(): String = text.toString()

    override fun contentEquals(other: CharSequence): Boolean = text.contentEquals(other)

    fun toCharArray(
        destination: CharArray,
        destinationOffset: Int,
        sourceStartIndex: Int,
        sourceEndIndex: Int
    ) {
        text.toCharArray(destination, destinationOffset, sourceStartIndex, sourceEndIndex)
    }

    /**
     * Returns true if [other] is a [TextFieldCharSequence] with the same contents, text, and composition.
     * To compare just the text, call [contentEquals].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as TextFieldCharSequenceWrapper

        if (selection != other.selection) return false
        if (composition != other.composition) return false
        if (!contentEquals(other.text)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + selection.hashCode()
        result = 31 * result + (composition?.hashCode() ?: 0)
        return result
    }
}

/**
 * Returns the text before the selection.
 *
 * @param maxChars maximum number of characters (inclusive) before the minimum value in
 * [TextFieldCharSequence.selection].
 *
 * @see TextRange.min
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldCharSequence.getTextBeforeSelection(maxChars: Int): CharSequence =
    subSequence(kotlin.math.max(0, selection.min - maxChars), selection.min)

/**
 * Returns the text after the selection.
 *
 * @param maxChars maximum number of characters (exclusive) after the maximum value in
 * [TextFieldCharSequence.selection].
 *
 * @see TextRange.max
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldCharSequence.getTextAfterSelection(maxChars: Int): CharSequence =
    subSequence(selection.max, kotlin.math.min(selection.max + maxChars, length))

/**
 * Returns the currently selected text.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldCharSequence.getSelectedText(): CharSequence =
    subSequence(selection.min, selection.max)
