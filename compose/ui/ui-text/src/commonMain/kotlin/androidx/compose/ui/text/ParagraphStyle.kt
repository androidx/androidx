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
import androidx.compose.ui.text.internal.checkPrecondition
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.MAX_LINE_BREAK_STRATEGY_VALUE
import androidx.compose.ui.text.style.MAX_LINE_BREAK_STRICTNESS_VALUE
import androidx.compose.ui.text.style.MAX_LINE_BREAK_WORD_BREAK_VALUE
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.isSpecified
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

internal val DefaultParagraphStyle =
    if (PackedTextStylesLatch.isEnabled) {
        ParagraphStyle(0L, null, null)
    } else {
        ParagraphStyle(DefaultLegacyParagraphStyle)
    }

private fun defaultLegacyParagraphStyle(lineHeightStyle: LineHeightStyle?): LegacyParagraphStyle =
    if (lineHeightStyle == null) {
        DefaultLegacyParagraphStyle
    } else {
        LegacyParagraphStyle(lineHeightStyle = lineHeightStyle)
    }

/**
 * Rare [ParagraphStyle] fields that do not fit in the packed bits. Holds a custom [TextIndent], a
 * custom [PlatformParagraphStyle], or a custom [TextMotion].
 */
@Immutable
internal class ParagraphStyleExtras(
    @JvmField val textIndent: TextIndent?,
    @JvmField val platformStyle: PlatformParagraphStyle?,
    @JvmField val textMotion: TextMotion?,
) {
    @Suppress("NOTHING_TO_INLINE")
    inline fun matches(
        textIndent: TextIndent?,
        platformStyle: PlatformParagraphStyle?,
        textMotion: TextMotion?,
    ): Boolean =
        this.textIndent === textIndent &&
            this.platformStyle === platformStyle &&
            this.textMotion === textMotion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParagraphStyleExtras) return false
        return textIndent == other.textIndent &&
            platformStyle == other.platformStyle &&
            textMotion == other.textMotion
    }

    override fun hashCode(): Int {
        var result = textIndent?.hashCode() ?: 0
        result = 31 * result + (platformStyle?.hashCode() ?: 0)
        result = 31 * result + (textMotion?.hashCode() ?: 0)
        return result
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ParagraphStyle(
    bits: Long,
    lineHeightStyle: LineHeightStyle?,
    extras: ParagraphStyleExtras?,
): ParagraphStyle = ParagraphStyle(bits, lineHeightStyle, extras, null)

@Suppress("NOTHING_TO_INLINE")
internal inline fun ParagraphStyle(legacyParagraphStyle: LegacyParagraphStyle): ParagraphStyle =
    ParagraphStyle(
        0L,
        legacyParagraphStyle.lineHeightStyle,
        null,
        if (PackedTextStylesLatch.isEnabled) null else legacyParagraphStyle,
    )

/**
 * Paragraph styling configuration.
 *
 * Defines styling parameters that apply to a whole paragraph (e.g., alignment, line height).
 *
 * In contrast to [SpanStyle] which applies at character level, [ParagraphStyle] separates the
 * marked text into a new paragraph, as if a line feed character was inserted.
 *
 * @sample androidx.compose.ui.text.samples.ParagraphStyleSample
 * @sample androidx.compose.ui.text.samples.ParagraphStyleAnnotatedStringsSample
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushParagraphStyleSample
 * @property textAlign alignment of the text within the lines of the paragraph.
 * @property textDirection algorithm used to resolve the final text direction: Left To Right or
 *   Right To Left.
 * @property lineHeight line height
 * @property textIndent paragraph indentation
 * @property platformStyle platform-specific parameters
 * @property lineHeightStyle line height distribution configuration
 * @property lineBreak line breaking rules.
 * @property hyphens hyphenation configuration.
 * @property textMotion character placement optimization.
 * @see Paragraph
 * @see AnnotatedString
 * @see SpanStyle
 * @see TextStyle
 */
@Immutable
public class ParagraphStyle
internal constructor(
    bits: Long,
    public val lineHeightStyle: LineHeightStyle?,
    /**
     * Rare fields ([textIndent], [platformStyle], [textMotion]) that only make a
     * [ParagraphStyleExtras] object when set to a custom value. Preset values ([TextIndent.None],
     * [PlatformParagraphStyle.Default], [TextMotion.Static], [TextMotion.Animated]) live in [bits],
     * so [extras] stays `null`.
     */
    internal val extras: ParagraphStyleExtras?,
    internal val legacyParagraphStyle: LegacyParagraphStyle?,
) : AnnotatedString.Annotation {

    /**
     * 64-bit packed word for [ParagraphStyle] enum, flag, and preset fields.
     *
     * ### Bit Layout (`bits 0..63`, LSB first)
     *
     * ```
     *  63                            32  31  30 29    25 24    23 22    20 19    18  17  16 15   12 11    8 7     5 4    3 2    1   0
     * +--------------------------------+---+---+--------+--------+--------+--------+---+---+-------+-------+-------+------+------+---+
     * |     lineHeight.toRawBits()     |em |lh | unused |wordBrk |strictns|strategy|lb |ti |textDir|textAln|textMtn|platSt|hyphen|ext|
     * +--------------------------------+---+---+--------+--------+--------+--------+---+---+-------+-------+-------+------+------+---+
     *  <----------- 32 bits ---------->  1   1   5 bits   2 bits   3 bits   2 bits   1   1   4 bits  4 bits  3 bits 2 bits 2 bits  1
     *                                   <--- 34 bits --> <------------- 8 bits ------------>
     * ```
     *
     * |Bits    |Width|Field / Meaning                                                                |
     * |--------|-----|-------------------------------------------------------------------------------|
     * |`0`     |1    |[PARA_HAS_EXTRAS]: `1` iff [extras] `!= null` (enforced by primary constructor)|
     * |`1..2`  |2    |[hyphens]: `0` = Unspecified, `1` (`0b01`) = None, `3` (`0b11`) = Auto         |
     * |`3..4`  |2    |[platformStyle]: `0` = null, `1` (`0b01`) = Default, `3` (`0b11`) = in [extras]|
     * |`5..7`  |3    |[textMotion]: `0` = null, `1` = Static, `3` = Animated, `5` = in [extras]      |
     * |`8..11` |4    |[textAlign]: `0` = Unspecified, else `(value shl 1) or 1` (`value` in `1..6`)  |
     * |`12..15`|4    |[textDirection]: `0` = Unspecified, else `(value shl 1) or 1` (`value` `1..5`) |
     * |`16`    |1    |[HAS_TEXT_INDENT]: `1` if [textIndent] is set ([TextIndent.None] if no extras) |
     * |`17..24`|8    |[lineBreak]: bit 17 = set, `18..19` strategy, `20..22` strictness, `23..24` wb |
     * |`25..29`|5    |Unused (always `0`)                                                            |
     * |`30..63`|34   |[lineHeight]: bit 30 = set, bit 31 = `isEm`, `32..63` = `value.toRawBits()`    |
     *
     * ### Odd-Code Rule & SWAR Merge
     * Each field with more than one bit stores `0` when unset and an **odd** code (`low bit = 1`)
     * when set. In [merge], [mergeParagraphBits] grabs the low bit of each field in `other`. It
     * then scales that bit by `(1 shl width) - 1` (`0x3`, `0x7`, `0xF`, `0xFF`) to build a full
     * field mask in one pass.
     */
    internal val bits: Long =
        if (extras == null) bits and PARA_HAS_EXTRAS.inv() else bits or PARA_HAS_EXTRAS

    public constructor() :
        this(
            0L,
            null,
            null,
            if (PackedTextStylesLatch.isEnabled) null else defaultLegacyParagraphStyle(null),
        )

    /**
     * @param textAlign alignment of the text within the lines of the paragraph.
     * @param textDirection algorithm used to resolve the final text direction: Left To Right or
     *   Right To Left.
     * @param lineHeight line height
     * @param textIndent paragraph indentation
     * @param platformStyle platform-specific parameters
     * @param lineHeightStyle line height distribution configuration
     * @param lineBreak line breaking rules.
     * @param hyphens hyphenation configuration.
     * @param textMotion character placement optimization.
     */
    public constructor(
        textAlign: TextAlign = TextAlign.Unspecified,
        textDirection: TextDirection = TextDirection.Unspecified,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak = LineBreak.Unspecified,
        hyphens: Hyphens = Hyphens.Unspecified,
        textMotion: TextMotion? = null,
    ) : this(
        bits =
            if (PackedTextStylesLatch.isEnabled) {
                packParagraphStyleBits(
                    textAlign,
                    textDirection,
                    lineHeight,
                    textIndent,
                    platformStyle,
                    lineBreak,
                    hyphens,
                    textMotion,
                )
            } else {
                0L
            },
        lineHeightStyle = lineHeightStyle,
        extras =
            if (PackedTextStylesLatch.isEnabled) {
                createParagraphStyleExtras(textIndent, platformStyle, textMotion)
            } else {
                null
            },
        legacyParagraphStyle =
            if (PackedTextStylesLatch.isEnabled) {
                null
            } else {
                LegacyParagraphStyle(
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
            },
    )

    public val textAlign: TextAlign
        get() =
            if (legacyParagraphStyle != null) {
                legacyParagraphStyle.textAlign
            } else {
                TextAlign(unpackParagraphField(bits, TEXT_ALIGN_MASK, TEXT_ALIGN_SHIFT))
            }

    public val textDirection: TextDirection
        get() =
            if (legacyParagraphStyle != null) {
                legacyParagraphStyle.textDirection
            } else {
                TextDirection(unpackParagraphField(bits, TEXT_DIRECTION_MASK, TEXT_DIRECTION_SHIFT))
            }

    public val lineHeight: TextUnit
        get() {
            if (legacyParagraphStyle != null) return legacyParagraphStyle.lineHeight
            val bits = bits
            if (bits and HAS_LINE_HEIGHT == 0L) return TextUnit.Unspecified
            val value = Float.fromBits((bits ushr LINE_HEIGHT_VALUE_SHIFT).toInt())
            return if (bits and LINE_HEIGHT_IS_EM != 0L) value.em else value.sp
        }

    public val textIndent: TextIndent?
        get() =
            if (legacyParagraphStyle != null) {
                legacyParagraphStyle.textIndent
            } else {
                extras?.textIndent ?: if (bits and HAS_TEXT_INDENT != 0L) TextIndent.None else null
            }

    public val platformStyle: PlatformParagraphStyle?
        get() =
            if (legacyParagraphStyle != null) {
                legacyParagraphStyle.platformStyle
            } else {
                extras?.platformStyle
                    ?: if (bits and PLATFORM_STYLE_MASK != 0L) PlatformParagraphStyle.Default
                    else null
            }

    public val lineBreak: LineBreak
        get() {
            if (legacyParagraphStyle != null) return legacyParagraphStyle.lineBreak
            val bits = bits
            if (bits and LINE_BREAK_MASK == 0L) return LineBreak.Unspecified
            val strategy =
                (bits ushr LINE_BREAK_STRATEGY_SHIFT).toInt() and LINE_BREAK_STRATEGY_MASK
            val strictness =
                (bits ushr LINE_BREAK_STRICTNESS_SHIFT).toInt() and LINE_BREAK_STRICTNESS_MASK
            val wordBreak =
                (bits ushr LINE_BREAK_WORD_BREAK_SHIFT).toInt() and LINE_BREAK_WORD_BREAK_MASK
            return LineBreak(
                strategy or
                    (strictness shl LINE_BREAK_UNPACKED_STRICTNESS_SHIFT) or
                    (wordBreak shl LINE_BREAK_UNPACKED_WORD_BREAK_SHIFT)
            )
        }

    public val hyphens: Hyphens
        get() {
            if (legacyParagraphStyle != null) return legacyParagraphStyle.hyphens
            val code = (bits and HYPHENS_MASK).toInt()
            // Codes 1 and 3 are None (1) and Auto (2).
            return if (code == 0) Hyphens.Unspecified
            else Hyphens((code ushr (HYPHENS_SHIFT + 1)) + 1)
        }

    public val textMotion: TextMotion?
        get() =
            if (legacyParagraphStyle != null) {
                legacyParagraphStyle.textMotion
            } else {
                extras?.textMotion
                    ?: when (bits and TEXT_MOTION_MASK) {
                        0L -> null
                        ANIMATED_MOTION -> TextMotion.Animated
                        else -> TextMotion.Static
                    }
            }

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getTextAlign-buA522U") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    public val deprecated_boxing_textAlign: TextAlign?
        get() = this.textAlign

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getTextDirection-mmuk1to") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    public val deprecated_boxing_textDirection: TextDirection?
        get() = this.textDirection

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getHyphens-EaSxIns") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    public val deprecated_boxing_hyphens: Hyphens?
        get() = this.hyphens

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getLineBreak-LgCVezo") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    public val deprecated_boxing_lineBreak: LineBreak?
        get() = this.lineBreak

    @Deprecated(
        "ParagraphStyle constructors that take nullable TextAlign, " +
            "TextDirection, LineBreak, and Hyphens are deprecated. Please use a new constructor " +
            "where these parameters are non-nullable. Null value has been replaced by a special " +
            "Unspecified object for performance reason.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak? = null,
        hyphens: Hyphens? = null,
        textMotion: TextMotion? = null,
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = lineBreak ?: LineBreak.Unspecified,
        hyphens = hyphens ?: Hyphens.Unspecified,
        textMotion = textMotion,
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = null,
        lineHeightStyle = null,
        lineBreak = LineBreak.Unspecified,
        hyphens = Hyphens.Unspecified,
        textMotion = null,
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "constructors.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = LineBreak.Unspecified,
        hyphens = Hyphens.Unspecified,
        textMotion = null,
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineBreak, Hyphens, TextMotion are deprecated. Please use the new stable " +
            "constructors.",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak? = null,
        hyphens: Hyphens? = null,
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = lineBreak ?: LineBreak.Unspecified,
        hyphens = hyphens ?: Hyphens.Unspecified,
        textMotion = null,
    )

    // `extras == null` does not need a separate check here: the primary constructor forces bit 0
    // (`PARA_HAS_EXTRAS`) of `bits` to 1 whenever `extras != null` (lines 111-113), so `bits == 0L`
    // guarantees `extras == null` without a second field read.
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun isDefaultFast(): Boolean = bits == 0L && lineHeightStyle == null

    internal fun isDefault(): Boolean {
        if (legacyParagraphStyle != null) {
            return legacyParagraphStyle === DefaultLegacyParagraphStyle ||
                legacyParagraphStyle == DefaultLegacyParagraphStyle
        }
        return isDefaultFast()
    }

    /**
     * Returns a new paragraph style that is a combination of this style and the given [other]
     * style.
     *
     * If the given paragraph style is null, returns this paragraph style.
     *
     * @param other style to merge
     */
    @Stable
    public fun merge(other: ParagraphStyle? = null): ParagraphStyle {
        if (other == null || other === this) return this
        if (legacyParagraphStyle != null) return mergeLegacy(other)
        if (other.isDefaultFast()) return this
        if (isDefaultFast()) return other
        val otherBits = other.bits
        val bits = mergeParagraphBits(this.bits, otherBits)
        val lineHeightStyle = other.lineHeightStyle ?: lineHeightStyle
        val thisExtras = extras
        val otherExtras = other.extras
        var extras: ParagraphStyleExtras? = null
        if ((this.bits or otherBits) and PARA_HAS_EXTRAS != 0L) {
            // A field set in `other` wins, even when its value is a preset that needs no extras.
            val textIndent =
                if (otherBits and HAS_TEXT_INDENT != 0L) otherExtras?.textIndent
                else thisExtras?.textIndent
            val platformStyle =
                if (otherBits and PLATFORM_STYLE_MASK != 0L) otherExtras?.platformStyle
                else thisExtras?.platformStyle
            val textMotion =
                if (otherBits and TEXT_MOTION_MASK != 0L) otherExtras?.textMotion
                else thisExtras?.textMotion
            extras =
                when {
                    thisExtras != null &&
                        thisExtras.matches(textIndent, platformStyle, textMotion) -> thisExtras
                    otherExtras != null &&
                        otherExtras.matches(textIndent, platformStyle, textMotion) -> otherExtras
                    else -> createParagraphStyleExtras(textIndent, platformStyle, textMotion)
                }
        }
        if (sameAs(bits, lineHeightStyle, extras)) return this
        if (other.sameAs(bits, lineHeightStyle, extras)) return other
        return ParagraphStyle(bits, lineHeightStyle, extras)
    }

    private fun mergeLegacy(other: ParagraphStyle): ParagraphStyle {
        val legacy = legacyParagraphStyle!!
        val mergedLegacy = legacy.merge(other.legacyParagraphStyle)
        if (mergedLegacy === legacy) return this
        if (mergedLegacy === other.legacyParagraphStyle) return other
        return ParagraphStyle(mergedLegacy)
    }

    internal fun fastMerge(
        textAlign: TextAlign,
        textDirection: TextDirection,
        lineHeight: TextUnit,
        textIndent: TextIndent?,
        platformStyle: PlatformParagraphStyle?,
        lineHeightStyle: LineHeightStyle?,
        lineBreak: LineBreak,
        hyphens: Hyphens,
        textMotion: TextMotion?,
    ): ParagraphStyle {
        if (legacyParagraphStyle != null) {
            return fastMergeLegacy(
                textAlign,
                textDirection,
                lineHeight,
                textIndent,
                platformStyle,
                lineHeightStyle,
                lineBreak,
                hyphens,
                textMotion,
            )
        }
        // prioritize the parameters to Text in diffs here
        /** textAlign: TextAlign? lineHeight: TextUnit */
        // any new vals should do a pre-merge check here
        // Fast path for callers (such as BasicText / Material Text) that only override `lineHeight`
        // (or pass all default/unspecified paragraph arguments). Guards against running the full
        // 8-field bit-pack, mask expansion, and extras merge when no other paragraph field is set.
        val onlyLineHeightMayChange =
            textAlign == TextAlign.Unspecified &&
                textDirection == TextDirection.Unspecified &&
                textIndent == null &&
                platformStyle == null &&
                lineHeightStyle == null &&
                lineBreak == LineBreak.Unspecified &&
                hyphens == Hyphens.Unspecified &&
                textMotion == null
        if (onlyLineHeightMayChange) {
            if (lineHeight.isUnspecified) return this
            val otherLineHeightBits = packLineHeightBits(lineHeight)
            // Keep this style's existing non-lineHeight packed fields (bits 1..29, masked by
            // NON_LINE_HEIGHT_BITS_MASK which also clears bit 0 / PARA_HAS_EXTRAS) and replace
            // bits 30..63 with the new `otherLineHeightBits`. Because only `lineHeight` changed,
            // `this.lineHeightStyle` and `this.extras` are reused unchanged.
            val bits = (this.bits and NON_LINE_HEIGHT_BITS_MASK) or otherLineHeightBits
            if (sameAs(bits, this.lineHeightStyle, this.extras)) return this
            return ParagraphStyle(bits, this.lineHeightStyle, this.extras)
        }
        val otherBits =
            packParagraphStyleBits(
                textAlign,
                textDirection,
                lineHeight,
                textIndent,
                platformStyle,
                lineBreak,
                hyphens,
                textMotion,
            )
        val thisBits = this.bits
        val bits = mergeParagraphBits(thisBits, otherBits)
        val mergedLineHeightStyle = lineHeightStyle ?: this.lineHeightStyle
        val extras =
            if (bits and (HAS_TEXT_INDENT or PLATFORM_STYLE_MASK or TEXT_MOTION_MASK) == 0L) {
                null
            } else {
                mergeParagraphStyleExtras(this.extras, textIndent, platformStyle, textMotion)
            }
        if (sameAs(bits, mergedLineHeightStyle, extras)) return this
        return ParagraphStyle(bits, mergedLineHeightStyle, extras)
    }

    private fun fastMergeLegacy(
        textAlign: TextAlign,
        textDirection: TextDirection,
        lineHeight: TextUnit,
        textIndent: TextIndent?,
        platformStyle: PlatformParagraphStyle?,
        lineHeightStyle: LineHeightStyle?,
        lineBreak: LineBreak,
        hyphens: Hyphens,
        textMotion: TextMotion?,
    ): ParagraphStyle {
        val legacy = legacyParagraphStyle!!
        val mergedLegacy =
            legacy.fastMerge(
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
        return if (mergedLegacy === legacy) this else ParagraphStyle(mergedLegacy)
    }

    internal fun withResolvedDefaults(direction: LayoutDirection): ParagraphStyle {
        if (legacyParagraphStyle != null) {
            return ParagraphStyle(
                resolveLegacyParagraphStyleDefaults(legacyParagraphStyle, direction)
            )
        }
        val resolved = resolveParagraphBits(bits, direction)
        return if (resolved == bits) this else ParagraphStyle(resolved, lineHeightStyle, extras)
    }

    /** Plus operator overload that applies a [merge]. */
    @Stable public operator fun plus(other: ParagraphStyle): ParagraphStyle = this.merge(other)

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
    ): ParagraphStyle {
        return copy(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = this.platformStyle,
            lineHeightStyle = this.lineHeightStyle,
            lineBreak = this.lineBreak,
            hyphens = this.hyphens,
            textMotion = this.textMotion,
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
    ): ParagraphStyle {
        return copy(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = this.lineBreak,
            hyphens = this.hyphens,
            textMotion = this.textMotion,
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineBreak, Hyphens, TextMotion are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak? = this.lineBreak,
        hyphens: Hyphens? = this.hyphens,
    ): ParagraphStyle {
        return copy(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak ?: LineBreak.Unspecified,
            hyphens = hyphens ?: Hyphens.Unspecified,
            textMotion = this.textMotion,
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that take nullable TextAlign, " +
            "TextDirection, LineBreak, and Hyphens are deprecated. Please use a new constructor " +
            "where these parameters are non-nullable. Null value has been replaced by a special " +
            "Unspecified object for performance reason.",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak? = this.lineBreak,
        hyphens: Hyphens? = this.hyphens,
        textMotion: TextMotion? = this.textMotion,
    ): ParagraphStyle {
        return copy(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak ?: LineBreak.Unspecified,
            hyphens = hyphens ?: Hyphens.Unspecified,
            textMotion = textMotion,
        )
    }

    public fun copy(
        textAlign: TextAlign = this.textAlign,
        textDirection: TextDirection = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak = this.lineBreak,
        hyphens: Hyphens = this.hyphens,
        textMotion: TextMotion? = this.textMotion,
    ): ParagraphStyle {
        if (legacyParagraphStyle != null) {
            return ParagraphStyle(
                legacyParagraphStyle.copy(
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
            )
        }
        return ParagraphStyle(
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

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParagraphStyle) return false
        if (legacyParagraphStyle != null) return legacyParagraphStyle == other.legacyParagraphStyle
        return bits == other.bits &&
            lineHeightStyle == other.lineHeightStyle &&
            extras == other.extras
    }

    public override fun hashCode(): Int {
        if (legacyParagraphStyle != null) return legacyParagraphStyle.hashCode()
        var result = mixLongHash(bits)
        result = 31 * result + (lineHeightStyle?.hashCode() ?: 0)
        result = 31 * result + (extras?.hashCode() ?: 0)
        return murmurFinalize32(result)
    }

    /**
     * Helper used by [merge], [fastMerge], and [withResolvedDefaults] to check if the new parts
     * ([bits], [lineHeightStyle], [extras]) match `this` (or `other`) before making a new
     * [ParagraphStyle] object.
     *
     * Unlike [equals] (which checks two [ParagraphStyle] objects with `==` on `lineHeightStyle` and
     * `extras`), [sameAs]:
     * 1. Takes raw parts ([bits], [lineHeightStyle], [extras]) so callers can reuse an old
     *    [ParagraphStyle] object without making a new one first.
     * 2. Clears bit 0 ([PARA_HAS_EXTRAS]) on `this.bits`. [mergeParagraphBits] and
     *    [packParagraphStyleBits] return raw field bits with bit 0 cleared, while [ParagraphStyle]
     *    sets [PARA_HAS_EXTRAS] when `extras != null`.
     * 3. Uses `===` on [lineHeightStyle] and [extras] since [merge], [fastMerge], and
     *    [withResolvedDefaults] reuse those objects when their fields do not change.
     */
    private fun sameAs(
        bits: Long,
        lineHeightStyle: LineHeightStyle?,
        extras: ParagraphStyleExtras?,
    ): Boolean =
        this.bits and PARA_HAS_EXTRAS.inv() == bits &&
            this.lineHeightStyle === lineHeightStyle &&
            this.extras === extras

    public override fun toString(): String {
        if (legacyParagraphStyle != null) return legacyParagraphStyle.toString()
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

// ParagraphStyle.bits layout, low bit first. A zero field means "unset". Every multi-bit field
// stores an odd code, so its low bit doubles as "set", and merge builds its field mask with a
// single multiply.
//   0       has extras (matches `extras != null`)
//   1..2    hyphens          1 = None, 3 = Auto
//   3..4    platformStyle    1 = Default, 3 = custom value in extras
//   5..7    textMotion       1 = Static, 3 = Animated, 5 = custom value in extras
//   8..11   textAlign        (value shl 1) or 1
//   12..15  textDirection    (value shl 1) or 1
//   16      textIndent set; TextIndent.None unless extras has one
//   17..24  lineBreak        bit 17 = set, 18..19 strategy, 20..22 strictness, 23..24 word break
//   30..63  lineHeight       bit 30 = set, bit 31 = em, 32..63 = raw float bits
private const val PARA_HAS_EXTRAS = 1L
private const val TWO_BIT_FIELD_MASK = 0x3L
private const val THREE_BIT_FIELD_MASK = 0x7L
private const val FOUR_BIT_FIELD_MASK = 0xFL
private const val LINE_BREAK_FIELD_MASK = 0xFFL
private const val HYPHENS_SHIFT = 1
private const val HYPHENS_MASK = TWO_BIT_FIELD_MASK shl HYPHENS_SHIFT
private const val HYPHENS_VALUE_BITS = 1
private const val HYPHENS_VALUE_MASK = (1 shl HYPHENS_VALUE_BITS) - 1
private const val HYPHENS_MAX_VALUE = 1 shl HYPHENS_VALUE_BITS
private const val PLATFORM_STYLE_MASK = TWO_BIT_FIELD_MASK shl 3
private const val DEFAULT_PLATFORM_STYLE = 0x1L shl 3
private const val CUSTOM_PLATFORM_STYLE = 0x3L shl 3
private const val TEXT_MOTION_SHIFT = 5
private const val TEXT_MOTION_MASK = THREE_BIT_FIELD_MASK shl TEXT_MOTION_SHIFT
private const val ANIMATED_MOTION = 0x3L shl TEXT_MOTION_SHIFT
private const val CUSTOM_MOTION = 0x5L shl TEXT_MOTION_SHIFT
private const val TEXT_ALIGN_SHIFT = 8
private const val TEXT_ALIGN_MASK = FOUR_BIT_FIELD_MASK shl TEXT_ALIGN_SHIFT
private const val TEXT_ALIGN_VALUE_BITS = 3
private const val TEXT_ALIGN_MAX_RAW = (1 shl TEXT_ALIGN_VALUE_BITS) - 1
private const val TEXT_DIRECTION_SHIFT = 12
private const val TEXT_DIRECTION_MASK = FOUR_BIT_FIELD_MASK shl TEXT_DIRECTION_SHIFT
private const val TEXT_DIRECTION_VALUE_BITS = 3
private const val TEXT_DIRECTION_MAX_RAW = (1 shl TEXT_DIRECTION_VALUE_BITS) - 1
private const val HAS_TEXT_INDENT = 1L shl 16
private const val LINE_BREAK_SHIFT = 17
private const val LINE_BREAK_MASK = LINE_BREAK_FIELD_MASK shl LINE_BREAK_SHIFT
private const val LINE_BREAK_STRATEGY_SHIFT = 18
private const val LINE_BREAK_STRATEGY_BITS = 2
private const val LINE_BREAK_STRATEGY_MASK = (1 shl LINE_BREAK_STRATEGY_BITS) - 1
private const val LINE_BREAK_STRICTNESS_SHIFT = 20
private const val LINE_BREAK_STRICTNESS_BITS = 3
private const val LINE_BREAK_STRICTNESS_MASK = (1 shl LINE_BREAK_STRICTNESS_BITS) - 1
private const val LINE_BREAK_WORD_BREAK_SHIFT = 23
private const val LINE_BREAK_WORD_BREAK_BITS = 2
private const val LINE_BREAK_WORD_BREAK_MASK = (1 shl LINE_BREAK_WORD_BREAK_BITS) - 1
private const val LINE_BREAK_PACKED_STRATEGY_SHIFT = 1
private const val LINE_BREAK_PACKED_STRICTNESS_SHIFT = 3
private const val LINE_BREAK_PACKED_WORD_BREAK_SHIFT = 6
private const val LINE_BREAK_UNPACKED_STRICTNESS_SHIFT = 8
private const val LINE_BREAK_UNPACKED_WORD_BREAK_SHIFT = 16
// LineBreak.Simple: set, strategy 1 (Simple), strictness 3 (Normal), word break 1 (Default).
private const val LINE_BREAK_SIMPLE = 0x5BL shl LINE_BREAK_SHIFT
private const val LINE_HEIGHT_SHIFT = 30
private const val HAS_LINE_HEIGHT = 1L shl LINE_HEIGHT_SHIFT
private const val LINE_HEIGHT_IS_EM = 1L shl 31
private const val LINE_HEIGHT_VALUE_SHIFT = 32
// Bits 1..29: all packed ParagraphStyle fields except bit 0 (PARA_HAS_EXTRAS) and bits 30..63
// (lineHeight).
private const val NON_LINE_HEIGHT_BITS_MASK = 0x3FFF_FFFEL

/**
 * Knuth 64-bit Fibonacci multiplier `0x9E3779B97F4A7C15UL` (`floor(2^64 / phi)`, odd, with inverse
 * `0xF1DE83E19937733DUL` mod 2^64). Multiplying a 64-bit word by [GOLDEN_RATIO_64] before folding
 * `(mixed xor (mixed ushr 32)).toInt()` diffuses low-half bits into the high 32-bit half,
 * preventing aligned bits `b` and `b + 32` in the input word from cancelling under the 32-bit XOR
 * fold.
 */
internal const val GOLDEN_RATIO_64 = -0x61c8864680b583ebL

/** Mixes a 64-bit word with [GOLDEN_RATIO_64] and folds it to 32 bits. */
@Suppress("NOTHING_TO_INLINE")
internal inline fun mixLongHash(bits: Long): Int {
    val mixed = bits * GOLDEN_RATIO_64
    return (mixed xor (mixed ushr 32)).toInt()
}

/** Mixes all bits of [hash] with the 32-bit Murmur3 tail mix. */
@Suppress("NOTHING_TO_INLINE")
internal inline fun murmurFinalize32(hash: Int): Int {
    var h = hash
    h = h xor (h ushr 16)
    h *= -0x7a143595 // 0x85ebca6b
    h = h xor (h ushr 13)
    h *= -0x3d4d51cb // 0xc2b2ae35
    h = h xor (h ushr 16)
    return h
}

private const val TWO_BIT_LOW_BITS = (1L shl 1) or (1L shl 3)
private const val THREE_BIT_LOW_BITS = 1L shl TEXT_MOTION_SHIFT
private const val FOUR_BIT_LOW_BITS = (1L shl TEXT_ALIGN_SHIFT) or (1L shl TEXT_DIRECTION_SHIFT)
private const val LINE_BREAK_LOW_BIT = 1L shl LINE_BREAK_SHIFT

// Fails with `Division by zero` if Hyphens.MAX_HYPHENS_VALUE exceeds HYPHENS_MAX_VALUE (2).
// Widen HYPHENS_VALUE_BITS and HYPHENS_MASK if a new Hyphens value is added.
@Suppress("unused")
private const val CHECK_HYPHENS_CAPACITY =
    1 / (1 - ((HYPHENS_MAX_VALUE - Hyphens.MAX_HYPHENS_VALUE) ushr 31))

// Fails with `Division by zero` if TextAlign.MAX_TEXT_ALIGN_VALUE exceeds TEXT_ALIGN_MAX_RAW (7).
// Widen TEXT_ALIGN_VALUE_BITS and TEXT_ALIGN_MASK if a new TextAlign value is added.
@Suppress("unused")
private const val CHECK_TEXT_ALIGN_CAPACITY =
    1 / (1 - ((TEXT_ALIGN_MAX_RAW - TextAlign.MAX_TEXT_ALIGN_VALUE) ushr 31))

// Fails with `Division by zero` if TextDirection.MAX_TEXT_DIRECTION_VALUE exceeds
// TEXT_DIRECTION_MAX_RAW (7).
// Widen TEXT_DIRECTION_VALUE_BITS and TEXT_DIRECTION_MASK if a new TextDirection value is added.
@Suppress("unused")
private const val CHECK_TEXT_DIRECTION_CAPACITY =
    1 / (1 - ((TEXT_DIRECTION_MAX_RAW - TextDirection.MAX_TEXT_DIRECTION_VALUE) ushr 31))

// Fails with `Division by zero` if MAX_LINE_BREAK_STRATEGY_VALUE exceeds
// LINE_BREAK_STRATEGY_MASK (3).
// Widen LINE_BREAK_STRATEGY_BITS and LINE_BREAK_MASK if a new LineBreak.Strategy is added.
@Suppress("unused")
private const val CHECK_LINE_BREAK_STRATEGY_CAPACITY =
    1 / (1 - ((LINE_BREAK_STRATEGY_MASK - MAX_LINE_BREAK_STRATEGY_VALUE) ushr 31))

// Fails with `Division by zero` if MAX_LINE_BREAK_STRICTNESS_VALUE exceeds
// LINE_BREAK_STRICTNESS_MASK (7).
// Widen LINE_BREAK_STRICTNESS_BITS and LINE_BREAK_MASK if a new LineBreak.Strictness is added.
@Suppress("unused")
private const val CHECK_LINE_BREAK_STRICTNESS_CAPACITY =
    1 / (1 - ((LINE_BREAK_STRICTNESS_MASK - MAX_LINE_BREAK_STRICTNESS_VALUE) ushr 31))

// Fails with `Division by zero` if MAX_LINE_BREAK_WORD_BREAK_VALUE exceeds
// LINE_BREAK_WORD_BREAK_MASK (3).
// Widen LINE_BREAK_WORD_BREAK_BITS and LINE_BREAK_MASK if a new LineBreak.WordBreak is added.
@Suppress("unused")
private const val CHECK_LINE_BREAK_WORD_BREAK_CAPACITY =
    1 / (1 - ((LINE_BREAK_WORD_BREAK_MASK - MAX_LINE_BREAK_WORD_BREAK_VALUE) ushr 31))

/**
 * Unpacks an odd-code `Int` field (`(value shl 1) or 1` when set, `0` when unset) from [bits] using
 * its field [mask] and low-bit [shift].
 *
 * Bit `shift` is the set flag (`1` when set, `0` when unset) and the value starts at bit `shift +
 * 1`. Shifting right by `shift + 1` after masking yields `value` when set and `0` (`Unspecified`)
 * when unset.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun unpackParagraphField(bits: Long, mask: Long, shift: Int): Int =
    ((bits and mask) ushr (shift + 1)).toInt()

/**
 * Merges all 8 packed [ParagraphStyle] fields from [other] into [base] in one pass (`O(1)` bit ops
 * instead of 8 branches).
 *
 * Each field in `ParagraphStyle.bits` stores an odd code (`(payload shl 1) or 1`) when set and `0`
 * when unset. Thus the low bit of each field is `1` if and only if that field is set in [other].
 * Scaling those low bits by `(1 shl width) - 1` turns each `1` bit into a full mask of `1`s across
 * that field:
 * - 2-bit fields (`hyphens`, `platformStyle`): `(other and TWO_BIT_LOW_BITS) * TWO_BIT_FIELD_MASK`
 * - 3-bit field (`textMotion`): `(other and THREE_BIT_LOW_BITS) * THREE_BIT_FIELD_MASK`
 * - 4-bit fields (`textAlign`, `textDirection`): `(other and FOUR_BIT_LOW_BITS) *
 *   FOUR_BIT_FIELD_MASK`
 * - 8-bit field (`lineBreak`): `(other and LINE_BREAK_LOW_BIT) * LINE_BREAK_FIELD_MASK`
 * - 1-bit field (`textIndent`): `other and HAS_TEXT_INDENT`
 * - 34-bit field (`lineHeight`, bits 30..63): `-((other ushr 30) and 1L)` gives
 *   `0xFFFF_FFFF_FFFF_FFFFL` when set or `0L` when unset; shifting left by 30 masks bits 30..63.
 *
 * Bit 0 ([PARA_HAS_EXTRAS]) is cleared on return and set again by [ParagraphStyle] when `extras` is
 * known.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun mergeParagraphBits(base: Long, other: Long): Long {
    val mask =
        ((other and TWO_BIT_LOW_BITS) * TWO_BIT_FIELD_MASK) or
            ((other and THREE_BIT_LOW_BITS) * THREE_BIT_FIELD_MASK) or
            ((other and FOUR_BIT_LOW_BITS) * FOUR_BIT_FIELD_MASK) or
            ((other and LINE_BREAK_LOW_BIT) * LINE_BREAK_FIELD_MASK) or
            (other and HAS_TEXT_INDENT) or
            (-((other ushr LINE_HEIGHT_SHIFT) and 1L) shl LINE_HEIGHT_SHIFT)
    return ((base and mask.inv()) or (other and mask)) and PARA_HAS_EXTRAS.inv()
}

/**
 * Resolves all unset fields in [bits] to their concrete layout defaults for [direction] directly in
 * packed bit form, avoiding unpacking and re-packing 8 individual properties during
 * [resolveParagraphStyleDefaults].
 *
 * Specifically:
 * - `textIndent`: sets [HAS_TEXT_INDENT] (`1L shl 16`). If `extras?.textIndent` is `null`,
 *   [ParagraphStyle.textIndent] decodes [HAS_TEXT_INDENT] with `extras == null` as
 *   [TextIndent.None].
 * - `hyphens`: if unset (`0`), sets code `1` (`1L shl HYPHENS_SHIFT` = [Hyphens.None]).
 * - `textMotion`: if unset (`0`), sets code `1` (`1L shl TEXT_MOTION_SHIFT` = [TextMotion.Static]).
 * - `textAlign`: if unset (`0`), packs [TextAlign.Start].
 * - `textDirection`: resolves [TextDirection.Unspecified] (`0`) to [TextDirection.Ltr] (`1`) or
 *   [TextDirection.Rtl] (`2`), and resolves [TextDirection.Content] (`3`) to
 *   [TextDirection.ContentOrLtr] (`4`) or [TextDirection.ContentOrRtl] (`5`) based on [direction]
 *   (`resolvedDirection = textDirection + if (Ltr) 1 else 2`).
 * - `lineBreak`: if unset (`0`), sets [LINE_BREAK_SIMPLE] ([LineBreak.Simple]).
 * - `lineHeight` and `platformStyle` have no layout-time bit fallback (`lineHeight` remains
 *   [TextUnit.Unspecified] when unset, and `platformStyle` remains `null`).
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun resolveParagraphBits(bits: Long, direction: LayoutDirection): Long {
    var resolved = bits or HAS_TEXT_INDENT
    if (bits and HYPHENS_MASK == 0L) resolved = resolved or (1L shl HYPHENS_SHIFT)
    if (bits and TEXT_MOTION_MASK == 0L) resolved = resolved or (1L shl TEXT_MOTION_SHIFT)
    if (bits and TEXT_ALIGN_MASK == 0L) {
        resolved = resolved or ((TextAlign.Start.value * 2L + 1) shl TEXT_ALIGN_SHIFT)
    }
    // Unspecified becomes Ltr or Rtl, and Content becomes ContentOrLtr or ContentOrRtl.
    val textDirection = unpackParagraphField(bits, TEXT_DIRECTION_MASK, TEXT_DIRECTION_SHIFT)
    if (textDirection == 0 || textDirection == TextDirection.Content.value) {
        val resolvedDirection = textDirection + if (direction == LayoutDirection.Ltr) 1 else 2
        resolved =
            (resolved and TEXT_DIRECTION_MASK.inv()) or
                ((resolvedDirection * 2L + 1) shl TEXT_DIRECTION_SHIFT)
    }
    if (bits and LINE_BREAK_MASK == 0L) resolved = resolved or LINE_BREAK_SIMPLE
    return resolved
}

/**
 * Packs a set [lineHeight] into bits 30..63 of `ParagraphStyle.bits`, or returns `0L` when
 * [lineHeight] is [TextUnit.Unspecified].
 *
 * Checks that [lineHeight] is not less than zero via [checkPrecondition]. Callers can build a
 * [TextUnit] with any float (and `TextUnit(Float.NaN)` has `isSpecified == true` and fails
 * `value >= 0f` just like values below zero).
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun packLineHeightBits(lineHeight: TextUnit): Long {
    if (lineHeight.isUnspecified) return 0L
    // Since we are checking if it's negative, no need to convert Sp into Px at this point.
    val value = lineHeight.value
    checkPrecondition(value >= 0f) { "lineHeight can't be negative ($value)" }
    val unit = if (lineHeight.isEm) HAS_LINE_HEIGHT or LINE_HEIGHT_IS_EM else HAS_LINE_HEIGHT
    return unit or (value.toRawBits().toLong() shl LINE_HEIGHT_VALUE_SHIFT)
}

/**
 * Packs [ParagraphStyle] fields into the 64-bit `bits` word. Leaves bit 0 ([PARA_HAS_EXTRAS]) at
 * `0`; [ParagraphStyle] sets bit 0 when `extras` is not `null`.
 */
internal fun packParagraphStyleBits(
    textAlign: TextAlign,
    textDirection: TextDirection,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    lineBreak: LineBreak,
    hyphens: Hyphens,
    textMotion: TextMotion?,
): Long {
    var bits = 0L
    // Hyphens.value is in 0..MAX_HYPHENS_VALUE (0 = Unspecified, 1 = None, 2 = Auto); capacity is
    // locked at compile time by CHECK_HYPHENS_CAPACITY and tested by StyleDomainLockTest.
    val hyphensValue = hyphens.value
    if (hyphensValue != 0) {
        bits =
            (((hyphensValue - 1) and HYPHENS_VALUE_MASK).toLong() shl (HYPHENS_SHIFT + 1)) or
                (1L shl HYPHENS_SHIFT)
    }
    if (platformStyle != null) {
        bits =
            bits or
                if (platformStyle == PlatformParagraphStyle.Default) DEFAULT_PLATFORM_STYLE
                else CUSTOM_PLATFORM_STYLE
    }
    if (textMotion != null) {
        bits =
            bits or
                when (textMotion) {
                    TextMotion.Static -> 1L shl TEXT_MOTION_SHIFT
                    TextMotion.Animated -> ANIMATED_MOTION
                    else -> CUSTOM_MOTION
                }
    }
    // TextAlign.value is in 0..MAX_TEXT_ALIGN_VALUE (0 = Unspecified, 1..6 = Left..End); capacity
    // is locked at compile time by CHECK_TEXT_ALIGN_CAPACITY and tested by StyleDomainLockTest.
    val align = textAlign.value
    if (align != 0) {
        bits =
            bits or ((((align and TEXT_ALIGN_MAX_RAW).toLong() shl 1) or 1L) shl TEXT_ALIGN_SHIFT)
    }
    // TextDirection.value is in 0..MAX_TEXT_DIRECTION_VALUE (0 = Unspecified, 1..5 =
    // Ltr..ContentOrRtl); capacity is locked at compile time by CHECK_TEXT_DIRECTION_CAPACITY and
    // tested by StyleDomainLockTest.
    val direction = textDirection.value
    if (direction != 0) {
        bits =
            bits or
                ((((direction and TEXT_DIRECTION_MAX_RAW).toLong() shl 1) or 1L) shl
                    TEXT_DIRECTION_SHIFT)
    }
    if (textIndent != null) bits = bits or HAS_TEXT_INDENT
    // LineBreak packs Strategy (byte 0, 0..3), Strictness (byte 1, 0..4), and WordBreak (byte 2,
    // 0..2); capacities are locked by CHECK_LINE_BREAK_*_CAPACITY and tested by
    // StyleDomainLockTest.
    val lineBreakMask = lineBreak.mask
    if (lineBreak.isSpecified) {
        val strategy = lineBreakMask and LINE_BREAK_STRATEGY_MASK
        val strictness =
            (lineBreakMask ushr LINE_BREAK_UNPACKED_STRICTNESS_SHIFT) and LINE_BREAK_STRICTNESS_MASK
        val wordBreak =
            (lineBreakMask ushr LINE_BREAK_UNPACKED_WORD_BREAK_SHIFT) and LINE_BREAK_WORD_BREAK_MASK
        val code =
            1 or
                (strategy shl LINE_BREAK_PACKED_STRATEGY_SHIFT) or
                (strictness shl LINE_BREAK_PACKED_STRICTNESS_SHIFT) or
                (wordBreak shl LINE_BREAK_PACKED_WORD_BREAK_SHIFT)
        bits = bits or (code.toLong() shl LINE_BREAK_SHIFT)
    }
    return bits or packLineHeightBits(lineHeight)
}

/**
 * Strips preset values from [textIndent], [platformStyle], and [textMotion] that already live in
 * `ParagraphStyle.bits` ([TextIndent.None], [PlatformParagraphStyle.Default], [TextMotion.Static],
 * [TextMotion.Animated]). Makes a [ParagraphStyleExtras] object only if a custom value remains.
 */
internal fun createParagraphStyleExtras(
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    textMotion: TextMotion?,
): ParagraphStyleExtras? {
    val normalizedIndent = if (textIndent == TextIndent.None) null else textIndent
    val normalizedPlatform =
        if (platformStyle == PlatformParagraphStyle.Default) null else platformStyle
    val normalizedMotion =
        if (textMotion == TextMotion.Static || textMotion == TextMotion.Animated) null
        else textMotion
    return if (normalizedIndent == null && normalizedPlatform == null && normalizedMotion == null) {
        null
    } else {
        ParagraphStyleExtras(normalizedIndent, normalizedPlatform, normalizedMotion)
    }
}

/**
 * Merges [textIndent], [platformStyle], and [textMotion] into [thisExtras]. Maps preset values to
 * `null` and reuses [thisExtras] when the fields match.
 */
internal fun mergeParagraphStyleExtras(
    thisExtras: ParagraphStyleExtras?,
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    textMotion: TextMotion?,
): ParagraphStyleExtras? {
    val mergedTextIndent =
        if (textIndent == null) thisExtras?.textIndent
        else if (textIndent == TextIndent.None) null else textIndent
    val mergedPlatformStyle =
        if (platformStyle == null) thisExtras?.platformStyle
        else if (platformStyle == PlatformParagraphStyle.Default) null else platformStyle
    val mergedTextMotion =
        if (textMotion == null) thisExtras?.textMotion
        else if (textMotion == TextMotion.Static || textMotion == TextMotion.Animated) null
        else textMotion
    if (
        thisExtras != null &&
            thisExtras.matches(mergedTextIndent, mergedPlatformStyle, mergedTextMotion)
    ) {
        return thisExtras
    }
    return if (
        mergedTextIndent == null && mergedPlatformStyle == null && mergedTextMotion == null
    ) {
        null
    } else {
        ParagraphStyleExtras(mergedTextIndent, mergedPlatformStyle, mergedTextMotion)
    }
}

/**
 * Interpolate between two [ParagraphStyle]s.
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
@Stable
public fun lerp(start: ParagraphStyle, stop: ParagraphStyle, fraction: Float): ParagraphStyle {
    if (start.legacyParagraphStyle != null) {
        return ParagraphStyle(
            lerp(start.legacyParagraphStyle, stop.legacyParagraphStyle!!, fraction)
        )
    }
    // `if` instead of lerpDiscrete, which would box each value class.
    val discrete = if (fraction < 0.5f) start else stop
    return createParagraphStyle(
        textAlign = discrete.textAlign,
        textDirection = discrete.textDirection,
        lineHeight = lerpTextUnitInheritable(start.lineHeight, stop.lineHeight, fraction),
        textIndent =
            if (start.extras?.textIndent == null && stop.extras?.textIndent == null) {
                TextIndent.None
            } else {
                lerp(
                    start.textIndent ?: TextIndent.None,
                    stop.textIndent ?: TextIndent.None,
                    fraction,
                )
            },
        platformStyle = lerpPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        lineHeightStyle = discrete.lineHeightStyle,
        lineBreak = discrete.lineBreak,
        hyphens = discrete.hyphens,
        textMotion = discrete.textMotion,
    )
}

private fun lerpPlatformStyle(
    start: PlatformParagraphStyle?,
    stop: PlatformParagraphStyle?,
    fraction: Float,
): PlatformParagraphStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformParagraphStyle.Default
    val stopNonNull = stop ?: PlatformParagraphStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}

/** Builds a [ParagraphStyle], or returns [DefaultParagraphStyle] when nothing is set. */
internal fun createParagraphStyle(
    textAlign: TextAlign = TextAlign.Unspecified,
    textDirection: TextDirection = TextDirection.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textIndent: TextIndent? = null,
    platformStyle: PlatformParagraphStyle? = null,
    lineHeightStyle: LineHeightStyle? = null,
    lineBreak: LineBreak = LineBreak.Unspecified,
    hyphens: Hyphens = Hyphens.Unspecified,
    textMotion: TextMotion? = null,
): ParagraphStyle {
    if (!PackedTextStylesLatch.isEnabled) {
        return createLegacyParagraphStyle(
            textAlign,
            textDirection,
            lineHeight,
            textIndent,
            platformStyle,
            lineHeightStyle,
            lineBreak,
            hyphens,
            textMotion,
        )
    }
    val bits =
        packParagraphStyleBits(
            textAlign,
            textDirection,
            lineHeight,
            textIndent,
            platformStyle,
            lineBreak,
            hyphens,
            textMotion,
        )
    if (bits == 0L && lineHeightStyle == null) return DefaultParagraphStyle
    return ParagraphStyle(
        bits,
        lineHeightStyle,
        if (bits and (HAS_TEXT_INDENT or PLATFORM_STYLE_MASK or TEXT_MOTION_MASK) == 0L) {
            /* extras = */ null
        } else {
            createParagraphStyleExtras(textIndent, platformStyle, textMotion)
        },
    )
}

private fun createLegacyParagraphStyle(
    textAlign: TextAlign,
    textDirection: TextDirection,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    lineHeightStyle: LineHeightStyle?,
    lineBreak: LineBreak,
    hyphens: Hyphens,
    textMotion: TextMotion?,
): ParagraphStyle =
    ParagraphStyle(
        LegacyParagraphStyle(
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
    )

/** Fills in all unset fields for [direction], reusing one object per dir when unset. */
internal fun resolveParagraphStyleDefaults(
    style: ParagraphStyle,
    direction: LayoutDirection,
): ParagraphStyle {
    if (style.legacyParagraphStyle != null) {
        return ParagraphStyle(
            resolveLegacyParagraphStyleDefaults(style.legacyParagraphStyle, direction)
        )
    }
    if (style.isDefaultFast()) {
        return if (direction == LayoutDirection.Ltr) {
            BoxedStyleValues.ResolvedDefaultParagraphStyleLtr
        } else {
            BoxedStyleValues.ResolvedDefaultParagraphStyleRtl
        }
    }
    return style.withResolvedDefaults(direction)
}
