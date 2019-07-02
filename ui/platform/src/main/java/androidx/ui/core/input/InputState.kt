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

package androidx.ui.core.input

import android.view.inputmethod.ExtractedText
import androidx.ui.core.TextRange
import androidx.ui.core.substring

/**
 * Stores an input state for IME
 *
 * IME can request editor state with calling getTextBeforeCursor, getSelectedText, etc.
 * This class stores a snapshot of the input state of the edit buffer and provide utility functions
 * for answering these information retrieval requests.
 */
internal data class InputState(
    /**
     * A text visible to IME
     */
    val text: String,

    /**
     * A selection range visible to IME.
     * The selection range must be valid range in the given text.
     */
    val selection: TextRange,

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
        text.substring(Math.max(0, selection.start - maxChars), selection.start)

    /**
     * Helper function for getting text after selection range.
     */
    fun getTextAfterSelection(maxChars: Int): String =
        text.substring(selection.end, Math.min(selection.end + maxChars, text.length))

    /**
     * Helper function for getting text currently selected.
     */
    fun getSelectedText(): String = text.substring(selection)

    /**
     * Make to ExtractedText
     */
    fun toExtractedText(): ExtractedText = ExtractedText().apply {
        text = this@InputState.text
        partialEndOffset = this@InputState.text.length
        partialStartOffset = -1 // -1 means full text
        selectionStart = selection.start
        selectionEnd = selection.end
        flags = ExtractedText.FLAG_SINGLE_LINE // TODO(nona): Support multiline text.
    }
}
