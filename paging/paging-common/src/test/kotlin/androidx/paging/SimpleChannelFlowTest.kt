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

package androidx.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CancellationException
import kotlin.test.fail

@RunWith(Parameterized::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SimpleChannelFlowTest(
    private val impl: Impl
) {
    val testScope = TestCoroutineScope()

    @Test
    fun basic() {
        val channelFlow = createFlow<Int> {
            send(1)
            send(2)
        }
        testScope.runBlockingTest {
            val items = channelFlow.toList()
            assertThat(items).containsExactly(1, 2)
        }
    }

    @Test
    fun emitWithLaunch() {
        val channelFlow = createFlow<Int> {
            launch {
                send(1)
                delay(100)
                send(2)
            }
            send(3)
        }
        testScope.runBlockingTest {
            val items = channelFlow.toList()
            assertThat(items).containsExactly(1, 3, 2).inOrder()
        }
    }

    @Test
    fun closedByCollector() {
        val emittedValues = mutableListOf<Int>()
        val channelFlow = createFlow<Int> {
            repeat(10) {
                send(it)
                emittedValues.add(it)
            }
        }
        testScope.runBlockingTest {
            assertThat(channelFlow.take(4).toList()).containsExactly(0, 1, 2, 3)
            assertThat(emittedValues).containsExactlyElementsIn((0..9).toList())
        }
    }

    @Test
    fun closedByCollector_noBuffer() {
        val emittedValues = mutableListOf<Int>()
        val channelFlow = createFlow<Int> {
            repeat(10) {
                send(it)
                emittedValues.add(it)
            }
        }
        testScope.runBlockingTest {
            assertThat(channelFlow.buffer(0).take(4).toList()).containsExactly(0, 1, 2, 3)
            when (impl) {
                Impl.CHANNEL_FLOW -> {
                    assertThat(emittedValues).containsExactly(0, 1, 2, 3)
                }
                else -> {
                    // simple channel flow cannot fuse properly, hence has an extra value
                    assertThat(emittedValues).containsExactly(0, 1, 2, 3, 4)
                }
            }
        }
    }

    @Test
    fun awaitClose() {
        val lastDispatched = CompletableDeferred<Int>()
        val channelFlow = createFlow<Int> {
            var dispatched = -1
            launch {
                repeat(10) {
                    dispatched = it
                    send(it)
                    delay(100)
                }
            }
            awaitClose {
                assertThat(lastDispatched.isActive).isTrue()
                lastDispatched.complete(dispatched)
            }
        }
        testScope.runBlockingTest {
            channelFlow.takeWhile { it < 3 }.toList()
            assertThat(lastDispatched.await()).isEqualTo(3)
        }
    }

    @Test
    fun scopeGetsCancelled() {
        var producerException: Throwable? = null
        val dispatched = mutableListOf<Int>()
        val channelFlow = createFlow<Int> {
            try {
                repeat(20) {
                    send(it)
                    dispatched.add(it)
                    delay(100)
                }
            } catch (th: Throwable) {
                producerException = th
                throw th
            }
        }
        testScope.runBlockingTest {
            val collection = launch {
                channelFlow.toList()
            }
            advanceTimeBy(250)
            collection.cancel(CancellationException("test message"))
            collection.join()
            assertThat(dispatched).containsExactly(0, 1, 2)
            assertThat(producerException).hasMessageThat()
                .contains("test message")
        }
    }

    @Test
    fun collectorThrows() {
        var producerException: Throwable? = null
        val channelFlow = createFlow<Int> {
            try {
                send(1)
                delay(1000)
                fail("should not arrive here")
            } catch (th: Throwable) {
                producerException = th
                throw th
            }
        }
        testScope.runBlockingTest {
            runCatching {
                channelFlow.collect {
                    throw IllegalArgumentException("expected failure")
                }
            }
        }
        assertThat(producerException).hasMessageThat()
            .contains("consumer had failed")
    }

    @Test
    fun upstreamThrows() {
        var producerException: Throwable? = null
        val upstream = flow<Int> {
            emit(5)
            delay(100)
            emit(13)
        }
        val combinedFlow = upstream.flatMapLatest { upstreamValue ->
            createFlow<Int> {
                try {
                    send(upstreamValue)
                    delay(2000)
                    send(upstreamValue * 2)
                } catch (th: Throwable) {
                    if (producerException == null) {
                        producerException = th
                    }
                    throw th
                }
            }
        }
        testScope.runBlockingTest {
            assertThat(
                combinedFlow.toList()
            ).containsExactly(
                5, 13, 26
            )
        }
        assertThat(producerException).hasMessageThat()
            .contains("Child of the scoped flow was cancelled")
    }

    @Test
    fun cancelingChannelClosesTheFlow() {
        val flow = createFlow<Int> {
            send(1)
            close()
            awaitCancellation()
        }
        testScope.runBlockingTest {
            assertThat(flow.toList()).containsExactly(1)
        }
    }

    private fun <T> createFlow(
        block: suspend TestProducerScope<T>.() -> Unit
    ): Flow<T> {
        return when (impl) {
            Impl.CHANNEL_FLOW -> channelFlow {
                ChannelFlowTestProducerScope(this).block()
            }
            Impl.SIMPLE_CHANNEL_FLOW -> simpleChannelFlow {
                SimpleChannelFlowTestProducerScope(this).block()
            }
        }
    }

    // we want to run these tests with both channelFlow and simpleChannelFlow to check behavior
    // equality, hence the abstraction
    interface TestProducerScope<T> : CoroutineScope, SendChannel<T> {
        suspend fun awaitClose(block: () -> Unit)
    }

    internal class SimpleChannelFlowTestProducerScope<T>(
        private val delegate: SimpleProducerScope<T>
    ) : TestProducerScope<T>, CoroutineScope by delegate, SendChannel<T> by delegate {
        override suspend fun awaitClose(block: () -> Unit) {
            delegate.awaitClose(block)
        }
    }

    class ChannelFlowTestProducerScope<T>(
        private val delegate: ProducerScope<T>
    ) : TestProducerScope<T>, CoroutineScope by delegate, SendChannel<T> by delegate {
        override suspend fun awaitClose(block: () -> Unit) {
            delegate.awaitClose(block)
        }
    }

    companion object {
        @Parameterized.Parameters(name = "impl={0}")
        @JvmStatic
        fun params() = Impl.values()
    }

    enum class Impl {
        CHANNEL_FLOW,
        SIMPLE_CHANNEL_FLOW
    }
}