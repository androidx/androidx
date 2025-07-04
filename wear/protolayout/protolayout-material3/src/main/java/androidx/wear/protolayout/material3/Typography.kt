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
package androidx.wear.protolayout.material3

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.material3.Versions.isAtLeastBaklava
import androidx.wear.protolayout.material3.tokens.TextStyle
import androidx.wear.protolayout.material3.tokens.TypographyTokens
import androidx.wear.protolayout.material3.tokens.TypographyTokensApi36

/**
 * Class holding typography definitions as defined by the Wear Material3 typography specification.
 *
 * Note that, on API 36 and above, typography styles changes slightly to better accommodate layouts
 * being in the system font defined by each device.
 */
// Don't override this file automatically as it has checks for versioning of OS to apply different
// numbers.
public object Typography {
    /** Returns the [TextStyle] from the typography tokens for the given token name. */
    internal fun fromToken(@TypographyToken typographyToken: Int): TextStyle {
        return if (isAtLeastBaklava()) {
            fromTokenSystemFont(typographyToken)
        } else {
            fromTokenSpecificFont(typographyToken)
        }
    }

    /** Typography spec for OS versions below API 36, when specific font is used. */
    private fun fromTokenSpecificFont(@TypographyToken typographyToken: Int): TextStyle {
        return when (typographyToken) {
            BODY_EXTRA_SMALL -> TypographyTokens.BODY_EXTRA_SMALL
            BODY_LARGE -> TypographyTokens.BODY_LARGE
            BODY_MEDIUM -> TypographyTokens.BODY_MEDIUM
            BODY_SMALL -> TypographyTokens.BODY_SMALL
            DISPLAY_LARGE -> TypographyTokens.DISPLAY_LARGE
            DISPLAY_MEDIUM -> TypographyTokens.DISPLAY_MEDIUM
            DISPLAY_SMALL -> TypographyTokens.DISPLAY_SMALL
            LABEL_LARGE -> TypographyTokens.LABEL_LARGE
            LABEL_MEDIUM -> TypographyTokens.LABEL_MEDIUM
            LABEL_SMALL -> TypographyTokens.LABEL_SMALL
            NUMERAL_EXTRA_LARGE -> TypographyTokens.NUMERAL_EXTRA_LARGE
            NUMERAL_EXTRA_SMALL -> TypographyTokens.NUMERAL_EXTRA_SMALL
            NUMERAL_LARGE -> TypographyTokens.NUMERAL_LARGE
            NUMERAL_MEDIUM -> TypographyTokens.NUMERAL_MEDIUM
            NUMERAL_SMALL -> TypographyTokens.NUMERAL_SMALL
            TITLE_LARGE -> TypographyTokens.TITLE_LARGE
            TITLE_MEDIUM -> TypographyTokens.TITLE_MEDIUM
            TITLE_SMALL -> TypographyTokens.TITLE_SMALL
            else -> throw IllegalArgumentException("Typography $typographyToken does not exit.")
        }
    }

    /** Typography spec for OS versions of API 36 and above, when sysetm font is used. */
    private fun fromTokenSystemFont(@TypographyToken typographyToken: Int): TextStyle {
        return when (typographyToken) {
            BODY_EXTRA_SMALL -> TypographyTokensApi36.BODY_EXTRA_SMALL
            BODY_LARGE -> TypographyTokensApi36.BODY_LARGE
            BODY_MEDIUM -> TypographyTokensApi36.BODY_MEDIUM
            BODY_SMALL -> TypographyTokensApi36.BODY_SMALL
            DISPLAY_LARGE -> TypographyTokensApi36.DISPLAY_LARGE
            DISPLAY_MEDIUM -> TypographyTokensApi36.DISPLAY_MEDIUM
            DISPLAY_SMALL -> TypographyTokensApi36.DISPLAY_SMALL
            LABEL_LARGE -> TypographyTokensApi36.LABEL_LARGE
            LABEL_MEDIUM -> TypographyTokensApi36.LABEL_MEDIUM
            LABEL_SMALL -> TypographyTokensApi36.LABEL_SMALL
            NUMERAL_EXTRA_LARGE -> TypographyTokensApi36.NUMERAL_EXTRA_LARGE
            NUMERAL_EXTRA_SMALL -> TypographyTokensApi36.NUMERAL_EXTRA_SMALL
            NUMERAL_LARGE -> TypographyTokensApi36.NUMERAL_LARGE
            NUMERAL_MEDIUM -> TypographyTokensApi36.NUMERAL_MEDIUM
            NUMERAL_SMALL -> TypographyTokensApi36.NUMERAL_SMALL
            TITLE_LARGE -> TypographyTokensApi36.TITLE_LARGE
            TITLE_MEDIUM -> TypographyTokensApi36.TITLE_MEDIUM
            TITLE_SMALL -> TypographyTokensApi36.TITLE_SMALL
            else -> throw IllegalArgumentException("Typography $typographyToken does not exit.")
        }
    }

    /**
     * BodyExtraSmall is the smallest body. Body texts are typically used for long-form writing as
     * it works well for small text sizes. For longer sections of text, a serif or sans serif
     * typeface is recommended.
     */
    public const val BODY_EXTRA_SMALL: Int = 2

    /**
     * BodyLarge is the largest body. Body texts are typically used for long-form writing as it
     * works well for small text sizes. For longer sections of text, a serif or sans serif typeface
     * is recommended.
     */
    public const val BODY_LARGE: Int = 3

    /**
     * BodyMedium is second largest body. Body texts are typically used for long-form writing as it
     * works well for small text sizes. For longer sections of text, a serif or sans serif typeface
     * is recommended.
     */
    public const val BODY_MEDIUM: Int = 4

    /**
     * BodySmall is third largest body. Body texts are typically used for long-form writing as it
     * works well for small text sizes. For longer sections of text, a serif or sans serif typeface
     * is recommended.
     */
    public const val BODY_SMALL: Int = 5

    /**
     * DisplayLarge is the largest headline. Displays are the largest text on the screen, reserved
     * for short, important text.
     */
    public const val DISPLAY_LARGE: Int = 6

    /**
     * DisplayMedium is the second largest headline. Displays are the largest text on the screen,
     * reserved for short, important text.
     */
    public const val DISPLAY_MEDIUM: Int = 7

    /**
     * DisplaySmall is the smallest headline. Displays are the largest text on the screen, reserved
     * for short, important text.
     */
    public const val DISPLAY_SMALL: Int = 8

    /**
     * LabelLarge is the largest label. They are used for displaying prominent texts like label on
     * title buttons.
     */
    public const val LABEL_LARGE: Int = 9

    /**
     * LabelMedium is the medium label. They are used for displaying texts like primary label on
     * buttons.
     */
    public const val LABEL_MEDIUM: Int = 10

    /**
     * LabelSmall is the small label. They are used for displaying texts like secondary label on
     * buttons, labels on compact buttons.
     */
    public const val LABEL_SMALL: Int = 11

    /**
     * NumeralsExtraLarge is the largest role for digits. Numerals use tabular spacing by default.
     * They highlight and express glanceable numbers that are limited to a two or three characters
     * only, where no localization is required like the charging screen.
     */
    public const val NUMERAL_EXTRA_LARGE: Int = 12

    /**
     * NumeralsExtraSmall is the smallest role for digits. Numerals use tabular spacing by default.
     * They are for numbers that need to accommodate longer strings of digits, where no localization
     * is required like in-workout metrics.
     */
    public const val NUMERAL_EXTRA_SMALL: Int = 13

    /**
     * NumeralsLarge is the second largest role for digits. Numerals use tabular spacing by default.
     * They are large sized number strings that are limited to big displays of time, where no
     * localization is required like a timer countdown.
     */
    public const val NUMERAL_LARGE: Int = 14

    /**
     * NumeralsMedium is the third largest role for digits. Numerals use tabular spacing by default.
     * They are medium sized numbers that are limited to short strings of digits, where no
     * localization is required like a steps count.
     */
    public const val NUMERAL_MEDIUM: Int = 15

    /**
     * NumeralsSmall is the fourth largest role for digits. Numerals use tabular spacing by default.
     * They are for numbers that need emphasis at a smaller scale, where no localization is required
     * like date and time pickers.
     */
    public const val NUMERAL_SMALL: Int = 16

    /**
     * TitleLarge is the largest title. Titles are smaller than Displays. They are typically
     * reserved for medium-emphasis text that is shorter in length.
     */
    public const val TITLE_LARGE: Int = 17

    /**
     * TitleMedium is the medium title. Titles are smaller than Displays. They are typically
     * reserved for medium-emphasis text that is shorter in length.
     */
    public const val TITLE_MEDIUM: Int = 18

    /**
     * TitleSmall is the smallest title. Titles are smaller than Displays. They are typically
     * reserved for medium-emphasis text that is shorter in length.
     */
    public const val TITLE_SMALL: Int = 19

    internal const val TOKEN_COUNT = 20

    /** The referencing token names for a range of contrasting text style in Material3. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        BODY_EXTRA_SMALL,
        BODY_LARGE,
        BODY_MEDIUM,
        BODY_SMALL,
        DISPLAY_LARGE,
        DISPLAY_MEDIUM,
        DISPLAY_SMALL,
        LABEL_LARGE,
        LABEL_MEDIUM,
        LABEL_SMALL,
        NUMERAL_EXTRA_LARGE,
        NUMERAL_EXTRA_SMALL,
        NUMERAL_LARGE,
        NUMERAL_MEDIUM,
        NUMERAL_SMALL,
        TITLE_LARGE,
        TITLE_MEDIUM,
        TITLE_SMALL,
    )
    public annotation class TypographyToken
}
