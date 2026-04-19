/*
 * Copyright 2026 The Android Open Source Project
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

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.core.graphics.toRect
import androidx.pdf.annotation.models.HighlightAnnotation

internal class HighlightAnnotationDrawer : PdfAnnotationDrawer<HighlightAnnotation> {

    @androidx.annotation.VisibleForTesting
    internal val paint: Paint =
        Paint().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = BlendMode.MULTIPLY
            } else {
                // Safe to ignore: HighlightAnnotationDrawer is only invoked on
                // Build.VERSION_CODES.S and above
            }
            style = Paint.Style.FILL
        }

    override fun draw(pdfAnnotation: HighlightAnnotation, canvas: Canvas, transform: Matrix) {
        paint.apply { color = pdfAnnotation.color }
        for (bound in pdfAnnotation.bounds) {
            val highlightBound = RectF(bound)
            transform.mapRect(highlightBound)
            canvas.drawRect(highlightBound.toRect(), paint)
        }
    }
}
