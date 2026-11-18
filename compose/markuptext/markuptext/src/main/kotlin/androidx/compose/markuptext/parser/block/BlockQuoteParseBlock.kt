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

import androidx.compose.markuptext.parser.LineScanner

/** Container block holding child blocks within a block quote (`> quoted`). */
internal class BlockQuoteParseBlock(id: Long = -1L) : BaseContainerParseBlock(id) {

    override val supportsLazyContinuation: Boolean = true

    override fun matchLineStart(context: ParseContext): Boolean {
        return context.currentLine.matchesBlockQuotePrefix()
    }
}

/**
 * Checks whether the current line starts with a block quote marker (`>`), preceded by up to 3
 * leading spaces, and consumes the marker along with at most one optional following space column.
 */
internal fun LineScanner.matchesBlockQuotePrefix(): Boolean {
    if (leadingSpaceCount > MAX_BLOCK_START_INDENT || firstNonSpaceChar != BLOCK_QUOTE_MARKER) {
        return false
    }
    consume(leadingSpaceCount + 1)
    if (peek() == ' ') {
        nextChar() // consume single optional space column after '>'
    }
    return true
}

/** The character that starts a block quote line (`> quoted`). */
internal const val BLOCK_QUOTE_MARKER = '>'
