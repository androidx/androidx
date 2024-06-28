/*
 * Copyright 2024 The Android Open Source Project
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LruCacheNonWebTest {

    @Test
    fun testAbleToUpdateFromAnotherThreadWithBlockedEntryRemoved() {
        val cache =
            object : LruCache<String, String>(3) {
                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: String,
                    newValue: String?
                ) {
                    if (key in setOf("a", "b", "c", "d")) {
                        runBlocking(Dispatchers.Default) {
                            put("x", "X")
                        }
                    }
                }
            }

        cache.put("a", "A")
        cache.put("a", "A2") // replaced
        cache.put("b", "B")
        cache.put("c", "C")
        cache.put("d", "D") // single eviction
        cache.remove("a") // removed
        cache.evictAll() // multiple eviction
    }

    /** Makes sure that LruCache operations are correctly synchronized to guarantee consistency.  */
    @OptIn(DelicateCoroutinesApi::class) // Using GlobalScope in tests
    @Test
    fun consistentMultithreadedAccess() {
        var nonNullValues = 0
        var nullValues = 0
        var valuesPut = 0
        var conflicts = 0
        var removed = 0

        val rounds = 10000
        val key = "key"
        val value = 42
        val cache =
            object : LruCache<String, Int>(1) {
                override fun create(key: String): Int = value
            }

        val t0 =
            GlobalScope.launch(Dispatchers.Default) {
                repeat(rounds) {
                    if (cache[key] != null) {
                        nonNullValues++
                    } else {
                        nullValues++
                    }
                }
            }

        val t1 =
            GlobalScope.launch(Dispatchers.Default) {
                repeat(rounds) { i ->
                    if (i % 2 == 0) {
                        if (cache.put(key, value) != null) {
                            conflicts++
                        } else {
                            valuesPut++
                        }
                    } else {
                        cache.remove(key)
                        removed++
                    }
                }
            }

        runBlocking {
            t0.join()
            t1.join()
        }

        assertEquals(rounds, nonNullValues)
        assertEquals(0, nullValues)
        assertEquals(rounds, valuesPut + conflicts + removed)
    }

}