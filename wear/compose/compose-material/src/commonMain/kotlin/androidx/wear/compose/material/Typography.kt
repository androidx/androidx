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
package androidx.wear.compose.material

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Class holding typography definitions as defined by the Wear Material typography specification.
 *
 * The text styles in this typography are scaled according to the user's preferred font size in
 * the system settings. Larger font sizes can be fixed if necessary in order to avoid pressure on
 * screen space, because they are already sufficiently accessible.
 * Here is an example of fixing the font size for Display1:
 * @sample androidx.wear.compose.material.samples.FixedFontSize
 *
 * @property display1 Display1 is the largest headline. Displays are the largest text on the screen,
 * reserved for short, important text or numerals.
 *
 * @property display2 Display2 is the second largest headline. Displays are the largest text on the
 * screen, reserved for short, important text or numerals.
 *
 * @property display3 Display3 is the third largest headline. Displays are the largest text on the
 * screen, reserved for short, important text or numerals.
 *
 * @property title1 Title1 is the largest title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property title2 Title2 is the medium title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property title3 Title3 is the smallest title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property body1 Body1 is the largest body. Body texts are typically used for long-form writing as
 * it works well for small text sizes. For longer sections of text, a serif or sans serif typeface
 * is recommended.
 *
 * @property body2 Body2 is the smallest body. Body texts are typically used for long-form writing
 * as it works well for small text sizes. For longer sections of text, a serif or sans serif
 * typeface is recommended.
 *
 * @property button Button text is a call to action used in different types of buttons (such as
 * text, outlined and contained buttons) and in tabs, dialogs, and cards. Button text is typically
 * sans serif, using all caps text.
 *
 * @property caption1 Caption1 is the largest caption. Caption texts are the smallest font sizes.
 * They are used on secondary content.
 *
 * @property caption2 Caption2 is the second largest caption. Caption texts are the smallest font
 * sizes. They are used on secondary content.
 *
 * @property caption3 Caption3 is an exceptional small font size which is used for the extra
 * long-form writing like legal texts.
 */
@Immutable
public class Typography internal constructor(
    public val display1: TextStyle,
    public val display2: TextStyle,
    public val display3: TextStyle,
    public val title1: TextStyle,
    public val title2: TextStyle,
    public val title3: TextStyle,
    public val body1: TextStyle,
    public val body2: TextStyle,
    public val button: TextStyle,
    public val caption1: TextStyle,
    public val caption2: TextStyle,
    public val caption3: TextStyle,
) {
    public constructor (
        defaultFontFamily: FontFamily = FontFamily.Default,
        display1: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 40.sp,
            lineHeight = 46.sp,
            letterSpacing = 0.5.sp
        ),
        display2: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = 1.sp
        ),
        display3: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.8.sp,
        ),
        title1: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.2.sp
        ),
        title2: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp
        ),
        title3: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        body1: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.18.sp
        ),
        body2: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        ),
        button: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.38.sp
        ),
        caption1: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        ),
        caption2: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp
        ),
        caption3: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.1.sp
        )

    ) : this(
        display1 = display1.withDefaultFontFamily(defaultFontFamily),
        display2 = display2.withDefaultFontFamily(defaultFontFamily),
        display3 = display3.withDefaultFontFamily(defaultFontFamily),
        title1 = title1.withDefaultFontFamily(defaultFontFamily),
        title2 = title2.withDefaultFontFamily(defaultFontFamily),
        title3 = title3.withDefaultFontFamily(defaultFontFamily),
        body1 = body1.withDefaultFontFamily(defaultFontFamily),
        body2 = body2.withDefaultFontFamily(defaultFontFamily),
        button = button.withDefaultFontFamily(defaultFontFamily),
        caption1 = caption1.withDefaultFontFamily(defaultFontFamily),
        caption2 = caption2.withDefaultFontFamily(defaultFontFamily),
        caption3 = caption3.withDefaultFontFamily(defaultFontFamily),
    )

    /**
     * Returns a copy of this Typography, optionally overriding some of the values.
     */
    public fun copy(
        display1: TextStyle = this.display1,
        display2: TextStyle = this.display2,
        display3: TextStyle = this.display3,
        title1: TextStyle = this.title1,
        title2: TextStyle = this.title2,
        title3: TextStyle = this.title3,
        body1: TextStyle = this.body1,
        body2: TextStyle = this.body2,
        button: TextStyle = this.button,
        caption1: TextStyle = this.caption1,
        caption2: TextStyle = this.caption2,
        caption3: TextStyle = this.caption3,
    ): Typography = Typography(
        display1,
        display2,
        display3,
        title1,
        title2,
        title3,
        body1,
        body2,
        button,
        caption1,
        caption2,
        caption3,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (display1 != other.display1) return false
        if (display2 != other.display2) return false
        if (display3 != other.display3) return false
        if (title1 != other.title1) return false
        if (title2 != other.title2) return false
        if (title3 != other.title3) return false
        if (body1 != other.body1) return false
        if (body2 != other.body2) return false
        if (button != other.button) return false
        if (caption1 != other.caption1) return false
        if (caption2 != other.caption2) return false
        if (caption3 != other.caption3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = display1.hashCode()
        result = 31 * result + display2.hashCode()
        result = 31 * result + display3.hashCode()
        result = 31 * result + title1.hashCode()
        result = 31 * result + title2.hashCode()
        result = 31 * result + title3.hashCode()
        result = 31 * result + body1.hashCode()
        result = 31 * result + body2.hashCode()
        result = 31 * result + button.hashCode()
        result = 31 * result + caption1.hashCode()
        result = 31 * result + caption2.hashCode()
        result = 31 * result + caption3.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(display1=$display1, display2=$display2, display3=$display3, " +
            "title1=$title1, title2=$title2, title3=$title3, body1=$body1, body2=$body2, " +
            "button=$button, caption1=$caption1, caption2=$caption2, caption3=$caption3)"
    }
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as
 * the [FontFamily].
 */
private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

/**
 * This Ambient holds on to the current definition of typography for this application as described
 * by the Wear Material spec. You can read the values in it when creating custom components that
 * want to use Wear Material types, as well as override the values when you want to re-style a part
 * of your hierarchy. Material components related to text such as [Button] will use this Ambient
 * to set values with which to style children text components.
 *
 * To access values within this ambient, use [MaterialTheme.typography].
 */
internal val LocalTypography = staticCompositionLocalOf { Typography() }
