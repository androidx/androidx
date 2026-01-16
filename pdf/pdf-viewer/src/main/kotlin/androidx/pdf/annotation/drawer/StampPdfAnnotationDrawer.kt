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
import android.graphics.Matrix
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.models.StampAnnotation

/** Draws [StampAnnotation] instances on a [Canvas]. */
internal class StampPdfAnnotationDrawer(
    private val pdfObjectDrawerFactory: PdfObjectDrawerFactory
) : PdfAnnotationDrawer<StampAnnotation> {
    override fun draw(pdfAnnotation: StampAnnotation, canvas: Canvas, transform: Matrix) {
        pdfAnnotation.pdfObjects.forEach { pdfObject ->
            try {
                val drawer = pdfObjectDrawerFactory.create(pdfObject)
                @Suppress("UNCHECKED_CAST")
                (drawer as PdfObjectDrawer<PdfObject>).draw(pdfObject, canvas, transform)
            } catch (e: UnsupportedOperationException) {
                // TODO: b/440966572 - Handle Logging of Unsupported Annotations and Pfd Objects.
            }
        }
    }
}
