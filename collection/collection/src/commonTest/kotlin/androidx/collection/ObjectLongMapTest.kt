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
import kotlin.test.assertNotNull
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
class ObjectLongTest {
    @Test
    fun objectLongMap() {
        val map = MutableObjectLongMap<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun emptyObjectLongMap() {
        val map = emptyObjectLongMap<String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyObjectLongMap<String>(), map)
    }

    @Test
    fun objectLongMapFunction() {
        val map = mutableObjectLongMapOf<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableObjectLongMap<String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectLongMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableObjectLongMap<String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectLongMapInitFunction() {
        val map1 =
            objectLongMapOf(
                "Hello",
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1["Hello"])

        val map2 =
            objectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2["Hello"])
        assertEquals(2L, map2["Bonjour"])

        val map3 =
            objectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3["Hello"])
        assertEquals(2L, map3["Bonjour"])
        assertEquals(3L, map3["Hallo"])

        val map4 =
            objectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
                "Konnichiwa",
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4["Hello"])
        assertEquals(2L, map4["Bonjour"])
        assertEquals(3L, map4["Hallo"])
        assertEquals(4L, map4["Konnichiwa"])

        val map5 =
            objectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
                "Konnichiwa",
                4L,
                "Ciao",
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5["Hello"])
        assertEquals(2L, map5["Bonjour"])
        assertEquals(3L, map5["Hallo"])
        assertEquals(4L, map5["Konnichiwa"])
        assertEquals(5L, map5["Ciao"])
    }

    @Test
    fun mutableObjectLongMapInitFunction() {
        val map1 =
            mutableObjectLongMapOf(
                "Hello",
                1L,
            )
        assertEquals(1, map1.size)
        assertEquals(1L, map1["Hello"])

        val map2 =
            mutableObjectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
            )
        assertEquals(2, map2.size)
        assertEquals(1L, map2["Hello"])
        assertEquals(2L, map2["Bonjour"])

        val map3 =
            mutableObjectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
            )
        assertEquals(3, map3.size)
        assertEquals(1L, map3["Hello"])
        assertEquals(2L, map3["Bonjour"])
        assertEquals(3L, map3["Hallo"])

        val map4 =
            mutableObjectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
                "Konnichiwa",
                4L,
            )

        assertEquals(4, map4.size)
        assertEquals(1L, map4["Hello"])
        assertEquals(2L, map4["Bonjour"])
        assertEquals(3L, map4["Hallo"])
        assertEquals(4L, map4["Konnichiwa"])

        val map5 =
            mutableObjectLongMapOf(
                "Hello",
                1L,
                "Bonjour",
                2L,
                "Hallo",
                3L,
                "Konnichiwa",
                4L,
                "Ciao",
                5L,
            )

        assertEquals(5, map5.size)
        assertEquals(1L, map5["Hello"])
        assertEquals(2L, map5["Bonjour"])
        assertEquals(3L, map5["Hallo"])
        assertEquals(4L, map5["Konnichiwa"])
        assertEquals(5L, map5["Ciao"])
    }

    @Test
    fun addToMap() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map["Hello"])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableObjectLongMap<String>(12)
        map["Hello"] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map["Hello"])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableObjectLongMap<String>(2)
        map["Hello"] = 1L

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1L, map["Hello"])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableObjectLongMap<String>(0)
        map["Hello"] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map["Hello"])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Hello"] = 2L

        assertEquals(1, map.size)
        assertEquals(2L, map["Hello"])
    }

    @Test
    fun put() {
        val map = MutableObjectLongMap<String>()

        map.put("Hello", 1L)
        assertEquals(1L, map["Hello"])
        map.put("Hello", 2L)
        assertEquals(2L, map["Hello"])
    }

    @Test
    fun putWithDefault() {
        val map = MutableObjectLongMap<String>()

        var previous = map.put("Hello", 1L, -1L)
        assertEquals(1L, map["Hello"])
        assertEquals(-1L, previous)

        previous = map.put("Hello", 2L, -1L)
        assertEquals(2L, map["Hello"])
        assertEquals(1L, previous)
    }

    @Test
    fun nullKey() {
        val map = MutableObjectLongMap<String?>()
        map[null] = 1L

        assertEquals(1, map.size)
        assertEquals(1L, map[null])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L

        assertFailsWith<NoSuchElementException> { map["Bonjour"] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L

        assertEquals(2L, map.getOrDefault("Bonjour", 2L))
    }

    @Test
    fun getOrElse() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L

        assertEquals(3L, map.getOrElse("Hallo") { 3L })
    }

    @Test
    fun getOrPut() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L

        var counter = 0
        map.getOrPut("Hello") {
            counter++
            2L
        }
        assertEquals(1L, map["Hello"])
        assertEquals(0, counter)

        map.getOrPut("Bonjour") {
            counter++
            2L
        }
        assertEquals(2L, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Bonjour") {
            counter++
            3L
        }
        assertEquals(2L, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Hallo") {
            counter++
            3L
        }
        assertEquals(3L, map["Hallo"])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableObjectLongMap<String?>()
        map.remove("Hello")

        map["Hello"] = 1L
        map.remove("Hello")
        assertEquals(0, map.size)

        map[null] = 1L
        map.remove(null)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableObjectLongMap<String>(6)
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L
        map["Konnichiwa"] = 4L
        map["Ciao"] = 5L
        map["Annyeong"] = 6L

        // Removing all the entries will mark the medata as deleted
        map.remove("Hello")
        map.remove("Bonjour")
        map.remove("Hallo")
        map.remove("Konnichiwa")
        map.remove("Ciao")
        map.remove("Annyeong")

        assertEquals(0, map.size)

        val capacity = map.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        map["Hello"] = 7L

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L
        map["Konnichiwa"] = 4L
        map["Ciao"] = 5L
        map["Annyeong"] = 6L

        map.removeIf { key, _ -> key.startsWith('H') }

        assertEquals(4, map.size)
        assertEquals(2L, map["Bonjour"])
        assertEquals(4L, map["Konnichiwa"])
        assertEquals(5L, map["Ciao"])
        assertEquals(6L, map["Annyeong"])
    }

    @Test
    fun minus() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L

        map -= "Hello"

        assertEquals(2, map.size)
        assertFalse("Hello" in map)
    }

    @Test
    fun minusArray() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L

        map -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusIterable() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L

        map -= listOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusSequence() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L

        map -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableObjectLongMap<String?>()
        assertFalse(map.remove("Hello", 1L))

        map["Hello"] = 1L
        assertTrue(map.remove("Hello", 1L))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableObjectLongMap<String>()

        for (i in 0 until 1700) {
            map[i.toString()] = i.toLong()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableObjectLongMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toLong()
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value.toInt().toString())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableObjectLongMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toLong()
            }

            var counter = 0
            map.forEachKey { key ->
                assertNotNull(key.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachValue() {
        for (i in 0..48) {
            val map = MutableObjectLongMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toLong()
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
        val map = MutableObjectLongMap<String>()

        for (i in 0 until 32) {
            map[i.toString()] = i.toLong()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableObjectLongMap<String?>()
        assertEquals("{}", map.toString())

        map["Hello"] = 1L
        map["Bonjour"] = 2L
        val oneString = 1L.toString()
        val twoString = 2L.toString()
        assertTrue(
            "{Hello=$oneString, Bonjour=$twoString}" == map.toString() ||
                "{Bonjour=$twoString, Hello=$oneString}" == map.toString()
        )

        map.clear()
        map[null] = 2L
        assertEquals("{null=$twoString}", map.toString())

        val selfAsKeyMap = MutableObjectLongMap<Any>()
        selfAsKeyMap[selfAsKeyMap] = 1L
        assertEquals("{(this)=$oneString}", selfAsKeyMap.toString())
    }

    @Test
    fun joinToString() {
        val map = MutableObjectLongMap<String?>()
        repeat(5) { map[it.toString()] = it.toLong() }
        val order = IntArray(5)
        var index = 0
        map.forEach { _, value -> order[index++] = value.toInt() }
        assertEquals(
            "${order[0]}=${order[0].toLong()}, ${order[1]}=${order[1].toLong()}, " +
                "${order[2]}=${order[2].toLong()}, ${order[3]}=${order[3].toLong()}, " +
                "${order[4]}=${order[4].toLong()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0]}=${order[0].toLong()}, ${order[1]}=${order[1].toLong()}, " +
                "${order[2]}=${order[2].toLong()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0]}=${order[0].toLong()}-${order[1]}=${order[1].toLong()}-" +
                "${order[2]}=${order[2].toLong()}-${order[3]}=${order[3].toLong()}-" +
                "${order[4]}=${order[4].toLong()}<",
            map.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            map.joinToString(limit = 3) { _, value -> names[value.toInt()] }
        )
    }

    @Test
    fun equalsTest() {
        val map = MutableObjectLongMap<String?>()
        map["Hello"] = 1L
        map[null] = 2L

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableObjectLongMap<String?>()
        map2[null] = 2L

        assertNotEquals(map, map2)

        map2["Hello"] = 1L
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableObjectLongMap<String?>()
        map["Hello"] = 1L
        map[null] = 2L

        assertTrue(map.containsKey("Hello"))
        assertTrue(map.containsKey(null))
        assertFalse(map.containsKey("Bonjour"))
    }

    @Test
    fun contains() {
        val map = MutableObjectLongMap<String?>()
        map["Hello"] = 1L
        map[null] = 2L

        assertTrue("Hello" in map)
        assertTrue(null in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun containsValue() {
        val map = MutableObjectLongMap<String?>()
        map["Hello"] = 1L
        map[null] = 2L

        assertTrue(map.containsValue(1L))
        assertTrue(map.containsValue(2L))
        assertFalse(map.containsValue(3L))
    }

    @Test
    fun empty() {
        val map = MutableObjectLongMap<String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map["Hello"] = 1L

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableObjectLongMap<String>()
        assertEquals(0, map.count())

        map["Hello"] = 1L
        assertEquals(1, map.count())

        map["Bonjour"] = 2L
        map["Hallo"] = 3L
        map["Konnichiwa"] = 4L
        map["Ciao"] = 5L
        map["Annyeong"] = 6L

        assertEquals(2, map.count { key, _ -> key.startsWith("H") })
        assertEquals(0, map.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L
        map["Konnichiwa"] = 4L
        map["Ciao"] = 5L
        map["Annyeong"] = 6L

        assertTrue(map.any { key, _ -> key.startsWith("K") })
        assertFalse(map.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val map = MutableObjectLongMap<String>()
        map["Hello"] = 1L
        map["Bonjour"] = 2L
        map["Hallo"] = 3L
        map["Konnichiwa"] = 4L
        map["Ciao"] = 5L
        map["Annyeong"] = 6L

        assertTrue(map.all { key, value -> key.length >= 4 && value.toInt() >= 1 })
        assertFalse(map.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun trim() {
        val map = MutableObjectLongMap<String>()
        assertEquals(7, map.trim())

        map["Hello"] = 1L
        map["Hallo"] = 3L

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toString()] = i.toLong()
        }

        assertEquals(2047, map.capacity)

        // After removing these items, our capacity needs should go
        // from 2047 down to 1023
        for (i in 0 until 1700) {
            if (i and 0x1 == 0x0) {
                val s = i.toString()
                map.remove(s)
            }
        }

        assertEquals(1024, map.trim())
        assertEquals(0, map.trim())
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableObjectLongMap<Int>()

        for (i in 0..1000000) {
            map[i] = i.toLong()
            map.remove(i)
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
