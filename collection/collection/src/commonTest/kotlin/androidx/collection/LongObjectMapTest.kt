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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

class LongObjectMapTest {
    @Test
    fun longObjectMap() {
        val map = MutableLongObjectMap<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyLongObjectMap() {
        val map = emptyLongObjectMap<String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyLongObjectMap<String>(), map)
    }

    @Test
    fun longObjectMapFunction() {
        val map = mutableLongObjectMapOf<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableLongObjectMap<String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longObjectMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableLongObjectMap<String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun longObjectMapInitFunction() {
        val map1 =
            longObjectMapOf(
                1L,
                "World",
            )
        assertEquals(1, map1.size)
        assertEquals("World", map1[1L])

        val map2 =
            longObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
            )
        assertEquals(2, map2.size)
        assertEquals("World", map2[1L])
        assertEquals("Monde", map2[2L])

        val map3 =
            longObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
            )
        assertEquals(3, map3.size)
        assertEquals("World", map3[1L])
        assertEquals("Monde", map3[2L])
        assertEquals("Welt", map3[3L])

        val map4 =
            longObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
                4L,
                "Sekai",
            )

        assertEquals(4, map4.size)
        assertEquals("World", map4[1L])
        assertEquals("Monde", map4[2L])
        assertEquals("Welt", map4[3L])
        assertEquals("Sekai", map4[4L])

        val map5 =
            longObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
                4L,
                "Sekai",
                5L,
                "Mondo",
            )

        assertEquals(5, map5.size)
        assertEquals("World", map5[1L])
        assertEquals("Monde", map5[2L])
        assertEquals("Welt", map5[3L])
        assertEquals("Sekai", map5[4L])
        assertEquals("Mondo", map5[5L])
    }

    @Test
    fun mutableLongObjectMapInitFunction() {
        val map1 =
            mutableLongObjectMapOf(
                1L,
                "World",
            )
        assertEquals(1, map1.size)
        assertEquals("World", map1[1L])

        val map2 =
            mutableLongObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
            )
        assertEquals(2, map2.size)
        assertEquals("World", map2[1L])
        assertEquals("Monde", map2[2L])

        val map3 =
            mutableLongObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
            )
        assertEquals(3, map3.size)
        assertEquals("World", map3[1L])
        assertEquals("Monde", map3[2L])
        assertEquals("Welt", map3[3L])

        val map4 =
            mutableLongObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
                4L,
                "Sekai",
            )

        assertEquals(4, map4.size)
        assertEquals("World", map4[1L])
        assertEquals("Monde", map4[2L])
        assertEquals("Welt", map4[3L])
        assertEquals("Sekai", map4[4L])

        val map5 =
            mutableLongObjectMapOf(
                1L,
                "World",
                2L,
                "Monde",
                3L,
                "Welt",
                4L,
                "Sekai",
                5L,
                "Mondo",
            )

        assertEquals(5, map5.size)
        assertEquals("World", map5[1L])
        assertEquals("Monde", map5[2L])
        assertEquals("Welt", map5[3L])
        assertEquals("Sekai", map5[4L])
        assertEquals("Mondo", map5[5L])
    }

    @Test
    fun buildLongObjectMapFunction() {
        val contract: Boolean
        val map = buildLongObjectMap {
            contract = true
            put(1L, "World")
            put(2L, "Monde")
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals("World", map[1L])
        assertEquals("Monde", map[2L])
    }

    @Test
    fun buildLongObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildLongObjectMap(20) {
                contract = true
                put(1L, "World")
                put(2L, "Monde")
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals("World", map[1L])
        assertEquals("Monde", map[2L])
    }

    @Test
    fun addToMap() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1L])
    }

    @Test
    fun insertIndex0() {
        val map = MutableLongObjectMap<String>()
        map.put(1L, "World")
        assertEquals("World", map[1L])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableLongObjectMap<String>(12)
        map[1L] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1L])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableLongObjectMap<String>(2)
        map[1L] = "World"

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals("World", map[1L])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableLongObjectMap<String>(0)
        map[1L] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1L])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[1L] = "Monde"

        assertEquals(1, map.size)
        assertEquals("Monde", map[1L])
    }

    @Test
    fun put() {
        val map = MutableLongObjectMap<String?>()

        assertNull(map.put(1L, "World"))
        assertEquals("World", map.put(1L, "Monde"))
        assertNull(map.put(2L, null))
        assertNull(map.put(2L, "Monde"))
    }

    @Test
    fun putAllMap() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        map.putAll(mutableLongObjectMapOf(3L, "Welt", 7L, "Mundo"))

        assertEquals(4, map.size)
        assertEquals("Welt", map[3L])
        assertEquals("Mundo", map[7L])
    }

    @Test
    fun plusMap() {
        val map = MutableLongObjectMap<String>()
        map += longObjectMapOf(3L, "Welt", 7L, "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map[3L])
        assertEquals("Mundo", map[7L])
    }

    @Test
    fun nullValue() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = null

        assertEquals(1, map.size)
        assertNull(map[1L])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"

        assertNull(map[2L])
    }

    @Test
    fun getOrDefault() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"

        assertEquals("Monde", map.getOrDefault(2L, "Monde"))
    }

    @Test
    fun getOrElse() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        assertEquals("Monde", map.getOrElse(2L) { "Monde" })
        assertEquals("Welt", map.getOrElse(3L) { "Welt" })
    }

    @Test
    fun getOrPut() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"

        var counter = 0
        map.getOrPut(1L) {
            counter++
            "Monde"
        }
        assertEquals("World", map[1L])
        assertEquals(0, counter)

        map.getOrPut(2L) {
            counter++
            "Monde"
        }
        assertEquals("Monde", map[2L])
        assertEquals(1, counter)

        map.getOrPut(2L) {
            counter++
            "Welt"
        }
        assertEquals("Monde", map[2L])
        assertEquals(1, counter)

        map.getOrPut(3L) {
            counter++
            null
        }
        assertNull(map[3L])
        assertEquals(2, counter)

        map.getOrPut(3L) {
            counter++
            "Welt"
        }
        assertEquals("Welt", map[3L])
        assertEquals(3, counter)
    }

    @Test
    fun remove() {
        val map = MutableLongObjectMap<String?>()
        assertNull(map.remove(1L))

        map[1L] = "World"
        assertEquals("World", map.remove(1L))
        assertEquals(0, map.size)

        map[1L] = null
        assertNull(map.remove(1L))
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableLongObjectMap<String>(6)
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"
        map[4L] = "Sekai"
        map[5L] = "Mondo"
        map[6L] = "Sesang"

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
        map[1L] = "Mundo"

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"
        map[4L] = "Sekai"
        map[5L] = "Mondo"
        map[6L] = "Sesang"

        map.removeIf { key, value -> key == 1L || key == 3L || value.startsWith('S') }

        assertEquals(2, map.size)
        assertEquals("Monde", map[2L])
        assertEquals("Mondo", map[5L])
    }

    @Test
    fun minus() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"

        map -= 1L

        assertEquals(2, map.size)
        assertNull(map[1L])
    }

    @Test
    fun minusArray() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"

        map -= longArrayOf(3L, 2L)

        assertEquals(1, map.size)
        assertNull(map[3L])
        assertNull(map[2L])
    }

    @Test
    fun minusSet() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"

        map -= longSetOf(3L, 2L)

        assertEquals(1, map.size)
        assertNull(map[3L])
        assertNull(map[2L])
    }

    @Test
    fun minusList() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"

        map -= longListOf(3L, 2L)

        assertEquals(1, map.size)
        assertNull(map[3L])
        assertNull(map[2L])
    }

    @Test
    fun conditionalRemove() {
        val map = MutableLongObjectMap<String?>()
        assertFalse(map.remove(1L, "World"))

        map[1L] = "World"
        assertTrue(map.remove(1L, "World"))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableLongObjectMap<String>()

        for (i in 0 until 1700) {
            map[i.toLong()] = i.toString()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableLongObjectMap<String>()

            for (j in 0 until i) {
                map[j.toLong()] = j.toString()
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key.toInt().toString(), value)
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableLongObjectMap<String>()

            for (j in 0 until i) {
                map[j.toLong()] = j.toString()
            }

            var counter = 0
            map.forEachKey { key ->
                assertEquals(key.toInt().toString(), map[key])
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachValue() {
        for (i in 0..48) {
            val map = MutableLongObjectMap<String>()

            for (j in 0 until i) {
                map[j.toLong()] = j.toString()
            }

            var counter = 0
            map.forEachValue { value ->
                assertNotNull(value.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun clear() {
        val map = MutableLongObjectMap<String>()

        for (i in 0 until 32) {
            map[i.toLong()] = i.toString()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableLongObjectMap<String?>()
        assertEquals("{}", map.toString())

        map[1L] = "World"
        map[2L] = "Monde"
        val oneKey = 1L.toString()
        val twoKey = 2L.toString()
        assertTrue(
            "{$oneKey=World, $twoKey=Monde}" == map.toString() ||
                "{$twoKey=Monde, $oneKey=World}" == map.toString()
        )

        map.clear()
        map[1L] = null
        assertEquals("{$oneKey=null}", map.toString())

        val selfAsValueMap = MutableLongObjectMap<Any>()
        selfAsValueMap[1L] = selfAsValueMap
        assertEquals("{$oneKey=(this)}", selfAsValueMap.toString())
    }

    @Test
    fun joinToString() {
        val map = MutableLongObjectMap<String>()
        repeat(5) { map[it.toLong()] = it.toString() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toLong()}=${order[0]}, ${order[1].toLong()}=${order[1]}, " +
                "${order[2].toLong()}=${order[2]}, ${order[3].toLong()}=${order[3]}, " +
                "${order[4].toLong()}=${order[4]}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toLong()}=${order[0]}, ${order[1].toLong()}=${order[1]}, " +
                "${order[2].toLong()}=${order[2]}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toLong()}=${order[0]}-${order[1].toLong()}=${order[1]}-" +
                "${order[2].toLong()}=${order[2]}-${order[3].toLong()}=${order[3]}-" +
                "${order[4].toLong()}=${order[4]}<",
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
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableLongObjectMap<String?>()
        map2[2L] = null

        assertNotEquals(map, map2)

        map2[1L] = "World"
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        assertTrue(map.containsKey(1L))
        assertFalse(map.containsKey(3L))
    }

    @Test
    fun contains() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        assertTrue(1L in map)
        assertFalse(3L in map)
    }

    @Test
    fun containsValue() {
        val map = MutableLongObjectMap<String?>()
        map[1L] = "World"
        map[2L] = null

        assertTrue(map.containsValue("World"))
        assertTrue(map.containsValue(null))
        assertFalse(map.containsValue("Monde"))
    }

    @Test
    fun empty() {
        val map = MutableLongObjectMap<String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1L] = "World"

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableLongObjectMap<String>()
        assertEquals(0, map.count())

        map[1L] = "World"
        assertEquals(1, map.count())

        map[2L] = "Monde"
        map[3L] = "Welt"
        map[4L] = "Sekai"
        map[5L] = "Mondo"
        map[6L] = "Sesang"

        assertEquals(2, map.count { key, _ -> key < 3L })
        assertEquals(0, map.count { key, _ -> key < 0L })
    }

    @Test
    fun any() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"
        map[4L] = "Sekai"
        map[5L] = "Mondo"
        map[6L] = "Sesang"

        assertTrue(map.any { key, _ -> key > 5L })
        assertFalse(map.any { key, _ -> key < 0L })
    }

    @Test
    fun all() {
        val map = MutableLongObjectMap<String>()
        map[1L] = "World"
        map[2L] = "Monde"
        map[3L] = "Welt"
        map[4L] = "Sekai"
        map[5L] = "Mondo"
        map[6L] = "Sesang"

        assertTrue(map.all { key, value -> key < 7L && value.isNotEmpty() })
        assertFalse(map.all { key, _ -> key < 6L })
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableLongObjectMap<String>()

        for (i in 0..1000000) {
            map[i.toLong()] = i.toString()
            map.remove(i.toLong())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
