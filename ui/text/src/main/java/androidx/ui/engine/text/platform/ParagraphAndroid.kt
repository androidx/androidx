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
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
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
import androidx.text.selection.WordBoundary
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.ShadowSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.text.style.WordSpacingSpan
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextIndent
import androidx.ui.engine.text.TextPosition
import androidx.ui.engine.text.hasFontAttributes
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import androidx.ui.painting.TextStyle
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

const val LINE_FEED = '\n'

internal class ParagraphAndroid constructor(
    val text: String,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<AnnotatedString.Item<TextStyle>>,
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

    val baseline: Float
        get() = layout?.let { it.layout.getLineBaseline(0).toFloat() } ?: Float.MAX_VALUE

    val didExceedMaxLines: Boolean
        get() = layout?.let { it.didExceedMaxLines } ?: false

    // TODO(Migration/haoyuchang): more getters needed to access the values in textPaint.
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

    fun layout(width: Float) {
        val floorWidth = floor(width)

        paragraphStyle.fontSize?.let {
            textPaint.textSize = it
        }

        // TODO(siyamed): This default values are problem here. If the user just gives a single font
        // in the family, and does not provide any fontWeight, TypefaceAdapter will still get the
        // call as FontWeight.normal (which is the default value)
        if (paragraphStyle.hasFontAttributes()) {
            textPaint.typeface = typefaceAdapter.create(
                fontFamily = paragraphStyle.fontFamily,
                fontWeight = paragraphStyle.fontWeight ?: FontWeight.normal,
                fontStyle = paragraphStyle.fontStyle ?: FontStyle.Normal,
                fontSynthesis = paragraphStyle.fontSynthesis ?: FontSynthesis.All
            )
        }

        paragraphStyle.locale?.let {
            textPaint.textLocale = Locale(
                it.languageCode,
                it.countryCode ?: ""
            )
        }

        val charSequence = applyTextStyle(text, paragraphStyle.textIndent, textStyles)

        val alignment = toLayoutAlign(paragraphStyle.textAlign)
        // TODO(Migration/haoyuchang): Layout has more settings that flutter,
        //  we may add them in future.
        val textDirectionHeuristic = when (paragraphStyle.textDirection) {
            TextDirection.Ltr -> TEXT_DIRECTION_LTR
            TextDirection.Rtl -> TEXT_DIRECTION_RTL
            else -> DEFAULT_TEXT_DIRECTION
        }

        val maxLines = paragraphStyle.maxLines ?: DEFAULT_MAX_LINES
        val justificationMode = when (paragraphStyle.textAlign) {
            TextAlign.Justify -> JUSTIFICATION_MODE_INTER_WORD
            else -> DEFAULT_JUSTIFICATION_MODE
        }

        val lineSpacingMultiplier = paragraphStyle.lineHeight ?: DEFAULT_LINESPACING_MULTIPLIER

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

    // TODO(qqd): TextAffinity in TextPosition is not implemented. We need to clean it up in future.
    fun getPositionForOffset(offset: Offset): TextPosition {
        val line = ensureLayout.getLineForVertical(offset.dy.toInt())
        return TextPosition(
            offset = ensureLayout.getOffsetForHorizontal(line, offset.dx),
            // TODO(Migration/siyamed): we provide a default value
            affinity = TextAffinity.upstream
        )
    }

    /**
     * Returns the bounding box as Rect of the character for given TextPosition. Rect includes the
     * top, bottom, left and right of a character.
     */
    // TODO:(qqd) Implement RTL case.
    fun getBoundingBoxForTextPosition(textPosition: TextPosition): Rect {
        val left = ensureLayout.getPrimaryHorizontal(textPosition.offset)
        val right = ensureLayout.getPrimaryHorizontal(textPosition.offset + 1)

        val line = ensureLayout.getLineForOffset(textPosition.offset)
        val top = ensureLayout.getLineTop(line)
        val bottom = ensureLayout.getLineBottom(line)

        return Rect(top = top, bottom = bottom, left = left, right = right)
    }

    fun getPathForRange(start: Int, end: Int): Path {
        val path = android.graphics.Path()
        ensureLayout.getSelectionPath(start, end, path)
        return Path(path)
    }

    fun getCursorRect(offset: Int): Rect {
        // TODO(nona): Support cursor drawable.
        val cursorWidth = 4.0f
        val layout = ensureLayout
        val horizontal = layout.getPrimaryHorizontal(offset)
        val line = layout.getLineForOffset(offset)

        return Rect(
            horizontal - 0.5f * cursorWidth,
            layout.getLineTop(line),
            horizontal + 0.5f * cursorWidth,
            layout.getLineBottom(line)
        )
    }

    private var wordBoundary: WordBoundary? = null

    fun getWordBoundary(offset: Int): Pair<Int, Int> {
        if (wordBoundary == null) {
            wordBoundary = WordBoundary(textLocale, ensureLayout.text)
        }

        return Pair(wordBoundary!!.getWordStart(offset), wordBoundary!!.getWordEnd(offset))
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
        tmpLayout.paint(canvas.nativeCanvas)
        canvas.translate(-x, -y)
    }

    private fun applyTextStyle(
        text: String,
        textIndent: TextIndent?,
        textStyles: List<AnnotatedString.Item<TextStyle>>
    ): CharSequence {
        if (textStyles.isEmpty() && textIndent == null) return text
        val spannableString = SpannableString(text)

        textIndent?.let { indent ->
            if (indent.firstLine == 0.px && indent.restLine == 0.px) return@let
            spannableString.setSpan(
                LeadingMarginSpan.Standard(
                    indent.firstLine.value.toInt(),
                    indent.restLine.value.toInt()
                ),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        for (textStyle in textStyles) {
            val start = textStyle.start
            val end = textStyle.end
            val style = textStyle.style.getTextStyle()

            if (start < 0 || start >= text.length || end <= start || end > text.length) continue

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
                    ForegroundColorSpan(it.toArgb()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            style.decoration?.let {
                if (it.contains(TextDecoration.Underline)) {
                    spannableString.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (it.contains(TextDecoration.LineThrough)) {
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

            // Be aware that fontSizeScale must be applied after fontSize.
            style.fontSizeScale?.let {
                spannableString.setSpan(
                    RelativeSizeSpan(it),
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
                    fontStyle = style.fontStyle ?: FontStyle.Normal,
                    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
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

            if (Build.VERSION.SDK_INT >= 29) {
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
                    LocaleSpan(Locale(it.languageCode, it.countryCode!!)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): framework only support background color now
            style.background?.let {
                spannableString.setSpan(
                    BackgroundColorSpan(it.toArgb()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // TODO(Migration/haoyuchang): implement foreground or decide if we really need it
            style.shadow?.let {
                spannableString.setSpan(
                    ShadowSpan(it.color.toArgb(), it.offset.dx, it.offset.dy, it.blurRadius.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannableString
    }
}

internal fun toLayoutAlign(align: TextAlign?): Int = when (align) {
    TextAlign.Left -> ALIGN_LEFT
    TextAlign.Right -> ALIGN_RIGHT
    TextAlign.Center -> ALIGN_CENTER
    TextAlign.Start -> ALIGN_NORMAL
    TextAlign.End -> ALIGN_OPPOSITE
    else -> DEFAULT_ALIGNMENT
}