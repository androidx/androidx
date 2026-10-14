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

internal data class AtxHeadingInfo(@IntRange(from = 1, to = 6) val level: Int, val text: String)

/**
 * Paragraph or ATX / Setext Heading block.
 *
 * Paragraphs consist of one or more lines of text. A paragraph can be converted into a Setext
 * heading by a subsequent underline line (`===` for H1 or `---` for H2). ATX headings (`#` through
 * `######`) are also represented as [ParagraphParseBlock] instances with [headingLevel] set to
 * 1..6, but unlike paragraphs, ATX headings cannot take continuation lines.
 *
 * @param id Unique identifier for this block.
 * @param headingLevel 0 for body text paragraphs, 1..6 for H1..H6 headings.
 * @param initialContent The initial text content of this block.
 * @param lineCount The initial line count of this block (defaults to 1).
 */
internal class ParagraphParseBlock(
    id: Long,
    @IntRange(from = 0, to = 6) var headingLevel: Int = 0,
    initialContent: String = "",
    @IntRange(from = 1) lineCount: Int = 1,
) : LeafParseBlock(id, lineCount) {
    val content: StringBuilder = StringBuilder(initialContent)

    override fun parseLine(context: ParseContext): ParseResult {
        // Headings cannot take continuation lines; close this block and propose starting a new
        // block.
        if (headingLevel > 0) {
            return ParseResult.rejectAndPropose(context.startBlock())
        }

        val scanner = context.currentLine

        // Setext underline (`===` or `---`) converts the paragraph into a heading.
        // This MUST be checked before `tryStartInterruptingBlock()` because a contiguous
        // `---` line is syntactically valid as both a Setext H2 underline and a Thematic Break
        // (horizontal rule). When immediately following a paragraph (and not on a lazy
        // continuation), Setext heading takes precedence over Thematic Break.
        // In contrast, `***`, `___`, or spaced `- - -` do not match `matchesSetextUnderline()`
        // and fall through to `tryStartInterruptingBlock()` to interrupt the paragraph.
        // Similarly, on lazy continuation lines (`isLazyContinuation == true`), `---` cannot
        // form a Setext heading and falls through to `tryStartInterruptingBlock()`.
        if (!context.isLazyContinuation) {
            val setextLevel = scanner.matchesSetextUnderline()
            if (setextLevel > 0) {
                this.headingLevel = setextLevel
                return ParseResult.accept()
            }
        }

        // Check if line starts an interrupting block (e.g. ATX Heading, BlockQuote, List)
        val interruptingBlock = context.tryStartInterruptingBlock()
        if (interruptingBlock != null) {
            return ParseResult.rejectAndPropose(interruptingBlock)
        }

        // Regular paragraph continuation line
        if (content.isNotEmpty()) {
            content.append('\n')
        }
        content.append(scanner.getRawUnconsumedText().trimStart())
        return ParseResult.accept()
    }
}

/**
 * Checks whether the current line matches a Setext heading underline (`===` or `---`).
 *
 * A Setext heading underline consists of up to 3 leading spaces, followed by a sequence of `=` or
 * `-` characters, and optional trailing whitespace.
 *
 * @return 1 for H1 (`=`), 2 for H2 (`-`), or 0 if the line does not match.
 */
@IntRange(from = 0, to = 2)
internal fun LineScanner.matchesSetextUnderline(): Int {
    val marker = firstNonSpaceChar
    if (leadingSpaceCount > 3 || (marker != '=' && marker != '-')) return 0
    val startPos = position
    consume(leadingSpaceCount)
    matchMultiple(marker)
    if (isEmptyLine()) {
        return if (marker == '=') 1 else 2
    }
    restore(startPos)
    return 0
}

/**
 * Checks whether the current line starts an ATX heading (`#` through `######`).
 *
 * An ATX heading consists of up to 3 leading spaces, 1..6 `#` characters followed by at least one
 * space/tab or the end of the line, followed by optional heading content and optional closing `#`
 * characters.
 *
 * @return [AtxHeadingInfo] with the heading level (1..6) and stripped text, or null if not a match.
 */
internal fun LineScanner.matchesAtxHeading(): AtxHeadingInfo? {
    if (leadingSpaceCount > 3 || firstNonSpaceChar != '#') return null
    val startPos = position
    consume(leadingSpaceCount)
    val level = matchMultiple('#')
    if (level <= 6 && (isAtEnd || peek() == ' ')) {
        matchSpaces()
        val rawText = getRawUnconsumedText().toString()
        val headingText = stripClosingAtxHashes(rawText)
        return AtxHeadingInfo(level, headingText)
    }
    restore(startPos)
    return null
}

/**
 * Strips optional closing `#` characters from an ATX heading.
 *
 * The closing sequence of `#` characters must be preceded by at least one space or tab.
 */
private fun stripClosingAtxHashes(text: String): String {
    val trimmed = text.trimEnd()
    var index = trimmed.length - 1
    while (index >= 0 && trimmed[index] == '#') {
        index--
    }
    if (index < trimmed.length - 1) {
        if (index < 0) {
            return ""
        }
        if (trimmed[index] == ' ' || trimmed[index] == '\t') {
            return trimmed.substring(0, index).trimEnd()
        }
    }
    return trimmed
}
