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
import androidx.camera.camera2.pipe.core.ProcessingQueue.Companion.processIn
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
class ProcessingQueueTest {
    private val testScope = TestScope()
    private val processingScope =
        CoroutineScope(
            Job() +
                StandardTestDispatcher(testScope.testScheduler) +
                CoroutineExceptionHandler { _, throwable -> lastUncaughtException = throwable }
        )

    private var lastUncaughtException: Throwable? = null
    private val unprocessedElements = mutableListOf<List<Int>>()
    private val processingCalls = mutableListOf<List<Int>>()
    private val unprocessElementHandler: (List<Int>) -> Unit = {
        unprocessedElements.add(it.toMutableList())
    }

    @Test
    fun processingQueueBuffersItems() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(
                    capacity = 2,
                    onUnprocessedElements = unprocessElementHandler
                ) {}

            assertThat(processingQueue.tryEmit(1)).isTrue()
            assertThat(processingQueue.tryEmit(2)).isTrue()
            assertThat(processingQueue.tryEmit(3)).isFalse() // Queue is full (2 items)
        }

    @Test
    fun processInProcessesItems() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(
                        capacity = 2,
                        onUnprocessedElements = unprocessElementHandler
                    ) {
                        processingCalls.add(it.toMutableList())
                        it.removeAt(0)
                    }
                    .processIn(processingScope)

            assertThat(processingQueue.tryEmit(1)).isTrue()
            assertThat(processingQueue.tryEmit(2)).isTrue()
            assertThat(processingQueue.tryEmit(3)).isFalse() // Queue is full

            advanceUntilIdle() // Processing loop runs

            // Processing loop receives [1, 2], removes 1, then is re-invoked with [2]
            assertThat(processingCalls).containsExactly(listOf(1, 2), listOf(2))

            processingScope.cancel()
        }

    @Test
    fun processingQueueIterativelyProcessesElements() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(
                        capacity = 2,
                        onUnprocessedElements = unprocessElementHandler
                    ) {
                        processingCalls.add(it.toMutableList())
                        it.removeAt(0) // Mutation works
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

            // Processing loop run 5 times:
            // [1, 2] (removes 1)
            // [2] (removes 2)
            // [3] (removes 3)
            // [4, 5] (removes 4)
            // [5] (removes 5)
            assertThat(processingCalls)
                .containsExactly(listOf(1, 2), listOf(2), listOf(3), listOf(4, 5), listOf(5))

            processingScope.cancel()
        }

    @Test
    fun processingQueueAggregatesElements() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                        processingCalls.add(it.toMutableList())
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

            // Processing loop does not remove anything
            assertThat(processingCalls)
                .containsExactly(listOf(1, 2), listOf(1, 2, 3), listOf(1, 2, 3, 4, 5))

            processingScope.cancel()
        }

    @Test
    fun processInOnCanceledScopeInvokesOnUnprocessedElements() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                    processingCalls.add(it.toMutableList())
                    it.clear()
                }

            processingQueue.tryEmit(1)
            processingQueue.tryEmit(2)

            processingScope.cancel()
            processingQueue.processIn(processingScope)

            // Processing loop does not receive anything
            assertThat(processingCalls).isEmpty()
            assertThat(unprocessedElements).containsExactly(listOf(1, 2))
        }

    @Test
    fun cancellingProcessingScopeStopsProcessing() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                        processingCalls.add(it.toMutableList())
                        it.clear()
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

            // Processing loop does not remove anything
            assertThat(processingCalls)
                .containsExactly(
                    listOf(1, 2),
                )
            // Processing loop does not remove anything
            assertThat(unprocessedElements).containsExactly(listOf(3, 4, 5, 6))
        }

    @Test
    fun longProcessingBlocksAggregateItems() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                        processingCalls.add(it.toMutableList())
                        delay(100)
                        it.clear()
                    }
                    .processIn(processingScope)

            processingQueue.emitChecked(1)
            processingQueue.emitChecked(2)
            processingQueue.emitChecked(3)
            advanceTimeBy(50) // Triggers initial processing call

            processingQueue.emitChecked(4)
            processingQueue.emitChecked(5)
            advanceTimeBy(25) // No updates, process function is still suspended

            processingQueue.emitChecked(6)
            advanceUntilIdle() // Last update includes all previous updates.

            // Processing loop does not remove anything
            assertThat(processingCalls)
                .containsExactly(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                )
            processingScope.cancel()
        }

    @Test
    fun exceptionsDuringProcessingArePropagated() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                        processingCalls.add(it.toMutableList())
                        it.clear()
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

            assertThat(processingCalls).containsExactly(listOf(1, 2, 3))
            assertThat(unprocessedElements).containsExactly(listOf(4, 5))
            assertThat(lastUncaughtException).isInstanceOf(RuntimeException::class.java)
        }

    @Test
    fun duplicateItemsAreNotOmitted() =
        testScope.runTest {
            val processingQueue =
                ProcessingQueue<Int>(onUnprocessedElements = unprocessElementHandler) {
                        processingCalls.add(it.toMutableList())
                        it.clear()
                    }
                    .processIn(processingScope)

            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            advanceUntilIdle()
            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            processingQueue.emitChecked(1)
            advanceUntilIdle()

            assertThat(processingCalls).containsExactly(listOf(1, 1), listOf(1, 1, 1))
        }
}
