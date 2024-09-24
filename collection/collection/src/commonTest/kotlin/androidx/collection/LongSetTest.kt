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
package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
// DO NOT MAKE CHANGES to the kotlin source file.
//
// This file was generated from a template in the template directory.
// Make a change to the original template and run the generateCollections.sh script
// to ensure the change is available on all versions of the map.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

class LongSetTest {
    @Test
    fun emptyLongSetConstructor() {
        val set = MutableLongSet()
        assertEquals(7, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun immutableEmptyLongSet() {
        val set: LongSet = emptyLongSet()
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun zeroCapacityLongSet() {
        val set = MutableLongSet(0)
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun emptyLongSetWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val set = MutableLongSet(1800)
        assertEquals(4095, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun mutableLongSetBuilder() {
        val empty = mutableLongSetOf()
        assertEquals(0, empty.size)

        val withElements = mutableLongSetOf(1L, 2L)
        assertEquals(2, withElements.size)
        assertTrue(1L in withElements)
        assertTrue(2L in withElements)
    }

    @Test
    fun addToLongSet() {
        val set = MutableLongSet()
        set += 1L
        assertTrue(set.add(2L))

        assertEquals(2, set.size)
        val elements = LongArray(2)
        var index = 0
        set.forEach { element -> elements[index++] = element }
        elements.sort()
        assertEquals(1L, elements[0])
        assertEquals(2L, elements[1])
    }

    @Test
    fun addToSizedLongSet() {
        val set = MutableLongSet(12)
        set += 1L

        assertEquals(1, set.size)
        assertEquals(1L, set.first())
    }

    @Test
    fun addExistingElement() {
        val set = MutableLongSet(12)
        set += 1L
        assertFalse(set.add(1L))
        set += 1L

        assertEquals(1, set.size)
        assertEquals(1L, set.first())
    }

    @Test
    fun addAllArray() {
        val set = mutableLongSetOf(1L)
        assertFalse(set.addAll(longArrayOf(1L)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(longArrayOf(1L, 2L)))
        assertEquals(2, set.size)
        assertTrue(2L in set)
    }

    @Test
    fun addAllLongSet() {
        val set = mutableLongSetOf(1L)
        assertFalse(set.addAll(mutableLongSetOf(1L)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(mutableLongSetOf(1L, 2L)))
        assertEquals(2, set.size)
        assertTrue(2L in set)
    }

    @Test
    fun plusAssignArray() {
        val set = mutableLongSetOf(1L)
        set += longArrayOf(1L)
        assertEquals(1, set.size)
        set += longArrayOf(1L, 2L)
        assertEquals(2, set.size)
        assertTrue(2L in set)
    }

    @Test
    fun plusAssignLongSet() {
        val set = mutableLongSetOf(1L)
        set += mutableLongSetOf(1L)
        assertEquals(1, set.size)
        set += mutableLongSetOf(1L, 2L)
        assertEquals(2, set.size)
        assertTrue(2L in set)
    }

    @Test
    fun firstWithValue() {
        val set = MutableLongSet()
        set += 1L
        set += 2L
        var element: Long = -1L
        var otherElement: Long = -1L
        set.forEach { if (element == -1L) element = it else otherElement = it }
        assertEquals(element, set.first())
        set -= element
        assertEquals(otherElement, set.first())
    }

    @Test
    fun firstEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableLongSet()
            set.first()
        }
    }

    @Test
    fun firstMatching() {
        val set = MutableLongSet()
        set += 1L
        set += 2L
        assertEquals(1L, set.first { it < 2L })
        assertEquals(2L, set.first { it > 1L })
    }

    @Test
    fun firstMatchingEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableLongSet()
            set.first { it > 0L }
        }
    }

    @Test
    fun firstMatchingNoMatch() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableLongSet()
            set += 1L
            set += 2L
            set.first { it < 0L }
        }
    }

    @Test
    fun remove() {
        val set = MutableLongSet()
        assertFalse(set.remove(1L))

        set += 1L
        assertTrue(set.remove(1L))
        assertEquals(0, set.size)

        set += 1L
        set -= 1L
        assertEquals(0, set.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val set = MutableLongSet(6)
        set += 1L
        set += 5L
        set += 6L
        set += 9L
        set += 11L
        set += 13L

        // Removing all the entries will mark the medata as deleted
        set.remove(1L)
        set.remove(5L)
        set.remove(6L)
        set.remove(9L)
        set.remove(11L)
        set.remove(13L)

        assertEquals(0, set.size)

        val capacity = set.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        set += 1L

        assertEquals(1, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun removeAllArray() {
        val set = mutableLongSetOf(1L, 2L)
        assertFalse(set.removeAll(longArrayOf(3L, 5L)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(longArrayOf(3L, 1L, 5L)))
        assertEquals(1, set.size)
        assertFalse(1L in set)
    }

    @Test
    fun removeAllLongSet() {
        val set = mutableLongSetOf(1L, 2L)
        assertFalse(set.removeAll(mutableLongSetOf(3L, 5L)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(mutableLongSetOf(3L, 1L, 5L)))
        assertEquals(1, set.size)
        assertFalse(1L in set)
    }

    @Test
    fun minusAssignArray() {
        val set = mutableLongSetOf(1L, 2L)
        set -= longArrayOf(3L, 5L)
        assertEquals(2, set.size)
        set -= longArrayOf(3L, 1L, 5L)
        assertEquals(1, set.size)
        assertFalse(1L in set)
    }

    @Test
    fun minusAssignLongSet() {
        val set = mutableLongSetOf(1L, 2L)
        set -= mutableLongSetOf(3L, 5L)
        assertEquals(2, set.size)
        set -= mutableLongSetOf(3L, 1L, 5L)
        assertEquals(1, set.size)
        assertFalse(1L in set)
    }

    @Test
    fun insertManyEntries() {
        val set = MutableLongSet()

        for (i in 0 until 1700) {
            set += i.toLong()
        }

        assertEquals(1700, set.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val set = MutableLongSet()

            for (j in 0 until i) {
                set += j.toLong()
            }

            val elements = LongArray(i)
            var index = 0
            set.forEach { element -> elements[index++] = element }
            elements.sort()

            index = 0
            elements.forEach { element ->
                assertEquals(element, index.toLong())
                index++
            }
        }
    }

    @Test
    fun clear() {
        val set = MutableLongSet()

        for (i in 0 until 32) {
            set += i.toLong()
        }

        val capacity = set.capacity
        set.clear()

        assertEquals(0, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun string() {
        val set = MutableLongSet()
        assertEquals("[]", set.toString())

        set += 1L
        set += 5L
        assertTrue("[${1L}, ${5L}]" == set.toString() || "[${5L}, ${1L}]" == set.toString())
    }

    @Test
    fun joinToString() {
        val set = longSetOf(1L, 2L, 3L, 4L, 5L)
        val order = IntArray(5)
        var index = 0
        set.forEach { element -> order[index++] = element.toInt() }
        assertEquals(
            "${order[0].toLong()}, ${order[1].toLong()}, ${order[2].toLong()}, " +
                "${order[3].toLong()}, ${order[4].toLong()}",
            set.joinToString()
        )
        assertEquals(
            "x${order[0].toLong()}, ${order[1].toLong()}, ${order[2].toLong()}...",
            set.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toLong()}-${order[1].toLong()}-${order[2].toLong()}-" +
                "${order[3].toLong()}-${order[4].toLong()}<",
            set.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            set.joinToString(limit = 3) { names[it.toInt()] }
        )
    }

    @Test
    fun equals() {
        val set = MutableLongSet()
        set += 1L
        set += 5L

        assertFalse(set.equals(null))
        assertEquals(set, set)

        val set2 = MutableLongSet()
        set2 += 5L

        assertNotEquals(set, set2)

        set2 += 1L
        assertEquals(set, set2)
    }

    @Test
    fun contains() {
        val set = MutableLongSet()
        set += 1L
        set += 5L

        assertTrue(set.contains(1L))
        assertTrue(set.contains(5L))
        assertFalse(set.contains(2L))
    }

    @Test
    fun empty() {
        val set = MutableLongSet()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        assertTrue(set.none())
        assertFalse(set.any())

        set += 1L

        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
        assertTrue(set.any())
        assertFalse(set.none())
    }

    @Test
    fun count() {
        val set = MutableLongSet()
        assertEquals(0, set.count())

        set += 1L
        assertEquals(1, set.count())

        set += 5L
        set += 6L
        set += 9L
        set += 11L
        set += 13L

        assertEquals(2, set.count { it < 6L })
        assertEquals(0, set.count { it < 0L })
    }

    @Test
    fun any() {
        val set = MutableLongSet()
        set += 1L
        set += 5L
        set += 6L
        set += 9L
        set += 11L
        set += 13L

        assertTrue(set.any { it >= 11L })
        assertFalse(set.any { it < 0L })
    }

    @Test
    fun all() {
        val set = MutableLongSet()
        set += 1L
        set += 5L
        set += 6L
        set += 9L
        set += 11L
        set += 13L

        assertTrue(set.all { it > 0L })
        assertFalse(set.all { it < 0L })
    }

    @Test
    fun trim() {
        val set = mutableLongSetOf(1L, 2L, 3L, 4L, 5L, 7L)
        val capacity = set.capacity
        assertEquals(0, set.trim())
        set.clear()
        assertEquals(capacity, set.trim())
        assertEquals(0, set.capacity)
        set.addAll(longArrayOf(1L, 2L, 3L, 4L, 5L, 7L, 6L, 8L, 9L, 10L, 11L, 12L, 13L, 14L))
        set.removeAll(longArrayOf(6L, 8L, 9L, 10L, 11L, 12L, 13L, 14L))
        assertTrue(set.trim() > 0)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun longSetOfEmpty() {
        assertSame(emptyLongSet(), longSetOf())
        assertEquals(0, longSetOf().size)
    }

    @Test
    fun longSetOfOne() {
        val set = longSetOf(1L)
        assertEquals(1, set.size)
        assertEquals(1L, set.first())
    }

    @Test
    fun longSetOfTwo() {
        val set = longSetOf(1L, 2L)
        assertEquals(2, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertFalse(5L in set)
    }

    @Test
    fun longSetOfThree() {
        val set = longSetOf(1L, 2L, 3L)
        assertEquals(3, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertTrue(3L in set)
        assertFalse(5L in set)
    }

    @Test
    fun longSetOfFour() {
        val set = longSetOf(1L, 2L, 3L, 4L)
        assertEquals(4, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertTrue(3L in set)
        assertTrue(4L in set)
        assertFalse(5L in set)
    }

    @Test
    fun mutableLongSetOfOne() {
        val set = mutableLongSetOf(1L)
        assertEquals(1, set.size)
        assertEquals(1L, set.first())
    }

    @Test
    fun mutableLongSetOfTwo() {
        val set = mutableLongSetOf(1L, 2L)
        assertEquals(2, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertFalse(5L in set)
    }

    @Test
    fun mutableLongSetOfThree() {
        val set = mutableLongSetOf(1L, 2L, 3L)
        assertEquals(3, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertTrue(3L in set)
        assertFalse(5L in set)
    }

    @Test
    fun mutableLongSetOfFour() {
        val set = mutableLongSetOf(1L, 2L, 3L, 4L)
        assertEquals(4, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
        assertTrue(3L in set)
        assertTrue(4L in set)
        assertFalse(5L in set)
    }

    @Test
    fun buildLongSetFunction() {
        val contract: Boolean
        val set = buildLongSet {
            contract = true
            add(1L)
            add(2L)
        }
        assertTrue(contract)
        assertEquals(2, set.size)
        assertTrue(1L in set)
        assertTrue(2L in set)
    }

    @Test
    fun buildLongSetWithCapacityFunction() {
        val contract: Boolean
        val set =
            buildLongSet(20) {
                contract = true
                add(1L)
                add(2L)
            }
        assertTrue(contract)
        assertEquals(2, set.size)
        assertTrue(set.capacity >= 18)
        assertTrue(1L in set)
        assertTrue(2L in set)
    }

    @Test
    fun insertManyRemoveMany() {
        val set = mutableLongSetOf()

        for (i in 0..1000000) {
            set.add(i.toLong())
            set.remove(i.toLong())
            assertTrue(set.capacity < 16, "Set grew larger than 16 after step $i")
        }
    }
}
