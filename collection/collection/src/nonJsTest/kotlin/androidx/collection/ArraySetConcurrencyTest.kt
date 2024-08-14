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
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

internal class ArraySetConcurrencyTest {
    private val set = ArraySet<String>()

    /**
     * Attempt to generate a ConcurrentModificationException in ArraySet.
     *
     * ArraySet is explicitly documented to be non-thread-safe, yet it's easy to accidentally screw
     * this up; ArraySet should (in the spirit of the core Java collection types) make an effort to
     * catch this and throw ConcurrentModificationException instead of crashing somewhere in its
     * internals.
     */
    @Test
    fun testConcurrentModificationException() = runTest {
        var error: Throwable? = null
        val nThreads = 20
        var nActiveThreads = 0
        val lock = Lock()
        val context: CoroutineContext = Dispatchers.Default
        val scope = CoroutineScope(context)

        repeat(nThreads) {
            scope.launch {
                lock.synchronized { nActiveThreads++ }

                while (isActive) {
                    val add = Random.nextBoolean()
                    try {
                        if (add) {
                            set.add(Random.nextLong().toString())
                        } else {
                            set.clear()
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

        scope.cancel()

        error?.also { throw it }
    }

    /** Check to make sure the same operations behave as expected in a single thread. */
    @Test
    fun testNonConcurrentAccesses() {
        repeat(100_000) { i ->
            set.add("key $i")
            if (i % 200 == 0) {
                print(".")
            }
            if (i % 500 == 0) {
                set.clear()
            }
        }
    }
}
