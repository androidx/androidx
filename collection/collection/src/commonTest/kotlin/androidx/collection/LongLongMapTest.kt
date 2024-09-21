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
class LongLongMapTest {
    @Test
    fun longLongMap() {
        val map = MutableLongLongMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyLongLongMap() {
        val map = emptyLongLongMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyLongLongMap(), map)
    }

    @Test
    fun longLongMapFunction() {
        val map = mutableLongLongMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableLongLongMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longLongMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableLongLongMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longLongMapInitFunction() {
        val map1 =
            longLongMapOf(
                1L,
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1[1L])

        val map2 =
            longLongMapOf(
                1L,
                1L,
                2L,
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2[1L])
        assertEquals(2L, map2[2L])

        val map3 =
            longLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3[1L])
        assertEquals(2L, map3[2L])
        assertEquals(3L, map3[3L])

        val map4 =
            longLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
                4L,
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4[1L])
        assertEquals(2L, map4[2L])
        assertEquals(3L, map4[3L])
        assertEquals(4L, map4[4L])

        val map5 =
            longLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
                4L,
                4L,
                5L,
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5[1L])
        assertEquals(2L, map5[2L])
        assertEquals(3L, map5[3L])
        assertEquals(4L, map5[4L])
        assertEquals(5L, map5[5L])
    }

    @Test
    fun mutableLongLongMapInitFunction() {
        val map1 =
            mutableLongLongMapOf(
                1L,
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1[1L])

        val map2 =
            mutableLongLongMapOf(
                1L,
                1L,
                2L,
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2[1L])
        assertEquals(2L, map2[2L])

        val map3 =
            mutableLongLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3[1L])
        assertEquals(2L, map3[2L])
        assertEquals(3L, map3[3L])

        val map4 =
            mutableLongLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
                4L,
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4[1L])
        assertEquals(2L, map4[2L])
        assertEquals(3L, map4[3L])
        assertEquals(4L, map4[4L])

        val map5 =
            mutableLongLongMapOf(
                1L,
                1L,
                2L,
                2L,
                3L,
                3L,
                4L,
                4L,
                5L,
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5[1L])
        assertEquals(2L, map5[2L])
        assertEquals(3L, map5[3L])
        assertEquals(4L, map5[4L])
        assertEquals(5L, map5[5L])
    }

    @Test
    fun buildLongLongMapFunction() {
        val contract: Boolean
        val map = buildLongLongMap {
            contract = true
            put(1L, 1L)
            put(2L, 2L)
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals(1L, map[1L])
        assertEquals(2L, map[2L])
    }

    @Test
    fun buildLongObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildLongLongMap(20) {
                contract = true
                put(1L, 1L)
                put(2L, 2L)
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals(1L, map[1L])
        assertEquals(2L, map[2L])
    }

    @Test
    fun addToMap() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1L])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableLongLongMap(12)
        map[1L] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1L])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableLongLongMap(2)
        map[1L] = 1L

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1L, map[1L])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableLongLongMap(0)
        map[1L] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[1L])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[1L] = 2L

        assertEquals(1, map.size)
        assertEquals(2L, map[1L])
    }

    @Test
    fun put() {
        val map = MutableLongLongMap()

        map.put(1L, 1L)
        assertEquals(1L, map[1L])
        map.put(1L, 2L)
        assertEquals(2L, map[1L])
    }

    @Test
    fun putWithDefault() {
        val map = MutableLongLongMap()

        var previous = map.put(1L, 1L, -1L)
        assertEquals(1L, map[1L])
        assertEquals(-1L, previous)

        previous = map.put(1L, 2L, -1L)
        assertEquals(2L, map[1L])
        assertEquals(1L, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertFailsWith<NoSuchElementException> { map[2L] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertEquals(2L, map.getOrDefault(2L, 2L))
    }

    @Test
    fun getOrElse() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertEquals(3L, map.getOrElse(3L) { 3L })
    }

    @Test
    fun getOrPut() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        var counter = 0
        map.getOrPut(1L) {
            counter++
            2L
        }
        assertEquals(1L, map[1L])
        assertEquals(0, counter)

        map.getOrPut(2L) {
            counter++
            2L
        }
        assertEquals(2L, map[2L])
        assertEquals(1, counter)

        map.getOrPut(2L) {
            counter++
            3L
        }
        assertEquals(2L, map[2L])
        assertEquals(1, counter)

        map.getOrPut(3L) {
            counter++
            3L
        }
        assertEquals(3L, map[3L])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableLongLongMap()
        map.remove(1L)

        map[1L] = 1L
        map.remove(1L)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableLongLongMap(6)
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L
        map[4L] = 4L
        map[5L] = 5L
        map[6L] = 6L

        // Removing all the entries will mark the medata as deleted
        map.remove(1L)
        map.remove(2L)
        map.remove(3L)
        map.remove(4L)
        map.remove(5L)
        map.remove(6L)

        assertEquals(0, map.size)

        val capacity = map.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        map[1L] = 7L

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L
        map[4L] = 4L
        map[5L] = 5L
        map[6L] = 6L

        map.removeIf { key, _ -> key == 1L || key == 3L }

        assertEquals(4, map.size)
        assertEquals(2L, map[2L])
        assertEquals(4L, map[4L])
        assertEquals(5L, map[5L])
        assertEquals(6L, map[6L])
    }

    @Test
    fun minus() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L

        map -= 1L

        assertEquals(2, map.size)
        assertFalse(1L in map)
    }

    @Test
    fun minusArray() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L

        map -= longArrayOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusSet() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L

        map -= longSetOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusList() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L

        map -= longListOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableLongLongMap()
        assertFalse(map.remove(1L, 1L))

        map[1L] = 1L
        assertTrue(map.remove(1L, 1L))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableLongLongMap()

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toLong()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableLongLongMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toLong()
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value.toLong())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableLongLongMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toLong()
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
            val map = MutableLongLongMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toLong()
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
        val map = MutableLongLongMap()

        for (i in 0 until 32) {
            map[i.toLong()] = i.toLong()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableLongLongMap()
        assertEquals("{}", map.toString())

        map[1L] = 1L
        map[2L] = 2L
        val oneValueString = 1L.toString()
        val twoValueString = 2L.toString()
        val oneKeyString = 1L.toString()
        val twoKeyString = 2L.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableLongLongMap()
        repeat(5) { map[it.toLong()] = it.toLong() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toLong()}=${order[0].toLong()}, ${order[1].toLong()}=" +
                "${order[1].toLong()}, ${order[2].toLong()}=${order[2].toLong()}," +
                " ${order[3].toLong()}=${order[3].toLong()}, ${order[4].toLong()}=" +
                "${order[4].toLong()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toLong()}=${order[0].toLong()}, ${order[1].toLong()}=" +
                "${order[1].toLong()}, ${order[2].toLong()}=${order[2].toLong()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toLong()}=${order[0].toLong()}-${order[1].toLong()}=" +
                "${order[1].toLong()}-${order[2].toLong()}=${order[2].toLong()}-" +
                "${order[3].toLong()}=${order[3].toLong()}-${order[4].toLong()}=" +
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
    fun equals() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableLongLongMap()
        assertNotEquals(map, map2)

        map2[1L] = 1L
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertTrue(map.containsKey(1L))
        assertFalse(map.containsKey(2L))
    }

    @Test
    fun contains() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertTrue(1L in map)
        assertFalse(2L in map)
    }

    @Test
    fun containsValue() {
        val map = MutableLongLongMap()
        map[1L] = 1L

        assertTrue(map.containsValue(1L))
        assertFalse(map.containsValue(3L))
    }

    @Test
    fun empty() {
        val map = MutableLongLongMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1L] = 1L

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableLongLongMap()
        assertEquals(0, map.count())

        map[1L] = 1L
        assertEquals(1, map.count())

        map[2L] = 2L
        map[3L] = 3L
        map[4L] = 4L
        map[5L] = 5L
        map[6L] = 6L

        assertEquals(2, map.count { key, _ -> key <= 2L })
        assertEquals(0, map.count { key, _ -> key < 0L })
    }

    @Test
    fun any() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L
        map[4L] = 4L
        map[5L] = 5L
        map[6L] = 6L

        assertTrue(map.any { key, _ -> key == 4L })
        assertFalse(map.any { key, _ -> key < 0L })
    }

    @Test
    fun all() {
        val map = MutableLongLongMap()
        map[1L] = 1L
        map[2L] = 2L
        map[3L] = 3L
        map[4L] = 4L
        map[5L] = 5L
        map[6L] = 6L

        assertTrue(map.all { key, value -> key > 0L && value >= 1L })
        assertFalse(map.all { key, _ -> key < 6L })
    }

    @Test
    fun trim() {
        val map = MutableLongLongMap()
        assertEquals(7, map.trim())

        map[1L] = 1L
        map[3L] = 3L

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toLong()
        }

        assertEquals(2047, map.capacity)

        // After removing these items, our capacity needs should go
        // from 2047 down to 1023
        for (i in 0 until 1700) {
            if (i and 0x1 == 0x0) {
                val s = i.toLong()
                map.remove(s)
            }
        }

        assertEquals(1024, map.trim())
        assertEquals(0, map.trim())
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableLongLongMap()

        for (i in 0..1000000) {
            map[i.toLong()] = i.toLong()
            map.remove(i.toLong())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
