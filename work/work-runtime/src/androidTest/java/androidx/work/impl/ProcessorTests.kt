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

import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.DatabaseTest
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.impl.foreground.SystemForegroundDispatcher.createStartForegroundIntent
import androidx.work.impl.foreground.SystemForegroundDispatcher.createStopForegroundIntent
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.model.generationalId
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.LatchWorker
import androidx.work.worker.StopAwareWorker
import androidx.work.worker.StopLatchWorker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessorTests : DatabaseTest() {
    val factory = TrackingWorkerFactory()
    lateinit var processor: Processor
    lateinit var defaultExecutor: ExecutorService
    lateinit var backgroundExecutor: ExecutorService
    lateinit var serialExecutor: SerialExecutorImpl
    private val context = TrackingContext(
        ApplicationProvider.getApplicationContext<Context>().applicationContext
    )

    private val foregroundInfo: ForegroundInfo
        get() {
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
            return ForegroundInfo(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }

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
        val configuration = Configuration.Builder()
            .setWorkerFactory(factory)
            .setExecutor(defaultExecutor)
            .build()
        processor = Processor(context, configuration, taskExecutor, mDatabase)
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

        val firstWorker = factory.awaitWorker(request1.id)
        val blockedThread = Executors.newSingleThreadExecutor()
        blockedThread.execute {
            // gonna stall for 10 seconds
            processor.stopWork(startStopToken, 0)
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
        val secondWorker = factory.awaitWorker(request2.id)
        (secondWorker as StopLatchWorker).countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
        firstWorker.countDown()
        blockedThread.shutdown()
        assertTrue(blockedThread.awaitTermination(3, TimeUnit.SECONDS))
        assertTrue(context.intents.isEmpty())
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

        processor.startForeground(startStopToken.id.workSpecId, foregroundInfo)
        // won't actually stopWork, because stopForeground should be used
        processor.stopWork(startStopToken, 0)
        // follow-up startWork shouldn't fail
        processor.startWork(StartStopToken(request.workSpec.generationalId()))
        assertTrue(processor.isEnqueued(startStopToken.id.workSpecId))
        val firstWorker = factory.awaitWorker(request.id)
        (firstWorker as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun testInterruptStopsService() {
        val request = OneTimeWorkRequest.Builder(StopAwareWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        val startStopToken = StartStopToken(id)
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        processor.startWork(startStopToken)
        processor.startForeground(startStopToken.id.workSpecId, foregroundInfo)
        val expected = createStartForegroundIntent(context, id, foregroundInfo)
        assertTrue(context.intents[0].filterEquals(expected))
        // won't actually stopWork, because stopForeground should be used
        processor.stopForegroundWork(startStopToken, STOP_REASON_CONSTRAINT_CONNECTIVITY)
        assertFalse(processor.isEnqueued(startStopToken.id.workSpecId))
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
        val stopIntentExpected = createStopForegroundIntent(context)

        val intent = context.intents.getOrNull(1)
            ?: throw AssertionError("Stop Intent wasn't sent")
        assertTrue(intent.filterEquals(stopIntentExpected))
    }

    @Test
    @MediumTest
    fun testStartOldGenerationDoesntStopCurrentWorker() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        mDatabase.workSpecDao().incrementGeneration(request.stringId)
        val token = StartStopToken(WorkGenerationalId(request.workSpec.id, 1))
        processor.startWork(token)
        val firstWorker = factory.awaitWorker(request.id)
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
        val firstWorker = factory.awaitWorker(request.id)
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
            assertEquals(0, factory.createdWorkers.value.size)
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

    private class TrackingContext(base: Context) : ContextWrapper(base) {
        val intents = mutableListOf<Intent>()
        override fun startService(service: Intent): ComponentName? {
            // don't start anything, simply track requests
            intents.add(service)
            // result isn't used so simply return null
            return null
        }

        override fun startForegroundService(service: Intent): ComponentName? {
            // simply track it
            return startService(service)
        }
    }
}
