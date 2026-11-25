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

package androidx.camera.camera2.impl

import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class UseCaseThreadsTest {

    /** A plain (non-scheduled) [Executor] whose queued tasks are run by the test thread. */
    private val backgroundExecutor = QueueingExecutor()
    private val backgroundDispatcher = backgroundExecutor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + backgroundDispatcher)
    private lateinit var schedulerThread: HandlerThread
    private lateinit var schedulerHandler: Handler

    @Before
    fun setUp() {
        schedulerThread = HandlerThread("UseCaseThreadsTest-scheduler").apply { start() }
        schedulerHandler = Handler(schedulerThread.looper)
    }

    @After
    fun tearDown() {
        scope.cancel()
        schedulerThread.quitSafely()
        schedulerThread.join(1000)
    }

    @Test
    fun withoutSchedulerHandler_sequentialExecutorIsNotScheduled() {
        val threads = UseCaseThreads(scope, backgroundExecutor, backgroundDispatcher)

        assertThat(threads.sequentialExecutor).isNotInstanceOf(ScheduledExecutorService::class.java)
    }

    @Test
    fun delay_isDrivenBySchedulerHandlerAndResumesOnSequentialThread() {
        val threads =
            UseCaseThreads(scope, backgroundExecutor, backgroundDispatcher, schedulerHandler)
        val threadsBefore = Thread.getAllStackTraces().keys
        var onSequentialThreadBeforeDelay = false
        var onSequentialThreadAfterDelay = false
        var resumedThread: Thread? = null

        val job =
            threads.sequentialScope.launch {
                onSequentialThreadBeforeDelay = threads.isOnSequentialThread()
                delay(DELAY_MS)
                onSequentialThreadAfterDelay = threads.isOnSequentialThread()
                resumedThread = Thread.currentThread()
                threads.checkOnSequentialThread()
            }
        // Starts the coroutine, which registers its timer on the scheduler handler and suspends.
        backgroundExecutor.runAll()
        assertThat(schedulerHandler.hasMessages(0)).isTrue()
        assertThat(onSequentialThreadBeforeDelay).isTrue()
        assertThat(job.isActive).isTrue()

        shadowOf(schedulerThread.looper).idleFor(DELAY_MS - 1, TimeUnit.MILLISECONDS)
        assertThat(backgroundExecutor.runAll()).isEqualTo(0)
        assertThat(job.isActive).isTrue()

        // The expired timer only hands the continuation back to the background executor.
        shadowOf(schedulerThread.looper).idleFor(1, TimeUnit.MILLISECONDS)
        assertThat(backgroundExecutor.runAll()).isGreaterThan(0)

        assertThat(job.isCompleted).isTrue()
        assertThat(onSequentialThreadAfterDelay).isTrue()
        assertThat(resumedThread).isNotEqualTo(schedulerThread)
        val newThreads = Thread.getAllStackTraces().keys - threadsBefore
        assertThat(newThreads.map { it.name }.filter { it.contains("DefaultExecutor") }).isEmpty()
    }

    @Test
    fun delay_onQuitSchedulerHandler_cancelsJobInsteadOfHanging() {
        val quitThread = HandlerThread("UseCaseThreadsTest-quitScheduler").apply { start() }
        val quitHandler = Handler(quitThread.looper)
        quitThread.quit()
        quitThread.join(1000)
        val threads = UseCaseThreads(scope, backgroundExecutor, backgroundDispatcher, quitHandler)

        val job = threads.sequentialScope.launch { delay(DELAY_MS) }
        backgroundExecutor.runAll()

        assertThat(job.isCancelled).isTrue()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun executorServiceDelegate_forwardsActiveExecutionAndShutdownRejection() {
        val executorService = Executors.newSingleThreadScheduledExecutor()
        val dispatcher = executorService.asCoroutineDispatcher()
        val parentScope = CoroutineScope(SupervisorJob() + dispatcher)
        val releaseGate = CountDownLatch(1)
        try {
            val threads = UseCaseThreads(parentScope, executorService, dispatcher, schedulerHandler)
            val scheduledSequential = threads.sequentialExecutor as ScheduledExecutorService
            assertThat(scheduledSequential).isNotSameInstanceAs(executorService)
            assertThat(scheduledSequential.isShutdown).isFalse()
            assertThat(scheduledSequential.isTerminated).isFalse()

            // Verify active execution through confinedExecutorService sets isOnSequentialThread
            // and cleans up pending withTimeoutOrNull timers on early completion.
            val enteredTimeout = CountDownLatch(1)
            val completedLatch = CountDownLatch(1)
            var ranOnSequentialThread = false
            var result: String? = null
            var workerFailure: Throwable? = null
            threads.sequentialScope.launch {
                try {
                    result =
                        withTimeoutOrNull(DELAY_MS) {
                            enteredTimeout.countDown()
                            assertThat(releaseGate.await(5, TimeUnit.SECONDS)).isTrue()
                            threads.checkOnSequentialThread()
                            ranOnSequentialThread = threads.isOnSequentialThread()
                            "ok"
                        }
                } catch (t: Throwable) {
                    workerFailure = t
                } finally {
                    completedLatch.countDown()
                }
            }
            assertThat(enteredTimeout.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(schedulerHandler.hasMessages(0)).isTrue()

            releaseGate.countDown()
            assertThat(completedLatch.await(5, TimeUnit.SECONDS)).isTrue()
            workerFailure?.let { throw it }
            assertThat(ranOnSequentialThread).isTrue()
            assertThat(result).isEqualTo("ok")
            assertThat(schedulerHandler.hasMessages(0)).isFalse()

            // Shut down the backing ExecutorService while schedulerHandler stays alive and verify
            // post-shutdown timers reject up front instead of posting never-resuming callbacks.
            executorService.shutdown()
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
            assertThat(scheduledSequential.isShutdown).isTrue()
            assertThat(scheduledSequential.isTerminated).isTrue()

            val delayJob =
                threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    delay(DELAY_MS)
                }
            val timeoutJob =
                threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(DELAY_MS) { awaitCancellation() }
                }

            assertThat(delayJob.isCancelled).isTrue()
            assertThat(delayJob.isCompleted).isTrue()
            assertThat(timeoutJob.isCancelled).isTrue()
            assertThat(timeoutJob.isCompleted).isTrue()
            assertThat(schedulerHandler.hasMessages(0)).isFalse()
        } finally {
            releaseGate.countDown()
            parentScope.cancel()
            executorService.shutdownNow()
        }
    }

    @Test
    fun confineDeferredList_propagatesResults() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val threads =
            UseCaseThreads(
                CoroutineScope(SupervisorJob() + dispatcher),
                dispatcher.asExecutor(),
                dispatcher,
            )
        val source1 = CompletableDeferred<Int>()
        val source2 = CompletableDeferred<Int>()

        val resultList = threads.confineDeferredList(2) { listOf(source1, source2) }
        advanceUntilIdle()
        source1.complete(10)
        source2.complete(20)

        assertThat(resultList[0].await()).isEqualTo(10)
        assertThat(resultList[1].await()).isEqualTo(20)
        threads.scope.cancel()
        threads.sequentialScope.cancel()
    }

    @Test
    fun confineDeferredList_cancelsAllSignalsWhenSequentialScopeAlreadyCancelled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val threads =
            UseCaseThreads(
                CoroutineScope(SupervisorJob() + dispatcher),
                dispatcher.asExecutor(),
                dispatcher,
            )
        threads.sequentialScope.cancel()

        val resultList =
            threads.confineDeferredList(2) {
                listOf(CompletableDeferred(1), CompletableDeferred(2))
            }
        advanceUntilIdle()

        assertThat(resultList).hasSize(2)
        assertThat(resultList.all { it.isCancelled }).isTrue()
        threads.scope.cancel()
    }

    @Test
    fun confineDeferredListSuspend_propagatesResults() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val threads =
            UseCaseThreads(
                CoroutineScope(SupervisorJob() + dispatcher),
                dispatcher.asExecutor(),
                dispatcher,
            )
        val gate = CompletableDeferred<Unit>()
        val source1 = CompletableDeferred<String>()
        val source2 = CompletableDeferred<String>()

        val resultList =
            threads.confineDeferredListSuspend(2) {
                gate.await()
                listOf(source1, source2)
            }
        advanceUntilIdle()
        assertThat(resultList.none { it.isCompleted }).isTrue()

        gate.complete(Unit)
        advanceUntilIdle()
        source1.complete("a")
        source2.complete("b")

        assertThat(resultList[0].await()).isEqualTo("a")
        assertThat(resultList[1].await()).isEqualTo("b")
        threads.scope.cancel()
        threads.sequentialScope.cancel()
    }

    @Test
    fun confineDeferredListSuspend_cancelsAllSignalsWhenCancelledDuringSuspension() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val threads =
            UseCaseThreads(
                CoroutineScope(SupervisorJob() + dispatcher),
                dispatcher.asExecutor(),
                dispatcher,
            )
        val started = CompletableDeferred<Unit>()

        val resultList =
            threads.confineDeferredListSuspend<Int>(2) {
                started.complete(Unit)
                awaitCancellation()
            }
        advanceUntilIdle()
        assertThat(started.isCompleted).isTrue()

        threads.sequentialScope.cancel()
        advanceUntilIdle()

        assertThat(resultList).hasSize(2)
        assertThat(resultList.all { it.isCancelled }).isTrue()
        threads.scope.cancel()
    }

    @Test
    fun confineDeferredList_propagatesExceptionWhenBlockThrows() = runTest {
        val uncaughtExceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            uncaughtExceptions.add(throwable)
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val threads =
            UseCaseThreads(
                CoroutineScope(SupervisorJob() + dispatcher + handler),
                dispatcher.asExecutor(),
                dispatcher,
            )
        val expected = IllegalStateException("capture sequence failure")

        val resultList = threads.confineDeferredList<Int>(2) { throw expected }
        advanceUntilIdle()

        assertThat(resultList).hasSize(2)
        assertThat(resultList[0].getCompletionExceptionOrNull()).isSameInstanceAs(expected)
        assertThat(resultList[1].getCompletionExceptionOrNull()).isSameInstanceAs(expected)
        assertThat(uncaughtExceptions).containsExactly(expected)
        threads.scope.cancel()
        threads.sequentialScope.cancel()
    }

    /** An [Executor] that queues tasks until explicitly run by the test thread. */
    private class QueueingExecutor : Executor {
        private val tasks = ConcurrentLinkedQueue<Runnable>()

        override fun execute(command: Runnable) {
            tasks.add(command)
        }

        /** Runs all queued tasks, including newly queued ones, and returns how many were run. */
        fun runAll(): Int {
            var count = 0
            while (true) {
                val task = tasks.poll() ?: return count
                task.run()
                count++
            }
        }
    }

    private companion object {
        const val DELAY_MS = 5_000L
    }
}
