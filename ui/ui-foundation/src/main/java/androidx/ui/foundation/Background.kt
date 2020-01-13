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

package androidx.ui.foundation

import androidx.ui.core.DrawModifier
import androidx.ui.core.draw
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.drawOutline
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize
import androidx.ui.unit.toRect

private fun background(paint: Paint) = draw { canvas, size ->
    canvas.drawRect(size.toRect(), paint)
}

/**
 * Returns a [DrawModifier] that fills the layout rectangle with the given background [color].
 */
fun background(color: Color): DrawModifier =
    background(Paint().also { paint ->
        paint.style = PaintingStyle.fill
        paint.color = color
    })

/**
 * Returns a [DrawModifier] that fills the layout rectangle with the given background [brush].
 */
fun background(brush: Brush): DrawModifier =
    background(Paint().also { brush.applyTo(it) })

/**
 * Returns a [DrawModifier] that draws [shape] with a solid [color], with the size of the
 * layout's rectangle.
 */
fun background(shape: Shape, color: Color): DrawModifier =
    background(shape, SolidColor(color))

/**
 * Returns a [DrawModifier] that draws [shape] with [brush], with the size of the layout's
 * rectangle.
 */
fun background(shape: Shape, brush: Brush): DrawModifier = object : DrawModifier {
    private val paint = Paint()

    init {
        brush.applyTo(paint)
    }

    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        val outline = shape.createOutline(size, density)
        canvas.drawOutline(outline, paint)
        drawContent()
    }
}