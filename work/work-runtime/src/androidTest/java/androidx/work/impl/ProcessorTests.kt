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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    private val context =
        TrackingContext(ApplicationProvider.getApplicationContext<Context>().applicationContext)

    private val foregroundInfo: ForegroundInfo
        get() {
            val channel =
                NotificationChannelCompat.Builder(
                        "test",
                        NotificationManagerCompat.IMPORTANCE_DEFAULT,
                    )
                    .setName("hello")
                    .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
            val notification =
                NotificationCompat.Builder(context, "test")
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
        val taskExecutor =
            object : TaskExecutor {
                val mainExecutor = Executor { runnable -> runnable.run() }
                val workCoroutineScope = CoroutineScope(taskCoroutineDispatcher)

                override fun getMainThreadExecutor(): Executor {
                    return mainExecutor
                }

                override fun getSerialTaskExecutor(): SerialExecutorImpl {
                    return serialExecutor
                }

                override fun getCoroutineScope(): CoroutineScope = workCoroutineScope
            }
        val configuration =
            Configuration.Builder().setWorkerFactory(factory).setExecutor(defaultExecutor).build()
        processor = Processor(context, configuration, taskExecutor, mDatabase)
    }

    @Test
    @MediumTest
    fun testInterruptNotInCriticalSection() {
        val request1 = OneTimeWorkRequest.Builder(StopLatchWorker::class.java).build()
        val request2 = OneTimeWorkRequest.Builder(StopLatchWorker::class.java).build()
        insertWork(request1)
        insertWork(request2)
        class CountDownListener(val expectedId: String) : ExecutionListener {
            val latch = CountDownLatch(1)

            override fun onExecuted(id: WorkGenerationalId, needsReschedule: Boolean) {
                if (id.workSpecId == expectedId) {
                    latch.countDown()
                }
            }
        }
        val firstListener = CountDownListener(request1.workSpec.id)
        processor.addExecutionListener(firstListener)
        val startStopToken = StartStopToken(WorkGenerationalId(request1.workSpec.id, 0))
        processor.startWork(startStopToken)

        val firstWorker = factory.awaitWorker(request1.id)
        Executors.newSingleThreadExecutor().execute {
            // wil result in long running onStop call, but it will block task thread
            processor.stopWork(startStopToken, 0)
        }
        assertTrue((firstWorker as StopLatchWorker).awaitOnStopCall())

        val secondListener = CountDownListener(request2.workSpec.id)
        processor.addExecutionListener(secondListener)
        // This would have previously failed trying to acquire a lock
        processor.startWork(StartStopToken(WorkGenerationalId(request2.workSpec.id, 0)))
        firstWorker.countDown()
        val secondWorker = factory.awaitWorker(request2.id)
        assertTrue(firstListener.latch.await(3, TimeUnit.SECONDS))
        (secondWorker as StopLatchWorker).countDown()
        assertTrue(secondListener.latch.await(3, TimeUnit.SECONDS))
        firstWorker.countDown()
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
        // The stop intent is dispatched from the task executor, not inline.
        awaitSerialExecutorIdle()
        val stopIntentExpected = createStopForegroundIntent(context)

        val intent = context.intents.getOrNull(1) ?: throw AssertionError("Stop Intent wasn't sent")
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
        val request =
            PeriodicWorkRequest.Builder(LatchWorker::class.java, 10, TimeUnit.DAYS).build()
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

    @Test
    @MediumTest
    fun testListenerCanModifyListDuringRunOnExecuted() {
        var firstListenerCalled = false
        var secondListenerCalled = false

        val secondListener = ExecutionListener { _, _ -> secondListenerCalled = true }

        val firstListener =
            object : ExecutionListener {
                override fun onExecuted(id: WorkGenerationalId, needsReschedule: Boolean) {
                    firstListenerCalled = true
                    processor.removeExecutionListener(this)
                    processor.addExecutionListener(secondListener)
                }
            }

        processor.addExecutionListener(firstListener)

        // 1. Test runOnExecuted pathway (startWork on non-existent work spec)
        val nonExistentToken = StartStopToken(WorkGenerationalId("non-existent-id", 0))
        assertFalse(processor.startWork(nonExistentToken))
        assertTrue(firstListenerCalled)
        // secondListener was added during the dispatch, so it shouldn't be called in the current
        // dispatch pass
        assertFalse(secondListenerCalled)

        // Reset state to verify secondListener is now active and firstListener is removed
        firstListenerCalled = false
        secondListenerCalled = false

        // 2. Test runOnExecuted pathway subsequent dispatch (to verify secondListener is now active
        // and firstListener is removed)
        val anotherNonExistentToken =
            StartStopToken(WorkGenerationalId("another-non-existent-id", 0))
        assertFalse(processor.startWork(anotherNonExistentToken))
        assertTrue(secondListenerCalled)
        assertFalse(firstListenerCalled)
    }

    @Test
    @MediumTest
    fun testListenerCanModifyListDuringOnExecuted() {
        var firstListenerCalled = false
        var secondListenerCalled = false

        val secondListener = ExecutionListener { _, _ -> secondListenerCalled = true }

        val firstListener =
            object : ExecutionListener {
                override fun onExecuted(id: WorkGenerationalId, needsReschedule: Boolean) {
                    firstListenerCalled = true
                    processor.removeExecutionListener(this)
                    processor.addExecutionListener(secondListener)
                }
            }

        processor.addExecutionListener(firstListener)

        // 1. Test onExecuted pathway
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val token = StartStopToken(WorkGenerationalId(request.workSpec.id, 0))
        assertTrue(processor.startWork(token))
        val worker = factory.awaitWorker(request.id)

        // Use a CountDownLatch to block the test thread until the asynchronous worker completes
        // and all preceding execution listeners (including the ones under test) have finished
        // running.
        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }

        (worker as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))

        assertTrue(firstListenerCalled)
        // secondListener was added during the dispatch, so it shouldn't be called in the current
        // dispatch pass
        assertFalse(secondListenerCalled)

        // 2. Test onExecuted pathway subsequent dispatch (to verify secondListener is now active
        // and firstListener is removed)
        firstListenerCalled = false
        secondListenerCalled = false

        val request2 = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request2)
        val token2 = StartStopToken(WorkGenerationalId(request2.workSpec.id, 0))

        assertTrue(processor.startWork(token2))
        val worker2 = factory.awaitWorker(request2.id)

        // Block until the second worker completes and all listeners have finished running
        val executionFinished2 = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished2.countDown() }

        (worker2 as LatchWorker).mLatch.countDown()
        assertTrue(executionFinished2.await(3, TimeUnit.SECONDS))

        assertTrue(secondListenerCalled)
        assertFalse(firstListenerCalled)
    }

    @Test
    @MediumTest
    fun testForegroundStart_notifiesListener() = runBlocking {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        val startStopToken = StartStopToken(id)

        val deferred = CompletableDeferred<Pair<WorkGenerationalId, Boolean>>()

        val listener = ForegroundListener { generationalId, isForeground ->
            deferred.complete(generationalId to isForeground)
        }

        processor.addForegroundListener(listener)

        processor.startWork(startStopToken)

        // Promote to foreground
        processor.startForeground(startStopToken.id.workSpecId, foregroundInfo)

        val (notifiedId, foregroundState) = withTimeout(3000) { deferred.await() }
        assertEquals(id, notifiedId)
        assertTrue(foregroundState)

        // Clean up / stop work so it doesn't leak into teardown
        val executionFinished = CompletableDeferred<Unit>()
        processor.addExecutionListener { _, _ -> executionFinished.complete(Unit) }
        val firstWorker = factory.awaitWorker(request.id)
        (firstWorker as LatchWorker).mLatch.countDown()
        withTimeout(3000) { executionFinished.await() }
    }

    @Test
    @MediumTest
    fun testExecutionFinishes_notifiesForegroundListener() = runBlocking {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        val startStopToken = StartStopToken(id)

        val deferred = CompletableDeferred<Pair<WorkGenerationalId, Boolean>>()

        val listener = ForegroundListener { generationalId, isForeground ->
            if (!isForeground) {
                deferred.complete(generationalId to isForeground)
            }
        }

        processor.addForegroundListener(listener)

        processor.startWork(startStopToken)

        // Promote to foreground
        processor.startForeground(startStopToken.id.workSpecId, foregroundInfo)

        // Clean up / stop foreground work
        val executionFinished = CompletableDeferred<Unit>()
        processor.addExecutionListener { _, _ -> executionFinished.complete(Unit) }
        val firstWorker = factory.awaitWorker(request.id)
        (firstWorker as LatchWorker).mLatch.countDown()
        withTimeout(3000) { executionFinished.await() }

        val (notifiedId, foregroundState) = withTimeout(3000) { deferred.await() }
        assertEquals(id, notifiedId)
        assertFalse(foregroundState)
    }

    @Test
    @MediumTest
    fun testStopForegroundWork_notifiesListener() = runBlocking {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        val startStopToken = StartStopToken(id)

        val deferred = CompletableDeferred<Pair<WorkGenerationalId, Boolean>>()

        val listener = ForegroundListener { generationalId, isForeground ->
            if (!isForeground) {
                deferred.complete(generationalId to isForeground)
            }
        }

        processor.addForegroundListener(listener)

        processor.startWork(startStopToken)

        // Promote to foreground
        processor.startForeground(startStopToken.id.workSpecId, foregroundInfo)

        val executionFinished = CompletableDeferred<Unit>()
        processor.addExecutionListener { _, _ -> executionFinished.complete(Unit) }

        // Stop foreground work explicitly
        processor.stopForegroundWork(startStopToken, 0)

        val (notifiedId, foregroundState) = withTimeout(3000) { deferred.await() }
        assertEquals(id, notifiedId)
        assertFalse(foregroundState)

        // Wait for worker execution finish to clean up (stopForegroundWork interrupts LatchWorker)
        withTimeout(3000) { executionFinished.await() }
    }

    /**
     * b/571217003: Processor must not hold mLock while inside a binder transaction
     * (startForegroundService). Otherwise every other mLock user — including the main-thread
     * callers SystemJobService.onStopJob()/onStartJob() and Processor.onExecuted() — stalls for as
     * long as ActivityManager takes, which on a loaded device is enough to miss JobScheduler's
     * onStopJob deadline (JOB_SERVICE_STOP ANR).
     */
    @Test
    @MediumTest
    fun testStartForeground_doesNotBlockLockUsersWhileBinderIsSlow() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        processor.startWork(StartStopToken(id))
        val worker = factory.awaitWorker(request.id) as LatchWorker

        // Emulate a slow ActivityManager: the next service transaction blocks until released.
        // Dispatch startForeground on the serial task executor, matching WorkForegroundUpdater.
        val slowBinder = context.gateNextTransaction()
        serialExecutor.execute { processor.startForeground(id.workSpecId, foregroundInfo) }
        try {
            assertTrue(context.awaitTransactionStarted(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            // Exactly what SystemJobService does on the main thread while that binder is in
            // flight.
            assertFalse(callWithTimeout { processor.isCancelled(id.workSpecId) }) // onStopJob
            assertTrue(callWithTimeout { processor.isForeground(id) }) // onStartJob
            assertTrue(callWithTimeout { processor.hasWork() })
            // And what any other worker's stop / onExecuted needs: the lock itself.
            val other = StartStopToken(WorkGenerationalId("some-other-id", 0))
            assertFalse(callWithTimeout { processor.stopWork(other, 0) })
        } finally {
            slowBinder.countDown()
        }
        awaitSerialExecutorIdle()

        val executionFinished = CountDownLatch(1)
        processor.addExecutionListener { _, _ -> executionFinished.countDown() }
        worker.mLatch.countDown()
        assertTrue(executionFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        awaitSerialExecutorIdle()
    }

    /**
     * b/571217003: when a foreground worker finishes, Processor.onExecuted() runs on the main
     * thread holding mLock. The stop-foreground-service transaction must not be issued from that
     * path; it is deferred to the task executor and the lock is free while it is in flight.
     */
    @Test
    @MediumTest
    fun testForegroundWorkerFinished_stopServiceTransactionIsOffTheExecutedPath() {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(request)
        val id = request.workSpec.generationalId()
        processor.startWork(StartStopToken(id))
        val worker = factory.awaitWorker(request.id) as LatchWorker
        // Called directly on the test thread for setup while serialExecutor is idle.
        processor.startForeground(id.workSpecId, foregroundInfo)
        assertEquals(1, context.intents.size)

        val slowBinder = context.gateNextTransaction()
        val executionFinished = CountDownLatch(1)
        var intentsDuringOnExecuted = -1
        processor.addExecutionListener { _, _ ->
            intentsDuringOnExecuted = context.intents.size
            executionFinished.countDown()
        }
        worker.mLatch.countDown()
        try {
            // Listeners run at the end of onExecuted() after leaving its critical section. The
            // stop-service transaction must not have been issued inline during onExecuted().
            assertTrue(
                "onExecuted() is blocked behind the stop-service transaction",
                executionFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
            )
            assertEquals(1, intentsDuringOnExecuted)
            assertTrue(context.awaitTransactionStarted(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            // Transaction in flight on the task executor; mLock must be free.
            assertFalse(callWithTimeout { processor.hasWork() })
        } finally {
            slowBinder.countDown()
        }
        awaitSerialExecutorIdle()
        assertEquals(2, context.intents.size)
        assertTrue(context.intents[1].filterEquals(createStopForegroundIntent(context)))
    }

    /**
     * A deferred stop must be dropped if new foreground work started before it ran. Otherwise
     * ACTION_STOP_FOREGROUND reaches SystemForegroundService after the newer start, and its stop()
     * calls stopForeground(true) on a service that still has live foreground work.
     */
    @Test
    @MediumTest
    fun testDeferredStopForeground_isSkippedWhenNewForegroundWorkStarted() {
        val first = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(first)
        insertWork(second)
        val firstId = first.workSpec.generationalId()
        val secondId = second.workSpec.generationalId()
        processor.startWork(StartStopToken(firstId))
        processor.startWork(StartStopToken(secondId))
        val firstWorker = factory.awaitWorker(first.id) as LatchWorker
        val secondWorker = factory.awaitWorker(second.id) as LatchWorker
        // Called directly on the test thread for setup while serialExecutor is idle.
        processor.startForeground(firstId.workSpecId, foregroundInfo)

        // Park the task executor so a queued startForeground(secondId) and a subsequent stop of
        // firstId accumulate on the serial queue in production FIFO order.
        val parked = CountDownLatch(1)
        serialExecutor.execute { parked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        try {
            serialExecutor.execute {
                processor.startForeground(secondId.workSpecId, foregroundInfo)
            }
            processor.stopForegroundWork(StartStopToken(firstId), 0) // queues the deferred stop
        } finally {
            parked.countDown()
        }
        awaitSerialExecutorIdle()

        val stopIntent = createStopForegroundIntent(context)
        assertEquals(2, context.intents.size) // START first, START second — no STOP
        assertTrue(context.intents.none { it.filterEquals(stopIntent) })
        assertTrue(processor.isForeground(secondId))

        processor.stopForegroundWork(StartStopToken(secondId), 0)
        awaitSerialExecutorIdle()
        assertEquals(3, context.intents.size)
        assertTrue(context.intents.last().filterEquals(stopIntent))
        firstWorker.mLatch.countDown()
        secondWorker.mLatch.countDown()
    }

    /**
     * When two foreground sessions drain before the first deferred stop runnable executes, the
     * first runnable stops the service and nulls `mForegroundLock`, and the second runnable sees
     * `mForegroundLock == null` and drops the redundant `ACTION_STOP_FOREGROUND`.
     */
    @Test
    @MediumTest
    fun testDeferredStopForeground_coalescesBackToBackStops() {
        val first = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        val second = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        insertWork(first)
        insertWork(second)
        val firstId = first.workSpec.generationalId()
        val secondId = second.workSpec.generationalId()
        processor.startWork(StartStopToken(firstId))
        processor.startWork(StartStopToken(secondId))
        val firstWorker = factory.awaitWorker(first.id) as LatchWorker
        val secondWorker = factory.awaitWorker(second.id) as LatchWorker
        // Called directly on the test thread for setup while serialExecutor is idle.
        processor.startForeground(firstId.workSpecId, foregroundInfo)

        // Simulate setForegroundAsync(secondId) already running on serialExecutor when firstId
        // stops (queuing stop #1), then promoting secondId on serialExecutor, and secondId
        // stopping (queuing stop #2) before the active serialExecutor task finishes.
        val firstStopped = CountDownLatch(1)
        val secondPromoted = CountDownLatch(1)
        val secondStopped = CountDownLatch(1)
        serialExecutor.execute {
            firstStopped.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            processor.startForeground(secondId.workSpecId, foregroundInfo)
            secondPromoted.countDown()
            secondStopped.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        try {
            processor.stopForegroundWork(StartStopToken(firstId), 0) // queues stop #1
            firstStopped.countDown()
            assertTrue(secondPromoted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            processor.stopForegroundWork(StartStopToken(secondId), 0) // queues stop #2
        } finally {
            firstStopped.countDown()
            secondStopped.countDown()
        }
        awaitSerialExecutorIdle()

        val stopIntent = createStopForegroundIntent(context)
        assertEquals(3, context.intents.size) // START first, START second, single STOP
        assertEquals(1, context.intents.count { it.filterEquals(stopIntent) })
        assertTrue(context.intents.last().filterEquals(stopIntent))
        firstWorker.mLatch.countDown()
        secondWorker.mLatch.countDown()
    }

    /**
     * Runs [block] on its own thread and fails if it has not returned within [LOCK_WAIT_LIMIT_MS] —
     * i.e. if it is waiting on a lock that is held across a binder.
     */
    private fun <T> callWithTimeout(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val future = executor.submit(block)
            return try {
                future.get(LOCK_WAIT_LIMIT_MS, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                throw AssertionError(
                    "Processor call blocked for > $LOCK_WAIT_LIMIT_MS ms " +
                        "(mLock held across a binder transaction?)",
                    e,
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @After
    fun tearDown() {
        // Workers first: they are what enqueues follow up work onto the task executor.
        defaultExecutor.shutdownNow()
        assertTrue(defaultExecutor.awaitTermination(3, TimeUnit.SECONDS))
        // Then drain before shutting the task executor down. Shutting its delegate down while
        // SerialExecutorImpl still holds queued work makes it hand a task to a dead executor, and
        // that rejection surfaces on the task executor's own thread, where no caller can catch it,
        // so it kills the test process instead of failing a test. b/560357724
        awaitSerialExecutorIdle()
        backgroundExecutor.shutdownNow()
        assertTrue(backgroundExecutor.awaitTermination(3, TimeUnit.SECONDS))
    }

    /**
     * Blocks until the task executor has run everything queued on it.
     *
     * A single barrier isn't enough, because a drained task can enqueue more work behind the
     * barrier. The runnable re-posts itself until it finds the queue empty.
     */
    private fun awaitSerialExecutorIdle() {
        val drained = CountDownLatch(1)
        lateinit var idleRunnable: Runnable
        idleRunnable = Runnable {
            if (serialExecutor.hasPendingTasks()) {
                serialExecutor.execute(idleRunnable)
            } else {
                drained.countDown()
            }
        }
        serialExecutor.execute(idleRunnable)
        assertTrue(drained.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private class TrackingContext(base: Context) : ContextWrapper(base) {
        /** Service intents "sent"; written from several threads once stops are deferred. */
        val intents = CopyOnWriteArrayList<Intent>()
        private val transactionStarted = Semaphore(0)
        @Volatile private var binderGate: CountDownLatch? = null

        /** Makes the next start/stop-service transaction block, emulating a slow AMS. */
        fun gateNextTransaction(): CountDownLatch {
            transactionStarted.drainPermits()
            return CountDownLatch(1).also { binderGate = it }
        }

        fun awaitTransactionStarted(timeout: Long, unit: TimeUnit): Boolean =
            transactionStarted.tryAcquire(timeout, unit)

        override fun startService(service: Intent): ComponentName? {
            // don't start anything, simply track requests
            transactionStarted.release()
            // Bounded (and longer than any assertion timeout) so a failing test reports the
            // assertion that timed out instead of wedging the executor.
            val gate = binderGate
            if (gate != null) {
                gate.await(2 * TIMEOUT_SECONDS, TimeUnit.SECONDS)
                binderGate = null
            }
            intents.add(service)
            // result isn't used so simply return null
            return null
        }

        override fun startForegroundService(service: Intent): ComponentName? {
            // simply track it
            return startService(service)
        }
    }

    private companion object {
        // Satisfied immediately in the happy path; the bound exists so a regression fails the
        // test rather than hanging it.
        const val TIMEOUT_SECONDS = 10L
        // Generous upper bound for a Processor call that must not wait on a binder.
        const val LOCK_WAIT_LIMIT_MS = 5_000L
    }
}
