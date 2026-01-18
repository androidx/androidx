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

package androidx.pdf.annotation.history

import androidx.pdf.annotation.AnnotationHandleIdGenerator
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.KeyedAnnotationRecord
import com.google.common.truth.Truth.assertThat
import createStampAnnotationWithPath
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnnotationRecordsHistoryManagerTest {

    private lateinit var historyManager: AnnotationRecordsHistoryManager

    @Before
    fun setup() {
        historyManager = AnnotationRecordsHistoryManager()
    }

    @Test
    fun recordAdd_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordAdd(keyedAnnotation)

        assertThat(historyManager.canUndo.value).isTrue()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun recordRemove_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordRemove(keyedAnnotation)

        assertThat(historyManager.canUndo.value).isTrue()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun recordUpdate_addsOperationToHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordUpdate(keyedAnnotation)

        assertThat(historyManager.canUndo.value).isTrue()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun undo_returnsLastOperationAndInvertsIt() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordAdd(keyedAnnotation)

        val undoneOperation = historyManager.undo()

        assertThat(undoneOperation).isNotNull()
        assertThat(undoneOperation?.recordType).isEqualTo(KeyedAnnotationRecord.Remove)
        assertThat(historyManager.canUndo.value).isFalse()
        assertThat(historyManager.canRedo.value).isTrue()
    }

    @Test
    fun redo_returnsUndoneOperation() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordAdd(keyedAnnotation)
        historyManager.undo()

        val redoneOperation = historyManager.redo()

        assertThat(redoneOperation).isNotNull()
        assertThat(redoneOperation?.recordType).isEqualTo(KeyedAnnotationRecord.Add)
        assertThat(historyManager.canUndo.value).isTrue()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun clear_emptiesHistory() {
        val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit1")
        val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

        historyManager.recordAdd(keyedAnnotation)
        historyManager.undo()

        historyManager.clear()

        assertThat(historyManager.canUndo.value).isFalse()
        assertThat(historyManager.canRedo.value).isFalse()
    }

    @Test
    fun historySizeLimit_isRespected() {
        val totalAnnotations = AnnotationRecordsHistoryManager.MAX_STACK_SIZE + 5
        for (i in 1..totalAnnotations) {
            val annotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 1)
            val key = AnnotationHandleIdGenerator.composeAnnotationId(pageNum = 1, id = "edit$i")
            val keyedAnnotation = KeyedPdfAnnotation(key, annotation)

            historyManager.recordAdd(keyedAnnotation)
        }

        for (i in 1..AnnotationRecordsHistoryManager.MAX_STACK_SIZE) {
            assertThat(historyManager.canUndo.value).isTrue()
            historyManager.undo()
        }
        assertThat(historyManager.canUndo.value).isFalse()
    }
}
