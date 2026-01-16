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

package androidx.pdf.annotation.processor

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.DraftEditOperation
import androidx.pdf.DraftEditResult
import androidx.pdf.InsertDraftEditOperation
import androidx.pdf.RemoveDraftEditOperation
import androidx.pdf.UpdateDraftEditOperation
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.annotation.converters.PdfAnnotationConvertersFactory
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * Processes draft annotation edits by applying them to the underlying PDF document using
 * [PdfDocumentRenderer].
 *
 * This class acts as a bridge between the high-level draft edit operations and the low-level PDF
 * rendering and manipulation APIs. It handles the conversion of annotation models and executes the
 * requested operations (insert, update, remove) on the document.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class PdfRendererAnnotationsProcessor(private val renderer: PdfDocumentRenderer) {

    /**
     * Processes a list of draft edit operations sequentially.
     *
     * @param operations The list of [DraftEditOperation]s to apply.
     * @return A [DraftEditResult] indicating the outcome of the processing.
     */
    fun process(operations: List<DraftEditOperation>): DraftEditResult {
        var appliedOperationIndex = 0
        val appliedAnnotationIds = mutableListOf<String>()

        try {
            operations.forEach { draftEditOperation ->
                val aospAnnotationId = applyOperation(draftEditOperation)
                appliedAnnotationIds.add(aospAnnotationId)
                appliedOperationIndex += 1
            }
            return DraftEditResult.Success(appliedAnnotationIds)
        } catch (e: Exception) {
            return DraftEditResult.Failure(
                failedBatchIndex = appliedOperationIndex,
                appliedIds = appliedAnnotationIds,
                errorMessage = e.message ?: "Unknown error",
            )
        }
    }

    private fun applyOperation(operation: DraftEditOperation): String {
        return when (operation) {
            is InsertDraftEditOperation -> insertPdfAnnotation(operation.annotation)
            is UpdateDraftEditOperation -> updatePdfAnnotation(operation.id, operation.annotation)
            is RemoveDraftEditOperation -> removePdfAnnotation(operation.id, operation.pageNum)
            else ->
                throw UnsupportedOperationException(
                    "Unsupported operation: ${operation.javaClass.simpleName}"
                )
        }
    }

    private fun insertPdfAnnotation(annotation: PdfAnnotation): String {
        val converter = PdfAnnotationConvertersFactory.create<PdfAnnotation>(annotation)
        val convertedAnnotation = converter.convert(annotation)

        val aospAnnotationId =
            renderer.withPage(pageNum = annotation.pageNum) { page ->
                return@withPage page.addPageAnnotation(convertedAnnotation).toString()
            }

        if (aospAnnotationId == null) {
            throw IllegalStateException("Failed to add annotation: PdfRenderer returned null ID.")
        }

        return aospAnnotationId
    }

    private fun updatePdfAnnotation(annotationId: String, newAnnotation: PdfAnnotation): String {
        val converter = PdfAnnotationConvertersFactory.create<PdfAnnotation>(newAnnotation)
        val convertedAnnotation = converter.convert(newAnnotation)

        val isUpdated =
            renderer.withPage(pageNum = newAnnotation.pageNum) { page ->
                return@withPage page.updatePageAnnotation(annotationId.toInt(), convertedAnnotation)
            }

        if (isUpdated == null || !isUpdated) {
            throw IllegalStateException("Failed to update annotation")
        }

        return annotationId
    }

    private fun removePdfAnnotation(annotationId: String, pageNum: Int): String {
        renderer.withPage(pageNum = pageNum) { page ->
            return@withPage page.removePageAnnotation(annotationId.toInt())
        }

        return annotationId
    }
}
