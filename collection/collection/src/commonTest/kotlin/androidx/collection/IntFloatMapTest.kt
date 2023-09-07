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
class IntFloatMapTest {
    @Test
    fun intFloatMap() {
        val map = MutableIntFloatMap()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyIntFloatMap() {
        val map = emptyIntFloatMap()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyIntFloatMap(), map)
    }

    @Test
    fun intFloatMapFunction() {
        val map = mutableIntFloatMapOf()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableIntFloatMap(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun intFloatMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableIntFloatMap(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun intFloatMapPairsFunction() {
        val map = mutableIntFloatMapOf(
            1 to 1f,
            2 to 2f
        )
        assertEquals(2, map.size)
        assertEquals(1f, map[1])
        assertEquals(2f, map[2])
    }

    @Test
    fun addToMap() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableIntFloatMap(12)
        map[1] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableIntFloatMap(2)
        map[1] = 1f

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1f, map[1])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableIntFloatMap(0)
        map[1] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[1] = 2f

        assertEquals(1, map.size)
        assertEquals(2f, map[1])
    }

    @Test
    fun put() {
        val map = MutableIntFloatMap()

        map.put(1, 1f)
        assertEquals(1f, map[1])
        map.put(1, 2f)
        assertEquals(2f, map[1])
    }

    @Test
    fun putAllArray() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f

        map.putAll(arrayOf(3 to 3f, 7 to 7f))

        assertEquals(4, map.size)
        assertEquals(3f, map[3])
        assertEquals(7f, map[7])
    }

    @Test
    fun plus() {
        val map = MutableIntFloatMap()
        map += 1 to 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[1])
    }

    @Test
    fun plusArray() {
        val map = MutableIntFloatMap()
        map += arrayOf(3 to 3f, 7 to 7f)

        assertEquals(2, map.size)
        assertEquals(3f, map[3])
        assertEquals(7f, map[7])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertFailsWith<NoSuchElementException> {
            map[2]
        }
    }

    @Test
    fun getOrDefault() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertEquals(2f, map.getOrDefault(2, 2f))
    }

    @Test
    fun getOrElse() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertEquals(3f, map.getOrElse(3) { 3f })
    }

    @Test
    fun getOrPut() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        var counter = 0
        map.getOrPut(1) {
            counter++
            2f
        }
        assertEquals(1f, map[1])
        assertEquals(0, counter)

        map.getOrPut(2) {
            counter++
            2f
        }
        assertEquals(2f, map[2])
        assertEquals(1, counter)

        map.getOrPut(2) {
            counter++
            3f
        }
        assertEquals(2f, map[2])
        assertEquals(1, counter)

        map.getOrPut(3) {
            counter++
            3f
        }
        assertEquals(3f, map[3])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableIntFloatMap()
        map.remove(1)

        map[1] = 1f
        map.remove(1)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableIntFloatMap(6)
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f
        map[4] = 4f
        map[5] = 5f
        map[6] = 6f

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
        map[7] = 7f

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f
        map[4] = 4f
        map[5] = 5f
        map[6] = 6f

        map.removeIf { key, _ -> key == 1 || key == 3 }

        assertEquals(4, map.size)
        assertEquals(2f, map[2])
        assertEquals(4f, map[4])
        assertEquals(5f, map[5])
        assertEquals(6f, map[6])
    }

    @Test
    fun minus() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f

        map -= 1

        assertEquals(2, map.size)
        assertFalse(1 in map)
    }

    @Test
    fun minusArray() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f

        map -= intArrayOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun minusSet() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f

        map -= intSetOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun minusList() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f

        map -= intListOf(3, 2)

        assertEquals(1, map.size)
        assertFalse(3 in map)
        assertFalse(2 in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableIntFloatMap()
        assertFalse(map.remove(1, 1f))

        map[1] = 1f
        assertTrue(map.remove(1, 1f))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableIntFloatMap()

        for (i in 0 until 1700) {
            map[i.toInt()] = i.toFloat()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableIntFloatMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toFloat()
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
            val map = MutableIntFloatMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toFloat()
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
            val map = MutableIntFloatMap()

            for (j in 0 until i) {
                map[j.toInt()] = j.toFloat()
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
        val map = MutableIntFloatMap()

        for (i in 0 until 32) {
            map[i.toInt()] = i.toFloat()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableIntFloatMap()
        assertEquals("{}", map.toString())

        map[1] = 1f
        map[2] = 2f
        val oneValueString = 1f.toString()
        val twoValueString = 2f.toString()
        val oneKeyString = 1.toString()
        val twoKeyString = 2.toString()
        assertTrue(
            "{$oneKeyString=$oneValueString, $twoKeyString=$twoValueString}" == map.toString() ||
                "{$twoKeyString=$twoValueString, $oneKeyString=$oneValueString}" == map.toString()
        )
    }

    @Test
    fun equals() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableIntFloatMap()
        assertNotEquals(map, map2)

        map2[1] = 1f
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertTrue(map.containsKey(1))
        assertFalse(map.containsKey(2))
    }

    @Test
    fun contains() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertTrue(1 in map)
        assertFalse(2 in map)
    }

    @Test
    fun containsValue() {
        val map = MutableIntFloatMap()
        map[1] = 1f

        assertTrue(map.containsValue(1f))
        assertFalse(map.containsValue(3f))
    }

    @Test
    fun empty() {
        val map = MutableIntFloatMap()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1] = 1f

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableIntFloatMap()
        assertEquals(0, map.count())

        map[1] = 1f
        assertEquals(1, map.count())

        map[2] = 2f
        map[3] = 3f
        map[4] = 4f
        map[5] = 5f
        map[6] = 6f

        assertEquals(2, map.count { key, _ -> key <= 2 })
        assertEquals(0, map.count { key, _ -> key < 0 })
    }

    @Test
    fun any() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f
        map[4] = 4f
        map[5] = 5f
        map[6] = 6f

        assertTrue(map.any { key, _ -> key == 4 })
        assertFalse(map.any { key, _ -> key < 0 })
    }

    @Test
    fun all() {
        val map = MutableIntFloatMap()
        map[1] = 1f
        map[2] = 2f
        map[3] = 3f
        map[4] = 4f
        map[5] = 5f
        map[6] = 6f

        assertTrue(map.all { key, value -> key > 0 && value >= 1f })
        assertFalse(map.all { key, _ -> key < 6 })
    }

    @Test
    fun trim() {
        val map = MutableIntFloatMap()
        assertEquals(7, map.trim())

        map[1] = 1f
        map[3] = 3f

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toInt()] = i.toFloat()
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
}
