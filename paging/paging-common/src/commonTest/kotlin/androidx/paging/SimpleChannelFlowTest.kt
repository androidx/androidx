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

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SimpleChannelFlowTest {
    val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun basic_CHANNEL_FLOW() = basic(Impl.CHANNEL_FLOW)

    @Test
    fun basic_SIMPLE_CHANNEL_FLOW() = basic(Impl.SIMPLE_CHANNEL_FLOW)

    private fun basic(impl: Impl) {
        val channelFlow = createFlow<Int>(impl) {
            send(1)
            send(2)
        }
        testScope.runTest {
            val items = channelFlow.toList()
            assertThat(items).containsExactly(1, 2)
        }
    }

    @Test
    fun emitWithLaunch_CHANNEL_FLOW() = emitWithLaunch(Impl.CHANNEL_FLOW)

    @Test
    fun emitWithLaunch_SIMPLE_CHANNEL_FLOW() = emitWithLaunch(Impl.SIMPLE_CHANNEL_FLOW)

    private fun emitWithLaunch(impl: Impl) {
        val channelFlow = createFlow<Int>(impl) {
            launch(coroutineContext, CoroutineStart.UNDISPATCHED) {
                send(1)
                delay(100)
                send(2)
            }
            send(3)
        }
        testScope.runTest {
            val items = channelFlow.toList()
            assertThat(items).containsExactly(1, 3, 2).inOrder()
        }
    }

    @Test
    fun closedByCollector_CHANNEL_FLOW() = closedByCollector(Impl.CHANNEL_FLOW)

    @Test
    fun closedByCollector_SIMPLE_CHANNEL_FLOW() = closedByCollector(Impl.SIMPLE_CHANNEL_FLOW)

    private fun closedByCollector(impl: Impl) {
        val emittedValues = mutableListOf<Int>()
        val channelFlow = createFlow<Int>(impl) {
            repeat(10) {
                send(it)
                emittedValues.add(it)
            }
        }
        testScope.runTest {
            assertThat(channelFlow.take(4).toList()).containsExactly(0, 1, 2, 3)
            assertThat(emittedValues).containsExactlyElementsIn((0..9).toList())
        }
    }

    @Test
    fun closedByCollector_noBuffer_CHANNEL_FLOW() = closedByCollector_noBuffer(Impl.CHANNEL_FLOW)

    @Test
    fun closedByCollector_noBuffer_SIMPLE_CHANNEL_FLOW() =
        closedByCollector_noBuffer(Impl.SIMPLE_CHANNEL_FLOW)

    private fun closedByCollector_noBuffer(impl: Impl) {
        val emittedValues = mutableListOf<Int>()
        val channelFlow = createFlow<Int>(impl) {
            repeat(10) {
                send(it)
                emittedValues.add(it)
            }
        }
        testScope.runTest {
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
    fun awaitClose_CHANNEL_FLOW() = awaitClose(Impl.CHANNEL_FLOW)

    @Test
    fun awaitClose_SIMPLE_CHANNEL_FLOW() = awaitClose(Impl.SIMPLE_CHANNEL_FLOW)

    private fun awaitClose(impl: Impl) {
        val lastDispatched = CompletableDeferred<Int>()
        val channelFlow = createFlow<Int>(impl) {
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
        testScope.runTest {
            channelFlow.takeWhile { it < 3 }.toList()
            assertThat(lastDispatched.await()).isEqualTo(3)
        }
    }

    @Test
    fun scopeGetsCancelled_CHANNEL_FLOW() = scopeGetsCancelled(Impl.CHANNEL_FLOW)

    @Test
    fun scopeGetsCancelled_SIMPLE_CHANNEL_FLOW() = scopeGetsCancelled(Impl.SIMPLE_CHANNEL_FLOW)

    private fun scopeGetsCancelled(impl: Impl) {
        var producerException: Throwable? = null
        val dispatched = mutableListOf<Int>()
        val channelFlow = createFlow<Int>(impl) {
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
        testScope.runTest {
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
    fun collectorThrows_CHANNEL_FLOW() = collectorThrows(Impl.CHANNEL_FLOW)

    @Test
    fun collectorThrows_SIMPLE_CHANNEL_FLOW() = collectorThrows(Impl.SIMPLE_CHANNEL_FLOW)

    private fun collectorThrows(impl: Impl) {
        var producerException: Throwable? = null
        val channelFlow = createFlow<Int>(impl) {
            try {
                send(1)
                delay(1000)
                fail("should not arrive here")
            } catch (th: Throwable) {
                producerException = th
                throw th
            }
        }
        testScope.runTest {
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
    fun upstreamThrows_CHANNEL_FLOW() = upstreamThrows(Impl.CHANNEL_FLOW)

    @Test
    fun upstreamThrows_SIMPLE_CHANNEL_FLOW() = upstreamThrows(Impl.SIMPLE_CHANNEL_FLOW)

    private fun upstreamThrows(impl: Impl) {
        var producerException: Throwable? = null
        val upstream = flow<Int> {
            emit(5)
            delay(100)
            emit(13)
        }
        val combinedFlow = upstream.flatMapLatest { upstreamValue ->
            createFlow<Int>(impl) {
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
        testScope.runTest {
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
    fun cancelingChannelClosesTheFlow_CHANNEL_FLOW() =
        cancelingChannelClosesTheFlow(Impl.CHANNEL_FLOW)

    @Test
    fun cancelingChannelClosesTheFlow_SIMPLE_CHANNEL_FLOW() =
        cancelingChannelClosesTheFlow(Impl.SIMPLE_CHANNEL_FLOW)

    private fun cancelingChannelClosesTheFlow(impl: Impl) {
        val flow = createFlow<Int>(impl) {
            send(1)
            close()
            awaitCancellation()
        }
        testScope.runTest {
            assertThat(flow.toList()).containsExactly(1)
        }
    }

    private fun <T> createFlow(
        impl: Impl,
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

    enum class Impl {
        CHANNEL_FLOW,
        SIMPLE_CHANNEL_FLOW
    }
}