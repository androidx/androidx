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
class FloatFloatMapTest {
    @Test
    fun floatFloatMap() {
        val map = MutableFloatFloatMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyFloatFloatMap() {
        val map = emptyFloatFloatMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyFloatFloatMap(), map)
    }

    @Test
    fun floatFloatMapFunction() {
        val map = mutableFloatFloatMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableFloatFloatMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatFloatMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableFloatFloatMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatFloatMapInitFunction() {
        val map1 =
            floatFloatMapOf(
                1f,
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1[1f])

        val map2 =
            floatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2[1f])
        assertEquals(2f, map2[2f])

        val map3 =
            floatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3[1f])
        assertEquals(2f, map3[2f])
        assertEquals(3f, map3[3f])

        val map4 =
            floatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
                4f,
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4[1f])
        assertEquals(2f, map4[2f])
        assertEquals(3f, map4[3f])
        assertEquals(4f, map4[4f])

        val map5 =
            floatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
                4f,
                4f,
                5f,
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5[1f])
        assertEquals(2f, map5[2f])
        assertEquals(3f, map5[3f])
        assertEquals(4f, map5[4f])
        assertEquals(5f, map5[5f])
    }

    @Test
    fun mutableFloatFloatMapInitFunction() {
        val map1 =
            mutableFloatFloatMapOf(
                1f,
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1[1f])

        val map2 =
            mutableFloatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2[1f])
        assertEquals(2f, map2[2f])

        val map3 =
            mutableFloatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3[1f])
        assertEquals(2f, map3[2f])
        assertEquals(3f, map3[3f])

        val map4 =
            mutableFloatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
                4f,
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4[1f])
        assertEquals(2f, map4[2f])
        assertEquals(3f, map4[3f])
        assertEquals(4f, map4[4f])

        val map5 =
            mutableFloatFloatMapOf(
                1f,
                1f,
                2f,
                2f,
                3f,
                3f,
                4f,
                4f,
                5f,
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5[1f])
        assertEquals(2f, map5[2f])
        assertEquals(3f, map5[3f])
        assertEquals(4f, map5[4f])
        assertEquals(5f, map5[5f])
    }

    @Test
    fun addToMap() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1f])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableFloatFloatMap(12)
        map[1f] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1f])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableFloatFloatMap(2)
        map[1f] = 1f

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1f, map[1f])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableFloatFloatMap(0)
        map[1f] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1f])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[1f] = 2f

        assertEquals(1, map.size)
        assertEquals(2f, map[1f])
    }

    @Test
    fun put() {
        val map = MutableFloatFloatMap()

        map.put(1f, 1f)
        assertEquals(1f, map[1f])
        map.put(1f, 2f)
        assertEquals(2f, map[1f])
    }

    @Test
    fun putWithDefault() {
        val map = MutableFloatFloatMap()

        var previous = map.put(1f, 1f, -1f)
        assertEquals(1f, map[1f])
        assertEquals(-1f, previous)

        previous = map.put(1f, 2f, -1f)
        assertEquals(2f, map[1f])
        assertEquals(1f, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertFailsWith<NoSuchElementException> { map[2f] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertEquals(2f, map.getOrDefault(2f, 2f))
    }

    @Test
    fun getOrElse() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertEquals(3f, map.getOrElse(3f) { 3f })
    }

    @Test
    fun getOrPut() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        var counter = 0
        map.getOrPut(1f) {
            counter++
            2f
        }
        assertEquals(1f, map[1f])
        assertEquals(0, counter)

        map.getOrPut(2f) {
            counter++
            2f
        }
        assertEquals(2f, map[2f])
        assertEquals(1, counter)

        map.getOrPut(2f) {
            counter++
            3f
        }
        assertEquals(2f, map[2f])
        assertEquals(1, counter)

        map.getOrPut(3f) {
            counter++
            3f
        }
        assertEquals(3f, map[3f])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableFloatFloatMap()
        map.remove(1f)

        map[1f] = 1f
        map.remove(1f)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableFloatFloatMap(6)
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f
        map[4f] = 4f
        map[5f] = 5f
        map[6f] = 6f

        // Removing all the entries will mark the medata as deleted
        map.remove(1f)
        map.remove(2f)
        map.remove(3f)
        map.remove(4f)
        map.remove(5f)
        map.remove(6f)

        assertEquals(0, map.size)

        val capacity = map.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        map[1f] = 7f

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f
        map[4f] = 4f
        map[5f] = 5f
        map[6f] = 6f

        map.removeIf { key, _ -> key == 1f || key == 3f }

        assertEquals(4, map.size)
        assertEquals(2f, map[2f])
        assertEquals(4f, map[4f])
        assertEquals(5f, map[5f])
        assertEquals(6f, map[6f])
    }

    @Test
    fun minus() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f

        map -= 1f

        assertEquals(2, map.size)
        assertFalse(1f in map)
    }

    @Test
    fun minusArray() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f

        map -= floatArrayOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun minusSet() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f

        map -= floatSetOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun minusList() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f

        map -= floatListOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableFloatFloatMap()
        assertFalse(map.remove(1f, 1f))

        map[1f] = 1f
        assertTrue(map.remove(1f, 1f))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableFloatFloatMap()

        for (i in 0 until 1700) {
            map[i.toFloat()] = i.toFloat()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableFloatFloatMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toFloat()
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value.toFloat())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableFloatFloatMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toFloat()
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
            val map = MutableFloatFloatMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toFloat()
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
        val map = MutableFloatFloatMap()

        for (i in 0 until 32) {
            map[i.toFloat()] = i.toFloat()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableFloatFloatMap()
        assertEquals("{}", map.toString())

        map[1f] = 1f
        map[2f] = 2f
        val oneValueString = 1f.toString()
        val twoValueString = 2f.toString()
        val oneKeyString = 1f.toString()
        val twoKeyString = 2f.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableFloatFloatMap()
        repeat(5) { map[it.toFloat()] = it.toFloat() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toFloat()}=${order[0].toFloat()}, ${order[1].toFloat()}=" +
                "${order[1].toFloat()}, ${order[2].toFloat()}=${order[2].toFloat()}," +
                " ${order[3].toFloat()}=${order[3].toFloat()}, ${order[4].toFloat()}=" +
                "${order[4].toFloat()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toFloat()}=${order[0].toFloat()}, ${order[1].toFloat()}=" +
                "${order[1].toFloat()}, ${order[2].toFloat()}=${order[2].toFloat()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toFloat()}=${order[0].toFloat()}-${order[1].toFloat()}=" +
                "${order[1].toFloat()}-${order[2].toFloat()}=${order[2].toFloat()}-" +
                "${order[3].toFloat()}=${order[3].toFloat()}-${order[4].toFloat()}=" +
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
    fun equalsTest() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableFloatFloatMap()
        assertNotEquals(map, map2)

        map2[1f] = 1f
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertTrue(map.containsKey(1f))
        assertFalse(map.containsKey(2f))
    }

    @Test
    fun contains() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertTrue(1f in map)
        assertFalse(2f in map)
    }

    @Test
    fun containsValue() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f

        assertTrue(map.containsValue(1f))
        assertFalse(map.containsValue(3f))
    }

    @Test
    fun empty() {
        val map = MutableFloatFloatMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1f] = 1f

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableFloatFloatMap()
        assertEquals(0, map.count())

        map[1f] = 1f
        assertEquals(1, map.count())

        map[2f] = 2f
        map[3f] = 3f
        map[4f] = 4f
        map[5f] = 5f
        map[6f] = 6f

        assertEquals(2, map.count { key, _ -> key <= 2f })
        assertEquals(0, map.count { key, _ -> key < 0f })
    }

    @Test
    fun any() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f
        map[4f] = 4f
        map[5f] = 5f
        map[6f] = 6f

        assertTrue(map.any { key, _ -> key == 4f })
        assertFalse(map.any { key, _ -> key < 0f })
    }

    @Test
    fun all() {
        val map = MutableFloatFloatMap()
        map[1f] = 1f
        map[2f] = 2f
        map[3f] = 3f
        map[4f] = 4f
        map[5f] = 5f
        map[6f] = 6f

        assertTrue(map.all { key, value -> key > 0f && value >= 1f })
        assertFalse(map.all { key, _ -> key < 6f })
    }

    @Test
    fun trim() {
        val map = MutableFloatFloatMap()
        assertEquals(7, map.trim())

        map[1f] = 1f
        map[3f] = 3f

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toFloat()] = i.toFloat()
        }

        assertEquals(2047, map.capacity)

        // After removing these items, our capacity needs should go
        // from 2047 down to 1023
        for (i in 0 until 1700) {
            if (i and 0x1 == 0x0) {
                val s = i.toFloat()
                map.remove(s)
            }
        }

        assertEquals(1024, map.trim())
        assertEquals(0, map.trim())
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableFloatFloatMap()

        for (i in 0..1000000) {
            map[i.toFloat()] = i.toFloat()
            map.remove(i.toFloat())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
