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
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

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
