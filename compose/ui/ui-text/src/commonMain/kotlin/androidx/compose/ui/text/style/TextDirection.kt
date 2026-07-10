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

package androidx.compose.ui.text.style

import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.internal.requirePrecondition

/**
 * Defines the algorithm used to determine text direction.
 *
 * @property value internal integer representation of the text direction.
 * @see ResolvedTextDirection
 */
@kotlin.jvm.JvmInline
value class TextDirection internal constructor(val value: Int) {

    override fun toString(): String {
        return when (this) {
            Ltr -> "Ltr"
            Rtl -> "Rtl"
            Content -> "Content"
            ContentOrLtr -> "ContentOrLtr"
            ContentOrRtl -> "ContentOrRtl"
            Unspecified -> "Unspecified"
            else -> "Invalid"
        }
    }

    companion object {
        /** Sets the text direction to Left-to-Right. */
        val Ltr = TextDirection(1)

        /** Sets the text direction to Right-to-Left. */
        val Rtl = TextDirection(2)

        /**
         * Resolves direction using the first strong directional character according to the Unicode
         * Bidirectional Algorithm.
         *
         * If no strong directional characters are present, falls back to
         * [androidx.compose.ui.unit.LayoutDirection], or to
         * [androidx.compose.ui.text.intl.LocaleList] when creating a
         * [androidx.compose.ui.text.Paragraph] (ignoring
         * [androidx.compose.ui.unit.LayoutDirection]).
         */
        val Content = TextDirection(3)

        /**
         * Resolves direction based on the first strong directional character according to the
         * Unicode Bidirectional Algorithm.
         *
         * Falls back to Left-to-Right if no strong directional characters are found.
         */
        val ContentOrLtr = TextDirection(4)

        /**
         * Resolves direction based on the first strong directional character according to the
         * Unicode Bidirectional Algorithm.
         *
         * Falls back to Right-to-Left if no strong directional characters are found.
         */
        val ContentOrRtl = TextDirection(5)

        /** Represents an unset [TextDirection] value. */
        val Unspecified = TextDirection(0)

        /**
         * Creates [TextDirection] from [value].
         *
         * Useful for serialization/deserialization.
         *
         * @param value internal integer representation.
         * @throws IllegalArgumentException if [value] is invalid.
         * @see androidx.compose.ui.text.style.TextDirection.value
         */
        fun valueOf(value: Int): TextDirection {
            requirePrecondition(value in 0..5) {
                "The given value=$value is not recognized by TextDirection."
            }
            return TextDirection(value)
        }
    }
}

/**
 * Returns `true` if this [TextDirection] is not [TextDirection.Unspecified].
 *
 * @see TextDirection.Unspecified
 */
inline val TextDirection.isSpecified: Boolean
    get() = value != 0

/**
 * If [isSpecified] is true then this is returned, otherwise [block] is executed and its result is
 * returned.
 */
inline fun TextDirection.takeOrElse(block: () -> TextDirection): TextDirection {
    return if (isSpecified) this else block()
}
