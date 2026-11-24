/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures.immediateFuture
import androidx.camera.video.internal.BufferProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class FakeBufferProviderTest {

    @Test
    fun addObserver_whenSetStateRacesBeforeNotificationQueued_endsInLatestState() {
        val provider = FakeBufferProvider(state = BufferProvider.State.INACTIVE)
        val pendingTasks = ArrayDeque<Runnable>()
        var transitioned = false
        val racingExecutor = Executor { task ->
            // Simulate another thread calling setState(ACTIVE) after addObserver() registers the
            // observer and releases its lock, but before addObserver() enqueues its initial state.
            if (!transitioned) {
                transitioned = true
                provider.setState(BufferProvider.State.ACTIVE)
            }
            pendingTasks.addLast(task)
        }
        val receivedStates = mutableListOf<BufferProvider.State?>()
        val observer =
            object : Observable.Observer<BufferProvider.State> {
                override fun onNewData(value: BufferProvider.State?) {
                    receivedStates.add(value)
                }

                override fun onError(t: Throwable) {
                    throw AssertionError(t)
                }
            }

        provider.addObserver(racingExecutor, observer)
        while (pendingTasks.isNotEmpty()) {
            pendingTasks.removeFirst().run()
        }

        assertThat(receivedStates).isNotEmpty()
        assertThat(receivedStates.last()).isEqualTo(BufferProvider.State.ACTIVE)
    }

    @Test
    fun addObserver_concurrentWithSetState_observerEndsInActiveState() {
        val racePool = Executors.newFixedThreadPool(2)
        val observerPool = Executors.newSingleThreadExecutor()
        try {
            val sequentialExecutor = CameraXExecutors.newSequentialExecutor(observerPool)
            repeat(200) {
                val provider = FakeBufferProvider(state = BufferProvider.State.INACTIVE)
                val receivedStates =
                    Collections.synchronizedList(mutableListOf<BufferProvider.State?>())
                val observer =
                    object : Observable.Observer<BufferProvider.State> {
                        override fun onNewData(value: BufferProvider.State?) {
                            receivedStates.add(value)
                        }

                        override fun onError(t: Throwable) {
                            throw AssertionError(t)
                        }
                    }

                val barrier = CyclicBarrier(2)
                val addFuture = racePool.submit {
                    barrier.await(5, TimeUnit.SECONDS)
                    provider.addObserver(sequentialExecutor, observer)
                }
                val setStateFuture = racePool.submit {
                    barrier.await(5, TimeUnit.SECONDS)
                    provider.setState(BufferProvider.State.ACTIVE)
                }

                addFuture.get(5, TimeUnit.SECONDS)
                setStateFuture.get(5, TimeUnit.SECONDS)

                val drainLatch = CountDownLatch(1)
                sequentialExecutor.execute { drainLatch.countDown() }
                assertThat(drainLatch.await(5, TimeUnit.SECONDS)).isTrue()

                assertThat(receivedStates).isNotEmpty()
                assertThat(receivedStates.last()).isEqualTo(BufferProvider.State.ACTIVE)
            }
        } finally {
            racePool.shutdownNow()
            observerPool.shutdownNow()
        }
    }

    @Test
    fun removeObserver_beforeQueuedNotificationRuns_doesNotNotifyObserver() {
        val provider = FakeBufferProvider(state = BufferProvider.State.INACTIVE)
        val pendingTasks = ArrayDeque<Runnable>()
        val deferredExecutor = Executor { task -> pendingTasks.addLast(task) }
        val receivedStates = mutableListOf<BufferProvider.State?>()
        val observer =
            object : Observable.Observer<BufferProvider.State> {
                override fun onNewData(value: BufferProvider.State?) {
                    receivedStates.add(value)
                }

                override fun onError(t: Throwable) {
                    throw AssertionError(t)
                }
            }

        provider.addObserver(deferredExecutor, observer)
        provider.removeObserver(observer)
        while (pendingTasks.isNotEmpty()) {
            pendingTasks.removeFirst().run()
        }

        assertThat(receivedStates).isEmpty()
    }

    @Test
    fun acquireBuffer_inactiveFailsAndActiveIncrementsIndex() {
        val acquiredIndices = mutableListOf<Int>()
        val provider =
            FakeBufferProvider(
                state = BufferProvider.State.INACTIVE,
                bufferFactory = { index ->
                    acquiredIndices.add(index)
                    immediateFuture(FakeInputBuffer())
                },
            )

        val failedFuture = provider.acquireBuffer()
        val exception = assertThrows(ExecutionException::class.java) { failedFuture.get() }
        assertThat(exception.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(acquiredIndices).isEmpty()

        provider.setState(BufferProvider.State.ACTIVE)
        assertThat(provider.acquireBuffer().get()).isNotNull()
        assertThat(provider.acquireBuffer().get()).isNotNull()
        assertThat(acquiredIndices).containsExactly(0, 1).inOrder()
    }
}
