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

package androidx.pdf.testapp.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

public class PdfPageObjectView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    public val mainPaint: Paint
    private var xPos: Float = 0.0f
    private var yPos: Float = 0.0f
    private var mBitmap: Bitmap? = null
    private var canvasPathList: MutableList<Path> = mutableListOf()
    public var pdfPathList: MutableList<Path> = mutableListOf()
        private set

    init {
        mainPaint = Paint()
        mainPaint.color = Color.BLACK
        mainPaint.style = Paint.Style.STROKE
        mainPaint.strokeWidth = 4.0f
    }

    override fun onDraw(canvas: Canvas) {
        mBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, paddingTop, width, height - paddingBottom)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        }

        if (!canvasPathList.isEmpty()) {
            for (path in canvasPathList) {
                canvas.drawPath(path, mainPaint)
            }
        }
    }

    public fun clear() {
        canvasPathList.clear()
        pdfPathList.clear()
    }

    public fun setPageBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (mBitmap == null) {
            return false
        }
        event?.let {
            if (event.action == MotionEvent.ACTION_DOWN) {
                xPos = event.x
                yPos = event.y

                val canvasPath = Path()
                canvasPath.moveTo(xPos, yPos)
                canvasPathList.add(canvasPath)

                var pdfPath = Path()
                val xcontentFactor = mBitmap!!.width / width.toFloat()
                val ycontentFactor =
                    mBitmap!!.height / (height - paddingTop - paddingBottom).toFloat()
                pdfPath.moveTo(xPos * xcontentFactor, (yPos - paddingTop) * ycontentFactor)
                pdfPathList.add(pdfPath)

                invalidate()
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                xPos = event.x
                yPos = event.y

                canvasPathList.last().lineTo(xPos, yPos)

                val xcontentFactor = mBitmap!!.width / width.toFloat()
                val ycontentFactor =
                    mBitmap!!.height / (height - paddingTop - paddingBottom).toFloat()
                pdfPathList
                    .last()
                    .lineTo(xPos * xcontentFactor, (yPos - paddingTop) * ycontentFactor)

                invalidate()
            }
        }
        return true
    }
}
