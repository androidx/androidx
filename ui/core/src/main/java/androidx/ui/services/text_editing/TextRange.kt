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

package androidx.ui.services.text_editing

/**
 * A range of characters in a string of text.
 *
 * The [start] and [end] arguments must not be null. Both the [start] and
 * [end] must either be greater than or equal to zero or both exactly -1.
 *
 * Instead of creating an empty text range, consider using the [empty]
 * constant.
 */

// @immutable
open class TextRange(
    /**
     * The index of the first character in the range.
     *
     * If [start] and [end] are both -1, the text range is empty.
     */
    val start: Int,

    /**
     * The next index after the characters in this range.
     *
     * If [start] and [end] are both -1, the text range is empty.
     */
    val end: Int
) {

    init {
        assert(start >= -1)
        assert(end >= -1)
    }

    companion object {
        /**
         * A text range that starts and ends at offset.
         *
         * The [offset] argument must be non-null and greater than or equal to -1.
         */
        fun collapsed(offset: Int): TextRange {
            assert(offset >= -1)
            return TextRange(
                start = offset,
                end = offset
            )
        }

        /** A text range that contains nothing and is not in the text. */
        val empty: TextRange = TextRange(start = -1, end = -1)
    }

    /** Whether this range represents a valid position in the text. */
    val isValid: Boolean
        get() = start >= 0 && end >= 0

    /** Whether this range is empty (but still potentially placed inside the text). */
    val isCollapsed: Boolean
        get() = start == end

    /** Whether the start of this range precedes the end. */
    val isNormalized: Boolean
        get() = end >= start

    /** The text before this range. */
    fun textBefore(text: String): String {
        assert(isNormalized)
        return text.substring(0, start)
    }

    /** The text after this range. */
    fun textAfter(text: String): String {
        assert(isNormalized)
        return text.substring(end)
    }

    /** The text inside this range. */
    fun textInside(text: String): String {
        assert(isNormalized)
        return text.substring(start, end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TextRange) {
            return false
        }
        return other.start == start && other.end == end
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }

    override fun toString(): String {
        return "TextRange(start: $start, end: $end)"
    }
}