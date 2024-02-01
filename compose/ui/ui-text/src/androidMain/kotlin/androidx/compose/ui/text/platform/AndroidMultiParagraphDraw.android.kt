/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.util.fastForEach

internal actual fun MultiParagraph.drawMultiParagraph(
    canvas: Canvas,
    brush: Brush,
    alpha: Float,
    shadow: Shadow?,
    decoration: TextDecoration?,
    drawStyle: DrawStyle?,
    blendMode: BlendMode
) {
    canvas.save()

    if (paragraphInfoList.size <= 1) {
        drawParagraphs(canvas, brush, alpha, shadow, decoration, drawStyle, blendMode)
    } else {
        when (brush) {
            is SolidColor -> {
                drawParagraphs(canvas, brush, alpha, shadow, decoration, drawStyle, blendMode)
            }
            is ShaderBrush -> {
                var height = 0f
                var width = 0f
                paragraphInfoList.fastForEach {
                    height += it.paragraph.height
                    width = maxOf(width, it.paragraph.width)
                }
                val shader = brush.createShader(Size(width, height))
                val matrix = Matrix()
                shader.getLocalMatrix(matrix)
                paragraphInfoList.fastForEach {
                    it.paragraph.paint(
                        canvas = canvas,
                        brush = ShaderBrush(shader),
                        alpha = alpha,
                        shadow = shadow,
                        textDecoration = decoration,
                        drawStyle = drawStyle,
                        blendMode = blendMode
                    )
                    canvas.translate(0f, it.paragraph.height)
                    matrix.setTranslate(0f, -it.paragraph.height)
                    shader.setLocalMatrix(matrix)
                }
            }
        }
    }

    canvas.restore()
}

private fun MultiParagraph.drawParagraphs(
    canvas: Canvas,
    brush: Brush,
    alpha: Float,
    shadow: Shadow?,
    decoration: TextDecoration?,
    drawStyle: DrawStyle?,
    blendMode: BlendMode
) {
    paragraphInfoList.fastForEach {
        it.paragraph.paint(canvas, brush, alpha, shadow, decoration, drawStyle, blendMode)
        canvas.translate(0f, it.paragraph.height)
    }
}
