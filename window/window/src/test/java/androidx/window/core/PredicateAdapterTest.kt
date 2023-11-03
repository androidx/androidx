/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.core

import android.os.Build
import java.util.function.Predicate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredicateAdapterTest {

    private val loader = PredicateAdapterTest::class.java.classLoader!!
    private val predicate = { s: String -> s.isEmpty() }
    private val pairPredicate = { s: String, t: String -> s == t }
    private val adapter = PredicateAdapter(loader)

    @Test
    fun testEquals_sameReference() {
        val obj = adapter.buildPredicate(String::class, predicate)

        assertTrue(obj == obj)
    }

    @Test
    fun testEquals_differentReference() {
        val lhs = adapter.buildPredicate(String::class, predicate)
        val rhs = adapter.buildPredicate(String::class, predicate)

        assertFalse(lhs == rhs)
    }

    @Test
    fun testPairEquals_sameReference() {
        val obj = adapter.buildPairPredicate(String::class, String::class, pairPredicate)

        assertTrue(obj == obj)
    }

    @Test
    fun testPairEquals_differentReference() {
        val lhs = adapter.buildPairPredicate(String::class, String::class, pairPredicate)
        val rhs = adapter.buildPairPredicate(String::class, String::class, pairPredicate)

        assertFalse(lhs == rhs)
    }

    @Test
    fun testHashCode() {
        val actual = adapter.buildPredicate(String::class, predicate).hashCode()
        assertEquals(predicate.hashCode(), actual)
    }

    @Test
    fun testPairHashCode() {
        val actual = adapter.buildPairPredicate(String::class, String::class, pairPredicate)
            .hashCode()
        assertEquals(pairPredicate.hashCode(), actual)
    }

    @Test
    fun testToString() {
        val actual = adapter.buildPredicate(String::class, predicate).toString()
        assertEquals(predicate.toString(), actual)
    }

    @Test
    fun testPairToString() {
        val actual = adapter.buildPairPredicate(String::class, String::class, pairPredicate)
            .toString()
        assertEquals(pairPredicate.toString(), actual)
    }

    @Test
    @Suppress("UNCHECKED_CAST") //
    fun testWrapPredicate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        val actual = adapter.buildPredicate(String::class, predicate) as Predicate<String>
        val inputs = listOf("", "a", "abcd")
        inputs.forEach { data ->
            assertEquals("Checking predicate on $data", predicate(data), actual.test(data))
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST") //
    fun testWrapPairPredicate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        val actual = adapter.buildPairPredicate(
            String::class,
            String::class,
            pairPredicate
        ) as Predicate<Pair<String, String>>

        val inputs = listOf("", "a").zip(listOf("", "b"))
        inputs.forEach { data ->
            assertEquals(
                "Checking predicate on $data",
                pairPredicate(data.first, data.second),
                actual.test(data)
            )
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST") //
    fun test_additionalPredicateMethods() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        val actual = adapter.buildPredicate(String::class, predicate) as Predicate<String>
        val innerAnd = actual.and { true }
        val outerAnd = Predicate<String> { true }.and(actual)

        val innerOr = actual.and { true }
        val outerOr = Predicate<String> { true }.and(actual)

        val notNot = actual.negate().negate()

        val inputs = listOf("", "a", "abcd")
        inputs.forEach { data ->
            assertEquals(
                "Checking innerAnd predicate on $data",
                innerAnd.test(data),
                actual.test(data)
            )
            assertEquals(
                "Checking outerAnd predicate on $data",
                outerAnd.test(data),
                actual.test(data)
            )
            assertEquals(
                "Checking innerOr predicate on $data",
                innerOr.test(data),
                actual.test(data)
            )
            assertEquals(
                "Checking outerOr predicate on $data",
                outerOr.test(data),
                actual.test(data)
            )
            assertEquals(
                "Checking notNot predicate on $data",
                notNot.test(data),
                actual.test(data)
            )
        }
    }
}
