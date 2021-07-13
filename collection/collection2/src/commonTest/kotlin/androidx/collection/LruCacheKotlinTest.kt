/*
 * Copyright 2020 The Android Open Source Project
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

class LruCacheKotlinTest {
    private var expectedCreateCount = 0
    private var expectedPutCount = 0
    private var expectedHitCount = 0
    private var expectedMissCount = 0
    private var expectedEvictionCount = 0

    @Test
    fun testStatistics() {
        val cache = LruCache<String, String>(3)
        assertStatistics(cache)
        assertEquals(null, cache.put("a", "A"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertSnapshot(cache, "a", "A")
        assertEquals(null, cache.put("b", "B"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertHit(cache, "b", "B")
        assertSnapshot(cache, "a", "A", "b", "B")
        assertEquals(null, cache.put("c", "C"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertHit(cache, "b", "B")
        assertHit(cache, "c", "C")
        assertSnapshot(cache, "a", "A", "b", "B", "c", "C")
        assertEquals(null, cache.put("d", "D"))
        expectedPutCount++
        expectedEvictionCount++ // a should have been evicted
        assertStatistics(cache)
        assertMiss(cache, "a")
        assertHit(cache, "b", "B")
        assertHit(cache, "c", "C")
        assertHit(cache, "d", "D")
        assertHit(cache, "b", "B")
        assertHit(cache, "c", "C")
        assertSnapshot(cache, "d", "D", "b", "B", "c", "C")
        assertEquals(null, cache.put("e", "E"))
        expectedPutCount++
        expectedEvictionCount++ // d should have been evicted
        assertStatistics(cache)
        assertMiss(cache, "d")
        assertMiss(cache, "a")
        assertHit(cache, "e", "E")
        assertHit(cache, "b", "B")
        assertHit(cache, "c", "C")
        assertSnapshot(cache, "e", "E", "b", "B", "c", "C")
    }

    @Test
    fun testStatisticsWithCreate() {
        val cache = newCreatingCache()
        assertStatistics(cache)
        assertCreated(cache, "aa", "created-aa")
        assertHit(cache, "aa", "created-aa")
        assertSnapshot(cache, "aa", "created-aa")
        assertCreated(cache, "bb", "created-bb")
        assertMiss(cache, "c")
        assertSnapshot(cache, "aa", "created-aa", "bb", "created-bb")
        assertCreated(cache, "cc", "created-cc")
        assertSnapshot(cache, "aa", "created-aa", "bb", "created-bb", "cc", "created-cc")
        expectedEvictionCount++ // aa will be evicted
        assertCreated(cache, "dd", "created-dd")
        assertSnapshot(cache, "bb", "created-bb", "cc", "created-cc", "dd", "created-dd")
        expectedEvictionCount++ // bb will be evicted
        assertCreated(cache, "aa", "created-aa")
        assertSnapshot(cache, "cc", "created-cc", "dd", "created-dd", "aa", "created-aa")
    }

    @Test
    fun testCreateOnCacheMiss() {
        val cache = newCreatingCache()
        val created = cache.get("aa")
        assertEquals("created-aa", created)
    }

    @Test
    fun testNoCreateOnCacheHit() {
        val cache = newCreatingCache()
        cache.put("aa", "put-aa")
        assertEquals("put-aa", cache.get("aa"))
    }

    @Test
    fun testConstructorDoesNotAllowZeroCacheSize() {
        assertThrows<IllegalArgumentException> { LruCache<String, String>(0) }
    }

    @Test
    fun testToString() {
        val cache = LruCache<String, String>(3)
        assertEquals(
            "LruCache[maxSize=3,hits=0,misses=0,hitRate=0%]",
            cache.toString()
        )
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.put("d", "D")
        cache.get("a") // miss
        cache.get("b") // hit
        cache.get("c") // hit
        cache.get("d") // hit
        cache.get("e") // miss
        assertEquals(
            "LruCache[maxSize=3,hits=3,misses=2,hitRate=60%]",
            cache.toString()
        )
    }

    @Test
    fun testEvictionWithSingletonCache() {
        val cache = LruCache<String, String>(1)
        cache.put("a", "A")
        cache.put("b", "B")
        assertSnapshot(cache, "b", "B")
    }

    @Test
    fun testEntryEvictedWhenFull() {
        val log = mutableListOf<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        assertEquals(emptyList<String>(), log)
        cache.put("d", "D")
        assertEquals(listOf("a=A"), log)
    }

    /**
     * Replacing the value for a key doesn't cause an eviction but it does bring
     * the replaced entry to the front of the queue.
     */
    @Test
    fun testPutCauseEviction() {
        val log = mutableListOf<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.put("b", "B2")
        assertEquals(listOf("b=B>B2"), log)
        assertSnapshot(cache, "a", "A", "c", "C", "b", "B2")
    }

    @Test
    fun testCustomSizesImpactsSize() {
        val cache = object : LruCache<String, String>(10) {
            override fun sizeOf(key: String, value: String): Int {
                return key.length + value.length
            }
        }
        assertEquals(0, cache.size)
        cache.put("a", "AA")
        assertEquals(3, cache.size)
        cache.put("b", "BBBB")
        assertEquals(8, cache.size)
        cache.put("a", "")
        assertEquals(6, cache.size)
    }

    @Test
    fun testEvictionWithCustomSizes() {
        val cache = object : LruCache<String, String>(4) {
            override fun sizeOf(key: String, value: String): Int {
                return value.length
            }
        }
        cache.put("a", "AAAA")
        assertSnapshot(cache, "a", "AAAA")
        cache.put("b", "BBBB") // should evict a
        assertSnapshot(cache, "b", "BBBB")
        cache.put("c", "CC") // should evict b
        assertSnapshot(cache, "c", "CC")
        cache.put("d", "DD")
        assertSnapshot(cache, "c", "CC", "d", "DD")
        cache.put("e", "E") // should evict c
        assertSnapshot(cache, "d", "DD", "e", "E")
        cache.put("f", "F")
        assertSnapshot(cache, "d", "DD", "e", "E", "f", "F")
        cache.put("g", "G") // should evict d
        assertSnapshot(cache, "e", "E", "f", "F", "g", "G")
        cache.put("h", "H")
        assertSnapshot(cache, "e", "E", "f", "F", "g", "G", "h", "H")
        cache.put("i", "III") // should evict e, f, and g
        assertSnapshot(cache, "h", "H", "i", "III")
        cache.put("j", "JJJ") // should evict h and i
        assertSnapshot(cache, "j", "JJJ")
    }

    @Test
    fun testEvictionThrowsWhenSizesAreInconsistent() {
        val cache = object : LruCache<String, IntArray>(4) {
            override fun sizeOf(key: String, value: IntArray): Int {
                return value[0]
            }
        }
        val a = intArrayOf(4)
        cache.put("a", a)
        // get the cache size out of sync
        a[0] = 1
        assertEquals(4, cache.size)

        // evict something
        assertThrows<IllegalStateException> {
            cache.put("b", intArrayOf(2))!!
        }
    }

    @Test
    fun testEvictionThrowsWhenSizesAreNegative() {
        val cache = object : LruCache<String, String>(4) {
            override fun sizeOf(key: String, value: String): Int {
                return -1
            }
        }
        assertThrows<IllegalStateException> {
            cache.put("a", "A")!!
        }
    }

    /**
     * Naive caches evict at most one element at a time. This is problematic
     * because evicting a small element may be insufficient to make room for a
     * large element.
     */
    @Test
    fun testDifferentElementSizes() {
        val cache = object : LruCache<String, String>(10) {
            override fun sizeOf(key: String, value: String): Int {
                return value.length
            }
        }
        cache.put("a", "1")
        cache.put("b", "12345678")
        cache.put("c", "1")
        assertSnapshot(cache, "a", "1", "b", "12345678", "c", "1")
        cache.put("d", "12345678") // should evict a and b
        assertSnapshot(cache, "c", "1", "d", "12345678")
        cache.put("e", "12345678") // should evict c and d
        assertSnapshot(cache, "e", "12345678")
    }

    @Test
    fun testEvict() {
        val log = mutableListOf<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.evictAll()
        assertEquals(0, cache.size)
        assertEquals(listOf("a=A", "b=B", "c=C"), log)
    }

    @Test
    fun testEvictAllEvictsSizeZeroElements() {
        val cache = object : LruCache<String, String>(10) {
            override fun sizeOf(key: String, value: String): Int {
                return 0
            }
        }
        cache.put("a", "A")
        cache.put("b", "B")
        cache.evictAll()
        assertSnapshot(cache)
    }

    @Test
    fun testRemoveWithCustomSizes() {
        val cache = object : LruCache<String, String>(10) {
            override fun sizeOf(key: String, value: String): Int {
                return value.length
            }
        }
        cache.put("a", "123456")
        cache.put("b", "1234")
        cache.remove("a")
        assertEquals(4, cache.size)
    }

    @Test
    fun testRemoveAbsentElement() {
        val cache = LruCache<String, String>(10)
        cache.put("a", "A")
        cache.put("b", "B")
        assertEquals(null, cache.remove("c"))
        assertEquals(2, cache.size)
    }

    @Test
    fun testRemoveNullThrows() {
        val cache = LruCache<String?, String>(10)
        assertThrows<NullPointerException> { cache.remove(null)!! }
    }

    @Test
    fun testRemoveCallsEntryRemoved() {
        val log = mutableListOf<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.remove("a")
        assertEquals(listOf("a=A>null"), log)
    }

    @Test
    fun testPutCallsEntryRemoved() {
        val log = mutableListOf<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("a", "A2")
        assertEquals(listOf("a=A>A2"), log)
    }

    /**
     * Test what happens when a value is added to the map while create is
     * working. The map value should be returned by get(), and the created value
     * should be released with entryRemoved().
     */
    @Test
    fun testCreateWithConcurrentPut() {
        val log = mutableListOf<String>()
        val cache = object : LruCache<String, String>(3) {
            override fun create(key: String): String {
                put(key, "B")
                return "A"
            }

            @Suppress("UNUSED_PARAMETER")
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: String,
                newValue: String?
            ) {
                log.add("$key=$oldValue>$newValue")
            }
        }
        assertEquals("B", cache.get("a"))
        assertEquals(listOf("a=A>B"), log)
    }

    /**
     * Test what happens when two creates happen concurrently. The result from
     * the first create to return is returned by both gets. The other created
     * values should be released with entryRemove().
     */
    @Test
    fun testCreateWithConcurrentCreate() {
        val log = mutableListOf<String>()
        val cache = object : LruCache<String, Int>(3) {
            var mCallCount = 0
            override fun create(key: String): Int {
                return if (mCallCount++ == 0) {
                    assertEquals(2, get(key))
                    1
                } else {
                    2
                }
            }

            @Suppress("UNUSED_PARAMETER")
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Int,
                newValue: Int?
            ) {
                log.add("$key=$oldValue>$newValue")
            }
        }
        assertEquals(2, cache.get("a"))
        assertEquals(listOf("a=1>2"), log)
    }

    private fun newCreatingCache(): LruCache<String, String> {
        return object : LruCache<String, String>(3) {
            override fun create(key: String): String? {
                return if (key.length > 1) "created-$key" else null
            }
        }
    }

    private fun newRemovalLogCache(log: MutableList<String>): LruCache<String, String> {
        return object : LruCache<String, String>(3) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: String,
                newValue: String?
            ) {
                val message = if (evicted) "$key=$oldValue" else "$key=$oldValue>$newValue"
                log.add(message)
            }
        }
    }

    private fun assertHit(cache: LruCache<String, String>, key: String, value: String) {
        assertEquals(value, cache.get(key))
        expectedHitCount++
        assertStatistics(cache)
    }

    private fun assertMiss(cache: LruCache<String, String>, key: String) {
        assertEquals(null, cache.get(key))
        expectedMissCount++
        assertStatistics(cache)
    }

    private fun assertCreated(cache: LruCache<String, String>, key: String, value: String) {
        assertEquals(value, cache.get(key))
        expectedMissCount++
        expectedCreateCount++
        assertStatistics(cache)
    }

    private fun assertStatistics(cache: LruCache<*, *>) {
        assertEquals(expectedCreateCount, cache.createCount(), "create count")
        assertEquals(expectedPutCount, cache.putCount(), "put count")
        assertEquals(expectedHitCount, cache.hitCount(), "hit count")
        assertEquals(expectedMissCount, cache.missCount(), "miss count")
        assertEquals(expectedEvictionCount, cache.evictionCount(), "eviction count")
    }

    private fun <T> assertSnapshot(cache: LruCache<T, T>, vararg keysAndValues: T) {
        val actualKeysAndValues = mutableListOf<T>()
        for ((key, value) in cache.snapshot()) {
            actualKeysAndValues.add(key)
            actualKeysAndValues.add(value)
        }
        // assert using lists because order is important for LRUs
        assertEquals(keysAndValues.asList(), actualKeysAndValues)
    }
}