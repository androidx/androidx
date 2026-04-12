/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Values that specify the text wrapping, scrolling, and height measurement behavior for text
 * fields.
 *
 * @see SingleLine
 * @see MultiLine
 */
@Stable
sealed interface TextFieldLineLimits {

    /**
     * The text field is always a single line tall, ignores newlines in the text, and scrolls
     * horizontally when the text overflows.
     */
    object SingleLine : TextFieldLineLimits {

        override fun toString(): String {
            return "TextFieldLineLimits.SingleLine"
        }
    }

    /**
     * The text field will be at least [minHeightInLines] tall, if the text overflows it will wrap,
     * and if the text ends up being more than one line the field will grow until it is
     * [maxHeightInLines] tall and then start scrolling vertically.
     *
     * It is required that 1 ≤ [minHeightInLines] ≤ [maxHeightInLines].
     *
     * To specify the minimum and/or maximum height of the field in non-text units, such as dps, use
     * the [heightIn] modifier.
     */
    @Immutable
    class MultiLine(val minHeightInLines: Int = 1, val maxHeightInLines: Int = Int.MAX_VALUE) :
        TextFieldLineLimits {
        init {
            requirePrecondition(minHeightInLines in 1..maxHeightInLines) {
                "Expected 1 ≤ minHeightInLines ≤ maxHeightInLines, were " +
                    "$minHeightInLines, $maxHeightInLines"
            }
        }

        override fun toString(): String =
            "MultiLine(minHeightInLines=$minHeightInLines, maxHeightInLines=$maxHeightInLines)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other === null) return false
            if (this::class != other::class) return false
            other as MultiLine
            if (minHeightInLines != other.minHeightInLines) return false
            if (maxHeightInLines != other.maxHeightInLines) return false
            return true
        }

        override fun hashCode(): Int {
            var result = minHeightInLines
            result = 31 * result + maxHeightInLines
            return result
        }
    }

    companion object {
        val Default: TextFieldLineLimits = MultiLine()
    }
}
