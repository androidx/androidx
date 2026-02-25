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

package androidx.pdf.annotation.manager

import androidx.pdf.EditsDraft
import androidx.pdf.annotation.AnnotationHandleIdGenerator.decomposeAnnotationId
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.draftstate.AnnotationEditsDraftState
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.operations.AnnotationOperationsTracker
import androidx.pdf.annotation.operations.KeyedAnnotationOperation
import androidx.pdf.annotation.registry.AnnotationHandleRegistry
import androidx.pdf.annotation.repository.AnnotationsRepository

/**
 * The internal orchestrator for managing PDF annotations, serving as the primary implementation of
 * [PdfAnnotationsManager].
 *
 * This class provides a unified view of annotations by combining persisted data from the underlying
 * PDF document with in-memory draft edits. It is responsible for reconciling these two sources,
 * tracking all user-initiated operations (add, remove, update), and ensuring data consistency.
 *
 * @param draftState Manages in-memory annotations that have not yet been saved.
 * @param annotationsRepository The single source of truth for annotations persisted in the PDF.
 * @param handleRegistry Maintains a mapping between persisted annotation IDs and stable,
 *   session-unique handle IDs.
 * @param operationsTracker Tracks pending operations (add, remove, update) on annotations, which
 *   are used to reconcile the final state.
 */
internal class PdfDocumentAnnotationsManager(
    private val draftState: AnnotationEditsDraftState,
    private val annotationsRepository: AnnotationsRepository,
    private val handleRegistry: AnnotationHandleRegistry,
    private val operationsTracker: AnnotationOperationsTracker,
) : PdfAnnotationsManager {
    override suspend fun getAnnotations(pageNum: Int): List<KeyedPdfAnnotation> {
        val draftAnnotations = draftState.getDraftAnnotations(pageNum)
        val persistedAnnotations = annotationsRepository.getAnnotationsForPage(pageNum)

        val reconciledAnnotations = reconcileAnnotations(persistedAnnotations)
        return reconciledAnnotations + draftAnnotations
    }

    override suspend fun getAnnotationModifications(): EditsDraft {
        val draftModificationsSnapshot = draftState.getModificationsSnapshot()
        val persistedModificationsSnapshot = operationsTracker.getModificationsSnapshot()
        return persistedModificationsSnapshot + draftModificationsSnapshot
    }

    override fun addAnnotation(annotation: PdfAnnotation): String {
        val draftId = draftState.addDraftAnnotation(annotation)
        operationsTracker.addEntry(
            operationType = KeyedAnnotationOperation.OperationType.ADD,
            key = draftId,
            annotation,
        )
        return draftId
    }

    override fun addAnnotation(keyedAnnotation: KeyedPdfAnnotation): String {
        var handleId = keyedAnnotation.key
        if (handleRegistry.getSourceId(handleId) == null) {
            handleId = draftState.addDraftAnnotation(keyedAnnotation)
        }
        operationsTracker.addEntry(
            operationType = KeyedAnnotationOperation.OperationType.ADD,
            key = handleId,
            keyedAnnotation.annotation,
        )
        return handleId
    }

    override suspend fun removeAnnotation(annotationId: String): PdfAnnotation? {
        val (pageNum, _) = decomposeAnnotationId(annotationId)
        val sourceId = handleRegistry.getSourceId(annotationId)

        val annotationToRemove: PdfAnnotation? =
            if (sourceId != null) {
                val updatedContent = operationsTracker.getUpdatedAnnotation(annotationId)
                updatedContent ?: annotationsRepository.getAnnotation(pageNum, sourceId)?.annotation
            } else {
                draftState.removeAnnotation(pageNum, annotationId)
            }

        if (annotationToRemove != null) {
            operationsTracker.addEntry(
                operationType = KeyedAnnotationOperation.OperationType.REMOVE,
                key = annotationId,
                annotation = annotationToRemove,
            )
        }

        return annotationToRemove
    }

    override suspend fun updateAnnotation(
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation {
        val (pageNum, _) = decomposeAnnotationId(annotationId)

        val previousAnnotation: PdfAnnotation = run {
            val pendingUpdate = operationsTracker.getUpdatedAnnotation(annotationId)
            if (pendingUpdate != null) return@run pendingUpdate

            val previousDraftAnnotation = draftState.getDraftAnnotation(pageNum, annotationId)
            if (previousDraftAnnotation != null) {
                return@run previousDraftAnnotation
            }

            val sourceId =
                handleRegistry.getSourceId(annotationId)
                    ?: throw NoSuchElementException(
                        "Cannot update: ID $annotationId not found on page $pageNum"
                    )

            annotationsRepository.getAnnotation(pageNum, sourceId)?.annotation
                ?: throw NoSuchElementException("Annotation $sourceId not found in repository")
        }

        val persistedAnnotationId = handleRegistry.getSourceId(annotationId)
        if (persistedAnnotationId == null) {
            draftState.updateDraftAnnotation(pageNum, annotationId, newAnnotation)
        }
        operationsTracker.addEntry(
            operationType = KeyedAnnotationOperation.OperationType.UPDATE,
            key = annotationId,
            annotation = newAnnotation,
        )
        return previousAnnotation
    }

    override fun discardChanges() {
        draftState.clear()
        handleRegistry.clear()
        operationsTracker.clear()
        annotationsRepository.clear()
    }

    /**
     * Reconciles the raw persisted annotations with the current pending operations (edits and
     * deletions).
     *
     * This function transforms the static repository data into the current view state by performing
     * three operations:
     * 1. **ID Translation:** Converts the repository's Source IDs into stable "Handle IDs" via the
     *    [handleRegistry], ensuring the caller interacts with a unified identifier system.
     * 2. **Filtering (Deletions):** Checks if an annotation has been marked as deleted in the
     *    [operationsTracker] and excludes it if so.
     * 3. **Overlaying (Updates):** Checks if an annotation has a pending update in the
     *    [operationsTracker] and substitutes the stale persisted content with the fresh updated
     *    content.
     *
     * @param keyedAnnotations The list of annotations fetched directly from the repository (keyed
     *   by Source ID).
     * @return A list of [KeyedPdfAnnotation]s keyed by Handle ID, reflecting the current user
     *   edits.
     */
    private fun reconcileAnnotations(
        keyedAnnotations: List<KeyedPdfAnnotation>
    ): List<KeyedPdfAnnotation> {
        return keyedAnnotations.mapNotNull { keyedAnnotation ->
            // Persisted annotations need to have a proxy id so that the caller can have a
            // unified id.
            val handleId =
                handleRegistry.getHandleId(keyedAnnotation.annotation.pageNum, keyedAnnotation.key)

            if (operationsTracker.isDeleted(handleId)) {
                return@mapNotNull null
            }
            val updatedContent = operationsTracker.getUpdatedAnnotation(handleId)
            if (updatedContent != null) {
                return@mapNotNull KeyedPdfAnnotation(handleId, updatedContent)
            }

            KeyedPdfAnnotation(key = handleId, annotation = keyedAnnotation.annotation)
        }
    }
}
