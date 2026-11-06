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
 * Indented (4-space) code block.
 *
 * @param id Unique identifier for this block.
 * @param initialCode Initial line of code (including trailing `'\n'`).
 * @param lineCount Initial line count of this block (defaults to 1).
 */
internal class IndentedCodeBlockParseBlock(
    id: Long,
    initialCode: String = "",
    @IntRange(from = 1) lineCount: Int = 1,
) : LeafParseBlock(id, lineCount) {

    private val content: StringBuilder = StringBuilder(initialCode)

    /**
     * Length of [content] immediately after the last non-empty code line was appended. Blank lines
     * are appended to [content] in case another indented code line follows. If none follows, they
     * stay in this block's lines (reported by [countTrailingEmptyLines]) but are not code, so
     * [getCodeContent] ends at [lastNonEmptyLength].
     */
    private var lastNonEmptyLength: Int = content.length

    /**
     * Returns the parsed code content (with trailing blank lines and the single final trailing
     * newline removed so `BasicText` does not render an extra trailing empty line).
     */
    internal fun getCodeContent(): String {
        val len = lastNonEmptyLength
        return if (len > 0 && content[len - 1] == '\n') {
            content.substring(0, len - 1)
        } else {
            content.substring(0, len)
        }
    }

    override fun parseLine(context: ParseContext): ParseResult {
        if (context.isLazyContinuation) {
            return ParseResult.reject()
        }

        val scanner = context.currentLine
        if (scanner.isEmptyLine()) {
            val spacesToStrip = minOf(scanner.leadingSpaceCount, INDENTED_CODE_SPACES)
            scanner.consume(spacesToStrip)
            content.append(scanner.getRawUnconsumedText()).append('\n')
            return ParseResult.accept()
        }
        if (scanner.matchesIndentedCodeBlock()) {
            content.append(scanner.getRawUnconsumedText()).append('\n')
            lastNonEmptyLength = content.length
            return ParseResult.accept()
        }
        return ParseResult.reject()
    }

    companion object {
        internal const val INDENTED_CODE_SPACES = 4
    }
}

/**
 * Checks whether the current line matches an indented code block line: at least 4 leading spaces on
 * a non-empty line. If matched, consumes the 4 leading spaces and returns `true`.
 */
internal fun LineScanner.matchesIndentedCodeBlock(): Boolean {
    if (!isEmptyLine() && leadingSpaceCount >= IndentedCodeBlockParseBlock.INDENTED_CODE_SPACES) {
        consume(IndentedCodeBlockParseBlock.INDENTED_CODE_SPACES)
        return true
    }
    return false
}
