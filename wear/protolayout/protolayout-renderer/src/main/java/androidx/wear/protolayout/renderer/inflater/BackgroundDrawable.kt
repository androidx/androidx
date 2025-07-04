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

package androidx.wear.protolayout.renderer.inflater

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable

/**
 * This class overrides [GradientDrawable] for drawing circular shapes and gradient color.
 *
 * For circular shapes:
 * * The default behavior is that for circles, a circle is drawn first with anti-aliasing applied,
 *   then the view is clipped to the circular outline causing anti-aliasing to be applied a second
 *   time.
 * * The new behavior is drawing a square first and then applying anti-aliasing once when clipping
 *   the view to the circular outline.
 * * Note that the fix is only applied for circular shapes with no stroke (border).
 */
internal class BackgroundDrawable : GradientDrawable() {
    private val customPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isStrokeUsed = false
    private var isAsymmetricalCornerUsed = false
    private var fillColor: Int? = null

    var linearGradientHelper: LinearGradientHelper? = null

    override fun setCornerRadii(radii: FloatArray?) {
        isAsymmetricalCornerUsed = true
        super.setCornerRadii(radii)
    }

    override fun setStroke(width: Int, color: Int) {
        isStrokeUsed = width != 0
        super.setStroke(width, color)
    }

    override fun setColor(argb: Int) {
        fillColor = argb
        super.setColor(argb)
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()

        if (linearGradientHelper == null && (isStrokeUsed || isAsymmetricalCornerUsed)) {
            // If the shape is not a circle or stroke is used, then use the default implementation
            // of draw();
            super.draw(canvas)
            return
        }

        // Shape of the drawable will be clipped by using outline on the caller site to prevent
        // aliasing. See b/357061501.
        if (fillColor != null || linearGradientHelper != null) {
            customPaint.apply {
                reset()
                style = Paint.Style.FILL
                linearGradientHelper?.let { setShader(it.shader) }
                if (shader == null) {
                    fillColor?.let { color = it }
                }
            }
            // Draw a square.
            // The square will be clipped using the outline.
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), customPaint)
        }
    }
}
