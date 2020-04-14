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
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxSize
import kotlin.math.roundToInt

private val EmptyPaint = Paint()

/**
 * [Painter] implementation used to draw an [ImageAsset] into the provided canvas
 * This implementation can handle applying alpha and [ColorFilter] to it's drawn result
 *
 * @param image The [ImageAsset] to draw
 * @param srcBounds Optional rectangle used to draw a subsection of the [ImageAsset]. If null is
 * provided the entire [ImageAsset] is drawn within the bounds.
 * These bounds must have the following requirements:
 *
 * 1) Left and top bounds must be greater than or equal to zero
 * 2) Right and bottom bounds must be greater than the left and top respectively
 * 3) Width and height of the bounds must be less than or equal to the dimensions of [image]
 */
data class ImagePainter(private val image: ImageAsset, val srcBounds: Rect? = null) : Painter() {

    private val size: PxSize = if (srcBounds != null) {
        require(
            srcBounds.left >= 0 &&
            srcBounds.top >= 0 &&
            srcBounds.right <= image.width &&
            srcBounds.bottom <= image.height &&
            srcBounds.right > srcBounds.left &&
            srcBounds.bottom > srcBounds.top
        )
        PxSize(IntPx(srcBounds.width.roundToInt()), IntPx(srcBounds.height.roundToInt()))
    } else {
        PxSize(IntPx(image.width), IntPx(image.height))
    }

    /**
     * Lazily allocated paint used to draw the [ImageAsset] if an alpha value between 0.0f and 1.0f
     * is provided or a color filter is defined on the [Painter]
     */
    private var paint: Paint? = null

    override fun onDraw(canvas: Canvas, bounds: PxSize) {
        canvas.drawImageRect(
            image,
            srcBounds,
            Rect.fromLTWH(0.0f, 0.0f, bounds.width.value, bounds.height.value),
            paint ?: EmptyPaint
        )
    }

    /**
     * Return the dimension of the underlying [ImageAsset] as it's intrinsic width and height
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