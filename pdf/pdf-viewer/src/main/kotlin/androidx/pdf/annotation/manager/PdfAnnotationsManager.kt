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

import androidx.annotation.RestrictTo
import androidx.pdf.EditsDraft
import androidx.pdf.PdfDocument
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.draftstate.AnnotationEditsDraftState
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.operations.AnnotationOperationsTracker
import androidx.pdf.annotation.registry.AnnotationHandleRegistry
import androidx.pdf.annotation.repository.AnnotationsRepository

/** Manages annotations for a PDF document. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PdfAnnotationsManager {
    /**
     * Retrieves the draft and persisted annotations for a given page number.
     *
     * @param pageNum The page number (0-indexed) to retrieve annotations for.
     * @return A list of [KeyedPdfAnnotation] for the specified page.
     */
    public suspend fun getAnnotations(pageNum: Int): List<KeyedPdfAnnotation>

    /**
     * Retrieves all local modifications made to the document annotations.
     *
     * @return A [EditsDraft] representing the current state of all modified items.
     */
    public suspend fun getAnnotationModifications(): EditsDraft

    /**
     * Adds a new keyed annotation.
     *
     * @param keyedAnnotation The [KeyedPdfAnnotation] to add.
     * @return The annotation id assigned to the newly added annotation.
     */
    public fun addAnnotation(keyedAnnotation: KeyedPdfAnnotation): String

    /**
     * Adds a new annotation.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The annotation id assigned to the newly added annotation.
     */
    public fun addAnnotation(annotation: PdfAnnotation): String

    /**
     * Removes an annotation from the document.
     *
     * @param annotationId The unique identifier of the annotation to remove.
     * @return The [PdfAnnotation] that was removed, or `null` if no annotation with the specified
     *   ID was found.
     */
    public suspend fun removeAnnotation(annotationId: String): PdfAnnotation?

    /**
     * Updates an existing annotation with new data.
     *
     * @param annotationId The unique identifier of the annotation to update.
     * @param newAnnotation The new [PdfAnnotation] data to apply.
     * @return The previous [PdfAnnotation] data before the update.
     * @throws NoSuchElementException if no annotation with the specified ID is found.
     */
    public suspend fun updateAnnotation(
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation

    /** Discards all changes made to the annotations. */
    public fun discardChanges(): Unit

    public companion object {
        public fun create(document: PdfDocument): PdfAnnotationsManager {
            val handleRegistry = AnnotationHandleRegistry.create()
            return PdfDocumentAnnotationsManager(
                draftState = AnnotationEditsDraftState.create(),
                annotationsRepository = AnnotationsRepository.create(document),
                handleRegistry = handleRegistry,
                operationsTracker = AnnotationOperationsTracker.create(handleRegistry),
            )
        }
    }
}
