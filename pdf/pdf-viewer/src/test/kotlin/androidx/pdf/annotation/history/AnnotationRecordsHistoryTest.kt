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
import androidx.pdf.annotation.models.KeyedAnnotationRecord
import com.google.common.truth.Truth.assertThat
import createDummyKeyedPdfAnnotation
import java.util.UUID
import kotlin.concurrent.thread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AnnotationRecordsHistoryImplTest {

    private lateinit var history: AnnotationRecordsHistoryImpl

    @Before
    fun setUp() {
        history = AnnotationRecordsHistoryImpl(maxSize = 10)
    }

    @Test
    fun addEntry_addsToUndoStackAndClearsRedoStack() {
        val record1 = createDummyKeyedAnnotationRecord(pageNum = 0)
        val record2 = createDummyKeyedAnnotationRecord(pageNum = 0)
        history.addEntry(record1)
        history.addEntry(record2)

        assertThat(history.canUndo.value).isTrue()

        val data1 = history.undo()
        assertThat(data1).isNotNull()
        assertThat(data1!!.recordType).isEqualTo(KeyedAnnotationRecord.Remove)
        assertThat(data1.keyedAnnotation).isEqualTo(record2.keyedAnnotation)
        assertThat(history.canRedo.value).isTrue()
        assertThat(history.canUndo.value).isTrue()

        val data2 = history.undo()
        assertThat(data2).isNotNull()
        assertThat(data2!!.recordType).isEqualTo(KeyedAnnotationRecord.Remove)
        assertThat(data2.keyedAnnotation).isEqualTo(record1.keyedAnnotation)
        assertThat(history.canRedo.value).isTrue()
        assertThat(history.canUndo.value).isFalse()
    }

    @Test
    fun addEntry_whenMaxSizeReached_removesOldestEdit() {
        for (i in 1..11) {
            val record = createDummyKeyedAnnotationRecord(i)
            history.addEntry(record)
        }

        val undoEdit = history.undo()
        assertThat(undoEdit).isNotNull()
        val key = undoEdit!!.keyedAnnotation.key
        val pageNum = AnnotationHandleIdGenerator.decomposeAnnotationId(key).first
        assertThat(pageNum).isEqualTo(11)

        var lastUndo: KeyedAnnotationRecord? = null
        while (history.canUndo.value) {
            lastUndo = history.undo()
        }

        assertThat(lastUndo).isNotNull()
        val lastKey = lastUndo!!.keyedAnnotation.key
        val lastPageNum = AnnotationHandleIdGenerator.decomposeAnnotationId(lastKey).first
        assertThat(lastPageNum).isEqualTo(2)
    }

    @Test
    fun undoAndRedo_addOperation_movesEditToRedoStack() {
        val record = createDummyKeyedAnnotationRecord(0)
        history.addEntry(record)
        history.undo()

        assertThat(history.canRedo.value).isTrue()
        assertThat(history.redo()).isEqualTo(record)
        assertThat(history.canRedo.value).isFalse()
    }

    @Test
    fun undoAndRedo_removeOperation_movesEditToRedoStack() {
        val record = createDummyKeyedAnnotationRecord(0, recordType = KeyedAnnotationRecord.Remove)
        history.addEntry(record)
        val record1 = history.undo()

        assertThat(record1).isNotNull()
        assertThat(record1!!.recordType).isEqualTo(KeyedAnnotationRecord.Add)

        assertThat(history.canRedo.value).isTrue()
        assertThat(history.redo()).isEqualTo(record)
        assertThat(history.canRedo.value).isFalse()
    }

    @Test
    fun redo_movesEditToUndoStack() {
        val record = createDummyKeyedAnnotationRecord(0)
        history.addEntry(record)
        history.undo()
        history.redo()

        assertThat(history.canUndo.value).isTrue()
        assertThat(history.canRedo.value).isFalse()

        val op1 = history.undo()
        assertThat(op1).isNotNull()
        assertThat(op1!!.recordType).isEqualTo(KeyedAnnotationRecord.Remove)
    }

    @Test
    fun clear_emptiesBothStacks() {
        val record = createDummyKeyedAnnotationRecord(0)
        history.addEntry(record)
        history.undo()
        history.clear()

        assertThat(history.canUndo.value).isFalse()
        assertThat(history.canRedo.value).isFalse()
    }

    @Test
    fun threadSafety_isMaintained() {
        val threads = mutableListOf<Thread>()
        val records = List(100) { createDummyKeyedAnnotationRecord(0) }

        repeat(10) {
            threads += thread {
                for (record in records) {
                    history.addEntry(record)
                }
            }
        }
        threads.forEach { it.join() }

        var undoCount = 0
        while (history.canUndo.value) {
            history.undo()
            undoCount++
        }
        assertThat(undoCount).isEqualTo(history.maxSize)
    }

    private fun createDummyKeyedAnnotationRecord(
        pageNum: Int,
        recordType: KeyedAnnotationRecord.RecordType = KeyedAnnotationRecord.Add,
    ): KeyedAnnotationRecord {
        val value = UUID.randomUUID().toString()
        val keyedPdfAnnotation = createDummyKeyedPdfAnnotation(pageNum, value)
        return KeyedAnnotationRecord(recordType, keyedPdfAnnotation)
    }
}
