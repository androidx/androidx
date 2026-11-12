/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.markuptext.parser.inline

/**
 * Pure character and position cursor over a [CharSequence] slice `[startIndex, endExclusive)`.
 *
 * [InlineScanner] only provides character inspection and cursor movement; to backtrack, save
 * [index] and [backwardTo] to it. It contains no Markdown syntax rules; syntax is handled by the
 * inline parser and its matchers.
 *
 * [InlineScanner] assumes [text] contains no U+0000 (callers replace it with U+FFFD beforehand). It
 * returns U+0000 ([END], or `0` for code points) when reading outside the slice.
 */
internal class InlineScanner(
    val text: CharSequence,
    val startIndex: Int = 0,
    val endExclusive: Int = text.length,
) {
    /**
     * Current character offset within [text], in `[startIndex, endExclusive]`. It only moves
     * through the reading methods, [advance], [backwardTo] and [forwardTo].
     */
    var index: Int = startIndex
        private set

    /**
     * Moves back to [savedIndex], an earlier value of [index], to retry from there (backtracking
     * after a failed match).
     */
    fun backwardTo(savedIndex: Int) {
        require(savedIndex in startIndex..index) { "Can only move backward to $startIndex..$index" }
        index = savedIndex
    }

    /**
     * Moves forward to [target], the end of a construct that a matcher measured directly on [text]
     * (such as an autolink's closing `>`).
     */
    fun forwardTo(target: Int) {
        require(target in index..endExclusive) { "Can only move forward to $index..$endExclusive" }
        index = target
    }

    /** Whether the cursor has reached [endExclusive]. */
    val isAtEnd: Boolean
        get() = index >= endExclusive

    /** Returns `true` if there are more characters to read before [endExclusive]. */
    fun hasNext(): Boolean = index < endExclusive

    /**
     * Returns the character at the current [index] without advancing, or [END] (`'\u0000'`) if at
     * or past [endExclusive].
     */
    fun peek(): Char {
        return if (index < endExclusive) text[index] else END
    }

    /**
     * Returns the character at `index + offset` without advancing, or [END] (`'\u0000'`) if out of
     * bounds.
     */
    fun peek(offset: Int): Char {
        val target = index + offset
        return if (target in startIndex until endExclusive) text[target] else END
    }

    /**
     * Returns the Unicode code point at the current [index] without advancing, or `0` ([END]) if at
     * [endExclusive].
     *
     * Used by the delimiter flanking rules, which classify the character next to a `*` / `_` run as
     * Unicode whitespace, punctuation or other. A `CharSequence` is UTF-16, so characters outside
     * the Basic Multilingual Plane (most emoji, some math and CJK characters) are two `Char`s (a
     * surrogate pair); reading a single `Char` would classify half a pair as "other" instead of,
     * for example, punctuation for an emoji.
     *
     * Reading one code point of a longer grapheme cluster (an emoji sequence joined by U+200D, a
     * letter with combining marks) is correct: the flanking rules are defined per code point, on
     * the code point right next to the delimiter run, not per grapheme cluster.
     */
    fun peekCodePoint(): Int {
        if (index >= endExclusive) return 0
        val char = text[index]
        if (char.isHighSurrogate() && index + 1 < endExclusive) {
            val low = text[index + 1]
            if (low.isLowSurrogate()) {
                return Character.toCodePoint(char, low)
            }
        }
        return char.code
    }

    /**
     * Returns the Unicode code point immediately preceding [index], or `0` ([END]) if at
     * [startIndex]. See [peekCodePoint] for why code points, and why a partial grapheme cluster is
     * fine.
     */
    fun peekPreviousCodePoint(): Int {
        if (index <= startIndex) return 0
        val previousIndex = index - 1
        val char = text[previousIndex]
        if (char.isLowSurrogate() && previousIndex > startIndex) {
            val high = text[previousIndex - 1]
            if (high.isHighSurrogate()) {
                return Character.toCodePoint(high, char)
            }
        }
        return char.code
    }

    /** Returns the character at [index] and advances by 1, or [END] if at [endExclusive]. */
    fun next(): Char {
        return if (index < endExclusive) text[index++] else END
    }

    /**
     * Checks if [expected] is the next character; if so, advances the position and returns `true`.
     */
    fun next(expected: Char): Boolean {
        if (index < endExclusive && text[index] == expected) {
            index++
            return true
        }
        return false
    }

    /**
     * Checks if [expected] matches at the current position; if so, advances the position by
     * `expected.length` and returns `true`.
     */
    fun next(expected: String): Boolean {
        if (index + expected.length > endExclusive) return false
        for (offset in expected.indices) {
            if (text[index + offset] != expected[offset]) return false
        }
        index += expected.length
        return true
    }

    /** Advances [index] by [count] (non-negative) characters, clamped to [endExclusive]. */
    fun advance(count: Int) {
        require(count >= 0) { "Count must be non-negative: $count" }
        index += count.coerceAtMost(endExclusive - index)
    }

    /**
     * Consumes all consecutive occurrences of [target] starting at [index] and returns the number
     * of characters consumed.
     */
    fun matchMultiple(target: Char): Int {
        val start = index
        while (index < endExclusive && text[index] == target) {
            index++
        }
        return index - start
    }

    /**
     * Advances the cursor until [target] is found or [endExclusive] is reached.
     *
     * @return The number of characters advanced before [target] was found (with the cursor
     *   positioned at [target]), or `-1` if [target] was not found (with the cursor at
     *   [endExclusive]).
     */
    fun find(target: Char): Int {
        var count = 0
        while (index < endExclusive) {
            if (text[index] == target) {
                return count
            }
            count++
            index++
        }
        return -1
    }

    companion object {
        /**
         * Returned when reading outside the slice. Never a real character, because [text] contains
         * no U+0000 (see [InlineScanner]).
         */
        const val END: Char = '\u0000'
    }
}
