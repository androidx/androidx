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

package androidx.pdf.annotation

import androidx.pdf.annotation.history.AnnotationRecordsHistoryManager
import androidx.pdf.annotation.manager.FakePdfAnnotationsManager
import androidx.pdf.annotation.models.PdfAnnotation
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfAnnotationsEditorTest {
    private lateinit var editor: PdfAnnotationsEditor
    private lateinit var fakeManager: FakePdfAnnotationsManager
    private lateinit var historyManager: AnnotationRecordsHistoryManager

    private val pageNum = 0
    private val annotA = TestPdfAnnotation(pageNum)
    private val annotB = TestPdfAnnotation(pageNum)

    @Before
    fun setup() {
        fakeManager = FakePdfAnnotationsManager()
        historyManager = AnnotationRecordsHistoryManager()

        editor = PdfAnnotationsEditor(historyManager, fakeManager)
    }

    @Test
    fun addDraftAnnotation_addsToManager_andRecordsHistory() = runTest {
        val id = editor.addDraftAnnotation(annotA)

        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotA)

        assertThat(historyManager.canUndo.value).isTrue()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun removeAnnotation_validId_removesFromManager_andRecordsHistory() = runTest {
        val id = editor.addDraftAnnotation(annotA)

        val removed = editor.removeAnnotation(id)

        assertThat(removed).isEqualTo(annotA)
        assertThat(fakeManager.getAnnotation(id)).isNull()

        assertThat(historyManager.canUndo.value).isTrue()
    }

    @Test
    fun removeAnnotation_invalidId_returnsNull_andDoesNotRecordHistory() = runTest {
        editor.addDraftAnnotation(annotA)

        val result = editor.removeAnnotation("non_existent_id")

        assertThat(result).isNull()

        editor.undo()
        assertThat(fakeManager.count()).isEqualTo(0)
    }

    @Test
    fun updateAnnotation_validId_updatesManager_andRecordsHistory() = runTest {
        val id = editor.addDraftAnnotation(annotA)

        editor.updateAnnotation(id, annotB)

        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotB)

        assertThat(historyManager.canUndo.value).isTrue()
    }

    @Test
    fun undo_addOperation_removesAnnotation() = runTest {
        val id = editor.addDraftAnnotation(annotA)

        editor.undo()

        assertThat(fakeManager.getAnnotation(id)).isNull()
        assertThat(historyManager.canUndo.value).isFalse()
    }

    @Test
    fun undo_removeOperation_restoresAnnotation() = runTest {
        val id = editor.addDraftAnnotation(annotA)
        editor.removeAnnotation(id)

        assertThat(fakeManager.getAnnotation(id)).isNull()

        editor.undo() // Undoes the REMOVE

        val restored = fakeManager.getAnnotation(id)
        assertThat(restored).isEqualTo(annotA)
    }

    @Test
    fun undo_updateOperation_revertsToOldContent() = runTest {
        val id = editor.addDraftAnnotation(annotA)
        editor.updateAnnotation(id, annotB)

        editor.undo()

        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotA)
    }

    @Test
    fun redo_addOperation_reAddsAnnotation() = runTest {
        val id = editor.addDraftAnnotation(annotA)
        editor.undo()

        editor.redo()

        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotA)
    }

    @Test
    fun redo_removeOperation_reRemovesAnnotation() = runTest {
        val id = editor.addDraftAnnotation(annotA)
        editor.removeAnnotation(id)
        editor.undo()

        editor.redo()

        assertThat(fakeManager.getAnnotation(id)).isNull()
    }

    @Test
    fun complexFlow_addUpdateRemoveUndoAll() = runTest {
        // 1. Add A
        val id = editor.addDraftAnnotation(annotA)

        // 2. Update to B
        editor.updateAnnotation(id, annotB)

        // 3. Remove B
        editor.removeAnnotation(id)
        assertThat(fakeManager.getAnnotation(id)).isNull()

        // 4. Undo Remove -> Should restore B
        editor.undo()
        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotB)

        // 5. Undo Update -> Should restore A
        editor.undo()
        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotA)

        // 6. Undo Add -> Should remove A
        editor.undo()
        assertThat(fakeManager.getAnnotation(id)).isNull()
    }

    @Test
    fun clear_resetsUndoStackAndChanges() = runTest {
        editor.addDraftAnnotation(annotA)
        assertThat(historyManager.canUndo.value).isTrue()

        editor.clear()

        assertThat(fakeManager.count()).isEqualTo(0)
        assertThat(historyManager.canUndo.value).isFalse()

        // Undo should do nothing now
        editor.undo()
        assertThat(fakeManager.count()).isEqualTo(0)
    }

    @Test
    fun undo_emptyStack_doesNothing() = runTest {
        editor.undo()
        assertThat(fakeManager.count()).isEqualTo(0)
    }

    @Test
    fun redo_emptyStack_doesNothing() = runTest {
        editor.redo()
        assertThat(fakeManager.count()).isEqualTo(0)
    }

    @Test
    fun add_thenUndo_thenRedo_thenUndo_doesNothing() = runTest {
        val id = editor.addDraftAnnotation(annotA)

        editor.undo()
        assertThat(fakeManager.getAnnotation(id)).isNull()

        editor.redo()
        assertThat(fakeManager.getAnnotation(id)).isEqualTo(annotA)

        editor.undo()
        assertThat(fakeManager.getAnnotation(id)).isNull()
    }

    class TestPdfAnnotation(override val pageNum: Int) : PdfAnnotation(pageNum)
}
