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

package androidx.compose.markuptext.parser

/**
 * Low-level line scanner for Markdown block-structure parsing and structural tab resolution.
 *
 * In Markdown block parsing, tab characters are not expanded to spaces in general content; however,
 * when whitespace helps define block structure (such as indentation, container prefixes like `>`,
 * list markers, and code block indents), tabs behave as if they were replaced by spaces with a tab
 * stop of 4 columns.
 *
 * This scanner virtualizes tabs into 4-column tab stops during block-level prefix and indentation
 * consumption:
 * - If a tab character is partially consumed while matching block structure, its remaining virtual
 *   columns are materialized as spaces by [getRawUnconsumedText].
 * - If a tab character has not been consumed (even if preceded by other characters or spaces),
 *   [getRawUnconsumedText] preserves the literal `'\t'` character untouched for inline or code
 *   block content.
 *
 * **Contract:** Line scanners operate on a single line and assume no newline characters (`\r`,
 * `\n`) are included in the scanned range by default. If a newline character is present in the
 * input, it will NOT be treated as a line delimiter or whitespace; it will be treated as an
 * ordinary character.
 *
 * Supports zero-allocation state save and restore via packed 64-bit Long primitives ([Position]).
 * Contains no block-specific grammar.
 *
 * @param text The source character sequence.
 * @param start The inclusive start offset in [text].
 * @param endExclusive The exclusive end offset in [text].
 */
internal class LineScanner(
    val text: CharSequence,
    val start: Int = 0,
    val endExclusive: Int = text.length,
) {
    /** Constructs a [LineScanner] for a complete line [String]. */
    constructor(line: String) : this(line, 0, line.length)

    /**
     * Current character index in [text], bounded between [start] and [endExclusive]. When expanding
     * a tab character (`\t`), [index] remains at the tab character until [column] reaches the next
     * 4-space tab stop (`column % 4 == 0`).
     */
    var index: Int = start
        private set

    /**
     * Current 0-based virtual column position on the line, accounting for 4-space tab expansion.
     * Incremented by 1 for each character or virtual tab space consumed.
     */
    var column: Int = 0
        private set

    /**
     * True if the scanner has partially consumed virtual spaces from a tab character at
     * `text[index]` without yet reaching the 4-column tab stop. False when positioned at the start
     * of a character (including an untouched tab).
     */
    private var isInsideTab: Boolean = false

    private var cachedLeadingSpaceCount: Int = -1
    private var cachedFirstNonSpaceIndex: Int = -1

    private fun ensureLeadingSpacesComputed() {
        if (cachedLeadingSpaceCount != -1) return
        var spaces = 0
        var col = column
        var idx = index
        while (idx < endExclusive) {
            val char = text[idx]
            if (char == ' ') {
                spaces++
                col++
                idx++
            } else if (char == '\t') {
                val tabSpaces = 4 - (col % 4)
                spaces += tabSpaces
                col += tabSpaces
                idx++
            } else {
                break
            }
        }
        cachedLeadingSpaceCount = spaces
        cachedFirstNonSpaceIndex = idx
    }

    /**
     * Number of leading virtual spaces (including expanded tabs) starting from the current scanning
     * position up to [firstNonSpaceIndex]. Computed lazily and cached.
     */
    val leadingSpaceCount: Int
        get() {
            ensureLeadingSpacesComputed()
            return cachedLeadingSpaceCount
        }

    /**
     * Index in [text] of the first non-whitespace character starting from the current scanning
     * position, or [endExclusive] if the remainder of the line is empty. Computed lazily and
     * cached.
     */
    val firstNonSpaceIndex: Int
        get() {
            ensureLeadingSpacesComputed()
            return cachedFirstNonSpaceIndex
        }

    /**
     * The first non-whitespace character starting from the current scanning position, or [EOF]
     * (`'\u0000'`) if the remainder of the line is empty.
     */
    val firstNonSpaceChar: Char
        get() {
            val idx = firstNonSpaceIndex
            return if (idx >= endExclusive) EOF else text[idx]
        }

    /** True if the scanner has reached the end of the line ([index] >= [endExclusive]). */
    val isAtEnd: Boolean
        get() = index >= endExclusive

    /**
     * Checks if the remaining unconsumed part of the line contains only whitespace (' ' or '\t').
     */
    fun isEmptyLine(): Boolean {
        return firstNonSpaceIndex >= endExclusive
    }

    /**
     * Peeks at the next character without consuming it. Returns `' '` if currently within an
     * expanded tab, or [EOF] (`'\u0000'`) if at end of line.
     */
    fun peek(): Char {
        if (index >= endExclusive) {
            return EOF
        }
        val char = text[index]
        return if (char == '\t') ' ' else char
    }

    /**
     * Consumes and returns the next character. Expands tabs into `' '` characters until completed,
     * or returns [EOF] (`'\u0000'`) if at end of line.
     */
    fun nextChar(): Char {
        if (index >= endExclusive) {
            return EOF
        }

        val char = text[index]
        column++
        if (char == '\t') {
            if (column % 4 == 0) {
                index++
                isInsideTab = false
            } else {
                isInsideTab = true
            }
            if (cachedLeadingSpaceCount > 0) {
                cachedLeadingSpaceCount--
            } else {
                cachedLeadingSpaceCount = -1
                cachedFirstNonSpaceIndex = -1
            }
            return ' '
        }

        index++
        isInsideTab = false
        if (char == ' ' && cachedLeadingSpaceCount > 0) {
            cachedLeadingSpaceCount--
        } else {
            cachedLeadingSpaceCount = -1
            cachedFirstNonSpaceIndex = -1
        }
        return char
    }

    /** Consumes up to [count] consecutive characters. */
    fun consume(count: Int) {
        var remaining = count
        while (remaining > 0 && !isAtEnd) {
            nextChar()
            remaining--
        }
    }

    /** Consumes consecutive spaces (including expanded tabs) and returns the count consumed. */
    fun matchSpaces(): Int {
        var count = 0
        while (!isAtEnd && peek() == ' ') {
            nextChar()
            count++
        }
        return count
    }

    /** Consumes consecutive occurrences of [char] and returns the count. */
    fun matchMultiple(char: Char): Int {
        var count = 0
        while (!isAtEnd && peek() == char) {
            nextChar()
            count++
        }
        return count
    }

    /**
     * Returns the remaining unconsumed slice of raw text on this line.
     *
     * Unlike [peek] and [nextChar], which virtualize all tabs into spaces, this method preserves
     * unconsumed `'\t'` characters as literal tabs. Only if the scanner is currently mid-way
     * through a partially consumed tab ([isInsideTab] == true) are the remaining virtual columns of
     * that single tab materialized as `' '` characters before appending the rest of the raw text.
     */
    fun getRawUnconsumedText(): CharSequence {
        if (index >= endExclusive) return ""
        if (isInsideTab) {
            val remainingTabSpaces = 4 - (column % 4)
            val sb =
                StringBuilder(remainingTabSpaces + (endExclusive - (index + 1)).coerceAtLeast(0))
            repeat(remainingTabSpaces) { sb.append(' ') }
            if (index + 1 < endExclusive) {
                sb.append(text, index + 1, endExclusive)
            }
            return sb
        }
        return text.subSequence(index, endExclusive)
    }

    /**
     * Zero-allocation value class representing a snapshot of the scanner's position in the line
     * ([index], [column], and [isInsideTab]).
     */
    @kotlin.jvm.JvmInline value class Position internal constructor(internal val packed: Long)

    /** The current scanning position in the line. */
    val position: Position
        get() =
            Position(
                (index.toLong() shl 32) or
                    ((column.toLong() and 0x7FFF_FFFFL) shl 1) or
                    (if (isInsideTab) 1L else 0L)
            )

    /** Restores the scanner's position ([index], [column], and [isInsideTab]) to [pos]. */
    fun restore(pos: Position) {
        index = (pos.packed ushr 32).toInt()
        column = ((pos.packed ushr 1) and 0x7FFF_FFFFL).toInt()
        isInsideTab = (pos.packed and 1L) != 0L
        cachedLeadingSpaceCount = -1
        cachedFirstNonSpaceIndex = -1
    }

    companion object {
        /**
         * Sentinel character returned by [peek], [nextChar], and [firstNonSpaceChar] at end of
         * line.
         */
        const val EOF: Char = '\u0000'
    }
}
