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

package androidx.compose.ui.text.platform

import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.platform.extensions.correctBlurRadius
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.modulate
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt

internal class AndroidTextPaint(flags: Int, density: Float) : TextPaint(flags) {
    init {
        this.density = density
    }

    // A wrapper to use Compose Paint APIs on this TextPaint
    private var backingComposePaint: Paint? = null
    private val composePaint: Paint
        get() {
            val finalBackingComposePaint = backingComposePaint
            if (finalBackingComposePaint != null) return finalBackingComposePaint
            return this.asComposePaint().also { backingComposePaint = it }
        }

    private var textDecoration: TextDecoration = TextDecoration.None

    private var backingBlendMode: BlendMode = DrawScope.DefaultBlendMode

    @VisibleForTesting internal var shadow: Shadow = Shadow.None

    @VisibleForTesting internal var brush: Brush? = null

    internal var shaderState: State<Shader?>? = null

    @VisibleForTesting internal var brushSize: Size? = null

    private var drawStyle: DrawStyle? = null

    fun setTextDecoration(textDecoration: TextDecoration?) {
        if (textDecoration == null) return
        if (this.textDecoration != textDecoration) {
            this.textDecoration = textDecoration
            isUnderlineText = TextDecoration.Underline in this.textDecoration
            isStrikeThruText = TextDecoration.LineThrough in this.textDecoration
        }
    }

    fun setShadow(shadow: Shadow?) {
        if (shadow == null) return
        if (this.shadow != shadow) {
            this.shadow = shadow
            if (this.shadow == Shadow.None) {
                clearShadowLayer()
            } else {
                setShadowLayer(
                    correctBlurRadius(this.shadow.blurRadius),
                    this.shadow.offset.x,
                    this.shadow.offset.y,
                    this.shadow.color.toArgb()
                )
            }
        }
    }

    fun setColor(color: Color) {
        if (color.isSpecified) {
            this.color = color.toArgb()
            clearShader()
        }
    }

    fun setBrush(brush: Brush?, size: Size, alpha: Float = Float.NaN) {
        when (brush) {
            // null brush should just clear the shader and leave `color` as the final decider
            // while painting
            null -> {
                clearShader()
            }
            // SolidColor brush can be treated just like setting a color.
            is SolidColor -> {
                setColor(brush.value.modulate(alpha))
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
                        this.shaderState = derivedStateOf { brush.createShader(size) }
                    }
                }
                composePaint.shader = this.shaderState?.value
                setAlpha(alpha)
            }
        }
    }

    fun setDrawStyle(drawStyle: DrawStyle?) {
        if (drawStyle == null) return
        if (this.drawStyle != drawStyle) {
            this.drawStyle = drawStyle
            when (drawStyle) {
                Fill -> {
                    // Stroke properties such as strokeWidth, strokeMiter are not re-set because
                    // Fill style should make those properties no-op. Next time the style is set
                    // as Stroke, stroke properties get re-set as well.

                    // avoid unnecessarily allocating a composePaint object in hot path.
                    this.style = Style.FILL
                }
                is Stroke -> {
                    composePaint.style = PaintingStyle.Stroke
                    composePaint.strokeWidth = drawStyle.width
                    composePaint.strokeMiterLimit = drawStyle.miter
                    composePaint.strokeJoin = drawStyle.join
                    composePaint.strokeCap = drawStyle.cap
                    composePaint.pathEffect = drawStyle.pathEffect
                }
            }
        }
    }

    // BlendMode is only available to DrawScope.drawText.
    // not intended to be used by TextStyle/SpanStyle.
    var blendMode: BlendMode
        get() {
            return backingBlendMode
        }
        set(value) {
            if (value == backingBlendMode) return
            composePaint.blendMode = value
            backingBlendMode = value
        }

    /** Clears all shader related cache parameters and native shader property. */
    private fun clearShader() {
        this.shaderState = null
        this.brush = null
        this.brushSize = null
        this.shader = null
    }
}

/** Accepts an alpha value in the range [0f, 1f] then maps to an integer value in [0, 255] range. */
internal fun TextPaint.setAlpha(alpha: Float) {
    if (!alpha.isNaN()) {
        val alphaInt = alpha.fastCoerceIn(0f, 1f).times(255).fastRoundToInt()
        setAlpha(alphaInt)
    }
}
