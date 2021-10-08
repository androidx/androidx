/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.paging

import androidx.paging.CombineSource.INITIAL
import androidx.paging.CombineSource.OTHER
import androidx.paging.CombineSource.RECEIVER
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtTest {
    val testScope = TestCoroutineScope()

    @Test
    fun scan_basic() = testScope.runBlockingTest {
        val arguments = mutableListOf<Pair<Int, Int>>()
        assertThat(
            flowOf(1, 2, 3).simpleScan(0) { acc, value ->
                arguments.add(acc to value)
                value + acc
            }.toList()
        ).containsExactly(
            0, 1, 3, 6
        ).inOrder()
        assertThat(arguments).containsExactly(
            0 to 1,
            1 to 2,
            3 to 3
        ).inOrder()
    }

    @Test
    fun scan_initialValue() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleScan("x") { _, value ->
                "$value"
            }.toList()
        ).containsExactly("x")
    }

    @Test
    fun runningReduce_basic() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4).simpleRunningReduce { acc, value ->
                acc + value
            }.toList()
        ).containsExactly(1, 3, 6, 10)
    }

    @Test
    fun runningReduce_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleRunningReduce { acc, value ->
                acc + value
            }.toList()
        ).isEmpty()
    }

    @Test
    fun mapLatest() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4)
                .onEach {
                    delay(1)
                }
                .simpleMapLatest { value ->
                    delay(value.toLong())
                    "$value-$value"
                }.toList()
        ).containsExactly(
            "1-1", "4-4"
        ).inOrder()
    }

    @Test
    fun mapLatest_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>().simpleMapLatest { value ->
                "$value-$value"
            }.toList()
        ).isEmpty()
    }

    @Test
    fun flatMapLatest() = testScope.runBlockingTest {
        assertThat(
            flowOf(1, 2, 3, 4)
                .onEach {
                    delay(1)
                }
                .simpleFlatMapLatest { value ->
                    flow {
                        repeat(value) {
                            emit(value)
                        }
                    }
                }.toList()
        ).containsExactly(
            1, 2, 2, 3, 3, 3, 4, 4, 4, 4
        ).inOrder()
    }

    @Test
    fun flatMapLatest_empty() = testScope.runBlockingTest {
        assertThat(
            emptyFlow<Int>()
                .simpleFlatMapLatest {
                    flowOf(it)
                }.toList()
        ).isEmpty()
    }

    @Test
    fun combineWithoutBatching_buffersEmissions() = testScope.runBlockingTest {
        val flow1 = Channel<Int>(BUFFERED)
        val flow2 = Channel<String>(BUFFERED)

        val result = mutableListOf<String>()
        launch {
            flow1.consumeAsFlow()
                .combineWithoutBatching(flow2.consumeAsFlow()) { first, second, _ ->
                    "$first$second"
                }
                .collect(result::add)
        }

        flow1.send(1)
        advanceUntilIdle()
        assertThat(result).isEmpty()

        flow1.send(2)
        advanceUntilIdle()
        assertThat(result).isEmpty()

        flow2.send("A")
        advanceUntilIdle()
        assertThat(result).containsExactly("1A", "2A")

        // This should automatically propagate cancellation to the launched collector.
        flow1.close()
        flow2.close()
    }

    @Test
    fun combineWithoutBatching_doesNotBatchOnSlowTransform() = testScope.runBlockingTest {
        val flow1 = flowOf(1, 2, 3)
        val flow2 = flowOf("A", "B", "C")
        val slowTransform: suspend (Int, String) -> String = { num: Int, letter: String ->
            delay(10)
            "$num$letter"
        }

        val batchedCombine = flow1
            .combine(flow2, slowTransform)
            .toList()
        advanceUntilIdle()
        assertThat(batchedCombine).containsExactly("1A", "3B", "3C")

        val unbatchedCombine = flow1
            .combineWithoutBatching(flow2) { num, letter, _ -> slowTransform(num, letter) }
            .toList()
        advanceUntilIdle()
        assertThat(unbatchedCombine).containsExactly("1A", "2A", "2B", "3B", "3C")
    }

    @Test
    fun combineWithoutBatching_updateFrom() = testScope.runBlockingTest {
        val flow1 = Channel<Int>(BUFFERED)
        val flow2 = Channel<Int>(BUFFERED)

        val result = mutableListOf<CombineSource>()
        launch {
            flow1.consumeAsFlow()
                .combineWithoutBatching(flow2.consumeAsFlow()) { _, _, updateFrom ->
                    result.add(updateFrom)
                }
                .collect { }
        }

        flow1.send(1)
        advanceUntilIdle()
        assertThat(result).isEmpty()

        flow1.send(1)
        advanceUntilIdle()
        assertThat(result).isEmpty()

        flow2.send(2)
        advanceUntilIdle()
        assertThat(result).containsExactly(INITIAL, RECEIVER)

        flow1.send(1)
        flow2.send(2)
        advanceUntilIdle()
        assertThat(result).containsExactly(INITIAL, RECEIVER, RECEIVER, OTHER)

        // This should automatically propagate cancellation to the launched collector.
        flow1.close()
        flow2.close()
    }

    @Test
    fun combineWithoutBatching_collectorCancellationPropagates() = testScope.runBlockingTest {
        val flow1Emissions = mutableListOf<Int>()
        val flow1 = flowOf(1, 2, 3).onEach(flow1Emissions::add)
        val flow2Emissions = mutableListOf<String>()
        val flow2 = flowOf("A", "B", "C").onEach(flow2Emissions::add)
        val result = mutableListOf<Unit>()

        flow1
            .combineWithoutBatching(flow2) { _, _, _ ->
                result.add(Unit)
            }
            .first()

        advanceUntilIdle()

        // We can't guarantee whether cancellation will propagate before or after the second item
        // is emitted, but we should never get the third.
        assertThat(flow1Emissions.size).isIn(1..2)
        assertThat(flow2Emissions.size).isIn(1..2)
        assertThat(result.size).isIn(1..2)
    }

    @Test
    fun combineWithoutBatching_stressTest() {
        val flow1 = flow {
            repeat(1000) {
                if (Random.nextBoolean()) {
                    delay(1)
                }
                emit(it)
            }
        }
        val flow2 = flow {
            repeat(1000) {
                if (Random.nextBoolean()) {
                    delay(1)
                }
                emit(it)
            }
        }

        repeat(10) {
            val result = runBlocking {
                flow1.combineWithoutBatching(flow2) { first, second, _ -> first to second }
                    .toList()
            }

            // Never emit the same values twice.
            assertThat(result).containsNoDuplicates()

            // Assert order of emissions
            result.scan(0 to 0) { acc, next ->
                assertThat(next.first).isAtLeast(acc.first)
                assertThat(next.second).isAtLeast(acc.second)
                next
            }

            // Check we don't miss any emissions
            assertThat(result).hasSize(1999)
        }
    }

    class UnbatchedFlowCombinerTest {
        private data class SendResult<T1, T2>(
            val receiverValue: T1,
            val otherValue: T2,
            val updateFrom: CombineSource,
        )

        @Test
        fun onNext_receiverBuffers() = runBlockingTest {
            val result = mutableListOf<SendResult<Int, Int>>()
            val combiner = UnbatchedFlowCombiner<Int, Int> { a, b, c ->
                result.add(SendResult(a, b, c))
            }

            combiner.onNext(index = 0, value = 0)
            val job = launch {
                repeat(9) { receiverValue ->
                    combiner.onNext(index = 0, value = receiverValue + 1)
                }
            }

            // Ensure subsequent calls to onNext from receiver suspends forever until onNext
            // is called for the other Flow.
            advanceUntilIdle()
            assertThat(job.isCompleted).isFalse()
            // No events should be received until we receive an event from the other Flow.
            assertThat(result).isEmpty()

            combiner.onNext(index = 1, value = 0)

            advanceUntilIdle()
            assertThat(job.isCompleted).isTrue()
            assertThat(result).containsExactly(
                SendResult(0, 0, INITIAL),
                SendResult(1, 0, RECEIVER),
                SendResult(2, 0, RECEIVER),
                SendResult(3, 0, RECEIVER),
                SendResult(4, 0, RECEIVER),
                SendResult(5, 0, RECEIVER),
                SendResult(6, 0, RECEIVER),
                SendResult(7, 0, RECEIVER),
                SendResult(8, 0, RECEIVER),
                SendResult(9, 0, RECEIVER),
            )
        }

        @Test
        fun onNext_otherBuffers() = runBlockingTest {
            val result = mutableListOf<SendResult<Int, Int>>()
            val combiner = UnbatchedFlowCombiner<Int, Int> { a, b, c ->
                result.add(SendResult(a, b, c))
            }

            combiner.onNext(index = 1, value = 0)
            val job = launch {
                repeat(9) { receiverValue ->
                    combiner.onNext(index = 1, value = receiverValue + 1)
                }
            }

            // Ensure subsequent calls to onNext from receiver suspends forever until onNext
            // is called for the other Flow.
            advanceUntilIdle()
            assertThat(job.isCompleted).isFalse()
            // No events should be received until we receive an event from the other Flow.
            assertThat(result).isEmpty()

            combiner.onNext(index = 0, value = 0)

            advanceUntilIdle()
            assertThat(job.isCompleted).isTrue()
            assertThat(result).containsExactly(
                SendResult(0, 0, INITIAL),
                SendResult(0, 1, OTHER),
                SendResult(0, 2, OTHER),
                SendResult(0, 3, OTHER),
                SendResult(0, 4, OTHER),
                SendResult(0, 5, OTHER),
                SendResult(0, 6, OTHER),
                SendResult(0, 7, OTHER),
                SendResult(0, 8, OTHER),
                SendResult(0, 9, OTHER),
            )
        }

        @Test
        fun onNext_initialDispatchesFirst() = runBlockingTest {
            val result = mutableListOf<SendResult<Int, Int>>()
            val combiner = UnbatchedFlowCombiner<Int, Int> { a, b, c ->
                // Give a chance for other calls to onNext to run.
                yield()
                result.add(SendResult(a, b, c))
            }

            launch {
                repeat(1000) { value ->
                    combiner.onNext(index = 0, value = value)
                }
            }

            repeat(1) { value ->
                launch {
                    combiner.onNext(index = 1, value = value)
                }
            }

            advanceUntilIdle()
            assertThat(result.first()).isEqualTo(
                SendResult(0, 0, INITIAL),
            )
        }
    }
}