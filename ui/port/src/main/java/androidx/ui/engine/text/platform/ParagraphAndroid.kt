/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.engine.text.platform

import android.text.TextPaint
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.Canvas
import kotlin.math.floor

internal class ParagraphAndroid constructor(
    val text: StringBuilder,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<ParagraphBuilder.TextStyleIndex>
) {

    private val textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    private var layout: TextLayout? = null

    // TODO(Migration/siyamed): width having -1 but others having 0 as default value is counter
    // intuitive
    var width: Double = -1.0
        get() = layout?.let { field } ?: -1.0

    val height: Double
        get() = layout?.let { it.layout.height.toDouble() } ?: 0.0

    // TODO(Migration/siyamed): we do not have this concept. they limit to the max word size.
    // it didn't make sense to me. I believe we might be able to do it. if we can use
    // wordbreaker.
    val minIntrinsicWidth: Double
        get() = 0.0

    val maxIntrinsicWidth: Double
        get() = layout?.let { it.maxIntrinsicWidth } ?: 0.0

    val alphabeticBaseline: Double
        get() = layout?.let { it.layout.getLineBaseline(0).toDouble() } ?: Double.MAX_VALUE

    // TODO(Migration/siyamed):  (metrics.fUnderlinePosition - metrics.fAscent) * style.height;
    val ideographicBaseline: Double
        get() = Double.MAX_VALUE

    val didExceedMaxLines: Boolean
        get() = false

    fun layout(width: Double, force: Boolean = false) {
        val floorWidth = floor(width)

        if (paragraphStyle.fontSize != null) {
            textPaint.textSize = paragraphStyle.fontSize.toFloat()
        }
        if (paragraphStyle.fontFamily?.typeface != null) {
            textPaint.typeface = paragraphStyle.fontFamily.typeface
        }
        val charSequence = text.toString() as CharSequence
        layout = TextLayout(charSequence = charSequence, width = floorWidth, textPaint = textPaint)
        this.width = floorWidth
    }

    fun getPositionForOffset(offset: Offset): TextPosition {
        val tmpLayout = layout ?: throw IllegalStateException("getPositionForOffset cannot be " +
                "called before layout() is called")

        val line = tmpLayout.layout.getLineForVertical(offset.dy.toInt())
        return TextPosition(
            offset = tmpLayout.layout.getOffsetForHorizontal(line, offset.dx.toFloat()),
            // TODO(Migration/siyamed): we provide a default value
            affinity = TextAffinity.upstream
        )
    }

    fun paint(canvas: Canvas, x: Double, y: Double) {
        val tmpLayout = layout ?: throw IllegalStateException("paint cannot be " +
                "called before layout() is called")
        canvas.translate(x, y)
        tmpLayout.paint(canvas.toFrameworkCanvas())
        canvas.translate(-x, -y)
    }
}