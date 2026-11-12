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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InlineScannerTest {

    @Test
    fun peekAndNext_readCharactersInOrder() {
        val scanner = InlineScanner("ab")
        assertTrue(scanner.hasNext())
        assertFalse(scanner.isAtEnd)
        assertEquals('a', scanner.peek())
        assertEquals('b', scanner.peek(1))
        assertEquals('a', scanner.next())
        assertEquals('b', scanner.next())
        assertFalse(scanner.hasNext())
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun endOfText_returnsEnd() {
        val scanner = InlineScanner("a")
        assertEquals(InlineScanner.END, scanner.peek(1))
        assertEquals(InlineScanner.END, scanner.peek(-1))
        scanner.next()
        assertEquals(InlineScanner.END, scanner.peek())
        assertEquals(InlineScanner.END, scanner.next())
        assertEquals(1, scanner.index)
    }

    @Test
    fun slice_staysWithinBounds() {
        val scanner = InlineScanner("xabcx", startIndex = 1, endExclusive = 4)
        assertEquals(1, scanner.index)
        assertEquals(InlineScanner.END, scanner.peek(-1))
        assertEquals(0, scanner.peekPreviousCodePoint())
        scanner.advance(10)
        assertEquals(4, scanner.index)
        assertEquals(InlineScanner.END, scanner.peek())
    }

    @Test
    fun advance_clampsLargeCountsWithoutOverflow() {
        val scanner = InlineScanner("a")
        scanner.advance(1)
        scanner.advance(Int.MAX_VALUE)
        assertEquals(1, scanner.index)
    }

    @Test
    fun advance_rejectsNegativeCount() {
        val scanner = InlineScanner("abc")
        scanner.advance(2)
        assertFailsWith<IllegalArgumentException> { scanner.advance(-1) }
        assertEquals(2, scanner.index)
    }

    @Test
    fun nextExpected_advancesOnlyOnMatch() {
        val scanner = InlineScanner("<!--x")
        assertFalse(scanner.next('!'))
        assertTrue(scanner.next('<'))
        assertFalse(scanner.next("-->"))
        assertTrue(scanner.next("!--"))
        assertEquals(4, scanner.index)
        assertFalse(scanner.next("xy"))
        assertEquals(4, scanner.index)
    }

    @Test
    fun matchMultiple_countsARun() {
        val scanner = InlineScanner("***a")
        assertEquals(3, scanner.matchMultiple('*'))
        assertEquals('a', scanner.peek())
        assertEquals(0, scanner.matchMultiple('*'))
    }

    @Test
    fun find_stopsAtTargetOrEnd() {
        val scanner = InlineScanner("ab`c")
        assertEquals(2, scanner.find('`'))
        assertEquals('`', scanner.peek())
        scanner.next()
        assertEquals(-1, scanner.find('`'))
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun backwardTo_returnsToASavedIndex() {
        val scanner = InlineScanner("abc")
        val saved = scanner.index
        scanner.advance(2)
        scanner.backwardTo(saved)
        assertEquals(0, scanner.index)
        scanner.backwardTo(0) // Staying in place is allowed.
        assertEquals(0, scanner.index)
    }

    @Test
    fun backwardTo_rejectsMovingForward() {
        val scanner = InlineScanner("abc")
        assertFailsWith<IllegalArgumentException> { scanner.backwardTo(2) }
    }

    @Test
    fun forwardTo_movesToAMeasuredEnd() {
        val scanner = InlineScanner("<a@b>x")
        scanner.forwardTo(5)
        assertEquals('x', scanner.peek())
        scanner.forwardTo(6) // The end of the text is allowed.
        assertFalse(scanner.hasNext())
    }

    @Test
    fun forwardTo_rejectsMovingBackwardOrPastTheEnd() {
        val scanner = InlineScanner("abc")
        scanner.advance(2)
        assertFailsWith<IllegalArgumentException> { scanner.forwardTo(1) }
        assertFailsWith<IllegalArgumentException> { scanner.forwardTo(4) }
    }

    @Test
    fun codePoints_combineSurrogatePairs() {
        // U+1F389 (party popper) is a surrogate pair in UTF-16.
        val text = "*\uD83C\uDF89*"
        val scanner = InlineScanner(text)
        scanner.next()
        assertEquals(0x1F389, scanner.peekCodePoint())
        scanner.advance(2)
        assertEquals(0x1F389, scanner.peekPreviousCodePoint())
    }

    @Test
    fun codePoints_atTheEdges() {
        val scanner = InlineScanner("ab")
        assertEquals(0, scanner.peekPreviousCodePoint())
        assertEquals('a'.code, scanner.peekCodePoint())
        scanner.advance(2)
        assertEquals(0, scanner.peekCodePoint())
        assertEquals('b'.code, scanner.peekPreviousCodePoint())
    }

    @Test
    fun codePoints_loneSurrogateIsReturnedAsIs() {
        val scanner = InlineScanner("\uD83Cx")
        assertEquals(0xD83C, scanner.peekCodePoint())
    }

    @Test
    fun codePoints_doNotPairAcrossSliceEdges() {
        // U+1F389 (party popper) is outside the BMP, so UTF-16 stores it as two chars, a surrogate
        // pair: a high surrogate (U+D800..U+DBFF) followed by a low surrogate (U+DC00..U+DFFF).
        // For U+1F389: subtract 0x10000 to get 0xF389, a 20-bit value. Its top 10 bits (0x3C) are
        // added to 0xD800 for the high surrogate '\uD83C', and its low 10 bits (0x389) are added
        // to 0xDC00 for the low surrogate '\uDF89'. Only a high followed by a low forms a pair, so
        // "\uD83C\uDF89" is U+1F389 while "\uDF89\uD83C" is two lone surrogates.

        // Text: [0] low '\uDF89', [1] high '\uD83C', [2] low '\uDF89'. The slice [1, 2) holds only
        // the high surrogate. Its low half at index 2 is past endExclusive, so the high surrogate
        // is read alone instead of being combined into U+1F389.
        val scanner = InlineScanner("\uDF89\uD83C\uDF89", startIndex = 1, endExclusive = 2)
        assertEquals(0xD83C, scanner.peekCodePoint())
        // At index 2 the previous char is the high surrogate; it is not a low surrogate, so it is
        // returned alone (the low surrogate at index 0 is before startIndex and is never read).
        scanner.advance(1)
        assertEquals(0xD83C, scanner.peekPreviousCodePoint())

        // Text: [0] high '\uD83C', [1] low '\uDF89', [2] 'a'. The slice starts at 1, so the pair's
        // high half is before startIndex: reading back from index 2 returns the low surrogate
        // alone instead of combining it into U+1F389.
        val lowAtStart = InlineScanner("\uD83C\uDF89a", startIndex = 1)
        lowAtStart.advance(1)
        assertEquals(0xDF89, lowAtStart.peekPreviousCodePoint())
    }

    @Test
    fun codePoints_reversedSurrogatesAreNotPaired() {
        // Low then high is not a surrogate pair: each half is a lone surrogate.
        val scanner = InlineScanner("\uDF89\uD83C")
        // A low surrogate at the cursor is not combined with the high surrogate after it.
        assertEquals(0xDF89, scanner.peekCodePoint())
        scanner.advance(2)
        // A high surrogate before the cursor is not combined with the low surrogate before it.
        assertEquals(0xD83C, scanner.peekPreviousCodePoint())
    }

    @Test
    fun peekPreviousCodePoint_loneLowSurrogateIsReturnedAsIs() {
        val scanner = InlineScanner("x\uDF89")
        scanner.advance(2)
        assertEquals(0xDF89, scanner.peekPreviousCodePoint())
    }

    @Test
    fun nextExpected_atEndReturnsFalse() {
        val scanner = InlineScanner("a")
        scanner.next()
        assertFalse(scanner.next('a'))
        assertFalse(scanner.next("a"))
        assertTrue(scanner.next(""))
        assertEquals(1, scanner.index)
    }

    @Test
    fun matchMultiple_runReachesTheEnd() {
        val scanner = InlineScanner("a**", startIndex = 1)
        assertEquals(2, scanner.matchMultiple('*'))
        assertTrue(scanner.isAtEnd)
        assertEquals(0, scanner.matchMultiple('*'))
    }

    @Test
    fun backwardTo_rejectsTargetBeforeStartIndex() {
        val scanner = InlineScanner("abc", startIndex = 1)
        assertFailsWith<IllegalArgumentException> { scanner.backwardTo(0) }
    }

    /**
     * For every slice of a text with surrogate pairs, reversed and lone surrogates and grapheme
     * clusters, walks the slice one code point at a time and checks [InlineScanner.peekCodePoint]
     * against [Character.codePointAt] and [InlineScanner.peekPreviousCodePoint] against
     * [Character.codePointBefore], both limited to the slice.
     */
    @Test
    fun codePoints_matchCharacterOnEverySlice() {
        // U+1F389 (party popper): one code point, stored as a high + low surrogate pair.
        val partyPopper = "\uD83C\uDF89"
        // The same halves reversed: low then high is not a pair, so these are two lone surrogates.
        val reversedHalves = "\uDF89\uD83C"
        // Grapheme clusters: each is one user-perceived character but several code points.
        // U+1F469 (woman) + U+200D (zero width joiner) + U+1F4BB (laptop) renders as one
        // "woman technologist" emoji, and 'e' + U+0301 (combining acute accent) renders as "é".
        // InlineScanner does not handle grapheme clusters: it reads them one code point at a time,
        // like Character.codePointAt. That is what the delimiter flanking rules need, since they
        // classify the single code point next to a delimiter run.
        val womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB"
        val eWithAcute = "e\u0301"
        // A high surrogate with nothing after it.
        val loneHigh = "\uD83C"
        val text =
            "a" + partyPopper + reversedHalves + womanTechnologist + eWithAcute + "*" + loneHigh
        val chars = text.toCharArray()
        for (start in 0..chars.size) {
            for (end in start..chars.size) {
                val scanner = InlineScanner(text, start, end)
                assertEquals(0, scanner.peekPreviousCodePoint())
                while (scanner.hasNext()) {
                    val context = "slice [$start, $end) at ${scanner.index}"
                    val expected = Character.codePointAt(chars, scanner.index, end)
                    assertEquals(expected, scanner.peekCodePoint(), context)
                    // Moves past both chars of a surrogate pair, or one char otherwise.
                    scanner.advance(Character.charCount(expected))
                    assertEquals(
                        Character.codePointBefore(chars, scanner.index, start),
                        scanner.peekPreviousCodePoint(),
                        context,
                    )
                }
                assertEquals(0, scanner.peekCodePoint())
            }
        }
    }
}
