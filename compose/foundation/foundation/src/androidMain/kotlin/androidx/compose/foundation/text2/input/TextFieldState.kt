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
import androidx.compose.foundation.text2.input.internal.EditProcessor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange

/**
 * The editable text state of a text field, including both the text itself and position of the
 * cursor or selection.
 *
 * To change the state, call [edit].
 */
@ExperimentalFoundationApi
class TextFieldState(
    initialText: String = "",
    initialSelectionInChars: TextRange = TextRange.Zero
) {

    internal var editProcessor =
        EditProcessor(TextFieldCharSequence(initialText, initialSelectionInChars))

    val text: TextFieldCharSequence
        get() = editProcessor.value

    /**
     * Runs [block] with a mutable version of the current state. The block can make changes to the
     * text, and must specify the new location of the cursor or selection by returning a
     * [TextEditResult] such as [placeCursorAtEnd] or [selectAll] (see the documentation on
     * [TextEditResult] for the full list of prebuilt results).
     *
     * @sample androidx.compose.foundation.samples.BasicTextField2StateEditSample
     * @see setTextAndPlaceCursorAtEnd
     * @see setTextAndSelectAll
     */
    inline fun edit(block: TextFieldBuffer.() -> TextEditResult) {
        val mutableValue = startEdit(text)
        val result = mutableValue.block()
        commitEdit(mutableValue, result)
    }

    override fun toString(): String =
        "TextFieldState(selection=${text.selectionInChars}, text=\"$text\")"

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun startEdit(value: TextFieldCharSequence): TextFieldBuffer =
        TextFieldBuffer(value)

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun commitEdit(newValue: TextFieldBuffer, result: TextEditResult) {
        val newSelection = result.calculateSelection(text, newValue)
        val finalValue = newValue.toTextFieldCharSequence(newSelection)
        editProcessor.reset(finalValue)
    }

    /**
     * Saves and restores a [TextFieldState] for [rememberSaveable].
     *
     * @see rememberTextFieldState
     */
    // Preserve nullability since this is public API.
    @Suppress("RedundantNullableReturnType")
    object Saver : androidx.compose.runtime.saveable.Saver<TextFieldState, Any> {
        override fun SaverScope.save(value: TextFieldState): Any? = listOf(
            value.text.toString(),
            value.text.selectionInChars.start,
            value.text.selectionInChars.end
        )

        override fun restore(value: Any): TextFieldState? {
            val (text, selectionStart, selectionEnd) = value as List<*>
            return TextFieldState(
                initialText = text as String,
                initialSelectionInChars = TextRange(
                    start = selectionStart as Int,
                    end = selectionEnd as Int
                )
            )
        }
    }
}

/**
 * Create and remember a [TextFieldState]. The state is remembered using [rememberSaveable] and so
 * will be saved and restored with the composition.
 *
 * If you need to store a [TextFieldState] in another object, use the [TextFieldState.Saver] object
 * to manually save and restore the state.
 */
@ExperimentalFoundationApi
@Composable
fun rememberTextFieldState(): TextFieldState =
    rememberSaveable(saver = TextFieldState.Saver) {
        TextFieldState()
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
    if (!text.selectionInChars.collapsed) {
        edit {
            selectCharsIn(TextRange.Zero)
        }
    }
}