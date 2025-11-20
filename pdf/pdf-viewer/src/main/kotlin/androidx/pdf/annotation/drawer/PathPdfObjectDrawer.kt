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
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.withMatrix
import androidx.pdf.annotation.models.PathPdfObject

/** Creates a [Path] from this object's input points. */
@VisibleForTesting
internal fun PathPdfObject.createPath(): Path {
    return Path().apply {
        inputs.forEachIndexed { index, point ->
            if (index == 0) {
                moveTo(point.x, point.y)
            } else {
                lineTo(point.x, point.y)
            }
        }
    }
}

/** Draws [PathPdfObject] annotations on a [Canvas] using [Canvas.drawPath] */
internal object PathPdfObjectDrawer : PdfObjectDrawer<PathPdfObject> {
    val paint =
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    /**
     * Draws the given [PathPdfObject] onto the provided [Canvas].
     *
     * @param pdfObject The [PathPdfObject] to be drawn.
     * @param canvas The [Canvas] on which to draw the object.
     * @param transform The [Matrix] transformation to be applied to the drawing.
     */
    override fun draw(pdfObject: PathPdfObject, canvas: Canvas, transform: Matrix) {
        paint.color = pdfObject.brushColor
        paint.strokeWidth = pdfObject.brushWidth

        val path = pdfObject.createPath()

        if (!path.isEmpty) {
            canvas.withMatrix(transform) { canvas.drawPath(path, paint) }
        }
    }
}
