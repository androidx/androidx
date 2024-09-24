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

class FloatObjectMapTest {
    @Test
    fun floatObjectMap() {
        val map = MutableFloatObjectMap<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun testEmptyFloatObjectMap() {
        val map = emptyFloatObjectMap<String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyFloatObjectMap<String>(), map)
    }

    @Test
    fun floatObjectMapFunction() {
        val map = mutableFloatObjectMapOf<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableFloatObjectMap<String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatObjectMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableFloatObjectMap<String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun floatObjectMapInitFunction() {
        val map1 =
            floatObjectMapOf(
                1f,
                "World",
            )
        assertEquals(1, map1.size)
        assertEquals("World", map1[1f])

        val map2 =
            floatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
            )
        assertEquals(2, map2.size)
        assertEquals("World", map2[1f])
        assertEquals("Monde", map2[2f])

        val map3 =
            floatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
            )
        assertEquals(3, map3.size)
        assertEquals("World", map3[1f])
        assertEquals("Monde", map3[2f])
        assertEquals("Welt", map3[3f])

        val map4 =
            floatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
                4f,
                "Sekai",
            )

        assertEquals(4, map4.size)
        assertEquals("World", map4[1f])
        assertEquals("Monde", map4[2f])
        assertEquals("Welt", map4[3f])
        assertEquals("Sekai", map4[4f])

        val map5 =
            floatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
                4f,
                "Sekai",
                5f,
                "Mondo",
            )

        assertEquals(5, map5.size)
        assertEquals("World", map5[1f])
        assertEquals("Monde", map5[2f])
        assertEquals("Welt", map5[3f])
        assertEquals("Sekai", map5[4f])
        assertEquals("Mondo", map5[5f])
    }

    @Test
    fun mutableFloatObjectMapInitFunction() {
        val map1 =
            mutableFloatObjectMapOf(
                1f,
                "World",
            )
        assertEquals(1, map1.size)
        assertEquals("World", map1[1f])

        val map2 =
            mutableFloatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
            )
        assertEquals(2, map2.size)
        assertEquals("World", map2[1f])
        assertEquals("Monde", map2[2f])

        val map3 =
            mutableFloatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
            )
        assertEquals(3, map3.size)
        assertEquals("World", map3[1f])
        assertEquals("Monde", map3[2f])
        assertEquals("Welt", map3[3f])

        val map4 =
            mutableFloatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
                4f,
                "Sekai",
            )

        assertEquals(4, map4.size)
        assertEquals("World", map4[1f])
        assertEquals("Monde", map4[2f])
        assertEquals("Welt", map4[3f])
        assertEquals("Sekai", map4[4f])

        val map5 =
            mutableFloatObjectMapOf(
                1f,
                "World",
                2f,
                "Monde",
                3f,
                "Welt",
                4f,
                "Sekai",
                5f,
                "Mondo",
            )

        assertEquals(5, map5.size)
        assertEquals("World", map5[1f])
        assertEquals("Monde", map5[2f])
        assertEquals("Welt", map5[3f])
        assertEquals("Sekai", map5[4f])
        assertEquals("Mondo", map5[5f])
    }

    @Test
    fun buildFloatObjectMapFunction() {
        val contract: Boolean
        val map = buildFloatObjectMap {
            contract = true
            put(1f, "World")
            put(2f, "Monde")
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals("World", map[1f])
        assertEquals("Monde", map[2f])
    }

    @Test
    fun buildFloatObjectMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildFloatObjectMap(20) {
                contract = true
                put(1f, "World")
                put(2f, "Monde")
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals("World", map[1f])
        assertEquals("Monde", map[2f])
    }

    @Test
    fun addToMap() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1f])
    }

    @Test
    fun insertIndex0() {
        val map = MutableFloatObjectMap<String>()
        map.put(1f, "World")
        assertEquals("World", map[1f])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableFloatObjectMap<String>(12)
        map[1f] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1f])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableFloatObjectMap<String>(2)
        map[1f] = "World"

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals("World", map[1f])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableFloatObjectMap<String>(0)
        map[1f] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[1f])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[1f] = "Monde"

        assertEquals(1, map.size)
        assertEquals("Monde", map[1f])
    }

    @Test
    fun put() {
        val map = MutableFloatObjectMap<String?>()

        assertNull(map.put(1f, "World"))
        assertEquals("World", map.put(1f, "Monde"))
        assertNull(map.put(2f, null))
        assertNull(map.put(2f, "Monde"))
    }

    @Test
    fun putAllMap() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        map.putAll(mutableFloatObjectMapOf(3f, "Welt", 7f, "Mundo"))

        assertEquals(4, map.size)
        assertEquals("Welt", map[3f])
        assertEquals("Mundo", map[7f])
    }

    @Test
    fun plusMap() {
        val map = MutableFloatObjectMap<String>()
        map += floatObjectMapOf(3f, "Welt", 7f, "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map[3f])
        assertEquals("Mundo", map[7f])
    }

    @Test
    fun nullValue() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = null

        assertEquals(1, map.size)
        assertNull(map[1f])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"

        assertNull(map[2f])
    }

    @Test
    fun getOrDefault() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"

        assertEquals("Monde", map.getOrDefault(2f, "Monde"))
    }

    @Test
    fun getOrElse() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        assertEquals("Monde", map.getOrElse(2f) { "Monde" })
        assertEquals("Welt", map.getOrElse(3f) { "Welt" })
    }

    @Test
    fun getOrPut() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"

        var counter = 0
        map.getOrPut(1f) {
            counter++
            "Monde"
        }
        assertEquals("World", map[1f])
        assertEquals(0, counter)

        map.getOrPut(2f) {
            counter++
            "Monde"
        }
        assertEquals("Monde", map[2f])
        assertEquals(1, counter)

        map.getOrPut(2f) {
            counter++
            "Welt"
        }
        assertEquals("Monde", map[2f])
        assertEquals(1, counter)

        map.getOrPut(3f) {
            counter++
            null
        }
        assertNull(map[3f])
        assertEquals(2, counter)

        map.getOrPut(3f) {
            counter++
            "Welt"
        }
        assertEquals("Welt", map[3f])
        assertEquals(3, counter)
    }

    @Test
    fun remove() {
        val map = MutableFloatObjectMap<String?>()
        assertNull(map.remove(1f))

        map[1f] = "World"
        assertEquals("World", map.remove(1f))
        assertEquals(0, map.size)

        map[1f] = null
        assertNull(map.remove(1f))
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableFloatObjectMap<String>(6)
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"
        map[4f] = "Sekai"
        map[5f] = "Mondo"
        map[6f] = "Sesang"

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
        map[1f] = "Mundo"

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"
        map[4f] = "Sekai"
        map[5f] = "Mondo"
        map[6f] = "Sesang"

        map.removeIf { key, value -> key == 1f || key == 3f || value.startsWith('S') }

        assertEquals(2, map.size)
        assertEquals("Monde", map[2f])
        assertEquals("Mondo", map[5f])
    }

    @Test
    fun minus() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"

        map -= 1f

        assertEquals(2, map.size)
        assertNull(map[1f])
    }

    @Test
    fun minusArray() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"

        map -= floatArrayOf(3f, 2f)

        assertEquals(1, map.size)
        assertNull(map[3f])
        assertNull(map[2f])
    }

    @Test
    fun minusSet() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"

        map -= floatSetOf(3f, 2f)

        assertEquals(1, map.size)
        assertNull(map[3f])
        assertNull(map[2f])
    }

    @Test
    fun minusList() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"

        map -= floatListOf(3f, 2f)

        assertEquals(1, map.size)
        assertNull(map[3f])
        assertNull(map[2f])
    }

    @Test
    fun conditionalRemove() {
        val map = MutableFloatObjectMap<String?>()
        assertFalse(map.remove(1f, "World"))

        map[1f] = "World"
        assertTrue(map.remove(1f, "World"))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableFloatObjectMap<String>()

        for (i in 0 until 1700) {
            map[i.toFloat()] = i.toString()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableFloatObjectMap<String>()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toString()
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
            val map = MutableFloatObjectMap<String>()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toString()
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
            val map = MutableFloatObjectMap<String>()

            for (j in 0 until i) {
                map[j.toFloat()] = j.toString()
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
        val map = MutableFloatObjectMap<String>()

        for (i in 0 until 32) {
            map[i.toFloat()] = i.toString()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableFloatObjectMap<String?>()
        assertEquals("{}", map.toString())

        map[1f] = "World"
        map[2f] = "Monde"
        val oneKey = 1f.toString()
        val twoKey = 2f.toString()
        assertTrue(
            "{$oneKey=World, $twoKey=Monde}" == map.toString() ||
                "{$twoKey=Monde, $oneKey=World}" == map.toString()
        )

        map.clear()
        map[1f] = null
        assertEquals("{$oneKey=null}", map.toString())

        val selfAsValueMap = MutableFloatObjectMap<Any>()
        selfAsValueMap[1f] = selfAsValueMap
        assertEquals("{$oneKey=(this)}", selfAsValueMap.toString())
    }

    @Test
    fun joinToString() {
        val map = MutableFloatObjectMap<String>()
        repeat(5) { map[it.toFloat()] = it.toString() }
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key.toInt() }
        assertEquals(
            "${order[0].toFloat()}=${order[0]}, ${order[1].toFloat()}=${order[1]}, " +
                "${order[2].toFloat()}=${order[2]}, ${order[3].toFloat()}=${order[3]}, " +
                "${order[4].toFloat()}=${order[4]}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0].toFloat()}=${order[0]}, ${order[1].toFloat()}=${order[1]}, " +
                "${order[2].toFloat()}=${order[2]}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0].toFloat()}=${order[0]}-${order[1].toFloat()}=${order[1]}-" +
                "${order[2].toFloat()}=${order[2]}-${order[3].toFloat()}=${order[3]}-" +
                "${order[4].toFloat()}=${order[4]}<",
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
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableFloatObjectMap<String?>()
        map2[2f] = null

        assertNotEquals(map, map2)

        map2[1f] = "World"
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        assertTrue(map.containsKey(1f))
        assertFalse(map.containsKey(3f))
    }

    @Test
    fun contains() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        assertTrue(1f in map)
        assertFalse(3f in map)
    }

    @Test
    fun containsValue() {
        val map = MutableFloatObjectMap<String?>()
        map[1f] = "World"
        map[2f] = null

        assertTrue(map.containsValue("World"))
        assertTrue(map.containsValue(null))
        assertFalse(map.containsValue("Monde"))
    }

    @Test
    fun empty() {
        val map = MutableFloatObjectMap<String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map[1f] = "World"

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableFloatObjectMap<String>()
        assertEquals(0, map.count())

        map[1f] = "World"
        assertEquals(1, map.count())

        map[2f] = "Monde"
        map[3f] = "Welt"
        map[4f] = "Sekai"
        map[5f] = "Mondo"
        map[6f] = "Sesang"

        assertEquals(2, map.count { key, _ -> key < 3f })
        assertEquals(0, map.count { key, _ -> key < 0f })
    }

    @Test
    fun any() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"
        map[4f] = "Sekai"
        map[5f] = "Mondo"
        map[6f] = "Sesang"

        assertTrue(map.any { key, _ -> key > 5f })
        assertFalse(map.any { key, _ -> key < 0f })
    }

    @Test
    fun all() {
        val map = MutableFloatObjectMap<String>()
        map[1f] = "World"
        map[2f] = "Monde"
        map[3f] = "Welt"
        map[4f] = "Sekai"
        map[5f] = "Mondo"
        map[6f] = "Sesang"

        assertTrue(map.all { key, value -> key < 7f && value.isNotEmpty() })
        assertFalse(map.all { key, _ -> key < 6f })
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableFloatObjectMap<String>()

        for (i in 0..1000000) {
            map[i.toFloat()] = i.toString()
            map.remove(i.toFloat())
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
