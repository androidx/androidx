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

package androidx.compose.ui.text.platform.style

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.roundToInt

internal class CustomBulletSpan(
    private val shape: Shape,
    private val bulletWidthPx: Float,
    private val bulletHeightPx: Float,
    gapWidthPx: Float,
    private val brush: Brush?,
    private val alpha: Float,
    private val drawStyle: DrawStyle,
    private val density: Density,
    textIndentPx: Float,
) : LeadingMarginSpan {

    private val minimumRequiredIndent = (bulletWidthPx + gapWidthPx).roundToInt()
    private val diff = textIndentPx.roundToInt() - minimumRequiredIndent

    override fun getLeadingMargin(first: Boolean): Int {
        // if there isn't enough margin added by the paragraph indentation, add the rest to at least
        // fit the bullet with its padding
        return if (diff >= 0) 0 else abs(diff)
    }

    override fun drawLeadingMargin(
        c: Canvas?,
        p: Paint?,
        x: Int, // the current position of the margin
        dir: Int, // the base direction of the paragraph
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) {
        if (c == null) return
        val yCenter = (top + bottom) / 2f
        val xStart = (x - minimumRequiredIndent).coerceAtLeast(0)

        // this check ensures we only draw bullet ones at the beginning of the paragraph and
        // not on every line
        if ((text as Spanned).getSpanStart(this) == start) {
            p?.let { paint ->
                // set bullet's style
                val currentStyle = paint.style
                paint.setDrawStyle(drawStyle)

                val bulletSize = Size(bulletWidthPx, bulletHeightPx)
                paint.setBrushAndDraw(brush, alpha, bulletSize) {
                    // draw bullet
                    val outline =
                        shape.createOutline(
                            bulletSize,
                            if (dir > 0) LayoutDirection.Ltr else LayoutDirection.Rtl,
                            density,
                        )
                    outline.draw(c, paint, xStart.toFloat(), yCenter, dir)
                }

                // restore Canvas's style
                paint.style = currentStyle
            }
        }
    }
}

private fun Paint.setDrawStyle(value: DrawStyle) {
    when (value) {
        Fill -> this.style = Paint.Style.FILL
        is Stroke -> {
            this.style = Paint.Style.STROKE
            this.strokeWidth = value.width
            this.strokeMiter = value.miter
            this.strokeCap = value.cap.toAndroidCap()
            this.strokeJoin = value.join.toAndroidJoin()
            this.pathEffect = value.pathEffect?.asAndroidPathEffect()
        }
    }
}

/**
 * @param canvas Canvas to draw on
 * @param paint Paint to use
 * @param xStart x coordinate of topLeft point of the bullet's bounds
 * @param yCenter y coordinate of center point of the bullet's bounds
 * @param dir the base direction of the paragraph; if negative, the margin is to the right of the
 *   text, otherwise it is to the left.
 */
private fun Outline.draw(canvas: Canvas, paint: Paint, xStart: Float, yCenter: Float, dir: Int) {
    when (this) {
        is Outline.Generic -> {
            canvas.save()
            canvas.translate(xStart, yCenter - bounds.height / 2f)
            canvas.drawPath(path.asAndroidPath(), paint)
            canvas.restore()
        }
        is Outline.Rounded -> {
            if (!roundRect.isSimple) {
                val path = Path().apply { addRoundRect(roundRect) }
                canvas.save()
                canvas.translate(xStart, yCenter - roundRect.height / 2f)
                canvas.drawPath(path.asAndroidPath(), paint)
                canvas.restore()
            } else {
                // simple rounded rect so all corner radius's are equal
                val xRadius = roundRect.topLeftCornerRadius.x
                canvas.drawRoundRect(
                    xStart,
                    yCenter - roundRect.height / 2f,
                    xStart + dir * roundRect.width,
                    yCenter + (roundRect.height / 2f),
                    xRadius,
                    xRadius,
                    paint,
                )
            }
        }
        is Outline.Rectangle -> {
            canvas.drawRect(
                xStart,
                yCenter - rect.height / 2f,
                xStart + dir * rect.width,
                yCenter + rect.height / 2f,
                paint,
            )
        }
    }
}

private fun Paint.setBrushAndDraw(brush: Brush?, alpha: Float, size: Size, draw: () -> Unit) {
    when (brush) {
        null -> {
            val currentAlpha =
                if (!alpha.isNaN()) {
                    this.alpha.also { this.alpha = kotlin.math.round(alpha * 255.0f).toInt() }
                } else null
            draw()
            currentAlpha?.let { this.alpha = it }
        }
        is SolidColor -> {
            val currentColor = this.color
            val currentAlpha =
                if (!alpha.isNaN()) {
                    this.alpha.also { this.alpha = kotlin.math.round(alpha * 255.0f).toInt() }
                } else null
            this.color = brush.value.toArgb()
            draw()
            this.color = currentColor
            currentAlpha?.let { this.alpha = it }
        }
        is ShaderBrush -> {
            val currentShader = this.shader
            val currentAlpha =
                if (!alpha.isNaN()) {
                    this.alpha.also { this.alpha = kotlin.math.round(alpha * 255.0f).toInt() }
                } else null
            this.setShader(brush.createShader(size))
            draw()
            this.setShader(currentShader)
            currentAlpha?.let { this.alpha = it }
        }
    }
}
