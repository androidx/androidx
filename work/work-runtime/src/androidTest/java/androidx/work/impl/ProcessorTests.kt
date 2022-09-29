/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.impl

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.DatabaseTest
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.generationalId
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.LatchWorker
import androidx.work.worker.StopLatchWorker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class ProcessorTests : DatabaseTest() {
    lateinit var scheduler: Scheduler
    lateinit var factory: WorkerFactory
    val lastCreatedWorker = MutableStateFlow<ListenableWorker?>(null)
    lateinit var processor: Processor
    lateinit var defaultExecutor: ExecutorService
    lateinit var backgroundExecutor: ExecutorService
    lateinit var serialExecutor: SerialExecutorImpl
    val context = ApplicationProvider.getApplicationContext<Context>().applicationContext

    @Before
    fun setUp() {
        // first worker will take over the first thread with its doWork
        // second worker will execute on the second thread
        defaultExecutor = Executors.newFixedThreadPool(2)
        backgroundExecutor = Executors.newSingleThreadExecutor()
        serialExecutor = SerialExecutorImpl(backgroundExecutor)
        val taskExecutor = object : TaskExecutor {
            val mainExecutor = Executor { runnable ->
                runnable.run()
            }

            override fun getMainThreadExecutor(): Executor {
                return mainExecutor
            }

            override fun getSerialTaskExecutor(): SerialExecutorImpl {
                return serialExecutor
            }
        }
        factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                val worker = getDefaultWorkerFactory()
                    .createWorkerWithDefaultFallback(
                        appContext,
                        workerClassName,
                        workerParameters
                    )!!
                lastCreatedWorker.value = worker
                return worker
            }
        }
        val configuration = Configuration.Builder()
            .setWorkerFactory(factory)
            .setExecutor(defaultExecutor)
            .build()
        scheduler = mock(Scheduler::class.java)
        processor = Processor(context, configuration, taskExecutor, mDatabase, listOf(scheduler))
    }

    @Test
    @MediumTest
    fun testInterruptNotInCriticalSection() {
        val request1 = OneTimeWorkRequest.Builder(StopLatchWorker::class.java).build()
        val request2 = OneTimeWorkRequest.Builder(StopLatchWorker::class.java).build()
        insertWork(request1)
        insertWork(request2)
        var listenerCalled = false
        val listener = ExecutionListener { id, _ ->
            if (!listenerCalled) {
                listenerCalled = true
                assertEquals(request1.workSpec.id, id.workSpecId)
            }
        }
        processor.addExecutionListener(listener)
        val startStopToken = StartStopToken(WorkGenerationalId(request1.workSpec.id, 0))
        processor.startWork(startStopToken)

        val firstWorker = runBlocking { lastCreatedWorker.filterNotNull().first() }
        val blockedThread = Executors.newSingleThreadExecutor()
        blockedThread.execute {
            // gonna stall for 10 seconds
            processor.stopWork(startStopToken)
        }
        assertTrue((firstWorker as StopLatchWorker).awaitOnStopCall())
        // onStop call results in onExecuted. It happens on "main thread", which is instant
        // in this case.
        assertTrue(listenerCalled)
        processor.removeExecutionListener(listener)
        listenerCalled = false
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        // This would have previously failed trying to acquire a lock
        processor.startWork(StartStopToken(WorkGenerationalId(request2.workSpec.id, 0)))
        val secondWorker =
            runBlocking { lastCreatedWorker.filterNotNull().filter { it != firstWorker }.first() }
        (secondWorker as StopLatchWorker).countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
        firstWorker.countDown()
        blockedThread.shutdown()
        assertTrue(blockedThread.awaitTermination(3, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun testStartForegroundStopWork() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val startStopToken = StartStopToken(request.workSpec.generationalId())
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        processor.startWork(startStopToken)

        val channel = NotificationChannelCompat
            .Builder("test", NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("hello")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, "test")
            .setOngoing(true)
            .setTicker("ticker")
            .setContentText("content text")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .build()
        val info = ForegroundInfo(1, notification)
        processor.startForeground(startStopToken.id.workSpecId, info)
        // won't actually stopWork, because stopForeground should be used
        processor.stopWork(startStopToken)
        processor.startWork(StartStopToken(request.workSpec.generationalId()))
        assertTrue(processor.isEnqueued(startStopToken.id.workSpecId))
        val firstWorker = runBlocking { lastCreatedWorker.filterNotNull().first() }
        (firstWorker as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun testStartOldGenerationDoesntStopCurrentWorker() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        mDatabase.workSpecDao().incrementGeneration(request.stringId)
        val token = StartStopToken(WorkGenerationalId(request.workSpec.id, 1))
        processor.startWork(token)
        val firstWorker = runBlocking { lastCreatedWorker.filterNotNull().first() }
        var called = false
        val oldGenerationListener = ExecutionListener { id, needsReschedule ->
            called = true
            assertEquals(WorkGenerationalId(request.workSpec.id, 0), id)
            assertFalse(needsReschedule)
        }
        processor.addExecutionListener(oldGenerationListener)
        processor.startWork(StartStopToken(WorkGenerationalId(request.workSpec.id, 0)))
        assertTrue(called)
        processor.removeExecutionListener(oldGenerationListener)
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        (firstWorker as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun testStartNewGenerationDoesntStopCurrentWorker() {
        val request = PeriodicWorkRequest.Builder(
            LatchWorker::class.java, 10, TimeUnit.DAYS
        ).build()
        insertWork(request)
        val token = StartStopToken(WorkGenerationalId(request.workSpec.id, 0))
        processor.startWork(token)
        val firstWorker = runBlocking { lastCreatedWorker.filterNotNull().first() }
        var called = false
        val oldGenerationListener = ExecutionListener { id, needsReschedule ->
            called = true
            assertEquals(WorkGenerationalId(request.workSpec.id, 1), id)
            assertFalse(needsReschedule)
        }
        processor.addExecutionListener(oldGenerationListener)
        mDatabase.workSpecDao().incrementGeneration(request.stringId)
        processor.startWork(StartStopToken(WorkGenerationalId(request.workSpec.id, 1)))
        assertTrue(called)
        processor.removeExecutionListener(oldGenerationListener)
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        (firstWorker as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun testOldGenerationDoesntStart() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        mDatabase.workSpecDao().incrementGeneration(request.stringId)
        val oldToken = StartStopToken(WorkGenerationalId(request.workSpec.id, 0))
        var called = false
        val oldGenerationListener = ExecutionListener { id, needsReschedule ->
            called = true
            // worker shouldn't have been created
            assertEquals(null, lastCreatedWorker.value)
            assertEquals(WorkGenerationalId(request.workSpec.id, 0), id)
            assertFalse(needsReschedule)
        }
        processor.addExecutionListener(oldGenerationListener)
        assertFalse(processor.startWork(oldToken))
        assertTrue(called)
    }

    @After
    fun tearDown() {
        defaultExecutor.shutdownNow()
        backgroundExecutor.shutdownNow()
        assertTrue(defaultExecutor.awaitTermination(3, TimeUnit.SECONDS))
        assertTrue(backgroundExecutor.awaitTermination(3, TimeUnit.SECONDS))
    }
}
