/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.sin

/**
 * Helper for rendering a dashed outline around a complication. Intended for use with
 * [LayerMode#DRAW_HIGHLIGHTED].
 */
public class ComplicationOutlineRenderer {
    public companion object {
        // Dashed lines are used for complication selection.
        internal const val DASH_WIDTH = 10.0f
        internal const val DASH_GAP = 2.0f
        internal const val DASH_LENGTH = 5.0f

        internal val dashPaint = Paint().apply {
            strokeWidth = DASH_WIDTH
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
            color = Color.RED
        }

        /** Draws a thick dotted line around the complication with the given bounds. */
        @JvmStatic
        public fun drawComplicationSelectOutline(canvas: Canvas, bounds: Rect) {
            if (bounds.width() == bounds.height()) {
                drawCircleDashBorder(canvas, bounds)
                return
            }
            val radius = bounds.height() / 2.0f

            // Draw left arc dash.
            var cx = bounds.left + radius
            var cy = bounds.centerY().toFloat()
            var startAngle = (Math.PI / 2.0f).toFloat()
            val dashCount = (Math.PI * radius / (DASH_WIDTH + DASH_GAP)).toInt()
            drawArcDashBorder(canvas, cx, cy, radius, startAngle, DASH_LENGTH, dashCount)

            // Draw right arc dash.
            cx = bounds.right - radius
            cy = bounds.centerY().toFloat()
            startAngle = (Math.PI / 2.0f).toFloat() * 3.0f
            drawArcDashBorder(canvas, cx, cy, radius, startAngle, DASH_LENGTH, dashCount)

            // Draw straight line dash.
            val rectangleWidth = bounds.width() - 2.0f * radius - 2.0f * DASH_GAP
            val cnt = (rectangleWidth / (DASH_WIDTH + DASH_GAP)).toInt()
            val baseX: Float = bounds.left + radius + DASH_GAP
            val fixGap: Float = (rectangleWidth - cnt * DASH_WIDTH) / (cnt - 1)
            for (i in 0 until cnt) {
                val startX: Float = baseX + i * (fixGap + DASH_WIDTH) + DASH_WIDTH / 2
                var startY = bounds.top.toFloat()
                var endY: Float = bounds.top - DASH_LENGTH
                canvas.drawLine(startX, startY, startX, endY, dashPaint)
                startY = bounds.bottom.toFloat()
                endY = startY + DASH_LENGTH
                canvas.drawLine(startX, startY, startX, endY, dashPaint)
            }
        }

        internal fun drawArcDashBorder(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            r: Float,
            startAngle: Float,
            dashLength: Float,
            dashCount: Int
        ) {
            for (i in 0 until dashCount) {
                val rot = (2.0 * Math.PI / (2.0 * (dashCount - 1).toDouble()) * i + startAngle)
                val startX = (r * cos(rot)).toFloat() + cx
                val startY = (r * sin(rot)).toFloat() + cy
                val endX = ((r + dashLength) * cos(rot).toFloat()) + cx
                val endY = ((r + dashLength) * sin(rot).toFloat()) + cy
                canvas.drawLine(startX, startY, endX, endY, dashPaint)
            }
        }

        internal fun drawCircleDashBorder(canvas: Canvas, bounds: Rect) {
            val radius = bounds.width() / 2.0f
            val dashCount = (2.0 * Math.PI * radius / (DASH_WIDTH + DASH_GAP)).toInt()
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            for (i in 0 until dashCount) {
                val rot = (i * 2.0 * Math.PI / dashCount)
                val startX = (radius * cos(rot).toFloat()) + cx
                val startY = (radius * sin(rot).toFloat()) + cy
                val endX = ((radius + DASH_LENGTH) * cos(rot).toFloat()) + cx
                val endY = ((radius + DASH_LENGTH) * sin(rot).toFloat()) + cy
                canvas.drawLine(startX, startY, endX, endY, dashPaint)
            }
        }
    }
}
