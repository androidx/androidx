/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ArraySetTest {
    private val set = ArraySet<String>()

    @Test
    fun testCanNotIteratePastEnd() {
        val set = ArraySet<String>()
        set.add("value")
        val iterator: Iterator<String> = set.iterator()
        assertTrue(iterator.hasNext())
        assertEquals("value", iterator.next())
        assertFalse(iterator.hasNext())
        assertTrue(runCatching { iterator.next() }.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun addAllTypeProjection() {
        val set1 = ArraySet<Any>()
        val set2 = ArraySet<String>()
        set2.add("Foo")
        set2.add("Bar")
        set1.addAll(set2)
        assertEquals(set1.asIterable().toSet(), setOf("Bar", "Foo"))
    }

    /**
     * Test for implementation correction. This makes sure that all branches in ArraySet's backstore
     * shrinking code gets exercised.
     */
    @Test
    fun addAllThenRemoveOneByOne() {
        val sourceList = (0 until 10).toList()
        val set = ArraySet(sourceList)
        assertEquals(sourceList.size, set.size)

        for (e in sourceList) {
            assertTrue(set.contains(e))
            set.remove(e)
        }
        assertTrue(set.isEmpty())
    }

    /** Test for implementation correction of indexOf. */
    @Test
    fun addObjectsWithSameHashCode() {
        @Suppress("EqualsOrHashCode") // Testing for hash code collisions
        class Value {
            override fun hashCode(): Int = 42
        }

        val sourceList = (0 until 10).map { Value() }
        val set = ArraySet(sourceList)
        assertEquals(sourceList.size, set.size)

        for (e in sourceList) {
            assertEquals(e, set.valueAt(set.indexOf(e)))
        }
    }

    @Test
    fun constructWithArray() {
        val strings: Array<String> = arrayOf("a", "b", "c")
        val set = ArraySet(strings)
        assertEquals(3, set.size)
        assertTrue(set.contains("a"))
        assertFalse(set.contains("d"))
    }

    @Test
    fun arraySetEqualsAndHashCode() {
        val set3 = ArraySet(listOf(1, 2, 3))
        val set3b = ArraySet(listOf(1, 2, 3))
        val set4 = ArraySet(listOf(1, 2, 3, 4))
        assertEquals(set3, set3)
        assertEquals(set3.hashCode(), set3.hashCode())

        assertEquals(set3, set3b)
        assertEquals(set3.hashCode(), set3b.hashCode())

        assertNotEquals(set3, set4)
        assertNotEquals(set3.hashCode(), set4.hashCode())
    }

    @Test
    fun testToString() {
        val set3 = ArraySet(listOf(1, 2, 3))
        val set4 = ArraySet(listOf(1, 2, 3, 4))
        assertEquals("{1, 2, 3}", set3.toString())
        assertEquals("{1, 2, 3, 4}", set4.toString())

        val set5 = ArraySet<Any>()
        set5.add(1)
        set5.add("one")
        set5.add(set5)
        assertEquals("{1, one, (this Set)}", set5.toString())
    }
}
