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

import android.text.Editable
import android.text.Selection
import android.view.inputmethod.BaseInputConnection
import androidx.ui.core.TextRange
import androidx.ui.input.EditorState

internal fun Editable.toEditorState(): EditorState {
    val selectionStart = Selection.getSelectionStart(this) // inclusive
    val selectionEnd = Selection.getSelectionEnd(this) // exclusive
    val composingStart = BaseInputConnection.getComposingSpanStart(this) // inclusive
    val composingEnd = BaseInputConnection.getComposingSpanEnd(this) // exclusive

    val selection = if (selectionStart == -1 || selectionEnd == -1) {
        // Translate no selection to (0, 0) selection (caret)
        TextRange(0, 0)
    } else {
        TextRange(selectionStart, selectionEnd)
    }

    val composition = if (composingStart == -1 || composingEnd == -1) {
        null
    } else {
        TextRange(composingStart, composingEnd)
    }

    return EditorState(
        text = this.toString(),
        selection = selection,
        composition = composition
    )
}

internal fun EditorState.toEditable(): Editable {
    return Editable.Factory.getInstance().newEditable(text).apply {
        Selection.setSelection(this, selection.start, selection.end)
    }
}