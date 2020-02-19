/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.painter

import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.Paint
import androidx.ui.unit.PxSize
import androidx.ui.unit.PxSize.Companion.UnspecifiedSize

/**
 * [Painter] implementation used to fill the provided bounds with the specified color
 */
data class ColorPainter(val color: Color) : Painter() {
    // TODO njawad replace with Brush + provide overloads for Color
    private val paint = Paint()

    init {
        paint.color = color
    }

    override fun onDraw(canvas: Canvas, bounds: PxSize) {
        // TODO njawad update with more shapes/ investigate merging/replacing DrawShape.kt
        canvas.drawRect(
            Rect.fromLTWH(0.0f, 0.0f, bounds.width.value, bounds.height.value),
            paint
        )
    }

    override fun applyAlpha(alpha: Float): Boolean {
        paint.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        paint.colorFilter = colorFilter
        return true
    }

    /**
     * Drawing a color does not have an intrinsic size, return [UnspecifiedSize] here
     */
    override val intrinsicSize: PxSize = UnspecifiedSize
}