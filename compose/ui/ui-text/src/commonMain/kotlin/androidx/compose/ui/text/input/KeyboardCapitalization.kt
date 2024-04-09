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
 * Options to request software keyboard to capitalize the text. Applies to languages which
 * has upper-case and lower-case letters.
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
        /**
         * Capitalization behavior is not specified.
         */
        @Stable
        val Unspecified = KeyboardCapitalization(-1)

        /**
         * Do not auto-capitalize text.
         */
        @Stable
        val None = KeyboardCapitalization(0)

        /**
         * Capitalize all characters.
         */
        @Stable
        val Characters = KeyboardCapitalization(1)

        /**
         * Capitalize the first character of every word.
         */
        @Stable
        val Words = KeyboardCapitalization(2)

        /**
         * Capitalize the first character of each sentence.
         */
        @Stable
        val Sentences = KeyboardCapitalization(3)
    }
}
