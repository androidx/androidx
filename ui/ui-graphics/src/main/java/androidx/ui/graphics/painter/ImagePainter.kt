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

import androidx.ui.geometry.Offset
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.Image
import androidx.ui.graphics.Paint
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize

private val EmptyPaint = Paint()

/**
 * [Painter] implementation used to draw an [Image] into the provided canvas
 * This implementation can handle applying alpha and [ColorFilter] to it's drawn result
 */
data class ImagePainter(private val image: Image) : Painter() {

    /**
     * Lazily allocated paint used to draw the [Image] if an alpha value between 0.0f and 1.0f
     * is provided or a color filter is defined on the [Painter]
     */
    private var paint: Paint? = null

    private val size = PxSize(IntPx(image.width), IntPx(image.height))

    override fun onDraw(canvas: Canvas, bounds: PxSize) {
        // Always draw the image in the top left as we expect it to be translated and scaled
        // in the appropriate position
        canvas.drawImage(image, Offset.zero, paint ?: EmptyPaint)
    }

    /**
     * Return the dimension of the underlying [Image] as it's intrinsic width and height
     */
    override val intrinsicSize: PxSize get() = size

    override fun applyAlpha(alpha: Float): Boolean {
        obtainPaint().alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        obtainPaint().colorFilter = colorFilter
        return true
    }

    private fun obtainPaint(): Paint {
        var target = paint
        if (target == null) {
            target = Paint()
            paint = target
        }
        return target
    }
}