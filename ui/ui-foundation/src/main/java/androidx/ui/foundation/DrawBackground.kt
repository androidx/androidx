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

import androidx.annotation.FloatRange
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.ui.geometry.Size
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Paint
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.graphics.drawOutline
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.DrawStyle
import androidx.ui.graphics.drawscope.Fill
import androidx.ui.graphics.drawscope.drawCanvas

/**
 * Draws [shape] with [paint] behind the content.
 */
@Deprecated("Prefer usage of drawBackground(color, shape) or " +
        "drawBackground(brush, shape)",
    ReplaceWith("Modifier.drawBackground(color, shape)"))
fun Modifier.drawBackground(
    paint: Paint,
    shape: Shape
) = this + DrawBackground(
                shape,
                {
                    drawCanvas { canvas, size ->
                        canvas.drawRect(0.0f, 0.0f, size.width, size.height, paint)
                    }
                },
                { outline ->
                    drawCanvas { canvas, _ ->
                        canvas.drawOutline(outline, paint)
                    }
                }
            )

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.ui.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.drawBackground(
    color: Color,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = DrawScope.DefaultAlpha,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) = this + DrawBackground(
                shape,
                {
                    drawRect(
                        color,
                        alpha = alpha,
                        style = style,
                        colorFilter = colorFilter,
                        blendMode = blendMode
                    )
                },
                { outline ->
                    drawOutline(
                        outline,
                        color,
                        alpha = alpha,
                        style = style,
                        colorFilter = colorFilter,
                        blendMode = blendMode
                    )
                }
            )

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.ui.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 */
fun Modifier.drawBackground(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = DrawScope.DefaultAlpha,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode
) = this + DrawBackground(
                shape,
                { drawRect(
                        brush = brush,
                        alpha = alpha,
                        style = style,
                        colorFilter = colorFilter,
                        blendMode = blendMode
                    )
                },
                { outline ->
                    drawOutline(outline,
                        brush,
                        alpha = alpha,
                        style = style,
                        colorFilter = colorFilter,
                        blendMode = blendMode
                    )
                }
            )

private data class DrawBackground internal constructor(
    private val shape: Shape,
    private val drawRect: ContentDrawScope.() -> Unit,
    private val drawOutline: ContentDrawScope.(outline: Outline) -> Unit
) : DrawModifier {

    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            val localOutline =
                if (size == lastSize) {
                    lastOutline!!
                } else {
                    shape.createOutline(size, this)
                }
            drawOutline(localOutline)
            lastOutline = localOutline
            lastSize = size
        }
        drawContent()
    }
}