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

package androidx.text.vertical.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * A style object for vertical text, containing only members supported by [VerticalText].
 *
 * @param color The text color.
 * @param fontSize The size of glyphs to use when painting the text.
 * @param fontWeight The thickness of the glyph strokes.
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when they are not available
 *   in the font family.
 * @param fontFamily The font family to use when rendering the text.
 * @param fontFeatureSettings The advanced typography settings for the font.
 * @param background The background color for the text.
 * @param localeList The locales of the text.
 */
@Immutable
public class VerticalTextStyle(
    public val color: Color = Color.Unspecified,
    public val fontSize: TextUnit = TextUnit.Unspecified,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
    public val fontSynthesis: FontSynthesis? = null,
    public val fontFamily: FontFamily? = null,
    public val fontFeatureSettings: String? = null,
    public val background: Color = Color.Unspecified,
    @get:Suppress("NullableCollection") public val localeList: LocaleList? = null,
) {
    /**
     * Returns a new [VerticalTextStyle] that is a combination of this style and the given [other]
     * style.
     *
     * If a property is specified in [other], it will be used in the result. Otherwise, the property
     * from this style will be used.
     */
    @Stable
    public fun merge(other: VerticalTextStyle?): VerticalTextStyle {
        if (other == null || other == Default) return this
        return VerticalTextStyle(
            color = if (other.color.isSpecified) other.color else this.color,
            fontSize = if (other.fontSize.isSpecified) other.fontSize else this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontSynthesis = other.fontSynthesis ?: this.fontSynthesis,
            fontFamily = other.fontFamily ?: this.fontFamily,
            fontFeatureSettings = other.fontFeatureSettings ?: this.fontFeatureSettings,
            background = if (other.background.isSpecified) other.background else this.background,
            localeList = other.localeList ?: this.localeList,
        )
    }

    /** Plus operator overload for [merge]. */
    @Stable public operator fun plus(other: VerticalTextStyle): VerticalTextStyle = merge(other)

    public fun copy(
        color: Color = this.color,
        fontSize: TextUnit = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontSynthesis: FontSynthesis? = this.fontSynthesis,
        fontFamily: FontFamily? = this.fontFamily,
        fontFeatureSettings: String? = this.fontFeatureSettings,
        background: Color = this.background,
        localeList: LocaleList? = this.localeList,
    ): VerticalTextStyle {
        return VerticalTextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            background = background,
            localeList = localeList,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerticalTextStyle) return false

        if (color != other.color) return false
        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false
        if (fontSynthesis != other.fontSynthesis) return false
        if (fontFamily != other.fontFamily) return false
        if (fontFeatureSettings != other.fontFeatureSettings) return false
        if (background != other.background) return false
        if (localeList != other.localeList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + (fontSynthesis?.hashCode() ?: 0)
        result = 31 * result + (fontFamily?.hashCode() ?: 0)
        result = 31 * result + (fontFeatureSettings?.hashCode() ?: 0)
        result = 31 * result + background.hashCode()
        result = 31 * result + (localeList?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "VerticalTextStyle(" +
            "color=$color, " +
            "fontSize=$fontSize, " +
            "fontWeight=$fontWeight, " +
            "fontStyle=$fontStyle, " +
            "fontSynthesis=$fontSynthesis, " +
            "fontFamily=$fontFamily, " +
            "fontFeatureSettings=$fontFeatureSettings, " +
            "background=$background, " +
            "localeList=$localeList, " +
            ")"
    }

    public companion object {
        /** Default [VerticalTextStyle] with all members unspecified. */
        public val Default: VerticalTextStyle = VerticalTextStyle()
    }
}
