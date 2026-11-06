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

internal data class FencedCodeBlockInfo(
    val fenceChar: Char,
    @IntRange(from = 3) val fenceLength: Int,
    @IntRange(from = 0, to = 3) val fenceIndent: Int,
    val language: String?,
)

/**
 * Fenced code block: lines between an opening fence of 3 or more backticks or tildes and a closing
 * fence of the same character.
 *
 * @param id Unique identifier for this block.
 * @param fenceChar The character the opening fence is made of: a backtick (&#96;) or a tilde (~).
 * @param fenceLength The number of opening fence characters (`>= 3`).
 * @param fenceIndent The leading space count (`0..3`) of the opening fence.
 * @param language Optional language identifier parsed from the fenced code block's info string.
 * @param lineCount Initial line count of this block (defaults to 1).
 */
internal class FencedCodeBlockParseBlock(
    id: Long,
    val fenceChar: Char,
    @IntRange(from = 3) val fenceLength: Int,
    @IntRange(from = 0, to = 3) val fenceIndent: Int,
    val language: String? = null,
    @IntRange(from = 1) lineCount: Int = 1,
) : LeafParseBlock(id, lineCount) {

    constructor(
        id: Long,
        fence: FencedCodeBlockInfo,
        @IntRange(from = 1) lineCount: Int = 1,
    ) : this(
        id = id,
        fenceChar = fence.fenceChar,
        fenceLength = fence.fenceLength,
        fenceIndent = fence.fenceIndent,
        language = fence.language,
        lineCount = lineCount,
    )

    private val content: StringBuilder = StringBuilder()

    /** True once this fenced code block has matched its closing fence line. */
    var isClosed: Boolean = false
        private set

    /**
     * Blank lines inside the fence are code. If the end of the container closes this block before a
     * closing fence, its trailing blank lines are still code, not a separator.
     */
    override val consumesTrailingEmptyLines: Boolean
        get() = true

    /**
     * Returns the parsed code content (with the single final trailing newline removed so
     * `BasicText` does not render an extra trailing empty line).
     */
    internal fun getCodeContent(): String {
        val len = content.length
        return if (len > 0 && content[len - 1] == '\n') {
            content.substring(0, len - 1)
        } else {
            content.toString()
        }
    }

    override fun parseLine(context: ParseContext): ParseResult {
        if (isClosed || context.isLazyContinuation) {
            return ParseResult.reject()
        }

        val scanner = context.currentLine
        if (matchesClosingFence(scanner)) {
            isClosed = true
            return ParseResult.accept()
        }

        // Strip up to fenceIndent leading spaces from content line
        val spacesToStrip = minOf(scanner.leadingSpaceCount, fenceIndent)
        scanner.consume(spacesToStrip)
        content.append(scanner.getRawUnconsumedText()).append('\n')
        return ParseResult.accept()
    }

    /**
     * Checks whether [scanner] matches a closing code fence: up to 3 leading spaces, at least
     * [fenceLength] occurrences of [fenceChar], followed only by optional spaces/tabs to end of
     * line.
     */
    private fun matchesClosingFence(scanner: LineScanner): Boolean {
        if (
            scanner.leadingSpaceCount > MAX_BLOCK_START_INDENT ||
                scanner.firstNonSpaceChar != fenceChar
        ) {
            return false
        }
        val startPos = scanner.position
        scanner.consume(scanner.leadingSpaceCount)
        val closingCount = scanner.matchMultiple(fenceChar)
        if (closingCount >= fenceLength && scanner.isEmptyLine()) {
            return true
        }
        scanner.restore(startPos)
        return false
    }
}

/**
 * Checks whether the current line starts a fenced code block (3 or more backticks or tildes).
 *
 * A fenced code block starts with 0..3 leading spaces, followed by at least 3 consecutive backticks
 * or tildes (`~`). If the fence uses backticks, the remainder of the line (the info string) must
 * not contain any backtick characters.
 */
internal fun LineScanner.matchesFencedCodeBlock(): FencedCodeBlockInfo? {
    val firstChar = firstNonSpaceChar
    if (leadingSpaceCount > MAX_BLOCK_START_INDENT || !firstChar.isFenceChar()) {
        return null
    }
    val startPos = position
    val indent = leadingSpaceCount
    consume(indent)
    val fenceLen = matchMultiple(firstChar)
    if (fenceLen < 3) {
        restore(startPos)
        return null
    }
    val rest = getRawUnconsumedText().toString()
    if (firstChar == '`' && rest.indexOf('`') != -1) {
        restore(startPos)
        return null
    }

    return FencedCodeBlockInfo(
        fenceChar = firstChar,
        fenceLength = fenceLen,
        fenceIndent = indent,
        language = parseInfoLanguage(rest),
    )
}

/**
 * Extracts the first whitespace-delimited word from the fenced code block's info string and
 * resolves backslash-escaped ASCII punctuation.
 */
private fun parseInfoLanguage(rawInfo: String): String? {
    val trimmed = rawInfo.trim { it.isSpaceOrTab() }
    if (trimmed.isEmpty()) return null
    val spaceIdx = trimmed.indexOfFirst { it.isSpaceOrTab() }
    val firstWord = if (spaceIdx == -1) trimmed else trimmed.substring(0, spaceIdx)
    if (firstWord.indexOf('\\') == -1) {
        return firstWord
    }
    val sb = StringBuilder(firstWord.length)
    var i = 0
    while (i < firstWord.length) {
        val c = firstWord[i]
        if (c == '\\' && i + 1 < firstWord.length && firstWord[i + 1].isAsciiPunctuation()) {
            sb.append(firstWord[i + 1])
            i += 2
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString().ifEmpty { null }
}

/** Whether this character can form a code fence: a backtick or a tilde (`~`). */
internal fun Char.isFenceChar(): Boolean =
    when (this) {
        '`',
        '~' -> true
        else -> false
    }
