/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.arch.core.executor.TaskExecutorWithFakeMainThread
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.util.InstantTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class ComputableLiveDataTest {
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var lifecycleOwner: TestLifecycleOwner

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        lifecycleOwner = TestLifecycleOwner(
            Lifecycle.State.INITIALIZED,
            UnconfinedTestDispatcher(null, null)
        )
    }

    @Before
    fun swapExecutorDelegate() {
        taskExecutor = spy(InstantTaskExecutor())
        ArchTaskExecutor.getInstance().setDelegate(taskExecutor)
    }

    @After
    fun removeExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun noComputeWithoutObservers() {
        val computable = TestComputable()
        verify(taskExecutor, never())?.executeOnDiskIO(computable.refreshRunnable)
        verify(taskExecutor, never())
            ?.executeOnDiskIO(computable.invalidationRunnable)
    }

    @Test
    @Throws(InterruptedException::class)
    fun noConcurrentCompute() {
        val executor = TaskExecutorWithFakeMainThread(2)
        ArchTaskExecutor.getInstance().setDelegate(executor)
        try {
            // # of compute calls
            val computeCounter = Semaphore(0)
            // available permits for computation
            val computeLock = Semaphore(0)
            val computable: TestComputable = object : TestComputable(1, 2) {
                override fun compute(): Int {
                    try {
                        computeCounter.release(1)
                        computeLock.tryAcquire(1, 20, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        throw AssertionError(e)
                    }
                    return super.compute()
                }
            }
            val captor = ArgumentCaptor.forClass(
                Int::class.java
            )
            @Suppress("unchecked_cast")
            val observer: Observer<in Int?> = mock(Observer::class.java) as Observer<in Int?>
            executor.postToMainThread {
                computable.liveData.observeForever(observer)
                verify(observer, never()).onChanged(anyInt())
            }
            // wait for first compute call
            assertThat(
                computeCounter.tryAcquire(1, 2, TimeUnit.SECONDS),
                `is`(true)
            )
            // re-invalidate while in compute
            computable.invalidate()
            computable.invalidate()
            computable.invalidate()
            computable.invalidate()
            // ensure another compute call does not arrive
            assertThat(
                computeCounter.tryAcquire(1, 2, TimeUnit.SECONDS),
                `is`(false)
            )
            // allow computation to finish
            computeLock.release(2)
            // wait for the second result, first will be skipped due to invalidation during compute
            verify(observer, timeout(2000)).onChanged(captor.capture())
            assertThat(captor.allValues, `is`(listOf(2)))
            reset(observer)
            // allow all computations to run, there should not be any.
            computeLock.release(100)
            // unfortunately, Mockito.after is not available in 1.9.5
            executor.drainTasks(2)
            // assert no other results arrive
            verify(observer, never()).onChanged(ArgumentMatchers.anyInt())
        } finally {
            ArchTaskExecutor.getInstance().setDelegate(null)
        }
    }

    @Test
    fun addingObserverShouldTriggerAComputation() {
        val computable = TestComputable(1)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val value = AtomicInteger(-1)
        computable.liveData.observe(lifecycleOwner) { integer -> value.set(integer!!) }
        verify(taskExecutor, never())?.executeOnDiskIO(any(Runnable::class.java))
        assertThat(value.get(), `is`(-1))
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(taskExecutor)?.executeOnDiskIO(computable.refreshRunnable)
        assertThat(value.get(), `is`(1))
    }

    @Test
    fun customExecutor() {
        val customExecutor = mock(
            Executor::class.java
        )
        val computable = TestComputable(customExecutor, 1)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        computable.liveData.observe(lifecycleOwner) {
            // ignored
        }
        verify(taskExecutor, never())?.executeOnDiskIO(
            any(
                Runnable::class.java
            )
        )
        verify(customExecutor, never()).execute(
            any(
                Runnable::class.java
            )
        )
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        verify(taskExecutor, never())?.executeOnDiskIO(computable.refreshRunnable)
        verify(customExecutor).execute(computable.refreshRunnable)
    }

    @Test
    fun invalidationShouldNotReTriggerComputationIfObserverIsInActive() {
        val computable = TestComputable(1, 2)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val value = AtomicInteger(-1)
        computable.liveData.observe(lifecycleOwner) { integer -> value.set(integer!!) }
        assertThat(value.get(), `is`(1))
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        computable.invalidate()
        reset(taskExecutor)
        verify(taskExecutor, never())?.executeOnDiskIO(computable.refreshRunnable)
        assertThat(value.get(), `is`(1))
    }

    @Test
    fun invalidationShouldReTriggerQueryIfObserverIsActive() {
        val computable = TestComputable(1, 2)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        val value = AtomicInteger(-1)
        computable.liveData.observe(lifecycleOwner) { integer -> value.set(integer!!) }
        assertThat(value.get(), `is`(1))
        computable.invalidate()
        assertThat(value.get(), `is`(2))
    }

    internal open class TestComputable : ComputableLiveData<Int?> {
        private val values: IntArray
        private var valueCounter = AtomicInteger()

        constructor(executor: Executor, vararg values: Int) : super(executor) {
            this.values = values
        }

        constructor(vararg values: Int) {
            this.values = values
        }

        override fun compute(): Int {
            return values[valueCounter.getAndIncrement()]
        }
    }
}
