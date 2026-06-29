/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.runtime.Stable

/**
 * The keyboard type to be used in `KeyboardOptions`.
 *
 * Note that this input type is honored by keyboard and shows corresponding keyboard but this is not
 * guaranteed. For example, some keyboards may send non-ASCII character even if you set
 * [KeyboardType.Ascii].
 */
@kotlin.jvm.JvmInline
value class KeyboardType private constructor(@Suppress("unused") private val value: Int) {

    override fun toString(): String {
        return when (this) {
            Unspecified -> "Unspecified"
            Text -> "Text"
            Ascii -> "Ascii"
            Number -> "Number"
            Phone -> "Phone"
            Uri -> "Uri"
            Email -> "Email"
            Password -> "Password"
            NumberPassword -> "NumberPassword"
            Decimal -> "Decimal"
            PasswordVisible -> "PasswordVisible"
            PostalAddress -> "PostalAddress"
            PersonName -> "PersonName"
            EmailSubject -> "EmailSubject"
            ShortMessage -> "ShortMessage"
            LongMessage -> "LongMessage"
            Filter -> "Filter"
            Phonetic -> "Phonetic"
            DateTime -> "DateTime"
            Date -> "Date"
            Time -> "Time"
            NumberSigned -> "NumberSigned"
            DecimalSigned -> "DecimalSigned"
            DecimalPassword -> "DecimalPassword"
            NumberPasswordSigned -> "NumberPasswordSigned"
            DecimalPasswordSigned -> "DecimalPasswordSigned"
            else -> "Invalid"
        }
    }

    companion object {
        /** The keyboard type is not specified. */
        @Stable val Unspecified: KeyboardType = KeyboardType(0)

        /**
         * Shows the standard text-based keyboard layout with auto-capitalization and spelling
         * suggestions.
         *
         * **When to use it**: For standard text entries (e.g. chat messaging, renaming
         * folders/routines, or general comments).
         */
        @Stable val Text: KeyboardType = KeyboardType(1)

        /**
         * Forces the keyboard to display Latin characters.
         *
         * **When to use it**: Ideal for usernames, system database IDs, or passcodes where you want
         * to restrict input to Latin characters.
         *
         * Note: Unlike other technical input types (like [Uri] or [Email]) which typically disable
         * auto-correct automatically, this type does not. Use
         * [androidx.compose.foundation.text.KeyboardOptions.autoCorrectEnabled] to disable it if
         * needed.
         */
        @Stable val Ascii: KeyboardType = KeyboardType(2)

        /**
         * Displays a numeric keypad with only digits 0-9.
         *
         * **When to use it**: Perfect for plain positive integers (e.g., specifying item counts,
         * loops, or maximum history buffer settings).
         *
         * Note: Lacks decimal points and positive/negative (+/-) signs.
         */
        @Stable val Number: KeyboardType = KeyboardType(3)

        /**
         * Displays a telephone dialer keypad. Includes numbers 0-9, and symbols like `*`, `#`, and
         * `+` for phone number formatting.
         *
         * **When to use it**: Phone number input fields.
         */
        @Stable val Phone: KeyboardType = KeyboardType(4)

        /**
         * Optimizes the keyboard for typing web links / URLs. Prominently displays `/` and `.com`
         * shortcuts next to the spacebar to save keystrokes.
         *
         * **When to use it**: Web address and URL entry forms.
         *
         * Note: IMEs typically disable auto-correct and suggestions for this type.
         */
        @Stable val Uri: KeyboardType = KeyboardType(5)

        /**
         * Optimizes the keyboard for typing email addresses. Prominently displays `@` and `.` near
         * the spacebar.
         *
         * **When to use it**: Login credential lines or registration forms.
         *
         * Note: IMEs typically disable auto-correct for this type.
         */
        @Stable val Email: KeyboardType = KeyboardType(6)

        /**
         * Shows a standard masked text-based keyboard layout. Masks all typed characters with
         * dots/asterisks for privacy.
         *
         * **When to use it**: Secured credentials or login screens.
         *
         * Note: Disables autocorrect and spelling suggestions, and prevents the keyboard from
         * learning your text.
         */
        @Stable val Password: KeyboardType = KeyboardType(7)

        /**
         * Shows a masked numeric keypad (0-9) for secure PIN entry.
         *
         * **When to use it**: Entering masked 4-digit employee authentication PINs, lockscreen
         * passcodes, or secure one-time verification codes (OTPs).
         */
        @Stable val NumberPassword: KeyboardType = KeyboardType(8)

        /**
         * Displays a numeric keypad containing a decimal point key (`.` or `,` depending on
         * locale).
         *
         * **When to use it**: Entering numbers that require decimal points, such as prices,
         * weights, or coordinates.
         */
        @Stable val Decimal: KeyboardType = KeyboardType(9)

        /**
         * Shows a text-based password layout, but the typed text remains visible (unmasked).
         *
         * **When to use it**: Toggle visibility password text fields. Pair it with standard
         * [Password] (masked) using an eye-icon button to let the user reveal what they typed.
         */
        @Stable val PasswordVisible: KeyboardType = KeyboardType(10)

        /** Optimizes the keyboard for entering shipping or mailing addresses. */
        @Stable val PostalAddress: KeyboardType = KeyboardType(11)

        /** Optimizes the keyboard for entering names, typically capitalizing each word. */
        @Stable val PersonName: KeyboardType = KeyboardType(12)

        /** Optimizes the keyboard for email subject lines. */
        @Stable val EmailSubject: KeyboardType = KeyboardType(13)

        /** Optimizes the keyboard for sending short instant messages (SMS). */
        @Stable val ShortMessage: KeyboardType = KeyboardType(14)

        /** Optimizes the keyboard for writing emails or other long-form content. */
        @Stable val LongMessage: KeyboardType = KeyboardType(15)

        /** Optimizes the keyboard for filtering lists or search queries. */
        @Stable val Filter: KeyboardType = KeyboardType(16)

        /** Keyboard layout optimized for entering phonetic spellings or pronunciation guides. */
        @Stable val Phonetic: KeyboardType = KeyboardType(17)

        /** Displays a numeric keypad optimized for entering both dates and times. */
        @Stable val DateTime: KeyboardType = KeyboardType(18)

        /** Displays a numeric keypad optimized for date entry, typically containing `/` or `-`. */
        @Stable val Date: KeyboardType = KeyboardType(19)

        /** Displays a numeric keypad optimized for time entry, typically containing `:`. */
        @Stable val Time: KeyboardType = KeyboardType(20)

        /** Displays a numeric keypad showing 0-9 and a negative/positive sign key (`+/-`). */
        @Stable val NumberSigned: KeyboardType = KeyboardType(21)

        /** Displays a numeric keypad showing 0-9, decimal separators, and sign keys. */
        @Stable val DecimalSigned: KeyboardType = KeyboardType(22)

        /** Shows a masked numeric keypad containing decimal separators. */
        @Stable val DecimalPassword: KeyboardType = KeyboardType(23)

        /** Shows a masked numeric keypad containing positive/negative signs. */
        @Stable val NumberPasswordSigned: KeyboardType = KeyboardType(24)

        /** Shows a masked numeric keypad containing both decimal separators and signs. */
        @Stable val DecimalPasswordSigned: KeyboardType = KeyboardType(25)
    }
}
