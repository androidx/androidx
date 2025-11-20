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

package androidx.pdf.ink.edits

import androidx.pdf.annotation.EditablePdfDocument
import androidx.pdf.annotation.models.AnnotationEditOperation
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditOperation
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEdit
import androidx.pdf.ink.AnnotationsDisplayStateFlowProvider
import androidx.pdf.ink.history.AnnotationEditsHistoryManager
import kotlinx.coroutines.flow.update

/**
 * Handles annotation edit operations by applying them to the [EditablePdfDocument] and managing the
 * edit history.
 *
 * @param document The [EditablePdfDocument] to which annotation edits are applied.
 * @param editsHistoryManager The [AnnotationEditsHistoryManager] that records and manages the
 *   history of annotation edits.
 * @param flowProvider The [AnnotationsDisplayStateFlowProvider] used to update the display state of
 *   annotations.
 */
internal class AnnotationEditOperationsHandler(
    private val document: EditablePdfDocument,
    private val editsHistoryManager: AnnotationEditsHistoryManager,
    private val flowProvider: AnnotationsDisplayStateFlowProvider,
) {
    /**
     * Adds a draft annotation to the document and records the operation in the history.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @param isOperationReplayed A boolean indicating whether this operation is a replayed action
     *   from the history. If `true`, the operation is not recorded in the history again.
     */
    fun addDraftAnnotation(annotation: PdfAnnotation, isOperationReplayed: Boolean = false) {
        val editId = document.addEdit(annotation)
        if (!isOperationReplayed) editsHistoryManager.recordAdd(editId, annotation)
        flowProvider.get().update { currentState ->
            currentState.copy(edits = document.getAllEdits())
        }
    }

    /**
     * Adds a draft annotation with a specific ID to the document and records the operation in the
     * history.
     *
     * @param id The [EditId] of the annotation to add.
     * @param annotation The [PdfAnnotation] to add.
     * @param isOperationReplayed A boolean indicating whether this operation is a replayed action
     *   from the history. If `true`, the operation is not recorded in the history again.
     */
    fun addDraftAnnotationById(
        id: EditId,
        annotation: PdfAnnotation,
        isOperationReplayed: Boolean = false,
    ) {
        val annotationData = PdfAnnotationData(id, annotation)
        document.addPdfEditEntry(annotationData)
        if (!isOperationReplayed) editsHistoryManager.recordAdd(id, annotation)
        flowProvider.get().update { currentState ->
            currentState.copy(edits = document.getAllEdits())
        }
    }

    /**
     * Removes an annotation by its ID from the document and records the operation in the history.
     *
     * @param id The [EditId] of the annotation to remove.
     * @param isOperationReplayed A boolean indicating whether this operation is a replayed action
     *   from the history. If `true`, the operation is not recorded in the history again.
     */
    fun removeAnnotationById(id: EditId, isOperationReplayed: Boolean = false) {
        val removedEdit = document.removeEdit(id)
        if (removedEdit is PdfAnnotation) {
            if (!isOperationReplayed) editsHistoryManager.recordRemove(id, removedEdit)
            flowProvider.get().update { currentState ->
                currentState.copy(edits = document.getAllEdits())
            }
        }
    }

    /**
     * Updates an annotation by its ID in the document and records the operation in the history.
     *
     * @param id The [EditId] of the annotation to update.
     * @param edit The [PdfEdit] containing the updated annotation data.
     * @param isOperationReplayed A boolean indicating whether this operation is a replayed action
     *   from the history. If `true`, the operation is not recorded in the history again.
     */
    fun updateAnnotationById(id: EditId, edit: PdfEdit, isOperationReplayed: Boolean = false) {
        val updatedEdit = document.updateEdit(id, edit)
        if (updatedEdit is PdfAnnotation) {
            if (!isOperationReplayed) editsHistoryManager.recordUpdate(id, updatedEdit)
            flowProvider.get().update { currentState ->
                currentState.copy(edits = document.getAllEdits())
            }
        }
    }

    /** Undoes the last annotation edit operation. */
    fun undo() = replayOperation(operation = editsHistoryManager.undo())

    /** Redoes the last undone annotation edit operation. */
    fun redo() = replayOperation(operation = editsHistoryManager.redo())

    /** Clears the annotation edit history. */
    fun clear() = editsHistoryManager.clear()

    private fun replayOperation(operation: AnnotationEditOperation?) {
        if (operation != null) {
            when (operation.op) {
                EditOperation.Add ->
                    addDraftAnnotationById(
                        operation.edit.id,
                        operation.edit.annotation,
                        isOperationReplayed = true,
                    )
                EditOperation.Remove ->
                    removeAnnotationById(operation.edit.id, isOperationReplayed = true)
                EditOperation.Update ->
                    updateAnnotationById(
                        operation.edit.id,
                        operation.edit.annotation,
                        isOperationReplayed = true,
                    )
                EditOperation.None -> {}
            }
        }
    }
}
