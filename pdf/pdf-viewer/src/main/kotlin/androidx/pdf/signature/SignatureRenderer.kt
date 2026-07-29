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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.TextPaint
import androidx.pdf.R
import androidx.pdf.signature.model.Signature.TypedSignature
import com.google.android.material.color.MaterialColors

/** Renders PDF signatures onto a canvas. */
internal class SignatureRenderer(
    private val context: Context,
    private val vectorPaint: Paint = SignatureDefaults.defaultVectorPaint,
    private val textPaint: TextPaint = SignatureDefaults.defaultTextPaint,
) {

    private val borderWidthPx = context.resources.getDimension(R.dimen.pdf_signature_border_width)
    private val vectorStrokeWidthBasePx =
        context.resources.getDimension(R.dimen.pdf_signature_vector_stroke_width)

    internal val handlePaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, Color.BLUE)
            style = Paint.Style.FILL
        }

    internal val borderPaint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, Color.BLUE)
            style = Paint.Style.STROKE
            strokeWidth = borderWidthPx
        }

    internal fun drawDrawnSignature(
        canvas: Canvas,
        contentWidth: Float,
        contentHeight: Float,
        path: Path,
        zoom: Float,
    ) {
        vectorPaint.strokeWidth = vectorStrokeWidthBasePx * zoom
        val path = SignatureUtils.scalePathToFit(path, contentWidth, contentHeight)
        canvas.drawPath(path, vectorPaint)
    }

    internal fun drawTypedSignature(
        canvas: Canvas,
        contentWidth: Float,
        contentHeight: Float,
        text: String,
        @TypedSignature.TypedFont fontId: Int,
    ) {
        val (startPosition, scaledPaint) =
            SignatureUtils.scaleTypedSignature(text, contentWidth, contentHeight, textPaint, fontId)
        canvas.drawText(text, startPosition.x.coerceAtLeast(0f), startPosition.y, scaledPaint)
    }

    internal fun drawUploadedSignature(
        canvas: Canvas,
        contentWidth: Float,
        contentHeight: Float,
        bitmap: Bitmap,
    ) {
        val targetRect = SignatureUtils.scaleUploadedSignature(contentWidth, contentHeight)
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), targetRect, null)
    }
}
