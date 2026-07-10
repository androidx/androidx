/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.style

import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.internal.requirePrecondition
import kotlin.jvm.JvmInline

/**
 * Automatically hyphenates words when wrapping text.
 *
 * Inserts hyphens at syllable boundaries based on language rules.
 *
 * Suggest manual line break opportunities using:
 * - **Soft hyphen (`\u00AD`)**: Marks a wrapping opportunity. The hyphen is only visible if the
 *   word wraps at this point.
 * - **Hard hyphen (`\u2010`)**: Inserts a permanently visible hyphen that also allows wrapping.
 *
 * Default is [Hyphens.None] (no automatic hyphenation).
 *
 * @property value internal integer representation of the hyphenation mode.
 */
@JvmInline
value class Hyphens internal constructor(val value: Int) {
    companion object {
        /**
         * Lines will break with no hyphenation.
         *
         * "Hard" hyphens will still be respected. However, no automatic hyphenation will be
         * attempted. If a word must be broken due to being longer than a line, it will break at any
         * character and will not attempt to break at a syllable boundary.
         * <pre>
         * +---------+
         * | Experim |
         * | ental   |
         * +---------+
         * </pre>
         */
        val None = Hyphens(1)

        /**
         * Breaks words automatically at syllable boundaries.
         *
         * Manual suggestions (like soft hyphens) override automatic breaks.
         * <pre>
         * +---------+
         * | Experi- |
         * | mental  |
         * +---------+
         * </pre>
         */
        val Auto = Hyphens(2)

        /** Represents an unset [Hyphens] value. */
        val Unspecified = Hyphens(0)

        /**
         * Creates [Hyphens] from [value].
         *
         * Useful for serialization/deserialization.
         *
         * @param value internal integer representation.
         * @throws IllegalArgumentException if [value] is invalid.
         * @see androidx.compose.ui.text.style.Hyphens.value
         */
        fun valueOf(value: Int): Hyphens {
            requirePrecondition(value in 0..2) {
                "The given value=$value is not recognized by Hyphens."
            }
            return Hyphens(value)
        }
    }

    override fun toString() =
        when (this) {
            None -> "Hyphens.None"
            Auto -> "Hyphens.Auto"
            Unspecified -> "Hyphens.Unspecified"
            else -> "Invalid"
        }
}

/**
 * Returns `true` if it is not [Hyphens.Unspecified].
 *
 * @see Hyphens.Unspecified
 */
inline val Hyphens.isSpecified: Boolean
    get() = value != 0

/**
 * If [isSpecified] is true then this is returned, otherwise [block] is executed and its result is
 * returned.
 */
inline fun Hyphens.takeOrElse(block: () -> Hyphens): Hyphens {
    return if (isSpecified) this else block()
}
