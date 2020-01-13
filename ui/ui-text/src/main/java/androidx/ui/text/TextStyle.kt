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

package androidx.ui.text

import androidx.compose.Immutable
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextGeometricTransform
import androidx.ui.text.style.TextIndent
import androidx.ui.unit.TextUnit

/**
 * Styling configuration for a `Text`.
 *
 * @sample androidx.ui.text.samples.TextStyleSample
 *
 * @param color The text color.
 * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
 * may be [TextUnit.Inherit] for inheriting from another [TextStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *  style cannot be found in the provided custom font family.
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontFeatureSettings The advanced typography settings provided by font. The format is the
 *  same as the CSS font-feature-settings attribute:
 *  https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * @param letterSpacing The amount of space (in em) to add between each letter.
 * @param baselineShift The amount by which the text is shifted up from the current baseline.
 * @param textGeometricTransform The geometric transformation applied the text.
 * @param localeList The locale list used to select region-specific glyphs.
 * @param background The background color for the text.
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * @param shadow The shadow effect applied on the text.
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param textDirectionAlgorithm The algorithm to be used to resolve the final text and paragraph
 * direction: Left To Right or Right To Left.
 * @param textIndent The indentation of the paragraph.
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 *
 * @see AnnotatedString
 * @see SpanStyle
 * @see ParagraphStyle
 */
@Immutable
data class TextStyle(
    val color: Color? = null,
    val fontSize: TextUnit = TextUnit.Inherit,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontSynthesis: FontSynthesis? = null,
    val fontFamily: FontFamily? = null,
    val fontFeatureSettings: String? = null,
    val letterSpacing: TextUnit = TextUnit.Inherit,
    val baselineShift: BaselineShift? = null,
    val textGeometricTransform: TextGeometricTransform? = null,
    val localeList: LocaleList? = null,
    val background: Color? = null,
    val textDecoration: TextDecoration? = null,
    val shadow: Shadow? = null,
    val textAlign: TextAlign? = null,
    val textDirectionAlgorithm: TextDirectionAlgorithm? = null,
    val lineHeight: TextUnit = TextUnit.Inherit,
    val textIndent: TextIndent? = null
) {
    internal constructor(spanStyle: SpanStyle, paragraphStyle: ParagraphStyle) : this (
        color = spanStyle.color,
        fontSize = spanStyle.fontSize,
        fontWeight = spanStyle.fontWeight,
        fontStyle = spanStyle.fontStyle,
        fontSynthesis = spanStyle.fontSynthesis,
        fontFamily = spanStyle.fontFamily,
        fontFeatureSettings = spanStyle.fontFeatureSettings,
        letterSpacing = spanStyle.letterSpacing,
        baselineShift = spanStyle.baselineShift,
        textGeometricTransform = spanStyle.textGeometricTransform,
        localeList = spanStyle.localeList,
        background = spanStyle.background,
        textDecoration = spanStyle.textDecoration,
        shadow = spanStyle.shadow,
        textAlign = paragraphStyle.textAlign,
        textDirectionAlgorithm = paragraphStyle.textDirectionAlgorithm,
        lineHeight = paragraphStyle.lineHeight,
        textIndent = paragraphStyle.textIndent
    )

    init {
        if (lineHeight != TextUnit.Inherit) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            assert(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    fun toSpanStyle(): SpanStyle = SpanStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSynthesis = fontSynthesis,
        fontFamily = fontFamily,
        fontFeatureSettings = fontFeatureSettings,
        letterSpacing = letterSpacing,
        baselineShift = baselineShift,
        textGeometricTransform = textGeometricTransform,
        localeList = localeList,
        background = background,
        textDecoration = textDecoration,
        shadow = shadow
    )

    fun toParagraphStyle(): ParagraphStyle = ParagraphStyle(
        textAlign = textAlign,
        textDirectionAlgorithm = textDirectionAlgorithm,
        lineHeight = lineHeight,
        textIndent = textIndent
    )
    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * [other] text style's null or inherit properties are replaced with the non-null properties of
     * this text style. Another way to think of it is that the "missing" properties of the [other]
     * style are _filled_ by the properties of this style.
     *
     * If the given text style is null, returns this text style.
     */
    fun merge(other: TextStyle? = null): TextStyle {
        if (other == null) return this
        return TextStyle(
            spanStyle = toSpanStyle().merge(other.toSpanStyle()),
            paragraphStyle = toParagraphStyle().merge(other.toParagraphStyle())
        )
    }

    fun merge(other: SpanStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle().merge(other),
            paragraphStyle = toParagraphStyle()
        )
    }

    fun merge(other: ParagraphStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle(),
            paragraphStyle = toParagraphStyle().merge(other)
        )
    }
}

/**
 * Interpolate between two text styles.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
fun lerp(start: TextStyle, stop: TextStyle, fraction: Float): TextStyle {
    return TextStyle(
        spanStyle = lerp(start.toSpanStyle(), stop.toSpanStyle(), fraction),
        paragraphStyle = lerp(start.toParagraphStyle(), stop.toParagraphStyle(), fraction)
    )
}