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

import androidx.collection.internal.Lock
import androidx.collection.internal.synchronized
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking

internal class ArrayMapTest {

    private val map = ArrayMap<String, String>()

    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     *
     * ArrayMap is explicitly documented to be non-thread-safe, yet it's easy to accidentally screw
     * this up; ArrayMap should (in the spirit of the core Java collection types) make an effort to
     * catch this and throw ConcurrentModificationException instead of crashing somewhere in its
     * internals.
     */
    @OptIn(DelicateCoroutinesApi::class) // newFixedThreadPoolContext is delicate in jvm
    @Test
    fun testConcurrentModificationException() {
        var error: Throwable? = null
        val nThreads = 20
        var nActiveThreads = 0
        val lock = Lock()
        val dispatcher = newFixedThreadPoolContext(nThreads = nThreads, name = "ArraySetTest")
        val scope = CoroutineScope(dispatcher)

        repeat(nThreads) {
            scope.launch {
                lock.synchronized { nActiveThreads++ }

                while (isActive) {
                    val add = Random.nextBoolean()
                    try {
                        if (add) {
                            map[Random.nextLong().toString()] = "B_DONT_DO_THAT"
                        } else {
                            map.clear()
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        if (!add) {
                            error = e
                            throw e
                        } // Expected exception otherwise
                    } catch (e: ClassCastException) {
                        error = e
                        throw e
                    } catch (ignored: ConcurrentModificationException) {
                        // Expected exception
                    }
                }
            }
        }

        runBlocking(Dispatchers.Default) {
            // Wait until all worker threads are started
            for (i in 0 until 100) {
                if (lock.synchronized { nActiveThreads == nThreads }) {
                    break
                } else {
                    delay(timeMillis = 10L)
                }
            }

            // Allow the worker threads to run concurrently for some time
            delay(timeMillis = 100L)
        }

        scope.cancel()
        dispatcher.close()

        error?.also { throw it }
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    fun testNonConcurrentAccesses() {
        repeat(100_000) { i ->
            map["key %i"] = "B_DONT_DO_THAT"
            if (i % 500 == 0) {
                map.clear()
            }
        }
    }

    @Test
    fun testIsSubclassOfSimpleArrayMap() {
        @Suppress("USELESS_IS_CHECK")
        assertTrue(map is SimpleArrayMap<*, *>)
    }

    /**
     * Regression test for ensure capacity: b/224971154
     */
    @Test
    fun putAll() {
        val otherMap = HashMap<String, String>()
        otherMap["abc"] = "def"
        map.putAll(otherMap)
        assertEquals(1, map.size)
        assertEquals("abc", map.keyAt(0))
        assertEquals("def", map.valueAt(0))
    }

    @Test
    fun keys() {
        val map = ArrayMap<String, Int>()

        map["a"] = 1
        assertEquals(map.keys, setOf("a"))

        map["b"] = 2
        map["c"] = 3
        assertEquals(map.keys, setOf("a", "b", "c"))

        map.remove("b")
        map.remove("c")
        assertEquals(map.keys, setOf("a"))
    }

    @Test
    fun values() {
        val map = ArrayMap<String, Int>()

        map["a"] = 1
        assertContentEquals(map.values, listOf(1))

        map["b"] = 2
        map["c"] = 3
        assertContentEquals(map.values, listOf(1, 2, 3))

        map.remove("b")
        map.remove("c")
        assertContentEquals(map.values, listOf(1))
    }
}
