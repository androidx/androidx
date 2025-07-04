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
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * Interface for drawing [PdfAnnotation] instances onto a [Canvas].
 *
 * Implementations of this interface define how a specific type of [PdfAnnotation] should be
 * rendered.
 *
 * @param T The type of [PdfAnnotation] that this drawer can handle.
 */
internal interface PdfAnnotationDrawer<T : PdfAnnotation> {

    /**
     * Draws the given [pdfAnnotation] onto the provided [canvas].
     *
     * @param pdfAnnotation The PDF annotation to be drawn.
     * @param canvas The canvas on which to draw the annotation.
     * @param transform The [Matrix] transformation to be applied to the drawing. Implementations
     *   will apply this transform to the [canvas] before rendering the annotation.
     */
    fun draw(pdfAnnotation: T, canvas: Canvas, transform: Matrix) {}
}
