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
 * Aligns text horizontally within its container.
 *
 * @property value internal integer representation of the text alignment.
 */
@kotlin.jvm.JvmInline
public value class TextAlign internal constructor(public val value: Int) {

    public override fun toString(): String {
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

    public companion object {
        /** Aligns text to the left edge. */
        public val Left: TextAlign
            get() = TextAlign(1)

        /** Aligns text to the right edge. */
        public val Right: TextAlign
            get() = TextAlign(2)

        /** Aligns text to the center. */
        public val Center: TextAlign
            get() = TextAlign(3)

        /**
         * Stretches lines of text to fill the container width.
         *
         * Lines ending with hard line breaks align to [Start].
         */
        public val Justify: TextAlign
            get() = TextAlign(4)

        /**
         * Aligns text to the leading edge.
         *
         * Maps to the left edge for LTR, and the right edge for RTL.
         */
        public val Start: TextAlign
            get() = TextAlign(5)

        /**
         * Aligns text to the trailing edge.
         *
         * Maps to the right edge for LTR, and the left edge for RTL.
         */
        public val End: TextAlign
            get() = TextAlign(6)

        /** Represents an unset [TextAlign] value. */
        public val Unspecified: TextAlign
            get() = TextAlign(0)

        /** Return a list containing all possible values of TextAlign. */
        public fun values(): List<TextAlign> = listOf(Left, Right, Center, Justify, Start, End)

        /**
         * Creates [TextAlign] from [value].
         *
         * Useful for serialization/deserialization.
         *
         * @param value internal integer representation.
         * @throws IllegalArgumentException if [value] is invalid.
         * @see [TextAlign.value]
         */
        public fun valueOf(value: Int): TextAlign {
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
public inline val TextAlign.isSpecified: Boolean
    get() = value != 0

/**
 * If [isSpecified] is true then this is returned, otherwise [block] is executed and its result is
 * returned.
 */
public inline fun TextAlign.takeOrElse(block: () -> TextAlign): TextAlign {
    return if (isSpecified) this else block()
}
