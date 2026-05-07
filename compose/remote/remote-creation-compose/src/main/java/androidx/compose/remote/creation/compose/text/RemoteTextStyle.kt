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

package androidx.compose.remote.creation.compose.text

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

/**
 * A remote-aware text style that mirrors [androidx.compose.ui.text.TextStyle] but uses remote types
 * where applicable.
 *
 * @param color The color of the text
 * @param fontSize the size of glyphs to use when painting the text in [RemoteTextUnit] .
 * @param fontWeight the typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontStyle The indentation of the paragraph.
 * @param fontFamily the font family to be used when rendering the text.
 * @param letterSpacing the amount of space to add between each letter in [RemoteTextUnit] .
 * @param background The background color for the text.
 * @param textAlign the alignment of the text within the lines of the paragraph.
 * @param lineHeight Line height for the text in [RemoteTextUnit] unit, e.g. SP or EM.
 * @param textDecoration The configuration of hyphenation.
 */
public class RemoteTextStyle
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    public val color: RemoteColor? = null,
    public val fontSize: RemoteTextUnit? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
    public val fontFamily: FontFamily? = null,
    public val letterSpacing: RemoteTextUnit? = null,
    public val background: RemoteColor? = null,
    public val textAlign: TextAlign? = null,
    public val lineHeight: RemoteTextUnit? = null,
    public val textDecoration: TextDecoration? = null,
) {

    /**
     * Returns a new [RemoteTextStyle] that is a combination of this style and the given [other]
     * style.
     *
     * If [other] is null, this style is returned. If [other] has any null properties, the values
     * from this style are used for those properties.
     *
     * @param other The style to merge into this style.
     * @return A new [RemoteTextStyle] with properties from [other] taking precedence.
     */
    public fun merge(other: RemoteTextStyle?): RemoteTextStyle {
        if (other == null) return this
        return RemoteTextStyle(
            color = other.color ?: this.color,
            fontSize = other.fontSize ?: this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontFamily = other.fontFamily ?: this.fontFamily,
            letterSpacing = other.letterSpacing ?: this.letterSpacing,
            background = other.background ?: this.background,
            textAlign = other.textAlign ?: this.textAlign,
            lineHeight = other.lineHeight ?: this.lineHeight,
            textDecoration = other.textDecoration ?: this.textDecoration,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun merge(
        color: RemoteColor? = null,
        fontSize: RemoteTextUnit? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: RemoteTextUnit? = null,
        background: RemoteColor? = null,
        textAlign: TextAlign? = null,
        lineHeight: RemoteTextUnit? = null,
        textDecoration: TextDecoration? = null,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color ?: this.color,
            fontSize = fontSize ?: this.fontSize,
            fontWeight = fontWeight ?: this.fontWeight,
            fontStyle = fontStyle ?: this.fontStyle,
            fontFamily = fontFamily ?: this.fontFamily,
            letterSpacing = letterSpacing ?: this.letterSpacing,
            background = background ?: this.background,
            textAlign = textAlign ?: this.textAlign,
            lineHeight = lineHeight ?: this.lineHeight,
            textDecoration = textDecoration ?: this.textDecoration,
        )
    }

    /**
     * Creates a copy of this [RemoteTextStyle] with the ability to override individual attributes.
     */
    public fun copy(
        color: RemoteColor? = this.color,
        fontSize: RemoteTextUnit? = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontFamily: FontFamily? = this.fontFamily,
        letterSpacing: RemoteTextUnit? = this.letterSpacing,
        background: RemoteColor? = this.background,
        textAlign: TextAlign? = this.textAlign,
        lineHeight: RemoteTextUnit? = this.lineHeight,
        textDecoration: TextDecoration? = this.textDecoration,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            background = background,
            textAlign = textAlign,
            lineHeight = lineHeight,
            textDecoration = textDecoration,
        )
    }

    public companion object {
        /** Creates a [RemoteTextStyle] from a [TextStyle]. */
        public fun fromTextStyle(style: TextStyle): RemoteTextStyle {
            // Maps unspecified color into null as it's not supported in remote compose.
            val color = if (style.color == Color.Unspecified) null else style.color.rc
            val background =
                if (style.background == Color.Unspecified) null else style.background.rc
            val fontSize =
                if (style.fontSize == TextUnit.Unspecified) null
                else style.fontSize.asRemoteTextUnit()
            val letterSpacing =
                if (style.letterSpacing == TextUnit.Unspecified) null
                else style.letterSpacing.asRemoteTextUnit()
            val lineHeight =
                if (style.lineHeight == TextUnit.Unspecified) null
                else style.lineHeight.asRemoteTextUnit()
            return RemoteTextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = style.fontWeight,
                fontStyle = style.fontStyle,
                fontFamily = style.fontFamily,
                letterSpacing = letterSpacing,
                background = background,
                textAlign = style.textAlign,
                lineHeight = lineHeight,
                textDecoration = style.textDecoration,
            )
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val Default: RemoteTextStyle = RemoteTextStyle()
    }
}
