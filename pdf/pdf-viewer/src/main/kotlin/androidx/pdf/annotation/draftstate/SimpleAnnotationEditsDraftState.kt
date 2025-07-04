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
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.SavedEdit
import java.util.UUID

/**
 * A simple implementation of [AnnotationEditsDraftState] that stores annotation edits in memory
 * using a [SparseArray]. This class provides methods to add, remove, update, and retrieve edits.
 *
 * The [ParcelFileDescriptor] provided in the constructor is intended to be used by the user to save
 * the draft state to a file when necessary.
 *
 * @param pfd The [ParcelFileDescriptor] for saving the draft state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SimpleAnnotationEditsDraftState(pfd: ParcelFileDescriptor) :
    AnnotationEditsDraftState(pfd) {

    private val editState: SparseArray<MutableMap<EditId, SavedEdit>> = SparseArray()

    /**
     * Retrieves a list of saved annotations for a given page number.
     *
     * @param pageNum The page number to retrieve edits for.
     * @return A list of [SavedEdit] objects for the specified page, or an empty list if no edits
     *   exist.
     */
    override fun getEdits(pageNum: Int): List<SavedEdit> {
        val pageEdits = editState.get(pageNum) ?: return emptyList()
        return pageEdits.values.toList()
    }

    /**
     * Adds a new edit for the given annotation.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The [EditId] of the newly added edit.
     */
    override fun addEdit(annotation: PdfAnnotation): EditId {
        val pageNum = annotation.pageNum

        val pageEdits = editState.get(pageNum) ?: mutableMapOf()

        // creates a new editId for the given page number
        val editId = getNewEditId(pageNum)
        pageEdits[editId] = SavedEdit(editId, annotation)
        editState.put(pageNum, pageEdits)

        return editId
    }

    /**
     * Removes an edit with the specified [EditId].
     *
     * @param editId The ID of the edit to remove.
     * @return The removed [PdfAnnotation].
     * @throws NoSuchElementException if the edit with the given ID is not found.
     */
    override fun removeEdit(editId: EditId): PdfAnnotation {

        val pageEdits = getPageEditsForId(editId)

        // Non-null assertion is safe as getPageEditsForId ensures editId exists in pageEdits,
        // throwing NoSuchElementException otherwise.
        return pageEdits.remove(editId)!!.annotation
    }

    /**
     * Updates an existing edit with the specified [EditId] and new [PdfAnnotation].
     *
     * @param editId The ID of the edit to update.
     * @param annotation The new [PdfAnnotation] data.
     * @return The updated [PdfAnnotation].
     * @throws NoSuchElementException if the edit with the given ID is not found.
     */
    override fun updateEdit(editId: EditId, annotation: PdfAnnotation): PdfAnnotation {
        val pageEdits = getPageEditsForId(editId)

        pageEdits[editId] = SavedEdit(editId, annotation)
        return annotation
    }

    /**
     * Retrieves the mutable map of edits to savedAnnotation for a given edit ID.
     *
     * @param editId The ID of the edit to retrieve.
     * @return The mutable map of edits to savedAnnotation for the given edit ID.
     * @throws NoSuchElementException if the edit with the given ID is not found.
     */
    @VisibleForTesting
    public fun getPageEditsForId(editId: EditId): MutableMap<EditId, SavedEdit> {
        val errorMessage = "Edit with ID $editId not found."

        val pageEdits = editState.get(editId.pageNum) ?: throw NoSuchElementException(errorMessage)
        if (!pageEdits.containsKey(editId)) {
            throw NoSuchElementException(errorMessage)
        }
        // Return the map of EditId to SavedEdit for the given page number
        return pageEdits
    }

    /**
     * Generates a new unique [EditId] for a given page number.
     *
     * @param pageNum The page number for which to generate the new ID.
     * @return A new, unique [EditId].
     */
    private fun getNewEditId(pageNum: Int): EditId {
        return EditId(pageNum, UUID.randomUUID().toString())
    }
}
