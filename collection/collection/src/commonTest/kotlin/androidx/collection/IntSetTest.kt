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

class IntSetTest {
    @Test
    fun emptyIntSetConstructor() {
        val set = MutableIntSet()
        assertEquals(7, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun immutableEmptyIntSet() {
        val set: IntSet = emptyIntSet()
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun zeroCapacityIntSet() {
        val set = MutableIntSet(0)
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun emptyIntSetWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val set = MutableIntSet(1800)
        assertEquals(4095, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun mutableIntSetBuilder() {
        val empty = mutableIntSetOf()
        assertEquals(0, empty.size)

        val withElements = mutableIntSetOf(1, 2)
        assertEquals(2, withElements.size)
        assertTrue(1 in withElements)
        assertTrue(2 in withElements)
    }

    @Test
    fun addToIntSet() {
        val set = MutableIntSet()
        set += 1
        assertTrue(set.add(2))

        assertEquals(2, set.size)
        val elements = IntArray(2)
        var index = 0
        set.forEach { element -> elements[index++] = element }
        elements.sort()
        assertEquals(1, elements[0])
        assertEquals(2, elements[1])
    }

    @Test
    fun addToSizedIntSet() {
        val set = MutableIntSet(12)
        set += 1

        assertEquals(1, set.size)
        assertEquals(1, set.first())
    }

    @Test
    fun addExistingElement() {
        val set = MutableIntSet(12)
        set += 1
        assertFalse(set.add(1))
        set += 1

        assertEquals(1, set.size)
        assertEquals(1, set.first())
    }

    @Test
    fun addAllArray() {
        val set = mutableIntSetOf(1)
        assertFalse(set.addAll(intArrayOf(1)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(intArrayOf(1, 2)))
        assertEquals(2, set.size)
        assertTrue(2 in set)
    }

    @Test
    fun addAllIntSet() {
        val set = mutableIntSetOf(1)
        assertFalse(set.addAll(mutableIntSetOf(1)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(mutableIntSetOf(1, 2)))
        assertEquals(2, set.size)
        assertTrue(2 in set)
    }

    @Test
    fun plusAssignArray() {
        val set = mutableIntSetOf(1)
        set += intArrayOf(1)
        assertEquals(1, set.size)
        set += intArrayOf(1, 2)
        assertEquals(2, set.size)
        assertTrue(2 in set)
    }

    @Test
    fun plusAssignIntSet() {
        val set = mutableIntSetOf(1)
        set += mutableIntSetOf(1)
        assertEquals(1, set.size)
        set += mutableIntSetOf(1, 2)
        assertEquals(2, set.size)
        assertTrue(2 in set)
    }

    @Test
    fun firstWithValue() {
        val set = MutableIntSet()
        set += 1
        set += 2
        var element: Int = -1
        var otherElement: Int = -1
        set.forEach { if (element == -1) element = it else otherElement = it }
        assertEquals(element, set.first())
        set -= element
        assertEquals(otherElement, set.first())
    }

    @Test
    fun firstEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableIntSet()
            set.first()
        }
    }

    @Test
    fun firstMatching() {
        val set = MutableIntSet()
        set += 1
        set += 2
        assertEquals(1, set.first { it < 2 })
        assertEquals(2, set.first { it > 1 })
    }

    @Test
    fun firstMatchingEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableIntSet()
            set.first { it > 0 }
        }
    }

    @Test
    fun firstMatchingNoMatch() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableIntSet()
            set += 1
            set += 2
            set.first { it < 0 }
        }
    }

    @Test
    fun remove() {
        val set = MutableIntSet()
        assertFalse(set.remove(1))

        set += 1
        assertTrue(set.remove(1))
        assertEquals(0, set.size)

        set += 1
        set -= 1
        assertEquals(0, set.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val set = MutableIntSet(6)
        set += 1
        set += 5
        set += 6
        set += 9
        set += 11
        set += 13

        // Removing all the entries will mark the medata as deleted
        set.remove(1)
        set.remove(5)
        set.remove(6)
        set.remove(9)
        set.remove(11)
        set.remove(13)

        assertEquals(0, set.size)

        val capacity = set.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        set += 1

        assertEquals(1, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun removeAllArray() {
        val set = mutableIntSetOf(1, 2)
        assertFalse(set.removeAll(intArrayOf(3, 5)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(intArrayOf(3, 1, 5)))
        assertEquals(1, set.size)
        assertFalse(1 in set)
    }

    @Test
    fun removeAllIntSet() {
        val set = mutableIntSetOf(1, 2)
        assertFalse(set.removeAll(mutableIntSetOf(3, 5)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(mutableIntSetOf(3, 1, 5)))
        assertEquals(1, set.size)
        assertFalse(1 in set)
    }

    @Test
    fun minusAssignArray() {
        val set = mutableIntSetOf(1, 2)
        set -= intArrayOf(3, 5)
        assertEquals(2, set.size)
        set -= intArrayOf(3, 1, 5)
        assertEquals(1, set.size)
        assertFalse(1 in set)
    }

    @Test
    fun minusAssignIntSet() {
        val set = mutableIntSetOf(1, 2)
        set -= mutableIntSetOf(3, 5)
        assertEquals(2, set.size)
        set -= mutableIntSetOf(3, 1, 5)
        assertEquals(1, set.size)
        assertFalse(1 in set)
    }

    @Test
    fun insertManyEntries() {
        val set = MutableIntSet()

        for (i in 0 until 1700) {
            set += i.toInt()
        }

        assertEquals(1700, set.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val set = MutableIntSet()

            for (j in 0 until i) {
                set += j.toInt()
            }

            val elements = IntArray(i)
            var index = 0
            set.forEach { element -> elements[index++] = element }
            elements.sort()

            index = 0
            elements.forEach { element ->
                assertEquals(element, index.toInt())
                index++
            }
        }
    }

    @Test
    fun clear() {
        val set = MutableIntSet()

        for (i in 0 until 32) {
            set += i.toInt()
        }

        val capacity = set.capacity
        set.clear()

        assertEquals(0, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun string() {
        val set = MutableIntSet()
        assertEquals("[]", set.toString())

        set += 1
        set += 5
        assertTrue("[${1}, ${5}]" == set.toString() || "[${5}, ${1}]" == set.toString())
    }

    @Test
    fun joinToString() {
        val set = intSetOf(1, 2, 3, 4, 5)
        val order = IntArray(5)
        var index = 0
        set.forEach { element -> order[index++] = element.toInt() }
        assertEquals(
            "${order[0].toInt()}, ${order[1].toInt()}, ${order[2].toInt()}, " +
                "${order[3].toInt()}, ${order[4].toInt()}",
            set.joinToString()
        )
        assertEquals(
            "x${order[0].toInt()}, ${order[1].toInt()}, ${order[2].toInt()}...",
            set.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toInt()}-${order[1].toInt()}-${order[2].toInt()}-" +
                "${order[3].toInt()}-${order[4].toInt()}<",
            set.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            set.joinToString(limit = 3) { names[it.toInt()] }
        )
    }

    @Test
    fun equalsTest() {
        val set = MutableIntSet()
        set += 1
        set += 5

        assertFalse(set.equals(null))
        assertEquals(set, set)

        val set2 = MutableIntSet()
        set2 += 5

        assertNotEquals(set, set2)

        set2 += 1
        assertEquals(set, set2)
    }

    @Test
    fun contains() {
        val set = MutableIntSet()
        set += 1
        set += 5

        assertTrue(set.contains(1))
        assertTrue(set.contains(5))
        assertFalse(set.contains(2))
    }

    @Test
    fun empty() {
        val set = MutableIntSet()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        assertTrue(set.none())
        assertFalse(set.any())

        set += 1

        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
        assertTrue(set.any())
        assertFalse(set.none())
    }

    @Test
    fun count() {
        val set = MutableIntSet()
        assertEquals(0, set.count())

        set += 1
        assertEquals(1, set.count())

        set += 5
        set += 6
        set += 9
        set += 11
        set += 13

        assertEquals(2, set.count { it < 6 })
        assertEquals(0, set.count { it < 0 })
    }

    @Test
    fun any() {
        val set = MutableIntSet()
        set += 1
        set += 5
        set += 6
        set += 9
        set += 11
        set += 13

        assertTrue(set.any { it >= 11 })
        assertFalse(set.any { it < 0 })
    }

    @Test
    fun all() {
        val set = MutableIntSet()
        set += 1
        set += 5
        set += 6
        set += 9
        set += 11
        set += 13

        assertTrue(set.all { it > 0 })
        assertFalse(set.all { it < 0 })
    }

    @Test
    fun trim() {
        val set = mutableIntSetOf(1, 2, 3, 4, 5, 7)
        val capacity = set.capacity
        assertEquals(0, set.trim())
        set.clear()
        assertEquals(capacity, set.trim())
        assertEquals(0, set.capacity)
        set.addAll(intArrayOf(1, 2, 3, 4, 5, 7, 6, 8, 9, 10, 11, 12, 13, 14))
        set.removeAll(intArrayOf(6, 8, 9, 10, 11, 12, 13, 14))
        assertTrue(set.trim() > 0)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun intSetOfEmpty() {
        assertSame(emptyIntSet(), intSetOf())
        assertEquals(0, intSetOf().size)
    }

    @Test
    fun intSetOfOne() {
        val set = intSetOf(1)
        assertEquals(1, set.size)
        assertEquals(1, set.first())
    }

    @Test
    fun intSetOfTwo() {
        val set = intSetOf(1, 2)
        assertEquals(2, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertFalse(5 in set)
    }

    @Test
    fun intSetOfThree() {
        val set = intSetOf(1, 2, 3)
        assertEquals(3, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertTrue(3 in set)
        assertFalse(5 in set)
    }

    @Test
    fun intSetOfFour() {
        val set = intSetOf(1, 2, 3, 4)
        assertEquals(4, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertTrue(3 in set)
        assertTrue(4 in set)
        assertFalse(5 in set)
    }

    @Test
    fun mutableIntSetOfOne() {
        val set = mutableIntSetOf(1)
        assertEquals(1, set.size)
        assertEquals(1, set.first())
    }

    @Test
    fun mutableIntSetOfTwo() {
        val set = mutableIntSetOf(1, 2)
        assertEquals(2, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertFalse(5 in set)
    }

    @Test
    fun mutableIntSetOfThree() {
        val set = mutableIntSetOf(1, 2, 3)
        assertEquals(3, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertTrue(3 in set)
        assertFalse(5 in set)
    }

    @Test
    fun mutableIntSetOfFour() {
        val set = mutableIntSetOf(1, 2, 3, 4)
        assertEquals(4, set.size)
        assertTrue(1 in set)
        assertTrue(2 in set)
        assertTrue(3 in set)
        assertTrue(4 in set)
        assertFalse(5 in set)
    }

    @Test
    fun insertManyRemoveMany() {
        val set = mutableIntSetOf()

        for (i in 0..1000000) {
            set.add(i.toInt())
            set.remove(i.toInt())
            assertTrue(set.capacity < 16, "Set grew larger than 16 after step $i")
        }
    }
}
