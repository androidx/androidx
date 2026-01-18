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

package androidx.pdf.annotation.draftstate

import androidx.annotation.RestrictTo
import androidx.pdf.EditsDraft
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation

/** Responsible for managing the draft edits of annotations. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface AnnotationEditsDraftState {
    /**
     * Retrieves the draft annotation by id for a specific page.
     *
     * @param pageNum The page number (0-indexed).
     * @param handleId The id associated with the annotation on the page.
     * @return [PdfAnnotation] object or null if the object is not found.
     */
    public fun getDraftAnnotation(pageNum: Int, handleId: String): PdfAnnotation?

    /**
     * Retrieves the draft annotations for a specific page.
     *
     * @param pageNum The page number (0-indexed).
     * @return A list of [KeyedPdfAnnotation] objects.
     */
    public fun getDraftAnnotations(pageNum: Int): List<KeyedPdfAnnotation>

    /**
     * Retrieves a snapshot of all current modifications accumulated in this draft state.
     *
     * @return An [EditsDraft] object containing the ordered collection of operations (Inserts,
     *   Updates, Removes).
     */
    public fun getModificationsSnapshot(): EditsDraft

    /**
     * Adds a keyed annotation to the draft.
     *
     * @param keyedAnnotation The [KeyedPdfAnnotation] to add.
     * @return The id assigned to the newly added annotation.
     */
    public fun addDraftAnnotation(keyedAnnotation: KeyedPdfAnnotation): String

    /**
     * Adds a new annotation edit to the draft state.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The id assigned to the newly added annotation.
     */
    public fun addDraftAnnotation(annotation: PdfAnnotation): String

    /**
     * Removes an existing annotation from the draft state.
     *
     * @param pageNum The specified page number.
     * @param annotationId The id of the annotation to remove.
     * @return The [PdfAnnotation] that was removed.
     */
    public fun removeAnnotation(pageNum: Int, annotationId: String): PdfAnnotation

    /**
     * Updates an existing annotation edit in the draft state.
     *
     * @param pageNum The specified page number.
     * @param annotationId The id of the annotation to update.
     * @param newAnnotation The new [PdfAnnotation] to replace the existing annotation.
     * @return The previous [PdfAnnotation].
     */
    public fun updateDraftAnnotation(
        pageNum: Int,
        annotationId: String,
        newAnnotation: PdfAnnotation,
    ): PdfAnnotation

    /** Clears all annotation edits from the draft state. */
    public fun clear()

    public companion object {
        public fun create(): AnnotationEditsDraftState {
            return InMemoryAnnotationEditsDraftState()
        }
    }
}
