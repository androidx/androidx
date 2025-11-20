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

import android.graphics.pdf.component.PdfAnnotation as AospPdfAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.adapter.PdfPage
import androidx.pdf.annotation.converters.PdfAnnotationConvertersFactory
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.JetpackAospIdPair
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class PdfRendererAnnotationsProcessor(private val renderer: PdfDocumentRenderer) :
    PdfAnnotationsProcessor {
    override fun process(annotations: List<PdfAnnotationData>): AnnotationResult {
        // TODO: Sort the list by page num and then add so that we don't have to always open/close
        // pages

        val (success, failures) = annotations.partition { applyAnnotationData(data = it) }
        return AnnotationResult(success = success, failures = failures.map { it.annotation })
    }

    override fun processAddEdits(annotations: List<PdfAnnotationData>): AddEditResult {
        val (success, failures) =
            processAnnotationsByPage(annotations) { editId, page, convertedAnnotation ->
                val aospId = page.addPageAnnotation(convertedAnnotation)
                JetpackAospIdPair(editId, EditId(editId.pageNum, aospId.toString()))
            }
        return AddEditResult(success, failures)
    }

    override fun processUpdateEdits(annotations: List<PdfAnnotationData>): ModifyEditResult {
        val (success, failures) =
            processAnnotationsByPage(annotations) { editId, page, convertedAnnotation ->
                val aospId: Int = editId.value.toInt()
                page.updatePageAnnotation(aospId, convertedAnnotation)
                editId
            }
        return ModifyEditResult(success, failures)
    }

    override fun processRemoveEdits(editIds: List<EditId>): ModifyEditResult {
        val success = mutableListOf<EditId>()
        val failures = mutableListOf<EditId>()

        editIds
            .groupBy { it.pageNum }
            .forEach { pageNum, editIds ->
                if (pageNum < 0 || pageNum > renderer.pageCount) {
                    failures.addAll(editIds)
                    return@forEach
                }
                renderer.withPage(pageNum) { page ->

                    // Defensive call to re-populate annotations mapping in Aosp before calling
                    // removing annotations.
                    // TODO: b/448063670 - Remove this once addressed
                    page.getPageAnnotations()

                    editIds.forEach {
                        try {
                            val aospId: Int = it.value.toInt()
                            page.removePageAnnotation(aospId)
                            success.add(it)
                        } catch (e: Exception) {
                            failures.add(it)
                        }
                    }
                }
            }
        return ModifyEditResult(success, failures)
    }

    // TODO: Revisit this code. We are losing exception info and generated annotation id
    private fun applyAnnotationData(data: PdfAnnotationData): Boolean {
        val converter = PdfAnnotationConvertersFactory.create<PdfAnnotation>(data.annotation)

        try {
            val convertedAnnotation = converter.convert(data.annotation)

            renderer.withPage(pageNum = data.editId.pageNum) { page ->
                val unused = page.addPageAnnotation(convertedAnnotation)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // Takes a list of annotations and operation to perform on success
    private fun <T> processAnnotationsByPage(
        annotations: List<PdfAnnotationData>,
        onSuccessAction:
            (editId: EditId, page: PdfPage, convertedAnnotation: AospPdfAnnotation) -> T,
    ): Pair<List<T>, List<EditId>> {

        val success = mutableListOf<T>()
        val failures = mutableListOf<EditId>()

        annotations
            .groupBy { it.editId.pageNum }
            .forEach { (pageNum, annotationsData) ->
                if (pageNum < 0 || pageNum > renderer.pageCount) {
                    failures.addAll(annotationsData.map { it.editId })
                    return@forEach
                }
                renderer.withPage(pageNum) { page ->
                    annotationsData.forEach { annotationData ->
                        try {
                            val converter =
                                PdfAnnotationConvertersFactory.create<PdfAnnotation>(
                                    annotationData.annotation
                                )
                            val aospAnnotation = converter.convert(annotationData.annotation)
                            val result =
                                onSuccessAction(annotationData.editId, page, aospAnnotation)
                            success.add(result)
                        } catch (e: Exception) {
                            failures.add(annotationData.editId)
                        }
                    }
                }
            }
        return success to failures
    }
}
