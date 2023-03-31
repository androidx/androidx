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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState.TextEditFilter
import androidx.compose.foundation.text2.input.internal.EditProcessor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The editable text state of a text field, including both the text itself and position of the
 * cursor or selection.
 *
 * To change the state, call the [edit] method, edit the text by calling [MutableTextFieldValue]
 * methods, then return an edit result to specify the new location of the cursor or selection by
 * calling one of the methods on [MutableTextFieldValue].
 */
@ExperimentalFoundationApi
class TextFieldState(
    initialValue: TextFieldValue = TextFieldValue(),
    filter: TextEditFilter = TextEditFilter.Default
) {
    internal var editProcessor = EditProcessor(initialValue, filter)

    val value: TextFieldValue
        get() = editProcessor.value

    /**
     * Runs [block] with a mutable version of the current state. The block can make changes to the
     * text, and must specify the new location of the cursor or selection by return a
     * [TextFieldEditResult].
     *
     * @see setTextAndPlaceCursorAtEnd
     * @see setTextAndSelectAll
     * @see MutableTextFieldValue.placeCursorAtEnd
     * @see MutableTextFieldValue.placeCursorBeforeChar
     * @see MutableTextFieldValue.placeCursorBeforeCodepoint
     * @see MutableTextFieldValue.selectChars
     * @see MutableTextFieldValue.selectCodepoints
     */
    inline fun edit(block: MutableTextFieldValue.() -> TextFieldEditResult) {
        val mutableValue = startEdit(value)
        val result = mutableValue.block()
        // Don't run the filter for programmatic edits.
        commitEdit(mutableValue, result)
    }

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun startEdit(value: TextFieldValue): MutableTextFieldValue =
        MutableTextFieldValue(value)

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun commitEdit(newValue: MutableTextFieldValue, result: TextFieldEditResult) {
        val newSelection = result.calculateSelection(value, newValue)
        val finalValue = newValue.toTextFieldValue(newSelection)
        editProcessor.reset(finalValue)
    }

    @ExperimentalFoundationApi
    fun interface TextEditFilter {

        fun filter(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue

        companion object {
            val Default = TextEditFilter { _, new -> new }
        }
    }
}

/**
 * Sets the text in this [TextFieldState] to [text], replacing any text that was previously there,
 * and places the cursor at the end of the new text.
 *
 * To perform more complicated edits on the text, call [TextFieldState.edit].
 */
@ExperimentalFoundationApi
fun TextFieldState.setTextAndPlaceCursorAtEnd(text: String) {
    edit {
        replace(0, length, text)
        placeCursorAtEnd()
    }
}

/**
 * Sets the text in this [TextFieldState] to [text], replacing any text that was previously there,
 * and selects all the text.
 *
 * To perform more complicated edits on the text, call [TextFieldState.edit].
 */
@ExperimentalFoundationApi
fun TextFieldState.setTextAndSelectAll(text: String) {
    edit {
        replace(0, length, text)
        selectAll()
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldState.deselect() {
    if (!value.selection.collapsed) {
        editProcessor.reset(value.copy(selection = TextRange.Zero, composition = TextRange.Zero))
    }
}