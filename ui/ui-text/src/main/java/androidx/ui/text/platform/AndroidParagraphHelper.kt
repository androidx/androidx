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
import androidx.text.LayoutCompat
import androidx.text.TextLayout
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.LineHeightSpan
import androidx.text.style.ShadowSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.ui.core.Density
import androidx.ui.core.TextUnit
import androidx.ui.core.TextUnitType
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.graphics.toArgb
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Locale
import androidx.ui.text.LocaleList
import androidx.ui.text.SpanStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.os.LocaleList as AndroidLocaleList
import java.util.Locale as JavaLocale

internal fun TextPaint.applySpanStyle(
    spanStyle: SpanStyle,
    typefaceAdapter: TypefaceAdapter,
    density: Density
): SpanStyle {

    when (spanStyle.fontSize.type) {
        TextUnitType.Sp -> withDensity(density) {
            textSize = spanStyle.fontSize.toPx().value
        }
        TextUnitType.Em -> {
            textSize *= spanStyle.fontSize.value
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    if (spanStyle.hasFontAttributes()) {
        typeface = createTypeface(spanStyle, typefaceAdapter)
    }

    spanStyle.localeList?.let {
        if (Build.VERSION.SDK_INT >= 24) {
            textLocales = it.toAndroidLocaleList()
        } else {
            val locale = if (it.isEmpty()) Locale.current else it[0]
            textLocale = locale.toJavaLocale()
        }
    }

    spanStyle.color?.let {
        color = it.toArgb()
    }

    when (spanStyle.letterSpacing.type) {
        TextUnitType.Sp -> withDensity(density) {
            // Platform accept EM as a letter space. Convert Sp to Em
            letterSpacing = spanStyle.letterSpacing.toPx().value / textSize
        }
        TextUnitType.Em -> {
            letterSpacing = spanStyle.letterSpacing.value
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    spanStyle.fontFeatureSettings?.let {
        fontFeatureSettings = it
    }

    spanStyle.textGeometricTransform?.let {
        textScaleX *= it.scaleX
    }

    spanStyle.textGeometricTransform?.let {
        textSkewX += it.skewX
    }

    spanStyle.shadow?.let {
        setShadowLayer(
            it.blurRadius.value,
            it.offset.dx,
            it.offset.dy,
            it.color.toArgb()
        )
    }

    spanStyle.decoration?.let {
        if (it.contains(TextDecoration.Underline)) {
            isUnderlineText = true
        }
        if (it.contains(TextDecoration.LineThrough)) {
            isStrikeThruText = true
        }
    }

    // baselineShift and bgColor is reset in the Android Layout constructor.
    // therefore we cannot apply them on paint, have to use spans.
    return SpanStyle(
        background = spanStyle.background,
        baselineShift = spanStyle.baselineShift
    )
}

internal fun createStyledText(
    text: String,
    contextFontSize: Float,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    spanStyles: List<AnnotatedString.Item<SpanStyle>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (spanStyles.isEmpty() && textIndent == null) return text
    val spannableString = SpannableString(text)

    when (lineHeight.type) {
        TextUnitType.Sp -> withDensity(density) {
            spannableString.setSpan(
                LineHeightSpan(ceil(lineHeight.toPx().value).toInt()),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        TextUnitType.Em -> {
            spannableString.setSpan(
                LineHeightSpan(ceil(lineHeight.value * contextFontSize).toInt()),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        TextUnitType.Inherit -> {} // Do nothing
    }

    textIndent?.let { indent ->
        if (indent.firstLine == 0.sp && indent.restLine == 0.sp) return@let
        if (indent.firstLine.isInherit || indent.restLine.isInherit) return@let
        withDensity(density) {
            val firstLine = when (indent.firstLine.type) {
                TextUnitType.Sp -> indent.firstLine.toPx()
                TextUnitType.Em -> {
                    // Support indents with Em unit type: b/144958549
                    0.px
                }
                TextUnitType.Inherit -> { 0.px } // do nothing
            }
            val restLine = when (indent.restLine.type) {
                TextUnitType.Sp -> indent.restLine.toPx()
                TextUnitType.Em -> {
                    // Support indents with Em unit type: b/144958549
                    0.px
                }
                TextUnitType.Inherit -> { 0.px } // do nothing
            }
            spannableString.setSpan(
                LeadingMarginSpan.Standard(
                    firstLine.value.toInt(),
                    restLine.value.toInt()
                ),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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

        when (style.fontSize.type) {
            TextUnitType.Sp -> withDensity(density) {
                spannableString.setSpan(
                    AbsoluteSizeSpan(style.fontSize.toPx().value.roundToInt(), true),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextUnitType.Em -> {
                spannableString.setSpan(
                    RelativeSizeSpan(style.fontSize.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextUnitType.Inherit -> {} // Do nothing
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
            spannableString.setSpan(
                TypefaceSpan(createTypeface(style, typefaceAdapter)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        style.textGeometricTransform?.let {
            if (it.scaleX != 1.0f) {
                spannableString.setSpan(
                    ScaleXSpan(it.scaleX),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        style.textGeometricTransform?.let {
            if (it.skewX != 0f) {
                spannableString.setSpan(
                    SkewXSpan(it.skewX),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        when (style.letterSpacing.type) {
            TextUnitType.Sp -> {
                // Support LetterSpacing with SP unit: b/144957997
            }
            TextUnitType.Em -> {
                spannableString.setSpan(
                    LetterSpacingSpan(style.letterSpacing.value),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextUnitType.Inherit -> {}
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
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        style.background?.let {
            spannableString.setSpan(
                BackgroundColorSpan(it.toArgb()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
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
