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
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.PdfObject

/**
 * Interface for drawing [PdfObject] instances onto a [Canvas].
 *
 * @param T The type of [PdfObject] that this drawer can handle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PdfObjectDrawer<T : PdfObject> {

    /**
     * Draws the given [pdfObject] onto the provided [canvas].
     *
     * @param pdfObject The PDF object to be drawn.
     * @param canvas The canvas on which to draw the object.
     * @param transform The [Matrix] transformation to be applied to the drawing. Implementations
     *   will apply this transform to the [canvas] before rendering the object.
     */
    public fun draw(pdfObject: T, canvas: Canvas, transform: Matrix)
}
