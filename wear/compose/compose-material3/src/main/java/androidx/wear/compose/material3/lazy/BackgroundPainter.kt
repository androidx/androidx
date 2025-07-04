/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.lazy

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter

internal class BackgroundPainter(
    internal val height: DrawScope.() -> Float,
    internal val shape: Shape,
    private val border: BorderStroke?,
    private val backgroundPainter: Painter,
) : Painter() {
    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val actualSize = size.copy(height = height())
        val shapeOutline = shape.createOutline(actualSize, layoutDirection, this@onDraw)

        if (shapeOutline != previousOutline) {
            previousOutline = shapeOutline
            cachedPath.run {
                reset()
                addOutline(shapeOutline)
            }
        }

        clipPath(cachedPath) {
            if (border != null) {
                drawOutline(
                    outline = shapeOutline,
                    brush = border.brush,
                    style = Stroke(border.width.toPx().coerceAtLeast(1f)),
                )
            }
            with(backgroundPainter) { draw(actualSize) }
        }
    }

    private val cachedPath: Path = Path()
    private var previousOutline: Outline? = null
}
