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

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument
import androidx.pdf.PdfWriteHandle
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditsResult
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEdit
import androidx.pdf.annotation.models.PdfEditEntry
import androidx.pdf.annotation.models.PdfEdits
import androidx.pdf.models.FormEditInfo

/** Represents a PDF document that allows for editing. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class EditablePdfDocument : PdfDocument {

    /**
     * Retrieves a list of all edits for the specified page.
     *
     * @param pageNum The page number (0-indexed) from which to retrieve edits.
     * @return A list of [PdfEditEntry] objects representing the staged edits on the page. Returns
     *   an empty list if there are no edits on the page.
     * @throws IllegalArgumentException if the page number is invalid.
     */
    public abstract suspend fun <T : PdfEditEntry<out PdfEdit>> getEditsForPage(
        pageNum: Int
    ): List<T>

    /**
     * Applies the changes specified by [record] to the form.
     *
     * Any areas which are invalidated due to this operation are notified via
     * [OnPdfContentInvalidatedListener] callback.
     *
     * It is recommended to maintain a list of [FormEditInfo] applied to the document so they can be
     * saved and restored across destructive events like low memory kills or configuration changes.
     *
     * @property record The [FormEditInfo] to apply to the form.
     * @throws IllegalArgumentException if the provided [record] cannot be applied to the widget
     *   indicated by the index, or if the index does not correspond to a widget on the page.
     */
    public abstract suspend fun applyEdit(record: FormEditInfo)

    /**
     * Applies a list of annotation edits to the document.
     *
     * @param annotations A list of [PdfAnnotationData] representing the edits to be applied.
     * @return An [AnnotationResult] indicating the success or failure of the operation.
     */
    public abstract suspend fun applyEdits(annotations: List<PdfAnnotationData>): AnnotationResult

    /**
     * Applies a list of annotation edits from a source [ParcelFileDescriptor] to the document.
     *
     * @param sourcePfd The [ParcelFileDescriptor] containing the annotation edits.
     * @return An [AnnotationResult] indicating the success or failure of the operation.
     */
    public abstract suspend fun applyEdits(sourcePfd: ParcelFileDescriptor): AnnotationResult

    /**
     * Creates a new [PdfEdit] onto to the document with the [EditId] provided.
     *
     * @param entry The [PdfEditEntry] to be added.
     */
    public abstract fun <T : PdfEdit> addPdfEditEntry(entry: PdfEditEntry<T>)

    /**
     * Creates a new PdfEdit onto the PdfDocument.
     *
     * @param edit The [PdfEdit] to be added.
     * @return An [EditId] that uniquely identifies this edit operation.
     */
    public abstract fun addEdit(edit: PdfEdit): EditId

    /**
     * Removes an existing PdfEdit from the PdfDocument.
     *
     * @param editId The [EditId] of the PdfEdit to be removed.
     * @return The [PdfEdit] that was removed.
     */
    public abstract fun removeEdit(editId: EditId): PdfEdit

    /**
     * Updates an existing PdfEdit in the PdfDocument.
     *
     * @param editId The [EditId] of the edit to be updated.
     * @param edit The [PdfEdit] to be updated.
     * @return The old [PdfEdit] that was updated.
     */
    public abstract fun updateEdit(editId: EditId, edit: PdfEdit): PdfEdit

    /** Commits all PdfEdits to the PDF document. */
    public abstract suspend fun commitEdits(): EditsResult

    /**
     * Returns an immutable snapshot of all [PdfEdit]s, organized by page number.
     *
     * @return [PdfEdits] representing all [PdfEdit]s in the document.
     */
    public abstract fun getAllEdits(): PdfEdits

    /** Discards all uncommitted edits. */
    public abstract fun clearUncommittedEdits()

    /**
     * Creates a [PdfWriteHandle] which can be used to save the document to a
     * [ParcelFileDescriptor].
     */
    public abstract fun createWriteHandle(): PdfWriteHandle
}
