/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse

/** The default values to use if they are not specified. */
internal val DefaultCurvedTextStyles =
    CurvedTextStyle(
        color = Color.Black,
        fontSize = 14.sp,
        background = Color.Transparent,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        fontSynthesis = FontSynthesis.All,
        letterSpacing = 0f.em,
        letterSpacingCounterClockwise = 0f.em,
        lineHeight = TextUnit.Unspecified
    )

/**
 * Styling configuration for a curved text.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 *
 * Sample using different letter spacings for top & bottom text:
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedLetterSpacingSample
 * @param background The background color for the text.
 * @param color The text color.
 * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This may be
 *   [TextUnit.Unspecified] for inheriting from another [CurvedTextStyle].
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontWeight The thickness of the glyphs, in a range of [1, 1000]. see [FontWeight]
 * @param fontStyle The typeface variant to use when drawing the letters (e.g. italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *   style cannot be found in the provided font family.
 * @param letterSpacing The amount of space (in em or sp) to add between each letter, when text is
 *   going clockwise.
 * @param letterSpacingCounterClockwise The amount of space (in em or sp) to add between each
 *   letter, when text is going counterClockwise. Note that this usually needs to be bigger than
 *   [letterSpacing] to account for the fact that going clockwise, text fans out from the baseline
 *   while going counter clockwise text fans in. If not specified, the value for [letterSpacing]
 *   will be used.
 * @param lineHeight Line height for the text in [TextUnit] unit, e.g. SP or EM.
 */
public class CurvedTextStyle(
    public val background: Color = Color.Unspecified,
    public val color: Color = Color.Unspecified,
    public val fontSize: TextUnit = TextUnit.Unspecified,
    public val fontFamily: FontFamily? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
    public val fontSynthesis: FontSynthesis? = null,
    public val letterSpacing: TextUnit = TextUnit.Unspecified,
    public val letterSpacingCounterClockwise: TextUnit = TextUnit.Unspecified,
    public val lineHeight: TextUnit = TextUnit.Unspecified
) {
    /**
     * Styling configuration for a curved text.
     *
     * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
     * @param background The background color for the text.
     * @param color The text color.
     * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
     *   may be [TextUnit.Unspecified] for inheriting from another [CurvedTextStyle].
     */
    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.0. A newer overload is available with additional font parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public constructor(
        background: Color = Color.Unspecified,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified
    ) : this(background, color, fontSize, null)

    /**
     * @param background The background color for the text.
     * @param color The text color.
     * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
     *   may be [TextUnit.Unspecified] for inheriting from another [CurvedTextStyle].
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontWeight The thickness of the glyphs, in a range of [1, 1000]. see [FontWeight]
     * @param fontStyle The typeface variant to use when drawing the letters (e.g. italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     *   or style cannot be found in the provided font family.
     */
    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.4. A newer overload is available with additional letter spacing " +
            "and lineHeight parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public constructor(
        background: Color = Color.Unspecified,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null
    ) : this(background, color, fontSize, fontFamily, fontWeight, fontStyle, fontSynthesis)

    /**
     * @param background The background color for the text.
     * @param color The text color.
     * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This
     *   may be [TextUnit.Unspecified] for inheriting from another [CurvedTextStyle].
     * @param fontFamily The font family to be used when rendering the text.
     * @param fontWeight The thickness of the glyphs, in a range of [1, 1000]. see [FontWeight]
     * @param fontStyle The typeface variant to use when drawing the letters (e.g. italic).
     * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
     *   or style cannot be found in the provided font family.
     * @param letterSpacing The amount of space (in em or sp) to add between each letter.
     */
    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.4. A newer overload is available with additional letter spacing " +
            "and lineHeight parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public constructor(
        background: Color = Color.Unspecified,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null,
        letterSpacing: TextUnit = TextUnit.Unspecified,
    ) : this(
        background,
        color,
        fontSize,
        fontFamily,
        fontWeight,
        fontStyle,
        fontSynthesis,
        letterSpacing,
        letterSpacing
    )

    /**
     * Create a curved text style from the given text style.
     *
     * Note that not all parameters in the text style will be used, only [TextStyle.color],
     * [TextStyle.fontSize], [TextStyle.background], [TextStyle.fontFamily], [TextStyle.fontWeight],
     * [TextStyle.fontStyle], [TextStyle.fontSynthesis], [TextStyle.letterSpacing],
     * [TextStyle.lineHeight].
     */
    public constructor(
        style: TextStyle
    ) : this(
        style.background,
        style.color,
        style.fontSize,
        style.fontFamily,
        style.fontWeight,
        style.fontStyle,
        style.fontSynthesis,
        style.letterSpacing,
        style.letterSpacing,
        style.lineHeight
    )

    /**
     * Returns a new curved text style that is a combination of this style and the given [other]
     * style.
     *
     * [other] curved text style's null or inherit properties are replaced with the non-null
     * properties of this curved text style. Another way to think of it is that the "missing"
     * properties of the [other] style are _filled_ by the properties of this style.
     *
     * If the given curved text style is null, returns this curved text style.
     */
    public fun merge(other: CurvedTextStyle? = null): CurvedTextStyle {
        if (other == null) return this

        return CurvedTextStyle(
            color = other.color.takeOrElse { this.color },
            fontSize = if (other.fontSize.isSpecified) other.fontSize else this.fontSize,
            background = other.background.takeOrElse { this.background },
            fontFamily = other.fontFamily ?: this.fontFamily,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontSynthesis = other.fontSynthesis ?: this.fontSynthesis,
            letterSpacing =
                if (other.letterSpacing.isSpecified) other.letterSpacing else this.letterSpacing,
            letterSpacingCounterClockwise =
                if (other.letterSpacingCounterClockwise.isSpecified)
                    other.letterSpacingCounterClockwise
                else this.letterSpacingCounterClockwise,
            lineHeight = other.lineHeight.takeOrElse { this.lineHeight }
        )
    }

    /** Plus operator overload that applies a [merge]. */
    public operator fun plus(other: CurvedTextStyle): CurvedTextStyle = this.merge(other)

    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.0. A newer overload is available with additional font parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public fun copy(
        background: Color = this.background,
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
    ): CurvedTextStyle {
        return CurvedTextStyle(
            background = background,
            color = color,
            fontSize = fontSize,
            fontFamily = this.fontFamily,
            fontWeight = this.fontWeight,
            fontStyle = this.fontStyle,
            fontSynthesis = this.fontSynthesis,
            letterSpacing = this.letterSpacing,
            letterSpacingCounterClockwise = this.letterSpacingCounterClockwise,
            lineHeight = this.lineHeight
        )
    }

    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.4. A newer overload is available with additional letter spacing parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public fun copy(
        background: Color = this.background,
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontFamily: FontFamily? = this.fontFamily,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis
    ): CurvedTextStyle {
        return CurvedTextStyle(
            background = background,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            letterSpacing = this.letterSpacing,
            letterSpacingCounterClockwise = this.letterSpacingCounterClockwise,
            lineHeight = this.lineHeight
        )
    }

    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.4. A newer overload is available with additional letter spacing parameters.",
        level = DeprecationLevel.HIDDEN
    )
    public fun copy(
        background: Color = this.background,
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontFamily: FontFamily? = this.fontFamily,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        // We do this so when the user doesn't specify letterSpacing, neither letterSpacing nor
        // letterSpacingCounterClockwise are modified, and when they do specify it we update both.
        letterSpacing: TextUnit = TextUnit.Unspecified,
    ): CurvedTextStyle {
        return CurvedTextStyle(
            background = background,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            letterSpacing = letterSpacing.takeOrElse { this@CurvedTextStyle.letterSpacing },
            letterSpacingCounterClockwise =
                letterSpacing.takeOrElse { letterSpacingCounterClockwise },
            lineHeight = this.lineHeight
        )
    }

    public fun copy(
        background: Color = this.background,
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontFamily: FontFamily? = this.fontFamily,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        letterSpacing: TextUnit = this.letterSpacing,
        letterSpacingCounterClockwise: TextUnit = this.letterSpacingCounterClockwise,
        lineHeight: TextUnit = this.lineHeight
    ): CurvedTextStyle {
        return CurvedTextStyle(
            background = background,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            letterSpacing = letterSpacing,
            letterSpacingCounterClockwise = letterSpacingCounterClockwise,
            lineHeight = lineHeight
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is CurvedTextStyle &&
            color == other.color &&
            fontSize == other.fontSize &&
            background == other.background &&
            fontFamily == other.fontFamily &&
            fontWeight == other.fontWeight &&
            fontStyle == other.fontStyle &&
            fontSynthesis == other.fontSynthesis &&
            letterSpacing == other.letterSpacing &&
            letterSpacingCounterClockwise == other.letterSpacingCounterClockwise &&
            lineHeight == other.lineHeight
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + background.hashCode()
        result = 31 * result + fontFamily.hashCode()
        result = 31 * result + fontWeight.hashCode()
        result = 31 * result + fontStyle.hashCode()
        result = 31 * result + fontSynthesis.hashCode()
        result = 31 * result + letterSpacing.hashCode()
        result = 31 * result + letterSpacingCounterClockwise.hashCode()
        result = 31 * result + lineHeight.hashCode()
        return result
    }

    override fun toString(): String {
        return "CurvedTextStyle(" +
            "background=$background" +
            "color=$color, " +
            "fontSize=$fontSize, " +
            "fontFamily=$fontFamily, " +
            "fontWeight=$fontWeight, " +
            "fontStyle=$fontStyle, " +
            "fontSynthesis=$fontSynthesis, " +
            "letterSpacing=$letterSpacing, " +
            "letterSpacingCounterClockwise=$letterSpacingCounterClockwise, " +
            "lineHeight=$lineHeight, " +
            ")"
    }
}
