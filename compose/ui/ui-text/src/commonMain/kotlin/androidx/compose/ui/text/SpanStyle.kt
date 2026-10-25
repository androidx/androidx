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

package androidx.compose.ui.text

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.isApplicable
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.text.style.modulate
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packInts
import kotlin.jvm.JvmField

/** The default font size if none is specified. */
private val DefaultFontSize = 14.sp
// TODO(nona): Introduce TextUnit.Original for representing "do not change the original result".
//  Need to distinguish from Inherit.
private val DefaultLetterSpacing = 0.sp

/** Cached values for `FontStyle` and `FontSynthesis` getters, plus default styles. */
internal object BoxedStyleValues {
    val NormalFontStyle: FontStyle? = FontStyle.Normal
    val ItalicFontStyle: FontStyle? = FontStyle.Italic
    val NoneFontSynthesis: FontSynthesis? = FontSynthesis.None
    val WeightFontSynthesis: FontSynthesis? = FontSynthesis.Weight
    val StyleFontSynthesis: FontSynthesis? = FontSynthesis.Style
    val AllFontSynthesis: FontSynthesis? = FontSynthesis.All
    val NoneBaselineShift: BaselineShift? = BaselineShift.None
    // Pre-allocated combined Underline + LineThrough instance; TextDecoration only caches the
    // single-flag constants on its companion object.
    @JvmField
    internal val CombinedTextDecoration: TextDecoration =
        TextDecoration.Underline + TextDecoration.LineThrough
    @JvmField
    internal val DefaultSpanStyle: SpanStyle =
        if (PackedTextStylesLatch.isEnabled) {
            SpanStyle(
                colorXor = 0L,
                bits = 0,
                extras = null,
                fontSize = TextUnit.Unspecified,
                letterSpacing = TextUnit.Unspecified,
                fontWeight = null,
                fontFamily = null,
            )
        } else {
            SpanStyle(DefaultLegacySpanStyle)
        }
    @JvmField
    internal val ResolvedDefaultSpanStyle: SpanStyle = DefaultSpanStyle.withResolvedDefaults()
    @JvmField
    internal val ResolvedDefaultParagraphStyleLtr: ParagraphStyle =
        DefaultParagraphStyle.withResolvedDefaults(LayoutDirection.Ltr)
    @JvmField
    internal val ResolvedDefaultParagraphStyleRtl: ParagraphStyle =
        DefaultParagraphStyle.withResolvedDefaults(LayoutDirection.Rtl)
}

/**
 * Rare [SpanStyle] fields that do not fit in the packed bits. Holds [Brush], [fontFeatureSettings],
 * [BaselineShift], [TextGeometricTransform], [LocaleList], [background], [Shadow], [DrawStyle], or
 * [PlatformSpanStyle] when set to a custom value.
 *
 * @param brush Shader or gradient brush used to draw text, or null if using a solid color.
 * @param alpha Opacity applied to [brush], or [Float.NaN] when unset.
 * @param fontFeatureSettings OpenType font feature settings string, or null if unset.
 * @param baselineShift Vertical text baseline offset, or null if unset.
 * @param textGeometricTransform Geometric transformation (horizontal scale and skew) for text.
 * @param localeList Locale list for text rendering, or null if unset.
 * @param background Background color behind the text characters.
 * @param shadow Blur and offset shadow effect drawn behind text, or null if unset.
 * @param drawStyle Stroke or fill drawing configuration, or null if unset.
 * @param platformStyle Platform-specific span styling configuration, or null if unset.
 */
@Immutable
internal class SpanStyleExtras(
    @JvmField val brush: Brush?,
    @JvmField val alpha: Float,
    @JvmField val fontFeatureSettings: String?,
    val baselineShift: BaselineShift?,
    @JvmField val textGeometricTransform: TextGeometricTransform?,
    @JvmField val localeList: LocaleList?,
    val background: Color,
    @JvmField val shadow: Shadow?,
    @JvmField val drawStyle: DrawStyle?,
    @JvmField val platformStyle: PlatformSpanStyle? = null,
) {
    /**
     * Checks if this instance's extra attributes match the given parameter values.
     *
     * Reference comparison (`===`) is used for heap objects ([brush], [fontFeatureSettings],
     * [textGeometricTransform], [localeList], [shadow], [drawStyle], and [platformStyle]). Value
     * equality is used for value classes ([baselineShift] and [background]), and [Float.compareTo]
     * for [alpha].
     *
     * Returns `true` when all attributes match, allowing callers to reuse this instance and avoid
     * allocating a new [SpanStyleExtras] object.
     */
    fun matches(
        brush: Brush?,
        alpha: Float,
        fontFeatureSettings: String?,
        baselineShift: BaselineShift?,
        textGeometricTransform: TextGeometricTransform?,
        localeList: LocaleList?,
        background: Color,
        shadow: Shadow?,
        drawStyle: DrawStyle?,
        platformStyle: PlatformSpanStyle? = null,
    ): Boolean =
        this.brush === brush &&
            this.alpha.compareTo(alpha) == 0 &&
            this.fontFeatureSettings === fontFeatureSettings &&
            this.baselineShift == baselineShift &&
            this.textGeometricTransform === textGeometricTransform &&
            this.localeList === localeList &&
            this.background.value == background.value &&
            this.shadow === shadow &&
            this.drawStyle === drawStyle &&
            this.platformStyle === platformStyle

    /**
     * Checks if the layout-affecting attributes ([fontFeatureSettings], [baselineShift],
     * [textGeometricTransform], [localeList], [background], and [platformStyle]) match between this
     * instance and [other].
     *
     * If [other] is null, checks that all layout-affecting attributes in this instance are unset.
     */
    fun hasSameLayoutAttributes(other: SpanStyleExtras?): Boolean =
        if (other == null) {
            fontFeatureSettings == null &&
                baselineShift == null &&
                textGeometricTransform == null &&
                localeList == null &&
                background.isUnspecified &&
                platformStyle == null
        } else {
            fontFeatureSettings == other.fontFeatureSettings &&
                baselineShift == other.baselineShift &&
                textGeometricTransform == other.textGeometricTransform &&
                localeList == other.localeList &&
                background == other.background &&
                platformStyle == other.platformStyle
        }

    /**
     * Checks if the non-layout (draw-only) attributes ([brush], [alpha], [shadow], and [drawStyle])
     * match between this instance and [other].
     *
     * If [other] is null, checks that all non-layout attributes in this instance are unset.
     */
    fun hasSameNonLayoutAttributes(other: SpanStyleExtras?): Boolean =
        if (other == null) {
            brush == null && shadow == null && drawStyle == null
        } else {
            brush == other.brush &&
                alpha.compareTo(other.alpha) == 0 &&
                shadow == other.shadow &&
                drawStyle == other.drawStyle
        }

    /**
     * Computes the combined hash code for all layout-affecting attributes in this extras container.
     */
    fun hashCodeLayoutAffectingAttributes(): Int {
        var result = fontFeatureSettings?.hashCode() ?: 0
        result = 31 * result + (baselineShift?.hashCode() ?: 0)
        result = 31 * result + (textGeometricTransform?.hashCode() ?: 0)
        result = 31 * result + (localeList?.hashCode() ?: 0)
        result = 31 * result + mixLongHash(packSpanColorXor(background))
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        return result
    }

    /**
     * Computes the combined hash code for all non-layout (draw-only) attributes in this extras
     * container.
     */
    fun hashCodeNonLayoutAttributes(): Int {
        var result = brush?.hashCode() ?: 0
        result = 31 * result + if (brush != null) alpha.hashCode() else 0
        result = 31 * result + (shadow?.hashCode() ?: 0)
        result = 31 * result + (drawStyle?.hashCode() ?: 0)
        return result
    }

    /**
     * Checks structural equality between this extras container and [other] by comparing both layout
     * and non-layout attributes.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpanStyleExtras) return false
        return hasSameLayoutAttributes(other) && hasSameNonLayoutAttributes(other)
    }

    /** Returns a combined hash code mixing both layout and non-layout attribute hash values. */
    override fun hashCode(): Int =
        31 * hashCodeLayoutAffectingAttributes() + hashCodeNonLayoutAttributes()
}

/**
 * Styling configuration that applies at the character level.
 *
 * Use [SpanStyle] to configure character-level styles such as text color, font size, font family,
 * background color, and text decorations.
 *
 * For paragraph-level styling (e.g., alignment, line height), see [ParagraphStyle].
 *
 * @sample androidx.compose.ui.text.samples.SpanStyleSample
 * @sample androidx.compose.ui.text.samples.SpanStyleBrushSample
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
 * @see AnnotatedString
 * @see TextStyle
 * @see ParagraphStyle
 */
@Immutable
public class SpanStyle : AnnotatedString.Annotation {
    // The fill to draw text, a unified representation of Color and Brush.
    /**
     * Text [Color] stored as `color.value.toLong() xor UNSPECIFIED_COLOR_BITS`. [Color.Unspecified]
     * (`0x10UL`) maps to `0L`, and `mergeSpanColorXor` checks for a set color via `(other or
     * -other) shr 63`. When [brush] is a `ShaderBrush`, [colorXor] is `0L`.
     */
    @JvmField internal val colorXor: Long

    /**
     * 32-bit packed word for [SpanStyle] enum, bitmask, flag, and preset fields.
     *
     * ### Bit Layout (`bits 0..31`, LSB first)
     *
     * ```
     *  31                    17 16   15  14  13  12  11  10   9 8      6 5      3   2   1   0
     * +------------------------+-------+---+---+---+---+---+---+--------+--------+---+---+---+
     * |      unused (0)        |fontSty|drw|shd|bg |tgt|ffs|plt|textDeco|fontSynt| - |bs |ext|
     * +------------------------+-------+---+---+---+---+---+---+--------+--------+---+---+---+
     *  <------- 15 bits ------> 2 bits   1   1   1   1   1   1   3 bits   3 bits   1   1   1
     *                           <-2b-->                         <- 3b -> <- 3b ->
     * ```
     *
     * |Bits    |Width|Field / Meaning                                                                              |
     * |--------|-----|---------------------------------------------------------------------------------------------|
     * |`0`     |1    |[SPAN_HAS_EXTRAS]: `1` iff [extras] `!= null` (enforced by internal constructor)             |
     * |`1`     |1    |[HAS_BASELINE_SHIFT]: `1` if set ([BaselineShift.None] when `extras.baselineShift` is null)  |
     * |`2`     |1    |Unused (`0`)                                                                                 |
     * |`3..5`  |3    |[fontSynthesis]: `0` = null, else `((value and 0x3) shl 1) or 1`                             |
     * |`6..8`  |3    |[textDecoration]: `0` = null, else `((mask and 0x3) shl 1) or 1`                             |
     * |`9`     |1    |[HAS_PLATFORM_STYLE]: `1` if set ([PlatformSpanStyle.Default] if no extras)                  |
     * |`10`    |1    |[HAS_FONT_FEATURES]: `1` if set (`""` when `extras.fontFeatureSettings` is null)             |
     * |`11`    |1    |[HAS_GEOMETRIC_TRANSFORM]: `1` if set ([TextGeometricTransform.None] if no extras)           |
     * |`12`    |1    |[HAS_BACKGROUND]: `1` if set ([Color.Transparent] when `extras.background` unset)            |
     * |`13`    |1    |[HAS_SHADOW]: `1` if set ([Shadow.None] when `extras.shadow` is null)                        |
     * |`14`    |1    |[HAS_DRAW_STYLE]: `1` if set ([Fill] when `extras.drawStyle` is null)                        |
     * |`15..16`|2    |[fontStyle]: `0` = null, `1` (`0b01`) = [FontStyle.Normal], `3` (`0b11`) = [FontStyle.Italic]|
     * |`17..31`|15   |Unused (`0`)                                                                                 |
     *
     * ### Odd-Code Rule & SWAR Merge
     * Fields in bits `3..8` ([fontSynthesis], [textDecoration]) and bits `15..16` ([fontStyle])
     * store `0` when unset and an **odd** code (`(raw shl 1) or 1`) when set. Their low bit acts as
     * the set flag, and [mergeSpanBits] scales that bit into a full mask via `* TWO_BIT_FIELD_MASK`
     * (`0x3`) and `* THREE_BIT_FIELD_MASK` (`0x7`).
     */
    internal val bits: Int

    /**
     * Rare span fields ([brush], [alpha], [fontFeatureSettings], [baselineShift],
     * [textGeometricTransform], [localeList], [background], [shadow], [drawStyle],
     * [platformStyle]). Stays `null` when all rare fields are unset or match their bit presets.
     */
    @JvmField internal val extras: SpanStyleExtras?
    internal val legacySpanStyle: LegacySpanStyle?

    /** Glyph size. If [TextUnit.Unspecified], inherits size from parent or default style. */
    public val fontSize: TextUnit

    /**
     * Amount of space (in SP or EM) to add between letters. If [TextUnit.Unspecified], inherits
     * from parent.
     */
    public val letterSpacing: TextUnit

    /** Typeface thickness (e.g., bold). */
    public val fontWeight: FontWeight?

    /** Font family for rendering. */
    public val fontFamily: FontFamily?

    internal constructor(
        colorXor: Long,
        bits: Int,
        extras: SpanStyleExtras?,
        fontSize: TextUnit,
        letterSpacing: TextUnit,
        fontWeight: FontWeight?,
        fontFamily: FontFamily?,
    ) {
        this.colorXor = colorXor
        this.bits = if (extras == null) bits and SPAN_HAS_EXTRAS.inv() else bits or SPAN_HAS_EXTRAS
        this.extras = extras
        this.legacySpanStyle = null
        this.fontSize = fontSize
        this.letterSpacing = letterSpacing
        this.fontWeight = fontWeight
        this.fontFamily = fontFamily
    }

    internal constructor(legacySpanStyle: LegacySpanStyle) {
        this.colorXor = 0L
        this.bits = 0
        this.extras = null
        this.legacySpanStyle = if (PackedTextStylesLatch.isEnabled) null else legacySpanStyle
        this.fontSize = legacySpanStyle.fontSize
        this.letterSpacing = legacySpanStyle.letterSpacing
        this.fontWeight = legacySpanStyle.fontWeight
        this.fontFamily = legacySpanStyle.fontFamily
    }

    /**
     * Styling configuration for a text span. This configuration only allows character level
     * styling, in order to set paragraph level styling such as line height, or text alignment
     * please see [ParagraphStyle].
     *
     * @sample androidx.compose.ui.text.samples.SpanStyleSample
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     * @param color The text color.
     * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
     *   may be [TextUnit.Unspecified] for inheriting from another [SpanStyle].
     * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
     * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     *   or style cannot be found in the provided font family.
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontFeatureSettings The advanced typography settings provided by font. The format is
     *   the same as the CSS font-feature-settings attribute:
     *   https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param letterSpacing The amount of space (in em) to add between each letter.
     * @param baselineShift The amount by which the text is shifted up from the current baseline.
     * @param textGeometricTransform The geometric transformation applied the text.
     * @param localeList The locale list used to select region-specific glyphs.
     * @param background The background color for the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param shadow The shadow effect applied on the text.
     * @see AnnotatedString
     * @see TextStyle
     * @see ParagraphStyle
     */
    @Deprecated(
        "SpanStyle constructors that do not take new stable parameters " +
            "like PlatformStyle, DrawStyle are deprecated. Please use the new stable " +
            "constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
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
    ) : this(
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
        shadow = shadow,
        platformStyle = null,
        drawStyle = null,
    )

    /**
     * Styling configuration for a text span. This configuration only allows character level
     * styling, in order to set paragraph level styling such as line height, or text alignment
     * please see [ParagraphStyle].
     *
     * @sample androidx.compose.ui.text.samples.SpanStyleSample
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     * @param color The color to draw the text.
     * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
     *   may be [TextUnit.Unspecified] for inheriting from another [SpanStyle].
     * @param fontWeight The typeface thickness to use when painting the text (e.g., bold).
     * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     *   or style cannot be found in the provided font family.
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontFeatureSettings The advanced typography settings provided by font. The format is
     *   the same as the CSS font-feature-settings attribute:
     *   https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param letterSpacing The amount of space (in em) to add between each letter.
     * @param baselineShift The amount by which the text is shifted up from the current baseline.
     * @param textGeometricTransform The geometric transformation applied the text.
     * @param localeList The locale list used to select region-specific glyphs.
     * @param background The background color for the text.
     * @param textDecoration The decorations to paint on the text (e.g., an underline).
     * @param shadow The shadow effect applied on the text.
     * @param platformStyle Platform specific [SpanStyle] parameters.
     * @see AnnotatedString
     * @see TextStyle
     * @see ParagraphStyle
     */
    @Deprecated(
        "SpanStyle constructors that do not take new stable parameters " +
            "like PlatformStyle, DrawStyle are deprecated. Please use the new stable " +
            "constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
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
    ) : this(
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
        shadow = shadow,
        platformStyle = platformStyle,
        drawStyle = null,
    )

    /**
     * Styling configuration for text spans.
     *
     * Configures character-level styles such as text color, font size, font family, background
     * color, and text decorations.
     *
     * For paragraph-level styling (e.g., alignment, line height), see [ParagraphStyle].
     *
     * @sample androidx.compose.ui.text.samples.SpanStyleSample
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     * @sample androidx.compose.ui.text.samples.BaselineShiftSample
     * @param color color to apply to the text
     * @param fontSize glyph size. If [TextUnit.Unspecified], inherits size from parent or default
     *   style.
     * @param fontWeight typeface thickness (e.g., bold)
     * @param fontStyle typeface variant (e.g., italic)
     * @param fontSynthesis font synthesis rules to fallback to bold/italic if the requested style
     *   is missing in [fontFamily]
     * @param fontFamily font family for rendering
     * @param fontFeatureSettings advanced font features in CSS format (e.g., "smcp" for small caps)
     * @param letterSpacing amount of space (in SP or EM) to add between letters. If
     *   [TextUnit.Unspecified], inherits from parent.
     * @param baselineShift vertical shift amount from the baseline (e.g., for superscript or
     *   subscript)
     * @param textGeometricTransform geometric transformation to apply
     * @param localeList locale list for region-specific glyphs
     * @param background color of background rectangle covering entire line height from start to end
     * @param textDecoration decorations (e.g., underline)
     * @param shadow shadow effect
     * @param platformStyle platform-specific parameters
     * @param drawStyle drawing style (fill or stroke)
     * @see AnnotatedString
     * @see TextStyle
     * @see ParagraphStyle
     */
    public constructor(
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
    ) {
        if (PackedTextStylesLatch.isEnabled) {
            val extras =
                createSpanStyleExtras(
                    /* brush = */ null,
                    Float.NaN,
                    fontFeatureSettings,
                    baselineShift,
                    textGeometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                    platformStyle,
                )
            val rawBits =
                packSpanStyleBits(
                    fontStyle,
                    fontSynthesis,
                    fontFeatureSettings,
                    textGeometricTransform,
                    background,
                    textDecoration,
                    shadow,
                    platformStyle,
                    drawStyle,
                    baselineShift,
                )
            this.colorXor = packSpanColorXor(color)
            this.bits =
                if (extras == null) rawBits and SPAN_HAS_EXTRAS.inv()
                else rawBits or SPAN_HAS_EXTRAS
            this.extras = extras
            this.legacySpanStyle = null
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        } else {
            this.colorXor = 0L
            this.bits = 0
            this.extras = null
            this.legacySpanStyle =
                LegacySpanStyle(
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
                    shadow = shadow,
                    platformStyle = platformStyle,
                    drawStyle = drawStyle,
                )
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        }
    }

    /**
     * Styling configuration for text spans.
     *
     * Configures character-level styles such as text brush, opacity, font size, font family,
     * background color, and text decorations.
     *
     * For paragraph-level styling (e.g., alignment, line height), see [ParagraphStyle].
     *
     * @sample androidx.compose.ui.text.samples.SpanStyleBrushSample
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     * @sample androidx.compose.ui.text.samples.BaselineShiftSample
     * @param brush [Brush] for painting text, null for unspecified
     * @param alpha opacity applied to [brush] (0.0 to 1.0)
     * @param fontSize glyph size. If [TextUnit.Unspecified], inherits size from parent or default
     *   style.
     * @param fontWeight typeface thickness (e.g., bold)
     * @param fontStyle typeface variant (e.g., italic)
     * @param fontSynthesis font synthesis rules to fallback to bold/italic if the requested style
     *   is missing in [fontFamily]
     * @param fontFamily font family for rendering
     * @param fontFeatureSettings advanced font features in CSS format (e.g., "smcp" for small caps)
     * @param letterSpacing amount of space (in SP or EM) to add between letters. If
     *   [TextUnit.Unspecified], inherits from parent.
     * @param baselineShift vertical shift amount from the baseline (e.g., for superscript or
     *   subscript)
     * @param textGeometricTransform geometric transformation to apply
     * @param localeList locale list for region-specific glyphs
     * @param background color of background rectangle covering entire line height from start to end
     * @param textDecoration decorations (e.g., underline)
     * @param shadow shadow effect
     * @param platformStyle platform-specific parameters
     * @param drawStyle drawing style (fill or stroke)
     * @see AnnotatedString
     * @see TextStyle
     * @see ParagraphStyle
     */
    public constructor(
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
    ) {
        if (PackedTextStylesLatch.isEnabled) {
            val extras =
                createSpanStyleExtras(
                    if (brush is SolidColor) /* brush= */ null else brush,
                    alpha,
                    fontFeatureSettings,
                    baselineShift,
                    textGeometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                    platformStyle,
                )
            val rawBits =
                packSpanStyleBits(
                    fontStyle,
                    fontSynthesis,
                    fontFeatureSettings,
                    textGeometricTransform,
                    background,
                    textDecoration,
                    shadow,
                    platformStyle,
                    drawStyle,
                    baselineShift,
                )
            this.colorXor =
                if (brush is SolidColor) packSpanColorXor(brush.value.modulate(alpha)) else 0L
            this.bits =
                if (extras == null) rawBits and SPAN_HAS_EXTRAS.inv()
                else rawBits or SPAN_HAS_EXTRAS
            this.extras = extras
            this.legacySpanStyle = null
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        } else {
            this.colorXor = 0L
            this.bits = 0
            this.extras = null
            this.legacySpanStyle =
                LegacySpanStyle(
                    brush = brush,
                    alpha = alpha,
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
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        }
    }

    // Not called. The old primary constructor took a TextForegroundStyle with defaults, and
    // its synthetic public `$default` overload is part of the binary API.
    @Suppress("unused")
    internal constructor(
        textForegroundStyle: TextForegroundStyle,
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
    ) {
        val brush = textForegroundStyle.brush
        if (PackedTextStylesLatch.isEnabled) {
            val extras =
                createSpanStyleExtras(
                    if (brush is SolidColor) null else brush,
                    if (brush != null) textForegroundStyle.alpha else Float.NaN,
                    fontFeatureSettings,
                    baselineShift,
                    textGeometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                    platformStyle,
                )
            val rawBits =
                packSpanStyleBits(
                    fontStyle,
                    fontSynthesis,
                    fontFeatureSettings,
                    textGeometricTransform,
                    background,
                    textDecoration,
                    shadow,
                    platformStyle,
                    drawStyle,
                    baselineShift,
                )
            this.colorXor =
                if (brush != null) {
                    if (brush is SolidColor) {
                        packSpanColorXor(brush.value.modulate(textForegroundStyle.alpha))
                    } else {
                        0L
                    }
                } else {
                    packSpanColorXor(textForegroundStyle.color)
                }
            this.bits =
                if (extras == null) rawBits and SPAN_HAS_EXTRAS.inv()
                else rawBits or SPAN_HAS_EXTRAS
            this.extras = extras
            this.legacySpanStyle = null
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        } else {
            this.colorXor = 0L
            this.bits = 0
            this.extras = null
            this.legacySpanStyle =
                LegacySpanStyle(
                    textForegroundStyle = textForegroundStyle,
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
            this.fontSize = fontSize
            this.letterSpacing = letterSpacing
            this.fontWeight = fontWeight
            this.fontFamily = fontFamily
        }
    }

    /** Color to draw text. */
    public val color: Color
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.color
            } else {
                Color((colorXor xor UNSPECIFIED_COLOR_BITS).toULong())
            }

    /** Brush to draw text. If not null, overrides [color]. */
    public val brush: Brush?
        get() = if (legacySpanStyle != null) legacySpanStyle.brush else extras?.brush

    /**
     * Opacity of text. This value is either provided along side Brush, or via alpha channel in
     * color.
     */
    public val alpha: Float
        get() {
            if (legacySpanStyle != null) return legacySpanStyle.alpha
            val extras = extras
            if (extras != null && extras.brush != null) return extras.alpha
            return if (colorXor == 0L) Float.NaN else color.alpha
        }

    /** Typeface variant (e.g., italic). */
    public val fontStyle: FontStyle?
        get() {
            if (legacySpanStyle != null) return legacySpanStyle.fontStyle
            val bits = bits
            if (bits and HAS_FONT_STYLE == 0) return null
            // `FontStyle` is an `@JvmInline value class`, so returning `FontStyle?` requires a
            // boxed wrapper object on the JVM. Returning the pre-boxed singletons from
            // `BoxedStyleValues` avoids allocating a new `FontStyle` box on every getter call.
            return if (bits and FONT_STYLE_IS_ITALIC == 0) {
                BoxedStyleValues.NormalFontStyle
            } else {
                BoxedStyleValues.ItalicFontStyle
            }
        }

    /**
     * Font synthesis rules to fallback to bold/italic if the requested style is missing in
     * [fontFamily].
     */
    public val fontSynthesis: FontSynthesis?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.fontSynthesis
            } else if (bits and FONT_SYNTHESIS_MASK == 0) {
                null
            } else {
                // The code is (value shl 1) or 1, so the value is the code without its low bit.
                // Like `fontStyle`, returns pre-boxed `BoxedStyleValues` singletons to avoid
                // value-class boxing allocations when returning nullable `FontSynthesis?`.
                when ((bits and FONT_SYNTHESIS_MASK) ushr (FONT_SYNTHESIS_SHIFT + 1)) {
                    0 -> BoxedStyleValues.NoneFontSynthesis
                    1 -> BoxedStyleValues.WeightFontSynthesis
                    2 -> BoxedStyleValues.StyleFontSynthesis
                    else -> BoxedStyleValues.AllFontSynthesis
                }
            }

    /** Advanced font features in CSS format (e.g., "smcp" for small caps). */
    public val fontFeatureSettings: String?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.fontFeatureSettings
            } else {
                extras?.fontFeatureSettings ?: if (bits and HAS_FONT_FEATURES != 0) "" else null
            }

    /** Vertical shift amount from the baseline (e.g., for superscript or subscript). */
    public val baselineShift: BaselineShift?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.baselineShift
            } else {
                extras?.baselineShift
                    ?: if (bits and HAS_BASELINE_SHIFT != 0) {
                        BoxedStyleValues.NoneBaselineShift
                    } else {
                        null
                    }
            }

    internal val baselineShiftOrNone: BaselineShift
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.baselineShift ?: BaselineShift.None
            } else {
                extras?.baselineShift ?: BaselineShift.None
            }

    /** Geometric transformation to apply. */
    public val textGeometricTransform: TextGeometricTransform?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.textGeometricTransform
            } else {
                extras?.textGeometricTransform
                    ?: if (hasTextGeometricTransform) TextGeometricTransform.None else null
            }

    /** Locale list for region-specific glyphs. */
    public val localeList: LocaleList?
        get() = if (legacySpanStyle != null) legacySpanStyle.localeList else extras?.localeList

    /** Color of background rectangle covering entire line height from start to end. */
    public val background: Color
        get() {
            if (legacySpanStyle != null) return legacySpanStyle.background
            val extras = extras
            // Non-transparent background colors are stored in `extras.background`. A transparent
            // background (`Color.Transparent`) is encoded via `HAS_BACKGROUND` alone so that common
            // resolved styles (which default `background` to `Color.Transparent`) do not allocate
            // a `SpanStyleExtras` sidecar.
            if (extras != null && extras.background.isSpecified) return extras.background
            return if (bits and HAS_BACKGROUND != 0) Color.Transparent else Color.Unspecified
        }

    /** Decorations (e.g., underline). */
    public val textDecoration: TextDecoration?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.textDecoration
            } else if (bits and TEXT_DECORATION_MASK == 0) {
                null
            } else {
                when ((bits and TEXT_DECORATION_MASK) ushr (TEXT_DECORATION_SHIFT + 1)) {
                    0 -> TextDecoration.None
                    1 -> TextDecoration.Underline
                    2 -> TextDecoration.LineThrough
                    else -> BoxedStyleValues.CombinedTextDecoration
                }
            }

    /** Shadow effect. */
    public val shadow: Shadow?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.shadow
            } else {
                extras?.shadow ?: if (bits and HAS_SHADOW != 0) Shadow.None else null
            }

    /** Platform-specific parameters. */
    public val platformStyle: PlatformSpanStyle?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.platformStyle
            } else {
                extras?.platformStyle
                    ?: if (bits and HAS_PLATFORM_STYLE != 0) PlatformSpanStyle.Default else null
            }

    /** Drawing style (fill or stroke). */
    public val drawStyle: DrawStyle?
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.drawStyle
            } else {
                extras?.drawStyle ?: if (bits and HAS_DRAW_STYLE != 0) Fill else null
            }

    internal val hasFontStyle: Boolean
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.fontStyle != null
            } else {
                bits and HAS_FONT_STYLE != 0
            }

    internal val hasFontSynthesis: Boolean
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.fontSynthesis != null
            } else {
                bits and FONT_SYNTHESIS_MASK != 0
            }

    internal fun copyFontAttributes(): SpanStyle {
        if (legacySpanStyle != null) {
            return SpanStyle(
                LegacySpanStyle(
                    fontFamily = legacySpanStyle.fontFamily,
                    fontWeight = legacySpanStyle.fontWeight,
                    fontStyle = legacySpanStyle.fontStyle,
                    fontSynthesis = legacySpanStyle.fontSynthesis,
                )
            )
        }
        return SpanStyle(
            colorXor = 0L,
            bits = bits and (FONT_STYLE_MASK or FONT_SYNTHESIS_MASK),
            extras = null,
            fontSize = TextUnit.Unspecified,
            letterSpacing = TextUnit.Unspecified,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
        )
    }

    internal val hasTextDecoration: Boolean
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.textDecoration != null
            } else {
                bits and TEXT_DECORATION_MASK != 0
            }

    internal val hasTextGeometricTransform: Boolean
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.textGeometricTransform != null
            } else {
                bits and HAS_GEOMETRIC_TRANSFORM != 0
            }

    internal val hasBaselineShift: Boolean
        get() =
            if (legacySpanStyle != null) {
                legacySpanStyle.baselineShift != null
            } else {
                bits and HAS_BASELINE_SHIFT != 0
            }

    /** True if a field that changes glyph size or spacing is set. */
    internal val hasMetricAffectingAttributes: Boolean
        get() {
            if (legacySpanStyle != null) return legacySpanStyle.hasMetricAffectingAttributes()
            if (
                bits and METRIC_BITS != 0 ||
                    fontSize.isSpecified ||
                    letterSpacing.isSpecified ||
                    fontWeight != null ||
                    fontFamily != null
            ) {
                return true
            }
            val extras = extras ?: return false
            val shift = extras.baselineShift
            return extras.localeList != null || (shift != null && shift.isApplicable)
        }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun isPackedDefault(): Boolean =
        colorXor == 0L &&
            bits == 0 &&
            fontSize.isUnspecified &&
            letterSpacing.isUnspecified &&
            fontWeight == null &&
            fontFamily == null

    internal fun isDefault(): Boolean {
        if (legacySpanStyle != null) {
            return legacySpanStyle === DefaultLegacySpanStyle ||
                legacySpanStyle == DefaultLegacySpanStyle
        }
        return isPackedDefault()
    }

    /**
     * Returns a new span style that is a combination of this style and the given [other] style.
     *
     * [other] span style's null or inherit properties are replaced with the non-null properties of
     * this span style. Another way to think of it is that the "missing" properties of the [other]
     * style are _filled_ by the properties of this style.
     *
     * If the given span style is null, returns this span style.
     *
     * @param other style to merge
     */
    @Stable
    public fun merge(other: SpanStyle? = null): SpanStyle {
        if (other == null || other === this) return this
        if (legacySpanStyle != null) return mergeLegacy(other)
        if (other.isPackedDefault()) return this
        if (isPackedDefault()) return other
        val thisBits = bits
        val otherBits = other.bits
        var colorXor = mergeSpanColorXor(colorXor, other.colorXor)
        val bits = mergeSpanBits(thisBits, otherBits)
        val fontSize = if (other.fontSize.isSpecified) other.fontSize else fontSize
        val letterSpacing =
            if (other.letterSpacing.isSpecified) other.letterSpacing else letterSpacing
        val fontWeight = other.fontWeight ?: fontWeight
        val fontFamily = other.fontFamily ?: fontFamily
        val thisExtras = extras
        val otherExtras = other.extras
        var extras: SpanStyleExtras? = null
        if ((thisBits or otherBits) and SPAN_HAS_EXTRAS != 0) {
            // A field set in `other` wins, even when its value is a preset that needs no extras.
            val thisBrush = thisExtras?.brush
            val otherBrush = otherExtras?.brush
            // Like TextForegroundStyle.merge (b/230787077): a color never replaces a brush.
            val brush = otherBrush ?: thisBrush
            val alpha =
                when {
                    otherBrush != null && !otherExtras.alpha.isNaN() -> otherExtras.alpha
                    thisBrush != null -> thisExtras.alpha
                    else -> Float.NaN
                }
            if (brush != null) colorXor = 0L
            val fontFeatureSettings =
                if (otherBits and HAS_FONT_FEATURES != 0) otherExtras?.fontFeatureSettings
                else thisExtras?.fontFeatureSettings
            val baselineShift =
                if (otherBits and HAS_BASELINE_SHIFT != 0) otherExtras?.baselineShift
                else thisExtras?.baselineShift
            val textGeometricTransform =
                if (otherBits and HAS_GEOMETRIC_TRANSFORM != 0) {
                    otherExtras?.textGeometricTransform
                } else {
                    thisExtras?.textGeometricTransform
                }
            val localeList = otherExtras?.localeList ?: thisExtras?.localeList
            val backgroundSource =
                if (otherBits and HAS_BACKGROUND != 0) otherExtras else thisExtras
            // Not `?.background`: a nullable Color would be boxed.
            val background =
                if (backgroundSource == null) Color.Unspecified else backgroundSource.background
            val shadow =
                if (otherBits and HAS_SHADOW != 0) otherExtras?.shadow else thisExtras?.shadow
            val drawStyle =
                if (otherBits and HAS_DRAW_STYLE != 0) otherExtras?.drawStyle
                else thisExtras?.drawStyle
            val platformStyle =
                mergeNormalizedPlatformStyle(
                    thisBits,
                    thisExtras?.platformStyle,
                    otherBits,
                    otherExtras?.platformStyle,
                )
            extras =
                when {
                    thisExtras != null &&
                        thisExtras.matches(
                            brush,
                            alpha,
                            fontFeatureSettings,
                            baselineShift,
                            textGeometricTransform,
                            localeList,
                            background,
                            shadow,
                            drawStyle,
                            platformStyle,
                        ) -> thisExtras
                    otherExtras != null &&
                        otherExtras.matches(
                            brush,
                            alpha,
                            fontFeatureSettings,
                            baselineShift,
                            textGeometricTransform,
                            localeList,
                            background,
                            shadow,
                            drawStyle,
                            platformStyle,
                        ) -> otherExtras
                    else ->
                        createSpanStyleExtras(
                            brush,
                            alpha,
                            fontFeatureSettings,
                            baselineShift,
                            textGeometricTransform,
                            localeList,
                            background,
                            shadow,
                            drawStyle,
                            platformStyle,
                        )
                }
        }
        if (sameAs(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)) {
            return this
        }
        if (other.sameAs(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)) {
            return other
        }
        return SpanStyle(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)
    }

    private fun mergeLegacy(other: SpanStyle): SpanStyle {
        val legacy = legacySpanStyle!!
        val mergedLegacy = legacy.merge(other.legacySpanStyle)
        if (mergedLegacy === legacy) return this
        if (mergedLegacy === other.legacySpanStyle) return other
        return SpanStyle(mergedLegacy)
    }

    /**
     * Merges raw [SpanStyle] arguments onto `this`.
     *
     * A color-only merge, the common case from `TextStyle.merge(color = ...)`, makes at most one
     * object.
     */
    internal fun fastMerge(
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
    ): SpanStyle {
        if (legacySpanStyle != null) {
            return fastMergeLegacy(
                color,
                brush,
                alpha,
                fontSize,
                fontWeight,
                fontStyle,
                fontSynthesis,
                fontFamily,
                fontFeatureSettings,
                letterSpacing,
                baselineShift,
                textGeometricTransform,
                localeList,
                background,
                textDecoration,
                shadow,
                platformStyle,
                drawStyle,
            )
        }
        // prioritize the parameters to Text in diffs here
        /**
         * color: Color fontSize: TextUnit fontStyle: FontStyle? fontWeight: FontWeight? fontFamily:
         * FontFamily? letterSpacing: TextUnit textDecoration: TextDecoration? textAlign: TextAlign?
         * lineHeight: TextUnit
         */
        // any new vals should do a pre-merge check here
        // ref check for font-family, since we don't want to compare lists in fast path
        // then compare the remaining params, for potential non-Text merges
        // Fast path for callers (such as BasicText / Material Text) that only override `color`
        // (or pass all default/unspecified span arguments). Avoids running the full 10-field
        // bit-pack, mask expansion, and extras merge when no other span property is set.
        val onlyColorMayChange =
            brush == null &&
                fontSize.isUnspecified &&
                fontWeight == null &&
                fontStyle == null &&
                fontSynthesis == null &&
                fontFamily == null &&
                fontFeatureSettings == null &&
                letterSpacing.isUnspecified &&
                baselineShift == null &&
                textGeometricTransform == null &&
                localeList == null &&
                background.isUnspecified &&
                textDecoration == null &&
                shadow == null &&
                platformStyle == null &&
                drawStyle == null
        if (onlyColorMayChange) {
            val otherColorXor = packSpanColorXor(color)
            if (otherColorXor == 0L || otherColorXor == colorXor || extras?.brush != null) {
                return this
            }
            return SpanStyle(
                otherColorXor,
                bits,
                extras,
                this.fontSize,
                this.letterSpacing,
                this.fontWeight,
                this.fontFamily,
            )
        }
        val otherColorXor =
            when (brush) {
                null -> packSpanColorXor(color)
                is SolidColor -> packSpanColorXor(brush.value.modulate(alpha))
                else -> 0L
            }
        val shaderBrush = if (brush is SolidColor) null else brush
        val otherBits =
            packSpanStyleBits(
                fontStyle,
                fontSynthesis,
                fontFeatureSettings,
                textGeometricTransform,
                background,
                textDecoration,
                shadow,
                platformStyle,
                drawStyle,
                baselineShift,
            )
        val thisBits = this.bits
        val extras =
            if (
                (thisBits or otherBits) and (SPAN_HAS_EXTRAS or ONE_BIT_FIELDS) == 0 &&
                    shaderBrush == null &&
                    localeList == null
            ) {
                null
            } else {
                mergeSpanStyleExtras(
                    this.extras,
                    shaderBrush,
                    alpha,
                    fontFeatureSettings,
                    baselineShift,
                    textGeometricTransform,
                    localeList,
                    background,
                    shadow,
                    drawStyle,
                    platformStyle,
                    thisBits,
                )
            }
        val colorXor =
            if (extras?.brush != null) 0L else mergeSpanColorXor(this.colorXor, otherColorXor)
        val bits = mergeSpanBits(thisBits, otherBits)
        val mergedFontSize = if (fontSize.isSpecified) fontSize else this.fontSize
        val mergedLetterSpacing =
            if (letterSpacing.isSpecified) letterSpacing else this.letterSpacing
        val mergedFontWeight = fontWeight ?: this.fontWeight
        val mergedFontFamily = fontFamily ?: this.fontFamily
        if (
            sameAs(
                colorXor,
                bits,
                extras,
                mergedFontSize,
                mergedLetterSpacing,
                mergedFontWeight,
                mergedFontFamily,
            )
        ) {
            // we're done
            return this
        }
        return SpanStyle(
            colorXor,
            bits,
            extras,
            mergedFontSize,
            mergedLetterSpacing,
            mergedFontWeight,
            mergedFontFamily,
        )
    }

    private fun fastMergeLegacy(
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
    ): SpanStyle {
        val legacy = legacySpanStyle!!
        val merged =
            legacy.fastMerge(
                color = color,
                brush = brush,
                alpha = alpha,
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
        return if (merged === legacy) this else SpanStyle(merged)
    }

    /**
     * Returns a [SpanStyle] with all unset (`null` or `Unspecified`) fields set to their layout and
     * draw defaults (see [resolveSpanStyleDefaults]). Returns `this` if all fields are set.
     */
    internal fun withResolvedDefaults(): SpanStyle {
        if (legacySpanStyle != null) {
            return SpanStyle(resolveLegacySpanStyleDefaults(legacySpanStyle))
        }
        var extras = extras
        val shift = extras?.baselineShift
        if (shift != null && shift.multiplier.isNaN()) {
            // BaselineShift.Unspecified resolves to None, which the bits hold on their own.
            extras =
                createSpanStyleExtras(
                    extras.brush,
                    extras.alpha,
                    extras.fontFeatureSettings,
                    /* baselineShift = */ null,
                    extras.textGeometricTransform,
                    extras.localeList,
                    extras.background,
                    extras.shadow,
                    extras.drawStyle,
                    extras.platformStyle,
                )
        }
        val colorXor = if (colorXor != 0L || extras?.brush != null) colorXor else DEFAULT_COLOR_XOR
        val bits = resolveSpanBits(bits)
        val fontSize = if (fontSize.isUnspecified) DefaultFontSize else fontSize
        val letterSpacing = if (letterSpacing.isUnspecified) DefaultLetterSpacing else letterSpacing
        val fontWeight = fontWeight ?: FontWeight.Normal
        val fontFamily = fontFamily ?: FontFamily.Default
        if (sameAs(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)) {
            return this
        }
        return SpanStyle(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)
    }

    /** Plus operator overload that applies a [merge]. */
    @Stable public operator fun plus(other: SpanStyle): SpanStyle = this.merge(other)

    @Deprecated(
        "SpanStyle copy constructors that do not take new stable parameters " +
            "like PlatformStyle, DrawStyle are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
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
    ): SpanStyle {
        return copy(
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
            shadow = shadow,
            platformStyle = this.platformStyle,
            drawStyle = this.drawStyle,
        )
    }

    @Deprecated(
        "SpanStyle copy constructors that do not take new stable parameters " +
            "like PlatformStyle, DrawStyle are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
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
    ): SpanStyle {
        return copy(
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
            shadow = shadow,
            platformStyle = platformStyle,
            drawStyle = this.drawStyle,
        )
    }

    public fun copy(
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
    ): SpanStyle {
        if (legacySpanStyle != null) {
            return SpanStyle(
                legacySpanStyle.copy(
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
                    shadow = shadow,
                    platformStyle = platformStyle,
                    drawStyle = drawStyle,
                )
            )
        }
        val extras = extras
        val keepBrush = extras?.brush != null && color == this.color
        return createSpanStyle(
            color = color,
            brush = if (keepBrush) extras.brush else null,
            alpha = if (keepBrush) extras.alpha else Float.NaN,
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

    public fun copy(
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
    ): SpanStyle {
        if (legacySpanStyle != null) {
            return SpanStyle(
                legacySpanStyle.copy(
                    brush = brush,
                    alpha = alpha,
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
            )
        }
        return createSpanStyle(
            color = Color.Unspecified,
            brush = brush,
            alpha = alpha,
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

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpanStyle) return false
        return hasSameLayoutAffectingAttributes(other) && hasSameNonLayoutAttributes(other)
    }

    internal fun hasSameLayoutAffectingAttributes(other: SpanStyle): Boolean {
        if (this === other) return true
        if (legacySpanStyle != null) {
            return legacySpanStyle.hasSameLayoutAffectingAttributes(other.legacySpanStyle!!)
        }
        if ((bits xor other.bits) and LAYOUT_BITS != 0) return false
        if (fontSize != other.fontSize || letterSpacing != other.letterSpacing) return false
        if (fontWeight != other.fontWeight || fontFamily != other.fontFamily) return false
        val thisExtras = extras
        val otherExtras = other.extras
        if (thisExtras === otherExtras) return true
        return thisExtras?.hasSameLayoutAttributes(otherExtras)
            ?: otherExtras!!.hasSameLayoutAttributes(/* other= */ null)
    }

    internal fun hasSameNonLayoutAttributes(other: SpanStyle): Boolean {
        if (legacySpanStyle != null) {
            return legacySpanStyle.hasSameNonLayoutAttributes(other.legacySpanStyle!!)
        }
        if (colorXor != other.colorXor) return false
        if ((bits xor other.bits) and NON_LAYOUT_BITS != 0) return false
        val thisExtras = extras
        val otherExtras = other.extras
        if (thisExtras === otherExtras) return true
        return thisExtras?.hasSameNonLayoutAttributes(otherExtras)
            ?: otherExtras!!.hasSameNonLayoutAttributes(/* other= */ null)
    }

    public override fun hashCode(): Int {
        if (legacySpanStyle != null) return legacySpanStyle.hashCode()
        var result = mixLongHash(colorXor)
        result = 31 * result + (bits and NON_LAYOUT_BITS)
        result = 31 * result + hashCodeLayoutAffectingAttributes()
        result = 31 * result + (extras?.hashCodeNonLayoutAttributes() ?: 0)
        return murmurFinalize32(result)
    }

    internal fun hashCodeLayoutAffectingAttributes(): Int {
        if (legacySpanStyle != null) return legacySpanStyle.hashCodeLayoutAffectingAttributes()
        var result = bits and LAYOUT_BITS
        result = 31 * result + mixLongHash(fontSize)
        result = 31 * result + mixLongHash(letterSpacing)
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (extras?.hashCodeLayoutAffectingAttributes() ?: 0)
        return murmurFinalize32(result)
    }

    /**
     * Helper used by [merge], [fastMerge], and [withResolvedDefaults] to check if the new parts
     * match `this` (or `other`) before making a new [SpanStyle] object.
     *
     * Unlike [equals] (which checks two [SpanStyle] objects with `==`), [sameAs]:
     * 1. Takes raw parts so callers can reuse an old [SpanStyle] object without making a new one
     *    first.
     * 2. Clears bit 0 ([SPAN_HAS_EXTRAS]) on `this.bits`. [mergeSpanBits], [packSpanStyleBits], and
     *    [resolveSpanBits] return raw field bits with bit 0 cleared, while [SpanStyle] sets
     *    [SPAN_HAS_EXTRAS] when `extras != null`.
     * 3. Uses `===` on [extras], [fontWeight], and [fontFamily] since callers reuse old objects
     *    when their fields do not change.
     */
    private fun sameAs(
        colorXor: Long,
        bits: Int,
        extras: SpanStyleExtras?,
        fontSize: TextUnit,
        letterSpacing: TextUnit,
        fontWeight: FontWeight?,
        fontFamily: FontFamily?,
    ): Boolean =
        this.bits and SPAN_HAS_EXTRAS.inv() == bits &&
            this.colorXor == colorXor &&
            this.extras === extras &&
            this.fontSize == fontSize &&
            this.letterSpacing == letterSpacing &&
            this.fontWeight === fontWeight &&
            this.fontFamily === fontFamily

    public override fun toString(): String {
        if (legacySpanStyle != null) return legacySpanStyle.toString()
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

// SpanStyle.bits layout, low bit first. A zero field means "unset". Multi-bit fields in bits 3..8
// and 15..16 store an odd code, so their low bit doubles as "set", and merge builds their field
// mask with a single multiply.
//   0      has extras (matches `extras != null`)
//   1      baselineShift set; BaselineShift.None unless extras has one
//   3..5   fontSynthesis    (value shl 1) or 1, for None, Weight, Style, All
//   6..8   textDecoration   (mask shl 1) or 1, for None, Underline, LineThrough, both
//   9      platformStyle set; PlatformSpanStyle.Default unless extras has one
//   10     fontFeatureSettings set; "" unless extras has the string
//   11     textGeometricTransform set; None unless extras has one
//   12     background set; Transparent unless extras has a color
//   13     shadow set; Shadow.None unless extras has one
//   14     drawStyle set; Fill unless extras has one
//   15..16 fontStyle        (value shl 1) or 1, for Normal (0 -> 1) and Italic (1 -> 3)
private const val SPAN_HAS_EXTRAS = 1
private const val TWO_BIT_FIELD_MASK = 0x3
private const val THREE_BIT_FIELD_MASK = 0x7
private const val HAS_BASELINE_SHIFT = 1 shl 1
private const val FONT_SYNTHESIS_SHIFT = 3
private const val FONT_SYNTHESIS_MASK = THREE_BIT_FIELD_MASK shl FONT_SYNTHESIS_SHIFT
private const val FONT_SYNTHESIS_VALUE_BITS = 2
private const val FONT_SYNTHESIS_MAX_RAW = (1 shl FONT_SYNTHESIS_VALUE_BITS) - 1
private const val TEXT_DECORATION_SHIFT = 6
private const val TEXT_DECORATION_MASK = THREE_BIT_FIELD_MASK shl TEXT_DECORATION_SHIFT
private const val TEXT_DECORATION_VALUE_BITS = 2
private const val TEXT_DECORATION_MAX_RAW = (1 shl TEXT_DECORATION_VALUE_BITS) - 1
private const val HAS_PLATFORM_STYLE = 1 shl 9
private const val HAS_FONT_FEATURES = 1 shl 10
private const val HAS_GEOMETRIC_TRANSFORM = 1 shl 11
private const val HAS_BACKGROUND = 1 shl 12
private const val HAS_SHADOW = 1 shl 13
private const val HAS_DRAW_STYLE = 1 shl 14
private const val FONT_STYLE_SHIFT = 15
private const val FONT_STYLE_MASK = TWO_BIT_FIELD_MASK shl FONT_STYLE_SHIFT
private const val FONT_STYLE_VALUE_BITS = 1
private const val FONT_STYLE_MAX_RAW = (1 shl FONT_STYLE_VALUE_BITS) - 1
private const val HAS_FONT_STYLE = 1 shl FONT_STYLE_SHIFT
private const val FONT_STYLE_IS_ITALIC = 1 shl (FONT_STYLE_SHIFT + 1)

// Bits cleared by SpanStyleSaver because drawStyle and platformStyle are not persisted by Saver,
// and SPAN_HAS_EXTRAS is recomputed from extras != null on restore.
internal const val UNSAVED_SPAN_BITS = HAS_DRAW_STYLE or HAS_PLATFORM_STYLE or SPAN_HAS_EXTRAS

private const val TWO_BIT_LOW_BITS = 1 shl FONT_STYLE_SHIFT
private const val THREE_BIT_LOW_BITS = (1 shl FONT_SYNTHESIS_SHIFT) or (1 shl TEXT_DECORATION_SHIFT)
private const val ONE_BIT_FIELDS =
    HAS_BASELINE_SHIFT or
        HAS_PLATFORM_STYLE or
        HAS_FONT_FEATURES or
        HAS_GEOMETRIC_TRANSFORM or
        HAS_BACKGROUND or
        HAS_SHADOW or
        HAS_DRAW_STYLE
private const val NON_LAYOUT_BITS = TEXT_DECORATION_MASK or HAS_SHADOW or HAS_DRAW_STYLE
private const val LAYOUT_BITS = (NON_LAYOUT_BITS or SPAN_HAS_EXTRAS).inv()
private const val METRIC_BITS =
    FONT_STYLE_MASK or FONT_SYNTHESIS_MASK or HAS_FONT_FEATURES or HAS_GEOMETRIC_TRANSFORM

private const val UNSPECIFIED_COLOR_BITS = 0x10L
// Color.Black is 0xFF000000 in the top 32 bits.
private const val DEFAULT_COLOR_XOR = (0xFF00_0000L shl 32) xor UNSPECIFIED_COLOR_BITS

// Fails with `Division by zero` if FontStyle.MAX_FONT_STYLE_VALUE exceeds FONT_STYLE_MAX_RAW (1).
// Widen FONT_STYLE_VALUE_BITS and FONT_STYLE_MASK if a new FontStyle value is added.
@Suppress("unused")
private const val CHECK_FONT_STYLE_CAPACITY =
    1 / (1 - ((FONT_STYLE_MAX_RAW - FontStyle.MAX_FONT_STYLE_VALUE) ushr 31))

// Fails with `Division by zero` if FontSynthesis.MAX_FONT_SYNTHESIS_MASK exceeds
// FONT_SYNTHESIS_MAX_RAW (3).
// Widen FONT_SYNTHESIS_VALUE_BITS and FONT_SYNTHESIS_MASK if a new FontSynthesis flag is added.
@Suppress("unused")
private const val CHECK_FONT_SYNTHESIS_CAPACITY =
    1 / (1 - ((FONT_SYNTHESIS_MAX_RAW - FontSynthesis.MAX_FONT_SYNTHESIS_MASK) ushr 31))

// Fails with `Division by zero` if TextDecoration.MAX_TEXT_DECORATION_MASK exceeds
// TEXT_DECORATION_MAX_RAW (3).
// Widen TEXT_DECORATION_VALUE_BITS and TEXT_DECORATION_MASK if a new TextDecoration flag is added.
@Suppress("unused")
private const val CHECK_TEXT_DECORATION_CAPACITY =
    1 / (1 - ((TEXT_DECORATION_MAX_RAW - TextDecoration.MAX_TEXT_DECORATION_MASK) ushr 31))

/**
 * Maps [color] into `SpanStyle.colorXor` form (`color.value.toLong() xor UNSPECIFIED_COLOR_BITS`).
 * `0L` means [Color.Unspecified] (unset), and any other `Long` is a set [Color] (such as
 * `Color.Transparent`, whose `0UL` bits map to `UNSPECIFIED_COLOR_BITS = 0x10L`).
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun packSpanColorXor(color: Color): Long =
    color.value.toLong() xor UNSPECIFIED_COLOR_BITS

/** Mixes a [TextUnit]'s type and raw float bits with [GOLDEN_RATIO_64] and folds to 32 bits. */
@Suppress("NOTHING_TO_INLINE")
internal inline fun mixLongHash(unit: TextUnit): Int {
    val type = if (unit.isSp) 1 else if (unit.isEm) 2 else 0
    return mixLongHash(packInts(type, unit.value.toRawBits()))
}

/**
 * Returns [other] when [other] is a set color (`other != 0L`). Returns [base] when [other] is unset
 * (`other == 0L`).
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun mergeSpanColorXor(base: Long, other: Long): Long {
    // `(other or -other)` has sign bit 63 set iff `other != 0L`, so arithmetic `shr 63` produces
    // `-1L` (`0xFFFF_FFFF_FFFF_FFFFL`) when `other != 0L` and `0L` when `other == 0L`.
    val mask = (other or -other) shr 63
    return (base and mask.inv()) or (other and mask)
}

/**
 * Merges [other] onto [base] across all packed [SpanStyle] fields in one SWAR pass:
 * - 2-bit odd-code field ([SpanStyle.fontStyle]): grabs low bit via [TWO_BIT_LOW_BITS] and scales
 *   by [TWO_BIT_FIELD_MASK] (`0x3` = `0b11`).
 * - 3-bit odd-code fields ([SpanStyle.fontSynthesis], [SpanStyle.textDecoration]): grabs low bits
 *   via [THREE_BIT_LOW_BITS] and scales by [THREE_BIT_FIELD_MASK] (`0x7` = `0b111`).
 * - 1-bit set flags ([ONE_BIT_FIELDS]): used as their own mask.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun mergeSpanBits(base: Int, other: Int): Int {
    val mask =
        ((other and TWO_BIT_LOW_BITS) * TWO_BIT_FIELD_MASK) or
            ((other and THREE_BIT_LOW_BITS) * THREE_BIT_FIELD_MASK) or
            (other and ONE_BIT_FIELDS)
    return ((base and mask.inv()) or (other and mask)) and SPAN_HAS_EXTRAS.inv()
}

/**
 * Resolves all unset fields in [bits] to their [resolveSpanStyleDefaults] defaults:
 * - All 1-bit preset flags except [HAS_PLATFORM_STYLE] become set (resolving unset fields to `""`,
 *   [BaselineShift.None], [TextGeometricTransform.None], [Color.Transparent], [Shadow.None], and
 *   [Fill]).
 * - [HAS_FONT_STYLE] is set, resolving an unset `0` (`0b00`) in bits `15..16` to [FontStyle.Normal]
 *   (`0b01`) while preserving [FontStyle.Italic] (`0b11`).
 * - Unset [SpanStyle.fontSynthesis] resolves to [FontSynthesis.All] (`0b111` at
 *   [FONT_SYNTHESIS_SHIFT]).
 * - Unset [SpanStyle.textDecoration] resolves to [TextDecoration.None] (`0b001` at
 *   [TEXT_DECORATION_SHIFT]).
 */
internal fun resolveSpanBits(bits: Int): Int {
    // platformStyle has no default, so it stays as it is. Unset fontStyle has bits 15..16 == 0,
    // so setting HAS_FONT_STYLE resolves it to FontStyle.Normal (0b01) while preserving Italic
    // (0b11).
    var resolved =
        (bits and SPAN_HAS_EXTRAS.inv()) or
            (ONE_BIT_FIELDS and HAS_PLATFORM_STYLE.inv()) or
            HAS_FONT_STYLE
    if (bits and FONT_SYNTHESIS_MASK == 0) resolved = resolved or FONT_SYNTHESIS_MASK
    if (bits and TEXT_DECORATION_MASK == 0) resolved = resolved or (1 shl TEXT_DECORATION_SHIFT)
    return resolved
}

/**
 * Packs [SpanStyle] properties into the 32-bit `SpanStyle.bits` word (leaving bit 0 /
 * [SPAN_HAS_EXTRAS] as `0`).
 */
internal fun packSpanStyleBits(
    fontStyle: FontStyle?,
    fontSynthesis: FontSynthesis?,
    fontFeatureSettings: String?,
    textGeometricTransform: TextGeometricTransform?,
    background: Color,
    textDecoration: TextDecoration?,
    shadow: Shadow?,
    platformStyle: PlatformSpanStyle?,
    drawStyle: DrawStyle?,
    baselineShift: BaselineShift?,
): Int {
    var bits = 0
    // FontStyle.value is in 0..MAX_FONT_STYLE_VALUE (0 = Normal, 1 = Italic); validated by
    // FontStyle's constructor and locked at compile time by CHECK_FONT_STYLE_CAPACITY.
    if (fontStyle != null) {
        bits = (((fontStyle.value and FONT_STYLE_MAX_RAW) shl 1) or 1) shl FONT_STYLE_SHIFT
    }
    // FontSynthesis.value is in 0..MAX_FONT_SYNTHESIS_MASK (0b00..0b11); capacity is locked by
    // CHECK_FONT_SYNTHESIS_CAPACITY and tested by StyleDomainLockTest.
    if (fontSynthesis != null) {
        bits =
            bits or
                ((((fontSynthesis.value and FONT_SYNTHESIS_MAX_RAW) shl 1) or 1) shl
                    FONT_SYNTHESIS_SHIFT)
    }
    // TextDecoration.mask is in 0..MAX_TEXT_DECORATION_MASK (0b00..0b11); capacity is locked by
    // CHECK_TEXT_DECORATION_CAPACITY and tested by StyleDomainLockTest.
    if (textDecoration != null) {
        bits =
            bits or
                ((((textDecoration.mask and TEXT_DECORATION_MAX_RAW) shl 1) or 1) shl
                    TEXT_DECORATION_SHIFT)
    }
    if (platformStyle != null) bits = bits or HAS_PLATFORM_STYLE
    if (fontFeatureSettings != null) bits = bits or HAS_FONT_FEATURES
    if (textGeometricTransform != null) bits = bits or HAS_GEOMETRIC_TRANSFORM
    if (background.isSpecified) bits = bits or HAS_BACKGROUND
    if (shadow != null) bits = bits or HAS_SHADOW
    if (drawStyle != null) bits = bits or HAS_DRAW_STYLE
    if (baselineShift != null) bits = bits or HAS_BASELINE_SHIFT
    return bits
}

/**
 * Strips preset values from rare [SpanStyle] fields that live in `SpanStyle.bits` (`""`,
 * [BaselineShift.None] with `+0.0f` bits, [TextGeometricTransform.None], [Color.Transparent],
 * [Shadow.None], [Fill], [PlatformSpanStyle.Default]). Makes a [SpanStyleExtras] object only if a
 * custom field or [brush]/[localeList] remains.
 */
internal fun createSpanStyleExtras(
    brush: Brush?,
    alpha: Float,
    fontFeatureSettings: String?,
    baselineShift: BaselineShift?,
    textGeometricTransform: TextGeometricTransform?,
    localeList: LocaleList?,
    background: Color,
    shadow: Shadow?,
    drawStyle: DrawStyle?,
    platformStyle: PlatformSpanStyle? = null,
): SpanStyleExtras? {
    val normalizedFontFeatures =
        if (fontFeatureSettings == null || fontFeatureSettings.isEmpty()) null
        else fontFeatureSettings
    val normalizedBaselineShift =
        if (baselineShift == null || baselineShift.multiplier.toRawBits() == 0) null
        else baselineShift
    val normalizedTransform =
        if (
            textGeometricTransform == null || textGeometricTransform == TextGeometricTransform.None
        ) {
            null
        } else {
            textGeometricTransform
        }
    val normalizedBackground =
        if (background == Color.Transparent) Color.Unspecified else background
    val normalizedShadow = if (shadow == null || shadow == Shadow.None) null else shadow
    val normalizedDrawStyle = if (drawStyle == null || drawStyle === Fill) null else drawStyle
    val normalizedPlatform =
        if (platformStyle == null || platformStyle == PlatformSpanStyle.Default) null
        else platformStyle
    if (
        brush == null &&
            normalizedFontFeatures == null &&
            normalizedBaselineShift == null &&
            normalizedTransform == null &&
            localeList == null &&
            normalizedBackground.isUnspecified &&
            normalizedShadow == null &&
            normalizedDrawStyle == null &&
            normalizedPlatform == null
    ) {
        return null
    }
    return SpanStyleExtras(
        brush,
        if (brush == null) Float.NaN else alpha,
        normalizedFontFeatures,
        normalizedBaselineShift,
        normalizedTransform,
        localeList,
        normalizedBackground,
        normalizedShadow,
        normalizedDrawStyle,
        normalizedPlatform,
    )
}

/**
 * Merges `this` and `other` [PlatformSpanStyle]s and normalizes [PlatformSpanStyle.Default] to
 * `null` for [SpanStyleExtras.platformStyle].
 */
private fun mergeNormalizedPlatformStyle(
    thisBits: Int,
    thisPlatformStyle: PlatformSpanStyle?,
    otherBits: Int,
    otherPlatformStyle: PlatformSpanStyle?,
): PlatformSpanStyle? {
    if (thisPlatformStyle == null && otherPlatformStyle == null) return null
    val base =
        thisPlatformStyle
            ?: if (thisBits and HAS_PLATFORM_STYLE != 0) PlatformSpanStyle.Default else null
    val other =
        otherPlatformStyle
            ?: if (otherBits and HAS_PLATFORM_STYLE != 0) PlatformSpanStyle.Default else null
    val merged = if (base == null) other else if (other == null) base else base.merge(other)
    return if (merged == null || merged == PlatformSpanStyle.Default) null else merged
}

/**
 * Merges rare [SpanStyle] fields onto [thisExtras]. Maps preset values to `null` or
 * [Color.Unspecified] and reuses [thisExtras] when all fields match.
 */
internal fun mergeSpanStyleExtras(
    thisExtras: SpanStyleExtras?,
    otherBrush: Brush?,
    otherAlpha: Float,
    fontFeatureSettings: String?,
    baselineShift: BaselineShift?,
    textGeometricTransform: TextGeometricTransform?,
    localeList: LocaleList?,
    background: Color,
    shadow: Shadow?,
    drawStyle: DrawStyle?,
    platformStyle: PlatformSpanStyle? = null,
    thisBits: Int = 0,
): SpanStyleExtras? {
    val thisBrush = thisExtras?.brush
    val brush = otherBrush ?: thisBrush
    val alpha =
        when {
            otherBrush != null && !otherAlpha.isNaN() -> otherAlpha
            thisBrush != null -> thisExtras.alpha
            else -> Float.NaN
        }
    val mergedFontFeatures =
        if (fontFeatureSettings == null) thisExtras?.fontFeatureSettings
        else if (fontFeatureSettings.isEmpty()) null else fontFeatureSettings
    val mergedBaselineShift =
        if (baselineShift == null) thisExtras?.baselineShift
        else if (baselineShift.multiplier.toRawBits() == 0) null else baselineShift
    val mergedTransform =
        if (textGeometricTransform == null) thisExtras?.textGeometricTransform
        else if (textGeometricTransform == TextGeometricTransform.None) null
        else textGeometricTransform
    val mergedLocaleList = localeList ?: thisExtras?.localeList
    val mergedBackground =
        if (background.isSpecified) {
            if (background == Color.Transparent) Color.Unspecified else background
        } else if (thisExtras == null) {
            Color.Unspecified
        } else {
            thisExtras.background
        }
    val mergedShadow =
        if (shadow == null) thisExtras?.shadow else if (shadow == Shadow.None) null else shadow
    val mergedDrawStyle =
        if (drawStyle == null) thisExtras?.drawStyle
        else if (drawStyle === Fill) null else drawStyle
    val normalizedOtherPlatform =
        if (platformStyle == null || platformStyle == PlatformSpanStyle.Default) null
        else platformStyle
    val mergedPlatformStyle =
        mergeNormalizedPlatformStyle(
            thisBits,
            thisExtras?.platformStyle,
            if (platformStyle != null) HAS_PLATFORM_STYLE else 0,
            normalizedOtherPlatform,
        )
    if (
        thisExtras != null &&
            thisExtras.matches(
                brush,
                alpha,
                mergedFontFeatures,
                mergedBaselineShift,
                mergedTransform,
                mergedLocaleList,
                mergedBackground,
                mergedShadow,
                mergedDrawStyle,
                mergedPlatformStyle,
            )
    ) {
        return thisExtras
    }
    if (
        brush == null &&
            mergedFontFeatures == null &&
            mergedBaselineShift == null &&
            mergedTransform == null &&
            mergedLocaleList == null &&
            mergedBackground.isUnspecified &&
            mergedShadow == null &&
            mergedDrawStyle == null &&
            mergedPlatformStyle == null
    ) {
        return null
    }
    return SpanStyleExtras(
        brush,
        if (brush == null) Float.NaN else alpha,
        mergedFontFeatures,
        mergedBaselineShift,
        mergedTransform,
        mergedLocaleList,
        mergedBackground,
        mergedShadow,
        mergedDrawStyle,
        mergedPlatformStyle,
    )
}

/** Builds a [SpanStyle], or returns [BoxedStyleValues.DefaultSpanStyle] when nothing is set. */
internal fun createSpanStyle(
    color: Color = Color.Unspecified,
    brush: Brush? = null,
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
): SpanStyle {
    if (!PackedTextStylesLatch.isEnabled) {
        return createLegacySpanStyle(
            color,
            brush,
            alpha,
            fontSize,
            fontWeight,
            fontStyle,
            fontSynthesis,
            fontFamily,
            fontFeatureSettings,
            letterSpacing,
            baselineShift,
            textGeometricTransform,
            localeList,
            background,
            textDecoration,
            shadow,
            platformStyle,
            drawStyle,
        )
    }
    // A SolidColor brush is stored as a color, like TextForegroundStyle.from does.
    val colorXor =
        when (brush) {
            null -> packSpanColorXor(color)
            is SolidColor -> packSpanColorXor(brush.value.modulate(alpha))
            else -> 0L
        }
    val shaderBrush = if (brush is SolidColor) null else brush
    val bits =
        packSpanStyleBits(
            fontStyle,
            fontSynthesis,
            fontFeatureSettings,
            textGeometricTransform,
            background,
            textDecoration,
            shadow,
            platformStyle,
            drawStyle,
            baselineShift,
        )
    val extras =
        if (bits and ONE_BIT_FIELDS == 0 && shaderBrush == null && localeList == null) {
            null
        } else {
            createSpanStyleExtras(
                shaderBrush,
                alpha,
                fontFeatureSettings,
                baselineShift,
                textGeometricTransform,
                localeList,
                background,
                shadow,
                drawStyle,
                platformStyle,
            )
        }
    if (
        colorXor == 0L &&
            bits == 0 &&
            extras == null &&
            fontSize.isUnspecified &&
            letterSpacing.isUnspecified &&
            fontWeight == null &&
            fontFamily == null
    ) {
        return BoxedStyleValues.DefaultSpanStyle
    }
    return SpanStyle(colorXor, bits, extras, fontSize, letterSpacing, fontWeight, fontFamily)
}

private fun createLegacySpanStyle(
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
): SpanStyle =
    SpanStyle(
        if (brush != null) {
            LegacySpanStyle(
                brush = brush,
                alpha = alpha,
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
        } else {
            LegacySpanStyle(
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
                shadow = shadow,
                platformStyle = platformStyle,
                drawStyle = drawStyle,
            )
        }
    )

/** Fills in all unset fields. Reuses one object for the default style. */
internal fun resolveSpanStyleDefaults(style: SpanStyle): SpanStyle {
    if (style.legacySpanStyle != null) {
        return SpanStyle(resolveLegacySpanStyleDefaults(style.legacySpanStyle))
    }
    return if (style.isPackedDefault()) BoxedStyleValues.ResolvedDefaultSpanStyle
    else style.withResolvedDefaults()
}

/**
 * @param a An sp value. Maybe [TextUnit.Unspecified]
 * @param b An sp value. Maybe [TextUnit.Unspecified]
 */
internal fun lerpTextUnitInheritable(a: TextUnit, b: TextUnit, t: Float): TextUnit {
    // Not lerpDiscrete: a generic call would box both TextUnits.
    if (a.isUnspecified || b.isUnspecified) return if (t < 0.5f) a else b
    return lerp(a, b, t)
}

/**
 * Lerp between two values that cannot be transitioned. Returns [a] if [fraction] is smaller than
 * 0.5 otherwise [b].
 */
internal fun <T> lerpDiscrete(a: T, b: T, fraction: Float): T = if (fraction < 0.5) a else b

/**
 * Interpolate between two span styles.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning that the
 * interpolation has not started, returning [start] (or something equivalent to [start]), 1.0
 * meaning that the interpolation has finished, returning [stop] (or something equivalent to
 * [stop]), and values in between meaning that the interpolation is at the relevant point on the
 * timeline between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and 1.0, so
 * negative values and values greater than 1.0 are valid.
 */
public fun lerp(start: SpanStyle, stop: SpanStyle, fraction: Float): SpanStyle {
    if (start.legacySpanStyle != null) {
        return SpanStyle(lerp(start.legacySpanStyle, stop.legacySpanStyle!!, fraction))
    }
    val startBrush = start.brush
    val stopBrush = stop.brush
    // Same rules as lerp(TextForegroundStyle): blend two colors, or blend the alpha of two
    // brushes, else jump from one to the other.
    val discreteStyleResolved = if (fraction < 0.5f) start else stop
    val startWeight = start.fontWeight ?: FontWeight.Normal
    val stopWeight = stop.fontWeight ?: FontWeight.Normal
    val startShift = start.baselineShiftOrNone
    val stopShift = stop.baselineShiftOrNone
    val startTransform = start.extras?.textGeometricTransform ?: TextGeometricTransform.None
    val stopTransform = stop.extras?.textGeometricTransform ?: TextGeometricTransform.None
    return createSpanStyle(
        color =
            if (startBrush == null && stopBrush == null) {
                lerp(start.color, stop.color, fraction)
            } else {
                discreteStyleResolved.color
            },
        brush = discreteStyleResolved.brush,
        alpha =
            if (startBrush != null && stopBrush != null) {
                lerp(start.alpha, stop.alpha, fraction)
            } else {
                discreteStyleResolved.alpha
            },
        fontSize = lerpTextUnitInheritable(start.fontSize, stop.fontSize, fraction),
        fontWeight =
            if (startWeight == stopWeight) startWeight else lerp(startWeight, stopWeight, fraction),
        fontStyle = discreteStyleResolved.fontStyle,
        fontSynthesis = discreteStyleResolved.fontSynthesis,
        fontFamily = discreteStyleResolved.fontFamily,
        fontFeatureSettings = discreteStyleResolved.fontFeatureSettings,
        letterSpacing = lerpTextUnitInheritable(start.letterSpacing, stop.letterSpacing, fraction),
        baselineShift =
            if (startShift == stopShift) {
                start.baselineShift ?: stop.baselineShift ?: BoxedStyleValues.NoneBaselineShift
            } else {
                lerp(startShift, stopShift, fraction)
            },
        textGeometricTransform =
            if (startTransform == stopTransform) {
                startTransform
            } else {
                lerp(startTransform, stopTransform, fraction)
            },
        localeList = discreteStyleResolved.localeList,
        background = lerp(start.background, stop.background, fraction),
        textDecoration = discreteStyleResolved.textDecoration,
        shadow = nullSafeLerp(start.shadow, stop.shadow, fraction),
        platformStyle = lerpPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        drawStyle = discreteStyleResolved.drawStyle,
    )
}

/**
 * Linearly interpolates between two [Shadow]s.
 *
 * If one of the shadows is null, it is treated as a transparent shadow. If both are null, null is
 * returned.
 */
internal fun nullSafeLerp(lhs: Shadow?, rhs: Shadow?, fraction: Float): Shadow? {
    if (lhs == null && rhs == null) {
        return null
    }
    if (lhs == null) {
        return lerp(rhs!!.dropAlpha(), rhs, fraction)
    }
    if (rhs == null) {
        return lerp(lhs, lhs.dropAlpha(), fraction)
    }
    return lerp(lhs, rhs, fraction)
}

private fun Shadow.dropAlpha(): Shadow = copy(color = color.copy(alpha = 0f))

private fun lerpPlatformStyle(
    start: PlatformSpanStyle?,
    stop: PlatformSpanStyle?,
    fraction: Float,
): PlatformSpanStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformSpanStyle.Default
    val stopNonNull = stop ?: PlatformSpanStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}
