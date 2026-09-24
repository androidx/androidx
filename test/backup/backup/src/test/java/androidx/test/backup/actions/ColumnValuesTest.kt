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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ColumnValuesTest {

    @Test
    fun decodesASinglePair() {
        assertEquals(listOf("name" to "Ada"), decodeColumnValues("name=Ada"))
    }

    @Test
    fun decodesMultiplePairsInOrder() {
        assertEquals(
            listOf("col1" to "val1", "col2" to "val2"),
            decodeColumnValues("col1=val1&col2=val2"),
        )
    }

    @Test
    fun decodesAnEmptyValue() {
        assertEquals(listOf("col" to ""), decodeColumnValues("col="))
    }

    /** The separators are exactly what percent-encoding exists to protect. */
    @Test
    fun decodesSeparatorsInsideAValue() {
        assertEquals(listOf("q" to "a&b=c"), decodeColumnValues("q=a%26b%3Dc"))
    }

    @Test
    fun decodesAPercentInsideAValue() {
        assertEquals(listOf("pct" to "50%"), decodeColumnValues("pct=50%25"))
    }

    /** Documents the `+` convention: an encoded space decodes back to a space. */
    @Test
    fun decodesPlusAsSpace() {
        assertEquals(listOf("city" to "Rio de Janeiro"), decodeColumnValues("city=Rio+de+Janeiro"))
    }

    /** Round-trips whatever the host produced with the same encoder. */
    @Test
    fun roundTripsValuesNeedingEscapes() {
        val pairs = listOf("a&b" to "c=d", "e%f" to "g+h", "unicode" to "café")
        val encoded =
            pairs.joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }

        assertEquals(pairs, decodeColumnValues(encoded))
    }

    @Test
    fun rejectsAnEmptyString() {
        assertThrows(IllegalArgumentException::class.java) { decodeColumnValues("") }
    }

    /** A malformed segment used to be dropped silently, hiding a column from the assertion. */
    @Test
    fun rejectsASegmentWithoutASeparator() {
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                decodeColumnValues("col1=val1&orphan")
            }
        assertEquals(true, failure.message?.contains("orphan"))
    }

    @Test
    fun rejectsAnEmptyColumnName() {
        assertThrows(IllegalArgumentException::class.java) { decodeColumnValues("=val") }
    }

    @Test
    fun rejectsInvalidPercentEncoding() {
        assertThrows(IllegalArgumentException::class.java) { decodeColumnValues("col=%zz") }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
