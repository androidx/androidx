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

package androidx.camera.core.impl.utils.futures

import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class ListFutureTest {

    @Test
    fun cancelAfterChildCompletionQueued_withAllMustSucceedFalse_doesNotThrowIse() {
        val queue = ArrayDeque<Runnable>()
        val manualExecutor = Executor { queue.add(it) }
        val future1 = ResolvableFuture.create<Int>()
        val future2 = ResolvableFuture.create<Int>()

        val listFuture =
            ListFuture(listOf(future1, future2), /* allMustSucceed= */ false, manualExecutor)

        // Complete future1 so its setOneValue() listener is queued in manualExecutor
        future1.set(1)
        assertThat(queue).isNotEmpty()

        // Cancel listFuture (clears mValues = null) before the queued setOneValue() runs
        assertThat(listFuture.cancel(false)).isTrue()

        // Drain the queued setOneValue(); it must not throw IllegalStateException or NPE
        while (queue.isNotEmpty()) {
            queue.removeFirst().run()
        }
        assertThat(listFuture.isCancelled).isTrue()
    }

    @Test
    fun cancel_withAllMustSucceedFalse_cancelsListFutureAndChildren() {
        val future1 = ResolvableFuture.create<Int>()
        val future2 = ResolvableFuture.create<Int>()
        val listFuture = Futures.successfulAsList(listOf(future1, future2))

        assertThat(listFuture.cancel(true)).isTrue()
        assertThat(listFuture.isCancelled).isTrue()
        assertThat(future1.isCancelled).isTrue()
        assertThat(future2.isCancelled).isTrue()
    }

    @Test
    fun cancel_withAllMustSucceedTrue_returnsTrueAndPropagatesInterruptToAllChildren() {
        val child2Interrupted = AtomicBoolean(false)
        val future1 = ResolvableFuture.create<Int>()
        val delegate2 = ResolvableFuture.create<Int>()
        val future2 =
            object : ListenableFuture<Int> by delegate2 {
                override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                    if (mayInterruptIfRunning) {
                        child2Interrupted.set(true)
                    }
                    return delegate2.cancel(mayInterruptIfRunning)
                }
            }
        val listFuture = Futures.allAsList(listOf(future1, future2))

        assertThat(listFuture.cancel(true)).isTrue()
        assertThat(listFuture.isCancelled).isTrue()
        assertThat(future1.isCancelled).isTrue()
        assertThat(future2.isCancelled).isTrue()
        assertThat(child2Interrupted.get()).isTrue()
    }

    @Test
    fun concurrentCancelAndCompletion_doesNotThrowNullPointerException() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val failureRef = AtomicReference<Throwable?>(null)
            repeat(500) {
                val future1 = ResolvableFuture.create<Int>()
                val future2 = ResolvableFuture.create<Int>()
                val listFuture =
                    ListFuture(
                        listOf(future1, future2),
                        /* allMustSucceed= */ false,
                        CameraXExecutors.directExecutor(),
                    )

                val startLatch = CountDownLatch(1)
                val doneLatch = CountDownLatch(2)
                val cancelledByCaller = AtomicBoolean(false)

                executor.execute {
                    try {
                        startLatch.await()
                        future1.set(1)
                        future2.set(2)
                    } catch (t: Throwable) {
                        failureRef.compareAndSet(null, t)
                    } finally {
                        doneLatch.countDown()
                    }
                }

                executor.execute {
                    try {
                        startLatch.await()
                        cancelledByCaller.set(listFuture.cancel(true))
                    } catch (t: Throwable) {
                        failureRef.compareAndSet(null, t)
                    } finally {
                        doneLatch.countDown()
                    }
                }

                startLatch.countDown()
                assertThat(doneLatch.await(5, TimeUnit.SECONDS)).isTrue()
                assertThat(failureRef.get()).isNull()
                assertThat(listFuture.isDone).isTrue()
                assertThat(listFuture.isCancelled).isEqualTo(cancelledByCaller.get())
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
