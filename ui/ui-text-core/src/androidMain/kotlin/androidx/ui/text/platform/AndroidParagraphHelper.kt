/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.platform

import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.ui.graphics.isSet
import androidx.ui.graphics.toArgb
import androidx.ui.intl.Locale
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Placeholder
import androidx.ui.text.PlaceholderVerticalAlign
import androidx.ui.text.SpanStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.platform.extensions.toAndroidLocaleList
import androidx.ui.text.platform.extensions.toJavaLocale
import androidx.ui.text.platform.style.BaselineShiftSpan
import androidx.ui.text.platform.style.FontFeatureSpan
import androidx.ui.text.platform.style.FontSpan
import androidx.ui.text.platform.style.FontWeightStyleSpan
import androidx.ui.text.platform.style.LetterSpacingSpanEm
import androidx.ui.text.platform.style.LetterSpacingSpanPx
import androidx.ui.text.platform.style.LineHeightSpan
import androidx.ui.text.platform.style.PlaceholderSpan
import androidx.ui.text.platform.style.ShadowSpan
import androidx.ui.text.platform.style.SkewXSpan
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextIndent
import androidx.ui.unit.Density
import androidx.ui.unit.TextUnit
import androidx.ui.unit.TextUnitType
import androidx.ui.unit.sp
import androidx.ui.util.fastForEach
import kotlin.math.ceil
import kotlin.math.roundToInt

private data class SpanRange(
    val span: Any,
    val start: Int,
    val end: Int,
    val priority: Int
)

// FontSpan and LetterSpacingSpanPx/LetterSpacingSpanSP has lower priority than normal spans. So
// they have negative priority.
// Meanwhile, FontSpan needs to be applied before LetterSpacing.
private const val SPAN_PRIORITY_FONT = -1
private const val SPAN_PRIORITY_LETTERSPACING = -2

@OptIn(InternalPlatformTextApi::class)
internal fun createStyledText(
    text: String,
    contextFontSize: Float,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (spanStyles.isEmpty() &&
        placeholders.isEmpty() &&
        textIndent == TextIndent.None &&
        lineHeight.isInherit
    ) {
        return text
    }

    val spannableString = SpannableString(text)

    when (lineHeight.type) {
        TextUnitType.Sp -> with(density) {
            spannableString.setSpan(
                LineHeightSpan(ceil(lineHeight.toPx()).toInt()),
                0,
                text.length
            )
        }
        TextUnitType.Em -> {
            spannableString.setSpan(
                LineHeightSpan(ceil(lineHeight.value * contextFontSize).toInt()),
                0,
                text.length
            )
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    textIndent?.let { indent ->
        if (indent.firstLine == 0.sp && indent.restLine == 0.sp) return@let
        if (indent.firstLine.isInherit || indent.restLine.isInherit) return@let
        with(density) {
            val firstLine = when (indent.firstLine.type) {
                TextUnitType.Sp -> indent.firstLine.toPx()
                TextUnitType.Em -> indent.firstLine.value * contextFontSize
                TextUnitType.Inherit -> { 0f } // do nothing
            }
            val restLine = when (indent.restLine.type) {
                TextUnitType.Sp -> indent.restLine.toPx()
                TextUnitType.Em -> indent.restLine.value * contextFontSize
                TextUnitType.Inherit -> { 0f } // do nothing
            }
            spannableString.setSpan(
                LeadingMarginSpan.Standard(
                    ceil(firstLine).toInt(),
                    ceil(restLine).toInt()
                ),
                0,
                text.length
            )
        }
    }

    val lowPrioritySpans = ArrayList<SpanRange>()

    for (spanStyle in spanStyles) {
        val start = spanStyle.start
        val end = spanStyle.end
        val style = spanStyle.item

        if (start < 0 || start >= text.length || end <= start || end > text.length) continue

        // Be aware that SuperscriptSpan needs to be applied before all other spans which
        // affect FontMetrics
        style.baselineShift?.let {
            spannableString.setSpan(BaselineShiftSpan(it.multiplier), start, end)
        }

        if (style.color.isSet) {
            spannableString.setSpan(ForegroundColorSpan(style.color.toArgb()), start, end)
        }

        style.textDecoration?.let {
            if (it.contains(TextDecoration.Underline)) {
                spannableString.setSpan(UnderlineSpan(), start, end)
            }
            if (it.contains(TextDecoration.LineThrough)) {
                spannableString.setSpan(StrikethroughSpan(), start, end)
            }
        }

        when (style.fontSize.type) {
            TextUnitType.Sp -> with(density) {
                spannableString.setSpan(
                    AbsoluteSizeSpan(style.fontSize.toPx().roundToInt(), true),
                    start,
                    end
                )
            }
            TextUnitType.Em -> {
                spannableString.setSpan(RelativeSizeSpan(style.fontSize.value), start, end)
            }
            TextUnitType.Inherit -> {} // Do nothing
        }

        style.fontFeatureSettings?.let {
            spannableString.setSpan(FontFeatureSpan(it), start, end)
        }

        if (style.fontStyle != null || style.fontWeight != null) {
            val weight = style.fontWeight?.weight ?: 0
            val fontStyle = when (style.fontStyle) {
                FontStyle.Normal -> FontWeightStyleSpan.STYLE_NORMAL
                FontStyle.Italic -> FontWeightStyleSpan.STYLE_ITALIC
                else -> FontWeightStyleSpan.STYLE_NONE
            }
            spannableString.setSpan(FontWeightStyleSpan(weight, fontStyle), start, end)
        }

        style.textGeometricTransform?.let {
            if (it.scaleX != 1.0f) {
                spannableString.setSpan(ScaleXSpan(it.scaleX), start, end)
            }
            if (it.skewX != 0f) {
                spannableString.setSpan(SkewXSpan(it.skewX), start, end)
            }
        }

        style.localeList?.let {
            spannableString.setSpan(
                if (Build.VERSION.SDK_INT >= 24) {
                    LocaleSpan(it.toAndroidLocaleList())
                } else {
                    val locale = if (it.isEmpty()) Locale.current else it[0]
                    LocaleSpan(locale.toJavaLocale())
                },
                start,
                end
            )
        }
        if (style.background.isSet) {
            spannableString.setSpan(
                BackgroundColorSpan(style.background.toArgb()),
                start,
                end
            )
        }
        style.shadow?.let {
            spannableString.setSpan(
                ShadowSpan(it.color.toArgb(), it.offset.x, it.offset.y, it.blurRadius),
                start,
                end
            )
        }

        style.fontFamily?.let {
            lowPrioritySpans.add(
                SpanRange(
                    FontSpan { weight, isItalic ->
                        createTypeface(
                            fontFamily = it,
                            weight = weight,
                            isItalic = isItalic,
                            fontSynthesis = style.fontSynthesis,
                            typefaceAdapter = typefaceAdapter
                        )
                    },
                    start,
                    end,
                    SPAN_PRIORITY_FONT
                )
            )
        }

        when (style.letterSpacing.type) {
            TextUnitType.Sp -> with(density) {
                lowPrioritySpans.add(
                    SpanRange(
                        LetterSpacingSpanPx(style.letterSpacing.toPx()),
                        start,
                        end,
                        SPAN_PRIORITY_LETTERSPACING
                    )
                )
            }
            TextUnitType.Em -> {
                lowPrioritySpans.add(
                    SpanRange(
                        LetterSpacingSpanEm(style.letterSpacing.value),
                        start,
                        end,
                        SPAN_PRIORITY_LETTERSPACING
                    )
                )
            }
            TextUnitType.Inherit -> {}
        }
    }

    lowPrioritySpans.sortBy { it.priority }
    lowPrioritySpans.fastForEach { (span, start, end) ->
        spannableString.setSpan(span, start, end)
    }

    placeholders.fastForEach {
        val (placeholder, start, end) = it
        with(placeholder) {
            spannableString.setSpan(
                PlaceholderSpan(
                    width = width.value,
                    widthUnit = width.spanUnit,
                    height = height.value,
                    heightUnit = height.spanUnit,
                    pxPerSp = density.fontScale * density.density,
                    verticalAlign = placeholderVerticalAlign.spanVerticalAlign
                ),
                start,
                end
            )
        }
    }
    return spannableString
}

private fun createTypeface(
    fontFamily: FontFamily?,
    weight: Int,
    isItalic: Boolean,
    fontSynthesis: FontSynthesis?,
    typefaceAdapter: TypefaceAdapter
): Typeface {
    val fontWeight = FontWeight(weight)
    val fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal

    return typefaceAdapter.create(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSynthesis = fontSynthesis ?: FontSynthesis.All
    )
}

/** Helper function that converts [TextUnit.type] to the unit in [PlaceholderSpan]. */
@OptIn(InternalPlatformTextApi::class)
private val TextUnit.spanUnit: Int
    get() = when (type) {
        TextUnitType.Sp -> PlaceholderSpan.UNIT_SP
        TextUnitType.Em -> PlaceholderSpan.UNIT_EM
        TextUnitType.Inherit -> PlaceholderSpan.UNIT_INHERIT
    }

/**
 * Helper function that converts [PlaceholderVerticalAlign] to the verticalAlign in
 * [PlaceholderSpan].
 */
@OptIn(InternalPlatformTextApi::class)
private val PlaceholderVerticalAlign.spanVerticalAlign: Int
    get() = when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderSpan.ALIGN_ABOVE_BASELINE
        PlaceholderVerticalAlign.Top -> PlaceholderSpan.ALIGN_TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderSpan.ALIGN_BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderSpan.ALIGN_CENTER
        PlaceholderVerticalAlign.TextTop -> PlaceholderSpan.ALIGN_TEXT_TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderSpan.ALIGN_TEXT_BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderSpan.ALIGN_TEXT_CENTER
    }

private fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}