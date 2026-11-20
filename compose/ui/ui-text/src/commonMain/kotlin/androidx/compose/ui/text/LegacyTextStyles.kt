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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.text.internal.checkPrecondition
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.isApplicable
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.text.style.takeOrElse
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * Fallback default line height used by [resolveLegacyParagraphStyleDefaults] when
 * [LegacyParagraphStyle.lineHeight] is [TextUnit.Unspecified].
 */
private val LegacyDefaultLineHeight = TextUnit.Unspecified

/**
 * Fallback default font size (`14.sp`) used by [resolveLegacySpanStyleDefaults] when
 * [LegacySpanStyle.fontSize] is [TextUnit.Unspecified].
 */
private val LegacyDefaultFontSize = 14.sp

/**
 * Fallback default letter spacing (`0.sp`) used by [resolveLegacySpanStyleDefaults] when
 * [LegacySpanStyle.letterSpacing] is [TextUnit.Unspecified].
 */
private val LegacyDefaultLetterSpacing = 0.sp

/**
 * Fallback default background color ([Color.Transparent]) used by [resolveLegacySpanStyleDefaults]
 * when [LegacySpanStyle.background] is [Color.Unspecified].
 */
private val LegacyDefaultBackgroundColor = Color.Transparent

/**
 * Fallback default text color ([Color.Black]) used by [resolveLegacySpanStyleDefaults] when
 * [LegacySpanStyle.textForegroundStyle] is [TextForegroundStyle.Unspecified].
 */
private val LegacyDefaultColor = Color.Black

/**
 * Pre-allocated [TextForegroundStyle] wrapping [LegacyDefaultColor] for
 * [resolveLegacySpanStyleDefaults].
 */
private val LegacyDefaultColorForegroundStyle = TextForegroundStyle.from(LegacyDefaultColor)

/**
 * Shared empty [LegacyParagraphStyle] instance with all fields set to their unspecified defaults.
 */
internal val DefaultLegacyParagraphStyle = LegacyParagraphStyle()

/** Shared empty [LegacySpanStyle] instance with all fields set to their unspecified defaults. */
internal val DefaultLegacySpanStyle = LegacySpanStyle()

/**
 * Legacy field-per-property representation of [ParagraphStyle] retained as the kill-switch fallback
 * when [ComposeUiTextFlags.isPackedTextStylesEnabled] (`PackedTextStylesLatch.isEnabled`) is
 * `false`.
 *
 * When the packed text styles flag is enabled, [ParagraphStyle] packs its scalar fields into a
 * single 64-bit `Long` (`bits`) and leaves `ParagraphStyle.legacyParagraphStyle` as `null`. When
 * the flag is disabled, [ParagraphStyle] allocates an instance of [LegacyParagraphStyle] and
 * delegates every property read, [merge], [fastMerge], [copy], [lerp],
 * [resolveParagraphStyleDefaults], [equals], [hashCode], and [toString] call to this class so
 * runtime behavior matches the pre-packing implementation bit-for-bit.
 *
 * @property textAlign The alignment of the text within the lines of the paragraph, or
 *   [TextAlign.Unspecified] if unset.
 * @property textDirection The algorithm to be used to resolve the text direction (`Ltr` or `Rtl`),
 *   or [TextDirection.Unspecified] if unset.
 * @property lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM, or
 *   [TextUnit.Unspecified] if unset.
 * @property textIndent The indentation of the paragraph, or `null` if unset.
 * @property platformStyle Platform specific [ParagraphStyle] parameters, or `null` if unset.
 * @property lineHeightStyle The configuration for line height such as alignment of the line in the
 *   provided line height and whether to apply trimming, or `null` if unset.
 * @property lineBreak The line breaking configuration for the paragraph, or [LineBreak.Unspecified]
 *   if unset.
 * @property hyphens The configuration of hyphenation, or [Hyphens.Unspecified] if unset.
 * @property textMotion Text character placement, whether to optimize for static or animated text,
 *   or `null` if unset.
 */
@Immutable
internal class LegacyParagraphStyle(
    /** The alignment of the text within the lines of the paragraph. */
    val textAlign: TextAlign = TextAlign.Unspecified,
    /** The algorithm used to resolve the paragraph text direction. */
    val textDirection: TextDirection = TextDirection.Unspecified,
    /** The line height of the paragraph in SP or EM units. */
    val lineHeight: TextUnit = TextUnit.Unspecified,
    /** The first-line and rest-line indentation of the paragraph. */
    val textIndent: TextIndent? = null,
    /** Platform-specific paragraph configuration. */
    val platformStyle: PlatformParagraphStyle? = null,
    /** Line height alignment, trim, and mode configuration. */
    val lineHeightStyle: LineHeightStyle? = null,
    /** Line breaking strategy, strictness, and word break configuration. */
    val lineBreak: LineBreak = LineBreak.Unspecified,
    /** Automatic hyphenation configuration for the paragraph. */
    val hyphens: Hyphens = Hyphens.Unspecified,
    /** Static or animated glyph placement configuration. */
    val textMotion: TextMotion? = null,
) {
    init {
        if (lineHeight != TextUnit.Unspecified) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            checkPrecondition(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    /**
     * Returns a new [LegacyParagraphStyle] that is a combination of this style and the given
     * [other] style.
     *
     * If the given paragraph style is `null`, returns this paragraph style unchanged. Otherwise,
     * specified fields in [other] override the corresponding fields in `this`.
     */
    @Stable
    fun merge(other: LegacyParagraphStyle? = null): LegacyParagraphStyle {
        if (other == null) return this
        return fastMerge(
            textAlign = other.textAlign,
            textDirection = other.textDirection,
            lineHeight = other.lineHeight,
            textIndent = other.textIndent,
            platformStyle = other.platformStyle,
            lineHeightStyle = other.lineHeightStyle,
            lineBreak = other.lineBreak,
            hyphens = other.hyphens,
            textMotion = other.textMotion,
        )
    }

    /** Returns a copy of this [LegacyParagraphStyle] with optionally overridden fields. */
    fun copy(
        textAlign: TextAlign = this.textAlign,
        textDirection: TextDirection = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak = this.lineBreak,
        hyphens: Hyphens = this.hyphens,
        textMotion: TextMotion? = this.textMotion,
    ): LegacyParagraphStyle {
        return LegacyParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion,
        )
    }

    /**
     * Compares all nine paragraph style fields one by one to match the legacy equality contract.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LegacyParagraphStyle) return false

        if (textAlign != other.textAlign) return false
        if (textDirection != other.textDirection) return false
        if (lineHeight != other.lineHeight) return false
        if (textIndent != other.textIndent) return false
        if (platformStyle != other.platformStyle) return false
        if (lineHeightStyle != other.lineHeightStyle) return false
        if (lineBreak != other.lineBreak) return false
        if (hyphens != other.hyphens) return false
        if (textMotion != other.textMotion) return false

        return true
    }

    /**
     * Computes the legacy polynomial `31 * result + field.hashCode()` hash across all nine fields.
     */
    override fun hashCode(): Int {
        var result = textAlign.hashCode()
        result = 31 * result + textDirection.hashCode()
        result = 31 * result + lineHeight.hashCode()
        result = 31 * result + (textIndent?.hashCode() ?: 0)
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (lineHeightStyle?.hashCode() ?: 0)
        result = 31 * result + lineBreak.hashCode()
        result = 31 * result + hyphens.hashCode()
        result = 31 * result + (textMotion?.hashCode() ?: 0)
        return result
    }

    /** Formats this fallback object using the public `ParagraphStyle(...)` string format. */
    override fun toString(): String {
        return "ParagraphStyle(" +
            "textAlign=$textAlign, " +
            "textDirection=$textDirection, " +
            "lineHeight=$lineHeight, " +
            "textIndent=$textIndent, " +
            "platformStyle=$platformStyle, " +
            "lineHeightStyle=$lineHeightStyle, " +
            "lineBreak=$lineBreak, " +
            "hyphens=$hyphens, " +
            "textMotion=$textMotion" +
            ")"
    }
}

/**
 * Legacy field-by-field linear interpolation between two [LegacyParagraphStyle] instances at
 * [fraction], used when [ComposeUiTextFlags.isPackedTextStylesEnabled] is `false`.
 */
internal fun lerp(
    start: LegacyParagraphStyle,
    stop: LegacyParagraphStyle,
    fraction: Float,
): LegacyParagraphStyle {
    return LegacyParagraphStyle(
        textAlign = lerpDiscrete(start.textAlign, stop.textAlign, fraction),
        textDirection = lerpDiscrete(start.textDirection, stop.textDirection, fraction),
        lineHeight = lerpTextUnitInheritable(start.lineHeight, stop.lineHeight, fraction),
        textIndent =
            lerp(start.textIndent ?: TextIndent.None, stop.textIndent ?: TextIndent.None, fraction),
        platformStyle = lerpLegacyPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        lineHeightStyle = lerpDiscrete(start.lineHeightStyle, stop.lineHeightStyle, fraction),
        lineBreak = lerpDiscrete(start.lineBreak, stop.lineBreak, fraction),
        hyphens = lerpDiscrete(start.hyphens, stop.hyphens, fraction),
        textMotion = lerpDiscrete(start.textMotion, stop.textMotion, fraction),
    )
}

/**
 * Interpolates two nullable [PlatformParagraphStyle] instances for [lerp], substituting
 * [PlatformParagraphStyle.Default] when only one side is non-null.
 */
private fun lerpLegacyPlatformStyle(
    start: PlatformParagraphStyle?,
    stop: PlatformParagraphStyle?,
    fraction: Float,
): PlatformParagraphStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformParagraphStyle.Default
    val stopNonNull = stop ?: PlatformParagraphStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}

/**
 * Resolves unspecified fields in [style] to their concrete defaults for the given layout
 * [direction] when [ComposeUiTextFlags.isPackedTextStylesEnabled] is `false`.
 */
internal fun resolveLegacyParagraphStyleDefaults(
    style: LegacyParagraphStyle,
    direction: LayoutDirection,
) =
    LegacyParagraphStyle(
        textAlign =
            if (style.textAlign == TextAlign.Unspecified) TextAlign.Start else style.textAlign,
        textDirection = resolveTextDirection(direction, style.textDirection),
        lineHeight =
            if (style.lineHeight.isUnspecified) LegacyDefaultLineHeight else style.lineHeight,
        textIndent = style.textIndent ?: TextIndent.None,
        platformStyle = style.platformStyle,
        lineHeightStyle = style.lineHeightStyle,
        lineBreak =
            if (style.lineBreak == LineBreak.Unspecified) LineBreak.Simple else style.lineBreak,
        hyphens = if (style.hyphens == Hyphens.Unspecified) Hyphens.None else style.hyphens,
        textMotion = style.textMotion ?: TextMotion.Static,
    )

/**
 * Merges individual paragraph style attributes into `this` [LegacyParagraphStyle], returning `this`
 * without allocating when no specified parameter differs from the existing value.
 */
internal fun LegacyParagraphStyle.fastMerge(
    textAlign: TextAlign,
    textDirection: TextDirection,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    lineHeightStyle: LineHeightStyle?,
    lineBreak: LineBreak,
    hyphens: Hyphens,
    textMotion: TextMotion?,
): LegacyParagraphStyle {
    // prioritize the parameters to Text in diffs here
    /** textAlign: TextAlign? lineHeight: TextUnit */
    // any new vals should do a pre-merge check here
    val requiresAlloc =
        textAlign != TextAlign.Unspecified && textAlign != this.textAlign ||
            lineHeight.isSpecified && lineHeight != this.lineHeight ||
            textIndent != null && textIndent != this.textIndent ||
            textDirection != TextDirection.Unspecified && textDirection != this.textDirection ||
            platformStyle != null && platformStyle != this.platformStyle ||
            lineHeightStyle != null && lineHeightStyle != this.lineHeightStyle ||
            lineBreak != LineBreak.Unspecified && lineBreak != this.lineBreak ||
            hyphens != Hyphens.Unspecified && hyphens != this.hyphens ||
            textMotion != null && textMotion != this.textMotion

    if (!requiresAlloc) {
        // we're done
        return this
    }

    return LegacyParagraphStyle(
        lineHeight =
            if (lineHeight.isUnspecified) {
                this.lineHeight
            } else {
                lineHeight
            },
        textIndent = textIndent ?: this.textIndent,
        textAlign = if (textAlign != TextAlign.Unspecified) textAlign else this.textAlign,
        textDirection =
            if (textDirection != TextDirection.Unspecified) textDirection else this.textDirection,
        platformStyle = mergePlatformStyle(platformStyle),
        lineHeightStyle = lineHeightStyle ?: this.lineHeightStyle,
        lineBreak = if (lineBreak != LineBreak.Unspecified) lineBreak else this.lineBreak,
        hyphens = if (hyphens != Hyphens.Unspecified) hyphens else this.hyphens,
        textMotion = textMotion ?: this.textMotion,
    )
}

/**
 * Merges [other] into [LegacyParagraphStyle.platformStyle], returning whichever side is non-null or
 * delegating to [PlatformParagraphStyle.merge] when both are present.
 */
private fun LegacyParagraphStyle.mergePlatformStyle(
    other: PlatformParagraphStyle?
): PlatformParagraphStyle? {
    if (platformStyle == null) return other
    if (other == null) return platformStyle
    return platformStyle.merge(other)
}

/**
 * Legacy field-per-property representation of [SpanStyle] retained as the kill-switch fallback when
 * [ComposeUiTextFlags.isPackedTextStylesEnabled] (`PackedTextStylesLatch.isEnabled`) is `false`.
 *
 * When the packed text styles flag is enabled, [SpanStyle] stores its foreground color in
 * `colorXor`, packs its enum/flag fields into a 32-bit `bits` word, moves cold properties into an
 * optional `SpanStyleExtras` sidecar, and leaves `SpanStyle.legacySpanStyle` as `null`. When the
 * flag is disabled, [SpanStyle] allocates an instance of [LegacySpanStyle] backed by
 * [TextForegroundStyle] and delegates every property read, [merge], [fastMerge], [copy], [lerp],
 * [resolveSpanStyleDefaults], [equals], [hasSameLayoutAffectingAttributes],
 * [hasSameNonLayoutAttributes], [hashCode], [hashCodeLayoutAffectingAttributes], and [toString]
 * call to this class so runtime behavior matches the pre-packing implementation bit-for-bit.
 *
 * @property textForegroundStyle The legacy [TextForegroundStyle] wrapper holding [color], [brush],
 *   and [alpha].
 * @property fontSize The size of glyphs (in logical pixels or EM) to use when painting the text, or
 *   [TextUnit.Unspecified] if unset.
 * @property fontWeight The typeface thickness to use when painting the text (e.g.,
 *   [FontWeight.Bold]), or `null` if unset.
 * @property fontStyle The typeface variant to use when drawing the letters (e.g.,
 *   [FontStyle.Italic]), or `null` if unset.
 * @property fontSynthesis Whether to synthesize font weight and/or style when the font family does
 *   not provide bold or italic glyphs natively, or `null` if unset.
 * @property fontFamily The font family to be used when rendering the text, or `null` if unset.
 * @property fontFeatureSettings OpenType font feature settings in CSS format, or `null` if unset.
 * @property letterSpacing The amount of space (in SP or EM) to add between each letter, or
 *   [TextUnit.Unspecified] if unset.
 * @property baselineShift The amount by which the text is shifted up or down from the baseline, or
 *   `null` if unset.
 * @property textGeometricTransform The geometric transformation (`scaleX`, `skewX`) applied to the
 *   text, or `null` if unset.
 * @property localeList The locale list used to select region-specific glyphs, or `null` if unset.
 * @property background The background color for the text span, or [Color.Unspecified] if unset.
 * @property textDecoration The decorations to paint on the text (e.g., underline or strikethrough),
 *   or `null` if unset.
 * @property shadow The shadow effect applied on the text span, or `null` if unset.
 * @property platformStyle Platform-specific [SpanStyle] parameters, or `null` if unset.
 * @property drawStyle Drawing style (e.g., [Fill] or `Stroke`) for the text span, or `null` if
 *   unset.
 */
@Immutable
internal class LegacySpanStyle
internal constructor(
    // The fill to draw text, a unified representation of Color and Brush.
    /** The legacy foreground style wrapper storing color, brush, and alpha. */
    internal val textForegroundStyle: TextForegroundStyle,
    /** The glyph size in SP or EM units, or [TextUnit.Unspecified] if unset. */
    val fontSize: TextUnit = TextUnit.Unspecified,
    /** The typeface weight, or `null` if unset. */
    val fontWeight: FontWeight? = null,
    /** The typeface style (`Normal` or `Italic`), or `null` if unset. */
    val fontStyle: FontStyle? = null,
    /** The fake-bold / fake-italic synthesis policy, or `null` if unset. */
    val fontSynthesis: FontSynthesis? = null,
    /** The typeface family, or `null` if unset. */
    val fontFamily: FontFamily? = null,
    /** The OpenType font feature settings string, or `null` if unset. */
    val fontFeatureSettings: String? = null,
    /** The inter-character spacing in SP or EM units, or [TextUnit.Unspecified] if unset. */
    val letterSpacing: TextUnit = TextUnit.Unspecified,
    /** The vertical baseline shift multiplier, or `null` if unset. */
    val baselineShift: BaselineShift? = null,
    /** The horizontal scale and skew transform, or `null` if unset. */
    val textGeometricTransform: TextGeometricTransform? = null,
    /** The locale list for glyph selection, or `null` if unset. */
    val localeList: LocaleList? = null,
    /** The span background color, or [Color.Unspecified] if unset. */
    val background: Color = Color.Unspecified,
    /** The underline / line-through decoration mask, or `null` if unset. */
    val textDecoration: TextDecoration? = null,
    /** The drop shadow configuration, or `null` if unset. */
    val shadow: Shadow? = null,
    /** Platform-specific span configuration, or `null` if unset. */
    val platformStyle: PlatformSpanStyle? = null,
    /** Fill or stroke draw style, or `null` if unset. */
    val drawStyle: DrawStyle? = null,
) {
    /**
     * Constructs a [LegacySpanStyle] with a solid foreground [color] wrapped via
     * [TextForegroundStyle.from].
     */
    constructor(
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        platformStyle: PlatformSpanStyle? = null,
        drawStyle: DrawStyle? = null,
    ) : this(
        textForegroundStyle = TextForegroundStyle.from(color),
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
        shadow = shadow,
        platformStyle = platformStyle,
        drawStyle = drawStyle,
    )

    /**
     * Constructs a [LegacySpanStyle] with a foreground [brush] and [alpha] wrapped via
     * [TextForegroundStyle.from].
     */
    constructor(
        brush: Brush?,
        alpha: Float = Float.NaN,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        fontFamily: FontFamily? = null,
        fontFeatureSettings: String? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
        baselineShift: BaselineShift? = null,
        textGeometricTransform: TextGeometricTransform? = null,
        localeList: LocaleList? = null,
        background: Color = Color.Unspecified,
        textDecoration: TextDecoration? = null,
        shadow: Shadow? = null,
        platformStyle: PlatformSpanStyle? = null,
        drawStyle: DrawStyle? = null,
    ) : this(
        textForegroundStyle = TextForegroundStyle.from(brush, alpha),
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
        shadow = shadow,
        platformStyle = platformStyle,
        drawStyle = drawStyle,
    )

    /** Foreground [Color] delegated to [textForegroundStyle]. */
    val color: Color
        get() = this.textForegroundStyle.color

    /** Foreground [Brush] delegated to [textForegroundStyle]. */
    val brush: Brush?
        get() = this.textForegroundStyle.brush

    /** Foreground opacity delegated to [textForegroundStyle]. */
    val alpha: Float
        get() = this.textForegroundStyle.alpha

    /**
     * Returns `true` if any attribute that affects glyph metrics or line height is specified on
     * this span style.
     */
    fun hasMetricAffectingAttributes(): Boolean {
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

    /**
     * Returns a new [LegacySpanStyle] that is a combination of this style and the given [other]
     * style, or returns `this` when [other] is `null`.
     */
    @Stable
    fun merge(other: LegacySpanStyle? = null): LegacySpanStyle {
        if (other == null) return this
        return fastMerge(
            color = other.textForegroundStyle.color,
            brush = other.textForegroundStyle.brush,
            alpha = other.textForegroundStyle.alpha,
            fontSize = other.fontSize,
            fontWeight = other.fontWeight,
            fontStyle = other.fontStyle,
            fontSynthesis = other.fontSynthesis,
            fontFamily = other.fontFamily,
            fontFeatureSettings = other.fontFeatureSettings,
            letterSpacing = other.letterSpacing,
            baselineShift = other.baselineShift,
            textGeometricTransform = other.textGeometricTransform,
            localeList = other.localeList,
            background = other.background,
            textDecoration = other.textDecoration,
            shadow = other.shadow,
            platformStyle = other.platformStyle,
            drawStyle = other.drawStyle,
        )
    }

    /**
     * Returns a copy of this [LegacySpanStyle] with optionally overridden fields, preserving the
     * existing [textForegroundStyle] (including a [Brush]) when [color] equals `this.color`.
     */
    fun copy(
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        fontFamily: FontFamily? = this.fontFamily,
        fontFeatureSettings: String? = this.fontFeatureSettings,
        letterSpacing: TextUnit = this.letterSpacing,
        baselineShift: BaselineShift? = this.baselineShift,
        textGeometricTransform: TextGeometricTransform? = this.textGeometricTransform,
        localeList: LocaleList? = this.localeList,
        background: Color = this.background,
        textDecoration: TextDecoration? = this.textDecoration,
        shadow: Shadow? = this.shadow,
        platformStyle: PlatformSpanStyle? = this.platformStyle,
        drawStyle: DrawStyle? = this.drawStyle,
    ): LegacySpanStyle {
        return LegacySpanStyle(
            textForegroundStyle =
                if (color == this.color) {
                    textForegroundStyle
                } else {
                    TextForegroundStyle.from(color)
                },
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
            shadow = shadow,
            platformStyle = platformStyle,
            drawStyle = drawStyle,
        )
    }

    /**
     * Returns a copy of this [LegacySpanStyle] with the foreground replaced by [brush] and [alpha].
     */
    fun copy(
        brush: Brush?,
        alpha: Float = this.alpha,
        fontSize: TextUnit = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        fontFamily: FontFamily? = this.fontFamily,
        fontFeatureSettings: String? = this.fontFeatureSettings,
        letterSpacing: TextUnit = this.letterSpacing,
        baselineShift: BaselineShift? = this.baselineShift,
        textGeometricTransform: TextGeometricTransform? = this.textGeometricTransform,
        localeList: LocaleList? = this.localeList,
        background: Color = this.background,
        textDecoration: TextDecoration? = this.textDecoration,
        shadow: Shadow? = this.shadow,
        platformStyle: PlatformSpanStyle? = this.platformStyle,
        drawStyle: DrawStyle? = this.drawStyle,
    ): LegacySpanStyle {
        return LegacySpanStyle(
            textForegroundStyle = TextForegroundStyle.from(brush, alpha),
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
            shadow = shadow,
            platformStyle = platformStyle,
            drawStyle = drawStyle,
        )
    }

    /**
     * Compares both layout-affecting and non-layout-affecting attributes to match the legacy
     * [SpanStyle.equals] contract.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LegacySpanStyle) return false
        return hasSameLayoutAffectingAttributes(other) && hasSameNonLayoutAttributes(other)
    }

    /**
     * Compares all twelve layout-affecting attributes one by one to match the legacy
     * implementation.
     */
    internal fun hasSameLayoutAffectingAttributes(other: LegacySpanStyle): Boolean {
        if (this === other) return true
        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false
        if (fontSynthesis != other.fontSynthesis) return false
        if (fontFamily != other.fontFamily) return false
        if (fontFeatureSettings != other.fontFeatureSettings) return false
        if (letterSpacing != other.letterSpacing) return false
        if (baselineShift != other.baselineShift) return false
        if (textGeometricTransform != other.textGeometricTransform) return false
        if (localeList != other.localeList) return false
        if (background != other.background) return false
        if (platformStyle != other.platformStyle) return false
        return true
    }

    /**
     * Compares all four draw-only attributes ([textForegroundStyle], [textDecoration], [shadow],
     * and [drawStyle]).
     */
    internal fun hasSameNonLayoutAttributes(other: LegacySpanStyle): Boolean {
        if (textForegroundStyle != other.textForegroundStyle) return false
        if (textDecoration != other.textDecoration) return false
        if (shadow != other.shadow) return false
        if (drawStyle != other.drawStyle) return false
        return true
    }

    /**
     * Computes the legacy polynomial `31 * result + field.hashCode()` hash across all eighteen
     * [SpanStyle] properties.
     */
    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + brush.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + (fontSynthesis?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (fontFeatureSettings?.hashCode() ?: 0)
        result = 31 * result + letterSpacing.hashCode()
        result = 31 * result + (baselineShift?.hashCode() ?: 0)
        result = 31 * result + (textGeometricTransform?.hashCode() ?: 0)
        result = 31 * result + (localeList?.hashCode() ?: 0)
        result = 31 * result + background.hashCode()
        result = 31 * result + (textDecoration?.hashCode() ?: 0)
        result = 31 * result + (shadow?.hashCode() ?: 0)
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (drawStyle?.hashCode() ?: 0)
        return result
    }

    /**
     * Computes the legacy polynomial `31 * result + field.hashCode()` hash across the twelve
     * layout-affecting [SpanStyle] properties.
     */
    internal fun hashCodeLayoutAffectingAttributes(): Int {
        var result = fontSize.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + (fontSynthesis?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (fontFeatureSettings?.hashCode() ?: 0)
        result = 31 * result + letterSpacing.hashCode()
        result = 31 * result + (baselineShift?.hashCode() ?: 0)
        result = 31 * result + (textGeometricTransform?.hashCode() ?: 0)
        result = 31 * result + (localeList?.hashCode() ?: 0)
        result = 31 * result + background.hashCode()
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        return result
    }

    /** Formats this fallback object using the public `SpanStyle(...)` string format. */
    override fun toString(): String {
        return "SpanStyle(" +
            "color=$color, " +
            "brush=$brush, " +
            "alpha=$alpha, " +
            "fontSize=$fontSize, " +
            "fontWeight=$fontWeight, " +
            "fontStyle=$fontStyle, " +
            "fontSynthesis=$fontSynthesis, " +
            "fontFamily=$fontFamily, " +
            "fontFeatureSettings=$fontFeatureSettings, " +
            "letterSpacing=$letterSpacing, " +
            "baselineShift=$baselineShift, " +
            "textGeometricTransform=$textGeometricTransform, " +
            "localeList=$localeList, " +
            "background=$background, " +
            "textDecoration=$textDecoration, " +
            "shadow=$shadow, " +
            "platformStyle=$platformStyle, " +
            "drawStyle=$drawStyle" +
            ")"
    }
}

/**
 * Legacy field-by-field linear interpolation between two [LegacySpanStyle] instances at [fraction],
 * used when [ComposeUiTextFlags.isPackedTextStylesEnabled] is `false`.
 */
internal fun lerp(start: LegacySpanStyle, stop: LegacySpanStyle, fraction: Float): LegacySpanStyle {
    return LegacySpanStyle(
        textForegroundStyle = lerp(start.textForegroundStyle, stop.textForegroundStyle, fraction),
        fontFamily = lerpDiscrete(start.fontFamily, stop.fontFamily, fraction),
        fontSize = lerpTextUnitInheritable(start.fontSize, stop.fontSize, fraction),
        fontWeight =
            lerp(
                start.fontWeight ?: FontWeight.Normal,
                stop.fontWeight ?: FontWeight.Normal,
                fraction,
            ),
        fontStyle = lerpDiscrete(start.fontStyle, stop.fontStyle, fraction),
        fontSynthesis = lerpDiscrete(start.fontSynthesis, stop.fontSynthesis, fraction),
        fontFeatureSettings =
            lerpDiscrete(start.fontFeatureSettings, stop.fontFeatureSettings, fraction),
        letterSpacing = lerpTextUnitInheritable(start.letterSpacing, stop.letterSpacing, fraction),
        baselineShift =
            lerp(
                start.baselineShift ?: BaselineShift(0f),
                stop.baselineShift ?: BaselineShift(0f),
                fraction,
            ),
        textGeometricTransform =
            lerp(
                start.textGeometricTransform ?: TextGeometricTransform.None,
                stop.textGeometricTransform ?: TextGeometricTransform.None,
                fraction,
            ),
        localeList = lerpDiscrete(start.localeList, stop.localeList, fraction),
        background = lerp(start.background, stop.background, fraction),
        textDecoration = lerpDiscrete(start.textDecoration, stop.textDecoration, fraction),
        shadow = nullSafeLerp(start.shadow, stop.shadow, fraction),
        platformStyle = lerpLegacyPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        drawStyle = lerpDiscrete(start.drawStyle, stop.drawStyle, fraction),
    )
}

/**
 * Interpolates two nullable [PlatformSpanStyle] instances for [lerp], substituting
 * [PlatformSpanStyle.Default] when only one side is non-null.
 */
private fun lerpLegacyPlatformStyle(
    start: PlatformSpanStyle?,
    stop: PlatformSpanStyle?,
    fraction: Float,
): PlatformSpanStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformSpanStyle.Default
    val stopNonNull = stop ?: PlatformSpanStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}

/**
 * Resolves unspecified fields in [style] to their concrete layout and draw defaults when
 * [ComposeUiTextFlags.isPackedTextStylesEnabled] is `false`.
 */
internal fun resolveLegacySpanStyleDefaults(style: LegacySpanStyle) =
    LegacySpanStyle(
        textForegroundStyle =
            style.textForegroundStyle.takeOrElse { LegacyDefaultColorForegroundStyle },
        fontSize = if (style.fontSize.isUnspecified) LegacyDefaultFontSize else style.fontSize,
        fontWeight = style.fontWeight ?: FontWeight.Normal,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontSynthesis = style.fontSynthesis ?: FontSynthesis.All,
        fontFamily = style.fontFamily ?: FontFamily.Default,
        fontFeatureSettings = style.fontFeatureSettings ?: "",
        letterSpacing =
            if (style.letterSpacing.isUnspecified) {
                LegacyDefaultLetterSpacing
            } else {
                style.letterSpacing
            },
        baselineShift =
            (style.baselineShift ?: BaselineShift.None).takeOrElse { BaselineShift.None },
        textGeometricTransform = style.textGeometricTransform ?: TextGeometricTransform.None,
        localeList = style.localeList,
        background = style.background.takeOrElse { LegacyDefaultBackgroundColor },
        textDecoration = style.textDecoration ?: TextDecoration.None,
        shadow = style.shadow ?: Shadow.None,
        platformStyle = style.platformStyle,
        drawStyle = style.drawStyle ?: Fill,
    )

/**
 * Merges individual span style attributes into `this` [LegacySpanStyle], returning `this` without
 * allocating when no specified parameter differs from the existing value.
 */
internal fun LegacySpanStyle.fastMerge(
    color: Color,
    brush: Brush?,
    alpha: Float,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontStyle: FontStyle?,
    fontSynthesis: FontSynthesis?,
    fontFamily: FontFamily?,
    fontFeatureSettings: String?,
    letterSpacing: TextUnit,
    baselineShift: BaselineShift?,
    textGeometricTransform: TextGeometricTransform?,
    localeList: LocaleList?,
    background: Color,
    textDecoration: TextDecoration?,
    shadow: Shadow?,
    platformStyle: PlatformSpanStyle?,
    drawStyle: DrawStyle?,
): LegacySpanStyle {
    // prioritize the parameters to Text in diffs here
    /**
     * color: Color fontSize: TextUnit fontStyle: FontStyle? fontWeight: FontWeight? fontFamily:
     * FontFamily? letterSpacing: TextUnit textDecoration: TextDecoration? textAlign: TextAlign?
     * lineHeight: TextUnit
     */
    // any new vals should do a pre-merge check here
    // ref check for font-family, since we don't want to compare lists in fast path
    // then compare the remaining params, for potential non-Text merges
    val requiresAlloc =
        fontSize.isSpecified && fontSize != this.fontSize ||
            brush == null && color.isSpecified && color != textForegroundStyle.color ||
            fontStyle != null && fontStyle != this.fontStyle ||
            fontWeight != null && fontWeight != this.fontWeight ||
            fontFamily != null && fontFamily !== this.fontFamily ||
            letterSpacing.isSpecified && letterSpacing != this.letterSpacing ||
            textDecoration != null && textDecoration != this.textDecoration ||
            brush != textForegroundStyle.brush ||
            brush != null && alpha != this.textForegroundStyle.alpha ||
            fontSynthesis != null && fontSynthesis != this.fontSynthesis ||
            fontFeatureSettings != null && fontFeatureSettings != this.fontFeatureSettings ||
            baselineShift != null && baselineShift != this.baselineShift ||
            textGeometricTransform != null &&
                textGeometricTransform != this.textGeometricTransform ||
            localeList != null && localeList != this.localeList ||
            background.isSpecified && background != this.background ||
            shadow != null && shadow != this.shadow ||
            platformStyle != null && platformStyle != this.platformStyle ||
            drawStyle != null && drawStyle != this.drawStyle

    if (!requiresAlloc) {
        return this
    }

    val otherTextForegroundStyle =
        if (brush != null) {
            TextForegroundStyle.from(brush, alpha)
        } else {
            TextForegroundStyle.from(color)
        }

    return LegacySpanStyle(
        textForegroundStyle = textForegroundStyle.merge(otherTextForegroundStyle),
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = if (!fontSize.isUnspecified) fontSize else this.fontSize,
        fontWeight = fontWeight ?: this.fontWeight,
        fontStyle = fontStyle ?: this.fontStyle,
        fontSynthesis = fontSynthesis ?: this.fontSynthesis,
        fontFeatureSettings = fontFeatureSettings ?: this.fontFeatureSettings,
        letterSpacing =
            if (!letterSpacing.isUnspecified) {
                letterSpacing
            } else {
                this.letterSpacing
            },
        baselineShift = baselineShift ?: this.baselineShift,
        textGeometricTransform = textGeometricTransform ?: this.textGeometricTransform,
        localeList = localeList ?: this.localeList,
        background = background.takeOrElse { this.background },
        textDecoration = textDecoration ?: this.textDecoration,
        shadow = shadow ?: this.shadow,
        platformStyle = mergePlatformStyle(platformStyle),
        drawStyle = drawStyle ?: this.drawStyle,
    )
}

/**
 * Merges [other] into [LegacySpanStyle.platformStyle], returning whichever side is non-null or
 * delegating to [PlatformSpanStyle.merge] when both are present.
 */
private fun LegacySpanStyle.mergePlatformStyle(other: PlatformSpanStyle?): PlatformSpanStyle? {
    if (platformStyle == null) return other
    if (other == null) return platformStyle
    return platformStyle.merge(other)
}
