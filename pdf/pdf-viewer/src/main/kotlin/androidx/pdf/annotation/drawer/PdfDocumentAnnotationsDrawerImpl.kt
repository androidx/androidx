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

package androidx.pdf.annotation.drawer

import android.graphics.Canvas
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.pdf.annotation.AnnotationsView.PageAnnotationsData
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * A [PdfDocumentAnnotationsDrawer] that is responsible for drawing a collection of PDF annotations
 * in the document.
 *
 * @param annotationDrawerFactory Factory to create specific drawers for each annotation type.
 */
internal class PdfDocumentAnnotationsDrawerImpl(
    private val annotationDrawerFactory: PdfAnnotationDrawerFactory
) : PdfDocumentAnnotationsDrawer {

    /**
     * Draws the annotations onto the provided [Canvas].
     *
     * @param pagesAnnotationData A [SparseArray] mapping page numbers to their
     *   [PageAnnotationsData], which includes the list of annotations and their transformation
     *   matrix.
     * @param canvas The [Canvas] on which to draw the annotations.
     */
    override fun draw(pagesAnnotationData: SparseArray<PageAnnotationsData>, canvas: Canvas) {
        pagesAnnotationData.forEach { _, pageAnnotationData ->
            pageAnnotationData.keyedAnnotations.forEach { keyedAnnotation ->
                val annotation = keyedAnnotation.annotation

                @Suppress("UNCHECKED_CAST")
                val drawer =
                    annotationDrawerFactory.create(annotation) as PdfAnnotationDrawer<PdfAnnotation>
                drawer.draw(annotation, canvas, pageAnnotationData.transform)
            }
        }
    }
}
