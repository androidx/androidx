/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.modulate

// Copied from AndroidTextPaint.

internal class SkiaTextPaint : Paint by Paint() {
    @VisibleForTesting
    internal var brush: Brush? = null

    internal var shaderState: State<Shader?>? = null

    @VisibleForTesting
    internal var brushSize: Size? = null

    fun setBrush(brush: Brush?, size: Size, alpha: Float = Float.NaN) {
        when (brush) {
            // null brush should just clear the shader and leave `color` as the final decider
            // while painting
            null -> {
                clearShader()
            }
            // SolidColor brush can be treated just like setting a color.
            is SolidColor -> {
                if (color.isSpecified) {
                    this.color = brush.value.modulate(alpha)
                    clearShader()
                }
            }
            // This is the brush type that we mostly refer to when we talk about brush support.
            // Below code is almost equivalent to;
            // val this.shaderState = remember(brush, brushSize) {
            //     derivedStateOf {
            //         brush.createShader(size)
            //     }
            // }
            is ShaderBrush -> {
                if (this.brush != brush || this.brushSize != size) {
                    if (size.isSpecified) {
                        this.brush = brush
                        this.brushSize = size
                        this.shaderState = derivedStateOf {
                            brush.createShader(size)
                        }
                    }
                }
                this.shader = this.shaderState?.value
                this.alpha = if (alpha.isNaN()) 1f else alpha.coerceIn(0f, 1f)
            }
        }
    }

    fun setDrawStyle(drawStyle: DrawStyle?) {
        when (drawStyle) {
            Fill, null -> {
                // Stroke properties such as strokeWidth, strokeMiter are not re-set because
                // Fill style should make those properties no-op. Next time the style is set
                // as Stroke, stroke properties get re-set as well.
                style = PaintingStyle.Fill
            }

            is Stroke -> {
                style = PaintingStyle.Stroke
                strokeWidth = drawStyle.width
                strokeMiterLimit = drawStyle.miter
                strokeJoin = drawStyle.join
                strokeCap = drawStyle.cap
                pathEffect = drawStyle.pathEffect
            }
        }
    }

    /**
     * Clears all shader related cache parameters and native shader property.
     */
    private fun clearShader() {
        this.shaderState = null
        this.brush = null
        this.brushSize = null
        this.shader = null
    }
}
