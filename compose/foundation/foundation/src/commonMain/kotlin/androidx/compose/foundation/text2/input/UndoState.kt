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

/**
 * Defines an interactable undo history.
 */
@ExperimentalFoundationApi
class UndoState internal constructor(private val state: TextFieldState) {

    /**
     * Whether it is possible to execute a meaningful undo action right now. If this value is false,
     * calling `undo` would be a no-op.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    val canUndo: Boolean
        get() = state.textUndoManager.canUndo

    /**
     * Whether it is possible to execute a meaningful redo action right now. If this value is false,
     * calling `redo` would be a no-op.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    val canRedo: Boolean
        get() = state.textUndoManager.canRedo

    /**
     * Reverts the latest edit action or a group of actions that are merged together. Calling it
     * repeatedly can continue undoing the previous actions.
     */
    fun undo() {
        state.textUndoManager.undo(state)
    }

    /**
     * Re-applies a change that was previously reverted via [undo].
     */
    fun redo() {
        state.textUndoManager.redo(state)
    }

    /**
     * Clears all undo and redo history up to this point.
     */
    fun clearHistory() {
        state.textUndoManager.clearHistory()
    }
}
