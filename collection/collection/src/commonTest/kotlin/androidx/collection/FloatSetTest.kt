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

class FloatSetTest {
    @Test
    fun emptyFloatSetConstructor() {
        val set = MutableFloatSet()
        assertEquals(7, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun immutableEmptyFloatSet() {
        val set: FloatSet = emptyFloatSet()
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun zeroCapacityFloatSet() {
        val set = MutableFloatSet(0)
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun emptyFloatSetWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val set = MutableFloatSet(1800)
        assertEquals(4095, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun mutableFloatSetBuilder() {
        val empty = mutableFloatSetOf()
        assertEquals(0, empty.size)

        val withElements = mutableFloatSetOf(1f, 2f)
        assertEquals(2, withElements.size)
        assertTrue(1f in withElements)
        assertTrue(2f in withElements)
    }

    @Test
    fun addToFloatSet() {
        val set = MutableFloatSet()
        set += 1f
        assertTrue(set.add(2f))

        assertEquals(2, set.size)
        val elements = FloatArray(2)
        var index = 0
        set.forEach { element -> elements[index++] = element }
        elements.sort()
        assertEquals(1f, elements[0])
        assertEquals(2f, elements[1])
    }

    @Test
    fun addToSizedFloatSet() {
        val set = MutableFloatSet(12)
        set += 1f

        assertEquals(1, set.size)
        assertEquals(1f, set.first())
    }

    @Test
    fun addExistingElement() {
        val set = MutableFloatSet(12)
        set += 1f
        assertFalse(set.add(1f))
        set += 1f

        assertEquals(1, set.size)
        assertEquals(1f, set.first())
    }

    @Test
    fun addAllArray() {
        val set = mutableFloatSetOf(1f)
        assertFalse(set.addAll(floatArrayOf(1f)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(floatArrayOf(1f, 2f)))
        assertEquals(2, set.size)
        assertTrue(2f in set)
    }

    @Test
    fun addAllFloatSet() {
        val set = mutableFloatSetOf(1f)
        assertFalse(set.addAll(mutableFloatSetOf(1f)))
        assertEquals(1, set.size)
        assertTrue(set.addAll(mutableFloatSetOf(1f, 2f)))
        assertEquals(2, set.size)
        assertTrue(2f in set)
    }

    @Test
    fun plusAssignArray() {
        val set = mutableFloatSetOf(1f)
        set += floatArrayOf(1f)
        assertEquals(1, set.size)
        set += floatArrayOf(1f, 2f)
        assertEquals(2, set.size)
        assertTrue(2f in set)
    }

    @Test
    fun plusAssignFloatSet() {
        val set = mutableFloatSetOf(1f)
        set += mutableFloatSetOf(1f)
        assertEquals(1, set.size)
        set += mutableFloatSetOf(1f, 2f)
        assertEquals(2, set.size)
        assertTrue(2f in set)
    }

    @Test
    fun firstWithValue() {
        val set = MutableFloatSet()
        set += 1f
        set += 2f
        var element: Float = -1f
        var otherElement: Float = -1f
        set.forEach { if (element == -1f) element = it else otherElement = it }
        assertEquals(element, set.first())
        set -= element
        assertEquals(otherElement, set.first())
    }

    @Test
    fun firstEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableFloatSet()
            set.first()
        }
    }

    @Test
    fun firstMatching() {
        val set = MutableFloatSet()
        set += 1f
        set += 2f
        assertEquals(1f, set.first { it < 2f })
        assertEquals(2f, set.first { it > 1f })
    }

    @Test
    fun firstMatchingEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableFloatSet()
            set.first { it > 0f }
        }
    }

    @Test
    fun firstMatchingNoMatch() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableFloatSet()
            set += 1f
            set += 2f
            set.first { it < 0f }
        }
    }

    @Test
    fun remove() {
        val set = MutableFloatSet()
        assertFalse(set.remove(1f))

        set += 1f
        assertTrue(set.remove(1f))
        assertEquals(0, set.size)

        set += 1f
        set -= 1f
        assertEquals(0, set.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val set = MutableFloatSet(6)
        set += 1f
        set += 5f
        set += 6f
        set += 9f
        set += 11f
        set += 13f

        // Removing all the entries will mark the medata as deleted
        set.remove(1f)
        set.remove(5f)
        set.remove(6f)
        set.remove(9f)
        set.remove(11f)
        set.remove(13f)

        assertEquals(0, set.size)

        val capacity = set.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        set += 1f

        assertEquals(1, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun removeAllArray() {
        val set = mutableFloatSetOf(1f, 2f)
        assertFalse(set.removeAll(floatArrayOf(3f, 5f)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(floatArrayOf(3f, 1f, 5f)))
        assertEquals(1, set.size)
        assertFalse(1f in set)
    }

    @Test
    fun removeAllFloatSet() {
        val set = mutableFloatSetOf(1f, 2f)
        assertFalse(set.removeAll(mutableFloatSetOf(3f, 5f)))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(mutableFloatSetOf(3f, 1f, 5f)))
        assertEquals(1, set.size)
        assertFalse(1f in set)
    }

    @Test
    fun minusAssignArray() {
        val set = mutableFloatSetOf(1f, 2f)
        set -= floatArrayOf(3f, 5f)
        assertEquals(2, set.size)
        set -= floatArrayOf(3f, 1f, 5f)
        assertEquals(1, set.size)
        assertFalse(1f in set)
    }

    @Test
    fun minusAssignFloatSet() {
        val set = mutableFloatSetOf(1f, 2f)
        set -= mutableFloatSetOf(3f, 5f)
        assertEquals(2, set.size)
        set -= mutableFloatSetOf(3f, 1f, 5f)
        assertEquals(1, set.size)
        assertFalse(1f in set)
    }

    @Test
    fun insertManyEntries() {
        val set = MutableFloatSet()

        for (i in 0 until 1700) {
            set += i.toFloat()
        }

        assertEquals(1700, set.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val set = MutableFloatSet()

            for (j in 0 until i) {
                set += j.toFloat()
            }

            val elements = FloatArray(i)
            var index = 0
            set.forEach { element -> elements[index++] = element }
            elements.sort()

            index = 0
            elements.forEach { element ->
                assertEquals(element, index.toFloat())
                index++
            }
        }
    }

    @Test
    fun clear() {
        val set = MutableFloatSet()

        for (i in 0 until 32) {
            set += i.toFloat()
        }

        val capacity = set.capacity
        set.clear()

        assertEquals(0, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun string() {
        val set = MutableFloatSet()
        assertEquals("[]", set.toString())

        set += 1f
        set += 5f
        assertTrue("[${1f}, ${5f}]" == set.toString() || "[${5f}, ${1f}]" == set.toString())
    }

    @Test
    fun joinToString() {
        val set = floatSetOf(1f, 2f, 3f, 4f, 5f)
        val order = IntArray(5)
        var index = 0
        set.forEach { element -> order[index++] = element.toInt() }
        assertEquals(
            "${order[0].toFloat()}, ${order[1].toFloat()}, ${order[2].toFloat()}, " +
                "${order[3].toFloat()}, ${order[4].toFloat()}",
            set.joinToString()
        )
        assertEquals(
            "x${order[0].toFloat()}, ${order[1].toFloat()}, ${order[2].toFloat()}...",
            set.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toFloat()}-${order[1].toFloat()}-${order[2].toFloat()}-" +
                "${order[3].toFloat()}-${order[4].toFloat()}<",
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
        val set = MutableFloatSet()
        set += 1f
        set += 5f

        assertFalse(set.equals(null))
        assertEquals(set, set)

        val set2 = MutableFloatSet()
        set2 += 5f

        assertNotEquals(set, set2)

        set2 += 1f
        assertEquals(set, set2)
    }

    @Test
    fun contains() {
        val set = MutableFloatSet()
        set += 1f
        set += 5f

        assertTrue(set.contains(1f))
        assertTrue(set.contains(5f))
        assertFalse(set.contains(2f))
    }

    @Test
    fun empty() {
        val set = MutableFloatSet()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        assertTrue(set.none())
        assertFalse(set.any())

        set += 1f

        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
        assertTrue(set.any())
        assertFalse(set.none())
    }

    @Test
    fun count() {
        val set = MutableFloatSet()
        assertEquals(0, set.count())

        set += 1f
        assertEquals(1, set.count())

        set += 5f
        set += 6f
        set += 9f
        set += 11f
        set += 13f

        assertEquals(2, set.count { it < 6f })
        assertEquals(0, set.count { it < 0f })
    }

    @Test
    fun any() {
        val set = MutableFloatSet()
        set += 1f
        set += 5f
        set += 6f
        set += 9f
        set += 11f
        set += 13f

        assertTrue(set.any { it >= 11f })
        assertFalse(set.any { it < 0f })
    }

    @Test
    fun all() {
        val set = MutableFloatSet()
        set += 1f
        set += 5f
        set += 6f
        set += 9f
        set += 11f
        set += 13f

        assertTrue(set.all { it > 0f })
        assertFalse(set.all { it < 0f })
    }

    @Test
    fun trim() {
        val set = mutableFloatSetOf(1f, 2f, 3f, 4f, 5f, 7f)
        val capacity = set.capacity
        assertEquals(0, set.trim())
        set.clear()
        assertEquals(capacity, set.trim())
        assertEquals(0, set.capacity)
        set.addAll(floatArrayOf(1f, 2f, 3f, 4f, 5f, 7f, 6f, 8f, 9f, 10f, 11f, 12f, 13f, 14f))
        set.removeAll(floatArrayOf(6f, 8f, 9f, 10f, 11f, 12f, 13f, 14f))
        assertTrue(set.trim() > 0)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun floatSetOfEmpty() {
        assertSame(emptyFloatSet(), floatSetOf())
        assertEquals(0, floatSetOf().size)
    }

    @Test
    fun floatSetOfOne() {
        val set = floatSetOf(1f)
        assertEquals(1, set.size)
        assertEquals(1f, set.first())
    }

    @Test
    fun floatSetOfTwo() {
        val set = floatSetOf(1f, 2f)
        assertEquals(2, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertFalse(5f in set)
    }

    @Test
    fun floatSetOfThree() {
        val set = floatSetOf(1f, 2f, 3f)
        assertEquals(3, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertTrue(3f in set)
        assertFalse(5f in set)
    }

    @Test
    fun floatSetOfFour() {
        val set = floatSetOf(1f, 2f, 3f, 4f)
        assertEquals(4, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertTrue(3f in set)
        assertTrue(4f in set)
        assertFalse(5f in set)
    }

    @Test
    fun mutableFloatSetOfOne() {
        val set = mutableFloatSetOf(1f)
        assertEquals(1, set.size)
        assertEquals(1f, set.first())
    }

    @Test
    fun mutableFloatSetOfTwo() {
        val set = mutableFloatSetOf(1f, 2f)
        assertEquals(2, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertFalse(5f in set)
    }

    @Test
    fun mutableFloatSetOfThree() {
        val set = mutableFloatSetOf(1f, 2f, 3f)
        assertEquals(3, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertTrue(3f in set)
        assertFalse(5f in set)
    }

    @Test
    fun mutableFloatSetOfFour() {
        val set = mutableFloatSetOf(1f, 2f, 3f, 4f)
        assertEquals(4, set.size)
        assertTrue(1f in set)
        assertTrue(2f in set)
        assertTrue(3f in set)
        assertTrue(4f in set)
        assertFalse(5f in set)
    }

    @Test
    fun insertManyRemoveMany() {
        val set = mutableFloatSetOf()

        for (i in 0..1000000) {
            set.add(i.toFloat())
            set.remove(i.toFloat())
            assertTrue(set.capacity < 16, "Set grew larger than 16 after step $i")
        }
    }
}
