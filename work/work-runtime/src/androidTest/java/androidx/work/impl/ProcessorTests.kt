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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.DatabaseTest
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.StopLatchWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProcessorTests : DatabaseTest() {
    lateinit var scheduler: Scheduler
    lateinit var factory: WorkerFactory
    val lastCreatedWorker = MutableStateFlow<ListenableWorker?>(null)
    lateinit var processor: Processor
    lateinit var defaultExecutor: ExecutorService
    lateinit var backgroundExecutor: ExecutorService
    lateinit var serialExecutor: SerialExecutor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        // first worker will take over the first thread with its doWork
        // second worker will execute on the second thread
        defaultExecutor = Executors.newFixedThreadPool(2)
        backgroundExecutor = Executors.newSingleThreadExecutor()
        serialExecutor = SerialExecutor(backgroundExecutor)
        val taskExecutor = object : TaskExecutor {
            val mainExecutor = Executor { runnable ->
                runnable.run()
            }

            override fun getMainThreadExecutor(): Executor {
                return mainExecutor
            }

            override fun getSerialTaskExecutor(): SerialExecutor {
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
                assertEquals(request1.workSpec.id, id)
            }
        }
        processor.addExecutionListener(listener)
        val workRunId = WorkRunId(request1.workSpec.id)
        processor.startWork(workRunId)

        val firstWorker = runBlocking { lastCreatedWorker.filterNotNull().first() }
        val blockedThread = Executors.newSingleThreadExecutor()
        blockedThread.execute {
            // gonna stall for 10 seconds
            processor.stopWork(workRunId)
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
        processor.startWork(WorkRunId(request2.workSpec.id))
        val secondWorker =
            runBlocking { lastCreatedWorker.filterNotNull().filter { it != firstWorker }.first() }
        (secondWorker as StopLatchWorker).countDown()
        assertTrue(executionFinished.await(3, TimeUnit.SECONDS))
        firstWorker.countDown()
        blockedThread.shutdown()
        assertTrue(blockedThread.awaitTermination(3, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        defaultExecutor.shutdownNow()
        backgroundExecutor.shutdownNow()
        assertTrue(defaultExecutor.awaitTermination(3, TimeUnit.SECONDS))
        assertTrue(backgroundExecutor.awaitTermination(3, TimeUnit.SECONDS))
    }
}
