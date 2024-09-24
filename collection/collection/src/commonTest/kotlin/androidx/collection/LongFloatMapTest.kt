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
class LongFloatMapTest {
    @Test
    fun longFloatMap() {
        val map = MutableLongFloatMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyLongFloatMap() {
        val map = emptyLongFloatMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyLongFloatMap(), map)
    }

    @Test
    fun longFloatMapFunction() {
        val map = mutableLongFloatMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableLongFloatMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longFloatMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableLongFloatMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longFloatMapInitFunction() {
        val map1 =
            longFloatMapOf(
                1L,
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1[1L])

        val map2 =
            longFloatMapOf(
                1L,
                1f,
                2L,
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2[1L])
        assertEquals(2f, map2[2L])

        val map3 =
            longFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3[1L])
        assertEquals(2f, map3[2L])
        assertEquals(3f, map3[3L])

        val map4 =
            longFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
                4L,
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4[1L])
        assertEquals(2f, map4[2L])
        assertEquals(3f, map4[3L])
        assertEquals(4f, map4[4L])

        val map5 =
            longFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
                4L,
                4f,
                5L,
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5[1L])
        assertEquals(2f, map5[2L])
        assertEquals(3f, map5[3L])
        assertEquals(4f, map5[4L])
        assertEquals(5f, map5[5L])
    }

    @Test
    fun mutableLongFloatMapInitFunction() {
        val map1 =
            mutableLongFloatMapOf(
                1L,
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1[1L])

        val map2 =
            mutableLongFloatMapOf(
                1L,
                1f,
                2L,
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2[1L])
        assertEquals(2f, map2[2L])

        val map3 =
            mutableLongFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3[1L])
        assertEquals(2f, map3[2L])
        assertEquals(3f, map3[3L])

        val map4 =
            mutableLongFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
                4L,
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4[1L])
        assertEquals(2f, map4[2L])
        assertEquals(3f, map4[3L])
        assertEquals(4f, map4[4L])

        val map5 =
            mutableLongFloatMapOf(
                1L,
                1f,
                2L,
                2f,
                3L,
                3f,
                4L,
                4f,
                5L,
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5[1L])
        assertEquals(2f, map5[2L])
        assertEquals(3f, map5[3L])
        assertEquals(4f, map5[4L])
        assertEquals(5f, map5[5L])
    }

    @Test
    fun buildLongFloatMapFunction() {
        val contract: Boolean
        val map = buildLongFloatMap {
            contract = true
            put(1L, 1f)
            put(2L, 2f)
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals(1f, map[1L])
        assertEquals(2f, map[2L])
    }

    @Test
    fun buildLongObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildLongFloatMap(20) {
                contract = true
                put(1L, 1f)
                put(2L, 2f)
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals(1f, map[1L])
        assertEquals(2f, map[2L])
    }

    @Test
    fun addToMap() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1L])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableLongFloatMap(12)
        map[1L] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1L])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableLongFloatMap(2)
        map[1L] = 1f

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1f, map[1L])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableLongFloatMap(0)
        map[1L] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1L])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[1L] = 2f

        assertEquals(1, map.size)
        assertEquals(2f, map[1L])
    }

    @Test
    fun put() {
        val map = MutableLongFloatMap()

        map.put(1L, 1f)
        assertEquals(1f, map[1L])
        map.put(1L, 2f)
        assertEquals(2f, map[1L])
    }

    @Test
    fun putWithDefault() {
        val map = MutableLongFloatMap()

        var previous = map.put(1L, 1f, -1f)
        assertEquals(1f, map[1L])
        assertEquals(-1f, previous)

        previous = map.put(1L, 2f, -1f)
        assertEquals(2f, map[1L])
        assertEquals(1f, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertFailsWith<NoSuchElementException> { map[2L] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertEquals(2f, map.getOrDefault(2L, 2f))
    }

    @Test
    fun getOrElse() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertEquals(3f, map.getOrElse(3L) { 3f })
    }

    @Test
    fun getOrPut() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        var counter = 0
        map.getOrPut(1L) {
            counter++
            2f
        }
        assertEquals(1f, map[1L])
        assertEquals(0, counter)

        map.getOrPut(2L) {
            counter++
            2f
        }
        assertEquals(2f, map[2L])
        assertEquals(1, counter)

        map.getOrPut(2L) {
            counter++
            3f
        }
        assertEquals(2f, map[2L])
        assertEquals(1, counter)

        map.getOrPut(3L) {
            counter++
            3f
        }
        assertEquals(3f, map[3L])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableLongFloatMap()
        map.remove(1L)

        map[1L] = 1f
        map.remove(1L)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableLongFloatMap(6)
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f
        map[4L] = 4f
        map[5L] = 5f
        map[6L] = 6f

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
        map[1L] = 7f

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f
        map[4L] = 4f
        map[5L] = 5f
        map[6L] = 6f

        map.removeIf { key, _ -> key == 1L || key == 3L }

        assertEquals(4, map.size)
        assertEquals(2f, map[2L])
        assertEquals(4f, map[4L])
        assertEquals(5f, map[5L])
        assertEquals(6f, map[6L])
    }

    @Test
    fun minus() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f

        map -= 1L

        assertEquals(2, map.size)
        assertFalse(1L in map)
    }

    @Test
    fun minusArray() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f

        map -= longArrayOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusSet() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f

        map -= longSetOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun minusList() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f

        map -= longListOf(3L, 2L)

        assertEquals(1, map.size)
        assertFalse(3L in map)
        assertFalse(2L in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableLongFloatMap()
        assertFalse(map.remove(1L, 1f))

        map[1L] = 1f
        assertTrue(map.remove(1L, 1f))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableLongFloatMap()

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toFloat()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableLongFloatMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toFloat()
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
            val map = MutableLongFloatMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toFloat()
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
            val map = MutableLongFloatMap()

            for (j in 0 until i) {
                map[j.toLong()] = j.toFloat()
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
        val map = MutableLongFloatMap()

        for (i in 0 until 32) {
            map[i.toLong()] = i.toFloat()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableLongFloatMap()
        assertEquals("{}", map.toString())

        map[1L] = 1f
        map[2L] = 2f
        val oneValueString = 1f.toString()
        val twoValueString = 2f.toString()
        val oneKeyString = 1L.toString()
        val twoKeyString = 2L.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableLongFloatMap()
        repeat(5) { map[it.toLong()] = it.toFloat() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toLong()}=${order[0].toFloat()}, ${order[1].toLong()}=" +
                "${order[1].toFloat()}, ${order[2].toLong()}=${order[2].toFloat()}," +
                " ${order[3].toLong()}=${order[3].toFloat()}, ${order[4].toLong()}=" +
                "${order[4].toFloat()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toLong()}=${order[0].toFloat()}, ${order[1].toLong()}=" +
                "${order[1].toFloat()}, ${order[2].toLong()}=${order[2].toFloat()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toLong()}=${order[0].toFloat()}-${order[1].toLong()}=" +
                "${order[1].toFloat()}-${order[2].toLong()}=${order[2].toFloat()}-" +
                "${order[3].toLong()}=${order[3].toFloat()}-${order[4].toLong()}=" +
                "${order[4].toFloat()}<",
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
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableLongFloatMap()
        assertNotEquals(map, map2)

        map2[1L] = 1f
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertTrue(map.containsKey(1L))
        assertFalse(map.containsKey(2L))
    }

    @Test
    fun contains() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertTrue(1L in map)
        assertFalse(2L in map)
    }

    @Test
    fun containsValue() {
        val map = MutableLongFloatMap()
        map[1L] = 1f

        assertTrue(map.containsValue(1f))
        assertFalse(map.containsValue(3f))
    }

    @Test
    fun empty() {
        val map = MutableLongFloatMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1L] = 1f

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableLongFloatMap()
        assertEquals(0, map.count())

        map[1L] = 1f
        assertEquals(1, map.count())

        map[2L] = 2f
        map[3L] = 3f
        map[4L] = 4f
        map[5L] = 5f
        map[6L] = 6f

        assertEquals(2, map.count { key, _ -> key <= 2L })
        assertEquals(0, map.count { key, _ -> key < 0L })
    }

    @Test
    fun any() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f
        map[4L] = 4f
        map[5L] = 5f
        map[6L] = 6f

        assertTrue(map.any { key, _ -> key == 4L })
        assertFalse(map.any { key, _ -> key < 0L })
    }

    @Test
    fun all() {
        val map = MutableLongFloatMap()
        map[1L] = 1f
        map[2L] = 2f
        map[3L] = 3f
        map[4L] = 4f
        map[5L] = 5f
        map[6L] = 6f

        assertTrue(map.all { key, value -> key > 0L && value >= 1f })
        assertFalse(map.all { key, _ -> key < 6L })
    }

    @Test
    fun trim() {
        val map = MutableLongFloatMap()
        assertEquals(7, map.trim())

        map[1L] = 1f
        map[3L] = 3f

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toFloat()
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
        val map = MutableLongFloatMap()

        for (i in 0..1000000) {
            map[i.toLong()] = i.toFloat()
            map.remove(i.toLong())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
