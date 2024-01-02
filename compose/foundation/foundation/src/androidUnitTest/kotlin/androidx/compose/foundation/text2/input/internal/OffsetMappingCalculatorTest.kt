/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OffsetMappingCalculatorTest {

    @Test
    fun noChanges() {
        val builder = TestEditBuffer()
        builder.assertIdentityMapping()
    }

    @Test
    fun insertCharIntoEmpty() {
        val builder = TestEditBuffer()
        builder.append("a")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 1),
            1 to TextRange(2),
            2 to TextRange(3),
            3 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(1),
        )
    }

    @Test
    fun insertCharIntoMiddle() {
        val builder = TestEditBuffer("ab")
        builder.insert(1, "c")
        assertThat(builder.toString()).isEqualTo("acb")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1, 2),
            2 to TextRange(3),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(2),
        )
    }

    @Test
    fun deleteCharFromMiddle() {
        val builder = TestEditBuffer("abc")
        builder.delete(1)
        assertThat(builder.toString()).isEqualTo("ac")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(2),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1, 2),
            2 to TextRange(3),
            3 to TextRange(4),
        )
    }

    @Test
    fun replaceCharInMiddle() {
        val builder = TestEditBuffer("abc")
        builder.replace(1, "d")
        assertThat(builder.toString()).isEqualTo("adc")
        builder.assertIdentityMapping()
    }

    @Test
    fun insertStringIntoEmpty() {
        val builder = TestEditBuffer("")
        builder.append("ab")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 2),
            1 to TextRange(3),
            2 to TextRange(4),
            3 to TextRange(5),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(1),
        )
    }

    @Test
    fun insertStringIntoMiddle() {
        val builder = TestEditBuffer("ab")
        builder.insert(1, "cd")
        assertThat(builder.toString()).isEqualTo("acdb")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1, 3),
            2 to TextRange(4),
            3 to TextRange(5),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(1),
            4 to TextRange(2),
            5 to TextRange(3),
        )
    }

    @Test
    fun deleteStringFromMiddle() {
        val builder = TestEditBuffer("abcd")
        builder.delete(1, 3)
        assertThat(builder.toString()).isEqualTo("ad")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(1),
            4 to TextRange(2),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1, 3),
            2 to TextRange(4),
            3 to TextRange(5),
        )
    }

    @Test
    fun replaceStringWithEqualLengthInMiddle() {
        val builder = TestEditBuffer("abcd")
        builder.replace(1, 3, "ef")
        assertThat(builder.toString()).isEqualTo("aefd")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1, 3),
            3 to TextRange(3),
            4 to TextRange(4),
            5 to TextRange(5),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1, 3),
            3 to TextRange(3),
            4 to TextRange(4),
            5 to TextRange(5),
        )
    }

    @Test
    fun replaceStringWithLongerInMiddle() {
        val builder = TestEditBuffer("abcd")
        builder.replace(1, 3, "efg")
        assertThat(builder.toString()).isEqualTo("aefgd")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1, 4),
            3 to TextRange(4),
            4 to TextRange(5),
            5 to TextRange(6),
            6 to TextRange(7),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1, 3),
            3 to TextRange(1, 3),
            4 to TextRange(3),
            5 to TextRange(4),
        )
    }

    @Test
    fun replaceStringWithShorterInMiddle() {
        val builder = TestEditBuffer("abcd")
        builder.replace(1, 3, "e")
        assertThat(builder.toString()).isEqualTo("aed")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1, 2),
            3 to TextRange(2),
            4 to TextRange(3),
            5 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(3),
            3 to TextRange(4),
            4 to TextRange(5),
            5 to TextRange(6),
        )
    }

    @Test
    fun replaceAllWithEqualLength() {
        val builder = TestEditBuffer("abcd")
        builder.replace("efgh")
        assertThat(builder.toString()).isEqualTo("efgh")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 4),
            2 to TextRange(0, 4),
            3 to TextRange(0, 4),
            4 to TextRange(4),
            5 to TextRange(5),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 4),
            2 to TextRange(0, 4),
            3 to TextRange(0, 4),
            4 to TextRange(4),
            5 to TextRange(5),
        )
    }

    @Test
    fun replaceAllWithLonger() {
        val builder = TestEditBuffer("abcd")
        builder.replace("efghi")
        assertThat(builder.toString()).isEqualTo("efghi")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 5),
            2 to TextRange(0, 5),
            3 to TextRange(0, 5),
            4 to TextRange(5),
            5 to TextRange(6),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 4),
            2 to TextRange(0, 4),
            3 to TextRange(0, 4),
            4 to TextRange(0, 4),
            5 to TextRange(4),
            6 to TextRange(5),
        )
    }

    @Test
    fun replaceAllWithShorter() {
        val builder = TestEditBuffer("abcd")
        builder.replace("ef")
        assertThat(builder.toString()).isEqualTo("ef")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 2),
            2 to TextRange(0, 2),
            3 to TextRange(0, 2),
            4 to TextRange(2),
            5 to TextRange(3),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 4),
            2 to TextRange(4),
            3 to TextRange(5),
            4 to TextRange(6),
            5 to TextRange(7),
        )
    }

    @Test
    fun prependCharToString() {
        val builder = TestEditBuffer("a")
        builder.insert(0, "b")
        assertThat(builder.toString()).isEqualTo("ba")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 1),
            1 to TextRange(2),
            2 to TextRange(3),
            3 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(1),
        )
    }

    @Test
    fun prependStringToString() {
        val builder = TestEditBuffer("a")
        builder.insert(0, "bc")
        assertThat(builder.toString()).isEqualTo("bca")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 2),
            1 to TextRange(3),
            2 to TextRange(4),
            3 to TextRange(5),
            4 to TextRange(6),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(1),
        )
    }

    @Test
    fun appendCharToString() {
        val builder = TestEditBuffer("a")
        builder.append("b")
        assertThat(builder.toString()).isEqualTo("ab")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1, 2),
            2 to TextRange(3),
            3 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(2),
        )
    }

    @Test
    fun appendStringToString() {
        val builder = TestEditBuffer("a")
        builder.append("bc")
        assertThat(builder.toString()).isEqualTo("abc")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1, 3),
            2 to TextRange(4),
            3 to TextRange(5),
            4 to TextRange(6),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(1),
            4 to TextRange(2),
        )
    }

    @Test
    fun multiplePrepends() {
        val builder = TestEditBuffer("ab")
        builder.insert(0, "c")
        builder.insert(0, "d")
        builder.insert(0, "ef")
        assertThat(builder.toString()).isEqualTo("efdcab")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 4),
            1 to TextRange(5),
            2 to TextRange(6),
            3 to TextRange(7),
            4 to TextRange(8),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(0),
            4 to TextRange(0),
            5 to TextRange(1),
            6 to TextRange(2),
            7 to TextRange(3),
        )
    }

    @Test
    fun multipleAppends() {
        val builder = TestEditBuffer("ab")
        builder.append("c")
        builder.append("d")
        builder.append("ef")
        assertThat(builder.toString()).isEqualTo("abcdef")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2, 6),
            3 to TextRange(7),
            4 to TextRange(8),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2),
            3 to TextRange(2),
            4 to TextRange(2),
            5 to TextRange(2),
            6 to TextRange(2),
            7 to TextRange(3),
        )
    }

    @Test
    fun multiplePrependsThenDeletesCancellingOut() {
        val builder = TestEditBuffer("ab")
        builder.insert(0, "cde") // cdeab
        builder.delete(2) // cdab
        builder.delete(0) // dab
        builder.insert(0, "f") // fdab
        builder.delete(0, 2)
        assertThat(builder.toString()).isEqualTo("ab")
        builder.assertIdentityMapping()
    }

    @Test
    fun multipleAppendsThenDeletesCancellingOut() {
        val builder = TestEditBuffer("ab")
        builder.append("cde") // abcde
        builder.delete(2) // abde
        builder.delete(3) // abd
        builder.append("f") // abdf
        builder.delete(2, 4)
        assertThat(builder.toString()).isEqualTo("ab")
        builder.assertIdentityMapping()
    }

    @Test
    fun multipleInsertsThenDeletesCancellingOut() {
        val builder = TestEditBuffer("ab")
        builder.insert(1, "c")
        builder.insert(2, "de")
        builder.insert(1, "f")
        builder.delete(1, 3)
        builder.delete(1)
        builder.delete(1)
        assertThat(builder.toString()).isEqualTo("ab")
        builder.assertIdentityMapping()
    }

    @Test
    fun multipleContinuousDeletesAtStartInOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(0)
        builder.delete(0)
        builder.delete(0)
        assertThat(builder.toString()).isEqualTo("def")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(0),
            4 to TextRange(1),
            5 to TextRange(2),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0, 3),
            1 to TextRange(4),
            2 to TextRange(5),
            3 to TextRange(6),
            4 to TextRange(7),
        )
    }

    @Test
    fun multipleContinuousDeletesAtStartOutOfOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(1)
        builder.delete(1)
        builder.delete(0)
        assertThat(builder.toString()).isEqualTo("def")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(0),
            4 to TextRange(1),
            5 to TextRange(2),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0, 3),
            1 to TextRange(4),
            2 to TextRange(5),
            3 to TextRange(6),
            4 to TextRange(7),
        )
    }

    @Test
    fun multipleContinuousDeletesAtEndInOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(builder.length - 1)
        builder.delete(builder.length - 1)
        builder.delete(builder.length - 1)
        assertThat(builder.toString()).isEqualTo("abc")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2),
            3 to TextRange(3),
            4 to TextRange(3),
            5 to TextRange(3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2),
            3 to TextRange(3, 6),
            4 to TextRange(7),
            5 to TextRange(8),
        )
    }

    @Test
    fun multipleContinuousDeletesAtEndOutOfOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(4)
        builder.delete(3)
        builder.delete(3)
        assertThat(builder.toString()).isEqualTo("abc")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2),
            3 to TextRange(3),
            4 to TextRange(3),
            5 to TextRange(3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(2),
            3 to TextRange(3, 6),
            4 to TextRange(7),
            5 to TextRange(8),
        )
    }

    @Test
    fun multipleContinuousDeletesInMiddleInOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(1)
        builder.delete(1, 3)
        builder.delete(1)
        assertThat(builder.toString()).isEqualTo("af")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(1),
            4 to TextRange(1),
            5 to TextRange(1),
            6 to TextRange(2),
            7 to TextRange(3),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1, 5),
            2 to TextRange(6),
            3 to TextRange(7),
            4 to TextRange(8),
            5 to TextRange(9),
        )
    }

    @Test
    fun multipleContinuousDeletesInMiddleOutOfOrder() {
        val builder = TestEditBuffer("abcdef")
        builder.delete(2)
        builder.delete(2, 4)
        builder.delete(1)
        assertThat(builder.toString()).isEqualTo("af")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(1),
            2 to TextRange(1),
            3 to TextRange(1),
            4 to TextRange(1),
            5 to TextRange(1),
            6 to TextRange(2),
            7 to TextRange(3),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(1, 5),
            2 to TextRange(6),
            3 to TextRange(7),
            4 to TextRange(8),
            5 to TextRange(9),
        )
    }

    @Test
    fun discontinuousInsertsAndDeletes() {
        val builder = TestEditBuffer("ab")
        builder.insert(1, "cde") // acdeb
        builder.delete(2) // aceb
        builder.append("fgh") // acebfgh
        builder.delete(4) // acebgh
        builder.insert(0, "ijk") // ijkacebgh
        builder.delete(2) // ijacebgh
        assertThat(builder.toString()).isEqualTo("ijacebgh")
        builder.assertMappingsFromSource(
            0 to TextRange(0, 2),
            1 to TextRange(3, 5),
            2 to TextRange(6, 8),
            3 to TextRange(9),
            4 to TextRange(10),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0),
            2 to TextRange(0),
            3 to TextRange(1),
            4 to TextRange(1),
            5 to TextRange(1),
            6 to TextRange(2),
            7 to TextRange(2),
            8 to TextRange(2),
            9 to TextRange(3),
        )
    }

    @Test
    fun multipleContinuousOneToOneReplacements() {
        val builder = TestEditBuffer("abc")
        builder.replace(0, "f")
        builder.replace(1, "f")
        builder.replace(2, "f")
        assertThat(builder.toString()).isEqualTo("fff")
        builder.assertIdentityMapping()
    }

    /** This simulates an expanding codepoint transform. */
    @Test
    fun multipleContinuousOneToManyReplacements() {
        val builder = TestEditBuffer("abc")
        builder.replace(0, "dd") // ddbc
        builder.replace(2, "ee") // ddeec
        builder.replace(4, "ff")
        assertThat(builder.toString()).isEqualTo("ddeeff")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(2),
            2 to TextRange(4),
            3 to TextRange(6),
            4 to TextRange(7),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 1),
            2 to TextRange(1),
            3 to TextRange(1, 2),
            4 to TextRange(2),
            5 to TextRange(2, 3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
    }

    @Test
    fun multipleContinuousOneToManyReplacementsReversed() {
        val builder = TestEditBuffer("abc")
        builder.replace(2, "dd") // abdd
        builder.replace(1, "ee") // aeedd
        builder.replace(0, "ff")
        assertThat(builder.toString()).isEqualTo("ffeedd")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(2),
            2 to TextRange(4),
            3 to TextRange(6),
            4 to TextRange(7),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 1),
            2 to TextRange(1),
            3 to TextRange(1, 2),
            4 to TextRange(2),
            5 to TextRange(2, 3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
    }

    /** This simulates a contracting codepoint transform. */
    @Test
    fun multipleContinuousManyToOneReplacements() {
        val builder = TestEditBuffer("abcdef")
        builder.replace(0, 2, "g") // gcdef
        builder.replace(1, 3, "h") // ghef
        builder.replace(2, 4, "i")
        assertThat(builder.toString()).isEqualTo("ghi")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 1),
            2 to TextRange(1),
            3 to TextRange(1, 2),
            4 to TextRange(2),
            5 to TextRange(2, 3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(2),
            2 to TextRange(4),
            3 to TextRange(6),
            4 to TextRange(7),
        )
    }

    @Test
    fun multipleContinuousManyToOneReplacementsReversed() {
        val builder = TestEditBuffer("abcdef")
        builder.replace(4, 6, "g") // abcdg
        builder.replace(2, 4, "h") // abhg
        builder.replace(0, 2, "i")
        assertThat(builder.toString()).isEqualTo("ihg")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 1),
            2 to TextRange(1),
            3 to TextRange(1, 2),
            4 to TextRange(2),
            5 to TextRange(2, 3),
            6 to TextRange(3),
            7 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(2),
            2 to TextRange(4),
            3 to TextRange(6),
            4 to TextRange(7),
        )
    }

    /**
     * This sequence of operations is basically nonsense and so the mappings don't make much sense
     * either. This test is just here to ensure the output is consistent and doesn't crash.
     */
    @Test
    fun twoOverlappingReplacements() {
        val builder = TestEditBuffer("abc")
        builder.replace(0, 2, "wx") // wxc
        builder.replace(1, 3, "yz")
        assertThat(builder.toString()).isEqualTo("wyz")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 3),
            2 to TextRange(1, 3),
            3 to TextRange(3),
            4 to TextRange(4),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 2),
            2 to TextRange(0, 3),
            3 to TextRange(3),
            4 to TextRange(4),
        )
    }

    /**
     * This sequence of operations is basically nonsense and so the mappings don't make much sense
     * either. This test is just here to ensure the output is consistent and doesn't crash.
     */
    @Test
    fun fourOverlappingReplacementsReversed() {
        val builder = TestEditBuffer("abcde")
        builder.replace(1, 3, "fg") // afgde
        builder.replace(2, 4, "hi") // afhie
        builder.replace(0, 2, "jk") // jkhie
        builder.replace(3, 5, "lm")
        assertThat(builder.toString()).isEqualTo("jkhlm")
        builder.assertMappingsFromSource(
            0 to TextRange(0),
            1 to TextRange(0, 2),
            2 to TextRange(0, 5),
            3 to TextRange(2, 5),
            4 to TextRange(3, 5),
            5 to TextRange(5),
            6 to TextRange(6),
            7 to TextRange(7),
        )
        builder.assertMappingsFromDest(
            0 to TextRange(0),
            1 to TextRange(0, 3),
            2 to TextRange(1, 3),
            3 to TextRange(1, 4),
            4 to TextRange(1, 5),
            5 to TextRange(5),
            6 to TextRange(6),
            7 to TextRange(7),
        )
    }

    private fun TestEditBuffer.assertIdentityMapping() {
        // Check well off the end of the valid index range just to be sure.
        repeat(length + 2) {
            assertWithMessage("Mapping from source offset $it")
                .that(mapFromSource(it)).isEqualTo(TextRange(it))
            assertWithMessage("Mapping from dest offset $it")
                .that(mapFromDest(it)).isEqualTo(TextRange(it))
        }
    }

    private fun TestEditBuffer.assertMappingsFromSource(
        vararg expectedMappings: Pair<Int, TextRange>
    ) {
        expectedMappings.forEach { (srcOffset, dstRange) ->
            assertWithMessage("Mapping from source offset $srcOffset")
                .that(mapFromSource(srcOffset)).isEqualTo(dstRange)
        }
    }

    private fun TestEditBuffer.assertMappingsFromDest(
        vararg expectedMappings: Pair<Int, TextRange>
    ) {
        expectedMappings.forEach { (dstOffset, srcRange) ->
            assertWithMessage("Mapping from dest offset $dstOffset")
                .that(mapFromDest(dstOffset)).isEqualTo(srcRange)
        }
    }

    /**
     * Basic implementation of a text editing buffer that uses [OffsetMappingCalculator] to make
     * testing easier.
     */
    private class TestEditBuffer private constructor(
        private val builder: StringBuilder
    ) : CharSequence by builder {
        constructor(text: CharSequence = "") : this(StringBuilder(text))

        private val tracker = OffsetMappingCalculator()

        fun append(text: CharSequence) {
            tracker.recordEditOperation(length, length, text.length)
            builder.append(text)
        }

        fun insert(offset: Int, value: CharSequence) {
            tracker.recordEditOperation(offset, offset, value.length)
            builder.insert(offset, value)
        }

        fun delete(start: Int, end: Int = start + 1) {
            tracker.recordEditOperation(start, end, 0)
            builder.delete(minOf(start, end), maxOf(start, end))
        }

        fun replace(value: String) {
            replace(0, length, value)
        }

        fun replace(start: Int, value: String) {
            replace(start, start + 1, value)
        }

        fun replace(start: Int, end: Int, value: String) {
            tracker.recordEditOperation(start, end, value.length)
            builder.replace(minOf(start, end), maxOf(start, end), value)
        }

        fun mapFromSource(offset: Int): TextRange = tracker.mapFromSource(offset)
        fun mapFromDest(offset: Int): TextRange = tracker.mapFromDest(offset)

        override fun toString(): String = builder.toString()
    }
}
