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

package androidx.camera.core.impl

import androidx.camera.core.impl.Observable.Observer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class StateObservableRobolectricTest {

    private lateinit var executor: ExecutorService

    @Before
    fun setUp() {
        executor = Executors.newFixedThreadPool(WORKER_THREADS)
    }

    @After
    fun tearDown() {
        executor.shutdown()
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun removeObserver_neverDeliversPostRemovalStateUnderConcurrency() {
        repeat(100) {
            val observable = MutableStateObservable.withInitialState(0)
            val observedPostRemoval = AtomicBoolean(false)
            val secondNotified = CountDownLatch(1)

            val observer1 =
                object : Observer<Int> {
                    override fun onNewData(value: Int?) {
                        if (value == 42) {
                            observedPostRemoval.set(true)
                        }
                    }

                    override fun onError(t: Throwable) {
                        observedPostRemoval.set(true)
                    }
                }
            val observer2 =
                object : Observer<Int> {
                    override fun onNewData(value: Int?) {
                        if (value == 42) {
                            secondNotified.countDown()
                        }
                    }

                    override fun onError(t: Throwable) {}
                }

            observable.addObserver(executor, observer1)
            observable.addObserver(executor, observer2)
            observable.setState(1)
            observable.removeObserver(observer1)
            observable.setState(42)

            assertThat(secondNotified.await(5, TimeUnit.SECONDS)).isTrue()
            executor.awaitAllWorkersIdle()
            assertThat(observedPostRemoval.get()).isFalse()
        }
    }

    @Test
    fun removeObservers_andSetErrorAfterRemove_neverDeliversPostRemovalUpdates() {
        val postRemovalError = IllegalStateException("post-removal")
        repeat(100) {
            val observable = MutableStateObservable.withInitialState(0)
            val unexpectedError = AtomicReference<Throwable?>(null)
            val secondErrorLatch = CountDownLatch(1)

            val observer1 =
                object : Observer<Int> {
                    override fun onNewData(value: Int?) {}

                    override fun onError(t: Throwable) {
                        unexpectedError.set(t)
                    }
                }
            val observer2 =
                object : Observer<Int> {
                    override fun onNewData(value: Int?) {}

                    override fun onError(t: Throwable) {
                        if (t === postRemovalError) {
                            secondErrorLatch.countDown()
                        }
                    }
                }

            observable.addObserver(executor, observer1)
            observable.removeObservers()
            observable.addObserver(executor, observer2)
            observable.setError(postRemovalError)

            assertThat(secondErrorLatch.await(5, TimeUnit.SECONDS)).isTrue()
            executor.awaitAllWorkersIdle()
            assertThat(unexpectedError.get()).isNull()
        }
    }

    @Test
    fun reentrantDirectExecutor_removeObserverDuringCallback_doesNotObserveSubsequentState() {
        val observable = MutableStateObservable.withInitialState(0)
        val receivedStates = mutableListOf<Int?>()

        lateinit var observer: Observer<Int>
        observer =
            object : Observer<Int> {
                override fun onNewData(value: Int?) {
                    receivedStates.add(value)
                    if (value == 0) {
                        observable.setState(1)
                        observable.removeObserver(observer)
                        observable.setState(42)
                    }
                }

                override fun onError(t: Throwable) {}
            }

        observable.addObserver(MoreExecutors.directExecutor(), observer)

        assertThat(receivedStates).containsExactly(0)
    }

    /**
     * Rendezvous barrier ensuring every worker thread in the fixed pool has completed all prior
     * queued tasks before returning.
     */
    private fun ExecutorService.awaitAllWorkersIdle(threads: Int = WORKER_THREADS) {
        val arrived = CountDownLatch(threads)
        val release = CountDownLatch(1)
        repeat(threads) {
            execute {
                arrived.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
        }
        try {
            assertThat(arrived.await(5, TimeUnit.SECONDS)).isTrue()
        } finally {
            release.countDown()
        }
    }

    private companion object {
        const val WORKER_THREADS = 4
    }
}
