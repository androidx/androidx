/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical.compose

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.annotation.Px
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt

private const val SPAN_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

/**
 * Converts this [AnnotatedString] into an Android [Spanned] for
 * [androidx.text.vertical.VerticalTextLayout].
 *
 * Supported [SpanStyle] properties are [SpanStyle.color], [SpanStyle.fontSize],
 * [SpanStyle.fontWeight], [SpanStyle.fontStyle], [SpanStyle.background], and
 * [SpanStyle.letterSpacing]. Other [SpanStyle] properties and [AnnotatedString.paragraphStyles] are
 * ignored.
 *
 * Note: [SpanStyle.fontWeight] currently maps to [Typeface.NORMAL] or [Typeface.BOLD] via an
 * additive [StyleSpan].
 */
internal fun AnnotatedString.toSpanned(density: Density): Spanned {
    return SpannableStringBuilder(text).apply {
        var hasLetterSpacing = false
        spanStyles.fastForEach { range ->
            if (!range.isValidIn(length)) return@fastForEach
            val style = range.item
            applySpanStyle(style, range.start, range.end, density)
            if (style.letterSpacing.isSp || style.letterSpacing.isEm) {
                hasLetterSpacing = true
            }
        }

        if (hasLetterSpacing) {
            // Attach letter-spacing spans after size spans. LayoutRun applies spans in insertion
            // order, and LetterSpacingSpanPx divides by the updated textSize on TextPaint.
            spanStyles.fastForEach { range ->
                if (!range.isValidIn(length)) return@fastForEach
                val letterSpacing = range.item.letterSpacing
                val span =
                    when {
                        letterSpacing.isEm -> LetterSpacingSpanEm(letterSpacing.value)
                        letterSpacing.isSp ->
                            LetterSpacingSpanPx(with(density) { letterSpacing.toPx() })
                        else -> null
                    }
                if (span != null) {
                    setSpan(span, range.start, range.end, SPAN_FLAG)
                }
            }
        }
    }
}

private fun AnnotatedString.Range<*>.isValidIn(length: Int): Boolean =
    start >= 0 && start < length && end > start && end <= length

private fun SpannableStringBuilder.applySpanStyle(
    style: SpanStyle,
    start: Int,
    end: Int,
    density: Density,
) {
    if (style.color.isSpecified) {
        setSpan(ForegroundColorSpan(style.color.toArgb()), start, end, SPAN_FLAG)
    }

    when {
        style.fontSize.isSp -> {
            val px = with(density) { style.fontSize.toPx().roundToInt() }
            setSpan(AbsoluteSizeSpan(px), start, end, SPAN_FLAG)
        }
        style.fontSize.isEm -> {
            setSpan(RelativeSizeSpan(style.fontSize.value), start, end, SPAN_FLAG)
        }
    }

    val weight = style.fontWeight
    val italic = style.fontStyle == FontStyle.Italic
    if (weight != null || style.fontStyle != null) {
        // TODO(b/502088091): Use the exact FontWeight (1..1000) instead of coarsening to
        //  Typeface.NORMAL or Typeface.BOLD. StyleSpan is additive (want = oldStyle | style) and
        //  only supports NORMAL, BOLD, ITALIC, and BOLD_ITALIC.
        // Match Compose ui-text's AndroidBold threshold (FontWeight.W600).
        val isBold = weight != null && weight >= FontWeight.W600
        val typefaceStyle =
            when {
                isBold && italic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
        setSpan(StyleSpan(typefaceStyle), start, end, SPAN_FLAG)
    }

    if (style.background.isSpecified) {
        setSpan(BackgroundColorSpan(style.background.toArgb()), start, end, SPAN_FLAG)
    }
}

/** Sets [TextPaint.letterSpacing] to [letterSpacing] in `em` units. */
internal class LetterSpacingSpanEm(val letterSpacing: Float) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.letterSpacing = letterSpacing
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.letterSpacing = letterSpacing
    }
}

/**
 * Sets [TextPaint.letterSpacing] from [letterSpacing] in pixels, divided by the `em` width of the
 * paint.
 */
internal class LetterSpacingSpanPx(@Px val letterSpacing: Float) : MetricAffectingSpan() {
    private fun TextPaint.updatePaint() {
        // In the framework, 1em of letter spacing equals textSize * textScaleX pixels.
        val emWidth = textSize * textScaleX
        if (emWidth != 0.0f) {
            letterSpacing = this@LetterSpacingSpanPx.letterSpacing / emWidth
        }
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.updatePaint()
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.updatePaint()
    }
}
