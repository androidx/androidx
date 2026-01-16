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

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.history.AnnotationRecordsHistoryManager
import androidx.pdf.annotation.manager.PdfAnnotationsManager
import androidx.pdf.annotation.models.KeyedAnnotationRecord
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * Represents a transactional editor that sits on top of the
 * [androidx.pdf.annotation.manager.PdfAnnotationsManager]. While the Manager handles the raw CRUD
 * operations and state reconciliation, the [PdfAnnotationsEditor] ensures that every modification
 * is automatically recorded in the [AnnotationRecordsHistoryManager] to maintain a consistent Undo
 * stack.
 *
 * @property historyManager Manages the stack of operations for Undo/Redo.
 * @property annotationsManager The single source of truth for annotation data (Persisted + Drafts).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PdfAnnotationsEditor(
    private val historyManager: AnnotationRecordsHistoryManager,
    private val annotationsManager: PdfAnnotationsManager,
) {

    /**
     * Adds a new draft annotation to the document and pushes the action to the Undo stack.
     *
     * @param annotation The [androidx.pdf.annotation.models.PdfAnnotation] content to add.
     * @return The unique Handle ID assigned to the new annotation.
     */
    public fun addDraftAnnotation(annotation: PdfAnnotation): String {
        val annotationId = annotationsManager.addAnnotation(annotation)

        // Record the operation with the generated ID so it can be removed later if Undone.
        val keyedAnnotation = KeyedPdfAnnotation(annotationId, annotation)
        historyManager.recordAdd(keyedAnnotation)

        return annotationId
    }

    /**
     * Removes an annotation from the document and pushes the action to the Undo stack.
     *
     * @param annotationId The unique Handle ID of the annotation to remove.
     * @return The [PdfAnnotation] data that was removed, or null if the ID was not found.
     */
    public suspend fun removeAnnotation(annotationId: String): PdfAnnotation? {
        val removedAnnotation = performRemove(annotationId)

        if (removedAnnotation != null) {
            // Record the content of the removed item so it can be restored if Undone.
            val keyedAnnotation = KeyedPdfAnnotation(annotationId, removedAnnotation)
            historyManager.recordRemove(keyedAnnotation)
        }
        return removedAnnotation
    }

    /**
     * Updates an existing annotation and pushes the action to the Undo stack.
     *
     * @param annotationId The unique Handle ID of the annotation to update.
     * @param newAnnotation The new content to apply.
     */
    public suspend fun updateAnnotation(annotationId: String, newAnnotation: PdfAnnotation) {
        val oldAnnotation = performUpdate(annotationId, newAnnotation)

        val keyedAnnotation = KeyedPdfAnnotation(annotationId, oldAnnotation)
        historyManager.recordUpdate(keyedAnnotation)
    }

    /**
     * Reverts the last recorded operation. This applies the inverse action to the
     * [PdfAnnotationsManager] (e.g., removing an added item).
     */
    public suspend fun undo(): Unit = replayOperation(record = historyManager.undo())

    /** Re-applies the last undone operation. */
    public suspend fun redo(): Unit = replayOperation(record = historyManager.redo())

    /** Clears all the unsaved changes and the entire history */
    public fun clear(): Unit {
        annotationsManager.discardChanges()
        historyManager.clear()
    }

    /**
     * Applies the logic of [androidx.pdf.annotation.models.KeyedAnnotationRecord] to the manager
     * without recording history.
     *
     * This method acts as a router, translating History Records into concrete Manager actions.
     */
    private suspend fun replayOperation(record: KeyedAnnotationRecord?) {
        if (record == null) return

        val key = record.keyedAnnotation.key
        val content = record.keyedAnnotation.annotation

        when (record.recordType) {
            KeyedAnnotationRecord.Add -> annotationsManager.addAnnotation(record.keyedAnnotation)
            KeyedAnnotationRecord.Remove -> performRemove(key)
            KeyedAnnotationRecord.Update -> performUpdate(key, content)
        }
    }

    private suspend fun performRemove(annotationId: String): PdfAnnotation? {
        return annotationsManager.removeAnnotation(annotationId)
    }

    private suspend fun performUpdate(
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation {
        return annotationsManager.updateAnnotation(annotationId, newAnnotation)
    }
}
