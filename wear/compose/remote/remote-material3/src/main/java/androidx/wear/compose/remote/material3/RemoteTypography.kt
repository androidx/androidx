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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.Typography

/**
 * Class holding typography definitions for [RemoteMaterialTheme].
 *
 * The text styles in this typography are scaled according to the user's preferred font size in the
 * system settings. Larger font sizes scale slower in order to avoid pressure on screen space,
 * because they are already sufficiently accessible.
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
public class RemoteTypography(
    public val displayLarge: RemoteTextStyle,
    public val displayMedium: RemoteTextStyle,
    public val displaySmall: RemoteTextStyle,
    public val titleLarge: RemoteTextStyle,
    public val titleMedium: RemoteTextStyle,
    public val titleSmall: RemoteTextStyle,
    public val labelLarge: RemoteTextStyle,
    public val labelMedium: RemoteTextStyle,
    public val labelSmall: RemoteTextStyle,
    public val bodyLarge: RemoteTextStyle,
    public val bodyMedium: RemoteTextStyle,
    public val bodySmall: RemoteTextStyle,
    public val bodyExtraSmall: RemoteTextStyle,
    public val numeralExtraLarge: RemoteTextStyle,
    public val numeralLarge: RemoteTextStyle,
    public val numeralMedium: RemoteTextStyle,
    public val numeralSmall: RemoteTextStyle,
    public val numeralExtraSmall: RemoteTextStyle,
) {
    public constructor(
        defaultFontFamily: RemoteFontFamily = RemoteFontFamily.Default,
        displayLarge: RemoteTextStyle = RemoteTypographyTokens.DisplayLarge,
        displayMedium: RemoteTextStyle = RemoteTypographyTokens.DisplayMedium,
        displaySmall: RemoteTextStyle = RemoteTypographyTokens.DisplaySmall,
        titleLarge: RemoteTextStyle = RemoteTypographyTokens.TitleLarge,
        titleMedium: RemoteTextStyle = RemoteTypographyTokens.TitleMedium,
        titleSmall: RemoteTextStyle = RemoteTypographyTokens.TitleSmall,
        labelLarge: RemoteTextStyle = RemoteTypographyTokens.LabelLarge,
        labelMedium: RemoteTextStyle = RemoteTypographyTokens.LabelMedium,
        labelSmall: RemoteTextStyle = RemoteTypographyTokens.LabelSmall,
        bodyLarge: RemoteTextStyle = RemoteTypographyTokens.BodyLarge,
        bodyMedium: RemoteTextStyle = RemoteTypographyTokens.BodyMedium,
        bodySmall: RemoteTextStyle = RemoteTypographyTokens.BodySmall,
        bodyExtraSmall: RemoteTextStyle = RemoteTypographyTokens.BodyExtraSmall,
        numeralExtraLarge: RemoteTextStyle = RemoteTypographyTokens.NumeralExtraLarge,
        numeralLarge: RemoteTextStyle = RemoteTypographyTokens.NumeralLarge,
        numeralMedium: RemoteTextStyle = RemoteTypographyTokens.NumeralMedium,
        numeralSmall: RemoteTextStyle = RemoteTypographyTokens.NumeralSmall,
        numeralExtraSmall: RemoteTextStyle = RemoteTypographyTokens.NumeralExtraSmall,
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
        bodyExtraSmall = bodyExtraSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraLarge = numeralExtraLarge.withDefaultFontFamily(defaultFontFamily),
        numeralLarge = numeralLarge.withDefaultFontFamily(defaultFontFamily),
        numeralMedium = numeralMedium.withDefaultFontFamily(defaultFontFamily),
        numeralSmall = numeralSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraSmall = numeralExtraSmall.withDefaultFontFamily(defaultFontFamily),
    )

    /** Creates a copy of Wear Remote Compose [RemoteTypography] from Wear Compose [Typography]. */
    public constructor(typography: Typography) : this() {
        RemoteTypography(
            displayLarge = RemoteTextStyle.fromTextStyle(typography.displayLarge),
            displayMedium = RemoteTextStyle.fromTextStyle(typography.displayMedium),
            displaySmall = RemoteTextStyle.fromTextStyle(typography.displaySmall),
            titleLarge = RemoteTextStyle.fromTextStyle(typography.titleLarge),
            titleMedium = RemoteTextStyle.fromTextStyle(typography.titleMedium),
            titleSmall = RemoteTextStyle.fromTextStyle(typography.titleSmall),
            labelLarge = RemoteTextStyle.fromTextStyle(typography.labelLarge),
            labelMedium = RemoteTextStyle.fromTextStyle(typography.labelMedium),
            labelSmall = RemoteTextStyle.fromTextStyle(typography.labelSmall),
            bodyLarge = RemoteTextStyle.fromTextStyle(typography.bodyLarge),
            bodyMedium = RemoteTextStyle.fromTextStyle(typography.bodyMedium),
            bodySmall = RemoteTextStyle.fromTextStyle(typography.bodySmall),
            bodyExtraSmall = RemoteTextStyle.fromTextStyle(typography.bodyExtraSmall),
            numeralExtraLarge = RemoteTextStyle.fromTextStyle(typography.numeralExtraLarge),
            numeralLarge = RemoteTextStyle.fromTextStyle(typography.numeralLarge),
            numeralMedium = RemoteTextStyle.fromTextStyle(typography.numeralMedium),
            numeralSmall = RemoteTextStyle.fromTextStyle(typography.numeralSmall),
            numeralExtraSmall = RemoteTextStyle.fromTextStyle(typography.numeralExtraSmall),
        )
    }

    /** Returns a copy of this RemoteTypography, optionally overriding some of the values. */
    public fun copy(
        displayLarge: RemoteTextStyle = this.displayLarge,
        displayMedium: RemoteTextStyle = this.displayMedium,
        displaySmall: RemoteTextStyle = this.displaySmall,
        titleLarge: RemoteTextStyle = this.titleLarge,
        titleMedium: RemoteTextStyle = this.titleMedium,
        titleSmall: RemoteTextStyle = this.titleSmall,
        labelLarge: RemoteTextStyle = this.labelLarge,
        labelMedium: RemoteTextStyle = this.labelMedium,
        labelSmall: RemoteTextStyle = this.labelSmall,
        bodyLarge: RemoteTextStyle = this.bodyLarge,
        bodyMedium: RemoteTextStyle = this.bodyMedium,
        bodySmall: RemoteTextStyle = this.bodySmall,
        bodyExtraSmall: RemoteTextStyle = this.bodyExtraSmall,
        numeralExtraLarge: RemoteTextStyle = this.numeralExtraLarge,
        numeralLarge: RemoteTextStyle = this.numeralLarge,
        numeralMedium: RemoteTextStyle = this.numeralMedium,
        numeralSmall: RemoteTextStyle = this.numeralSmall,
        numeralExtraSmall: RemoteTextStyle = this.numeralExtraSmall,
    ): RemoteTypography =
        RemoteTypography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            bodyExtraSmall = bodyExtraSmall,
            numeralExtraLarge = numeralExtraLarge,
            numeralLarge = numeralLarge,
            numeralMedium = numeralMedium,
            numeralSmall = numeralSmall,
            numeralExtraSmall = numeralExtraSmall,
        )
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as the
 *   [FontFamily].
 */
private fun RemoteTextStyle.withDefaultFontFamily(default: RemoteFontFamily): RemoteTextStyle {
    return if (default == RemoteFontFamily.Default && fontFamily != null) this
    else copy(fontFamily = default)
}
