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
class FloatIntMapTest {
    @Test
    fun floatIntMap() {
        val map = MutableFloatIntMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyFloatIntMap() {
        val map = emptyFloatIntMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyFloatIntMap(), map)
    }

    @Test
    fun floatIntMapFunction() {
        val map = mutableFloatIntMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableFloatIntMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatIntMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableFloatIntMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatIntMapInitFunction() {
        val map1 =
            floatIntMapOf(
                1f,
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1[1f])

        val map2 =
            floatIntMapOf(
                1f,
                1,
                2f,
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2[1f])
        assertEquals(2, map2[2f])

        val map3 =
            floatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3[1f])
        assertEquals(2, map3[2f])
        assertEquals(3, map3[3f])

        val map4 =
            floatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
                4f,
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4[1f])
        assertEquals(2, map4[2f])
        assertEquals(3, map4[3f])
        assertEquals(4, map4[4f])

        val map5 =
            floatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
                4f,
                4,
                5f,
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5[1f])
        assertEquals(2, map5[2f])
        assertEquals(3, map5[3f])
        assertEquals(4, map5[4f])
        assertEquals(5, map5[5f])
    }

    @Test
    fun mutableFloatIntMapInitFunction() {
        val map1 =
            mutableFloatIntMapOf(
                1f,
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1[1f])

        val map2 =
            mutableFloatIntMapOf(
                1f,
                1,
                2f,
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2[1f])
        assertEquals(2, map2[2f])

        val map3 =
            mutableFloatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3[1f])
        assertEquals(2, map3[2f])
        assertEquals(3, map3[3f])

        val map4 =
            mutableFloatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
                4f,
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4[1f])
        assertEquals(2, map4[2f])
        assertEquals(3, map4[3f])
        assertEquals(4, map4[4f])

        val map5 =
            mutableFloatIntMapOf(
                1f,
                1,
                2f,
                2,
                3f,
                3,
                4f,
                4,
                5f,
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5[1f])
        assertEquals(2, map5[2f])
        assertEquals(3, map5[3f])
        assertEquals(4, map5[4f])
        assertEquals(5, map5[5f])
    }

    @Test
    fun buildFloatIntMapFunction() {
        val contract: Boolean
        val map = buildFloatIntMap {
            contract = true
            put(1f, 1)
            put(2f, 2)
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals(1, map[1f])
        assertEquals(2, map[2f])
    }

    @Test
    fun buildFloatObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildFloatIntMap(20) {
                contract = true
                put(1f, 1)
                put(2f, 2)
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals(1, map[1f])
        assertEquals(2, map[2f])
    }

    @Test
    fun addToMap() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1f])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableFloatIntMap(12)
        map[1f] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1f])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableFloatIntMap(2)
        map[1f] = 1

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1, map[1f])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableFloatIntMap(0)
        map[1f] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[1f])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[1f] = 2

        assertEquals(1, map.size)
        assertEquals(2, map[1f])
    }

    @Test
    fun put() {
        val map = MutableFloatIntMap()

        map.put(1f, 1)
        assertEquals(1, map[1f])
        map.put(1f, 2)
        assertEquals(2, map[1f])
    }

    @Test
    fun putWithDefault() {
        val map = MutableFloatIntMap()

        var previous = map.put(1f, 1, -1)
        assertEquals(1, map[1f])
        assertEquals(-1, previous)

        previous = map.put(1f, 2, -1)
        assertEquals(2, map[1f])
        assertEquals(1, previous)
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertFailsWith<NoSuchElementException> { map[2f] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertEquals(2, map.getOrDefault(2f, 2))
    }

    @Test
    fun getOrElse() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertEquals(3, map.getOrElse(3f) { 3 })
    }

    @Test
    fun getOrPut() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        var counter = 0
        map.getOrPut(1f) {
            counter++
            2
        }
        assertEquals(1, map[1f])
        assertEquals(0, counter)

        map.getOrPut(2f) {
            counter++
            2
        }
        assertEquals(2, map[2f])
        assertEquals(1, counter)

        map.getOrPut(2f) {
            counter++
            3
        }
        assertEquals(2, map[2f])
        assertEquals(1, counter)

        map.getOrPut(3f) {
            counter++
            3
        }
        assertEquals(3, map[3f])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableFloatIntMap()
        map.remove(1f)

        map[1f] = 1
        map.remove(1f)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableFloatIntMap(6)
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3
        map[4f] = 4
        map[5f] = 5
        map[6f] = 6

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
        map[1f] = 7

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3
        map[4f] = 4
        map[5f] = 5
        map[6f] = 6

        map.removeIf { key, _ -> key == 1f || key == 3f }

        assertEquals(4, map.size)
        assertEquals(2, map[2f])
        assertEquals(4, map[4f])
        assertEquals(5, map[5f])
        assertEquals(6, map[6f])
    }

    @Test
    fun minus() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3

        map -= 1f

        assertEquals(2, map.size)
        assertFalse(1f in map)
    }

    @Test
    fun minusArray() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3

        map -= floatArrayOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun minusSet() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3

        map -= floatSetOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun minusList() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3

        map -= floatListOf(3f, 2f)

        assertEquals(1, map.size)
        assertFalse(3f in map)
        assertFalse(2f in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableFloatIntMap()
        assertFalse(map.remove(1f, 1))

        map[1f] = 1
        assertTrue(map.remove(1f, 1))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableFloatIntMap()

        for (i in 0 until 1700) {
            map[i.toFloat()] = i.toInt()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableFloatIntMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toInt()
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
            val map = MutableFloatIntMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toInt()
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
            val map = MutableFloatIntMap()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toInt()
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
        val map = MutableFloatIntMap()

        for (i in 0 until 32) {
            map[i.toFloat()] = i.toInt()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableFloatIntMap()
        assertEquals("{}", map.toString())

        map[1f] = 1
        map[2f] = 2
        val oneValueString = 1.toString()
        val twoValueString = 2.toString()
        val oneKeyString = 1f.toString()
        val twoKeyString = 2f.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = MutableFloatIntMap()
        repeat(5) { map[it.toFloat()] = it.toInt() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toFloat()}=${order[0].toInt()}, ${order[1].toFloat()}=" +
                "${order[1].toInt()}, ${order[2].toFloat()}=${order[2].toInt()}," +
                " ${order[3].toFloat()}=${order[3].toInt()}, ${order[4].toFloat()}=" +
                "${order[4].toInt()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toFloat()}=${order[0].toInt()}, ${order[1].toFloat()}=" +
                "${order[1].toInt()}, ${order[2].toFloat()}=${order[2].toInt()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toFloat()}=${order[0].toInt()}-${order[1].toFloat()}=" +
                "${order[1].toInt()}-${order[2].toFloat()}=${order[2].toInt()}-" +
                "${order[3].toFloat()}=${order[3].toInt()}-${order[4].toFloat()}=" +
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
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableFloatIntMap()
        assertNotEquals(map, map2)

        map2[1f] = 1
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertTrue(map.containsKey(1f))
        assertFalse(map.containsKey(2f))
    }

    @Test
    fun contains() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertTrue(1f in map)
        assertFalse(2f in map)
    }

    @Test
    fun containsValue() {
        val map = MutableFloatIntMap()
        map[1f] = 1

        assertTrue(map.containsValue(1))
        assertFalse(map.containsValue(3))
    }

    @Test
    fun empty() {
        val map = MutableFloatIntMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1f] = 1

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableFloatIntMap()
        assertEquals(0, map.count())

        map[1f] = 1
        assertEquals(1, map.count())

        map[2f] = 2
        map[3f] = 3
        map[4f] = 4
        map[5f] = 5
        map[6f] = 6

        assertEquals(2, map.count { key, _ -> key <= 2f })
        assertEquals(0, map.count { key, _ -> key < 0f })
    }

    @Test
    fun any() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3
        map[4f] = 4
        map[5f] = 5
        map[6f] = 6

        assertTrue(map.any { key, _ -> key == 4f })
        assertFalse(map.any { key, _ -> key < 0f })
    }

    @Test
    fun all() {
        val map = MutableFloatIntMap()
        map[1f] = 1
        map[2f] = 2
        map[3f] = 3
        map[4f] = 4
        map[5f] = 5
        map[6f] = 6

        assertTrue(map.all { key, value -> key > 0f && value >= 1 })
        assertFalse(map.all { key, _ -> key < 6f })
    }

    @Test
    fun trim() {
        val map = MutableFloatIntMap()
        assertEquals(7, map.trim())

        map[1f] = 1
        map[3f] = 3

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toFloat()] = i.toInt()
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
        val map = MutableFloatIntMap()

        for (i in 0..1000000) {
            map[i.toFloat()] = i.toInt()
            map.remove(i.toFloat())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
