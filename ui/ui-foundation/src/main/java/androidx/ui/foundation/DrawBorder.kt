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
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.graphics.PathOperation
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.addOutline
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
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
        BorderDrawingCache(shape, size, brush)
    }
    cache.lastBrush = brush
    cache.lastShape = shape
    cache.lastBorderWidth = size
    return DrawBorder(cache)
}

class DrawBorder internal constructor(private val cache: BorderDrawingCache) : DrawModifier {

    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        with(cache) {
            lastParentSize = size

            if (!outerPathIsCached) {
                outerPath.reset()
                outerPath.addOutline(lastShape.createOutline(size, density))
                outerPathIsCached = true
            }

            if (!diffPathIsCached) {
                // to have an inner path we provide a smaller parent size and shift the result
                val borderSize =
                    if (lastBorderWidth == Dp.Hairline) {
                        1.px
                    } else {
                        Px(lastBorderWidth.value * density.density)
                    }
                val sizeMinusBorder = PxSize(
                    width = size.width - borderSize * 2,
                    height = size.height - borderSize * 2
                )
                innerPath.reset()
                innerPath.addOutline(lastShape.createOutline(sizeMinusBorder, density))
                innerPath.shift(Offset(borderSize.value, borderSize.value))

                // now we calculate the diff between the inner and the outer paths
                diffPath.op(outerPath, innerPath, PathOperation.difference)
                diffPathIsCached = true
            }

            lastBrush.applyTo(paint)
            drawContent()
            canvas.drawPath(diffPath, paint)
        }
    }
}

internal class BorderDrawingCache(shape: Shape, width: Dp, brush: Brush) {
    val outerPath = Path()
    val innerPath = Path()
    val diffPath = Path()
    val paint = Paint().apply { isAntiAlias = true }
    var outerPathIsCached = false
    var diffPathIsCached = false
    var lastBrush: Brush = brush
    var lastParentSize: PxSize? = null
        set(value) {
            if (value != field) {
                field = value
                outerPathIsCached = false
                diffPathIsCached = false
            }
        }
    var lastShape: Shape = shape
        set(value) {
            if (value != field) {
                field = value
                outerPathIsCached = false
                diffPathIsCached = false
            }
        }
    var lastBorderWidth: Dp = width
        set(value) {
            if (value != field) {
                field = value
                diffPathIsCached = false
            }
        }
}