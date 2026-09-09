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

package androidx.compose.ui.text.intl

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

/**
 * A `Locale` object represents a specific geographical, political, or cultural region. An operation
 * that requires a `Locale` to perform its task is called _locale-sensitive_ and uses the `Locale`
 * to tailor information for the user. For example, displaying a number is a locale-sensitive
 * operation— the number should be formatted according to the customs and conventions of the user's
 * native country, region, or culture.
 *
 * @see TextStyle
 * @see SpanStyle
 */
@Immutable
public expect class Locale {
    public companion object {
        /** Returns a [Locale] object which represents current locale */
        @Deprecated(
            "Locale.current is not backed by snapshot state and does not notify readers on updates. " +
                "In @Composable functions, use LocalLocale.current instead. Outside composables, " +
                "pass the current locale explicitly from an observable state source.",
            replaceWith =
                ReplaceWith("LocalLocale.current", "androidx.compose.ui.platform.LocalLocale"),
        )
        public val current: Locale
    }

    /**
     * Create Locale object from a language tag.
     *
     * @param languageTag A [IETF BCP47](https://tools.ietf.org/html/bcp47) compliant language tag.
     * @return a locale object
     */
    public constructor(languageTag: String)

    /** The ISO 639 compliant language code. */
    public val language: String

    /** The ISO 15924 compliant 4-letter script code. */
    public val script: String

    /** The ISO 3166 compliant region code. */
    public val region: String

    /**
     * Returns a IETF BCP47 compliant language tag representation of this Locale.
     *
     * @return A IETF BCP47 compliant language tag.
     */
    public fun toLanguageTag(): String

    public override fun equals(other: Any?): Boolean

    public override fun hashCode(): Int

    public override fun toString(): String
}
