/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.diagnose

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

/**
 * Overlay View for drawing alignment result and target grid line to {@link Canvas} in
 * diagnosis mode.
 */
class OverlayView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val unalignedTargetGridPaint = Paint()
    private var alignedTargetGridPaint = Paint()
    private var thresholdPaint = Paint()
    // TODO: should it be nullable? only drawing when there are 4 barcodes
    private var result: Calibration? = null

    init {
        // TODO: scale density of the width with the screen's pixel density
        unalignedTargetGridPaint.color = Color.RED
        unalignedTargetGridPaint.strokeWidth = 8F
        alignedTargetGridPaint.color = Color.GREEN
        alignedTargetGridPaint.strokeWidth = 8F
        thresholdPaint.color = Color.WHITE
    }

    fun setCalibrationResult(result: Calibration) {
        this.result = result
    }

    private fun drawGridLines(canvas: Canvas, line: Pair<PointF, PointF>, paint: Paint) {
        canvas.drawLine(line.first.x, line.first.y, line.second.x, line.second.y, paint)
    }

    private fun getGridLinePaint(isAligned: Boolean?): Paint {
        return if (isAligned == true) {
            alignedTargetGridPaint
        } else {
            unalignedTargetGridPaint
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "calling on draw")

        result?.let {
            val paintColor = getGridLinePaint(result!!.isAligned)
            result!!.topGrid?.let {
                drawGridLines(canvas, it, paintColor)
            }
            result!!.bottomGrid?.let {
                drawGridLines(canvas, it, paintColor)
            }
            result!!.rightGrid?.let {
                drawGridLines(canvas, it, paintColor)
            }
            result!!.leftGrid?.let {
                drawGridLines(canvas, it, paintColor)
            }

            // drawing threshold box
            result!!.thresholdTopLeft?.let {
                canvas.drawRect(it, thresholdPaint)
            }
            result!!.thresholdTopRight?.let {
                canvas.drawRect(it, thresholdPaint)
            }
            result!!.thresholdBottomLeft?.let {
                canvas.drawRect(it, thresholdPaint)
            }
            result!!.thresholdBottomRight?.let {
                canvas.drawRect(it, thresholdPaint)
            }
        }
        Log.d(TAG, "finished drawing")
    }

    companion object {
        private const val TAG = "OverlayView"
    }
}
