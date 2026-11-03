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
 * Thematic break / horizontal divider block (`---`, `***`, `___`).
 *
 * A thematic break is a single-line leaf block that never accepts continuation lines.
 */
internal class DividerParseBlock(id: Long, @IntRange(from = 1) lineCount: Int = 1) :
    LeafParseBlock(id, lineCount) {
    override fun parseLine(context: ParseContext): ParseResult {
        return ParseResult.reject()
    }
}

/**
 * Checks whether the current line matches a thematic break (`---`, `***`, or `___`).
 *
 * A thematic break consists of 0..3 leading spaces followed by at least 3 matching `-`, `_`, or `*`
 * characters, with only optional spaces or tabs between and after them.
 */
internal fun LineScanner.matchesDivider(): Boolean {
    val marker = firstNonSpaceChar
    if (leadingSpaceCount > MAX_BLOCK_START_INDENT || !marker.isDividerMarker()) {
        return false
    }
    var count = 0
    for (idx in firstNonSpaceIndex until endExclusive) {
        val c = text[idx]
        if (c == marker) {
            count++
        } else if (!c.isSpaceOrTab()) {
            return false
        }
    }
    return count >= 3
}

/** Whether this character can form a divider (thematic break): `-`, `_`, or `*`. */
internal fun Char.isDividerMarker(): Boolean =
    when (this) {
        '-',
        '_',
        '*' -> true
        else -> false
    }
