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
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.pdf.R
import androidx.pdf.signature.model.Signature

/** A custom [View] responsible for drawing a single signature onto a Canvas. */
internal class SignatureView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    internal var signatureData: Signature? = null
        get() = field ?: error("signatureData accessed before initialization")
        set(value) {
            if (field != value) {
                field = value
                invalidate()
                requestLayout()
            }
        }

    private val renderer: SignatureRenderer = SignatureRenderer(context)

    private var scaleFactor: Float = 1f

    private val handleRadiusPx: Float =
        context.resources.getDimension(R.dimen.pdf_signature_handle_radius)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentSignature = signatureData ?: return

        val r = handleRadiusPx
        val contentWidth = width - (r * 2)
        val contentHeight = height - (r * 2)

        if (contentWidth <= 0 || contentHeight <= 0) return

        canvas.save()
        canvas.translate(r, r)

        when (currentSignature) {
            is Signature.DrawnSignature ->
                renderer.drawDrawnSignature(
                    canvas,
                    contentWidth,
                    contentHeight,
                    currentSignature.drawnPath,
                    scaleFactor,
                )
            is Signature.TypedSignature ->
                renderer.drawTypedSignature(
                    canvas,
                    contentWidth,
                    contentHeight,
                    currentSignature.typedText,
                    currentSignature.typedFont,
                )
            is Signature.UploadedSignature ->
                renderer.drawUploadedSignature(
                    canvas,
                    contentWidth,
                    contentHeight,
                    currentSignature.imageBitmap,
                )
            else -> {}
        }

        canvas.restore()
        drawSizeHandles(canvas)
    }

    private fun drawSizeHandles(canvas: Canvas) {
        val r = handleRadiusPx
        if (signatureData?.isSelected == true) {
            val w = width.toFloat()
            val h = height.toFloat()
            val midX = w / 2f
            val midY = h / 2f

            canvas.drawRect(r, r, w - r, h - r, renderer.borderPaint)

            canvas.drawCircle(r, r, r, renderer.handlePaint)
            canvas.drawCircle(w - r, r, r, renderer.handlePaint)
            canvas.drawCircle(r, h - r, r, renderer.handlePaint)
            canvas.drawCircle(w - r, h - r, r, renderer.handlePaint)

            canvas.drawCircle(midX, r, r, renderer.handlePaint)
            canvas.drawCircle(midX, h - r, r, renderer.handlePaint)
            canvas.drawCircle(r, midY, r, renderer.handlePaint)
            canvas.drawCircle(w - r, midY, r, renderer.handlePaint)
        }
    }

    internal fun updatePdfViewport(scaleFactor: Float) {
        if (this.scaleFactor != scaleFactor) {
            this.scaleFactor = scaleFactor
            invalidate()
        }
    }

    internal interface OnSignatureUpdatedListener {
        fun onSignatureUpdated(updatedSignature: Signature)

        fun onSignatureDeleted(id: String)
    }

    private var signatureUpdatedListener: OnSignatureUpdatedListener? = null

    /**
     * Sets the callback triggered for all signature interactions, including selection,
     * moving/resizing, and deletion.
     */
    internal fun setOnSignatureUpdatedListener(listener: OnSignatureUpdatedListener?) {
        this.signatureUpdatedListener = listener
    }
}
