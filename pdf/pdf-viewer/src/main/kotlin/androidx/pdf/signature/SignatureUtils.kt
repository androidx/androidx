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

package androidx.pdf.signature

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.pdf.signature.model.Signature

internal object SignatureUtils {

    private const val CONTENT_FIT_SCALE = 0.9f

    /** Scales text to be horizontally centered within the given dimensions. */
    fun scaleTypedSignature(
        text: String,
        width: Float,
        height: Float,
        paint: TextPaint,
        fontId: Int,
    ): Pair<PointF, Paint> {
        if (text.isEmpty() || width <= 0f || height <= 0f) return Pair(PointF(0f, 0f), paint)

        paint.typeface = getTypeface(fontId)
        val availableWidth = width * CONTENT_FIT_SCALE
        val availableHeight = height * CONTENT_FIT_SCALE
        paint.textSize = availableHeight
        val metrics = paint.fontMetrics
        val totalFontHeight = metrics.descent - metrics.ascent
        paint.textSize = availableHeight * (availableHeight / totalFontHeight)

        paint.textScaleX = 1f

        val textWidth = paint.measureText(text)
        if (textWidth > availableWidth && textWidth > 0) {
            paint.textScaleX = availableWidth / textWidth
        }

        val startY = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        val startX = (width - paint.measureText(text)) / 2f
        return Pair(PointF(startX, startY), paint)
    }

    /** Scales a bitmap perfectly centered within the given dimensions. */
    fun scaleUploadedSignature(width: Float, height: Float): RectF {
        if (width <= 0f || height <= 0f) return RectF(0f, 0f, width, height)

        val targetWidth = width * CONTENT_FIT_SCALE
        val targetHeight = height * CONTENT_FIT_SCALE

        val offX = (width - targetWidth) / 2f
        val offY = (height - targetHeight) / 2f

        return RectF(offX, offY, offX + targetWidth, offY + targetHeight)
    }

    /** Scales a path object perfectly centered within the given dimensions. */
    fun scalePathToFit(path: Path, targetWidth: Float, targetHeight: Float): Path {
        if (path.isEmpty || targetWidth <= 0f || targetHeight <= 0f) return path
        val bounds = RectF()
        path.computeBounds(bounds, true)

        if (bounds.isEmpty) return Path()

        val fitWidth = targetWidth * CONTENT_FIT_SCALE
        val fitHeight = targetHeight * CONTENT_FIT_SCALE

        val offsetX = (targetWidth - fitWidth) / 2f
        val offsetY = (targetHeight - fitHeight) / 2f

        val targetBounds = RectF(offsetX, offsetY, offsetX + fitWidth, offsetY + fitHeight)

        val matrix = Matrix()
        matrix.setRectToRect(bounds, targetBounds, Matrix.ScaleToFit.FILL)

        val scaledPath = Path()
        path.transform(matrix, scaledPath)
        return scaledPath
    }

    private fun getTypeface(fontId: Int): Typeface {
        return when (fontId) {
            Signature.TypedSignature.FONT_SERIF -> Typeface.SERIF
            Signature.TypedSignature.FONT_SANS_SERIF -> Typeface.SANS_SERIF
            Signature.TypedSignature.FONT_MONOSPACE -> Typeface.MONOSPACE
            Signature.TypedSignature.FONT_CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
    }
}
