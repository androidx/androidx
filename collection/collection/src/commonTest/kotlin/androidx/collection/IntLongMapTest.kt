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
//
// Note that there are 3 templates for maps, one for object-to-primitive, one
// for primitive-to-object and one for primitive-to-primitive. Also, the
// object-to-object is ScatterMap.kt, which doesn't have a template.
// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

@Suppress("RemoveRedundantCallsOfConversionMethods")
class IntLongMapTest {
    @Test
    fun intLongMap() {
        val map = MutableIntLongMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyIntLongMap() {
        val map = emptyIntLongMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyIntLongMap(), map)
    }

    @Test
    fun intLongMapFunction() {
        val map = mutableIntLongMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableIntLongMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun intLongMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableIntLongMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun intLongMapInitFunction() {
        val map1 =
            intLongMapOf(
                1,
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1[1])

        val map2 =
            intLongMapOf(
                1,
                1L,
                2,
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2[1])
        assertEquals(2L, map2[2])

        val map3 =
            intLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3[1])
        assertEquals(2L, map3[2])
        assertEquals(3L, map3[3])

        val map4 =
            intLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
                4,
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4[1])
        assertEquals(2L, map4[2])
        assertEquals(3L, map4[3])
        assertEquals(4L, map4[4])

        val map5 =
            intLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
                4,
                4L,
                5,
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5[1])
        assertEquals(2L, map5[2])
        assertEquals(3L, map5[3])
        assertEquals(4L, map5[4])
        assertEquals(5L, map5[5])
    }

    @Test
    fun mutableIntLongMapInitFunction() {
        val map1 =
            mutableIntLongMapOf(
                1,
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1[1])

        val map2 =
            mutableIntLongMapOf(
                1,
                1L,
                2,
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2[1])
        assertEquals(2L, map2[2])

        val map3 =
            mutableIntLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3[1])
        assertEquals(2L, map3[2])
        assertEquals(3L, map3[3])

        val map4 =
            mutableIntLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
                4,
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4[1])
        assertEquals(2L, map4[2])
        assertEquals(3L, map4[3])
        assertEquals(4L, map4[4])

        val map5 =
            mutableIntLongMapOf(
                1,
                1L,
                2,
                2L,
                3,
                3L,
                4,
                4L,
                5,
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5[1])
        assertEquals(2L, map5[2])
        assertEquals(3L, map5[3])
        assertEquals(4L, map5[4])
        assertEquals(5L, map5[5])
    }

    @Test
    fun addToMap() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableIntLongMap(12)
        map[1] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableIntLongMap(2)
        map[1] = 1L

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1L, map[1])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableIntLongMap(0)
        map[1] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[1] = 2L

        assertEquals(1, map.size)
        assertEquals(2L, map[1])
    }

    @Test
    fun put() {
        val map = MutableIntLongMap()

        map.put(1, 1L)
        assertEquals(1L, map[1])
        map.put(1, 2L)
        assertEquals(2L, map[1])
    }

    @Test
    fun putWithDefault() {
        val map = MutableIntLongMap()

        var previous = map.put(1, 1L, -1L)
        assertEquals(1L, map[1])
        assertEquals(-1L, previous)

        previous = map.put(1, 2L, -1L)
        assertEquals(2L, map[1])
        assertEquals(1L, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertFailsWith<NoSuchElementException> { map[2] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertEquals(2L, map.getOrDefault(2, 2L))
    }

    @Test
    fun getOrElse() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertEquals(3L, map.getOrElse(3) { 3L })
    }

    @Test
    fun getOrPut() {
        val map = MutableIntLongMap()
        map[1] = 1L

        var counter = 0
        map.getOrPut(1) {
            counter++
            2L
        }
        assertEquals(1L, map[1])
        assertEquals(0, counter)

        map.getOrPut(2) {
            counter++
            2L
        }
        assertEquals(2L, map[2])
        assertEquals(1, counter)

        map.getOrPut(2) {
            counter++
            3L
        }
        assertEquals(2L, map[2])
        assertEquals(1, counter)

        map.getOrPut(3) {
            counter++
            3L
        }
        assertEquals(3L, map[3])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableIntLongMap()
        map.remove(1)

        map[1] = 1L
        map.remove(1)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableIntLongMap(6)
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L
        map[4] = 4L
        map[5] = 5L
        map[6] = 6L

        // Removing all the entries will mark the medata as deleted
        map.remove(1)
        map.remove(2)
        map.remove(3)
        map.remove(4)
        map.remove(5)
        map.remove(6)

        assertEquals(0, map.size)

        val capacity = map.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        map[1] = 7L

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L
        map[4] = 4L
        map[5] = 5L
        map[6] = 6L

        map.removeIf { key, _ -> key == 1 || key == 3 }

        assertEquals(4, map.size)
        assertEquals(2L, map[2])
        assertEquals(4L, map[4])
        assertEquals(5L, map[5])
        assertEquals(6L, map[6])
    }

    @Test
    fun minus() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L

        map -= 1

        assertEquals(2, map.size)
        assertFalse(1 in map)
    }

    @Test
    fun minusArray() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L

        map -= intArrayOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun minusSet() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L

        map -= intSetOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun minusList() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L

        map -= intListOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableIntLongMap()
        assertFalse(map.remove(1, 1L))

        map[1] = 1L
        assertTrue(map.remove(1, 1L))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableIntLongMap()

        for (i in 0 until 1700) {
            map[i.toInt()] = i.toLong()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableIntLongMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toLong()
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value.toInt())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableIntLongMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toLong()
            }

            var counter = 0
            val keys = BooleanArray(map.size)
            map.forEachKey { key ->
                keys[key.toInt()] = true
                counter++
            }

            assertEquals(i, counter)
            keys.forEach { assertTrue(it) }
        }
    }

    @Test
    fun forEachValue() {
        for (i in 0..48) {
            val map = MutableIntLongMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toLong()
            }

            var counter = 0
            val values = BooleanArray(map.size)
            map.forEachValue { value ->
                values[value.toInt()] = true
                counter++
            }

            assertEquals(i, counter)
            values.forEach { assertTrue(it) }
        }
    }

    @Test
    fun clear() {
        val map = MutableIntLongMap()

        for (i in 0 until 32) {
            map[i.toInt()] = i.toLong()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableIntLongMap()
        assertEquals("{}", map.toString())

        map[1] = 1L
        map[2] = 2L
        val oneValueString = 1L.toString()
        val twoValueString = 2L.toString()
        val oneKeyString = 1.toString()
        val twoKeyString = 2.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableIntLongMap()
        repeat(5) { map[it.toInt()] = it.toLong() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toInt()}=${order[0].toLong()}, ${order[1].toInt()}=" +
                "${order[1].toLong()}, ${order[2].toInt()}=${order[2].toLong()}," +
                " ${order[3].toInt()}=${order[3].toLong()}, ${order[4].toInt()}=" +
                "${order[4].toLong()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toInt()}=${order[0].toLong()}, ${order[1].toInt()}=" +
                "${order[1].toLong()}, ${order[2].toInt()}=${order[2].toLong()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toInt()}=${order[0].toLong()}-${order[1].toInt()}=" +
                "${order[1].toLong()}-${order[2].toInt()}=${order[2].toLong()}-" +
                "${order[3].toInt()}=${order[3].toLong()}-${order[4].toInt()}=" +
                "${order[4].toLong()}<",
            map.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            map.joinToString(limit = 3) { key, _ -> names[key.toInt()] }
        )
    }

    @Test
    fun equalsTest() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableIntLongMap()
        assertNotEquals(map, map2)

        map2[1] = 1L
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertTrue(map.containsKey(1))
        assertFalse(map.containsKey(2))
    }

    @Test
    fun contains() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertTrue(1 in map)
        assertFalse(2 in map)
    }

    @Test
    fun containsValue() {
        val map = MutableIntLongMap()
        map[1] = 1L

        assertTrue(map.containsValue(1L))
        assertFalse(map.containsValue(3L))
    }

    @Test
    fun empty() {
        val map = MutableIntLongMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1] = 1L

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableIntLongMap()
        assertEquals(0, map.count())

        map[1] = 1L
        assertEquals(1, map.count())

        map[2] = 2L
        map[3] = 3L
        map[4] = 4L
        map[5] = 5L
        map[6] = 6L

        assertEquals(2, map.count { key, _ -> key <= 2 })
        assertEquals(0, map.count { key, _ -> key < 0 })
    }

    @Test
    fun any() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L
        map[4] = 4L
        map[5] = 5L
        map[6] = 6L

        assertTrue(map.any { key, _ -> key == 4 })
        assertFalse(map.any { key, _ -> key < 0 })
    }

    @Test
    fun all() {
        val map = MutableIntLongMap()
        map[1] = 1L
        map[2] = 2L
        map[3] = 3L
        map[4] = 4L
        map[5] = 5L
        map[6] = 6L

        assertTrue(map.all { key, value -> key > 0 && value >= 1L })
        assertFalse(map.all { key, _ -> key < 6 })
    }

    @Test
    fun trim() {
        val map = MutableIntLongMap()
        assertEquals(7, map.trim())

        map[1] = 1L
        map[3] = 3L

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toInt()] = i.toLong()
        }

        assertEquals(2047, map.capacity)

        // After removing these items, our capacity needs should go
        // from 2047 down to 1023
        for (i in 0 until 1700) {
            if (i and 0x1 == 0x0) {
                val s = i.toInt()
                map.remove(s)
            }
        }

        assertEquals(1024, map.trim())
        assertEquals(0, map.trim())
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableIntLongMap()

        for (i in 0..1000000) {
            map[i.toInt()] = i.toLong()
            map.remove(i.toInt())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
