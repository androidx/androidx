/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.input

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@SmallTest
@RunWith(JUnit4::class)
class GapBufferTest {
    @Test
    fun insertTest_insert_to_empty_string() {
        assertEquals("A", PartialGapBuffer("").apply {
            replace(0, 0, "A")
        }.toString())
    }

    @Test
    fun insertTest_insert_and_append() {
        assertEquals("BA", PartialGapBuffer("").apply {
            replace(0, 0, "A")
            replace(0, 0, "B")
        }.toString())
    }

    @Test
    fun insertTest_insert_and_prepend() {
        assertEquals("AB", PartialGapBuffer("").apply {
            replace(0, 0, "A")
            replace(1, 1, "B")
        }.toString())
    }

    @Test
    fun insertTest_insert_and_insert_into_middle() {
        assertEquals("ABA", PartialGapBuffer("").apply {
            replace(0, 0, "AA")
            replace(1, 1, "B")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_prepend() {
        assertEquals("AXX", PartialGapBuffer("XX").apply {
            replace(0, 0, "A")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_insert_into_middle() {
        assertEquals("XAX", PartialGapBuffer("XX").apply {
            replace(1, 1, "A")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_append() {
        assertEquals("XXA", PartialGapBuffer("XX").apply {
            replace(2, 2, "A")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_prepend() {
        assertEquals("BAXX", PartialGapBuffer("XX").apply {
            replace(0, 0, "A")
            replace(0, 0, "B")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_append() {
        assertEquals("ABXX", PartialGapBuffer("XX").apply {
            replace(0, 0, "A")
            replace(1, 1, "B")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_prepend_and_insert_middle() {
        assertEquals("AXBX", PartialGapBuffer("XX").apply {
            replace(0, 0, "A")
            replace(2, 2, "B")
        }.toString())
    }

    @Test
    fun insertTest_intoExistingText_insert_two_chars_and_append() {
        assertEquals("ABAXX", PartialGapBuffer("XX").apply {
            replace(0, 0, "AA")
            replace(1, 1, "B")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_from_head() {
        assertEquals("BC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_middle() {
        assertEquals("AC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_tail() {
        assertEquals("AB", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(2, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_two_head() {
        assertEquals("C", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_two_tail() {
        assertEquals("A", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_with_two_instruction_from_haed() {
        assertEquals("C", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "")
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_head_and_tail() {
        assertEquals("B", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "")
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delet_with_two_instruction_from_tail() {
        assertEquals("A", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 2, "")
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars() {
        assertEquals("", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_insert_and_delete_three_chars_with_three_instructions() {
        assertEquals("", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "")
            replace(0, 1, "")
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_from_head() {
        assertEquals("BC", PartialGapBuffer("ABC").apply {
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_from_middle() {
        assertEquals("AC", PartialGapBuffer("ABC").apply {
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_from_tail() {
        assertEquals("AB", PartialGapBuffer("ABC").apply {
            replace(2, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_head() {
        assertEquals("C", PartialGapBuffer("ABC").apply {
            replace(0, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_from_tail() {
        assertEquals("A", PartialGapBuffer("ABC").apply {
            replace(1, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head() {
        assertEquals("C", PartialGapBuffer("ABC").apply {
            replace(0, 1, "")
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_head_and_tail() {
        assertEquals("B", PartialGapBuffer("ABC").apply {
            replace(0, 1, "")
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_two_chars_with_two_instruction_from_tail() {
        assertEquals("A", PartialGapBuffer("ABC").apply {
            replace(1, 2, "")
            replace(1, 2, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars() {
        assertEquals("", PartialGapBuffer("ABC").apply {
            replace(0, 3, "")
        }.toString())
    }

    @Test
    fun deleteTest_fromExistingText_delete_three_chars_with_three_instructions() {
        assertEquals("", PartialGapBuffer("ABC").apply {
            replace(0, 1, "")
            replace(0, 1, "")
            replace(0, 1, "")
        }.toString())
    }

    @Test
    fun replaceTest_head() {
        assertEquals("XBC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "X")
        }.toString())
    }

    @Test
    fun replaceTest_middle() {
        assertEquals("AXC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 2, "X")
        }.toString())
    }

    @Test
    fun replaceTest_tail() {
        assertEquals("ABX", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(2, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_head_two_chars() {
        assertEquals("XC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 2, "X")
        }.toString())
    }

    @Test
    fun replaceTest_middle_two_chars() {
        assertEquals("AX", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_three_chars() {
        assertEquals("X", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_head() {
        assertEquals("XYBC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 1, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_middle() {
        assertEquals("AXYC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 2, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_one_char_with_two_chars_from_tail() {
        assertEquals("ABXY", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(2, 3, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_head() {
        assertEquals("XYC", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 2, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_two_chars_with_two_chars_from_tail() {
        assertEquals("AXY", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(1, 3, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_three_chars_with_two_char() {
        assertEquals("XY", PartialGapBuffer("").apply {
            replace(0, 0, "ABC")
            replace(0, 3, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_head() {
        assertEquals("XBC", PartialGapBuffer("ABC").apply {
            replace(0, 1, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_middle() {
        assertEquals("AXC", PartialGapBuffer("ABC").apply {
            replace(1, 2, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_tail() {
        assertEquals("ABX", PartialGapBuffer("ABC").apply {
            replace(2, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_head() {
        assertEquals("XC", PartialGapBuffer("ABC").apply {
            replace(0, 2, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_one_char_from_tail() {
        assertEquals("AX", PartialGapBuffer("ABC").apply {
            replace(1, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_three_chars() {
        assertEquals("X", PartialGapBuffer("ABC").apply {
            replace(0, 3, "X")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_head() {
        assertEquals("XYBC", PartialGapBuffer("ABC").apply {
            replace(0, 1, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_middle() {
        assertEquals("AXYC", PartialGapBuffer("ABC").apply {
            replace(1, 2, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_one_char_with_two_chars_from_tail() {
        assertEquals("ABXY", PartialGapBuffer("ABC").apply {
            replace(2, 3, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_head() {
        assertEquals("XYC", PartialGapBuffer("ABC").apply {
            replace(0, 2, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_two_chars_with_two_chars_from_tail() {
        assertEquals("AXY", PartialGapBuffer("ABC").apply {
            replace(1, 3, "XY")
        }.toString())
    }

    @Test
    fun replaceTest_fromExistingText_three_chars_with_three_chars() {
        assertEquals("XY", PartialGapBuffer("ABC").apply {
            replace(0, 3, "XY")
        }.toString())
    }

    // Compare with the result of StringBuffer. We trust the StringBuffer works correctly
    private fun assertReplace(
        start: Int,
        end: Int,
        str: String,
        sb: StringBuffer,
        gb: PartialGapBuffer
    ) {
        sb.replace(start, end, str)
        gb.replace(start, end, str)
        assertEquals(sb.toString(), gb.toString())
    }

    private val LONG_INIT_TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(256)
    private val SHORT_TEXT = "A"
    private val MEDIUM_TEXT = "Hello, World"
    private val LONG_TEXT = "abcdefghijklmnopqrstuvwxyz".repeat(16)

    @Test
    fun longTextTest_keep_insertion() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        var c = 256 // cursor
        assertReplace(c, c, SHORT_TEXT, sb, gb)
        c += SHORT_TEXT.length
        assertReplace(c, c, MEDIUM_TEXT, sb, gb)
        c += MEDIUM_TEXT.length
        assertReplace(c, c, LONG_TEXT, sb, gb)
        c += LONG_TEXT.length
        assertReplace(c, c, MEDIUM_TEXT, sb, gb)
        c += MEDIUM_TEXT.length
        assertReplace(c, c, SHORT_TEXT, sb, gb)
    }

    @Test
    fun longTextTest_keep_deletion() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        var c = 2048 // cursor
        // Forward deletion
        assertReplace(c, c + 10, "", sb, gb)
        assertReplace(c, c + 100, "", sb, gb)
        assertReplace(c, c + 1000, "", sb, gb)

        // Backspacing
        assertReplace(c - 10, c, "", sb, gb)
        c -= 10
        assertReplace(c - 100, c, "", sb, gb)
        c -= 100
        assertReplace(c - 1000, c, "", sb, gb)
    }

    @Test
    fun longTextTest_farInput() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        assertReplace(1024, 1024, "Hello, World", sb, gb)
        assertReplace(128, 128, LONG_TEXT, sb, gb)
    }

    @Test
    fun randomInsertDeleteStressTest() {
        val sb = StringBuffer(LONG_INIT_TEXT)
        val gb = PartialGapBuffer(LONG_INIT_TEXT)

        val r = Random(10 /* fix the seed for reproduction */)

        val INSERT_TEXTS = arrayOf(SHORT_TEXT, MEDIUM_TEXT, LONG_TEXT)
        val DEL_LENGTHS = arrayOf(1, 10, 100)

        var c = LONG_INIT_TEXT.length / 2

        for (i in 0..100) {
            when (r.nextInt() % 4) {
                0 -> { // insert
                    val txt = INSERT_TEXTS.random(r)
                    assertReplace(c, c, txt, sb, gb)
                    c += txt.length
                }
                1 -> { // forward delete
                    assertReplace(c, c + DEL_LENGTHS.random(r), "", sb, gb)
                }
                2 -> { // backspacing
                    val len = DEL_LENGTHS.random(r)
                    assertReplace(c - len, c, "", sb, gb)
                    c -= len
                }
                3 -> { // replacing
                    val txt = INSERT_TEXTS.random(r)
                    val len = DEL_LENGTHS.random(r)

                    assertReplace(c, c + len, txt, sb, gb)
                }
            }
        }
    }
}