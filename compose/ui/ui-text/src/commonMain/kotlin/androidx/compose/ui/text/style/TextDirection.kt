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
 * Defines the algorithm to be used while determining the text direction.
 *
 * @property value The integer representation of TextDirection.
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
        /** Always sets the text direction to be Left to Right. */
        val Ltr = TextDirection(1)

        /** Always sets the text direction to be Right to Left. */
        val Rtl = TextDirection(2)

        /**
         * This value indicates that the text direction depends on the first strong directional
         * character in the text according to the Unicode Bidirectional Algorithm. If no strong
         * directional character is present, then [androidx.compose.ui.unit.LayoutDirection] is used
         * to resolve the final TextDirection.
         * * if used while creating a Paragraph object, [androidx.compose.ui.text.intl.LocaleList]
         *   will be used to resolve the direction as a fallback instead of
         *   [androidx.compose.ui.unit.LayoutDirection].
         */
        val Content = TextDirection(3)

        /**
         * This value indicates that the text direction depends on the first strong directional
         * character in the text according to the Unicode Bidirectional Algorithm. If no strong
         * directional character is present, then Left to Right will be used as the default
         * direction.
         */
        val ContentOrLtr = TextDirection(4)

        /**
         * This value indicates that the text direction depends on the first strong directional
         * character in the text according to the Unicode Bidirectional Algorithm. If no strong
         * directional character is present, then Right to Left will be used as the default
         * direction.
         */
        val ContentOrRtl = TextDirection(5)

        /**
         * This represents an unset value, a usual replacement for "null" when a primitive value is
         * desired.
         */
        val Unspecified = TextDirection(0)

        /**
         * Creates a TextDirection from the given integer value. This can be useful if you need to
         * serialize/deserialize TextDirection values.
         *
         * @param value The integer representation of the TextDirection.
         * @throws IllegalArgumentException if the given [value] is not recognized.
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
