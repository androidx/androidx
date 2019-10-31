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
import android.os.LocaleList as AndroidLocaleList
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
import androidx.ui.core.Sp
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.graphics.toArgb
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Locale
import androidx.ui.text.LocaleList
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextIndent
import kotlin.math.ceil
import kotlin.math.roundToInt
import java.util.Locale as JavaLocale

internal fun TextPaint.applyTextStyle(
    style: TextStyle,
    typefaceAdapter: TypefaceAdapter,
    density: Density
): TextStyle {
    style.fontSize?.let {
        withDensity(density) {
            textSize = it.toPx().value
        }
    }

    // fontSizeScale must be applied after fontSize is applied.
    style.fontSizeScale?.let {
        textSize *= it
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

    style.color?.let {
        color = it.toArgb()
    }

    style.letterSpacing?.let {
        letterSpacing = it.value
    }

    style.fontFeatureSettings?.let {
        fontFeatureSettings = it
    }

    style.textGeometricTransform?.scaleX?.let {
        textScaleX *= it
    }

    style.textGeometricTransform?.skewX?.let {
        textSkewX += it
    }

    style.shadow?.let {
        setShadowLayer(
            it.blurRadius.value,
            it.offset.dx,
            it.offset.dy,
            it.color.toArgb()
        )
    }

    style.decoration?.let {
        if (it.contains(TextDecoration.Underline)) {
            isUnderlineText = true
        }
        if (it.contains(TextDecoration.LineThrough)) {
            isStrikeThruText = true
        }
    }

    // baselineShift and bgColor is reset in the Android Layout constructor.
    // therefore we cannot apply them on paint, have to use spans.
    return TextStyle(
        background = style.background,
        baselineShift = style.baselineShift
    )
}

internal fun createStyledText(
    text: String,
    lineHeight: Sp?,
    textIndent: TextIndent?,
    textStyles: List<AnnotatedString.Item<TextStyle>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (textStyles.isEmpty() && textIndent == null) return text
    val spannableString = SpannableString(text)

    lineHeight?.let {
        withDensity(density) {
            spannableString.setSpan(
                LineHeightSpan(ceil(it.toPx().value).toInt()),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    textIndent?.let { indent ->
        if (indent.firstLine == 0.sp && indent.restLine == 0.sp) return@let
        withDensity(density) {
            spannableString.setSpan(
                LeadingMarginSpan.Standard(
                    indent.firstLine.toPx().value.toInt(),
                    indent.restLine.toPx().value.toInt()
                ),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    for (textStyle in textStyles) {
        val start = textStyle.start
        val end = textStyle.end
        val style = textStyle.style

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
            withDensity(density) {
                spannableString.setSpan(
                    AbsoluteSizeSpan(it.toPx().value.roundToInt(), true),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
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
            spannableString.setSpan(
                TypefaceSpan(createTypeface(style, typefaceAdapter)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

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

        style.letterSpacing?.let {
            spannableString.setSpan(
                LetterSpacingSpan(it.value),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
 * Returns true if this [TextStyle] contains any font style attributes set.
 */
private fun TextStyle.hasFontAttributes(): Boolean {
    return fontFamily != null || fontStyle != null || fontWeight != null
}

private fun createTypeface(style: TextStyle, typefaceAdapter: TypefaceAdapter): Typeface {
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