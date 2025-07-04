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

package androidx.camera.camera2.pipe.core

import android.os.Build
import androidx.camera.camera2.pipe.core.PruningProcessingQueue.Companion.processIn
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class PruningProcessingQueueTest {
    private val testScope = TestScope()
    private val processingScope =
        CoroutineScope(
            Job() +
                StandardTestDispatcher(testScope.testScheduler) +
                CoroutineExceptionHandler { _, throwable -> lastUncaughtException = throwable }
        )

    private var lastUncaughtException: Throwable? = null
    private val pruningCalls = mutableListOf<List<Int>>()
    private val processingCalls = mutableListOf<Int>()
    private val unprocessedElements = mutableListOf<List<Int>>()
    private val unprocessElementHandler: (List<Int>) -> Unit = {
        unprocessedElements.add(it.toMutableList())
    }

    @Test
    fun processingQueueBuffersItems() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                    capacity = 2,
                    onUnprocessedElements = unprocessElementHandler,
                ) {}

            assertThat(processingQueue.tryEmit(1)).isTrue()
            assertThat(processingQueue.tryEmit(2)).isTrue()
            assertThat(processingQueue.tryEmit(3)).isFalse() // Queue is full (2 items)
        }

    @Test
    fun processingQueueProcessesElements() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        capacity = 2,
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                    }
                    .processIn(processingScope)

            assertThat(processingQueue.tryEmit(1)).isTrue()
            assertThat(processingQueue.tryEmit(2)).isTrue()
            assertThat(processingQueue.tryEmit(3)).isFalse() // Queue is full

            advanceUntilIdle() // Processing loop runs

            // The pruning step should continue to receive elements whenever possible to reduce
            // prune calls. It should therefore be called only once with [1, 2].
            assertThat(pruningCalls).containsExactly(listOf(1, 2))
            // Processing loop receives 1 and 2.
            assertThat(processingCalls).containsExactly(1, 2)

            processingScope.cancel()
        }

    @Test
    fun processingQueuePrunesElements() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        prune = { elements ->
                            pruningCalls.add(elements.toList())

                            // Prune algorithm: A number supersedes all preceding numbers that are
                            // smaller than it. Repeat |size - 1| times for all neighboring pairs -
                            // the biggest number can supersede smaller numbers at most |size - 1|
                            // times.
                            repeat(elements.size - 1) {
                                for (i in 0..elements.size - 2) {
                                    if (elements[i] < elements[i + 1]) {
                                        elements.removeAt(i)
                                        break
                                    }
                                }
                            }
                        },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                    }
                    .processIn(processingScope)

            processingQueue.tryEmit(2)
            processingQueue.tryEmit(5)
            processingQueue.tryEmit(4)
            advanceUntilIdle()

            processingQueue.tryEmit(1)
            processingQueue.tryEmit(3)
            processingQueue.tryEmit(6)
            advanceUntilIdle()

            // Processing loop should run the following:
            // 1. prune([2, 5, 4]) --> reduces the list to [5, 4]
            // 2. process(5)
            // 3. process(4)
            // 4. prune([1, 3, 6]) --> reduces the list to [6]
            // 5. process(6)
            assertThat(pruningCalls).containsExactly(listOf(2, 5, 4), listOf(1, 3, 6))
            assertThat(processingCalls).containsExactly(5, 4, 6)
        }

    @Test
    fun processingQueueIterativelyAggregatesAndProcessesElements() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        capacity = 2,
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                    }
                    .processIn(processingScope)

            processingQueue.tryEmit(1)
            processingQueue.tryEmit(2)
            advanceUntilIdle()

            processingQueue.tryEmit(3)
            advanceUntilIdle()

            processingQueue.tryEmit(4)
            processingQueue.tryEmit(5)
            advanceUntilIdle()

            // Processing loop should run the following:
            // 1. prune([1, 2])
            // 2. process(1)
            // 3. process(2)
            // 4. prune([3])
            // 5. process(3)
            // 6. prune([4, 5])
            // 7. process(4)
            // 8. process(5)
            assertThat(pruningCalls).containsExactly(listOf(1, 2), listOf(3), listOf(4, 5))
            assertThat(processingCalls).containsExactly(1, 2, 3, 4, 5)

            processingScope.cancel()
        }

    @Test
    fun processInOnCanceledScopeInvokesOnUnprocessedElements() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                    prune = { pruningCalls.add(it.toList()) },
                    onUnprocessedElements = unprocessElementHandler,
                ) {}

            processingQueue.tryEmit(1)
            processingQueue.tryEmit(2)

            processingScope.cancel()
            processingQueue.processIn(processingScope)

            // Processing loop does not receive anything
            assertThat(pruningCalls).isEmpty()
            assertThat(unprocessedElements).containsExactly(listOf(1, 2))
        }

    @Test
    fun cancellingProcessingScopeStopsProcessing() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                    }
                    .processIn(processingScope)

            processingQueue.tryEmit(1)
            processingQueue.tryEmit(2)
            advanceUntilIdle()

            assertThat(processingQueue.tryEmit(3)).isTrue() // Normal
            assertThat(processingQueue.tryEmit(4)).isTrue() // Normal
            processingScope.cancel()
            assertThat(processingQueue.tryEmit(5)).isTrue() // Channel hasn't been closed
            assertThat(processingQueue.tryEmit(6)).isTrue() // Channel hasn't been closed
            advanceUntilIdle()

            assertThat(processingQueue.tryEmit(7)).isFalse() // fails
            assertThat(processingQueue.tryEmit(8)).isFalse() // fails

            assertThat(pruningCalls).containsExactly(listOf(1, 2))
            assertThat(processingCalls).containsExactly(1, 2)
            assertThat(unprocessedElements).containsExactly(listOf(3, 4, 5, 6))
        }

    @Test
    fun longProcessingDoesNotBlockPruning() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                        delay(100)
                    }
                    .processIn(processingScope)

            processingQueue.emitChecked(1)
            processingQueue.emitChecked(2)
            processingQueue.emitChecked(3)
            advanceTimeBy(50) // Triggers initial processing call
            assertThat(processingCalls).containsExactly(1)

            processingQueue.emitChecked(4)
            processingQueue.emitChecked(5)
            advanceTimeBy(25) // Still processing 1, but elements are still aggregated and pruned.
            assertThat(processingCalls).containsExactly(1)

            processingQueue.emitChecked(6)
            advanceTimeBy(50) // Processed 1, and processing 2.
            assertThat(processingCalls).containsExactly(1, 2)

            processingQueue.emitChecked(7)
            processingQueue.emitChecked(8)
            advanceUntilIdle() // Last update includes all previous updates.

            assertThat(pruningCalls)
                .containsExactly(
                    listOf(1, 2, 3),
                    listOf(2, 3, 4, 5),
                    listOf(2, 3, 4, 5, 6),
                    listOf(3, 4, 5, 6, 7, 8),
                )
            assertThat(processingCalls).containsExactly(1, 2, 3, 4, 5, 6, 7, 8)
            processingScope.cancel()
        }

    @Test
    fun exceptionsDuringProcessingArePropagated() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                        delay(100)
                        throw RuntimeException("Test")
                    }
                    .processIn(processingScope)

            processingQueue.emitChecked(1)
            processingQueue.emitChecked(2)
            processingQueue.emitChecked(3)
            advanceTimeBy(50) // Triggers initial processing call, but not exception

            processingQueue.emitChecked(4)
            processingQueue.emitChecked(5)
            advanceUntilIdle() // Trigger exception.

            assertThat(pruningCalls).containsExactly(listOf(1, 2, 3), listOf(2, 3, 4, 5))
            assertThat(unprocessedElements).containsExactly(listOf(2, 3, 4, 5))
            assertThat(lastUncaughtException).isInstanceOf(RuntimeException::class.java)
        }

    @Test
    fun duplicateItemsAreNotOmitted() =
        testScope.runTest {
            val processingQueue =
                PruningProcessingQueue<Int>(
                        prune = { pruningCalls.add(it.toList()) },
                        onUnprocessedElements = unprocessElementHandler,
                    ) {
                        processingCalls.add(it)
                    }
                    .processIn(processingScope)

            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            advanceUntilIdle()
            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            advanceUntilIdle()

            assertThat(pruningCalls).containsExactly(listOf(1, 1), listOf(1, 1, 1))
            assertThat(processingCalls).containsExactly(1, 1, 1, 1, 1)
        }
}
