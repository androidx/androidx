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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Paint
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.drawOutline
import androidx.ui.unit.PxSize
import androidx.ui.unit.toRect

/**
 * Returns a [DrawModifier] that draws [shape] with a solid [color], with the size of the
 * layout's rectangle.
 *
 * @sample androidx.ui.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Use Modifier.drawBackground",
    replaceWith = ReplaceWith(
        "Modifier.drawBackground(color, shape)",
        "androidx.ui.core.Modifier",
        "androidx.ui.foundation.drawBackground",
        "androidx.ui.foundation.shape.RectangleShape"
    )
)
@Composable
fun DrawBackground(color: Color, shape: Shape = RectangleShape): DrawBackground =
    DrawBackground(SolidColor(color), shape)

/**
 * Returns a [DrawModifier] that draws [shape] with [brush], with the size of the layout's
 * rectangle.
 *
 * @sample androidx.ui.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 */
@Deprecated(
    "Use Modifier.drawBackground",
    replaceWith = ReplaceWith(
        "Modifier.drawBackground(brush, shape)",
        "androidx.ui.core.Modifier",
        "androidx.ui.foundation.drawBackground",
        "androidx.ui.foundation.shape.RectangleShape"
    )
)
@Composable
fun DrawBackground(brush: Brush, shape: Shape = RectangleShape): DrawBackground {
    return remember(shape, brush) {
        val paint = Paint().also {
            brush.applyTo(it)
        }
        DrawBackground(paint, shape)
    }
}

/**
 * Draws [shape] with [paint] behind the content.
 */
fun Modifier.drawBackground(
    paint: Paint,
    shape: Shape
) = this + DrawBackground(paint, shape)

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.drawBackground(
    color: Color,
    shape: Shape = RectangleShape
) = drawBackground(SolidColor(color), shape)

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.drawBackground(
    brush: Brush,
    shape: Shape = RectangleShape
) = composed {
    drawBackground(
        remember(brush) {
            Paint().also { brush.applyTo(it) }
        },
        shape
    )
}

data class DrawBackground internal constructor(
    private val paint: Paint,
    private val shape: Shape
) : DrawModifier {

    // naive cache outline calculation if size is the same
    private var lastSize: PxSize? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect(size.toRect(), paint)
        } else {
            val localOutline =
                if (size == lastSize) lastOutline!! else shape.createOutline(size, this)
            drawOutline(localOutline, paint)
            lastOutline = localOutline
            lastSize = size
        }
        drawContent()
    }
}