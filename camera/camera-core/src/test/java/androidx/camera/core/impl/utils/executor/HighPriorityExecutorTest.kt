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

package androidx.camera.core.impl.utils.executor

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
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
@Config(sdk = [Config.ALL_SDKS])
class HighPriorityExecutorTest {
    private lateinit var baselineThreads: Set<Thread>
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    @Before
    fun setUp() {
        baselineThreads = liveHighPriorityThreads()
        backgroundThread = HandlerThread("HighPriorityExecutorTest").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    @After
    fun tearDown() {
        backgroundThread.quitSafely()
        backgroundThread.join(TIMEOUT_MILLIS)
        joinAndAssertNoNewHighPriorityThreads()
    }

    @Test
    fun highPriorityExecutor_isSingleton() {
        assertThat(CameraXExecutors.highPriorityExecutor())
            .isSameInstanceAs(CameraXExecutors.highPriorityExecutor())
    }

    @Test
    fun highPriorityExecutor_isPureExecutor() {
        assertThat(CameraXExecutors.highPriorityExecutor())
            .isNotInstanceOf(ExecutorService::class.java)
    }

    @Test
    fun construction_doesNotStartThread() {
        val executor = HighPriorityExecutor()

        assertThat(newHighPriorityThreads()).isEmpty()
        assertThat(executor).isNotSameInstanceAs(HighPriorityExecutor.getInstance())
    }

    @Test
    fun acquireHandlerLease_highPriorityExecutor_startsBackgroundThreadAndQuitsWhenClosed() {
        val executor = CameraXExecutors.highPriorityExecutor()
        assertThat(newHighPriorityThreads()).isEmpty()

        val leasedLooper: Looper
        checkNotNull(CameraXExecutors.acquireHandlerLease(executor)).use { lease ->
            assertBackgroundLooper(lease.handler)
            leasedLooper = lease.handler.looper
            assertThat(newHighPriorityThreads()).containsExactly(leasedLooper.thread)
            assertThat(leasedLooper.thread.isAlive).isTrue()
        }

        leasedLooper.thread.join(TIMEOUT_MILLIS)
        assertThat(leasedLooper.thread.isAlive).isFalse()
        assertThat(newHighPriorityThreads()).isEmpty()
    }

    @Test
    fun acquireHandlerLease_overlappingLeases_shareSameHandlerUntilLastLeaseCloses() {
        val executor = CameraXExecutors.highPriorityExecutor()
        val lease1 = checkNotNull(CameraXExecutors.acquireHandlerLease(executor))
        var lease2: CameraXExecutors.HandlerLease? = null
        try {
            lease2 = checkNotNull(CameraXExecutors.acquireHandlerLease(executor))
            val looper = lease1.handler.looper

            assertThat(lease2.handler).isSameInstanceAs(lease1.handler)
            assertThat(newHighPriorityThreads()).containsExactly(looper.thread)

            // Closing lease1 (even multiple times) must not quit the shared thread while lease2 is
            // still open.
            lease1.close()
            lease1.close()
            assertThat(looper.thread.isAlive).isTrue()

            lease2.close()
            looper.thread.join(TIMEOUT_MILLIS)
            assertThat(looper.thread.isAlive).isFalse()
        } finally {
            lease1.close()
            lease2?.close()
        }
    }

    @Test
    fun acquireHandlerLease_afterThreadQuits_startsNewBackgroundThread() {
        val executor = CameraXExecutors.highPriorityExecutor()

        val firstLooper =
            checkNotNull(CameraXExecutors.acquireHandlerLease(executor)).use { it.handler.looper }
        firstLooper.thread.join(TIMEOUT_MILLIS)
        assertThat(firstLooper.thread.isAlive).isFalse()

        checkNotNull(CameraXExecutors.acquireHandlerLease(executor)).use { secondLease ->
            assertBackgroundLooper(secondLease.handler)
            assertThat(secondLease.handler.looper).isNotSameInstanceAs(firstLooper)
            assertThat(secondLease.handler.looper.thread.isAlive).isTrue()
        }
    }

    @Test
    fun acquireHandlerLease_newHandlerExecutor_returnsHandlerAndCloseDoesNotQuitCallerLooper() {
        val lease =
            checkNotNull(
                CameraXExecutors.acquireHandlerLease(
                    CameraXExecutors.newHandlerExecutor(backgroundHandler)
                )
            )
        assertThat(lease.handler).isSameInstanceAs(backgroundHandler)

        lease.close()

        val latch = CountDownLatch(1)
        assertThat(backgroundHandler.post { latch.countDown() }).isTrue()
        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun acquireHandlerLease_myLooperExecutorOnBackgroundThread_returnsBackgroundLooperHandler() {
        val leasedHandler = AtomicReference<Handler?>()
        val latch = CountDownLatch(1)
        backgroundHandler.post {
            try {
                CameraXExecutors.acquireHandlerLease(CameraXExecutors.myLooperExecutor())?.use {
                    leasedHandler.set(it.handler)
                }
            } finally {
                latch.countDown()
            }
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(requireNotNull(leasedHandler.get()).looper)
            .isSameInstanceAs(backgroundThread.looper)
    }

    @Test
    fun acquireHandlerLease_mainThreadExecutor_returnsMainLooperHandler() {
        checkNotNull(CameraXExecutors.acquireHandlerLease(CameraXExecutors.mainThreadExecutor()))
            .use { lease ->
                assertThat(lease.handler.looper).isSameInstanceAs(Looper.getMainLooper())
            }
    }

    @Test
    fun acquireHandlerLease_nonHandlerExecutors_returnsNull() {
        assertThat(CameraXExecutors.acquireHandlerLease(CameraXExecutors.directExecutor())).isNull()
        assertThat(CameraXExecutors.acquireHandlerLease(CameraXExecutors.ioExecutor())).isNull()
        assertThat(
                CameraXExecutors.acquireHandlerLease(
                    CameraXExecutors.newSequentialExecutor(CameraXExecutors.highPriorityExecutor())
                )
            )
            .isNull()
    }

    // Thread priority is not asserted: Robolectric does not apply HandlerThread priorities, so
    // Process.getThreadPriority() always returns 0 on the host JVM.
    @Test
    fun execute_withOpenLease_runsTasksInOrderOnLeasedHighPriorityThread() {
        val executor = CameraXExecutors.highPriorityExecutor()
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val threadName = AtomicReference<String>()
        val looper = AtomicReference<Looper?>()
        val latch = CountDownLatch(TASK_COUNT)

        checkNotNull(CameraXExecutors.acquireHandlerLease(executor)).use { lease ->
            for (i in 0 until TASK_COUNT) {
                executor.execute {
                    try {
                        order.add(i)
                        threadName.set(Thread.currentThread().name)
                        looper.set(Looper.myLooper())
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
            assertThat(order).containsExactlyElementsIn((0 until TASK_COUNT).toList()).inOrder()
            assertThat(threadName.get()).isEqualTo(THREAD_NAME)
            assertBackgroundLooper(lease.handler)
            assertThat(looper.get()).isSameInstanceAs(lease.handler.looper)
        }
    }

    @Test
    fun execute_withoutOpenLease_runsReentrantTasksOnSameLooperAndQuitsThreadWhenIdle() {
        val executor = CameraXExecutors.highPriorityExecutor()
        val outerLooper = AtomicReference<Looper?>()
        val innerLooper = AtomicReference<Looper?>()
        val latch = CountDownLatch(1)

        executor.execute {
            outerLooper.set(Looper.myLooper())
            executor.execute {
                try {
                    innerLooper.set(Looper.myLooper())
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        val taskLooper = requireNotNull(outerLooper.get())
        assertThat(innerLooper.get()).isSameInstanceAs(taskLooper)
        taskLooper.thread.join(TIMEOUT_MILLIS)
        assertThat(taskLooper.thread.isAlive).isFalse()
    }

    @Test
    fun execute_inFlightTaskKeepsThreadAliveAfterLastLeaseClosesAndQuitsOnCompletion() {
        val executor = CameraXExecutors.highPriorityExecutor()
        val taskStarted = CountDownLatch(1)
        val allowFinish = CountDownLatch(1)
        val taskFinished = CountDownLatch(1)
        val lease = checkNotNull(CameraXExecutors.acquireHandlerLease(executor))
        val thread = lease.handler.looper.thread

        try {
            executor.execute {
                try {
                    taskStarted.countDown()
                    assertThat(allowFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
                } finally {
                    taskFinished.countDown()
                }
            }
            assertThat(taskStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

            // Closing the last open lease while an execute() task is in flight must keep the
            // backing thread alive until that task finishes.
            lease.close()
            assertThat(thread.isAlive).isTrue()
        } finally {
            allowFinish.countDown()
            lease.close()
        }

        assertThat(taskFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        thread.join(TIMEOUT_MILLIS)
        assertThat(thread.isAlive).isFalse()
        assertThat(newHighPriorityThreads()).isEmpty()
    }

    @Test
    fun execute_nullCommand_throwsNullPointerException() {
        val executor: Executor = CameraXExecutors.highPriorityExecutor()

        assertThrows<NullPointerException> { executor.execute(null) }
    }

    private fun assertBackgroundLooper(handler: Handler?) {
        val looper = requireNotNull(handler).looper
        assertThat(looper).isNotNull()
        assertThat(looper).isNotSameInstanceAs(Looper.getMainLooper())
    }

    private fun liveHighPriorityThreads(): Set<Thread> =
        Thread.getAllStackTraces().keys.filterTo(mutableSetOf()) {
            it.name == THREAD_NAME && it.isAlive
        }

    private fun newHighPriorityThreads(): Set<Thread> = liveHighPriorityThreads() - baselineThreads

    private fun joinAndAssertNoNewHighPriorityThreads() {
        for (thread in newHighPriorityThreads()) {
            thread.join(TIMEOUT_MILLIS)
            assertThat(thread.isAlive).isFalse()
        }
    }

    private companion object {
        const val THREAD_NAME = "CameraX-camerax_high_priority"
        const val TASK_COUNT = 10
        const val TIMEOUT_SECONDS = 5L
        const val TIMEOUT_MILLIS = TIMEOUT_SECONDS * 1000L
    }
}
