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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.text.style.CharacterStyle
import androidx.compose.ui.text.AndroidComposeUiTextFlags
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.DefaultIncludeFontPadding
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.getTextDirectionHeuristic
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.extensions.setBulletSpans
import androidx.compose.ui.text.platform.extensions.setLineHeight
import androidx.compose.ui.text.platform.extensions.setPlaceholders
import androidx.compose.ui.text.platform.extensions.setSpan
import androidx.compose.ui.text.platform.extensions.setSpanStyles
import androidx.compose.ui.text.platform.extensions.setTextIndent
import androidx.compose.ui.text.resolveTextDirectionHeuristics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.isApplicable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.REPLACE_STRATEGY_ALL
import androidx.emoji2.text.EmojiCompat.REPLACE_STRATEGY_DEFAULT

@OptIn(ExperimentalTextApi::class)
@Suppress("UNCHECKED_CAST")
internal fun createCharSequence(
    text: String,
    contextFontSize: Float,
    contextTextStyle: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    userAnnotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface,
    useEmojiCompat: Boolean,
    softWrap: Boolean,
    mayHaveNewLine: Boolean, // passed to avoid recomputing the check
): CharSequence {

    val currentText =
        if (useEmojiCompat && EmojiCompat.isConfigured()) {
            val emojiSupportMatch =
                contextTextStyle.platformStyle?.paragraphStyle?.emojiSupportMatch
            val replaceStrategy =
                if (emojiSupportMatch == EmojiSupportMatch.All) {
                    REPLACE_STRATEGY_ALL
                } else {
                    REPLACE_STRATEGY_DEFAULT
                }
            EmojiCompat.get().process(text, 0, text.length, Int.MAX_VALUE, replaceStrategy)!!
        } else {
            text
        }

    if (
        annotations.isEmpty() &&
            placeholders.isEmpty() &&
            contextTextStyle.textIndent == TextIndent.None &&
            contextTextStyle.lineHeight.isUnspecified
    ) {
        return currentText
    }

    val spannableString =
        if (currentText is Spannable) {
            currentText
        } else {
            SpannableString(currentText)
        }

    // b/199939617
    // Due to a bug in the platform's native drawText stack, some CJK characters cause a bolder
    // than intended underline to be painted when TextDecoration is set to Underline.
    // If there's a CharacterStyle span that takes the entire length of the text, even if
    // it's no-op, it causes a different native call to render the text that prevents the bug.
    if (contextTextStyle.textDecoration == TextDecoration.Underline) {
        spannableString.setSpan(NoopSpan, 0, text.length)
    }

    if (
        contextTextStyle.isIncludeFontPaddingEnabled() && contextTextStyle.lineHeightStyle == null
    ) {
        // keep the existing line height behavior for includeFontPadding=true
        spannableString.setLineHeight(
            lineHeight = contextTextStyle.lineHeight,
            contextFontSize = contextFontSize,
            density = density,
        )
    } else {
        // When the single-line line height optimization is active, we avoid adding
        // LineHeightStyleSpan upfront to prevent the text from being forced into an
        // expensive StaticLayout measurement pass. Instead, the line height padding
        // will be applied manually inside Paragraph.
        //
        // We bypass this optimization and apply the span upfront if:
        // 1. Soft wrapping is enabled (may result in multiple lines).
        // 2. The text contains explicit newlines (guaranteed multi-line).
        // 3. Baseline shift is applied (forces StaticLayout anyway).
        // 4. Inline content which result in ReplacementSpans.
        // 5. Paragraph level indentation
        // 6. baseline shift require StaticLayout.
        // 7. Rtl-affecting scripts require StaticLayout.
        // 8. User-provided annotations contain metric-affecting SpanStyles (fontSize, fontFamily,
        // etc.).
        //    We check user-provided annotations (`userAnnotations`) rather than `finalSpanStyles`
        // (`annotations`),
        //    because `finalSpanStyles` includes internal fallback spans generated by Compose (such
        // as `notAppliedStyle`
        //    for `LetterSpacingSpanPx`, which works around Android's native TextLine tracking reset
        // bug across span
        //    boundaries). Internal fallback spans match base TextPaint values and do not alter
        // vertical font metrics
        //    or layout width under BoringLayout, so they do not require StaticLayout.
        // 9. IncludeFontPadding is enabled.
        if (
            !AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled ||
                softWrap ||
                mayHaveNewLine ||
                placeholders.isNotEmpty() ||
                (contextTextStyle.textIndent ?: TextIndent.None) != TextIndent.None ||
                contextTextStyle.baselineShift?.isApplicable == true ||
                spannableString.couldAffectRtl {
                    val textDirInt =
                        resolveTextDirectionHeuristics(
                            contextTextStyle.textDirection,
                            contextTextStyle.localeList,
                        )
                    getTextDirectionHeuristic(textDirInt)
                } ||
                userAnnotations.hasMetricAffectingSpanStyle() ||
                contextTextStyle.isIncludeFontPaddingEnabled()
        ) {
            val lineHeightStyle = contextTextStyle.lineHeightStyle ?: LineHeightStyle.Default
            spannableString.setLineHeight(
                lineHeight = contextTextStyle.lineHeight,
                lineHeightStyle = lineHeightStyle,
                contextFontSize = contextFontSize,
                density = density,
            )
        }
    }

    spannableString.setTextIndent(contextTextStyle.textIndent, contextFontSize, density)

    spannableString.setSpanStyles(contextTextStyle, annotations, density, resolveTypeface)

    // apply this after setTextIndent so we have space to draw the bullets. Bullets by itself don't
    // add any paddings
    spannableString.setBulletSpans(
        annotations,
        contextFontSize,
        density,
        contextTextStyle.textIndent,
    )

    spannableString.setPlaceholders(placeholders, density)

    return spannableString
}

internal fun TextStyle.isIncludeFontPaddingEnabled(): Boolean {
    return platformStyle?.paragraphStyle?.includeFontPadding ?: DefaultIncludeFontPadding
}

private val NoopSpan =
    object : CharacterStyle() {
        override fun updateDrawState(p0: TextPaint?) {}
    }

private const val MaxSingleLineLengthThreshold = 512

private fun Char.couldAffectRtl(): Boolean {
    return (this in
        '\u0590'..'\u08FF') || // RTL scripts (Hebrew, Arabic, Syriac, Thaana, Mandaic, etc.)
        this == '\u200E' || // LRM (Left-To-Right Mark)
        this == '\u200F' || // RLM (Right-To-Left Mark)
        (this in
            '\u202A'..'\u202E') || // BiDi embedding/override controls (LRE, RLE, PDF, LRO, RLO)
        (this in '\u2066'..'\u2069') || // BiDi isolate controls (LRI, RLI, FSI, PDI)
        (this in '\uD800'..'\uDFFF') || // High/Low Surrogates (Emojis, SMP RTL scripts)
        (this in '\uFB1D'..'\uFDFF') || // Hebrew and Arabic presentation forms A
        (this in '\uFE70'..'\uFEFE') // Arabic presentation forms B
}

private fun CharSequence.couldAffectRtl(textDirProvider: () -> TextDirectionHeuristic): Boolean {
    val limit = minOf(length, MaxSingleLineLengthThreshold)
    for (i in 0 until limit) {
        if (this[i].couldAffectRtl()) return true
    }
    if (length > MaxSingleLineLengthThreshold) return true
    return textDirProvider().isRtl(this, 0, length)
}

private fun SpanStyle.hasMetricAffectingSpanStyle(): Boolean {
    return fontSize.isSpecified ||
        fontFamily != null ||
        fontWeight != null ||
        fontStyle != null ||
        fontSynthesis != null ||
        fontFeatureSettings != null ||
        baselineShift?.isApplicable == true ||
        letterSpacing.isSpecified ||
        textGeometricTransform != null ||
        localeList != null
}

internal fun List<AnnotatedString.Range<*>>.hasMetricAffectingSpanStyle(): Boolean {
    for (i in indices) {
        val item = this[i].item
        if (item is SpanStyle && item.hasMetricAffectingSpanStyle()) {
            return true
        }
        if (item is LinkAnnotation) {
            val styles = item.styles
            if (styles != null) {
                if (
                    styles.style?.hasMetricAffectingSpanStyle() == true ||
                        styles.focusedStyle?.hasMetricAffectingSpanStyle() == true ||
                        styles.hoveredStyle?.hasMetricAffectingSpanStyle() == true ||
                        styles.pressedStyle?.hasMetricAffectingSpanStyle() == true
                ) {
                    return true
                }
            }
        }
    }
    return false
}
