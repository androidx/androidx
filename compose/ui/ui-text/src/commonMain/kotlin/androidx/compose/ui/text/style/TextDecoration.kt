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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.internal.requirePrecondition
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastJoinToString

/**
 * Defines text decorations such as underline or line-through.
 *
 * @property mask bitmask representing the combined decorations.
 */
@Immutable
class TextDecoration internal constructor(val mask: Int) {

    companion object {
        @Stable val None: TextDecoration = TextDecoration(0x0)

        /**
         * Draws a horizontal line below the text.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationUnderlineSample
         */
        @Stable val Underline: TextDecoration = TextDecoration(0x1)

        /**
         * Draws a horizontal line over the text.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationLineThroughSample
         */
        @Stable val LineThrough: TextDecoration = TextDecoration(0x2)

        /**
         * Combines multiple [TextDecoration]s into a single decoration.
         *
         * @sample androidx.compose.ui.text.samples.TextDecorationCombinedSample
         * @param decorations decorations to combine.
         */
        fun combine(decorations: List<TextDecoration>): TextDecoration {
            val mask = decorations.fastFold(0) { acc, decoration -> acc or decoration.mask }
            return TextDecoration(mask)
        }

        /**
         * Creates a [TextDecoration] from a [mask].
         *
         * Attempts to avoid allocations for well-known decorations.
         *
         * @param mask bitmask of the decoration.
         * @throws IllegalArgumentException if [mask] is invalid.
         * @see androidx.compose.ui.text.style.TextDecoration.mask
         */
        fun valueOf(mask: Int): TextDecoration {
            // Prevent creating an invalid TextDecoration combination.
            requirePrecondition((mask or 0b11) == 0b11) {
                "The given mask=$mask is not recognized by TextDecoration."
            }
            return when (mask) {
                0 -> None
                1 -> Underline
                2 -> LineThrough
                else -> TextDecoration(mask)
            }
        }
    }

    /**
     * Combines this decoration with [decoration].
     *
     * @sample androidx.compose.ui.text.samples.TextDecorationCombinedSample
     */
    operator fun plus(decoration: TextDecoration): TextDecoration {
        return TextDecoration(this.mask or decoration.mask)
    }

    /**
     * Checks if this decoration contains [other].
     *
     * @param other decoration to check.
     */
    operator fun contains(other: TextDecoration): Boolean {
        return (mask or other.mask) == mask
    }

    override fun toString(): String {
        if (mask == 0) {
            return "TextDecoration.None"
        }

        val values: MutableList<String> = mutableListOf()
        if ((mask and Underline.mask) != 0) {
            values.add("Underline")
        }
        if ((mask and LineThrough.mask) != 0) {
            values.add("LineThrough")
        }
        if ((values.size == 1)) {
            return "TextDecoration.${values[0]}"
        }
        return "TextDecoration[${values.fastJoinToString(separator = ", ")}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextDecoration) return false
        if (mask != other.mask) return false
        return true
    }

    override fun hashCode(): Int {
        return mask
    }
}
