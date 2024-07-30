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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SieveCacheTest {
    @Test
    fun sizeMustBeGreaterThan0() {
        assertFailsWith<IllegalArgumentException> { SieveCache<String, String>(-1) }
        assertFailsWith<IllegalArgumentException> { SieveCache<String, String>(0) }
    }

    @Test
    fun emptyCache() {
        val cache = createStandardCache()
        assertEquals(0, cache.size)
        assertEquals(4, cache.maxSize)
        assertEquals(0, cache.count)
        assertEquals(7, cache.capacity)
    }

    @Test
    fun zeroCapacityCache() {
        val cache = SieveCache<String, String>(4, 0)
        assertEquals(0, cache.capacity)
        assertEquals(0, cache.size)
    }

    @Test
    fun cacheWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val cache = SieveCache<String, String>(4, 1800)
        assertEquals(4095, cache.capacity)
        assertEquals(0, cache.size)
    }

    @Test
    fun addEntry() {
        val cache = createStandardCache()
        cache["Hello"] = "World"

        assertEquals(1, cache.size)
        assertEquals(1, cache.count)
        assertEquals(7, cache.capacity)
        assertEquals("World", cache["Hello"])
    }

    @Test
    fun addEntryCustomSize() {
        val cache = createCacheWithCustomSize()
        cache["Hello"] = "World"

        assertEquals("World".length, cache.size)
        assertEquals(1, cache.count)
        assertEquals(7, cache.capacity)
        assertEquals("World", cache["Hello"])
    }

    @Test
    fun addEntryReplacesOldValue() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Hello"] = "Goodbye"

        assertEquals("Goodbye".length, cache.size)
        assertContentEquals(listOf("World"), removedEntries)
        assertContentEquals(listOf(), evictedEntries)
    }

    @Test
    fun addEntryTrimsSize() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"

        assertEquals(4, cache.size)
        assertEquals(4, cache.count)
        assertEquals(7, cache.capacity)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World"), evictedEntries)
    }

    @Test
    fun addIncreaseCapacity() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries, evictedEntries)

        assertEquals(7, cache.capacity)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"
        cache["Goodbye"] = "World"

        assertEquals(7, cache.count)
        assertEquals(15, cache.capacity)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf(), evictedEntries)
    }

    @Test
    fun addToZeroCapacityCache() {
        val cache = SieveCache<String, String>(4, 0)
        cache["Hello"] = "World"

        assertEquals(1, cache.size)
        assertEquals(1, cache.count)
        assertEquals("World", cache["Hello"])
    }

    @Test
    fun get() {
        val cache = createStandardCache()
        cache["Hello"] = "World"

        assertEquals("World", cache["Hello"])
    }

    @Test
    fun getReturnsNull() {
        val cache = createStandardCache()
        assertNull(cache["Hello"])
    }

    @Test
    fun getCreatesValue() {
        val cache = createCreatingCache()
        val value = cache["Hello"]

        assertEquals(cache.size, 1)
        assertEquals(cache.count, 1)
        assertEquals("created-Hello", value)
    }

    @Test
    fun contains() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"

        assertTrue("Hello" in cache)
        assertFalse("World" in cache)
    }

    @Test
    fun containsKey() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"

        assertTrue(cache.containsKey("Hello"))
        assertFalse(cache.containsKey("World"))
    }

    @Test
    fun containsValue() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"

        assertTrue(cache.containsValue("World"))
        assertFalse(cache.containsValue("Hello"))
    }

    @Test
    fun empty() {
        val cache = createStandardCache()
        assertTrue(cache.isEmpty())
        assertFalse(cache.isNotEmpty())
        assertTrue(cache.none())
        assertFalse(cache.any())

        cache["Hello"] = "World"

        assertFalse(cache.isEmpty())
        assertTrue(cache.isNotEmpty())
        assertTrue(cache.any())
        assertFalse(cache.none())
    }

    @Test
    fun count() {
        val cache = createStandardCache()
        assertEquals(0, cache.count())

        cache["Hello"] = "World"
        assertEquals(1, cache.count())

        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        assertEquals(2, cache.count { key, _ -> key.startsWith("H") })
        assertEquals(0, cache.count { key, _ -> key.startsWith("W") })
    }

    @Test
    fun any() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"

        assertTrue(cache.any { key, _ -> key.startsWith("K") })
        assertFalse(cache.any { key, _ -> key.startsWith("W") })
    }

    @Test
    fun all() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"

        assertTrue(cache.any { key, value -> key.length >= 5 && value.length >= 4 })
        assertFalse(cache.all { key, _ -> key.startsWith("W") })
    }

    @Test
    fun putReturnsNull() {
        val cache = createStandardCache()
        val previousValue = cache.put("Hello", "World")
        assertNull(previousValue)
    }

    @Test
    fun putReturnsPreviousValue() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        val previousValue = cache.put("Hello", "Goodbye")
        assertEquals("World", previousValue)
    }

    @Test
    fun readPreventsEviction() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        // Preserve Hello=World
        assertEquals("World", cache["Hello"])
        cache["Ciao"] = "Mondo"

        assertEquals(4, cache.size)
        assertContentEquals(listOf(), removedEntries)
        // Reading "Hello" kept it, the next eviction candidate is "Bonjour"
        assertContentEquals(listOf("Monde"), evictedEntries)
    }

    @Test
    fun plus() {
        val cache = createStandardCache()
        cache += "Hello" to "World"

        assertEquals(1, cache.size)
        assertEquals("World", cache["Hello"])
    }

    @Test
    fun plusMap() {
        val cache = createStandardCache()
        cache += mapOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, cache.size)
        assertEquals("Welt", cache["Hallo"])
        assertEquals("Mundo", cache["Hola"])
    }

    @Test
    fun plusArray() {
        val cache = createStandardCache()
        cache += arrayOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, cache.size)
        assertEquals("Welt", cache["Hallo"])
        assertEquals("Mundo", cache["Hola"])
    }

    @Test
    fun plusIterable() {
        val cache = createStandardCache()
        cache += listOf("Hallo" to "Welt", "Hola" to "Mundo")

        assertEquals(2, cache.size)
        assertEquals("Welt", cache["Hallo"])
        assertEquals("Mundo", cache["Hola"])
    }

    @Test
    fun plusSequence() {
        val cache = createStandardCache()
        cache += listOf("Hallo" to "Welt", "Hola" to "Mundo").asSequence()

        assertEquals(2, cache.size)
        assertEquals("Welt", cache["Hallo"])
        assertEquals("Mundo", cache["Hola"])
    }

    @Test
    fun remove() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        val previousValue = cache.remove("Hello")

        assertEquals(0, cache.size)
        assertEquals("World", previousValue)
        assertContentEquals(listOf("World"), removedEntries)
        assertContentEquals(listOf(), evictedEntries)
    }

    @Test
    fun removeUnknownEntry() {
        val cache = createStandardCache()
        cache["Hello"] = "World"

        assertNull(cache.remove("Bonjour"))
        assertEquals(1, cache.size)
        assertEquals(1, cache.count)
    }

    @Test
    fun removeFromEmptyCache() {
        val cache = createStandardCache()

        assertNull(cache.remove("Bonjour"))
        assertEquals(0, cache.size)
        assertEquals(0, cache.count)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val cache = SieveCache<String, String>(2048, 6)
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"

        // Removing all the entries will mark the metadata as deleted
        cache.remove("Hello")
        cache.remove("Bonjour")
        cache.remove("Hallo")
        cache.remove("Konnichiwa")
        cache.remove("Ciao")
        cache.remove("Annyeong")

        assertEquals(0, cache.count)

        val capacity = cache.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        cache["Hello"] = "World"

        assertEquals(1, cache.count)
        assertEquals(capacity, cache.capacity)
    }

    @Test
    fun removeIf() {
        val removedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries)
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"

        cache.removeIf { key, value -> key.startsWith('H') || value.startsWith('S') }

        assertEquals(2, cache.count)
        assertEquals("Monde", cache["Bonjour"])
        assertEquals("Mondo", cache["Ciao"])
        assertContentEquals(listOf("Sekai", "Sesang", "World", "Welt"), removedEntries)
    }

    @Test
    fun removeDoesNotCauseGrowthOnInsert() {
        val cache = SieveCache<String, String>(2048, 10) // Must be > GroupWidth (8)
        assertEquals(15, cache.capacity)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"
        cache["Ciao"] = "Mondo"
        cache["Annyeong"] = "Sesang"

        // Reach the upper limit of what we can store without increasing the cache size
        for (i in 0..7) {
            cache[i.toString()] = i.toString()
        }

        // Delete a few items
        for (i in 0..5) {
            cache.remove(i.toString())
        }

        // Inserting a new item shouldn't cause growth, but the deleted markers to be purged
        cache["Foo"] = "Bar"
        assertEquals(15, cache.capacity)

        assertEquals("Bar", cache["Foo"])
    }

    @Test
    fun conditionalRemove() {
        val removedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries)
        assertFalse(cache.remove("Hello", "World"))
        assertContentEquals(listOf(), removedEntries)

        cache["Hello"] = "World"
        assertTrue(cache.remove("Hello", "World"))
        assertEquals(0, cache.count)
        assertContentEquals(listOf("World"), removedEntries)
    }

    @Test
    fun minus() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= "Hello"

        assertEquals(2, cache.count)
        assertNull(cache["Hello"])
    }

    @Test
    fun minusArray() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= arrayOf("Hallo", "Bonjour")

        assertEquals(1, cache.count)
        assertNull(cache["Hallo"])
        assertNull(cache["Bonjour"])
    }

    @Test
    fun minusIterable() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= listOf("Hallo", "Bonjour")

        assertEquals(1, cache.count)
        assertNull(cache["Hallo"])
        assertNull(cache["Bonjour"])
    }

    @Test
    fun minusSequence() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= listOf("Hallo", "Bonjour").asSequence()

        assertEquals(1, cache.count)
        assertNull(cache["Hallo"])
        assertNull(cache["Bonjour"])
    }

    @Test
    fun minusScatterSet() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= scatterSetOf("Hallo", "Bonjour")

        assertEquals(1, cache.count)
        assertNull(cache["Hallo"])
        assertNull(cache["Bonjour"])
    }

    @Test
    fun minusObjectList() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"

        cache -= objectListOf("Hallo", "Bonjour")

        assertEquals(1, cache.count)
        assertNull(cache["Hallo"])
        assertNull(cache["Bonjour"])
    }

    @Test
    fun evictAllFromEmptyCache() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache.evictAll()

        assertEquals(0, cache.size)
        assertEquals(0, cache.count)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf(), evictedEntries)
    }

    @Test
    fun evictAll() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        cache.evictAll()

        assertEquals(0, cache.size)
        assertEquals(0, cache.count)
        assertEquals(7, cache.capacity)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World", "Monde", "Welt", "Sekai"), evictedEntries)
    }

    @Test
    fun evictAllEvictsZeroSizeElements() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createZeroSizeCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        cache.evictAll()

        assertEquals(0, cache.size)
        assertEquals(0, cache.count)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World", "Monde", "Welt", "Sekai"), evictedEntries)
    }

    @Test
    fun resize() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        cache.resize(6)

        assertEquals(6, cache.maxSize)
        assertEquals(5, cache.size)
        assertEquals(1, cache.count)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World", "Monde", "Welt"), evictedEntries)
    }

    @Test
    fun trimToSize() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createCacheWithCustomSize(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        val maxSize = cache.maxSize
        cache.trimToSize(6)

        assertEquals(maxSize, cache.maxSize)
        assertEquals(5, cache.size)
        assertEquals(1, cache.count)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World", "Monde", "Welt"), evictedEntries)
    }

    @Test
    fun equals() {
        val cache = createStandardCache()
        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"

        assertFalse(cache.equals(null))
        assertEquals(cache, cache)

        val cache2 = createStandardCache()
        cache2["Bonjour"] = "Monde"
        cache2["Hello"] = "Monde"

        assertNotEquals(cache, cache2)

        cache2["Hello"] = "World"
        assertEquals(cache, cache2)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val cache = createCacheWithCustomSize()

            for (j in 0 until i) {
                val s = j.toString()
                cache[s] = s
            }

            var counter = 0
            cache.forEach { key, value ->
                assertEquals(key, value)
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachDoesNotPreventsEviction() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        var count = 0
        cache.forEach { _, _ -> count++ }
        assertEquals(4, count)

        cache["Ciao"] = "Mondo"

        assertEquals(4, cache.size)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World"), evictedEntries)
    }

    @Test
    fun forEachKey() {
        for (i in 0..48) {
            val cache = createCacheWithCustomSize()

            for (j in 0 until i) {
                val s = j.toString()
                cache[s] = s
            }

            var counter = 0
            cache.forEachKey { key ->
                assertNotNull(key.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachKeyDoesNotPreventsEviction() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        var count = 0
        cache.forEachKey { _ -> count++ }
        assertEquals(4, count)

        cache["Ciao"] = "Mondo"

        assertEquals(4, cache.size)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World"), evictedEntries)
    }

    @Test
    fun forEachValue() {
        for (i in 0..48) {
            val cache = createCacheWithCustomSize()

            for (j in 0 until i) {
                val s = j.toString()
                cache[s] = s
            }

            var counter = 0
            cache.forEachValue { value ->
                assertNotNull(value.toIntOrNull())
                counter++
            }

            assertEquals(i, counter)
        }
    }

    @Test
    fun forEachValueDoesNotPreventsEviction() {
        val removedEntries = mutableListOf<String>()
        val evictedEntries = mutableListOf<String>()
        val cache = createStandardCache(removedEntries, evictedEntries)

        cache["Hello"] = "World"
        cache["Bonjour"] = "Monde"
        cache["Hallo"] = "Welt"
        cache["Konnichiwa"] = "Sekai"

        var count = 0
        cache.forEachKey { _ -> count++ }
        assertEquals(4, count)

        cache["Ciao"] = "Mondo"

        assertEquals(4, cache.size)
        assertContentEquals(listOf(), removedEntries)
        assertContentEquals(listOf("World"), evictedEntries)
    }

    @Test
    fun insertOneRemoveOne() {
        val cache = SieveCache<Int, String>(4)

        for (i in 0..1000000) {
            cache[i] = i.toString()
            cache.remove(i)
            assertTrue(cache.capacity < 16, "Map grew larger than 16 after step $i")
        }
    }

    @Test
    fun insertManyRemoveMany() {
        val cache = SieveCache<Int, String>(1024)

        for (i in 0..100) {
            cache[i] = i.toString()
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                cache.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                cache[i] = i.toString()
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                cache.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                cache[i] = i.toString()
            }
        }

        assertEquals(127, cache.capacity)
        for (i in 0..100) {
            assertTrue(cache.contains(i), "Map should contain element $i")
        }
    }

    @Test
    fun putReplaceInCacheOfSize1() {
        val cache = SieveCache<String, String>(1, 1)

        for (i in 0..9) {
            cache[i.toString()] = (i * 2).toString()
            if (i % 2 == 0) {
                cache.remove(i.toString())
            }
        }

        assertEquals(cache["9"], "18")
        assertEquals(1, cache.size)
    }

    private fun createCreatingCache(): SieveCache<String, String> {
        return SieveCache(4, createValueFromKey = { key -> "created-$key" })
    }

    private fun createStandardCache(
        removedEntries: MutableList<String> = mutableListOf(),
        evictedEntries: MutableList<String> = mutableListOf()
    ): SieveCache<String, String> {
        return SieveCache(
            4,
            onEntryRemoved = { _, old, _, evicted ->
                (if (evicted) evictedEntries else removedEntries).add(old)
            }
        )
    }

    private fun createCacheWithCustomSize(
        removedEntries: MutableList<String> = mutableListOf(),
        evictedEntries: MutableList<String> = mutableListOf()
    ): SieveCache<String, String> {
        return SieveCache(
            4096,
            sizeOf = { _, v -> v.length },
            onEntryRemoved = { _, old, _, evicted ->
                (if (evicted) evictedEntries else removedEntries).add(old)
            }
        )
    }

    private fun createZeroSizeCache(
        removedEntries: MutableList<String> = mutableListOf(),
        evictedEntries: MutableList<String> = mutableListOf()
    ): SieveCache<String, String> {
        return SieveCache(
            4,
            sizeOf = { _, _ -> 0 },
            onEntryRemoved = { _, old, _, evicted ->
                (if (evicted) evictedEntries else removedEntries).add(old)
            }
        )
    }
}
