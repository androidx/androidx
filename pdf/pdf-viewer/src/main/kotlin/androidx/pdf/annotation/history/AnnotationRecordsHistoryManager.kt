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

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.KeyedAnnotationRecord
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the history of edits for PDF annotations.
 *
 * @param annotationRecordsHistory The underlying history log where all edits are stored.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnnotationRecordsHistoryManager() {
    private val annotationRecordsHistory: AnnotationRecordsHistory =
        AnnotationRecordsHistoryImpl(maxSize = MAX_STACK_SIZE)

    /**
     * Checks if there are any edits available to be undone.
     *
     * @return true if an undo operation can be performed, false otherwise.
     */
    public val canUndo: StateFlow<Boolean> = annotationRecordsHistory.canUndo

    /**
     * Checks if there are any edits available to be redone.
     *
     * @return true if a redo operation can be performed, false otherwise.
     */
    public val canRedo: StateFlow<Boolean> = annotationRecordsHistory.canRedo

    /**
     * Records the addition of a new annotation to the history.
     *
     * @param keyedAnnotation The [KeyedPdfAnnotation] that was added.
     */
    public fun recordAdd(keyedAnnotation: KeyedPdfAnnotation): Unit =
        recordOperation(recordType = KeyedAnnotationRecord.Add, keyedAnnotation)

    /**
     * Records the removal of an existing annotation from the history.
     *
     * @param keyedAnnotation The [KeyedPdfAnnotation] that was removed.
     */
    public fun recordRemove(keyedAnnotation: KeyedPdfAnnotation): Unit =
        recordOperation(recordType = KeyedAnnotationRecord.Remove, keyedAnnotation)

    /**
     * Records an update to an existing annotation in the history.
     *
     * @param keyedAnnotation The [KeyedPdfAnnotation] that was updated.
     */
    public fun recordUpdate(keyedAnnotation: KeyedPdfAnnotation): Unit =
        recordOperation(recordType = KeyedAnnotationRecord.Update, keyedAnnotation)

    /**
     * Executes an undo operation.
     *
     * @return The [KeyedAnnotationRecord] that was undone, or null if there is nothing to undo.
     */
    public fun undo(): KeyedAnnotationRecord? = annotationRecordsHistory.undo()

    /**
     * Executes a redo operation.
     *
     * @return The [KeyedAnnotationRecord] that was redone, or null if there is nothing to redo.
     */
    public fun redo(): KeyedAnnotationRecord? = annotationRecordsHistory.redo()

    /** Clears all edits from the history. */
    public fun clear(): Unit = annotationRecordsHistory.clear()

    private fun recordOperation(
        recordType: KeyedAnnotationRecord.RecordType,
        keyedAnnotation: KeyedPdfAnnotation,
    ) {
        val record = KeyedAnnotationRecord(recordType, keyedAnnotation)
        annotationRecordsHistory.addEntry(record)
    }

    public companion object {
        public const val MAX_STACK_SIZE: Int = 50
    }
}
