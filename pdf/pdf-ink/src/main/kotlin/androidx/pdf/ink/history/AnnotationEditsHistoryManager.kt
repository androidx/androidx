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
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData

/**
 * Manages the history of edits for PDF annotations.
 *
 * @param annotationEditsHistory The underlying history log where all edits are stored.
 */
internal class AnnotationEditsHistoryManager() {
    private val annotationEditsHistory: AnnotationEditsHistory =
        AnnotationEditsHistoryImpl(maxSize = MAX_STACK_SIZE)

    /**
     * Records the addition of a new annotation to the history.
     *
     * @param id The unique identifier for this edit operation.
     * @param annotation The [PdfAnnotation] that was added.
     */
    fun recordAdd(id: EditId, annotation: PdfAnnotation) =
        recordOperation(EditOperation.Add, id, annotation)

    /**
     * Records the removal of an existing annotation from the history.
     *
     * @param id The unique identifier for this edit operation.
     * @param annotation The [PdfAnnotation] that was removed.
     */
    fun recordRemove(id: EditId, annotation: PdfAnnotation) =
        recordOperation(EditOperation.Remove, id, annotation)

    /**
     * Records an update to an existing annotation in the history.
     *
     * @param id The unique identifier for this edit operation.
     * @param annotation The [PdfAnnotation] that was updated, containing the new state.
     */
    fun recordUpdate(id: EditId, annotation: PdfAnnotation) =
        recordOperation(EditOperation.Update, id, annotation)

    /**
     * Executes an undo operation.
     *
     * @return The [AnnotationEditOperation] that was undone, or null if there is nothing to undo.
     */
    fun undo(): AnnotationEditOperation? = annotationEditsHistory.undo()

    /**
     * Executes a redo operation.
     *
     * @return The [AnnotationEditOperation] that was redone, or null if there is nothing to redo.
     */
    fun redo(): AnnotationEditOperation? = annotationEditsHistory.redo()

    /**
     * Checks if there are any edits available to be undone.
     *
     * @return true if an undo operation can be performed, false otherwise.
     */
    fun canUndo(): Boolean = annotationEditsHistory.canUndo()

    /**
     * Checks if there are any edits available to be redone.
     *
     * @return true if a redo operation can be performed, false otherwise.
     */
    fun canRedo(): Boolean = annotationEditsHistory.canRedo()

    /** Clears all edits from the history. */
    fun clear() = annotationEditsHistory.clear()

    private fun recordOperation(
        op: EditOperation.Operation,
        id: EditId,
        annotation: PdfAnnotation,
    ) {
        val annotationData = PdfAnnotationData(id, annotation)
        val editOperation = AnnotationEditOperation(op = op, edit = annotationData)
        annotationEditsHistory.addEntry(editOperation)
    }

    companion object {
        private const val MAX_STACK_SIZE = 20
    }
}
