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

package androidx.ui.baseui.shape

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Draw
import androidx.ui.core.PxSize
import androidx.ui.core.vectorgraphics.Brush
import androidx.ui.core.vectorgraphics.SolidColor
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.drawOutline
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint

/**
 * Draw the [shape] with the provided [color].
 *
 * @param shape the [Shape] to draw.
 * @param color the [Color] to use for filling the shape.
 */
@Composable
fun DrawShape(shape: Shape, color: Color) {
    DrawShape(shape = shape, brush = +memo(color) { SolidColor(color) })
}

/**
 * Draw the [shape] with the provided [brush].
 *
 * @param shape the [Shape] to draw.
 * @param brush the [Brush] to use for filling the shape.
 */
@Composable
fun DrawShape(shape: Shape, brush: Brush) {
    with(+memo { DrawShapeCacheHolder() }) {
        lastShape = shape
        Draw { canvas, parentSize ->
            brush.applyBrush(paint)
            lastParentSize = parentSize
            val outline =
                lastOutline ?: shape.createOutline(parentSize, density).also { lastOutline = it }
            canvas.drawOutline(outline, paint)
        }
    }
}

private class DrawShapeCacheHolder {
    val paint = Paint().apply { isAntiAlias = true }
    var lastOutline: Outline? = null
    var lastParentSize: PxSize? = null
        set(value) {
            if (value != field) {
                field = value
                lastOutline = null
            }
        }
    var lastShape: Shape? = null
        set(value) {
            if (value != field) {
                field = value
                lastOutline = null
            }
        }
}
