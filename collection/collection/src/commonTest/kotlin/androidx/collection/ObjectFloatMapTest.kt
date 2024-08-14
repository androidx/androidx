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
class ObjectFloatTest {
    @Test
    fun objectFloatMap() {
        val map = MutableObjectFloatMap<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun emptyObjectFloatMap() {
        val map = emptyObjectFloatMap<String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyObjectFloatMap<String>(), map)
    }

    @Test
    fun objectFloatMapFunction() {
        val map = mutableObjectFloatMapOf<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableObjectFloatMap<String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectFloatMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableObjectFloatMap<String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectFloatMapInitFunction() {
        val map1 =
            objectFloatMapOf(
                "Hello",
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1["Hello"])

        val map2 =
            objectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2["Hello"])
        assertEquals(2f, map2["Bonjour"])

        val map3 =
            objectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3["Hello"])
        assertEquals(2f, map3["Bonjour"])
        assertEquals(3f, map3["Hallo"])

        val map4 =
            objectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
                "Konnichiwa",
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4["Hello"])
        assertEquals(2f, map4["Bonjour"])
        assertEquals(3f, map4["Hallo"])
        assertEquals(4f, map4["Konnichiwa"])

        val map5 =
            objectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
                "Konnichiwa",
                4f,
                "Ciao",
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5["Hello"])
        assertEquals(2f, map5["Bonjour"])
        assertEquals(3f, map5["Hallo"])
        assertEquals(4f, map5["Konnichiwa"])
        assertEquals(5f, map5["Ciao"])
    }

    @Test
    fun mutableObjectFloatMapInitFunction() {
        val map1 =
            mutableObjectFloatMapOf(
                "Hello",
                1f,
            )
        assertEquals(1, map1.size)
        assertEquals(1f, map1["Hello"])

        val map2 =
            mutableObjectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
            )
        assertEquals(2, map2.size)
        assertEquals(1f, map2["Hello"])
        assertEquals(2f, map2["Bonjour"])

        val map3 =
            mutableObjectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
            )
        assertEquals(3, map3.size)
        assertEquals(1f, map3["Hello"])
        assertEquals(2f, map3["Bonjour"])
        assertEquals(3f, map3["Hallo"])

        val map4 =
            mutableObjectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
                "Konnichiwa",
                4f,
            )

        assertEquals(4, map4.size)
        assertEquals(1f, map4["Hello"])
        assertEquals(2f, map4["Bonjour"])
        assertEquals(3f, map4["Hallo"])
        assertEquals(4f, map4["Konnichiwa"])

        val map5 =
            mutableObjectFloatMapOf(
                "Hello",
                1f,
                "Bonjour",
                2f,
                "Hallo",
                3f,
                "Konnichiwa",
                4f,
                "Ciao",
                5f,
            )

        assertEquals(5, map5.size)
        assertEquals(1f, map5["Hello"])
        assertEquals(2f, map5["Bonjour"])
        assertEquals(3f, map5["Hallo"])
        assertEquals(4f, map5["Konnichiwa"])
        assertEquals(5f, map5["Ciao"])
    }

    @Test
    fun addToMap() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map["Hello"])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableObjectFloatMap<String>(12)
        map["Hello"] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map["Hello"])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableObjectFloatMap<String>(2)
        map["Hello"] = 1f

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1f, map["Hello"])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableObjectFloatMap<String>(0)
        map["Hello"] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map["Hello"])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Hello"] = 2f

        assertEquals(1, map.size)
        assertEquals(2f, map["Hello"])
    }

    @Test
    fun put() {
        val map = MutableObjectFloatMap<String>()

        map.put("Hello", 1f)
        assertEquals(1f, map["Hello"])
        map.put("Hello", 2f)
        assertEquals(2f, map["Hello"])
    }

    @Test
    fun putWithDefault() {
        val map = MutableObjectFloatMap<String>()

        var previous = map.put("Hello", 1f, -1f)
        assertEquals(1f, map["Hello"])
        assertEquals(-1f, previous)

        previous = map.put("Hello", 2f, -1f)
        assertEquals(2f, map["Hello"])
        assertEquals(1f, previous)
    }

    @Test
    fun nullKey() {
        val map = MutableObjectFloatMap<String?>()
        map[null] = 1f

        assertEquals(1, map.size)
        assertEquals(1f, map[null])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f

        assertFailsWith<NoSuchElementException> { map["Bonjour"] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f

        assertEquals(2f, map.getOrDefault("Bonjour", 2f))
    }

    @Test
    fun getOrElse() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f

        assertEquals(3f, map.getOrElse("Hallo") { 3f })
    }

    @Test
    fun getOrPut() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f

        var counter = 0
        map.getOrPut("Hello") {
            counter++
            2f
        }
        assertEquals(1f, map["Hello"])
        assertEquals(0, counter)

        map.getOrPut("Bonjour") {
            counter++
            2f
        }
        assertEquals(2f, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Bonjour") {
            counter++
            3f
        }
        assertEquals(2f, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Hallo") {
            counter++
            3f
        }
        assertEquals(3f, map["Hallo"])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableObjectFloatMap<String?>()
        map.remove("Hello")

        map["Hello"] = 1f
        map.remove("Hello")
        assertEquals(0, map.size)

        map[null] = 1f
        map.remove(null)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableObjectFloatMap<String>(6)
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f
        map["Konnichiwa"] = 4f
        map["Ciao"] = 5f
        map["Annyeong"] = 6f

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
        map["Hello"] = 7f

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f
        map["Konnichiwa"] = 4f
        map["Ciao"] = 5f
        map["Annyeong"] = 6f

        map.removeIf { key, _ -> key.startsWith('H') }

        assertEquals(4, map.size)
        assertEquals(2f, map["Bonjour"])
        assertEquals(4f, map["Konnichiwa"])
        assertEquals(5f, map["Ciao"])
        assertEquals(6f, map["Annyeong"])
    }

    @Test
    fun minus() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f

        map -= "Hello"

        assertEquals(2, map.size)
        assertFalse("Hello" in map)
    }

    @Test
    fun minusArray() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f

        map -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusIterable() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f

        map -= listOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusSequence() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f

        map -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableObjectFloatMap<String?>()
        assertFalse(map.remove("Hello", 1f))

        map["Hello"] = 1f
        assertTrue(map.remove("Hello", 1f))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableObjectFloatMap<String>()

        for (i in 0 until 1700) {
            map[i.toString()] = i.toFloat()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableObjectFloatMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toFloat()
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
            val map = MutableObjectFloatMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toFloat()
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
            val map = MutableObjectFloatMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toFloat()
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
        val map = MutableObjectFloatMap<String>()

        for (i in 0 until 32) {
            map[i.toString()] = i.toFloat()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableObjectFloatMap<String?>()
        assertEquals("{}", map.toString())

        map["Hello"] = 1f
        map["Bonjour"] = 2f
        val oneString = 1f.toString()
        val twoString = 2f.toString()
        assertTrue(
            "{Hello=$oneString, Bonjour=$twoString}" == map.toString() ||
                "{Bonjour=$twoString, Hello=$oneString}" == map.toString()
        )

        map.clear()
        map[null] = 2f
        assertEquals("{null=$twoString}", map.toString())

        val selfAsKeyMap = MutableObjectFloatMap<Any>()
        selfAsKeyMap[selfAsKeyMap] = 1f
        assertEquals("{(this)=$oneString}", selfAsKeyMap.toString())
    }

    @Test
    fun joinToString() {
        val map = MutableObjectFloatMap<String?>()
        repeat(5) { map[it.toString()] = it.toFloat() }
        val order = IntArray(5)
        var index = 0
        map.forEach { _, value -> order[index++] = value.toInt() }
        assertEquals(
            "${order[0]}=${order[0].toFloat()}, ${order[1]}=${order[1].toFloat()}, " +
                "${order[2]}=${order[2].toFloat()}, ${order[3]}=${order[3].toFloat()}, " +
                "${order[4]}=${order[4].toFloat()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0]}=${order[0].toFloat()}, ${order[1]}=${order[1].toFloat()}, " +
                "${order[2]}=${order[2].toFloat()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0]}=${order[0].toFloat()}-${order[1]}=${order[1].toFloat()}-" +
                "${order[2]}=${order[2].toFloat()}-${order[3]}=${order[3].toFloat()}-" +
                "${order[4]}=${order[4].toFloat()}<",
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
        val map = MutableObjectFloatMap<String?>()
        map["Hello"] = 1f
        map[null] = 2f

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableObjectFloatMap<String?>()
        map2[null] = 2f

        assertNotEquals(map, map2)

        map2["Hello"] = 1f
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableObjectFloatMap<String?>()
        map["Hello"] = 1f
        map[null] = 2f

        assertTrue(map.containsKey("Hello"))
        assertTrue(map.containsKey(null))
        assertFalse(map.containsKey("Bonjour"))
    }

    @Test
    fun contains() {
        val map = MutableObjectFloatMap<String?>()
        map["Hello"] = 1f
        map[null] = 2f

        assertTrue("Hello" in map)
        assertTrue(null in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun containsValue() {
        val map = MutableObjectFloatMap<String?>()
        map["Hello"] = 1f
        map[null] = 2f

        assertTrue(map.containsValue(1f))
        assertTrue(map.containsValue(2f))
        assertFalse(map.containsValue(3f))
    }

    @Test
    fun empty() {
        val map = MutableObjectFloatMap<String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map["Hello"] = 1f

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableObjectFloatMap<String>()
        assertEquals(0, map.count())

        map["Hello"] = 1f
        assertEquals(1, map.count())

        map["Bonjour"] = 2f
        map["Hallo"] = 3f
        map["Konnichiwa"] = 4f
        map["Ciao"] = 5f
        map["Annyeong"] = 6f

        assertEquals(2, map.count { key, _ -> key.startsWith("H") })
        assertEquals(0, map.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f
        map["Konnichiwa"] = 4f
        map["Ciao"] = 5f
        map["Annyeong"] = 6f

        assertTrue(map.any { key, _ -> key.startsWith("K") })
        assertFalse(map.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val map = MutableObjectFloatMap<String>()
        map["Hello"] = 1f
        map["Bonjour"] = 2f
        map["Hallo"] = 3f
        map["Konnichiwa"] = 4f
        map["Ciao"] = 5f
        map["Annyeong"] = 6f

        assertTrue(map.all { key, value -> key.length >= 4 && value.toInt() >= 1 })
        assertFalse(map.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun trim() {
        val map = MutableObjectFloatMap<String>()
        assertEquals(7, map.trim())

        map["Hello"] = 1f
        map["Hallo"] = 3f

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toString()] = i.toFloat()
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
        val map = MutableObjectFloatMap<Int>()

        for (i in 0..1000000) {
            map[i] = i.toFloat()
            map.remove(i)
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
