/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.wear.compose.material3.tokens.TypographyKeyTokens
import androidx.wear.compose.material3.tokens.TypographyTokens

/**
 * Class holding typography definitions as defined by the Wear Material typography specification.
 *
 * The text styles in this typography are scaled according to the user's preferred font size in the
 * system settings. Larger font sizes can be fixed if necessary in order to avoid pressure on screen
 * space, because they are already sufficiently accessible. Here is an example of fixing the font
 * size for DisplayLarge:
 *
 * @sample androidx.wear.compose.material3.samples.FixedFontSize
 *
 * Display styles are utilized for large, short strings of text used to display highly glanceable
 * hero information, significant metrics, confidence or expressive brand moments.
 *
 * Title styles are hierarchical text used as a mechanism for way-finding, like a page, section
 * title, or sub-section title (in the case of Title Small).
 *
 * Label styles are used for component level text that describes an action that would happen if
 * interacted with. The most common and widely used application for label is for text nested within
 * a button.
 *
 * Body styles are reserved for content text like paragraphs of body copy, text used in complex data
 * visualisation, time stamps and metadata.
 *
 * Numeral text styles are used for numerical digits, usually limited to a few characters. These can
 * take on more expressive properties at the larger display sizes. They give flexibility to expand
 * width axis with minimal localization and font scaling concerns.
 *
 * Arc text styles are used for curved text making up the signposting on the UI such as time text
 * and curved labels, a tailored font axis that specifically optimizes type along a curve.
 *
 * @property arcLarge ArcLarge is for arc headers and titles. Arc is for text along a curved path on
 *   the screen, reserved for short header text strings at the very top or bottom of the screen like
 *   confirmation overlays.
 * @property arcMedium ArcMedium is for arc headers and titles. Arc is for text along a curved path
 *   on the screen, reserved for short header text strings at the very top or bottom of the screen
 *   like page titles.
 * @property arcSmall ArcSmall is for arc headers and titles. Arc is for text along a curved path on
 *   the screen, reserved for short header text strings at the very top or bottom of the screen.
 * @property displayLarge DisplayLarge is the largest headline. Displays are the largest text on the
 *   screen, reserved for short, important text or numerals.
 * @property displayMedium DisplayMedium is the second largest headline. Displays are the largest
 *   text on the screen, reserved for short, important text or numerals.
 * @property displaySmall DisplaySmall is the smallest headline. Displays are the largest text on
 *   the screen, reserved for short, important text or numerals.
 * @property titleLarge TitleLarge is the largest title. Titles are smaller than Displays. They are
 *   typically reserved for medium-emphasis text that is shorter in length.
 * @property titleMedium TitleMedium is the medium title. Titles are smaller than Displays. They are
 *   typically reserved for medium-emphasis text that is shorter in length.
 * @property titleSmall TitleSmall is the smallest title. Titles are smaller than Displays. They are
 *   typically reserved for medium-emphasis text that is shorter in length.
 * @property labelLarge LabelLarge is the largest label. They are used for displaying prominent
 *   texts like label on title buttons.
 * @property labelMedium LabelMedium is the medium label. They are used for displaying texts like
 *   primary label on buttons.
 * @property labelSmall LabelSmall is the small label. They are used for displaying texts like
 *   secondary label on buttons, labels on compact buttons.
 * @property bodyLarge BodyLarge is the largest body. Body texts are typically used for long-form
 *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
 *   serif typeface is recommended.
 * @property bodyMedium BodyMedium is second largest body. Body texts are typically used for
 *   long-form writing as it works well for small text sizes. For longer sections of text, a serif
 *   or sans serif typeface is recommended.
 * @property bodySmall BodySmall is third largest body. Body texts are typically used for long-form
 *   writing as it works well for small text sizes. For longer sections of text, a serif or sans
 *   serif typeface is recommended.
 * @property bodyExtraSmall BodyExtraSmall is the smallest body. Body texts are typically used for
 *   long-form writing as it works well for small text sizes. For longer sections of text, a serif
 *   or sans serif typeface is recommended.
 * @property numeralExtraLarge NumeralExtraLarge is the largest role for digits. Numerals use
 *   tabular spacing by default. They highlight and express glanceable numbers that are limited to a
 *   two or three characters only, where no localization is required like the charging screen.
 * @property numeralLarge NumeralLarge is the second largest role for digits. Numerals use tabular
 *   spacing by default. They are large sized number strings that are limited to big displays of
 *   time, where no localization is required like a timer countdown.
 * @property numeralMedium NumeralMedium is the third largest role for digits. Numerals use tabular
 *   spacing by default. They are medium sized numbers that are limited to short strings of digits,
 *   where no localization is required like a steps count.
 * @property numeralSmall NumeralSmall is the fourth largest role for digits. Numerals use tabular
 *   spacing by default. They are for numbers that need emphasis at a smaller scale, where no
 *   localization is required like date and time pickers.
 * @property numeralExtraSmall NumeralExtraSmall is the smallest role for digits. Numerals use
 *   tabular spacing by default. They are for numbers that need to accommodate longer strings of
 *   digits, where no localization is required like in-workout metrics.
 */
// TODO(b/273526150) Review documentation for typography, add examples for each size.
@Immutable
class Typography
internal constructor(
    val arcLarge: TextStyle,
    val arcMedium: TextStyle,
    val arcSmall: TextStyle,
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val bodyExtraSmall: TextStyle,
    val numeralExtraLarge: TextStyle,
    val numeralLarge: TextStyle,
    val numeralMedium: TextStyle,
    val numeralSmall: TextStyle,
    val numeralExtraSmall: TextStyle,
) {
    constructor(
        defaultFontFamily: FontFamily = FontFamily.Default,
        arcLarge: TextStyle = TypographyTokens.ArcLarge,
        arcMedium: TextStyle = TypographyTokens.ArcMedium,
        arcSmall: TextStyle = TypographyTokens.ArcSmall,
        displayLarge: TextStyle = TypographyTokens.DisplayLarge,
        displayMedium: TextStyle = TypographyTokens.DisplayMedium,
        displaySmall: TextStyle = TypographyTokens.DisplaySmall,
        titleLarge: TextStyle = TypographyTokens.TitleLarge,
        titleMedium: TextStyle = TypographyTokens.TitleMedium,
        titleSmall: TextStyle = TypographyTokens.TitleSmall,
        labelLarge: TextStyle = TypographyTokens.LabelLarge,
        labelMedium: TextStyle = TypographyTokens.LabelMedium,
        labelSmall: TextStyle = TypographyTokens.LabelSmall,
        bodyLarge: TextStyle = TypographyTokens.BodyLarge,
        bodyMedium: TextStyle = TypographyTokens.BodyMedium,
        bodySmall: TextStyle = TypographyTokens.BodySmall,
        bodyExtraSmall: TextStyle = TypographyTokens.BodyExtraSmall,
        numeralExtraLarge: TextStyle = TypographyTokens.NumeralExtraLarge,
        numeralLarge: TextStyle = TypographyTokens.NumeralLarge,
        numeralMedium: TextStyle = TypographyTokens.NumeralMedium,
        numeralSmall: TextStyle = TypographyTokens.NumeralSmall,
        numeralExtraSmall: TextStyle = TypographyTokens.NumeralExtraSmall,
    ) : this(
        arcLarge = arcLarge.withDefaultFontFamily(defaultFontFamily),
        arcMedium = arcMedium.withDefaultFontFamily(defaultFontFamily),
        arcSmall = arcSmall.withDefaultFontFamily(defaultFontFamily),
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
        bodyExtraSmall = bodyExtraSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraLarge = numeralExtraLarge.withDefaultFontFamily(defaultFontFamily),
        numeralLarge = numeralLarge.withDefaultFontFamily(defaultFontFamily),
        numeralMedium = numeralMedium.withDefaultFontFamily(defaultFontFamily),
        numeralSmall = numeralSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraSmall = numeralExtraSmall.withDefaultFontFamily(defaultFontFamily),
    )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    fun copy(
        arcLarge: TextStyle = this.arcLarge,
        arcMedium: TextStyle = this.arcMedium,
        arcSmall: TextStyle = this.arcSmall,
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
        bodyExtraSmall: TextStyle = this.bodyExtraSmall,
        numeralExtraLarge: TextStyle = this.numeralExtraLarge,
        numeralLarge: TextStyle = this.numeralLarge,
        numeralMedium: TextStyle = this.numeralMedium,
        numeralSmall: TextStyle = this.numeralSmall,
        numeralExtraSmall: TextStyle = this.numeralExtraSmall,
    ): Typography =
        Typography(
            arcLarge,
            arcMedium,
            arcSmall,
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
            bodyExtraSmall,
            numeralExtraLarge,
            numeralLarge,
            numeralMedium,
            numeralSmall,
            numeralExtraSmall,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (arcLarge != other.arcLarge) return false
        if (arcMedium != other.arcMedium) return false
        if (arcSmall != other.arcSmall) return false
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
        if (numeralExtraLarge != other.numeralExtraLarge) return false
        if (numeralLarge != other.numeralLarge) return false
        if (numeralMedium != other.numeralMedium) return false
        if (numeralSmall != other.numeralSmall) return false
        if (numeralExtraSmall != other.numeralExtraSmall) return false

        return true
    }

    override fun hashCode(): Int {
        var result = arcLarge.hashCode()
        result = 31 * result + arcMedium.hashCode()
        result = 31 * result + arcSmall.hashCode()
        result = 31 * result + displayLarge.hashCode()
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
        result = 31 * result + numeralExtraLarge.hashCode()
        result = 31 * result + numeralLarge.hashCode()
        result = 31 * result + numeralMedium.hashCode()
        result = 31 * result + numeralSmall.hashCode()
        result = 31 * result + numeralExtraSmall.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(" +
            "arcLarge=$arcLarge, " +
            "arcMedium=$arcMedium, " +
            "arcSmall=$arcSmall, " +
            "displayLarge=$displayLarge, " +
            "displayMedium=$displayMedium, " +
            "displaySmall=$displaySmall, " +
            "titleLarge=$titleLarge, " +
            "titleMedium=$titleMedium, " +
            "titleSmall=$titleSmall, " +
            "labelLarge=$labelLarge, " +
            "labelMedium=$labelMedium, " +
            "labelSmall=$labelSmall, " +
            "bodyLarge=$bodyLarge, " +
            "bodyMedium=$bodyMedium, " +
            "bodySmall=$bodySmall, " +
            "bodyExtraSmall=$bodyExtraSmall)" +
            "numeralExtraLarge=$numeralExtraLarge, " +
            "numeralLarge=$numeralLarge, " +
            "numeralMedium=$numeralMedium, " +
            "numeralSmall=$numeralSmall, " +
            "numeralExtraSmall=$numeralExtraSmall)"
    }
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as the
 *   [FontFamily].
 */
private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

private const val DefaultIncludeFontPadding = false

internal val DefaultLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

/** Returns theme default [TextStyle] with default [PlatformTextStyle]. */
internal val DefaultTextStyle =
    TextStyle.Default.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = DefaultIncludeFontPadding),
        lineHeightStyle = DefaultLineHeightStyle,
    )

/** Helper function for typography tokens. */
internal fun Typography.fromToken(value: TypographyKeyTokens): TextStyle {
    return when (value) {
        TypographyKeyTokens.ArcLarge -> arcLarge
        TypographyKeyTokens.ArcMedium -> arcMedium
        TypographyKeyTokens.ArcSmall -> arcSmall
        TypographyKeyTokens.DisplayLarge -> displayLarge
        TypographyKeyTokens.DisplayMedium -> displayMedium
        TypographyKeyTokens.DisplaySmall -> displaySmall
        TypographyKeyTokens.TitleLarge -> titleLarge
        TypographyKeyTokens.TitleMedium -> titleMedium
        TypographyKeyTokens.TitleSmall -> titleSmall
        TypographyKeyTokens.LabelLarge -> labelLarge
        TypographyKeyTokens.LabelMedium -> labelMedium
        TypographyKeyTokens.LabelSmall -> labelSmall
        TypographyKeyTokens.BodyLarge -> bodyLarge
        TypographyKeyTokens.BodyMedium -> bodyMedium
        TypographyKeyTokens.BodySmall -> bodySmall
        TypographyKeyTokens.BodyExtraSmall -> bodyExtraSmall
        TypographyKeyTokens.NumeralExtraLarge -> numeralExtraLarge
        TypographyKeyTokens.NumeralLarge -> numeralLarge
        TypographyKeyTokens.NumeralMedium -> numeralMedium
        TypographyKeyTokens.NumeralSmall -> numeralSmall
        TypographyKeyTokens.NumeralExtraSmall -> numeralExtraSmall
    }
}

/**
 * Converts the [TypographyKeyTokens] to the local text style provided by the theme. The text style
 * refers to the [LocalTypography].
 */
internal val TypographyKeyTokens.value: TextStyle
    @Composable @ReadOnlyComposable get() = MaterialTheme.typography.fromToken(this)

/**
 * This Ambient holds on to the current definition of typography for this application as described
 * by the Wear Material spec. You can read the values in it when creating custom components that
 * want to use Wear Material types, as well as override the values when you want to re-style a part
 * of your hierarchy. Material components related to text such as Button will use this Ambient to
 * set values with which to style children text components.
 *
 * To access values within this ambient, use [MaterialTheme.typography].
 */
internal val LocalTypography = staticCompositionLocalOf { Typography() }
