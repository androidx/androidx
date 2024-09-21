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
class ObjectIntTest {
    @Test
    fun objectIntMap() {
        val map = MutableObjectIntMap<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun emptyObjectIntMap() {
        val map = emptyObjectIntMap<String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyObjectIntMap<String>(), map)
    }

    @Test
    fun objectIntMapFunction() {
        val map = mutableObjectIntMapOf<String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityHashMap() {
        val map = MutableObjectIntMap<String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectIntMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableObjectIntMap<String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun objectIntMapInitFunction() {
        val map1 =
            objectIntMapOf(
                "Hello",
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1["Hello"])

        val map2 =
            objectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2["Hello"])
        assertEquals(2, map2["Bonjour"])

        val map3 =
            objectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3["Hello"])
        assertEquals(2, map3["Bonjour"])
        assertEquals(3, map3["Hallo"])

        val map4 =
            objectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
                "Konnichiwa",
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4["Hello"])
        assertEquals(2, map4["Bonjour"])
        assertEquals(3, map4["Hallo"])
        assertEquals(4, map4["Konnichiwa"])

        val map5 =
            objectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
                "Konnichiwa",
                4,
                "Ciao",
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5["Hello"])
        assertEquals(2, map5["Bonjour"])
        assertEquals(3, map5["Hallo"])
        assertEquals(4, map5["Konnichiwa"])
        assertEquals(5, map5["Ciao"])
    }

    @Test
    fun mutableObjectIntMapInitFunction() {
        val map1 =
            mutableObjectIntMapOf(
                "Hello",
                1,
            )
        assertEquals(1, map1.size)
        assertEquals(1, map1["Hello"])

        val map2 =
            mutableObjectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
            )
        assertEquals(2, map2.size)
        assertEquals(1, map2["Hello"])
        assertEquals(2, map2["Bonjour"])

        val map3 =
            mutableObjectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
            )
        assertEquals(3, map3.size)
        assertEquals(1, map3["Hello"])
        assertEquals(2, map3["Bonjour"])
        assertEquals(3, map3["Hallo"])

        val map4 =
            mutableObjectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
                "Konnichiwa",
                4,
            )

        assertEquals(4, map4.size)
        assertEquals(1, map4["Hello"])
        assertEquals(2, map4["Bonjour"])
        assertEquals(3, map4["Hallo"])
        assertEquals(4, map4["Konnichiwa"])

        val map5 =
            mutableObjectIntMapOf(
                "Hello",
                1,
                "Bonjour",
                2,
                "Hallo",
                3,
                "Konnichiwa",
                4,
                "Ciao",
                5,
            )

        assertEquals(5, map5.size)
        assertEquals(1, map5["Hello"])
        assertEquals(2, map5["Bonjour"])
        assertEquals(3, map5["Hallo"])
        assertEquals(4, map5["Konnichiwa"])
        assertEquals(5, map5["Ciao"])
    }

    @Test
    fun buildObjectIntMapFunction() {
        val contract: Boolean
        val map = buildObjectIntMap {
            contract = true
            put("Hello", 1)
            put("Bonjour", 2)
        }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertEquals(1, map["Hello"])
        assertEquals(2, map["Bonjour"])
    }

    @Test
    fun buildObjectIntMapWithCapacityFunction() {
        val contract: Boolean
        val map =
            buildObjectIntMap(20) {
                contract = true
                put("Hello", 1)
                put("Bonjour", 2)
            }
        assertTrue(contract)
        assertEquals(2, map.size)
        assertTrue(map.capacity >= 18)
        assertEquals(1, map["Hello"])
        assertEquals(2, map["Bonjour"])
    }

    @Test
    fun addToMap() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1

        assertEquals(1, map.size)
        assertEquals(1, map["Hello"])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableObjectIntMap<String>(12)
        map["Hello"] = 1

        assertEquals(1, map.size)
        assertEquals(1, map["Hello"])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableObjectIntMap<String>(2)
        map["Hello"] = 1

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals(1, map["Hello"])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableObjectIntMap<String>(0)
        map["Hello"] = 1

        assertEquals(1, map.size)
        assertEquals(1, map["Hello"])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Hello"] = 2

        assertEquals(1, map.size)
        assertEquals(2, map["Hello"])
    }

    @Test
    fun put() {
        val map = MutableObjectIntMap<String>()

        map.put("Hello", 1)
        assertEquals(1, map["Hello"])
        map.put("Hello", 2)
        assertEquals(2, map["Hello"])
    }

    @Test
    fun putWithDefault() {
        val map = MutableObjectIntMap<String>()

        var previous = map.put("Hello", 1, -1)
        assertEquals(1, map["Hello"])
        assertEquals(-1, previous)

        previous = map.put("Hello", 2, -1)
        assertEquals(2, map["Hello"])
        assertEquals(1, previous)
    }

    @Test
    fun nullKey() {
        val map = MutableObjectIntMap<String?>()
        map[null] = 1

        assertEquals(1, map.size)
        assertEquals(1, map[null])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1

        assertFailsWith<NoSuchElementException> { map["Bonjour"] }
    }

    @Test
    fun getOrDefault() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1

        assertEquals(2, map.getOrDefault("Bonjour", 2))
    }

    @Test
    fun getOrElse() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1

        assertEquals(3, map.getOrElse("Hallo") { 3 })
    }

    @Test
    fun getOrPut() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1

        var counter = 0
        map.getOrPut("Hello") {
            counter++
            2
        }
        assertEquals(1, map["Hello"])
        assertEquals(0, counter)

        map.getOrPut("Bonjour") {
            counter++
            2
        }
        assertEquals(2, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Bonjour") {
            counter++
            3
        }
        assertEquals(2, map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Hallo") {
            counter++
            3
        }
        assertEquals(3, map["Hallo"])
        assertEquals(2, counter)
    }

    @Test
    fun remove() {
        val map = MutableObjectIntMap<String?>()
        map.remove("Hello")

        map["Hello"] = 1
        map.remove("Hello")
        assertEquals(0, map.size)

        map[null] = 1
        map.remove(null)
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableObjectIntMap<String>(6)
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3
        map["Konnichiwa"] = 4
        map["Ciao"] = 5
        map["Annyeong"] = 6

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
        map["Hello"] = 7

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3
        map["Konnichiwa"] = 4
        map["Ciao"] = 5
        map["Annyeong"] = 6

        map.removeIf { key, _ -> key.startsWith('H') }

        assertEquals(4, map.size)
        assertEquals(2, map["Bonjour"])
        assertEquals(4, map["Konnichiwa"])
        assertEquals(5, map["Ciao"])
        assertEquals(6, map["Annyeong"])
    }

    @Test
    fun minus() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3

        map -= "Hello"

        assertEquals(2, map.size)
        assertFalse("Hello" in map)
    }

    @Test
    fun minusArray() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3

        map -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusIterable() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3

        map -= listOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun minusSequence() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3

        map -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, map.size)
        assertFalse("Hallo" in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun conditionalRemove() {
        val map = MutableObjectIntMap<String?>()
        assertFalse(map.remove("Hello", 1))

        map["Hello"] = 1
        assertTrue(map.remove("Hello", 1))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableObjectIntMap<String>()

        for (i in 0 until 1700) {
            map[i.toString()] = i.toInt()
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableObjectIntMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toInt()
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
            val map = MutableObjectIntMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toInt()
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
            val map = MutableObjectIntMap<String>()

            for (j in 0 until i) {
                map[j.toString()] = j.toInt()
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
        val map = MutableObjectIntMap<String>()

        for (i in 0 until 32) {
            map[i.toString()] = i.toInt()
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun string() {
        val map = MutableObjectIntMap<String?>()
        assertEquals("{}", map.toString())

        map["Hello"] = 1
        map["Bonjour"] = 2
        val oneString = 1.toString()
        val twoString = 2.toString()
        assertTrue(
            "{Hello=$oneString, Bonjour=$twoString}" == map.toString() ||
                "{Bonjour=$twoString, Hello=$oneString}" == map.toString()
        )

        map.clear()
        map[null] = 2
        assertEquals("{null=$twoString}", map.toString())

        val selfAsKeyMap = MutableObjectIntMap<Any>()
        selfAsKeyMap[selfAsKeyMap] = 1
        assertEquals("{(this)=$oneString}", selfAsKeyMap.toString())
    }

    @Test
    fun joinToString() {
        val map = MutableObjectIntMap<String?>()
        repeat(5) { map[it.toString()] = it.toInt() }
        val order = IntArray(5)
        var index = 0
        map.forEach { _, value -> order[index++] = value.toInt() }
        assertEquals(
            "${order[0]}=${order[0].toInt()}, ${order[1]}=${order[1].toInt()}, " +
                "${order[2]}=${order[2].toInt()}, ${order[3]}=${order[3].toInt()}, " +
                "${order[4]}=${order[4].toInt()}",
            map.joinToString()
        )
        assertEquals(
            "x${order[0]}=${order[0].toInt()}, ${order[1]}=${order[1].toInt()}, " +
                "${order[2]}=${order[2].toInt()}...",
            map.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0]}=${order[0].toInt()}-${order[1]}=${order[1].toInt()}-" +
                "${order[2]}=${order[2].toInt()}-${order[3]}=${order[3].toInt()}-" +
                "${order[4]}=${order[4].toInt()}<",
            map.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            map.joinToString(limit = 3) { _, value -> names[value.toInt()] }
        )
    }

    @Test
    fun equals() {
        val map = MutableObjectIntMap<String?>()
        map["Hello"] = 1
        map[null] = 2

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableObjectIntMap<String?>()
        map2[null] = 2

        assertNotEquals(map, map2)

        map2["Hello"] = 1
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableObjectIntMap<String?>()
        map["Hello"] = 1
        map[null] = 2

        assertTrue(map.containsKey("Hello"))
        assertTrue(map.containsKey(null))
        assertFalse(map.containsKey("Bonjour"))
    }

    @Test
    fun contains() {
        val map = MutableObjectIntMap<String?>()
        map["Hello"] = 1
        map[null] = 2

        assertTrue("Hello" in map)
        assertTrue(null in map)
        assertFalse("Bonjour" in map)
    }

    @Test
    fun containsValue() {
        val map = MutableObjectIntMap<String?>()
        map["Hello"] = 1
        map[null] = 2

        assertTrue(map.containsValue(1))
        assertTrue(map.containsValue(2))
        assertFalse(map.containsValue(3))
    }

    @Test
    fun empty() {
        val map = MutableObjectIntMap<String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map["Hello"] = 1

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableObjectIntMap<String>()
        assertEquals(0, map.count())

        map["Hello"] = 1
        assertEquals(1, map.count())

        map["Bonjour"] = 2
        map["Hallo"] = 3
        map["Konnichiwa"] = 4
        map["Ciao"] = 5
        map["Annyeong"] = 6

        assertEquals(2, map.count { key, _ -> key.startsWith("H") })
        assertEquals(0, map.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3
        map["Konnichiwa"] = 4
        map["Ciao"] = 5
        map["Annyeong"] = 6

        assertTrue(map.any { key, _ -> key.startsWith("K") })
        assertFalse(map.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val map = MutableObjectIntMap<String>()
        map["Hello"] = 1
        map["Bonjour"] = 2
        map["Hallo"] = 3
        map["Konnichiwa"] = 4
        map["Ciao"] = 5
        map["Annyeong"] = 6

        assertTrue(map.all { key, value -> key.length >= 4 && value.toInt() >= 1 })
        assertFalse(map.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun trim() {
        val map = MutableObjectIntMap<String>()
        assertEquals(7, map.trim())

        map["Hello"] = 1
        map["Hallo"] = 3

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            map[i.toString()] = i.toInt()
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
        val map = MutableObjectIntMap<Int>()

        for (i in 0..1000000) {
            map[i] = i.toInt()
            map.remove(i)
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }
}
