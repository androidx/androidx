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
import androidx.annotation.VisibleForTesting
import androidx.pdf.annotation.draftstate.AnnotationEditsDraftState
import androidx.pdf.annotation.draftstate.InMemoryAnnotationEditsDraftState
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationEditOperation
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditOperation
import androidx.pdf.annotation.models.EditsResult
import androidx.pdf.annotation.models.JetpackAospIdPair
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEdits
import androidx.pdf.annotation.processor.PdfAnnotationsProcessor
import java.util.Collections
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Manages annotations for a PDF document, storing them in memory. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InMemoryAnnotationsManager(
    private val fetcher: PageAnnotationFetcher,
    private val annotationsProcessor: PdfAnnotationsProcessor,
) : AnnotationsManager {
    @VisibleForTesting
    internal val annotationEditsDraftState: AnnotationEditsDraftState =
        InMemoryAnnotationEditsDraftState()

    // Tracks the existing annotations per page. If the request has been invoked and no annotations
    // have been found then the value will be empty.
    @VisibleForTesting
    internal val existingAnnotationsPerPage: MutableMap<Int, List<PdfAnnotation>> =
        Collections.synchronizedMap(HashMap())
    private val editIdToAospIdMap: MutableMap<EditId, EditId> =
        Collections.synchronizedMap(HashMap())

    internal val operationsToCommit = mutableListOf<AnnotationEditOperation>()

    private val pageFetchLocks: MutableMap<Int, Mutex> = Collections.synchronizedMap(HashMap())

    /**
     * Fetches annotations for the given page from the document and caches them.
     *
     * @param pageNum The page number (0-indexed) to fetch annotations for.
     */
    private suspend fun fetchAndCacheAnnotationsForPage(pageNum: Int): List<PdfAnnotation> {
        val existingPageAnnotations = fetcher.fetchAnnotations(pageNum)
        existingAnnotationsPerPage.put(pageNum, existingPageAnnotations)
        return existingPageAnnotations
    }

    /**
     * Retrieves all annotations for a given page number.
     *
     * This function first checks if the annotations for the specified page have already been
     * fetched from the document. If not, it fetches them, stores them locally, and adds them to the
     * [AnnotationEditsDraftState]. It then returns all annotations (both existing and newly added)
     * for that page from the draft state.
     *
     * @param pageNum The page number (0-indexed) to retrieve annotations for.
     * @return A list of [PdfAnnotationData] for the specified page.
     */
    override suspend fun getAnnotationsForPage(pageNum: Int): List<PdfAnnotationData> {
        if (existingAnnotationsPerPage[pageNum] != null) {
            return annotationEditsDraftState.getEdits(pageNum)
        }
        val lock = pageFetchLocks.computeIfAbsent(pageNum) { Mutex() }
        lock.withLock {
            // After acquiring the lock, another coroutine might have
            // already fetched the data while this one was waiting. This check
            // prevents a redundant fetch.
            if (existingAnnotationsPerPage[pageNum] == null) {
                val existingPageAnnotations = fetchAndCacheAnnotationsForPage(pageNum)
                // Add the annotations to the draft state.
                existingPageAnnotations.forEach { annotationEditsDraftState.addEdit(it) }
            }
        }

        return annotationEditsDraftState.getEdits(pageNum)
    }

    override fun addAnnotationById(id: EditId, annotation: PdfAnnotation): Unit =
        annotationEditsDraftState.addEditById(id, annotation)

    /**
     * Adds a new annotation to the draft state.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The [EditId] assigned to the newly added annotation in the draft state.
     */
    override fun addAnnotation(annotation: PdfAnnotation): EditId {
        val editId = annotationEditsDraftState.addEdit(annotation)
        val editOperation =
            AnnotationEditOperation(EditOperation.Add, PdfAnnotationData(editId, annotation))
        operationsToCommit.add(editOperation)
        return editId
    }

    /**
     * Removes an annotation from the draft state.
     *
     * @param editId The [EditId] of the annotation to remove.
     * @return The removed [PdfAnnotation].
     * @throws NoSuchElementException if the annotation with the given [editId] is not found.
     */
    public fun removeAnnotation(editId: EditId): PdfAnnotation {
        val annotation = annotationEditsDraftState.removeEdit(editId)
        val editOperation =
            AnnotationEditOperation(EditOperation.Remove, PdfAnnotationData(editId, annotation))
        operationsToCommit.add(editOperation)
        return annotation
    }

    /**
     * Updates an existing annotation in the draft state.
     *
     * @param editId The [EditId] of the annotation to update.
     * @param annotation The new [PdfAnnotation] data.
     * @return The updated [PdfAnnotation].
     * @throws NoSuchElementException if the annotation with the given [editId] is not found.
     */
    public fun updateAnnotation(editId: EditId, annotation: PdfAnnotation): PdfAnnotation {
        val annotation = annotationEditsDraftState.updateEdit(editId, annotation)
        val editOperation =
            AnnotationEditOperation(EditOperation.Update, PdfAnnotationData(editId, annotation))
        operationsToCommit.add(editOperation)
        return annotation
    }

    /**
     * Returns an immutable snapshot of the current annotation draft state including unedited
     * existing annotations as well.
     *
     * @return An [PdfEdits] representing the current draft.
     */
    override fun getSnapshot(): PdfEdits = annotationEditsDraftState.toPdfEdits()

    /** Clears uncommitted edits, restoring the draft state to the last saved state. */
    override fun clearUncommittedEdits() {
        annotationEditsDraftState.clear()

        existingAnnotationsPerPage.forEach { (_, annotations) ->
            annotations.forEach { annotationEditsDraftState.addEdit(it) }
        }
    }

    /** Commits all pending edits to the document. */
    internal suspend fun commitEdits(): EditsResult {

        val operationsMap =
            operationsToCommit.groupBy { annotationEditOperation -> annotationEditOperation.op }
        val pagesToRefetch = HashSet<Int>()

        operationsMap[EditOperation.Add]?.let {
            val addEditResult = it.processAddOperations()
            addEditResult.success.forEach { (jetpackId, _) ->
                pagesToRefetch.add(jetpackId.pageNum)
            }
        }
        operationsMap[EditOperation.Update]?.let {
            val modifyEditResult = it.processUpdateOperations()
            modifyEditResult.success.forEach { editId -> pagesToRefetch.add(editId.pageNum) }
        }
        operationsMap[EditOperation.Remove]?.let {
            val modifyEditResult = it.processRemoveOperations()
            modifyEditResult.success.forEach { editId ->
                pagesToRefetch.add(editId.pageNum)
                editIdToAospIdMap.remove(editId)
            }
        }

        // Clear the operations list after processing
        operationsToCommit.clear()
        // Invalidates and re-fetches existing annotations for the specified pages.
        clearExistingAnnotationForPages(pagesToRefetch)

        // TODO: b/450764663 - Handle Reconciliation of Failed Annotations
        return EditsResult(listOf(), listOf())
    }

    /** Processes the add operations for commit. */
    private fun List<AnnotationEditOperation>.processAddOperations(): AddEditResult {
        val addPdfAnnotationsData = this.map { it.edit }
        val addEditResult = annotationsProcessor.processAddEdits(addPdfAnnotationsData)
        associateAospIds(addEditResult.success)
        return addEditResult
    }

    /** Processes the remove operations for commit. */
    private fun List<AnnotationEditOperation>.processRemoveOperations(): ModifyEditResult {
        val aospIds = this.mapNotNull { editIdToAospIdMap[it.edit.editId] }
        return annotationsProcessor.processRemoveEdits(aospIds)
    }

    /** Processes the update operations for commit. */
    private fun List<AnnotationEditOperation>.processUpdateOperations(): ModifyEditResult {
        val pdfAnnotationsData =
            this.mapNotNull {
                val editId = it.edit.editId
                editIdToAospIdMap[editId]?.let { aospId ->
                    PdfAnnotationData(aospId, it.edit.annotation)
                }
            }

        return annotationsProcessor.processUpdateEdits(pdfAnnotationsData)
    }

    /**
     * Associates Aosp IDs with jetpack editId in the internal mapping.
     *
     * @param jetpackAospIdPairs The list of [JetpackAospIdPair] mapping Jetpack IDs to AOSP IDs.
     */
    private fun associateAospIds(jetpackAospIdPairs: List<JetpackAospIdPair>) {
        jetpackAospIdPairs.forEach { (editId, aospId) ->
            // Update the AospId in the Map
            editIdToAospIdMap[editId] = aospId
        }
    }

    /**
     * Clears the existing cached annotations for the specified pages.
     *
     * @param pages A list of page numbers (0-indexed) to refresh.
     */
    private fun clearExistingAnnotationForPages(pages: HashSet<Int>) {
        pages.forEach { pageNum -> existingAnnotationsPerPage.remove(pageNum) }
    }
}
