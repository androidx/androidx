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
class LongIntMapTest {
    @Test
    fun longIntMap() {
        val map = MutableLongIntMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyLongIntMap() {
        val map = emptyLongIntMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyLongIntMap(), map)
    }

    @Test
    fun longIntMapFunction() {
        val map = mutableLongIntMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableLongIntMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longIntMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableLongIntMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longIntMapInitFunction() {
        val map1 =
            longIntMapOf(
                1L,
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1[1L])

        val map2 =
            longIntMapOf(
                1L,
                1,
                2L,
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2[1L])
        assertEquals(2, map2[2L])

        val map3 =
            longIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3[1L])
        assertEquals(2, map3[2L])
        assertEquals(3, map3[3L])

        val map4 =
            longIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
                4L,
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4[1L])
        assertEquals(2, map4[2L])
        assertEquals(3, map4[3L])
        assertEquals(4, map4[4L])

        val map5 =
            longIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
                4L,
                4,
                5L,
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5[1L])
        assertEquals(2, map5[2L])
        assertEquals(3, map5[3L])
        assertEquals(4, map5[4L])
        assertEquals(5, map5[5L])
    }

    @Test
    fun mutableLongIntMapInitFunction() {
        val map1 =
            mutableLongIntMapOf(
                1L,
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1[1L])

        val map2 =
            mutableLongIntMapOf(
                1L,
                1,
                2L,
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2[1L])
        assertEquals(2, map2[2L])

        val map3 =
            mutableLongIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3[1L])
        assertEquals(2, map3[2L])
        assertEquals(3, map3[3L])

        val map4 =
            mutableLongIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
                4L,
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4[1L])
        assertEquals(2, map4[2L])
        assertEquals(3, map4[3L])
        assertEquals(4, map4[4L])

        val map5 =
            mutableLongIntMapOf(
                1L,
                1,
                2L,
                2,
                3L,
                3,
                4L,
                4,
                5L,
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5[1L])
        assertEquals(2, map5[2L])
        assertEquals(3, map5[3L])
        assertEquals(4, map5[4L])
        assertEquals(5, map5[5L])
    }

    @Test
    fun buildLongIntMapFunction() {
        val contract: Boolean
        val map = buildLongIntMap {
            contract = true
            put(1L, 1)
            put(2L, 2)
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals(1, map[1L])
        assertEquals(2, map[2L])
    }

    @Test
    fun buildLongObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildLongIntMap(20) {
                contract = true
                put(1L, 1)
                put(2L, 2)
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals(1, map[1L])
        assertEquals(2, map[2L])
    }

    @Test
    fun addToMap() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1L])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableLongIntMap(12)
        map[1L] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1L])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableLongIntMap(2)
        map[1L] = 1

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1, map[1L])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableLongIntMap(0)
        map[1L] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1L])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[1L] = 2

        assertEquals(1, map.size)
        assertEquals(2, map[1L])
    }

    @Test
    fun put() {
        val map = MutableLongIntMap()

        map.put(1L, 1)
        assertEquals(1, map[1L])
        map.put(1L, 2)
        assertEquals(2, map[1L])
    }

    @Test
    fun putWithDefault() {
        val map = MutableLongIntMap()

        var previous = map.put(1L, 1, -1)
        assertEquals(1, map[1L])
        assertEquals(-1, previous)

        previous = map.put(1L, 2, -1)
        assertEquals(2, map[1L])
        assertEquals(1, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertFailsWith<NoSuchElementException> { map[2L] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertEquals(2, map.getOrDefault(2L, 2))
    }

    @Test
    fun getOrElse() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertEquals(3, map.getOrElse(3L) { 3 })
    }

    @Test
    fun getOrPut() {
        val map = MutableLongIntMap()
        map[1L] = 1

        var counter = 0
        map.getOrPut(1L) {
            counter++
            2
        }
        assertEquals(1, map[1L])
        assertEquals(0, counter)

        map.getOrPut(2L) {
            counter++
            2
        }
        assertEquals(2, map[2L])
        assertEquals(1, counter)

        map.getOrPut(2L) {
            counter++
            3
        }
        assertEquals(2, map[2L])
        assertEquals(1, counter)

        map.getOrPut(3L) {
            counter++
            3
        }
        assertEquals(3, map[3L])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableLongIntMap()
        map.remove(1L)

        map[1L] = 1
        map.remove(1L)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableLongIntMap(6)
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3
        map[4L] = 4
        map[5L] = 5
        map[6L] = 6

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
        map[1L] = 7

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3
        map[4L] = 4
        map[5L] = 5
        map[6L] = 6

        map.removeIf { key, _ -> key == 1L || key == 3L }

        assertEquals(4, map.size)
        assertEquals(2, map[2L])
        assertEquals(4, map[4L])
        assertEquals(5, map[5L])
        assertEquals(6, map[6L])
    }

    @Test
    fun minus() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3

        map -= 1L

        assertEquals(2, map.size)
        assertFalse(1L in map)
    }

    @Test
    fun minusArray() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3

        map -= longArrayOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusSet() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3

        map -= longSetOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusList() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3

        map -= longListOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableLongIntMap()
        assertFalse(map.remove(1L, 1))

        map[1L] = 1
        assertTrue(map.remove(1L, 1))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableLongIntMap()

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toInt()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableLongIntMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toInt()
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
            val map = MutableLongIntMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toInt()
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
            val map = MutableLongIntMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toInt()
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
        val map = MutableLongIntMap()

        for (i in 0 until 32) {
            map[i.toLong()] = i.toInt()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableLongIntMap()
        assertEquals("{}", map.toString())

        map[1L] = 1
        map[2L] = 2
        val oneValueString = 1.toString()
        val twoValueString = 2.toString()
        val oneKeyString = 1L.toString()
        val twoKeyString = 2L.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableLongIntMap()
        repeat(5) { map[it.toLong()] = it.toInt() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toLong()}=${order[0].toInt()}, ${order[1].toLong()}=" +
                "${order[1].toInt()}, ${order[2].toLong()}=${order[2].toInt()}," +
                " ${order[3].toLong()}=${order[3].toInt()}, ${order[4].toLong()}=" +
                "${order[4].toInt()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toLong()}=${order[0].toInt()}, ${order[1].toLong()}=" +
                "${order[1].toInt()}, ${order[2].toLong()}=${order[2].toInt()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toLong()}=${order[0].toInt()}-${order[1].toLong()}=" +
                "${order[1].toInt()}-${order[2].toLong()}=${order[2].toInt()}-" +
                "${order[3].toLong()}=${order[3].toInt()}-${order[4].toLong()}=" +
                "${order[4].toInt()}<",
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
        val map = MutableLongIntMap()
        map[1L] = 1

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableLongIntMap()
        assertNotEquals(map, map2)

        map2[1L] = 1
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertTrue(map.containsKey(1L))
        assertFalse(map.containsKey(2L))
    }

    @Test
    fun contains() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertTrue(1L in map)
        assertFalse(2L in map)
    }

    @Test
    fun containsValue() {
        val map = MutableLongIntMap()
        map[1L] = 1

        assertTrue(map.containsValue(1))
        assertFalse(map.containsValue(3))
    }

    @Test
    fun empty() {
        val map = MutableLongIntMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1L] = 1

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableLongIntMap()
        assertEquals(0, map.count())

        map[1L] = 1
        assertEquals(1, map.count())

        map[2L] = 2
        map[3L] = 3
        map[4L] = 4
        map[5L] = 5
        map[6L] = 6

        assertEquals(2, map.count { key, _ -> key <= 2L })
        assertEquals(0, map.count { key, _ -> key < 0L })
    }

    @Test
    fun any() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3
        map[4L] = 4
        map[5L] = 5
        map[6L] = 6

        assertTrue(map.any { key, _ -> key == 4L })
        assertFalse(map.any { key, _ -> key < 0L })
    }

    @Test
    fun all() {
        val map = MutableLongIntMap()
        map[1L] = 1
        map[2L] = 2
        map[3L] = 3
        map[4L] = 4
        map[5L] = 5
        map[6L] = 6

        assertTrue(map.all { key, value -> key > 0L && value >= 1 })
        assertFalse(map.all { key, _ -> key < 6L })
    }

    @Test
    fun trim() {
        val map = MutableLongIntMap()
        assertEquals(7, map.trim())

        map[1L] = 1
        map[3L] = 3

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toInt()
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
        val map = MutableLongIntMap()

        for (i in 0..1000000) {
            map[i.toLong()] = i.toInt()
            map.remove(i.toLong())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
