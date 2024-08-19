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
    )

/**
 * Styling configuration for a curved text.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedAndNormalText
 * @param background The background color for the text.
 * @param color The text color.
 * @param fontSize The size of glyphs (in logical pixels) to use when painting the text. This may be
 *   [TextUnit.Unspecified] for inheriting from another [CurvedTextStyle].
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontWeight The thickness of the glyphs, in a range of [1, 1000]. see [FontWeight]
 * @param fontStyle The typeface variant to use when drawing the letters (e.g. italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight or
 *   style cannot be found in the provided font family.
 * @param letterSpacing The amount of space (in em) to add between each letter.
 */
class CurvedTextStyle(
    val background: Color = Color.Unspecified,
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontFamily: FontFamily? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontSynthesis: FontSynthesis? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified,
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
    constructor(
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
            "Wear OS 1.4. A newer overload is available with an additional letter spacing " +
            "parameter.",
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        background: Color = Color.Unspecified,
        color: Color = Color.Unspecified,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontSynthesis: FontSynthesis? = null
    ) : this(background, color, fontSize, fontFamily, fontWeight, fontStyle, fontSynthesis)

    /**
     * Create a curved text style from the given text style.
     *
     * Note that not all parameters in the text style will be used, only [TextStyle.color],
     * [TextStyle.fontSize], [TextStyle.background], [TextStyle.fontFamily], [TextStyle.fontWeight],
     * [TextStyle.fontStyle], [TextStyle.fontSynthesis] and [TextStyle.letterSpacing].
     */
    constructor(
        style: TextStyle
    ) : this(
        style.background,
        style.color,
        style.fontSize,
        style.fontFamily,
        style.fontWeight,
        style.fontStyle,
        style.fontSynthesis,
        style.letterSpacing
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
    fun merge(other: CurvedTextStyle? = null): CurvedTextStyle {
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
        )
    }

    /** Plus operator overload that applies a [merge]. */
    operator fun plus(other: CurvedTextStyle): CurvedTextStyle = this.merge(other)

    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.0. A newer overload is available with additional font parameters.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
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
            letterSpacing = this.letterSpacing
        )
    }

    @Deprecated(
        "This overload is provided for backwards compatibility with Compose for " +
            "Wear OS 1.4. A newer overload is available with additional letter spacing parameter.",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
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
            letterSpacing = this.letterSpacing
        )
    }

    fun copy(
        background: Color = this.background,
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontFamily: FontFamily? = this.fontFamily,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        letterSpacing: TextUnit = this.letterSpacing
    ): CurvedTextStyle {
        return CurvedTextStyle(
            background = background,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            letterSpacing = letterSpacing
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
            letterSpacing == other.letterSpacing
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
            ")"
    }
}
