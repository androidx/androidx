/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work.testing

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Clock
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.RunnableScheduler
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.SUCCEEDED
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.workers.TestWorker
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import java.util.Collections
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * Verifies ability to use [kotlinx.coroutines.test.TestCoroutineScheduler] standard Coroutines test
 * dispatchers to test WorkManager over the progress of time.
 */
@SQLiteMode(SQLiteMode.Mode.LEGACY) // b/285714232
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, maxSdk = 33)
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var testDriver: TestDriver
    private lateinit var workManager: WorkManager

    private val shadowLooper = shadowOf(Looper.getMainLooper())
    private val testCoroutineScheduler = TestCoroutineScheduler()
    private val testCoroutineDispatcher = StandardTestDispatcher(testCoroutineScheduler)
    private val testRunnableScheduler =
        CoroutineDispatcherRunnableScheduler(testCoroutineDispatcher)

    @Before
    fun setUp() {
        val schedulerClock = Clock { testCoroutineScheduler.currentTime }
        val configuration =
            Configuration.Builder()
                .setClock(schedulerClock)
                .setExecutor(testCoroutineDispatcher.asExecutor())
                .setTaskExecutor(testCoroutineDispatcher.asExecutor())
                .setRunnableScheduler(testRunnableScheduler)
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            configuration,
            WorkManagerTestInitHelper.ExecutorsMode.USE_TIME_BASED_SCHEDULING
        )

        testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        workManager = WorkManagerImpl.getInstance(context)

        synchronizeThreads()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testDelayedWork_runsOnceTimeHasPassed() {
        val initialDelayMillis = HOURS.toMillis(2)
        val request: WorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(initialDelayMillis, MILLISECONDS)
                .build()

        workManager.enqueue(listOf(request))
        synchronizeThreads()

        val workInfoEarly = workManager.getWorkInfoById(request.id)
        synchronizeThreads()

        assertThat(Futures.getDone(workInfoEarly)!!.state).isEqualTo(ENQUEUED)

        // Work should run and finish now.
        testCoroutineScheduler.advanceTimeBy(initialDelayMillis)
        synchronizeThreads()

        // Verify result
        val workInfoOnTime = workManager.getWorkInfoById(request.id)
        synchronizeThreads()
        assertThat(Futures.getDone(workInfoOnTime)!!.state).isEqualTo(SUCCEEDED)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testClockDelay_testDriverDelayMet_throws() {
        val initialDelayMillis = HOURS.toMillis(2)
        val request: WorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(initialDelayMillis, MILLISECONDS)
                .build()

        workManager.enqueue(listOf(request))
        synchronizeThreads()

        val workInfoEarly = workManager.getWorkInfoById(request.id)
        synchronizeThreads()

        assertThat(Futures.getDone(workInfoEarly)!!.state).isEqualTo(ENQUEUED)

        // Can't use setXDelayMet with clock-based scheduling
        assertThrows(IllegalStateException::class.java) {
            testDriver.setInitialDelayMet(request.id)
        }

        assertThrows(IllegalStateException::class.java) { testDriver.setPeriodDelayMet(request.id) }
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testDispatcherSchedule() {
        val runnable1 = MarkedRunnable()
        val runnable2 = MarkedRunnable()

        testRunnableScheduler.scheduleWithDelay(1000, runnable1)
        testRunnableScheduler.scheduleWithDelay(2000, runnable2)
        assertThat(testRunnableScheduler.runnables).hasSize(2)

        testCoroutineScheduler.advanceTimeBy(1000)
        testCoroutineScheduler.runCurrent()
        assertThat(runnable1.ran).isTrue()
        assertThat(runnable2.ran).isFalse()
        assertThat(testRunnableScheduler.runnables).hasSize(1)

        testCoroutineScheduler.advanceTimeBy(1000)
        testCoroutineScheduler.runCurrent()
        assertThat(runnable1.ran).isTrue()
        assertThat(runnable2.ran).isTrue()
        assertThat(testRunnableScheduler.runnables).isEmpty()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun testDispatcherCancel() {
        val runnable1 = MarkedRunnable()

        testRunnableScheduler.scheduleWithDelay(1000, runnable1)
        testCoroutineScheduler.runCurrent()
        assertThat(testRunnableScheduler.runnables).hasSize(1)

        testRunnableScheduler.cancel(runnable1)
        assertThat(testRunnableScheduler.runnables).isEmpty()

        testCoroutineScheduler.advanceTimeBy(1000)
        testCoroutineScheduler.runCurrent()
        assertThat(runnable1.ran).isFalse()
    }

    /**
     * TestCoroutineDispatcher works by forcing all dispatched coroutines to wait on the scheduler
     * before they are allowed to execute, similar to Robolectric's
     * [org.robolectric.shadows.ShadowLooper.pause] mode.
     *
     * Unfortunately, TestCoroutineScheduler does not wait for the main thread to be idle before
     * continuing. WorkManager posts certain internal operations to the main thread, and then the
     * result of those posts may launch a coroutine, etc.
     *
     * This is a limitation of the Kotlin testing framework. Here we alternate synchronizing the
     * Robolectric main thread and Coroutines until both queues are empty at the current timepoint.
     */
    private fun synchronizeThreads() {
        testCoroutineScheduler.runCurrent()
        while (!shadowLooper.isIdle) {
            shadowLooper.idle()
            testCoroutineScheduler.runCurrent()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            val job = launch { WorkManagerTestInitHelper.closeWorkDatabase() }
            while (job.isActive) {
                synchronizeThreads()
                yield()
            }
        }
    }
}

private class MarkedRunnable : Runnable {
    var ran = false

    override fun run() {
        ran = true
    }
}

/**
 * A [RunnableScheduler] that delegates to a Coroutines [kotlinx.coroutines.CoroutineDispatcher] for
 * scheduling.
 */
private class CoroutineDispatcherRunnableScheduler(private val testDispatcher: TestDispatcher) :
    RunnableScheduler {
    val runnables: MutableMap<Runnable, DisposableHandle> = Collections.synchronizedMap(HashMap())

    override fun scheduleWithDelay(delayInMillis: Long, runnable: Runnable) {
        runnables[runnable] =
            testDispatcher.invokeOnTimeout(
                delayInMillis,
                {
                    runnable.run()
                    runnables.remove(runnable)
                },
                EmptyCoroutineContext
            )
    }

    override fun cancel(runnable: Runnable) {
        runnables[runnable]?.dispose() // unregisters from schedule callback
        runnables.remove(runnable)
    }
}
