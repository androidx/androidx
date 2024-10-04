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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import kotlin.jvm.JvmName

private val DefaultLineHeight = TextUnit.Unspecified

/**
 * Paragraph styling configuration for a paragraph. The difference between [SpanStyle] and
 * `ParagraphStyle` is that, `ParagraphStyle` can be applied to a whole [Paragraph] while
 * [SpanStyle] can be applied at the character level. Once a portion of the text is marked with a
 * `ParagraphStyle`, that portion will be separated from the remaining as if a line feed character
 * was added.
 *
 * @sample androidx.compose.ui.text.samples.ParagraphStyleSample
 * @sample androidx.compose.ui.text.samples.ParagraphStyleAnnotatedStringsSample
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param textDirection The algorithm to be used to resolve the final text direction: Left To Right
 *   or Right To Left.
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 * @param textIndent The indentation of the paragraph.
 * @param platformStyle Platform specific [ParagraphStyle] parameters.
 * @param lineHeightStyle the configuration for line height such as vertical alignment of the line,
 *   whether to apply additional space as a result of line height to top of first line top and
 *   bottom of last line. The configuration is applied only when a [lineHeight] is defined. When
 *   null, [LineHeightStyle.Default] is used.
 * @param lineBreak The line breaking configuration for the text.
 * @param hyphens The configuration of hyphenation.
 * @param textMotion Text character placement, whether to optimize for animated or static text.
 * @see Paragraph
 * @see AnnotatedString
 * @see SpanStyle
 * @see TextStyle
 */
@Immutable
class ParagraphStyle(
    val textAlign: TextAlign = TextAlign.Unspecified,
    val textDirection: TextDirection = TextDirection.Unspecified,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val textIndent: TextIndent? = null,
    val platformStyle: PlatformParagraphStyle? = null,
    val lineHeightStyle: LineHeightStyle? = null,
    val lineBreak: LineBreak = LineBreak.Unspecified,
    val hyphens: Hyphens = Hyphens.Unspecified,
    val textMotion: TextMotion? = null
) : AnnotatedString.Annotation {
    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getTextAlign-buA522U") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    val deprecated_boxing_textAlign: TextAlign?
        get() = this.textAlign

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getTextDirection-mmuk1to") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    val deprecated_boxing_textDirection: TextDirection?
        get() = this.textDirection

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getHyphens-EaSxIns") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    val deprecated_boxing_hyphens: Hyphens?
        get() = this.hyphens

    @Deprecated("Kept for backwards compatibility.", level = DeprecationLevel.WARNING)
    @get:JvmName("getLineBreak-LgCVezo") // b/320819734
    @Suppress("unused", "RedundantNullableReturnType", "PropertyName")
    val deprecated_boxing_lineBreak: LineBreak?
        get() = this.lineBreak

    @Deprecated(
        "ParagraphStyle constructors that take nullable TextAlign, " +
            "TextDirection, LineBreak, and Hyphens are deprecated. Please use a new constructor " +
            "where these parameters are non-nullable. Null value has been replaced by a special " +
            "Unspecified object for performance reason.",
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak? = null,
        hyphens: Hyphens? = null,
        textMotion: TextMotion? = null
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = lineBreak ?: LineBreak.Unspecified,
        hyphens = hyphens ?: Hyphens.Unspecified,
        textMotion = textMotion
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "constructor.",
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = null,
        lineHeightStyle = null,
        lineBreak = LineBreak.Unspecified,
        hyphens = Hyphens.Unspecified,
        textMotion = null
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "constructors.",
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = LineBreak.Unspecified,
        hyphens = Hyphens.Unspecified,
        textMotion = null
    )

    @Deprecated(
        "ParagraphStyle constructors that do not take new stable parameters " +
            "like LineBreak, Hyphens, TextMotion are deprecated. Please use the new stable " +
            "constructors.",
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        lineHeight: TextUnit = TextUnit.Unspecified,
        textIndent: TextIndent? = null,
        platformStyle: PlatformParagraphStyle? = null,
        lineHeightStyle: LineHeightStyle? = null,
        lineBreak: LineBreak? = null,
        hyphens: Hyphens? = null
    ) : this(
        textAlign = textAlign ?: TextAlign.Unspecified,
        textDirection = textDirection ?: TextDirection.Unspecified,
        lineHeight = lineHeight,
        textIndent = textIndent,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle,
        lineBreak = lineBreak ?: LineBreak.Unspecified,
        hyphens = hyphens ?: Hyphens.Unspecified,
        textMotion = null
    )

    init {
        if (lineHeight != TextUnit.Unspecified) {
            // Since we are checking if it's negative, no need to convert Sp into Px at this point.
            checkPrecondition(lineHeight.value >= 0f) {
                "lineHeight can't be negative (${lineHeight.value})"
            }
        }
    }

    /**
     * Returns a new paragraph style that is a combination of this style and the given [other]
     * style.
     *
     * If the given paragraph style is null, returns this paragraph style.
     */
    @Stable
    fun merge(other: ParagraphStyle? = null): ParagraphStyle {
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
            textMotion = other.textMotion
        )
    }

    /** Plus operator overload that applies a [merge]. */
    @Stable operator fun plus(other: ParagraphStyle): ParagraphStyle = this.merge(other)

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = this.platformStyle,
            lineHeightStyle = this.lineHeightStyle,
            lineBreak = this.lineBreak,
            hyphens = this.hyphens,
            textMotion = this.textMotion
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineHeightStyle, LineBreak, Hyphens are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = this.lineBreak,
            hyphens = this.hyphens,
            textMotion = this.textMotion
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that do not take new stable parameters " +
            "like LineBreak, Hyphens, TextMotion are deprecated. Please use the new stable " +
            "copy constructor.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak? = this.lineBreak,
        hyphens: Hyphens? = this.hyphens
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak ?: LineBreak.Unspecified,
            hyphens = hyphens ?: Hyphens.Unspecified,
            textMotion = this.textMotion
        )
    }

    @Deprecated(
        "ParagraphStyle copy constructors that take nullable TextAlign, " +
            "TextDirection, LineBreak, and Hyphens are deprecated. Please use a new constructor " +
            "where these parameters are non-nullable. Null value has been replaced by a special " +
            "Unspecified object for performance reason.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
        textAlign: TextAlign? = this.textAlign,
        textDirection: TextDirection? = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak? = this.lineBreak,
        hyphens: Hyphens? = this.hyphens,
        textMotion: TextMotion? = this.textMotion
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDirection = textDirection ?: TextDirection.Unspecified,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak ?: LineBreak.Unspecified,
            hyphens = hyphens ?: Hyphens.Unspecified,
            textMotion = textMotion
        )
    }

    fun copy(
        textAlign: TextAlign = this.textAlign,
        textDirection: TextDirection = this.textDirection,
        lineHeight: TextUnit = this.lineHeight,
        textIndent: TextIndent? = this.textIndent,
        platformStyle: PlatformParagraphStyle? = this.platformStyle,
        lineHeightStyle: LineHeightStyle? = this.lineHeightStyle,
        lineBreak: LineBreak = this.lineBreak,
        hyphens: Hyphens = this.hyphens,
        textMotion: TextMotion? = this.textMotion
    ): ParagraphStyle {
        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParagraphStyle) return false

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
fun lerp(start: ParagraphStyle, stop: ParagraphStyle, fraction: Float): ParagraphStyle {
    return ParagraphStyle(
        textAlign = lerpDiscrete(start.textAlign, stop.textAlign, fraction),
        textDirection = lerpDiscrete(start.textDirection, stop.textDirection, fraction),
        lineHeight = lerpTextUnitInheritable(start.lineHeight, stop.lineHeight, fraction),
        textIndent =
            lerp(start.textIndent ?: TextIndent.None, stop.textIndent ?: TextIndent.None, fraction),
        platformStyle = lerpPlatformStyle(start.platformStyle, stop.platformStyle, fraction),
        lineHeightStyle = lerpDiscrete(start.lineHeightStyle, stop.lineHeightStyle, fraction),
        lineBreak = lerpDiscrete(start.lineBreak, stop.lineBreak, fraction),
        hyphens = lerpDiscrete(start.hyphens, stop.hyphens, fraction),
        textMotion = lerpDiscrete(start.textMotion, stop.textMotion, fraction)
    )
}

private fun lerpPlatformStyle(
    start: PlatformParagraphStyle?,
    stop: PlatformParagraphStyle?,
    fraction: Float
): PlatformParagraphStyle? {
    if (start == null && stop == null) return null
    val startNonNull = start ?: PlatformParagraphStyle.Default
    val stopNonNull = stop ?: PlatformParagraphStyle.Default
    return lerp(startNonNull, stopNonNull, fraction)
}

internal fun resolveParagraphStyleDefaults(style: ParagraphStyle, direction: LayoutDirection) =
    ParagraphStyle(
        textAlign =
            if (style.textAlign == TextAlign.Unspecified) TextAlign.Start else style.textAlign,
        textDirection = resolveTextDirection(direction, style.textDirection),
        lineHeight = if (style.lineHeight.isUnspecified) DefaultLineHeight else style.lineHeight,
        textIndent = style.textIndent ?: TextIndent.None,
        platformStyle = style.platformStyle,
        lineHeightStyle = style.lineHeightStyle,
        lineBreak =
            if (style.lineBreak == LineBreak.Unspecified) LineBreak.Simple else style.lineBreak,
        hyphens = if (style.hyphens == Hyphens.Unspecified) Hyphens.None else style.hyphens,
        textMotion = style.textMotion ?: TextMotion.Static
    )

internal fun ParagraphStyle.fastMerge(
    textAlign: TextAlign,
    textDirection: TextDirection,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    platformStyle: PlatformParagraphStyle?,
    lineHeightStyle: LineHeightStyle?,
    lineBreak: LineBreak,
    hyphens: Hyphens,
    textMotion: TextMotion?
): ParagraphStyle {
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

    return ParagraphStyle(
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
        textMotion = textMotion ?: this.textMotion
    )
}

private fun ParagraphStyle.mergePlatformStyle(
    other: PlatformParagraphStyle?
): PlatformParagraphStyle? {
    if (platformStyle == null) return other
    if (other == null) return platformStyle
    return platformStyle.merge(other)
}
