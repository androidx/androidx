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

import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.annotation.VisibleForTesting
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
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.text.style.WordSpacingSpan
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextPosition
import androidx.ui.engine.text.hasFontAttributes
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

internal class ParagraphAndroid constructor(
    val text: StringBuilder,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<ParagraphBuilder.TextStyleIndex>,
    val typefaceAdapter: TypefaceAdapter = TypefaceAdapter()
) {

    @VisibleForTesting
    internal val textPaint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var layout: TextLayout? = null

    // TODO(Migration/siyamed): width having -1 but others having 0 as default value is counter
    // intuitive
    var width: Float = -1.0f
        get() = layout?.let { field } ?: -1.0f

    val height: Float
        get() = layout?.let {
            // TODO(Migration/haoyuchang): Figure out a way to add bottomPadding properly
            val lineCount = it.lineCount
            if (paragraphStyle.maxLines != null &&
                paragraphStyle.maxLines >= 0 &&
                paragraphStyle.maxLines < lineCount
            ) {
                it.layout.getLineBottom(paragraphStyle.maxLines - 1).toFloat()
            } else {
                it.layout.height.toFloat()
            }
        } ?: 0.0f

    // TODO(Migration/siyamed): we do not have this concept. they limit to the max word size.
    // it didn't make sense to me. I believe we might be able to do it. if we can use
    // wordbreaker.
    val minIntrinsicWidth: Float
        get() = 0.0f

    val maxIntrinsicWidth: Float
        get() = layout?.let { it.maxIntrinsicWidth } ?: 0.0f

    val alphabeticBaseline: Float
        get() = layout?.let { it.layout.getLineBaseline(0).toFloat() } ?: Float.MAX_VALUE

    // TODO(Migration/siyamed):  (metrics.fUnderlinePosition - metrics.fAscent) * style.height;
    val ideographicBaseline: Float
        get() = Float.MAX_VALUE

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

    fun layout(width: Float, force: Boolean = false) {
        val floorWidth = floor(width)

        paragraphStyle.fontSize?.let {
            textPaint.textSize = it
        }

        // TODO: This default values are problem here. If the user just gives a single font
        // in the family, and does not provide any fontWeight, TypefaceAdapter will still get the
        // call as FontWeight.normal (which is the default value)
        if (paragraphStyle.hasFontAttributes()) {
            textPaint.typeface = typefaceAdapter.create(
                fontFamily = paragraphStyle.fontFamily,
                fontWeight = paragraphStyle.fontWeight ?: FontWeight.normal,
                fontStyle = paragraphStyle.fontStyle ?: FontStyle.normal,
                fontSynthesis = paragraphStyle.fontSynthesis ?: FontSynthesis.all

            )
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
            paragraphStyle.lineHeight ?: DEFAULT_LINESPACING_MULTIPLIER

        val ellipsize = if (paragraphStyle.ellipsis == true) {
            TextUtils.TruncateAt.END
        } else {
            null
        }

        layout = TextLayout(
            charSequence = charSequence,
            width = floorWidth,
            textPaint = textPaint,
            ellipsize = ellipsize,
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
            offset = ensureLayout.layout.getOffsetForHorizontal(line, offset.dx),
            // TODO(Migration/siyamed): we provide a default value
            affinity = TextAffinity.upstream
        )
    }

    fun getPathForRange(start: Int, end: Int): Path {
        val path = android.graphics.Path()
        ensureLayout.layout.getSelectionPath(start, end, path)
        return Path(path)
    }

    fun getLineLeft(lineIndex: Int): Float = ensureLayout.getLineLeft(lineIndex)

    fun getLineRight(lineIndex: Int): Float = ensureLayout.getLineRight(lineIndex)

    fun getLineHeight(lineIndex: Int): Float = ensureLayout.getLineHeight(lineIndex)

    fun getLineWidth(lineIndex: Int): Float = ensureLayout.getLineWidth(lineIndex)

    /**
     * @return true if the given line is ellipsized, else false.
     */
    fun isEllipsisApplied(lineIndex: Int): Boolean = ensureLayout.isEllipsisApplied(lineIndex)

    fun paint(canvas: Canvas, x: Float, y: Float) {
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

            // Be aware that SuperscriptSpan needs to be applied before all other spans which
            // affect FontMetrics
            style.baselineShift?.let {
                spannableString.setSpan(
                    BaselineShiftSpan(it.multiplier),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            style.color?.let {
                spannableString.setSpan(
                    ForegroundColorSpan(it.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            style.decoration?.let {
                if (it.contains(TextDecoration.underline)) {
                    spannableString.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
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

            style.fontFeatureSettings?.let {
                spannableString.setSpan(
                    FontFeatureSpan(it),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (style.hasFontAttributes()) {
                val typeface = typefaceAdapter.create(
                    fontFamily = style.fontFamily,
                    fontWeight = style.fontWeight ?: FontWeight.normal,
                    fontStyle = style.fontStyle ?: FontStyle.normal,
                    fontSynthesis = style.fontSynthesis ?: FontSynthesis.all
                )
                spannableString.setSpan(
                    TypefaceSpan(typeface),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // TODO(Migration/haoyuchang): implement textBaseLine
            style.textGeometricTransform?.scaleX?.let {
                spannableString.setSpan(
                    ScaleXSpan(it),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            style.textGeometricTransform?.skewX?.let {
                spannableString.setSpan(
                    SkewXSpan(it),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (Build.VERSION.SDK_INT >= 28) {
                style.wordSpacing?.let {
                    spannableString.setSpan(
                        WordSpacingSpan(it),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            // TODO(Migration/haoyuchang): support letter spacing with pixel.
            style.letterSpacing?.let {
                spannableString.setSpan(
                    LetterSpacingSpan(it),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): implement height
            style.locale?.let {
                spannableString.setSpan(
                    // TODO(Migration/haoyuchang): support locale fallback in the framework
                    LocaleSpan(Locale(it.languageCode, it.countryCode)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): framework only support background color now
            style.background?.let {
                spannableString.setSpan(
                    BackgroundColorSpan(it.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): implement foreground or decide if we really need it
        }
        return spannableString
    }
}