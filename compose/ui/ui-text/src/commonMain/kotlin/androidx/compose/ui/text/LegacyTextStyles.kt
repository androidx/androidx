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
import androidx.compose.ui.text.internal.checkPrecondition
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified

/**
 * Fallback default line height used by [resolveLegacyParagraphStyleDefaults] when
 * [LegacyParagraphStyle.lineHeight] is [TextUnit.Unspecified].
 */
private val LegacyDefaultLineHeight = TextUnit.Unspecified

/**
 * Shared empty [LegacyParagraphStyle] instance with all fields set to their unspecified defaults.
 */
internal val DefaultLegacyParagraphStyle = LegacyParagraphStyle()

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
