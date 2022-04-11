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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class LruCacheTest {

    private var expectedCreateCount = 0
    private var expectedPutCount = 0
    private var expectedHitCount = 0
    private var expectedMissCount = 0
    private var expectedEvictionCount = 0

    @Test
    fun testStatistics() {
        val cache = LruCache<String, String>(3)
        assertStatistics(cache)
        assertNull(cache.put("a", "A"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertSnapshot(cache, "a", "A")
        assertNull(cache.put("b", "B"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertHit(cache, "b", "B")
        assertSnapshot(cache, "a", "A", "b", "B")
        assertNull(cache.put("c", "C"))
        expectedPutCount++
        assertStatistics(cache)
        assertHit(cache, "a", "A")
        assertHit(cache, "b", "B")
        assertHit(cache, "c", "C")
        assertSnapshot(cache, "a", "A", "b", "B", "c", "C")
        assertNull(cache.put("d", "D"))
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
        assertNull(cache.put("e", "E"))
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
        val created = cache["aa"]
        assertEquals("created-aa", created)
    }

    @Test
    fun testNoCreateOnCacheHit() {
        val cache = newCreatingCache()
        cache.put("aa", "put-aa")
        assertEquals("put-aa", cache["aa"])
    }

    @Test
    fun testConstructorDoesNotAllowZeroCacheSize() {
        assertFailsWith<IllegalArgumentException> {
            LruCache<String, String>(0)
        }
    }

    @Test
    fun testToString() {
        val cache = LruCache<String, String>(3)
        assertEquals("LruCache[maxSize=3,hits=0,misses=0,hitRate=0%]", cache.toString())
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.put("d", "D")
        cache["a"] // miss
        cache["b"] // hit
        cache["c"] // hit
        cache["d"] // hit
        cache["e"] // miss
        assertEquals("LruCache[maxSize=3,hits=3,misses=2,hitRate=60%]", cache.toString())
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
        val log = ArrayList<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        assertContentEquals(emptyList(), log)
        cache.put("d", "D")
        assertContentEquals(listOf("a=A"), log)
    }

    /**
     * Replacing the value for a key doesn't cause an eviction but it does bring
     * the replaced entry to the front of the queue.
     */
    @Test
    fun testPutCauseEviction() {
        val log = ArrayList<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.put("b", "B2")
        assertContentEquals(listOf("b=B>B2"), log)
        assertSnapshot(cache, "a", "A", "c", "C", "b", "B2")
    }

    @Test
    fun testCustomSizesImpactsSize() {
        val cache =
            object : LruCache<String, String>(10) {
                override fun sizeOf(key: String, value: String): Int =
                    key.length + value.length
            }
        assertEquals(0, cache.size())
        cache.put("a", "AA")
        assertEquals(3, cache.size())
        cache.put("b", "BBBB")
        assertEquals(8, cache.size())
        cache.put("a", "")
        assertEquals(6, cache.size())
    }

    @Test
    fun testEvictionWithCustomSizes() {
        val cache =
            object : LruCache<String, String>(4) {
                override fun sizeOf(key: String, value: String): Int = value.length
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
            override fun sizeOf(key: String, value: IntArray): Int = value[0]
        }
        val a = intArrayOf(4)
        cache.put("a", a)
        // get the cache size out of sync
        a[0] = 1
        assertEquals(4, cache.size())
        // evict something
        assertFailsWith<IllegalStateException> {
            cache.put("b", intArrayOf(2))
        }
    }

    @Test
    fun testEvictionThrowsWhenSizesAreNegative() {
        val cache =
            object : LruCache<String, String>(4) {
                override fun sizeOf(key: String, value: String): Int = -1
            }
        assertFailsWith<IllegalStateException> {
            cache.put("a", "A")
        }
    }

    /**
     * Naive caches evict at most one element at a time. This is problematic
     * because evicting a small element may be insufficient to make room for a
     * large element.
     */
    @Test
    fun testDifferentElementSizes() {
        val cache =
            object : LruCache<String, String>(10) {
                override fun sizeOf(key: String, value: String): Int = value.length
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
    fun testEvictAll() {
        val log = ArrayList<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.put("c", "C")
        cache.evictAll()
        assertEquals(0, cache.size())
        assertContentEquals(listOf("a=A", "b=B", "c=C"), log)
    }

    @Test
    fun testEvictAllEvictsSizeZeroElements() {
        val cache =
            object : LruCache<String, String>(10) {
                override fun sizeOf(key: String, value: String): Int = 0
            }
        cache.put("a", "A")
        cache.put("b", "B")
        cache.evictAll()
        assertSnapshot(cache)
    }

    @Test
    fun testRemoveWithCustomSizes() {
        val cache =
            object : LruCache<String, String>(10) {
                override fun sizeOf(key: String, value: String): Int = value.length
            }
        cache.put("a", "123456")
        cache.put("b", "1234")
        cache.remove("a")
        assertEquals(4, cache.size())
    }

    @Test
    fun testRemoveAbsentElement() {
        val cache = LruCache<String, String>(10)
        cache.put("a", "A")
        cache.put("b", "B")
        assertNull(cache.remove("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun testRemoveCallsEntryRemoved() {
        val log = ArrayList<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.remove("a")
        assertContentEquals(listOf("a=A>null"), log)
    }

    @Test
    fun testPutCallsEntryRemoved() {
        val log = ArrayList<String>()
        val cache = newRemovalLogCache(log)
        cache.put("a", "A")
        cache.put("a", "A2")
        assertContentEquals(listOf("a=A>A2"), log)
    }

    /**
     * Test what happens when a value is added to the map while create is
     * working. The map value should be returned by get(), and the created value
     * should be released with entryRemoved().
     */
    @Test
    fun testCreateWithConcurrentPut() {
        val log = java.util.ArrayList<String>()
        val cache =
            object : LruCache<String, String>(3) {
                override fun create(key: String): String {
                    put(key, "B")
                    return "A"
                }

                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: String,
                    newValue: String?
                ) {
                    log.add("$key=$oldValue>$newValue")
                }
            }
        assertEquals("B", cache["a"])
        assertContentEquals(listOf("a=A>B"), log)
    }

    /**
     * Test what happens when two creates happen concurrently. The result from
     * the first create to return is returned by both gets. The other created
     * values should be released with entryRemove().
     */
    @Test
    fun testCreateWithConcurrentCreate() {
        val log = ArrayList<String>()
        val cache =
            object : LruCache<String, Int>(3) {
                var mCallCount = 0

                override fun create(key: String): Int =
                    if (mCallCount++ == 0) {
                        assertEquals(2, get(key))
                        1
                    } else {
                        2
                    }

                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: Int,
                    newValue: Int?
                ) {
                    log.add("$key=$oldValue>$newValue")
                }
            }
        assertEquals(2, cache["a"])
        assertContentEquals(listOf("a=1>2"), log)
    }

    private fun newCreatingCache(): LruCache<String, String> =
        object : LruCache<String, String>(3) {
            override fun create(key: String): String? =
                if (key.length > 1) "created-$key" else null
        }

    private fun newRemovalLogCache(log: MutableList<String>): LruCache<String, String> =
        object : LruCache<String, String>(3) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: String,
                newValue: String?
            ) {
                log += if (evicted) "$key=$oldValue" else "$key=$oldValue>$newValue"
            }
        }

    private fun assertHit(cache: LruCache<String, String>, key: String, value: String) {
        assertEquals(value, cache[key])
        expectedHitCount++
        assertStatistics(cache)
    }

    private fun assertMiss(cache: LruCache<String, String>, key: String) {
        assertEquals(null, cache[key])
        expectedMissCount++
        assertStatistics(cache)
    }

    private fun assertCreated(cache: LruCache<String, String>, key: String, value: String) {
        assertEquals(value, cache[key])
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

    private fun <T : Any> assertSnapshot(cache: LruCache<T, T>, vararg keysAndValues: T) {
        val actualKeysAndValues = cache.snapshot().flatMap { (key, value) -> listOf(key, value) }
        // assert using lists because order is important for LRUs
        assertContentEquals(keysAndValues.asList(), actualKeysAndValues)
    }
}
