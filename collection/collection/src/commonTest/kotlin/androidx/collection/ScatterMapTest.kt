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
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class ScatterMapTest {
    @Test
    fun scatterMap() {
        val map = MutableScatterMap<String, String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun emptyScatterMap() {
        val map = emptyScatterMap<String, String>()
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)

        assertSame(emptyScatterMap<String, String>(), map)
    }

    @Test
    fun scatterMapFunction() {
        val map = mutableScatterMapOf<String, String>()
        assertEquals(7, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun zeroCapacityMap() {
        val map = MutableScatterMap<String, String>(0)
        assertEquals(0, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun scatterMapWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val map = MutableScatterMap<String, String>(1800)
        assertEquals(4095, map.capacity)
        assertEquals(0, map.size)
    }

    @Test
    fun scatterMapPairsFunction() {
        val map = mutableScatterMapOf("Hello" to "World", "Bonjour" to "Monde")
        assertEquals(2, map.size)
        assertEquals("World", map["Hello"])
        assertEquals("Monde", map["Bonjour"])
    }

    @Test
    fun addToMap() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun insertIndex0() {
        val map = MutableScatterMap<Float, Long>()
        map.put(1f, 100L)
        assertEquals(100L, map[1f])
    }

    @Test
    fun addToSizedMap() {
        val map = MutableScatterMap<String, String>(12)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun addToSmallMap() {
        val map = MutableScatterMap<String, String>(2)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals(7, map.capacity)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun addToZeroCapacityMap() {
        val map = MutableScatterMap<String, String>(0)
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun replaceExistingKey() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Hello"] = "Monde"

        assertEquals(1, map.size)
        assertEquals("Monde", map["Hello"])
    }

    @Test
    fun put() {
        val map = MutableScatterMap<String, String?>()

        assertNull(map.put("Hello", "World"))
        assertEquals("World", map.put("Hello", "Monde"))
        assertNull(map.put("Bonjour", null))
        assertNull(map.put("Bonjour", "Monde"))
    }

    @Test
    fun putAllMap() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(mapOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllArray() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(arrayOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllIterable() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(listOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun putAllSequence() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        map.putAll(listOf("Hallo" to "Welt", "Hola" to "Mundo").asSequence())

        assertEquals(5, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plus() {
        val map = MutableScatterMap<String, String>()
        map += "Hello" to "World"

        assertEquals(1, map.size)
        assertEquals("World", map["Hello"])
    }

    @Test
    fun plusMap() {
        val map = MutableScatterMap<String, String>()
        map += mapOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusArray() {
        val map = MutableScatterMap<String, String>()
        map += arrayOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusIterable() {
        val map = MutableScatterMap<String, String>()
        map += listOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun plusSequence() {
        val map = MutableScatterMap<String, String>()
        map += listOf("Hallo" to "Welt", "Hola" to "Mundo").asSequence()

        assertEquals(2, map.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun nullKey() {
        val map = MutableScatterMap<String?, String>()
        map[null] = "World"

        assertEquals(1, map.size)
        assertEquals("World", map[null])
    }

    @Test
    fun nullValue() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = null

        assertEquals(1, map.size)
        assertNull(map["Hello"])
    }

    @Test
    fun findNonExistingKey() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"

        assertNull(map["Bonjour"])
    }

    @Test
    fun getOrDefault() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"

        assertEquals("Monde", map.getOrDefault("Bonjour", "Monde"))
    }

    @Test
    fun getOrElse() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"
        map["Bonjour"] = null

        assertEquals("Monde", map.getOrElse("Bonjour") { "Monde" })
        assertEquals("Welt", map.getOrElse("Hallo") { "Welt" })
    }

    @Test
    fun getOrPut() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"

        var counter = 0
        map.getOrPut("Hello") {
            counter++
            "Monde"
        }
        assertEquals("World", map["Hello"])
        assertEquals(0, counter)

        map.getOrPut("Bonjour") {
            counter++
            "Monde"
        }
        assertEquals("Monde", map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Bonjour") {
            counter++
            "Welt"
        }
        assertEquals("Monde", map["Bonjour"])
        assertEquals(1, counter)

        map.getOrPut("Hallo") {
            counter++
            null
        }
        assertNull(map["Hallo"])
        assertEquals(2, counter)

        map.getOrPut("Hallo") {
            counter++
            "Welt"
        }
        assertEquals("Welt", map["Hallo"])
        assertEquals(3, counter)
    }

    @Test
    fun compute() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"

        var computed = map.compute("Hello") { _, _ -> "New World" }
        assertEquals("New World", map["Hello"])
        assertEquals("New World", computed)

        computed = map.compute("Bonjour") { _, _ -> "Monde" }
        assertEquals("Monde", map["Bonjour"])
        assertEquals("Monde", computed)

        map.compute("Bonjour") { _, v -> v ?: "Welt" }
        assertEquals("Monde", map["Bonjour"])

        map.compute("Hallo") { _, _ -> null }
        assertNull(map["Hallo"])

        map.compute("Hallo") { _, v -> v ?: "Welt" }
        assertEquals("Welt", map["Hallo"])
    }

    @Test
    fun remove() {
        val map = MutableScatterMap<String?, String?>()
        assertNull(map.remove("Hello"))

        map["Hello"] = "World"
        assertEquals("World", map.remove("Hello"))
        assertEquals(0, map.size)

        map[null] = "World"
        assertEquals("World", map.remove(null))
        assertEquals(0, map.size)

        map["Hello"] = null
        assertNull(map.remove("Hello"))
        assertEquals(0, map.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val map = MutableScatterMap<String, String>(6)
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        // Removing all the entries will mark the metadata as deleted
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
        map["Hello"] = "World"

        assertEquals(1, map.size)
        assertEquals(capacity, map.capacity)
    }

    @Test
    fun removeIf() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        map.removeIf { key, value -> key.startsWith('H') || value.startsWith('S') }

        assertEquals(2, map.size)
        assertEquals("Monde", map["Bonjour"])
        assertEquals("Mondo", map["Ciao"])
    }

    @Test
    fun removeDoesNotCauseGrowthOnInsert() {
        val map = MutableScatterMap<String, String>(10) // Must be > GroupWidth (8)
        assertEquals(15, map.capacity)

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        // Reach the upper limit of what we can store without increasing the map size
        for (i in 0..7) {
            map[i.toString()] = i.toString()
        }

        // Delete a few items
        for (i in 0..5) {
            map.remove(i.toString())
        }

        // Inserting a new item shouldn't cause growth, but the deleted markers to be purged
        map["Foo"] = "Bar"
        assertEquals(15, map.capacity)

        assertEquals("Bar", map["Foo"])
    }

    @Test
    fun minus() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= "Hello"

        assertEquals(2, map.size)
        assertNull(map["Hello"])
    }

    @Test
    fun minusArray() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusIterable() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= listOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusSequence() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusScatterSet() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= scatterSetOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun minusObjectList() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        map -= objectListOf("Hallo", "Bonjour")

        assertEquals(1, map.size)
        assertNull(map["Hallo"])
        assertNull(map["Bonjour"])
    }

    @Test
    fun conditionalRemove() {
        val map = MutableScatterMap<String?, String?>()
        assertFalse(map.remove("Hello", "World"))

        map["Hello"] = "World"
        assertTrue(map.remove("Hello", "World"))
        assertEquals(0, map.size)
    }

    @Test
    fun insertManyEntries() {
        val map = MutableScatterMap<String, String>()

        for (i in 0 until 1700) {
            val s = i.toString()
            map[s] = s
        }

        assertEquals(1700, map.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val map = MutableScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
            }

            var counter = 0
            map.forEach { key, value ->
                assertEquals(key, value)
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val map = MutableScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
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
            val map = MutableScatterMap<String, String>()

            for (j in 0 until i) {
                val s = j.toString()
                map[s] = s
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
        val map = MutableScatterMap<String, String>()

        for (i in 0 until 32) {
            val s = i.toString()
            map[s] = s
        }

        val capacity = map.capacity
        map.clear()

        assertEquals(0, map.size)
        assertEquals(capacity, map.capacity)

        map.forEach { _, _ -> fail() }
    }

    @Test
    fun string() {
        val map = MutableScatterMap<String?, String?>()
        assertEquals("{}", map.toString())

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        assertTrue(
            "{Hello=World, Bonjour=Monde}" == map.toString() ||
                "{Bonjour=Monde, Hello=World}" == map.toString()
        )

        map.clear()
        map["Hello"] = null
        assertEquals("{Hello=null}", map.toString())

        map.clear()
        map[null] = "Monde"
        assertEquals("{null=Monde}", map.toString())

        val selfAsKeyMap = MutableScatterMap<Any, String>()
        selfAsKeyMap[selfAsKeyMap] = "Hello"
        assertEquals("{(this)=Hello}", selfAsKeyMap.toString())

        val selfAsValueMap = MutableScatterMap<String, Any>()
        selfAsValueMap["Hello"] = selfAsValueMap
        assertEquals("{Hello=(this)}", selfAsValueMap.toString())

        // Test with a small map
        val map2 = MutableScatterMap<String?, String?>(2)
        map2["Hello"] = "World"
        map2["Bonjour"] = "Monde"
        assertTrue(
            "{Hello=World, Bonjour=Monde}" == map2.toString() ||
                "{Bonjour=Monde, Hello=World}" == map2.toString()
        )
    }

    @Test
    fun joinToString() {
        val map = mutableScatterMapOf(1 to 1f, 2 to 2f, 3 to 3f, 4 to 4f, 5 to 5f)
        val order = IntArray(5)
        var index = 0
        map.forEach { key, _ -> order[index++] = key }
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
            map.joinToString(limit = 3) { key, _ -> names[key] }
        )
    }

    @Test
    fun equals() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertFalse(map.equals(null))
        assertEquals(map, map)

        val map2 = MutableScatterMap<String?, String?>()
        map2["Bonjour"] = null
        map2[null] = "Monde"

        assertNotEquals(map, map2)

        map2["Hello"] = "World"
        assertEquals(map, map2)
    }

    @Test
    fun containsKey() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue(map.containsKey("Hello"))
        assertTrue(map.containsKey(null))
        assertFalse(map.containsKey("World"))
    }

    @Test
    fun contains() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue("Hello" in map)
        assertTrue(null in map)
        assertFalse("World" in map)
    }

    @Test
    fun containsValue() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertTrue(map.containsValue("World"))
        assertTrue(map.containsValue(null))
        assertFalse(map.containsValue("Hello"))
    }

    @Test
    fun empty() {
        val map = MutableScatterMap<String?, String?>()
        assertTrue(map.isEmpty())
        assertFalse(map.isNotEmpty())
        assertTrue(map.none())
        assertFalse(map.any())

        map["Hello"] = "World"

        assertFalse(map.isEmpty())
        assertTrue(map.isNotEmpty())
        assertTrue(map.any())
        assertFalse(map.none())
    }

    @Test
    fun count() {
        val map = MutableScatterMap<String, String>()
        assertEquals(0, map.count())

        map["Hello"] = "World"
        assertEquals(1, map.count())

        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertEquals(2, map.count { key, _ -> key.startsWith("H") })
        assertEquals(0, map.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertTrue(map.any { key, _ -> key.startsWith("K") })
        assertFalse(map.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        map["Konnichiwa"] = "Sekai"
        map["Ciao"] = "Mondo"
        map["Annyeong"] = "Sesang"

        assertTrue(map.any { key, value -> key.length >= 5 && value.length >= 4 })
        assertFalse(map.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun asMapSize() {
        val map = MutableScatterMap<String, String>()
        val asMap = map.asMap()
        assertEquals(0, asMap.size)

        map["Hello"] = "World"
        assertEquals(1, asMap.size)
    }

    @Test
    fun asMapIsEmpty() {
        val map = MutableScatterMap<String, String>()
        val asMap = map.asMap()
        assertTrue(asMap.isEmpty())

        map["Hello"] = "World"
        assertFalse(asMap.isEmpty())
    }

    @Test
    fun asMapContainsValue() {
        val map = MutableScatterMap<String, String?>()
        map["Hello"] = "World"
        map["Bonjour"] = null

        val asMap = map.asMap()
        assertTrue(asMap.containsValue("World"))
        assertTrue(asMap.containsValue(null))
        assertFalse(asMap.containsValue("Monde"))
    }

    @Test
    fun asMapContainsKey() {
        val map = MutableScatterMap<String?, String>()
        map["Hello"] = "World"
        map[null] = "Monde"

        val asMap = map.asMap()
        assertTrue(asMap.containsKey("Hello"))
        assertTrue(asMap.containsKey(null))
        assertFalse(asMap.containsKey("Bonjour"))
    }

    @Test
    fun asMapGet() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        val asMap = map.asMap()
        assertEquals("World", asMap["Hello"])
        assertEquals("Monde", asMap[null])
        assertEquals(null, asMap["Bonjour"])
        assertEquals(null, asMap["Hallo"])
    }

    @Test
    fun asMapValues() {
        val map = MutableScatterMap<String?, String?>()
        val values = map.asMap().values
        assertTrue(values.isEmpty())
        assertEquals(0, values.size)

        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null
        map["Hello2"] = "World" // duplicate value

        assertEquals(map.size, values.size)
        for (value in values) {
            assertTrue(map.containsValue(value))
        }

        map.forEachValue { value -> assertTrue(values.contains(value)) }
    }

    @Test
    fun asMapKeys() {
        val map = MutableScatterMap<String?, String?>()
        val keys = map.asMap().keys
        assertTrue(keys.isEmpty())
        assertEquals(0, keys.size)

        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertEquals(map.size, keys.size)
        for (key in keys) {
            assertTrue(map.containsKey(key))
        }

        map.forEachKey { key -> assertTrue(keys.contains(key)) }
    }

    @Test
    fun asMapEntries() {
        val map = MutableScatterMap<String?, String?>()
        val entries = map.asMap().entries
        assertTrue(entries.isEmpty())
        assertEquals(0, entries.size)

        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        assertEquals(map.size, entries.size)
        for (entry in entries) {
            assertEquals(map[entry.key], entry.value)
        }

        map.forEach { key, value ->
            assertTrue(
                entries.contains(
                    object : Map.Entry<String?, String?> {
                        override val key: String?
                            get() = key

                        override val value: String?
                            get() = value
                    }
                )
            )
        }
    }

    @Test
    fun asMapToList() {
        val map = mutableScatterMapOf<Int, Int>()
        map[0] = 0
        map[1] = -1

        val list = map.asMap().toList() // this requires the iterator to return new Entry instances

        assertEquals(map.size, list.size)
        assertTrue(list.contains(0 to 0))
        assertTrue(list.contains(1 to -1))
    }

    @Test
    fun asMutableMapClear() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        val mutableMap = map.asMutableMap()
        mutableMap.clear()

        assertEquals(0, mutableMap.size)
        assertEquals(map.size, mutableMap.size)
        assertTrue(map.isEmpty())
        assertTrue(mutableMap.isEmpty())
    }

    @Test
    fun asMutableMapPut() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        val mutableMap = map.asMutableMap()
        assertEquals(map.size, mutableMap.size)
        assertEquals(3, mutableMap.size)

        assertNull(mutableMap.put("Hallo", "Welt"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(4, mutableMap.size)

        assertEquals(null, mutableMap.put("Bonjour", "Monde"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(4, mutableMap.size)
    }

    @Test
    fun asMutableMapRemove() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        val mutableMap = map.asMutableMap()
        assertEquals(map.size, mutableMap.size)
        assertEquals(3, mutableMap.size)

        assertNull(mutableMap.remove("Hallo"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(3, mutableMap.size)

        assertEquals("World", mutableMap.remove("Hello"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(2, mutableMap.size)
    }

    @Test
    fun asMutableMapPutAll() {
        val map = MutableScatterMap<String?, String?>()
        map["Hello"] = "World"
        map[null] = "Monde"
        map["Bonjour"] = null

        val mutableMap = map.asMutableMap()
        mutableMap.putAll(mapOf("Hallo" to "Welt", "Hola" to "Mundo"))

        assertEquals(map.size, mutableMap.size)
        assertEquals(5, mutableMap.size)
        assertEquals("Welt", map["Hallo"])
        assertEquals("Mundo", map["Hola"])
    }

    @Test
    fun asMutableMapValuesContains() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val values = mutableMap.values

        assertFalse(values.contains("Mundo"))
        assertTrue(values.contains("Monde"))
        assertFalse(values.containsAll(listOf("Monde", "Mundo")))
        assertTrue(values.containsAll(listOf("Monde", "Welt")))
    }

    @Test
    fun asMutableMapValuesAdd() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val values = mutableMap.values

        assertFailsWith(UnsupportedOperationException::class) { values.add("XXX") }

        assertFailsWith(UnsupportedOperationException::class) { values.addAll(listOf("XXX")) }
    }

    @Test
    fun asMutableMapValuesRemoveRetain() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val values = mutableMap.values

        assertTrue(values.remove("Monde"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(map.size, values.size)
        assertEquals(2, map.size)

        assertFalse(values.remove("Monde"))
        assertEquals(2, map.size)

        assertFalse(values.removeAll(listOf("Mundo")))
        assertEquals(2, map.size)

        assertTrue(values.removeAll(listOf("World", "Welt")))
        assertEquals(0, map.size)

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        assertEquals(3, map.size)

        assertFalse(values.retainAll(listOf("World", "Monde", "Welt")))
        assertEquals(3, map.size)

        assertTrue(values.retainAll(listOf("World", "Welt")))
        assertEquals(2, map.size)

        values.clear()
        assertEquals(0, map.size)
    }

    @Test
    fun asMutableMapValuesIterator() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val values = mutableMap.values

        for (value in values) {
            assertTrue(map.containsValue(value))
        }

        val size = map.size
        assertEquals(3, map.size)
        // No-op before a call to next()
        val iterator = values.iterator()
        iterator.remove()
        assertEquals(size, map.size)

        assertTrue(iterator.hasNext())
        assertEquals("Monde", iterator.next())
        iterator.remove()
        assertEquals(2, map.size)

        assertFalse(MutableScatterMap<String, String>().asMutableMap().values.iterator().hasNext())
    }

    @Test
    fun asMutableMapKeysContains() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val keys = mutableMap.keys

        assertFalse(keys.contains("Hola"))
        assertTrue(keys.contains("Bonjour"))
        assertFalse(keys.containsAll(listOf("Bonjour", "Hola")))
        assertTrue(keys.containsAll(listOf("Bonjour", "Hallo")))
    }

    @Test
    fun asMutableMapKeysAdd() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val keys = mutableMap.keys

        assertFailsWith(UnsupportedOperationException::class) { keys.add("XXX") }

        assertFailsWith(UnsupportedOperationException::class) { keys.addAll(listOf("XXX")) }
    }

    @Test
    fun asMutableMapKeysRemoveRetain() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val keys = mutableMap.keys

        assertTrue(keys.remove("Bonjour"))
        assertEquals(map.size, mutableMap.size)
        assertEquals(map.size, keys.size)
        assertEquals(2, map.size)

        assertFalse(keys.remove("Bonjour"))
        assertEquals(2, map.size)

        assertFalse(keys.removeAll(listOf("Hola")))
        assertEquals(2, map.size)

        assertTrue(keys.removeAll(listOf("Hello", "Hallo")))
        assertEquals(0, map.size)

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        assertEquals(3, map.size)

        assertFalse(keys.retainAll(listOf("Hello", "Bonjour", "Hallo")))
        assertEquals(3, map.size)

        assertTrue(keys.retainAll(listOf("Hello", "Hallo")))
        assertEquals(2, map.size)

        keys.clear()
        assertEquals(0, map.size)
    }

    @Test
    fun asMutableMapKeysIterator() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val keys = mutableMap.keys

        for (key in keys) {
            assertTrue(key in map)
        }

        val size = map.size
        assertEquals(3, map.size)
        // No-op before a call to next()
        val iterator = keys.iterator()
        iterator.remove()
        assertEquals(size, map.size)

        assertTrue(iterator.hasNext())
        assertEquals("Bonjour", iterator.next())
        iterator.remove()
        assertEquals(2, map.size)

        assertFalse(MutableScatterMap<String, String>().asMutableMap().keys.iterator().hasNext())
    }

    class MutableMapEntry(override val key: String, override val value: String) :
        MutableMap.MutableEntry<String, String> {
        override fun setValue(newValue: String): String {
            throw UnsupportedOperationException()
        }
    }

    @Test
    fun asMutableMapEntriesContains() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val entries = mutableMap.entries

        assertFalse(entries.contains(MutableMapEntry("Hola", "Mundo")))
        assertTrue(entries.contains(MutableMapEntry("Bonjour", "Monde")))
        assertFalse(entries.contains(MutableMapEntry("Bonjour", "Le Monde")))
        assertFalse(
            entries.containsAll(
                listOf(MutableMapEntry("Bonjour", "Monde"), MutableMapEntry("Hola", "Mundo"))
            )
        )
        assertTrue(
            entries.containsAll(
                listOf(MutableMapEntry("Bonjour", "Monde"), MutableMapEntry("Hallo", "Welt"))
            )
        )
        assertFalse(
            entries.containsAll(
                listOf(MutableMapEntry("Bonjour", "Le Monde"), MutableMapEntry("Hallo", "Welt"))
            )
        )
    }

    @Test
    fun asMutableMapEntriesAdd() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val entries = mutableMap.entries

        assertFailsWith(UnsupportedOperationException::class) {
            entries.add(MutableMapEntry("X", "XX"))
        }

        assertFailsWith(UnsupportedOperationException::class) {
            entries.addAll(listOf(MutableMapEntry("X", "XX")))
        }
    }

    @Suppress("ConvertArgumentToSet")
    @Test
    fun asMutableMapEntriesRemoveRetain() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val entries = mutableMap.entries

        assertTrue(entries.remove(MutableMapEntry("Bonjour", "Monde")))
        assertEquals(map.size, mutableMap.size)
        assertEquals(map.size, entries.size)
        assertEquals(2, map.size)

        assertFalse(entries.remove(MutableMapEntry("Bonjour", "Monde")))
        assertEquals(2, map.size)

        assertFalse(entries.remove(MutableMapEntry("Hello", "The World")))
        assertEquals(2, map.size)

        assertFalse(entries.removeAll(listOf(MutableMapEntry("Hola", "Mundo"))))
        assertEquals(2, map.size)

        assertTrue(
            entries.removeAll(
                listOf(MutableMapEntry("Hello", "World"), MutableMapEntry("Hallo", "Welt"))
            )
        )
        assertEquals(0, map.size)

        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"
        assertEquals(3, map.size)

        assertFalse(
            entries.retainAll(
                listOf(
                    MutableMapEntry("Hello", "World"),
                    MutableMapEntry("Bonjour", "Monde"),
                    MutableMapEntry("Hallo", "Welt")
                )
            )
        )
        assertEquals(3, map.size)

        assertTrue(
            entries.retainAll(
                listOf(
                    MutableMapEntry("Hello", "World"),
                    MutableMapEntry("Bonjour", "Le Monde"),
                    MutableMapEntry("Hallo", "Welt")
                )
            )
        )
        assertEquals(2, map.size)

        assertTrue(
            entries.retainAll(
                listOf(
                    MutableMapEntry("Hello", "World"),
                )
            )
        )
        assertEquals(1, map.size)

        entries.clear()
        assertEquals(0, map.size)
    }

    @Test
    fun asMutableMapEntriesIterator() {
        val map = MutableScatterMap<String, String>()
        map["Hello"] = "World"
        map["Bonjour"] = "Monde"
        map["Hallo"] = "Welt"

        val mutableMap = map.asMutableMap()
        val entries = mutableMap.entries

        for (entry in entries) {
            assertEquals(entry.value, map[entry.key])
        }

        val size = map.size
        assertEquals(3, map.size)
        // No-op before a call to next()
        val iterator = entries.iterator()
        iterator.remove()
        assertEquals(size, map.size)

        assertTrue(iterator.hasNext())
        val next = iterator.next()
        assertEquals("Bonjour", next.key)
        assertEquals("Monde", next.value)
        iterator.remove()
        assertEquals(2, map.size)

        map.clear()
        map["Hello"] = "World"
        map["Hallo"] = "Welt"

        assertFalse(map.any { _, value -> value == "XX" })
        for (entry in entries) {
            assertTrue(entry.setValue("XX").startsWith("W"))
        }
        assertTrue(map.all { _, value -> value == "XX" })

        assertFalse(MutableScatterMap<String, String>().asMutableMap().entries.iterator().hasNext())
    }

    @Test
    fun trim() {
        val map = MutableScatterMap<String, String>()
        assertEquals(7, map.trim())

        map["Hello"] = "World"
        map["Hallo"] = "Welt"

        assertEquals(0, map.trim())

        for (i in 0 until 1700) {
            val s = i.toString()
            map[s] = s
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
    fun insertOneRemoveOne() {
        val map = MutableScatterMap<Int, String>()

        for (i in 0..1000000) {
            map[i] = i.toString()
            map.remove(i)
            assertTrue(map.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableScatterMap<Int, String>()

        for (i in 0..100) {
            map[i] = i.toString()
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                map.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                map[i] = i.toString()
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                map.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                map[i] = i.toString()
            }
        }

        assertEquals(127, map.capacity)
        for (i in 0..100) {
            assertTrue(map.contains(i), "Map should contain element $i")
        }
    }
}
