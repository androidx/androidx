/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import org.junit.Assert.assertEquals
import org.junit.Test

class FailureMessagesTest {

    @Test
    fun shownQuotesAShortValue() {
        assertEquals("'abc'", shown("abc"))
        assertEquals("'7'", shown(7))
        assertEquals("'null'", shown(null))
    }

    @Test
    fun shownCutsALongValueAndGivesItsLength() {
        assertEquals("'${"a".repeat(64)}...' (100 chars)", shown("a".repeat(100)))
    }

    @Test
    fun shownNeverSplitsASurrogatePair() {
        val value = "a".repeat(63) + "\uD83D\uDE00" + "b".repeat(10)

        assertEquals("'${"a".repeat(63)}...' (75 chars)", shown(value))
    }

    /** A stored set can come back in another order, so sets are shown sorted. */
    @Test
    fun shownSortsTheElementsOfASet() {
        assertEquals("'[a, b]'", shown(linkedSetOf("b", "a")))
    }

    @Test
    fun shownGivesTheSizeOfAByteArray() {
        assertEquals("(3 bytes)", shown(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun textMismatchShowsShortTextsWhole() {
        assertEquals(
            "Expected file content 'abc' but was 'abd'",
            textMismatch("file content", "abc", "abd"),
        )
    }

    @Test
    fun textMismatchShowsWhereLongTextsDiffer() {
        val expected = "x".repeat(100) + "A" + "y".repeat(100)
        val actual = "x".repeat(100) + "B" + "y".repeat(100)

        assertEquals(
            "Expected file content of 201 chars but was 201 chars, first differing at index 100: " +
                "expected '...${"x".repeat(16)}A${"y".repeat(47)}...' but was " +
                "'...${"x".repeat(16)}B${"y".repeat(47)}...'",
            textMismatch("file content", expected, actual),
        )
    }

    @Test
    fun textMismatchShowsTheEndOfAShorterText() {
        val expected = "a".repeat(100)
        val actual = "a".repeat(70)

        assertEquals(
            "Expected file content of 100 chars but was 70 chars, first differing at index 70: " +
                "expected '...${"a".repeat(46)}' but was <end>",
            textMismatch("file content", expected, actual),
        )
    }

    /** Both ends of an excerpt that would fall inside a surrogate pair move out of it. */
    @Test
    fun textMismatchNeverSplitsASurrogatePair() {
        val emoji = "\uD83D\uDE00"
        // The excerpt would start at index 21 and end at index 84, inside the two emojis.
        val expected = "a".repeat(20) + emoji + "x".repeat(15) + "B" + "y".repeat(45) + emoji + "z"
        val actual = expected.replace("B", "C")

        assertEquals(
            "Expected file content of 86 chars but was 86 chars, first differing at index 37: " +
                "expected '...${emoji}${"x".repeat(15)}B${"y".repeat(45)}...' but was " +
                "'...${emoji}${"x".repeat(15)}C${"y".repeat(45)}...'",
            textMismatch("file content", expected, actual),
        )
    }

    @Test
    fun binaryMismatchGivesTheSizesAndTheFirstDifference() {
        assertEquals(
            "Expected binary file content of 3 bytes but was 3 bytes, first differing at byte 1",
            binaryMismatch("binary file content", byteArrayOf(1, 2, 3), byteArrayOf(1, 9, 3)),
        )
        assertEquals(
            "Expected binary file content of 3 bytes but was 2 bytes, first differing at byte 2",
            binaryMismatch("binary file content", byteArrayOf(1, 2, 3), byteArrayOf(1, 2)),
        )
    }
}
