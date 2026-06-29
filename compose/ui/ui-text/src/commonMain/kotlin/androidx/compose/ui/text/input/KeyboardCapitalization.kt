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
 * The capitalization style to be used in `KeyboardOptions`.
 *
 * Only applicable to text-based [KeyboardType]s such as [KeyboardType.Text] or
 * [KeyboardType.Ascii]. IMEs may ignore this option.
 */
@kotlin.jvm.JvmInline
value class KeyboardCapitalization private constructor(private val value: Int) {

    override fun toString(): String {
        return when (this) {
            Unspecified -> "Unspecified"
            None -> "None"
            Characters -> "Characters"
            Words -> "Words"
            Sentences -> "Sentences"
            else -> "Invalid"
        }
    }

    companion object {
        /** The capitalization behavior is not specified. */
        @Stable val Unspecified = KeyboardCapitalization(-1)

        /**
         * Disables auto-capitalization.
         *
         * **When to use it**: Ideal for passwords, email addresses, URLs, or search filters.
         */
        @Stable val None = KeyboardCapitalization(0)

        /**
         * Capitalizes all characters.
         *
         * **When to use it**: Ideal for coupon codes, state abbreviations, or license plates.
         */
        @Stable val Characters = KeyboardCapitalization(1)

        /**
         * Capitalizes the first character of every word.
         *
         * **When to use it**: Ideal for mailing addresses or contact names.
         */
        @Stable val Words = KeyboardCapitalization(2)

        /**
         * Capitalizes the first character of every sentence.
         *
         * **When to use it**: Ideal for chat messages, email bodies, or other free-form text.
         */
        @Stable val Sentences = KeyboardCapitalization(3)
    }
}
