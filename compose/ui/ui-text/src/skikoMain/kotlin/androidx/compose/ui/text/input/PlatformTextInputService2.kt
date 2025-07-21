/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.TextRange

/**
 * An adapter for `foundation.TextFieldState`, which is not accessible in the `ui` module.
 *
 * The text itself is provided by the [CharSequence] supertype.
 * The selection is provided by [selection].
 * The composition is provided by [composition].
 */
@ExperimentalComposeUiApi
interface TextEditorState : CharSequence {
    /**
     * The selection in the text field.
     */
    val selection: TextRange

    /**
     * The composition in the text field.
     */
    val composition: TextRange?
}

/**
 * The scope in which the text input service implementations can make changes to the
 * [TextEditorState].
 */
@ExperimentalComposeUiApi
interface TextEditingScope {
    /**
     * Deletes text around the cursor.
     *
     * This intends to replicate [DeleteSurroundingTextInCodePointsCommand].
     */
    fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int)

    /**
     * Commits text and repositions the cursor.
     *
     * This intends to replicate [CommitTextCommand].
     */
    fun commitText(text: CharSequence, newCursorPosition: Int)

    /**
     * Sets the composing text and repositions the cursor.
     *
     * This intends to replicate [SetComposingTextCommand].
     */
    fun setComposingText(text: CharSequence, newCursorPosition: Int)

    /**
     * Finishes composing, leaving the text as-is.
     *
     * This intends to replicate [FinishComposingTextCommand].
     */
    fun finishComposingText()
}