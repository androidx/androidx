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

import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditOperation
import com.google.common.truth.Truth.assertThat
import createStampAnnotationWithPath
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnnotationEditsHistoryManagerTest {

    private lateinit var historyManager: AnnotationEditsHistoryManager

    @Before
    fun setup() {
        historyManager = AnnotationEditsHistoryManager()
    }

    @Test
    fun recordAdd_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")

        historyManager.recordAdd(editId, annotation)

        assertThat(historyManager.canUndo()).isTrue()
        assertThat(historyManager.canRedo()).isFalse()
    }

    @Test
    fun recordRemove_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")

        historyManager.recordRemove(editId, annotation)

        assertThat(historyManager.canUndo()).isTrue()
        assertThat(historyManager.canRedo()).isFalse()
    }

    @Test
    fun recordUpdate_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")

        historyManager.recordUpdate(editId, annotation)

        assertThat(historyManager.canUndo()).isTrue()
        assertThat(historyManager.canRedo()).isFalse()
    }

    @Test
    fun undo_returnsLastOperationAndInvertsIt() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")
        historyManager.recordAdd(editId, annotation)

        val undoneOperation = historyManager.undo()

        assertThat(undoneOperation).isNotNull()
        assertThat(undoneOperation?.op).isEqualTo(EditOperation.Remove)
        assertThat(historyManager.canUndo()).isFalse()
        assertThat(historyManager.canRedo()).isTrue()
    }

    @Test
    fun redo_returnsUndoneOperation() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")
        historyManager.recordAdd(editId, annotation)
        historyManager.undo()

        val redoneOperation = historyManager.redo()

        assertThat(redoneOperation).isNotNull()
        assertThat(redoneOperation?.op).isEqualTo(EditOperation.Add)
        assertThat(historyManager.canUndo()).isTrue()
        assertThat(historyManager.canRedo()).isFalse()
    }

    @Test
    fun clear_emptiesHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val editId = EditId(1, "edit1")
        historyManager.recordAdd(editId, annotation)
        historyManager.undo()

        historyManager.clear()

        assertThat(historyManager.canUndo()).isFalse()
        assertThat(historyManager.canRedo()).isFalse()
    }

    @Test
    fun historySizeLimit_isRespected() {
        for (i in 1..25) {
            val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 1)
            val editId = EditId(1, "edit$i")
            historyManager.recordAdd(editId, annotation)
        }

        // Undo should only be possible 20 times (the max size)
        for (i in 1..20) {
            assertThat(historyManager.canUndo()).isTrue()
            historyManager.undo()
        }
        assertThat(historyManager.canUndo()).isFalse()
    }
}
