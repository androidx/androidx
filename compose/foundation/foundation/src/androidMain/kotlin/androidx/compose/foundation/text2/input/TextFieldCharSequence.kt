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
import androidx.compose.ui.text.constrain

/**
 * An immutable snapshot of the contents of a [TextFieldState].
 *
 * This class is a [CharSequence] and directly represents the text being edited. It also stores
 * the current [selectionInChars] of the field, which may either represent the cursor (if the
 * selection is [collapsed][TextRange.collapsed]) or the selection range.
 *
 * This class also may contain the range being composed by the IME, if any, although this is not
 * exposed.
 *
 * @see TextFieldBuffer
 */
@ExperimentalFoundationApi
sealed interface TextFieldCharSequence : CharSequence {
    /**
     * The selection range. If the selection is collapsed, it represents cursor
     * location. When selection range is out of bounds, it is constrained with the text length.
     */
    val selectionInChars: TextRange

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
    val compositionInChars: TextRange?

    /**
     * Returns true if the text in this object is equal to the text in [other], disregarding any
     * other properties of this (such as selection) or [other].
     */
    fun contentEquals(other: CharSequence): Boolean

    abstract override fun toString(): String
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

@ExperimentalFoundationApi
fun TextFieldCharSequence(
    text: String = "",
    selection: TextRange = TextRange.Zero
): TextFieldCharSequence = TextFieldCharSequenceWrapper(text, selection, composition = null)

@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldCharSequence(
    text: CharSequence,
    selection: TextRange,
    composition: TextRange? = null
): TextFieldCharSequence = TextFieldCharSequenceWrapper(text, selection, composition)

@OptIn(ExperimentalFoundationApi::class)
private class TextFieldCharSequenceWrapper(
    private val text: CharSequence,
    selection: TextRange,
    composition: TextRange?
) : TextFieldCharSequence {

    override val length: Int
        get() = text.length

    override val selectionInChars: TextRange = selection.constrain(0, text.length)

    override val compositionInChars: TextRange? = composition?.constrain(0, text.length)

    override operator fun get(index: Int): Char = text[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        text.subSequence(startIndex, endIndex)

    override fun toString(): String = text.toString()

    override fun contentEquals(other: CharSequence): Boolean = text.contentEquals(other)

    /**
     * Returns true if [other] is a [TextFieldCharSequence] with the same contents, text, and composition.
     * To compare just the text, call [contentEquals].
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextFieldCharSequenceWrapper

        if (selectionInChars != other.selectionInChars) return false
        if (compositionInChars != other.compositionInChars) return false
        if (!contentEquals(other.text)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + selectionInChars.hashCode()
        result = 31 * result + (compositionInChars?.hashCode() ?: 0)
        return result
    }
}