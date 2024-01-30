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

package androidx.compose.foundation.text2.input.internal.undo

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UndoManagerTest {

    @Test
    fun negativeCapacityThrows() {
        assertFailsWith<IllegalArgumentException>("Capacity must be a positive integer") {
            UndoManager<Int>(capacity = -1)
        }
    }

    @Test
    fun initialLowCapacityThrows_undoStack() {
        assertFailsWith<IllegalArgumentException>(
            getInitialCapacityErrorMessage(
                capacity = 1,
                totalStackSize = 2
            )
        ) {
            UndoManager(
                initialUndoStack = listOf(1, 2),
                capacity = 1
            )
        }
    }

    @Test
    fun initialLowCapacityThrows_redoStack() {
        assertFailsWith<IllegalArgumentException>(
            getInitialCapacityErrorMessage(
                capacity = 1,
                totalStackSize = 2
            )
        ) {
            UndoManager(
                initialRedoStack = listOf(1, 2),
                capacity = 1
            )
        }
    }

    @Test
    fun initialLowCapacityThrows_bothStacks() {
        assertFailsWith<IllegalArgumentException>(
            getInitialCapacityErrorMessage(
                capacity = 3,
                totalStackSize = 4
            )
        ) {
            UndoManager(
                initialUndoStack = listOf(1, 2),
                initialRedoStack = listOf(1, 2),
                capacity = 3
            )
        }
    }

    @Test
    fun commitSingleItem_canUndo() {
        val undoManager = UndoManager<Int>()
        undoManager.record(1)

        assertThat(undoManager.canUndo).isTrue()
        assertThat(undoManager.canRedo).isFalse()

        undoManager.undo()
        assertThat(undoManager.canUndo).isFalse()
        assertThat(undoManager.canRedo).isTrue()
    }

    @Test
    fun cannotRedoWithoutFirstUndo() {
        val undoManager = UndoManager<Int>()
        undoManager.record(1)
        undoManager.record(2)
        undoManager.record(3)

        assertThat(undoManager.canRedo).isFalse()
        assertFailsWith<IllegalStateException>(
            "It's an error to call redo while there is nothing to redo. " +
                "Please first check `canRedo` value before calling the `redo` function."
        ) {
            undoManager.redo()
        }
    }

    @Test
    fun commitItem_clearsRedoStack() {
        val undoManager = UndoManager<Int>()
        undoManager.record(1)
        undoManager.record(2)
        undoManager.record(3)

        undoManager.undo()
        assertThat(undoManager.canRedo).isTrue()

        undoManager.record(4)
        assertThat(undoManager.canRedo).isFalse()
    }

    @Test
    fun clearHistoryRemovesUndoAndRedo() {
        val undoManager = UndoManager<Int>()
        undoManager.record(1)
        undoManager.record(2)
        undoManager.record(3)

        undoManager.undo()

        assertThat(undoManager.canUndo).isTrue()
        assertThat(undoManager.canRedo).isTrue()

        undoManager.clearHistory()

        assertThat(undoManager.canUndo).isFalse()
        assertThat(undoManager.canRedo).isFalse()
    }

    @Test
    fun capacityOverflow_removesFromTheBottomOfStack() {
        val undoManager = UndoManager<Int>(capacity = 2)
        undoManager.record(1)
        undoManager.record(2)
        // overflow the capacity, undo history should forget the first item
        undoManager.record(3)

        var item = undoManager.undo()
        assertThat(item).isEqualTo(3)

        item = undoManager.undo()
        assertThat(item).isEqualTo(2)

        assertThat(undoManager.canUndo).isFalse()
    }

    @Test
    fun capacityOverflow_shouldRemoveRedoActionsFirst() {
        val undoManager = UndoManager<Int>(capacity = 20)
        undoManager.record(1)
        undoManager.record(2)
        undoManager.record(3)
        undoManager.record(4)

        undoManager.undo() // total size does not change  undo; 1-2-3 redo; 4
        undoManager.undo() // total size does not change  undo; 1-2 redo; 4-3

        // this should not remove anything from the undo stack, auto removed items from redo should
        // suffice
        undoManager.record(5)

        assertThat(undoManager.canUndo).isTrue()
        assertThat(undoManager.canRedo).isFalse()

        var item = undoManager.undo()
        assertThat(item).isEqualTo(5)

        item = undoManager.undo()
        assertThat(item).isEqualTo(2)

        item = undoManager.undo()
        assertThat(item).isEqualTo(1)
    }

    private fun getInitialCapacityErrorMessage(capacity: Int, totalStackSize: Int) =
        "Initial list of undo and redo operations have a size=($totalStackSize) greater " +
        "than the given capacity=($capacity)."
}
