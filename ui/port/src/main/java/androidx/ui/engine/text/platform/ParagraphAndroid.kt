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

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.text.LayoutCompat.ALIGN_CENTER
import androidx.text.LayoutCompat.ALIGN_LEFT
import androidx.text.LayoutCompat.ALIGN_NORMAL
import androidx.text.LayoutCompat.ALIGN_OPPOSITE
import androidx.text.LayoutCompat.ALIGN_RIGHT
import androidx.text.LayoutCompat.DEFAULT_ALIGNMENT
import androidx.text.LayoutCompat.DEFAULT_JUSTIFICATION_MODE
import androidx.text.LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER
import androidx.text.LayoutCompat.DEFAULT_MAX_LINES
import androidx.text.LayoutCompat.DEFAULT_TEXT_DIRECTION
import androidx.text.LayoutCompat.JUSTIFICATION_MODE_INTER_WORD
import androidx.text.LayoutCompat.TEXT_DIRECTION_LTR
import androidx.text.LayoutCompat.TEXT_DIRECTION_RTL
import androidx.text.TextLayout
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.Canvas
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

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
        get() = layout?.let { it.didExceedMaxLines } ?: false

    // TODO(Migraition/haoyuchang): more getters needed to access the values in textPaint.
    val textLocale: Locale
        get() = textPaint.textLocale

    val lineCount: Int
        get() = ensureLayout.lineCount

    private val ensureLayout: TextLayout
        get() {
            val tmpLayout = this.layout ?: throw java.lang.IllegalStateException(
                "layout() should be called first"
            )
            return tmpLayout
        }

    val underlyingText: CharSequence
        get() = ensureLayout.text

    fun layout(width: Double, force: Boolean = false) {
        val floorWidth = floor(width)

        paragraphStyle.fontSize?.let {
            textPaint.textSize = it.toFloat()
        }

        paragraphStyle.fontFamily?.typeface?.let {
            textPaint.typeface = it
        }

        paragraphStyle.locale?.let {
            textPaint.textLocale = Locale(
                it.languageCode,
                it.countryCode ?: ""
            )
        }

        val charSequence = applyTextStyle(text, textStyles)
        val alignment = when (paragraphStyle.textAlign) {
            TextAlign.LEFT -> ALIGN_LEFT
            TextAlign.RIGHT -> ALIGN_RIGHT
            TextAlign.CENTER -> ALIGN_CENTER
            TextAlign.START -> ALIGN_NORMAL
            TextAlign.END -> ALIGN_OPPOSITE
            else -> DEFAULT_ALIGNMENT
        }
        // TODO(Migration/haoyuchang): Layout has more settings that flutter,
        //  we may add them in future.
        val textDirectionHeuristic = when (paragraphStyle.textDirection) {
            TextDirection.LTR -> TEXT_DIRECTION_LTR
            TextDirection.RTL -> TEXT_DIRECTION_RTL
            else -> DEFAULT_TEXT_DIRECTION
        }
        val maxLines = paragraphStyle.maxLines ?: DEFAULT_MAX_LINES
        val justificationMode = when (paragraphStyle.textAlign) {
            TextAlign.JUSTIFY -> JUSTIFICATION_MODE_INTER_WORD
            else -> DEFAULT_JUSTIFICATION_MODE
        }

        val lineSpacingMultiplier =
            paragraphStyle.lineHeight ?: DEFAULT_LINESPACING_MULTIPLIER.toDouble()

        layout = TextLayout(
            charSequence = charSequence,
            width = floorWidth,
            textPaint = textPaint,
            alignment = alignment,
            textDirectionHeuristic = textDirectionHeuristic,
            lineSpacingMultiplier = lineSpacingMultiplier,
            maxLines = maxLines,
            justificationMode = justificationMode
        )
        this.width = floorWidth
    }

    fun getPositionForOffset(offset: Offset): TextPosition {
        val line = ensureLayout.layout.getLineForVertical(offset.dy.toInt())
        return TextPosition(
            offset = ensureLayout.layout.getOffsetForHorizontal(line, offset.dx.toFloat()),
            // TODO(Migration/siyamed): we provide a default value
            affinity = TextAffinity.upstream
        )
    }

    fun getLineLeft(index: Int): Double = ensureLayout.getLineLeft(index)

    fun getLineRight(index: Int): Double = ensureLayout.getLineRight(index)

    fun getLineHeight(index: Int): Double = ensureLayout.getLineHeight(index)

    fun getLineWidth(index: Int): Double = ensureLayout.getLineWidth(index)

    fun paint(canvas: Canvas, x: Double, y: Double) {
        val tmpLayout = layout ?: throw IllegalStateException("paint cannot be " +
                "called before layout() is called")
        canvas.translate(x, y)
        tmpLayout.paint(canvas.toFrameworkCanvas())
        canvas.translate(-x, -y)
    }

    private fun applyTextStyle(
        text: StringBuilder,
        textStyles: List<ParagraphBuilder.TextStyleIndex>
    ): CharSequence {
        if (textStyles.isEmpty()) return text
        val spannableString = SpannableString(text)
        for (textStyle in textStyles) {
            val start = textStyle.start
            val end = textStyle.end
            val style = textStyle.textStyle

            style.color?.let {
                spannableString.setSpan(
                    ForegroundColorSpan(it.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): implement decorationStyle, decorationColor
            style.decoration?.let {
                if (it.contains(TextDecoration.underline)) {
                    spannableString.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (it.contains(TextDecoration.overline)) {
                    // TODO(Migration/haoyuchang): implement overline
                }
                if (it.contains(TextDecoration.lineThrough)) {
                    spannableString.setSpan(
                        StrikethroughSpan(),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            style.fontSize?.let {
                spannableString.setSpan(
                    AbsoluteSizeSpan(it.roundToInt()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): implement fontWeight, fontStyle, fontFamily
            // TODO(Migration/haoyuchang): implement textBaseLine
            // TODO(Migration/haoyuchang): implement letterSpacing, wordSpacing
            // TODO(Migration/haoyuchang): implement height
            // TODO(Migration/haoyuchang): implement locale
            // TODO(Migration/haoyuchang): implement background
            // TODO(Migration/haoyuchang): implement foreground or decide if we really need it
        }
        return spannableString
    }
}