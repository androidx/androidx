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

import androidx.annotation.VisibleForTesting
import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.core.LayoutDirection
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.graphics.useOrElse
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
import androidx.ui.unit.sp

/** The default font size if none is specified. */
private val DefaultFontSize = 14.sp
private val DefaultLetterSpacing = 0.sp
private val DefaultBackgroundColor = Color.Transparent
// TODO(nona): Introduce TextUnit.Original for representing "do not change the original result".
//  Need to distinguish from Inherit.
private val DefaultLineHeight = TextUnit.Inherit
private val DefaultColor = Color.Black

/**
 * Styling configuration for a `Text`.
 *
 * @sample androidx.ui.text.samples.TextStyleSample
 *
 * @param color The text color.
 * @param fontSize The size of glyphs to use when painting the text. This
 * may be [TextUnit.Inherit] for inheriting from another [TextStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *  style cannot be found in the provided custom font family.
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontFeatureSettings The advanced typography settings provided by font. The format is the
 *  same as the CSS font-feature-settings attribute:
 *  https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
 * @param letterSpacing The amount of space to add between each letter.
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
    val color: Color = Color.Unset,
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
    val background: Color = Color.Unset,
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
            check(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    @Stable
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

    @Stable
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
    @Stable
    fun merge(other: TextStyle? = null): TextStyle {
        if (other == null || other == Default) return this
        return TextStyle(
            spanStyle = toSpanStyle().merge(other.toSpanStyle()),
            paragraphStyle = toParagraphStyle().merge(other.toParagraphStyle())
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: SpanStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle().merge(other),
            paragraphStyle = toParagraphStyle()
        )
    }

    /**
     * Returns a new text style that is a combination of this style and the given [other] style.
     *
     * @see merge
     */
    @Stable
    fun merge(other: ParagraphStyle): TextStyle {
        return TextStyle(
            spanStyle = toSpanStyle(),
            paragraphStyle = toParagraphStyle().merge(other)
        )
    }

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: TextStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: ParagraphStyle): TextStyle = this.merge(other)

    /**
     * Plus operator overload that applies a [merge].
     */
    @Stable
    operator fun plus(other: SpanStyle): TextStyle = this.merge(other)

    companion object {
        /**
         * Constant for default text style.
         */
        @Stable
        val Default = TextStyle()
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

/**
 * Fills missing values in TextStyle with default values and resolve TextDirectionAlgorithm.
 *
 * This function will fill all null or [TextUnit.Inherit] field with actual values.
 * @param style a text style to be resolved
 * @param direction a layout direction to be used for resolving text layout direction algorithm
 * @return resolved text style.
 */
fun resolveDefaults(style: TextStyle, direction: LayoutDirection) = TextStyle(
    color = style.color.useOrElse { DefaultColor },
    fontSize = if (style.fontSize == TextUnit.Inherit) DefaultFontSize else style.fontSize,
    fontWeight = style.fontWeight ?: FontWeight.Normal,
    fontStyle = style.fontStyle ?: FontStyle.Normal,
    fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
    fontFamily = style.fontFamily ?: FontFamily.Default,
    fontFeatureSettings = style.fontFeatureSettings ?: "",
    letterSpacing = if (style.letterSpacing.isInherit) {
        DefaultLetterSpacing
    } else {
        style.letterSpacing
    },
    baselineShift = style.baselineShift ?: BaselineShift.None,
    textGeometricTransform = style.textGeometricTransform ?: TextGeometricTransform.None,
    localeList = style.localeList ?: LocaleList.current,
    background = style.background.useOrElse { DefaultBackgroundColor },
    textDecoration = style.textDecoration ?: TextDecoration.None,
    shadow = style.shadow ?: Shadow.None,
    textAlign = style.textAlign ?: TextAlign.Start,
    textDirectionAlgorithm = resolveTextDirectionAlgorithm(direction, style.textDirectionAlgorithm),
    lineHeight = if (style.lineHeight.isInherit) DefaultLineHeight else style.lineHeight,
    textIndent = style.textIndent ?: TextIndent.None
)

/**
 * If [textDirectionAlgorithm] is null returns a [TextDirectionAlgorithm] based on
 * [layoutDirection].
 */
@VisibleForTesting
internal fun resolveTextDirectionAlgorithm(
    layoutDirection: LayoutDirection,
    textDirectionAlgorithm: TextDirectionAlgorithm?
): TextDirectionAlgorithm {
    return textDirectionAlgorithm
        ?: when (layoutDirection) {
            LayoutDirection.Ltr -> TextDirectionAlgorithm.ContentOrLtr
            LayoutDirection.Rtl -> TextDirectionAlgorithm.ContentOrRtl
        }
}