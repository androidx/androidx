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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

class LruCacheThreadedTest {
    @Test
    fun testEntryRemovedIsCalledWithoutSynchronization() {
        val cache = object : LruCache<String, String>(3) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: String,
                newValue: String?
            ) {
                assertFalse(Thread.holdsLock(this))
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
    @Test
    fun consistentMultithreadedAccess() {
        class Tally {
            var mNonNullValues = 0
            var mNullValues = 0
            var mValuesPut = 0
            var mConflicts = 0
            var mRemoved = 0
        }

        val tally = Tally()
        val rounds = 10000
        val key = "key"
        val value = 42
        val cache = object : LruCache<String, Int?>(1) {
            override fun create(key: String): Int? {
                return value
            }
        }

        val r0 = Runnable {
            for (i in 0 until rounds) {
                if (cache.get(key) != null) {
                    tally.mNonNullValues++
                } else {
                    tally.mNullValues++
                }
            }
        }
        val r1 = Runnable {
            for (i in 0 until rounds) {
                if (i % 2 == 0) {
                    if (cache.put(key, value) != null) {
                        tally.mConflicts++
                    } else {
                        tally.mValuesPut++
                    }
                } else {
                    cache.remove(key)
                    tally.mRemoved++
                }
            }
        }

        val t0: Thread = Thread(r0)
        val t1: Thread = Thread(r1)
        t0.start()
        t1.start()
        try {
            t0.join()
            t1.join()
        } catch (e: InterruptedException) {
            fail()
        }
        assertEquals(rounds.toLong(), tally.mNonNullValues.toLong())
        assertEquals(0, tally.mNullValues.toLong())
        assertEquals(
            rounds.toLong(),
            tally.mValuesPut + tally.mConflicts + tally.mRemoved.toLong()
        )
    }
}