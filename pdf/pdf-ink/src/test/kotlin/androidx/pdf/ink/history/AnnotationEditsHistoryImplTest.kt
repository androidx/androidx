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

package androidx.pdf.ink.history

import androidx.pdf.annotation.models.AnnotationEditOperation
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditOperation
import com.google.common.truth.Truth.assertThat
import createDummyPdfAnnotationData
import java.util.UUID
import kotlin.concurrent.thread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnnotationEditsHistoryImplTest {

    private lateinit var history: AnnotationEditsHistoryImpl

    @Before
    fun setUp() {
        history = AnnotationEditsHistoryImpl(maxSize = 10)
    }

    @Test
    fun addEntry_addsToUndoStackAndClearsRedoStack() {
        val op1 = createDummyAnnotationEditOperation(pageNum = 0)
        val op2 = createDummyAnnotationEditOperation(pageNum = 0)
        history.addEntry(op1)
        history.addEntry(op2)

        assertThat(history.canUndo()).isTrue()

        val data1 = history.undo()
        assertThat(data1).isNotNull()
        assertThat(data1!!.op).isEqualTo(EditOperation.Remove)
        assertThat(data1.edit).isEqualTo(op2.edit)
        assertThat(history.canRedo()).isTrue()
        assertThat(history.canUndo()).isTrue()

        val data2 = history.undo()
        assertThat(data2).isNotNull()
        assertThat(data2!!.op).isEqualTo(EditOperation.Remove)
        assertThat(data2.edit).isEqualTo(op1.edit)
        assertThat(history.canRedo()).isTrue()
        assertThat(history.canUndo()).isFalse()
    }

    @Test
    fun addEntry_whenMaxSizeReached_removesOldestEdit() {
        for (i in 1..11) {
            val op = createDummyAnnotationEditOperation(i)
            history.addEntry(op)
        }

        val undoEdit = history.undo()
        assertThat(undoEdit).isNotNull()
        assertThat(undoEdit!!.edit.editId.pageNum).isEqualTo(11)
        // Oldest edit (1) should be gone.
        var lastUndo: AnnotationEditOperation? = null
        while (history.canUndo()) {
            lastUndo = history.undo()
        }

        assertThat(lastUndo).isNotNull()
        assertThat(lastUndo!!.edit.editId.pageNum).isEqualTo(2)
    }

    @Test
    fun undoAndRedo_addOperation_movesEditToRedoStack() {
        val op = createDummyAnnotationEditOperation(0)
        history.addEntry(op)
        history.undo()

        assertThat(history.canRedo()).isTrue()
        assertThat(history.redo()).isEqualTo(op)
        assertThat(history.canRedo()).isFalse()
    }

    @Test
    fun undoAndRedo_removeOperation_movesEditToRedoStack() {
        val op = createDummyAnnotationEditOperation(0, op = EditOperation.Remove)
        history.addEntry(op)
        val op1 = history.undo()

        assertThat(op1).isNotNull()
        assertThat(op1!!.op).isEqualTo(EditOperation.Add)

        assertThat(history.canRedo()).isTrue()
        assertThat(history.redo()).isEqualTo(op)
        assertThat(history.canRedo()).isFalse()
    }

    @Test
    fun redo_movesEditToUndoStack() {
        val op = createDummyAnnotationEditOperation(0)
        history.addEntry(op)
        history.undo()
        history.redo()

        assertThat(history.canUndo()).isTrue()
        assertThat(history.canRedo()).isFalse()

        val op1 = history.undo()
        assertThat(op1).isNotNull()
        assertThat(op1!!.op).isEqualTo(EditOperation.Remove)
    }

    @Test
    fun clear_emptiesBothStacks() {
        val op = createDummyAnnotationEditOperation(0)
        history.addEntry(op)
        history.undo()
        history.clear()

        assertThat(history.canUndo()).isFalse()
        assertThat(history.canRedo()).isFalse()
    }

    @Test
    fun threadSafety_isMaintained() {
        val threads = mutableListOf<Thread>()
        val edits = List(100) { createDummyAnnotationEditOperation(0) }

        repeat(10) {
            threads += thread {
                for (edit in edits) {
                    history.addEntry(edit)
                }
            }
        }
        threads.forEach { it.join() }

        var undoCount = 0
        while (history.canUndo()) {
            history.undo()
            undoCount++
        }
        assertThat(undoCount).isEqualTo(history.maxSize)
    }

    private fun createDummyAnnotationEditOperation(
        pageNum: Int,
        op: EditOperation.Operation = EditOperation.Add,
    ): AnnotationEditOperation {
        val editId = EditId(pageNum, value = UUID.randomUUID().toString())
        val annotationData = createDummyPdfAnnotationData(editId)
        return AnnotationEditOperation(op, annotationData)
    }
}
