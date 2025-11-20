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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.SparseArray
import android.view.View
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.drawer.DefaultPdfObjectDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactory
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfDocumentAnnotationsDrawerImpl
import androidx.pdf.annotation.drawer.PdfObjectDrawerFactory
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * A custom Android [View] responsible for drawing a collection of annotations onto a Canvas. Each
 * set of page annotations can have its own transformation matrix.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationsView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    /**
     * Represents of page annotations that will be rendered on the view. The collection is
     * referenced by the page number (0-indexed).
     */
    public var annotations: SparseArray<PageAnnotationsData> = SparseArray()
        set(value) {
            field = value
            invalidate()
        }

    private var pdfObjectDrawerFactory: PdfObjectDrawerFactory = DefaultPdfObjectDrawerFactoryImpl

    private var annotationDrawerFactory: PdfAnnotationDrawerFactory =
        PdfAnnotationDrawerFactoryImpl(pdfObjectDrawerFactory)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        PdfDocumentAnnotationsDrawerImpl(annotationDrawerFactory).draw(annotations, canvas)
    }

    /**
     * Holds all annotations for a single PDF page and their transformation matrix.
     *
     * @property annotations List of [PdfAnnotation]s on the page.
     * @property transform [Matrix] to apply when drawing these annotations.
     */
    public data class PageAnnotationsData(
        val annotations: List<PdfAnnotation>,
        val transform: Matrix,
    )
}
