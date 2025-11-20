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

package androidx.pdf.ink.view.brush

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.pdf.ink.R
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import kotlin.math.min

/**
 * A custom [View] that draws a tilted oval to represent a brush tip. The size of the oval is
 * determined by the `brushSize` property.
 */
internal class BrushPreviewView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color =
                MaterialColors.getColor(
                    this@BrushPreviewView,
                    MaterialR.attr.colorPrimary,
                    ContextCompat.getColor(context, R.color.default_brush_preview_color),
                )
            style = Paint.Style.FILL
        }

    // A RectF object to define the bounds of the oval, reused to avoid allocation in onDraw
    private val ovalRect = RectF()

    /**
     * The desired size of the brush in pixels.
     *
     * This value determines the height of the oval drawn to represent the brush. The width of the
     * oval is calculated as half of this value to create a stylized brush-tip shape.
     */
    var brushSize: Float = resources.getDimension(R.dimen.default_brush_preview_size)
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        // Center should be within the padded area
        val centerX = paddingLeft + availableWidth / 2f
        val centerY = paddingTop + availableHeight / 2f

        // The max size of the brush should not exceed the PADDED view's bounds
        val maxBrushSize = min(availableWidth, availableHeight).toFloat()
        // Clamp the effective size to prevent it from being drawn outside the view
        val effectiveSize = brushSize.coerceIn(0f, maxBrushSize)

        // The oval's width is narrower than its height to create the brush tip shape
        val ovalHeight = effectiveSize
        val ovalWidth = effectiveSize / 2f

        // Calculate the bounding box for the oval
        val left = centerX - ovalWidth / 2f
        val top = centerY - ovalHeight / 2f
        val right = centerX + ovalWidth / 2f
        val bottom = centerY + ovalHeight / 2f
        ovalRect.set(left, top, right, bottom)

        canvas.save()
        // Rotate the canvas to make the oval appear tilted
        canvas.rotate(45f, centerX, centerY)
        canvas.drawOval(ovalRect, paint)
        canvas.restore()
    }
}
