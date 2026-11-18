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

package androidx.compose.markuptext.parser.block

import androidx.annotation.IntRange
import androidx.compose.markuptext.parser.LineScanner

/**
 * Single item container within a [ListParseBlock].
 *
 * A list item starts from its marker alone, without knowing whether it continues a list: a list of
 * the same type adopts it, and any other container wraps it in a new list ([prepareForAttach]).
 */
internal class ListItemParseBlock(
    id: Long = -1L,
    /** The marker this item started from, fixed when the item opened. */
    val marker: ListMarker,
) : BaseContainerParseBlock(id) {

    override val supportsLazyContinuation: Boolean = true

    /**
     * Whether this item is empty: it had no content on its marker line, which then opened an
     * [EmptyLineParseBlock] (holding the marker line plus any blank lines after it), and no content
     * followed. The marker line is not a blank line for the item's parent.
     */
    private val isEmptyItem: Boolean
        get() = children.singleOrNull() is EmptyLineParseBlock

    /** Whether this item is empty and at least one blank line followed its marker line. */
    private val isEmptyItemFollowedByBlankLine: Boolean
        get() = isEmptyItem && children[0].lineCount >= 2

    override fun matchLineStart(context: ParseContext): Boolean {
        val scanner = context.currentLine
        // An empty list item followed by a blank line cannot accept subsequent non-blank lines.
        if (!scanner.isEmptyLine() && isEmptyItemFollowedByBlankLine) {
            return false
        }
        return scanner.matchesIndent(marker.contentIndentColumn)
    }

    /**
     * Returns this item for a [ListParseBlock] container, which accepts only items of its own type,
     * and otherwise a new [ListParseBlock] of this item's type holding this item, which [container]
     * attaches instead.
     */
    override fun prepareForAttach(
        container: ContainerParseBlock,
        context: ParseContext,
    ): ParseBlock {
        if (container is ListParseBlock) return this
        return ListParseBlock(id = context.generateBlockId(), marker = marker).also {
            it.initFirstChild(this)
        }
    }

    /**
     * Counts the blank lines at the end of this item, which are those of its last child: a blank
     * line matches the item's indentation, so it stays blank for the item's parent. For an empty
     * item ([isEmptyItem]), the first line of its only child is the marker line, which is not
     * blank.
     */
    override fun countTrailingEmptyLines(): Int {
        val count = children.lastOrNull()?.countTrailingEmptyLines() ?: 0
        return if (isEmptyItem) count - 1 else count
    }

    override val consumesTrailingEmptyLines: Boolean
        get() = children.lastOrNull()?.consumesTrailingEmptyLines ?: false

    /**
     * Returns `true` if blank lines separate any two non-empty direct children of this item: either
     * an [EmptyLineParseBlock] between them, or blank lines at the end of the first one (e.g. an
     * indented code block followed by a paragraph). Blank lines before the first non-empty child
     * (the marker line of an item like `-`) or after the last one do not count.
     */
    internal fun hasBlankLineBetweenChildren(): Boolean {
        // The previous non-empty child, whose trailing blank lines count only if another non-empty
        // child follows; computed lazily, because it walks the child's last-child chain.
        var previousNonEmpty: ParseBlock? = null
        var blankAfterPrevious = false
        for (index in children.indices) {
            val child = children[index]
            if (child is EmptyLineParseBlock) {
                blankAfterPrevious = previousNonEmpty != null
                continue
            }
            val previous = previousNonEmpty
            if (
                previous != null && (blankAfterPrevious || previous.separatingEmptyLineCount() > 0)
            ) {
                return true
            }
            previousNonEmpty = child
            blankAfterPrevious = false
        }
        return false
    }
}

/**
 * Checks whether the current line has at least [requiredSpaces] leading space columns (or is an
 * empty line) and consumes up to [requiredSpaces] columns.
 */
internal fun LineScanner.matchesIndent(@IntRange(from = 0) requiredSpaces: Int): Boolean {
    if (leadingSpaceCount < requiredSpaces && !isEmptyLine()) return false
    consume(requiredSpaces)
    return true
}

/**
 * A list item marker as matched on its line ([matchesListMarker]): immutable, fixed when its item
 * opened. A [ListItemParseBlock] keeps its own marker, and a [ListParseBlock] keeps the marker of
 * its first item. Compare list types with [isSameListType], never with `==`, which compares every
 * field.
 */
internal data class ListMarker(
    /**
     * The character that decides the list type: the bullet (`-`, `+`, `*`) of a bullet list, or the
     * delimiter (`.`, `)`) after the number of an ordered list. Two items belong to the same list
     * only if this character is the same.
     */
    val bulletOrDelimiter: Char,
    @IntRange(from = 0) val startNumber: Int,
    /**
     * Number of columns from where the item's line starts (after the prefixes of enclosing
     * containers) to the item's content: leading indentation + marker width + padding. For an empty
     * item or one starting with indented code, leading indentation + marker width + 1.
     */
    @IntRange(from = 0) val contentIndentColumn: Int,
    val isEmptyItem: Boolean,
) {
    /** Whether this marker is an ordered list marker. */
    val isOrdered: Boolean
        get() = bulletOrDelimiter.isOrderedListDelimiter()

    /** Whether an item with [other] belongs to the same list as an item with this marker. */
    fun isSameListType(other: ListMarker): Boolean = bulletOrDelimiter == other.bulletOrDelimiter

    /**
     * Whether a list starting with this marker can interrupt a paragraph: the item must not be
     * empty and, if ordered, must start at 1.
     */
    val canInterruptParagraph: Boolean
        get() = !isEmptyItem && (!isOrdered || startNumber == 1)
}

/**
 * Checks whether the current line starts with a bullet list marker (`-`, `*`, `+`) or an ordered
 * list marker (up to [MAX_ORDERED_LIST_MARKER_DIGITS] digits followed by `.` or `)`), preceded by
 * up to [MAX_BLOCK_START_INDENT] leading spaces and followed by a space or the end of the line. If
 * so, consumes the marker and its padding (see [consumeListMarkerPadding]); otherwise consumes
 * nothing and returns `null`.
 */
internal fun LineScanner.matchesListMarker(): ListMarker? {
    val firstChar = firstNonSpaceChar
    if (
        leadingSpaceCount > MAX_BLOCK_START_INDENT ||
            (!firstChar.isBulletListMarker() && !firstChar.isAsciiDigit())
    ) {
        return null
    }
    val lineStartPosition = position
    val lineStartColumn = column
    consume(leadingSpaceCount)

    val firstMarkerChar = nextChar()
    val bulletOrDelimiter: Char
    var startNumber = 1
    if (firstMarkerChar.isBulletListMarker()) {
        bulletOrDelimiter = firstMarkerChar
    } else {
        startNumber = firstMarkerChar - '0'
        var digitCount = 1
        while (!isAtEnd && digitCount < MAX_ORDERED_LIST_MARKER_DIGITS && peek().isAsciiDigit()) {
            startNumber = startNumber * 10 + (nextChar() - '0')
            digitCount++
        }
        bulletOrDelimiter = nextChar()
        if (!bulletOrDelimiter.isOrderedListDelimiter()) {
            restore(lineStartPosition)
            return null
        }
    }
    if (!isAtEnd && peek() != ' ') {
        restore(lineStartPosition)
        return null
    }

    val isEmptyItem = isEmptyLine()
    return ListMarker(
        bulletOrDelimiter = bulletOrDelimiter,
        startNumber = startNumber,
        contentIndentColumn = consumeListMarkerPadding(lineStartColumn, isEmptyItem),
        isEmptyItem = isEmptyItem,
    )
}

/**
 * Consumes the padding after a list marker and returns the item's [ListMarker.contentIndentColumn],
 * counted from [lineStartColumn] (where the item's line starts after the prefixes of enclosing
 * containers):
 * - With 1 to [MAX_LIST_MARKER_PADDING] spaces after the marker, all of them are consumed, and the
 *   content starts at the first non-space character.
 * - With more spaces (the item starts with indented code), only one column is consumed, and the
 *   content starts one column after the marker.
 * - For an empty item ([isEmptyItem]), nothing is consumed, and the content starts one column after
 *   the marker.
 */
private fun LineScanner.consumeListMarkerPadding(lineStartColumn: Int, isEmptyItem: Boolean): Int {
    val markerEndColumn = column
    if (isEmptyItem) {
        return markerEndColumn - lineStartColumn + 1
    }
    val markerEndPosition = position
    if (matchSpaces() > MAX_LIST_MARKER_PADDING) {
        restore(markerEndPosition)
        nextChar()
        return markerEndColumn - lineStartColumn + 1
    }
    return column - lineStartColumn
}

/** Maximum number of digits in an ordered list marker. */
private const val MAX_ORDERED_LIST_MARKER_DIGITS = 9

/**
 * Maximum number of spaces after a list marker that are padding; with more, the item starts with
 * indented code.
 */
private const val MAX_LIST_MARKER_PADDING = 4

/** Whether this character is a bullet list marker: `-`, `*`, or `+`. */
internal fun Char.isBulletListMarker(): Boolean =
    when (this) {
        '-',
        '*',
        '+' -> true
        else -> false
    }

/** Whether this character ends the number of an ordered list marker: `.` or `)`. */
internal fun Char.isOrderedListDelimiter(): Boolean =
    when (this) {
        '.',
        ')' -> true
        else -> false
    }
