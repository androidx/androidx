/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.compose.material3

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
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
 * Here is an example of fixing the font size for DisplayLarge:
 * @sample androidx.wear.compose.material3.samples.FixedFontSize
 *
 * TODO(b/273526150) Review documentation for typography, add examples for each size.
 *
 * @property displayLarge DisplayLarge is the largest headline. Displays are the largest text
 * on the screen, reserved for short, important text or numerals.
 *
 * @property displayMedium DisplayMedium is the second largest headline. Displays are the
 * largest text on the screen, reserved for short, important text or numerals.
 *
 * @property displaySmall DisplaySmall is the smallest headline. Displays are the largest
 * text on the screen, reserved for short, important text or numerals.
 *
 * @property titleLarge TitleLarge is the largest title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property titleMedium TitleMedium is the medium title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property titleSmall TitleSmall is the smallest title. Titles are smaller than Displays. They are
 * typically reserved for medium-emphasis text that is shorter in length.
 *
 * @property labelLarge LabelLarge is the largest label. They are used for displaying prominent
 * texts like label on title buttons.
 *
 * @property labelMedium LabelMedium is the medium label. They are used for displaying texts like
 * primary label on buttons.
 *
 * @property labelSmall LabelSmall is the small label. They are used for displaying texts like
 * secondary label on buttons, labels on compact buttons.
 *
 * @property bodyLarge BodyLarge is the largest body. Body texts are typically used for long-form
 * writing as it works well for small text sizes. For longer sections of text, a serif or
 * sans serif typeface is recommended.
 *
 * @property bodyMedium BodyMedium is second largest body. Body texts are typically used for
 * long-form writing as it works well for small text sizes. For longer sections of text, a serif
 * or sans serif typeface is recommended.
 *
 * @property bodySmall BodySmall is third largest body. Body texts are typically used for long-form
 * writing as it works well for small text sizes. For longer sections of text, a serif or sans serif
 * typeface is recommended.
 *
 * @property bodyExtraSmall BodyExtraSmall is the smallest body. Body texts are typically used for
 * long-form writing as it works well for small text sizes. For longer sections of text, a serif
 * or sans serif typeface is recommended.
 */
@Immutable
public class Typography internal constructor(
    public val displayLarge: TextStyle,
    public val displayMedium: TextStyle,
    public val displaySmall: TextStyle,
    public val titleLarge: TextStyle,
    public val titleMedium: TextStyle,
    public val titleSmall: TextStyle,
    public val labelLarge: TextStyle,
    public val labelMedium: TextStyle,
    public val labelSmall: TextStyle,
    public val bodyLarge: TextStyle,
    public val bodyMedium: TextStyle,
    public val bodySmall: TextStyle,
    public val bodyExtraSmall: TextStyle
) {
    public constructor (
        defaultFontFamily: FontFamily = FontFamily.Default,
        displayLarge: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        displayMedium: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp
        ),
        displaySmall: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        ),
        titleMedium: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.3.sp
        ),
        titleSmall: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelLarge: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelSmall: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.3.sp
        ),
        bodyLarge: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.3.sp,
        ),
        bodyMedium: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        bodyExtraSmall: TextStyle = DefaultTextStyle.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        )
    ) : this(
        displayLarge = displayLarge.withDefaultFontFamily(defaultFontFamily),
        displayMedium = displayMedium.withDefaultFontFamily(defaultFontFamily),
        displaySmall = displaySmall.withDefaultFontFamily(defaultFontFamily),
        titleLarge = titleLarge.withDefaultFontFamily(defaultFontFamily),
        titleMedium = titleMedium.withDefaultFontFamily(defaultFontFamily),
        titleSmall = titleSmall.withDefaultFontFamily(defaultFontFamily),
        labelLarge = labelLarge.withDefaultFontFamily(defaultFontFamily),
        labelMedium = labelMedium.withDefaultFontFamily(defaultFontFamily),
        labelSmall = labelSmall.withDefaultFontFamily(defaultFontFamily),
        bodyLarge = bodyLarge.withDefaultFontFamily(defaultFontFamily),
        bodyMedium = bodyMedium.withDefaultFontFamily(defaultFontFamily),
        bodySmall = bodySmall.withDefaultFontFamily(defaultFontFamily),
        bodyExtraSmall = bodyExtraSmall.withDefaultFontFamily(defaultFontFamily)
    )

    /**
     * Returns a copy of this Typography, optionally overriding some of the values.
     */
    public fun copy(
        displayLarge: TextStyle = this.displayLarge,
        displayMedium: TextStyle = this.displayMedium,
        displaySmall: TextStyle = this.displaySmall,
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        labelLarge: TextStyle = this.labelLarge,
        labelMedium: TextStyle = this.labelMedium,
        labelSmall: TextStyle = this.labelSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
        bodyExtraSmall: TextStyle = this.bodyExtraSmall
    ): Typography = Typography(
        displayLarge,
        displayMedium,
        displaySmall,
        titleLarge,
        titleMedium,
        titleSmall,
        labelLarge,
        labelMedium,
        labelSmall,
        bodyLarge,
        bodyMedium,
        bodySmall,
        bodyExtraSmall
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (displayLarge != other.displayLarge) return false
        if (displayMedium != other.displayMedium) return false
        if (displaySmall != other.displaySmall) return false
        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (labelLarge != other.labelLarge) return false
        if (labelMedium != other.labelMedium) return false
        if (labelSmall != other.labelSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false
        if (bodyExtraSmall != other.bodyExtraSmall) return false

        return true
    }

    override fun hashCode(): Int {
        var result = displayLarge.hashCode()
        result = 31 * result + displayMedium.hashCode()
        result = 31 * result + displaySmall.hashCode()
        result = 31 * result + titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + labelLarge.hashCode()
        result = 31 * result + labelMedium.hashCode()
        result = 31 * result + labelSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        result = 31 * result + bodyExtraSmall.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(displayLarge=$displayLarge, displayMedium=$displayMedium, " +
            "displaySmall=$displaySmall, titleLarge=$titleLarge, titleMedium=$titleMedium, " +
            "titleSmall=$titleSmall, labelLarge=$labelLarge, labelMedium=$labelMedium, " +
            "labelSmall=$labelSmall, bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, " +
            "bodySmall=$bodySmall, bodyExtraSmall=$bodyExtraSmall)"
    }
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as
 * the [FontFamily].
 */
private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

private const val DefaultIncludeFontPadding = false

/**
 * Returns theme default [TextStyle] with default [PlatformTextStyle].
 */
internal val DefaultTextStyle = TextStyle.Default.copy(
    platformStyle = PlatformTextStyle(
        includeFontPadding = DefaultIncludeFontPadding
    )
)

/**
 * This Ambient holds on to the current definition of typography for this application as described
 * by the Wear Material spec. You can read the values in it when creating custom components that
 * want to use Wear Material types, as well as override the values when you want to re-style a part
 * of your hierarchy. Material components related to text such as Button will use this Ambient
 * to set values with which to style children text components.
 *
 * To access values within this ambient, use [MaterialTheme.typography].
 */
internal val LocalTypography = staticCompositionLocalOf { Typography() }
