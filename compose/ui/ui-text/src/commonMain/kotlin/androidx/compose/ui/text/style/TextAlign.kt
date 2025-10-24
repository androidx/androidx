/*
 * Copyright 2018 The Android Open Source Project
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
 * Defines how to align text horizontally. `TextAlign` controls how text aligns in the space it
 * appears.
 *
 * @property value The integer representation of the TextAlign.
 */
@kotlin.jvm.JvmInline
value class TextAlign internal constructor(val value: Int) {

    override fun toString(): String {
        return when (this) {
            Left -> "Left"
            Right -> "Right"
            Center -> "Center"
            Justify -> "Justify"
            Start -> "Start"
            End -> "End"
            Unspecified -> "Unspecified"
            else -> "Invalid"
        }
    }

    companion object {
        /** Align the text on the left edge of the container. */
        val Left = TextAlign(1)

        /** Align the text on the right edge of the container. */
        val Right = TextAlign(2)

        /** Align the text in the center of the container. */
        val Center = TextAlign(3)

        /**
         * Stretch lines of text that end with a soft line break to fill the width of the container.
         *
         * Lines that end with hard line breaks are aligned towards the [Start] edge.
         */
        val Justify = TextAlign(4)

        /**
         * Align the text on the leading edge of the container.
         *
         * For Left to Right text ([ResolvedTextDirection.Ltr]), this is the left edge.
         *
         * For Right to Left text ([ResolvedTextDirection.Rtl]), like Arabic, this is the right
         * edge.
         */
        val Start = TextAlign(5)

        /**
         * Align the text on the trailing edge of the container.
         *
         * For Left to Right text ([ResolvedTextDirection.Ltr]), this is the right edge.
         *
         * For Right to Left text ([ResolvedTextDirection.Rtl]), like Arabic, this is the left edge.
         */
        val End = TextAlign(6)

        /**
         * This represents an unset value, a usual replacement for "null" when a primitive value is
         * desired.
         */
        val Unspecified = TextAlign(0)

        /** Return a list containing all possible values of TextAlign. */
        fun values(): List<TextAlign> = listOf(Left, Right, Center, Justify, Start, End)

        /**
         * Creates a TextAlign from the given integer value. This can be useful if you need to
         * serialize/deserialize TextAlign values.
         *
         * This function throws an [IllegalArgumentException] if the given [value] is not recognized
         * by the preset [TextAlign] values.
         *
         * @param value The integer representation of the TextAlign.
         * @see [TextAlign.value]
         */
        fun valueOf(value: Int): TextAlign {
            requirePrecondition(value in 0..6) {
                "The given value=$value is not recognized by TextAlign."
            }
            return TextAlign(value)
        }
    }
}

/**
 * Returns `true` if this [TextAlign] is not [TextAlign.Unspecified].
 *
 * @see TextAlign.Unspecified
 */
inline val TextAlign.isSpecified: Boolean
    get() = value != 0

/**
 * If [isSpecified] is true then this is returned, otherwise [block] is executed and its result is
 * returned.
 */
inline fun TextAlign.takeOrElse(block: () -> TextAlign): TextAlign {
    return if (isSpecified) this else block()
}
