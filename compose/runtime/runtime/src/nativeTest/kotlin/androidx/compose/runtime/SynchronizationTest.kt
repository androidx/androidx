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

package androidx.compose.runtime

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.test.*

private const val iterations = 100
private const val nWorkers = 4
private const val increments = 500
private const val nLocks = 5

private fun nest(lock: SynchronizedObject, nestedLocks: Int, count: AtomicInt) {
    synchronized(lock) {
        if (nestedLocks == 1) {
            val oldValue = count.value
            count.value = oldValue + 1
        } else {
            nest(lock, nestedLocks - 1, count)
        }
    }
}

/**
 * Test is taken from [kotlinx-atomicfu](https://github.com/Kotlin/kotlinx-atomicfu) with a few modifications.
*/
class SynchronizedTest {
    @Test
    fun stressCounterTest() {
        repeat(iterations) {
            val workers = Array(nWorkers) { Worker.start() }
            val counter = AtomicInt(0)
            val so = SynchronizedObject()
            workers.forEach { worker ->
                worker.execute(TransferMode.SAFE, {
                    counter to so
                }) { (count, lock) ->
                    repeat(increments) {
                        val nestedLocks = (1..3).random()
                        nest(lock, nestedLocks, count)
                    }
                }
            }
            workers.forEach {
                it.requestTermination().result
            }
            assertEquals(nWorkers * increments, counter.value)
        }
    }

    @Test
    fun manyLocksTest() {
        repeat(iterations) {
            val workers = Array(nWorkers) { Worker.start() }
            val counters = Array(nLocks) { AtomicInt(0) }
            val locks = Array(nLocks) { SynchronizedObject() }
            workers.forEach { worker ->
                worker.execute(TransferMode.SAFE, {
                    counters to locks
                }) { (counters, locks) ->
                    locks.forEachIndexed { i, lock ->
                        repeat(increments) {
                            synchronized(lock) {
                                val oldValue = counters[i].value
                                counters[i].value = oldValue + 1
                            }
                        }
                    }
                }
            }
            workers.forEach {
                it.requestTermination().result
            }
            assertEquals(nWorkers * nLocks * increments, counters.sumOf { it.value })
        }
    }
}