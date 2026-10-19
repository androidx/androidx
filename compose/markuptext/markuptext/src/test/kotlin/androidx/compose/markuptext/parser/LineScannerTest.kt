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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineScannerTest {

    @Test
    fun testSimpleScanning() {
        val scanner = LineScanner("abc")
        assertEquals('a', scanner.peek())
        assertEquals('a', scanner.nextChar())
        assertEquals(1, scanner.column)
        assertEquals('b', scanner.nextChar())
        assertEquals(2, scanner.column)
        assertEquals('c', scanner.nextChar())
        assertEquals(3, scanner.column)
        assertEquals(LineScanner.EOF, scanner.peek())
        assertEquals(LineScanner.EOF, scanner.nextChar())
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun testTabExpansionAtColumn0() {
        val scanner = LineScanner("\tA")
        // Tab at column 0 expands to 4 spaces
        assertEquals(' ', scanner.peek())
        assertEquals(' ', scanner.nextChar())
        assertEquals(1, scanner.column)
        assertEquals(' ', scanner.nextChar())
        assertEquals(2, scanner.column)
        assertEquals(' ', scanner.nextChar())
        assertEquals(3, scanner.column)
        assertEquals(' ', scanner.nextChar())
        assertEquals(4, scanner.column)

        // Then 'A'
        assertEquals('A', scanner.peek())
        assertEquals('A', scanner.nextChar())
        assertEquals(5, scanner.column)
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun testTabExpansionWithLeadingSpaces() {
        val scanner = LineScanner("  \tB")
        // Two spaces
        assertEquals(' ', scanner.nextChar())
        assertEquals(1, scanner.column)
        assertEquals(' ', scanner.nextChar())
        assertEquals(2, scanner.column)

        // Tab at column 2 expands to 2 spaces (4 - 2 = 2)
        assertEquals(' ', scanner.nextChar())
        assertEquals(3, scanner.column)
        assertEquals(' ', scanner.nextChar())
        assertEquals(4, scanner.column)

        // Then 'B'
        assertEquals('B', scanner.nextChar())
        assertEquals(5, scanner.column)
    }

    @Test
    fun testMatchSpaces() {
        val scanner = LineScanner("  \t  text")
        // 2 spaces + 2 tab spaces + 2 spaces = 6 spaces
        val spaces = scanner.matchSpaces()
        assertEquals(6, spaces)
        assertEquals(6, scanner.column)
        assertEquals('t', scanner.peek())
    }

    @Test
    fun testMatchMultiple() {
        val scanner = LineScanner("### Heading")
        val hashes = scanner.matchMultiple('#')
        assertEquals(3, hashes)
        assertEquals(' ', scanner.peek())
    }

    @Test
    fun testSaveAndRestoreDuringTab() {
        val scanner = LineScanner("\tText")
        // Consume 2 of the 4 tab spaces
        scanner.nextChar() // col 1
        scanner.nextChar() // col 2
        assertEquals(2, scanner.column)

        val savedPos = scanner.position

        // Consume remaining
        scanner.nextChar() // col 3
        scanner.nextChar() // col 4
        assertEquals('T', scanner.nextChar()) // col 5

        // Restore
        scanner.restore(savedPos)
        assertEquals(2, scanner.column)
        assertEquals(' ', scanner.nextChar()) // col 3
        assertEquals(' ', scanner.nextChar()) // col 4
        assertEquals('T', scanner.nextChar()) // col 5
    }

    @Test
    fun testIsEmptyLine() {
        assertTrue(LineScanner("").isEmptyLine())
        assertTrue(LineScanner("    ").isEmptyLine())
        assertTrue(LineScanner(" \t \t ").isEmptyLine())
        assertFalse(LineScanner("  a  ").isEmptyLine())
    }

    @Test
    fun testNewlinesTreatedAsRegularCharacters() {
        // LineScanner does not treat '\n' as whitespace or line end; it is an ordinary char
        val scanner = LineScanner("\n")
        assertFalse(scanner.isEmptyLine(), "Newline should not be treated as empty line whitespace")
        assertEquals('\n', scanner.peek())
        assertEquals('\n', scanner.nextChar())
        assertEquals(1, scanner.column)
        assertTrue(scanner.isAtEnd)
    }

    @Test
    fun testGetRawUnconsumedTextUntouchedTabAtStart() {
        val scanner = LineScanner("\tabc")
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testGetRawUnconsumedTextUntouchedTabAfterConsumingLeadingSpace() {
        // 1 space followed by a tab: consuming the space leaves the tab completely untouched
        val scanner = LineScanner(" \tabc")
        assertEquals(' ', scanner.nextChar())
        assertEquals(1, scanner.index)
        assertEquals(1, scanner.column)
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testGetRawUnconsumedTextUntouchedTabAfterBlockQuotePrefix() {
        // "> " consumed (2 columns), followed by an untouched tab
        val scanner = LineScanner("> \tabc")
        assertEquals('>', scanner.nextChar())
        assertEquals(' ', scanner.nextChar())
        assertEquals(2, scanner.index)
        assertEquals(2, scanner.column)
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testGetRawUnconsumedTextPartiallyConsumedTab() {
        // ">\tHello": '>' is at col 0; consuming optional space after '>' consumes 1st tab space
        val scanner = LineScanner(">\tHello")
        assertEquals('>', scanner.nextChar()) // col 1
        assertEquals(' ', scanner.nextChar()) // col 2 (1st virtual space of tab consumed)
        // 2 virtual tab spaces remaining (col 2..4) materialized as spaces + "Hello"
        assertEquals("  Hello", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testGetRawUnconsumedTextFullyConsumedTabFollowedByUntouchedTab() {
        val scanner = LineScanner("\t\tabc")
        // Consume all 4 spaces of the first tab
        scanner.consume(4)
        assertEquals(1, scanner.index)
        assertEquals(4, scanner.column)
        // The second tab is untouched and must remain '\t'
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testSaveAndRestorePreservesInsideTabState() {
        val scanner = LineScanner(" \tabc")
        scanner.nextChar() // consume ' ' -> index 1 ('\t'), col 1, untouched tab
        val posBeforeTab = scanner.position
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())

        scanner.nextChar() // consume 1 virtual space of '\t' -> index 1, col 2, inside tab
        val posInsideTab = scanner.position
        assertEquals("  abc", scanner.getRawUnconsumedText().toString())

        // Restore to before tab was touched -> must preserve literal '\t'
        scanner.restore(posBeforeTab)
        assertEquals(1, scanner.index)
        assertEquals(1, scanner.column)
        assertEquals("\tabc", scanner.getRawUnconsumedText().toString())

        // Restore to inside tab -> must materialize remaining 2 spaces
        scanner.restore(posInsideTab)
        assertEquals(1, scanner.index)
        assertEquals(2, scanner.column)
        assertEquals("  abc", scanner.getRawUnconsumedText().toString())
    }

    @Test
    fun testLeadingSpacesAndFirstNonSpace() {
        val scanner = LineScanner("  \t> Quote")
        // 2 spaces + 2 tab spaces (to col 4) = 4 leading spaces, firstNonSpaceIndex = 3 ('>')
        assertEquals(4, scanner.leadingSpaceCount)
        assertEquals(3, scanner.firstNonSpaceIndex)
        assertEquals('>', scanner.firstNonSpaceChar)

        // Consume 1 space (' '): leadingSpaceCount should decrement to 3, index stays 3
        scanner.nextChar()
        assertEquals(3, scanner.leadingSpaceCount)
        assertEquals(3, scanner.firstNonSpaceIndex)
        assertEquals('>', scanner.firstNonSpaceChar)
    }
}
