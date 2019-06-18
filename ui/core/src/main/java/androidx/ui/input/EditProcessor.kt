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

import androidx.annotation.RestrictTo
import androidx.ui.core.TextRange

/**
 * The core editing implementation
 *
 * This class accepts latest text edit state from developer and also receives edit operations from
 * IME.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class EditProcessor {

    // The previous editor state we passed back to the user of this class.
    private var mPreviousState: EditorState? = null

    // The editing buffer used for applying editor commands from IME.
    private var mBuffer: EditingBuffer =
        EditingBuffer(initialText = "", initialSelection = TextRange(0, 0))

    /**
     * Must be called whenever new editor state arrives.
     *
     * This method updates the internal editing buffer with the given editor state.
     * This method may tell the IME about the selection offset changes or extracted text changes.
     */
    fun onNewState(state: EditorState) {
        if (mPreviousState === state) {
            return
        }

        mBuffer = EditingBuffer(
            initialText = state.text,
            initialSelection = state.selection)

        // TODO(nona): Tell IME about the selection/extracted text changes.
    }

    /**
     * Must be called whenever new edit operations sent from IMEs arrives.
     *
     * This method updates internal editing buffer with the given edit operations and returns the
     * latest editor state representation of the editing buffer.
     */
    fun onEditCommands(ops: List<EditOperation>): EditorState {
        ops.forEach { it.process(mBuffer) }

        val newState = EditorState(
            text = mBuffer.toString(),
            selection = TextRange(mBuffer.selectionStart, mBuffer.selectionEnd),
            composition = if (mBuffer.hasComposition()) {
                TextRange(mBuffer.compositionStart, mBuffer.compositionEnd)
            } else {
                null
            })

        mPreviousState = newState
        return newState
    }
}