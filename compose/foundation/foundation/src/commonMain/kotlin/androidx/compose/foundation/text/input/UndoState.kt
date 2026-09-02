/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

/**
 * Controls the undo and redo history for a [TextFieldState].
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldUndoSample
 * @see TextFieldState.undoState
 */
public class UndoState internal constructor(private val state: TextFieldState) {

    /**
     * Whether an [undo] action can currently be performed.
     *
     * If this value is `false`, calling [undo] is a no-op. This property is backed by snapshot
     * state and will cause recomposition when its value changes.
     *
     * @see undo
     * @see canRedo
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    public val canUndo: Boolean
        get() = state.textUndoManager.canUndo

    /**
     * Whether a [redo] action can currently be performed.
     *
     * If this value is `false`, calling [redo] is a no-op. This property is backed by snapshot
     * state and will cause recomposition when its value changes.
     *
     * @see redo
     * @see canUndo
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    public val canRedo: Boolean
        get() = state.textUndoManager.canRedo

    /**
     * Reverts the latest edit action or a group of actions that are merged together.
     *
     * If [canUndo] is `false`, this is a no-op. Calling it repeatedly continues undoing previous
     * actions.
     *
     * @see canUndo
     * @see redo
     */
    public fun undo() {
        state.textUndoManager.undo(state)
    }

    /**
     * Re-applies a change that was previously reverted via [undo].
     *
     * If [canRedo] is `false`, this is a no-op.
     *
     * @see canRedo
     * @see undo
     */
    public fun redo() {
        state.textUndoManager.redo(state)
    }

    /**
     * Clears all undo and redo history up to this point.
     *
     * Calling this sets both [canUndo] and [canRedo] to `false`.
     */
    public fun clearHistory() {
        state.textUndoManager.clearHistory()
    }
}
