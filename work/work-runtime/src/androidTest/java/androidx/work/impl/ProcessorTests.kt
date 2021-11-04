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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class ProcessorTests : DatabaseTest() {
    lateinit var scheduler: Scheduler
    lateinit var factory: WorkerFactory
    lateinit var workerMap: MutableMap<String, ListenableWorker?>
    lateinit var processor: Processor
    lateinit var defaultExecutor: ExecutorService
    lateinit var backgroundExecutor: ExecutorService
    lateinit var serialExecutor: SerialExecutor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        defaultExecutor = Executors.newSingleThreadExecutor()
        backgroundExecutor = Executors.newSingleThreadExecutor()
        serialExecutor = SerialExecutor(backgroundExecutor)
        val taskExecutor = object : TaskExecutor {
            val mainExecutor = Executor { runnable ->
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                mainExecutor.execute(runnable)
            }

            override fun getMainThreadExecutor(): Executor {
                return mainExecutor
            }

            override fun executeOnBackgroundThread(runnable: Runnable) {
                backgroundExecutor.execute(runnable)
            }

            override fun getBackgroundExecutor(): SerialExecutor {
                return serialExecutor
            }
        }
        workerMap = mutableMapOf()
        factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                val worker = getDefaultWorkerFactory()
                    .createWorkerWithDefaultFallback(
                        appContext,
                        workerClassName,
                        workerParameters
                    )
                workerMap[workerParameters.id.toString()] = worker
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
        val request = OneTimeWorkRequest.Builder(StopLatchWorker::class.java)
            .build()
        insertWork(request)
        var listenerCalled = false
        val listener = ExecutionListener { id, _ ->
            if (!listenerCalled) {
                listenerCalled = true
                assertEquals(request.workSpec.id, id)
            }
        }
        processor.addExecutionListener(listener)
        processor.startWork(request.workSpec.id)
        processor.stopWork(request.workSpec.id)
        // This would have previously failed trying to acquire a lock
        processor.startWork(request.workSpec.id)
        (workerMap[request.workSpec.id] as? StopLatchWorker)?.countDown()
        while (serialExecutor.hasPendingTasks()) {
            // Wait until we are done
        }
        assertEquals(listenerCalled, true)
    }

    @After
    fun tearDown() {
        defaultExecutor.shutdownNow()
        backgroundExecutor.shutdownNow()
    }
}
