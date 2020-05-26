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
data class EditorValue(
    /**
     * A text visible to IME
     */
    val text: String = "",

    /**
     * A selection range visible to IME.
     * The selection range must be valid range in the given text.
     */
    val selection: TextRange = TextRange(0, 0),

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
