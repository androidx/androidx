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

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.SavedEdit

/**
 * Abstract class representing the draft state of annotation edits. This class manages the
 * persistence of annotation edits associated with a PDF file, identified by a
 * [ParcelFileDescriptor].
 *
 * Implementations of this class are responsible for handling the storage and retrieval of
 * annotation edits.
 *
 * @property pfd The [ParcelFileDescriptor] of the draft state file where annotation edits are being
 *   saved and managed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class AnnotationEditsDraftState(public open val pfd: ParcelFileDescriptor) {

    /** Returns the [ParcelFileDescriptor] associated with this draft state. */
    public val fileDescriptor: ParcelFileDescriptor = pfd

    /**
     * Retrieves all annotation edits for a specific page.
     *
     * @param pageNum The page number (0-indexed) for which to retrieve edits.
     * @return A list of [SavedEdit] objects representing the id and the persisted annotation.
     */
    public abstract fun getEdits(pageNum: Int): List<SavedEdit>

    /**
     * Adds a new annotation edit to the draft state.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The [EditId] assigned to the newly added annotation.
     */
    public abstract fun addEdit(annotation: PdfAnnotation): EditId

    /**
     * Removes an existing annotation edit from the draft state.
     *
     * @param editId The [EditId] of the annotation to remove.
     * @return The [PdfAnnotation] that was removed.
     */
    public abstract fun removeEdit(editId: EditId): PdfAnnotation

    /**
     * Updates an existing annotation edit in the draft state.
     *
     * @param editId The [EditId] of the annotation to update.
     * @param annotation The new [PdfAnnotation] to replace the existing annotation.
     * @return The updated [PdfAnnotation].
     */
    public abstract fun updateEdit(editId: EditId, annotation: PdfAnnotation): PdfAnnotation
}
