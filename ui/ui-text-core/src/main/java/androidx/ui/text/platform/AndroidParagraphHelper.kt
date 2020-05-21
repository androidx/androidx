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
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.ui.text.platform.style.BaselineShiftSpan
import androidx.ui.text.platform.style.FontFeatureSpan
import androidx.ui.text.platform.style.LetterSpacingSpanEm
import androidx.ui.text.platform.style.LetterSpacingSpanPx
import androidx.ui.text.platform.style.LineHeightSpan
import androidx.ui.text.platform.style.PlaceholderSpan
import androidx.ui.text.platform.style.ShadowSpan
import androidx.ui.text.platform.style.SkewXSpan
import androidx.ui.graphics.Color
import androidx.ui.graphics.isSet
import androidx.ui.unit.Density
import androidx.ui.unit.TextUnit
import androidx.ui.unit.TextUnitType
import androidx.ui.unit.sp
import androidx.ui.graphics.toArgb
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Locale
import androidx.ui.text.LocaleList
import androidx.ui.text.Placeholder
import androidx.ui.text.PlaceholderVerticalAlign
import androidx.ui.text.SpanStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontListFontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.platform.style.FontSpan
import androidx.ui.text.platform.style.FontWeightStyleSpan
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent
import androidx.ui.util.fastForEach
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.os.LocaleList as AndroidLocaleList
import java.util.Locale as JavaLocale

// Maximum span priority supported by android framework.
private const val SPAN_PRIORITY_MAX = 255
// Minimum span priority supported by android framework.
private const val SPAN_PRIORITY_MIN = 0
// Span priority is in the range of [0, 255]. Here we used 127 as default priority(instead of 0)
// in case some spans need  lower priority.
private const val SPAN_PRIORITY_NORMAL = 127
//  FontSpan must be applied after FontWeightStyleSpan, but before LetterSpacingSpan.
private const val SPAN_PRIORITY_FONTSPAN = 1
//  LetterSpacingSpanPx or LetterSpacingSpanEm must be applied after all other spans
//  that change fontSize and scaleX.
private const val SPAN_PRIORITY_LETTERSPACING = 0

internal fun TextPaint.applySpanStyle(
    style: SpanStyle,
    typefaceAdapter: TypefaceAdapter,
    density: Density
): SpanStyle {

    when (style.fontSize.type) {
        TextUnitType.Sp -> with(density) {
            textSize = style.fontSize.toPx().value
        }
        TextUnitType.Em -> {
            textSize *= style.fontSize.value
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    if (style.hasFontAttributes()) {
        typeface = createTypeface(style, typefaceAdapter)
    }

    style.localeList?.let {
        if (Build.VERSION.SDK_INT >= 24) {
            textLocales = it.toAndroidLocaleList()
        } else {
            val locale = if (it.isEmpty()) Locale.current else it[0]
            textLocale = locale.toJavaLocale()
        }
    }

    if (style.color.isSet) {
        color = style.color.toArgb()
    }

    when (style.letterSpacing.type) {
        TextUnitType.Em -> { letterSpacing = style.letterSpacing.value }
        TextUnitType.Sp -> {} // Sp will be handled by applying a span
        TextUnitType.Inherit -> {} // Do nothing
    }

    style.fontFeatureSettings?.let {
        fontFeatureSettings = it
    }

    style.textGeometricTransform?.let {
        textScaleX *= it.scaleX
    }

    style.textGeometricTransform?.let {
        textSkewX += it.skewX
    }

    style.shadow?.let {
        setShadowLayer(
            it.blurRadius,
            it.offset.dx,
            it.offset.dy,
            it.color.toArgb()
        )
    }

    style.textDecoration?.let {
        if (it.contains(TextDecoration.Underline)) {
            isUnderlineText = true
        }
        if (it.contains(TextDecoration.LineThrough)) {
            isStrikeThruText = true
        }
    }

    // When FontFamily is a custom font(FontListFontFamily), it needs to be applied on Paint to
    // compute empty paragraph height. Meanwhile, we also need a FontSpan for
    // FontStyle/FontWeight span to work correctly.
    // letterSpacing with unit Sp needs to be handled by span.
    // baselineShift and bgColor is reset in the Android Layout constructor,
    // therefore we cannot apply them on paint, have to use spans.
    return SpanStyle(
        fontFamily = if (style.fontFamily != null && style.fontFamily is FontListFontFamily) {
            style.fontFamily
        } else {
            null
        },
        letterSpacing = if (style.letterSpacing.type == TextUnitType.Sp &&
                    style.letterSpacing.value != 0f) {
            style.letterSpacing
        } else {
            TextUnit.Inherit
        },
        background = if (style.background == Color.Transparent) {
            Color.Unset // No need to add transparent background for default text style.
        } else {
            style.background
        },
        baselineShift = if (style.baselineShift == BaselineShift.None) {
            null
        } else {
            style.baselineShift
        }
    )
}

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
    if (spanStyles.isEmpty() && textIndent == null) return text
    val spannableString = SpannableString(text)

    when (lineHeight.type) {
        TextUnitType.Sp -> with(density) {
            spannableString.setSpanWithPriority(
                LineHeightSpan(ceil(lineHeight.toPx().value).toInt()),
                0,
                text.length,
                SPAN_PRIORITY_NORMAL
            )
        }
        TextUnitType.Em -> {
            spannableString.setSpanWithPriority(
                LineHeightSpan(ceil(lineHeight.value * contextFontSize).toInt()),
                0,
                text.length,
                SPAN_PRIORITY_NORMAL
            )
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    textIndent?.let { indent ->
        if (indent.firstLine == 0.sp && indent.restLine == 0.sp) return@let
        if (indent.firstLine.isInherit || indent.restLine.isInherit) return@let
        with(density) {
            val firstLine = when (indent.firstLine.type) {
                TextUnitType.Sp -> indent.firstLine.toPx().value
                TextUnitType.Em -> indent.firstLine.value * contextFontSize
                TextUnitType.Inherit -> { 0f } // do nothing
            }
            val restLine = when (indent.restLine.type) {
                TextUnitType.Sp -> indent.restLine.toPx().value
                TextUnitType.Em -> indent.restLine.value * contextFontSize
                TextUnitType.Inherit -> { 0f } // do nothing
            }
            spannableString.setSpanWithPriority(
                LeadingMarginSpan.Standard(
                    ceil(firstLine).toInt(),
                    ceil(restLine).toInt()
                ),
                0,
                text.length,
                SPAN_PRIORITY_NORMAL
            )
        }
    }

    for (spanStyle in spanStyles) {
        val start = spanStyle.start
        val end = spanStyle.end
        val style = spanStyle.item

        if (start < 0 || start >= text.length || end <= start || end > text.length) continue

        // Be aware that SuperscriptSpan needs to be applied before all other spans which
        // affect FontMetrics
        style.baselineShift?.let {
            spannableString.setSpanWithPriority(
                BaselineShiftSpan(it.multiplier),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }

        if (style.color.isSet) {
            spannableString.setSpanWithPriority(
                ForegroundColorSpan(style.color.toArgb()),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }

        style.textDecoration?.let {
            if (it.contains(TextDecoration.Underline)) {
                spannableString.setSpanWithPriority(
                    UnderlineSpan(),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
            if (it.contains(TextDecoration.LineThrough)) {
                spannableString.setSpanWithPriority(
                    StrikethroughSpan(),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
        }

        when (style.fontSize.type) {
            TextUnitType.Sp -> with(density) {
                spannableString.setSpanWithPriority(
                    AbsoluteSizeSpan(style.fontSize.toPx().value.roundToInt(), true),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
            TextUnitType.Em -> {
                spannableString.setSpanWithPriority(
                    RelativeSizeSpan(style.fontSize.value),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
            TextUnitType.Inherit -> {} // Do nothing
        }

        style.fontFeatureSettings?.let {
            spannableString.setSpanWithPriority(
                FontFeatureSpan(it),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }

        style.fontFamily?.let {
            spannableString.setSpanWithPriority(
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
                if (it is FontListFontFamily) {
                    SPAN_PRIORITY_FONTSPAN
                } else {
                    SPAN_PRIORITY_NORMAL
                }
            )
        }

        if (style.fontStyle != null || style.fontWeight != null) {
            val weight = style.fontWeight?.weight ?: 0
            val fontStyle = when (style.fontStyle) {
                FontStyle.Normal -> FontWeightStyleSpan.STYLE_NORMAL
                FontStyle.Italic -> FontWeightStyleSpan.STYLE_ITALIC
                else -> FontWeightStyleSpan.STYLE_NONE
            }
            spannableString.setSpanWithPriority(
                FontWeightStyleSpan(weight, fontStyle),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }

        style.textGeometricTransform?.let {
            if (it.scaleX != 1.0f) {
                spannableString.setSpanWithPriority(
                    ScaleXSpan(it.scaleX),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
        }

        style.textGeometricTransform?.let {
            if (it.skewX != 0f) {
                spannableString.setSpanWithPriority(
                    SkewXSpan(it.skewX),
                    start,
                    end,
                    SPAN_PRIORITY_NORMAL
                )
            }
        }

        when (style.letterSpacing.type) {
            TextUnitType.Sp -> with(density) {
                spannableString.setSpanWithPriority(
                    LetterSpacingSpanPx(style.letterSpacing.toPx().value),
                    start,
                    end,
                    SPAN_PRIORITY_LETTERSPACING
                )
            }
            TextUnitType.Em -> {
                spannableString.setSpanWithPriority(
                    LetterSpacingSpanEm(style.letterSpacing.value),
                    start,
                    end,
                    SPAN_PRIORITY_LETTERSPACING
                )
            }
            TextUnitType.Inherit -> {}
        }

        style.localeList?.let {
            spannableString.setSpanWithPriority(
                if (Build.VERSION.SDK_INT >= 24) {
                    LocaleSpan(it.toAndroidLocaleList())
                } else {
                    val locale = if (it.isEmpty()) Locale.current else it[0]
                    LocaleSpan(locale.toJavaLocale())
                },
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }
        if (style.background.isSet) {
            spannableString.setSpanWithPriority(
                BackgroundColorSpan(style.background.toArgb()),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }
        style.shadow?.let {
            spannableString.setSpanWithPriority(
                ShadowSpan(it.color.toArgb(), it.offset.dx, it.offset.dy, it.blurRadius),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }
    }

    placeholders.fastForEach {
        val (placeholder, start, end) = it
        with(placeholder) {
            spannableString.setSpanWithPriority(
                PlaceholderSpan(
                    width = width.value,
                    widthUnit = width.spanUnit,
                    height = height.value,
                    heightUnit = height.spanUnit,
                    pxPerSp = density.fontScale * density.density,
                    verticalAlign = placeholderVerticalAlign.spanVerticalAlign
                ),
                start,
                end,
                SPAN_PRIORITY_NORMAL
            )
        }
    }
    return spannableString
}

@VisibleForTesting
internal fun Spannable.setSpanWithPriority(span: Any, start: Int, end: Int, priority: Int) {
    require(priority >= SPAN_PRIORITY_MIN && priority <= SPAN_PRIORITY_MAX) {
        "Invalid span priority: $priority must be in the range of [0, 255]."
    }
    val flag = (priority shl Spanned.SPAN_PRIORITY_SHIFT) or Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    setSpan(span, start, end, flag)
}

/**
 * Returns true if this [SpanStyle] contains any font style attributes set.
 */
private fun SpanStyle.hasFontAttributes(): Boolean {
    return fontFamily != null || fontStyle != null || fontWeight != null
}

private fun createTypeface(style: SpanStyle, typefaceAdapter: TypefaceAdapter): Typeface {
    return typefaceAdapter.create(
        fontFamily = style.fontFamily,
        fontWeight = style.fontWeight ?: FontWeight.Normal,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontSynthesis = style.fontSynthesis ?: FontSynthesis.All
    )
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

/**
 * For a given [TextDirectionAlgorithm] return [TextLayout] constants for text direction
 * heuristics.
 */
internal fun resolveTextDirectionHeuristics(
    textDirectionAlgorithm: TextDirectionAlgorithm
): Int {
    return when (textDirectionAlgorithm) {
        TextDirectionAlgorithm.ContentOrLtr -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
        TextDirectionAlgorithm.ContentOrRtl -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
        TextDirectionAlgorithm.ForceLtr -> LayoutCompat.TEXT_DIRECTION_LTR
        TextDirectionAlgorithm.ForceRtl -> LayoutCompat.TEXT_DIRECTION_RTL
    }
}

private fun Locale.toJavaLocale(): JavaLocale = (platformLocale as AndroidLocale).javaLocale

@RequiresApi(api = 24)
private fun LocaleList.toAndroidLocaleList(): AndroidLocaleList =
    AndroidLocaleList(*map { it.toJavaLocale() }.toTypedArray())

/** Helper function that converts [TextUnit.type] to the unit in [PlaceholderSpan]. */
internal val TextUnit.spanUnit: Int
    get() = when (type) {
        TextUnitType.Sp -> PlaceholderSpan.UNIT_SP
        TextUnitType.Em -> PlaceholderSpan.UNIT_EM
        TextUnitType.Inherit -> PlaceholderSpan.UNIT_INHERIT
    }

/**
 * Helper function that converts [PlaceholderVerticalAlign] to the verticalAlign in
 * [PlaceholderSpan].
 */
internal val PlaceholderVerticalAlign.spanVerticalAlign: Int
    get() = when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderSpan.ALIGN_ABOVE_BASELINE
        PlaceholderVerticalAlign.Top -> PlaceholderSpan.ALIGN_TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderSpan.ALIGN_BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderSpan.ALIGN_CENTER
        PlaceholderVerticalAlign.TextTop -> PlaceholderSpan.ALIGN_TEXT_TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderSpan.ALIGN_TEXT_BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderSpan.ALIGN_TEXT_CENTER
    }
