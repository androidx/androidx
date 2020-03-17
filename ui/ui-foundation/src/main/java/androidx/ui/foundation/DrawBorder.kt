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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.geometry.isSimple
import androidx.ui.geometry.outerRect
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathOperation
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.addOutline
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxSize
import androidx.ui.unit.minDimension
import androidx.ui.unit.px

/**
 * Returns a [Modifier] that adds border with appearance specified with a [border] and a [shape]
 *
 * @sample androidx.ui.foundation.samples.BorderSample()
 *
 * @param border [Border] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
@Composable
fun DrawBorder(border: Border, shape: Shape = RectangleShape): DrawBorder =
    DrawBorder(size = border.size, brush = border.brush, shape = shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [size], [color] and a
 * [shape]
 *
 * @sample androidx.ui.foundation.samples.BorderSampleWithDataClass()
 *
 * @param size width of the border. Use [Dp.Hairline] for a hairline border.
 * @param color color to paint the border with
 * @param shape shape of the border
 */
@Composable
fun DrawBorder(size: Dp, color: Color, shape: Shape = RectangleShape): DrawBorder =
    DrawBorder(size, SolidColor(color), shape)

/**
 * Returns a [Modifier] that adds border with appearance specified with [size], [brush] and a
 * [shape]
 *
 * @sample androidx.ui.foundation.samples.BorderSampleWithBrush()
 *
 * @param size width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
@Composable
fun DrawBorder(size: Dp, brush: Brush, shape: Shape): DrawBorder {
    val cache = remember {
        DrawBorderCache()
    }
    return DrawBorder(cache, shape, size, brush)
}

class DrawBorder internal constructor(
    private val cache: DrawBorderCache,
    private val shape: Shape,
    private val borderWidth: Dp,
    private val brush: Brush
) : DrawModifier {

    // put params to constructor to ensure proper equals and update cache after construction
    init {
        cache.lastShape = shape
        cache.borderSize = borderWidth
    }

    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        with(cache) {
            drawContent()
            modifierSize = size
            val outline = modifierSizeOutline(density)
            val borderSize =
                if (borderWidth == Dp.Hairline) 1f else borderWidth.value * density.density
            brush.applyTo(paint)
            paint.strokeWidth = borderSize

            if (borderSize <= 0 || size.minDimension <= 0.px) {
                return
            } else if (outline is Outline.Rectangle) {
                drawRoundRectBorder(borderSize, outline.rect, 0f, canvas, paint)
            } else if (outline is Outline.Rounded && outline.rrect.isSimple) {
                val radius = outline.rrect.bottomLeftRadiusY
                drawRoundRectBorder(borderSize, outline.rrect.outerRect(), radius, canvas, paint)
            } else {
                val path = borderPath(density, borderSize)
                paint.style = PaintingStyle.fill
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawRoundRectBorder(
        borderSize: Float,
        rect: Rect,
        radius: Float,
        canvas: Canvas,
        paint: Paint
    ) {
        val fillWithBorder = borderSize * 2 >= rect.getShortestSide()
        paint.style = if (fillWithBorder) PaintingStyle.fill else PaintingStyle.stroke

        val delta = if (fillWithBorder) 0f else borderSize / 2
        canvas.drawRoundRect(
            left = rect.left + delta,
            top = rect.top + delta,
            right = rect.right - delta,
            bottom = rect.bottom - delta,
            radiusX = radius,
            radiusY = radius,
            paint = paint
        )
    }

    // cannot make DrawBorder data class because of the cache, though need hashcode/equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DrawBorder

        if (shape != other.shape) return false
        if (borderWidth != other.borderWidth) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }
}

internal class DrawBorderCache {
    private val outerPath = Path()
    private val innerPath = Path()
    private val diffPath = Path()
    private var dirtyPath = true
    private var dirtyOutline = true
    private var outline: Outline? = null

    val paint = Paint().apply { isAntiAlias = true }

    var lastShape: Shape? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
                dirtyOutline = true
            }
        }

    var borderSize: Dp = Dp.Unspecified
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
            }
        }

    var modifierSize: PxSize? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyPath = true
                dirtyOutline = true
            }
        }

    fun modifierSizeOutline(density: Density): Outline {
        if (dirtyOutline) {
            outline = lastShape?.createOutline(modifierSize!!, density)
            dirtyOutline = false
        }
        return outline!!
    }

    fun borderPath(density: Density, borderPixelSize: Float): Path {
        if (dirtyPath) {
            val size = modifierSize!!
            diffPath.reset()
            outerPath.reset()
            innerPath.reset()
            if (borderPixelSize * 2 >= size.minDimension.value) {
                diffPath.addOutline(modifierSizeOutline(density))
            } else {
                outerPath.addOutline(lastShape!!.createOutline(size, density))
                val sizeMinusBorder =
                    PxSize(
                        size.width - borderPixelSize.px * 2,
                        size.height - borderPixelSize.px * 2
                    )
                innerPath.addOutline(lastShape!!.createOutline(sizeMinusBorder, density))
                innerPath.shift(Offset(borderPixelSize, borderPixelSize))

                // now we calculate the diff between the inner and the outer paths
                diffPath.op(outerPath, innerPath, PathOperation.difference)
            }
            dirtyPath = false
        }
        return diffPath
    }
}