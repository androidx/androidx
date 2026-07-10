/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.latency.aggregators.internal

import androidx.ink.authoring.ExperimentalInkLatencyDataApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalInkLatencyDataApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class ConcurrentIntervalQueueTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun concurrentIntervalQueue_recordFromSameCoroutinePutsIntervalsInTheQueue() {
        val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

        queue.record(10, 20)
        queue.record(20, 30)

        assertThat(queue.size()).isEqualTo(2)
        queue.applyToOldestAndRecycle() { interval ->
            assertThat(interval.startNanos).isEqualTo(10)
            assertThat(interval.endNanos).isEqualTo(20)
        }
        queue.applyToOldestAndRecycle() { interval ->
            assertThat(interval.startNanos).isEqualTo(20)
            assertThat(interval.endNanos).isEqualTo(30)
        }
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun concurrentIntervalQueue_twoCoroutinesCanBothRecordAndRead() =
        testScope.runTest {
            val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

            launch {
                // Record two samples in coroutine A.
                delay(1.seconds)
                queue.record(10, 20)
                queue.record(20, 30)

                // We can read the samples recorded in coroutine B over here in A.
                delay(2.seconds)
                assertThat(queue.size()).isEqualTo(2)
                queue.applyToOldestAndRecycle() { interval ->
                    assertThat(interval.startNanos).isEqualTo(30)
                    assertThat(interval.endNanos).isEqualTo(40)
                }
                queue.applyToOldestAndRecycle() { interval ->
                    assertThat(interval.startNanos).isEqualTo(40)
                    assertThat(interval.endNanos).isEqualTo(50)
                }
            }

            launch {
                // We can read the samples recorded in coroutine A over here in B.
                delay(2.seconds)
                assertThat(queue.size()).isEqualTo(2)
                queue.applyToOldestAndRecycle() { interval ->
                    assertThat(interval.startNanos).isEqualTo(10)
                    assertThat(interval.endNanos).isEqualTo(20)
                }
                queue.applyToOldestAndRecycle() { interval ->
                    assertThat(interval.startNanos).isEqualTo(20)
                    assertThat(interval.endNanos).isEqualTo(30)
                }

                // Now record samples in B.
                queue.record(30, 40)
                queue.record(40, 50)
                delay(2.seconds)
            }

            testScope.advanceTimeBy(5.seconds)
        }

    @Test
    fun concurrentIntervalQueue_recordAllocatesNewIntervalsIfNotEnoughWerePreallocated() {
        // Preallocate 5 interval instances.
        val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

        // Record 5 intervals. This uses up the preallocated instances.
        assertThat(queue.numLateAllocations).isEqualTo(0)
        for (i in 1..5) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(0)

        // Record 3 more. New instances should automatically be allocated.
        for (i in 6..8) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(3)

        // Everything we recorded should show up in the queue.
        for (i in 1..8) {
            queue.applyToOldestAndRecycle() { interval ->
                assertThat(interval.startNanos).isEqualTo(10L * i)
                assertThat(interval.endNanos).isEqualTo(10L * i + 10)
            }
        }
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun concurrentIntervalQueue_applyToOldestAndRecyclePreventsNewAllocations() {
        // Preallocate 5 interval instances.
        val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

        // Record 4 intervals.
        assertThat(queue.numLateAllocations).isEqualTo(0)
        for (i in 1..4) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(0)

        // Read and recycle 3 of them.
        assertThat(queue.size()).isEqualTo(4)
        for (i in 1..3) {
            queue.applyToOldestAndRecycle() { interval ->
                assertThat(interval.startNanos).isEqualTo(10L * i)
                assertThat(interval.endNanos).isEqualTo(10L * i + 10)
            }
        }
        assertThat(queue.size()).isEqualTo(1)

        // Record 4 more. This should not require any new allocations.
        for (i in 5..8) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(0)

        // Read and recycle the rest of the 8 intervals.
        assertThat(queue.size()).isEqualTo(5)
        for (i in 4..8) {
            queue.applyToOldestAndRecycle() { interval ->
                assertThat(interval.startNanos).isEqualTo(10L * i)
                assertThat(interval.endNanos).isEqualTo(10L * i + 10)
            }
        }
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun concurrentIntervalQueue_recycleOldestLeavesSecondOldest() {
        val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

        // Record 4 intervals.
        for (i in 1..4) {
            queue.record(10L * i, 10L * i + 10)
        }

        // Recycle the oldest of them.
        assertThat(queue.size()).isEqualTo(4)
        queue.recycleOldest()
        assertThat(queue.size()).isEqualTo(3)

        // The next oldest is now the oldest interval.
        queue.applyToOldestAndRecycle() { interval ->
            assertThat(interval.startNanos).isEqualTo(20L)
            assertThat(interval.endNanos).isEqualTo(30L)
        }
    }

    @Test
    fun concurrentIntervalQueue_recycleOldestPreventsNewAllocations() {
        // Preallocate 5 interval instances.
        val queue = ConcurrentIntervalQueue(numPreallocatedIntervals = 5)

        // Record 4 intervals.
        assertThat(queue.numLateAllocations).isEqualTo(0)
        for (i in 1..4) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(0)

        // Recycle the oldest 3 of them.
        assertThat(queue.size()).isEqualTo(4)
        repeat(3) { queue.recycleOldest() }
        assertThat(queue.size()).isEqualTo(1)

        // Record 4 more. This should not require any new allocations.
        for (i in 5..8) {
            queue.record(10L * i, 10L * i + 10)
        }
        assertThat(queue.numLateAllocations).isEqualTo(0)

        // Recycle the rest of the 8 intervals.
        assertThat(queue.size()).isEqualTo(5)
        repeat(5) { queue.recycleOldest() }
        assertThat(queue.isEmpty()).isTrue()
    }
}
