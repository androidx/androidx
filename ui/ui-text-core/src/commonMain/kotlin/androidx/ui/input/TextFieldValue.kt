/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.input

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.listSaver
import androidx.ui.text.TextRange
import androidx.ui.text.substring
import kotlin.math.max
import kotlin.math.min

/**
 * Stores an input state for IME
 *
 * IME can request editor state with calling getTextBeforeCursor, getSelectedText, etc.
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering these information retrieval requests.
 */
@Immutable
@Deprecated(
    "Please use androidx.ui.input.TextFieldValue instead",
    ReplaceWith("TextFieldValue", "androidx.ui.input.TextFieldValue")
)
data class EditorValue(
    /**
     * A text visible to IME
     */
    val text: String = "",

    /**
     * A selection range visible to IME.
     * The selection range must be valid range in the given text.
     */
    val selection: TextRange = TextRange.Zero,

    /**
     * A composition range visible to IME.
     * If null, there is no composition range.
     * If non-null, the composition range must be valid range in the given text.
     */
    val composition: TextRange? = null
) {
    /**
     * Helper function for getting text before selection range.
     */
    fun getTextBeforeSelection(maxChars: Int): String =
        text.substring(max(0, selection.min - maxChars), selection.min)

    /**
     * Helper function for getting text after selection range.
     */
    fun getTextAfterSelection(maxChars: Int): String =
        text.substring(selection.max, min(selection.max + maxChars, text.length))

    /**
     * Helper function for getting text currently selected.
     */
    fun getSelectedText(): String = text.substring(selection)
}

/**
 * A class holding information about the editing state.
 *
 * The input service updates text selection or cursor as well as text. You can observe and
 * control the selection, cursor and text altogether.
 *
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering IME requests such as getTextBeforeCursor, getSelectedText.
 *
 * @param text the text will be rendered
 * @param selection the selection range. If the selection is collapsed, it represents cursor
 * location. Do not specify outside of the text buffer.
 * @param composition A composition range visible to IME.If null, there is no composition range.
 * If non-null, the composition range must be valid range in the given text.
 */
@Immutable
data class TextFieldValue(
    @Stable
    val text: String = "",
    @Stable
    val selection: TextRange = TextRange.Zero,
    @Stable
    val composition: TextRange? = null
) {
    companion object {
        /**
         * The default [Saver] implementation for [TextFieldValue].
         */
        val Saver = listSaver<TextFieldValue, Any>(
            save = {
                listOf(it.text, it.selection.start, it.selection.end)
            },
            restore = {
                TextFieldValue(it[0] as String, TextRange(it[1] as Int, it[2] as Int))
            }
        )
    }
}

/**
 * Helper function for getting text before selection range.
 */
fun TextFieldValue.getTextBeforeSelection(maxChars: Int): String =
    text.substring(max(0, selection.min - maxChars), selection.min)

/**
 * Helper function for getting text after selection range.
 */
fun TextFieldValue.getTextAfterSelection(maxChars: Int): String =
    text.substring(selection.max, min(selection.max + maxChars, text.length))

/**
 * Helper function for getting text currently selected.
 */
fun TextFieldValue.getSelectedText(): String = text.substring(selection)
