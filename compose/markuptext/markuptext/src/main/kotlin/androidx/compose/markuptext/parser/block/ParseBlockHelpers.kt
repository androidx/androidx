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

/**
 * Maximum number of leading spaces before a block marker: the `#` of an ATX heading, a code fence
 * (opening or closing), a divider, a setext underline, a `>` block quote marker, or a list marker.
 *
 * With 4 or more leading spaces, the line is not a block start: it starts an indented code block,
 * or continues the open paragraph (an indented line cannot interrupt a paragraph).
 */
internal const val MAX_BLOCK_START_INDENT = 3

/** Whether this character is a space or a tab, the only whitespace inside a Markdown line. */
internal fun Char.isSpaceOrTab(): Boolean = this == ' ' || this == '\t'

/**
 * Whether this character is an ASCII digit (`0..9`). Unlike [Char.isDigit], this rejects other
 * Unicode digits, which never form ordered list markers.
 */
internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/**
 * Whether this character is an ASCII punctuation character, the characters that a backslash can
 * escape.
 *
 * In the ASCII table, the printable characters from `!` (0x21) to `~` (0x7E) are punctuation,
 * except for three runs of letters and digits. So the punctuation forms the four ranges between
 * those runs:
 * - `!` to `/` (0x21..0x2F), after the space (0x20) and before the digits `0..9`: `!"#$%&'()*+,-./`
 * - `:` to `@` (0x3A..0x40), between the digits and the uppercase letters `A..Z`: `:;<=>?@`
 * - `[` to the backtick (0x5B..0x60), between the uppercase and the lowercase letters `a..z`:
 *   `[\]^_` and the backtick
 * - `{` to `~` (0x7B..0x7E), after the lowercase letters and before DEL (0x7F): `{|}~`
 */
internal fun Char.isAsciiPunctuation(): Boolean =
    this in '!'..'/' || this in ':'..'@' || this in '['..'`' || this in '{'..'~'
