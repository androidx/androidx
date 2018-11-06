/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.couroutines

import android.arch.core.executor.ArchTaskExecutor
import android.content.Context
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.coroutines.CoroutineWorker
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.workDataOf
import kotlinx.coroutines.asCoroutineDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@SmallTest
class CoroutineWorkerTest {

    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var database: WorkDatabase
    private lateinit var workManagerImpl: WorkManagerImpl

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance()
            .setDelegate(object : android.arch.core.executor.TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) {
                    runnable.run()
                }

                override fun postToMainThread(runnable: Runnable) {
                    runnable.run()
                }

                override fun isMainThread(): Boolean {
                    return true
                }
            })

        context = InstrumentationRegistry.getTargetContext()
        configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        workManagerImpl = WorkManagerImpl(context, configuration, InstantWorkTaskExecutor())
        WorkManagerImpl.setDelegate(workManagerImpl)
        database = workManagerImpl.getWorkDatabase()
        Logger.setMinimumLoggingLevel(Log.DEBUG)
    }

    @After
    fun tearDown() {
        WorkManagerImpl.setDelegate(null)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun testCoroutineWorker_basicUsage() {
        val workerFactory = WorkerFactory.getDefaultWorkerFactory()
        val worker = workerFactory.createWorkerWithDefaultFallback(
            context,
            SynchronousCoroutineWorker::class.java.name,
            WorkerParameters(
                UUID.randomUUID(),
                Data.EMPTY,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                configuration.executor,
                workManagerImpl.workTaskExecutor,
                workerFactory)) as SynchronousCoroutineWorker

        assertThat(worker.job.isCompleted, `is`(false))

        val future = worker.startWork()
        val payload = future.get()

        assertThat(future.isDone, `is`(true))
        assertThat(future.isCancelled, `is`(false))
        assertThat(worker.job.isCompleted, `is`(true))
        assertThat(payload.result, `is`(ListenableWorker.Result.SUCCESS))
        assertThat(payload.outputData.getLong("output", 0L), `is`(999L))
    }

    @Test
    fun testCoroutineWorker_cancellingFutureCancelsJob() {
        val workerFactory = WorkerFactory.getDefaultWorkerFactory()
        val worker = workerFactory.createWorkerWithDefaultFallback(
            context,
            SynchronousCoroutineWorker::class.java.name,
            WorkerParameters(
                UUID.randomUUID(),
                Data.EMPTY,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                configuration.executor,
                workManagerImpl.workTaskExecutor,
                workerFactory)) as SynchronousCoroutineWorker

        assertThat(worker.job.isCancelled, `is`(false))
        worker.future.cancel(true)
        assertThat(worker.job.isCancelled, `is`(true))
    }

    class SynchronousExecutor : Executor {

        override fun execute(command: Runnable) {
            command.run()
        }
    }

    class InstantWorkTaskExecutor : TaskExecutor {

        private val mSynchronousExecutor = SynchronousExecutor()

        override fun postToMainThread(runnable: Runnable) {
            runnable.run()
        }

        override fun getMainThreadExecutor(): Executor {
            return mSynchronousExecutor
        }

        override fun executeOnBackgroundThread(runnable: Runnable) {
            runnable.run()
        }

        override fun getBackgroundExecutor(): Executor {
            return mSynchronousExecutor
        }

        override fun getBackgroundExecutorThread(): Thread {
            return Thread.currentThread()
        }
    }

    class SynchronousCoroutineWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Payload {
            return Payload(Result.SUCCESS, workDataOf("output" to 999L))
        }

        override val coroutineContext = SynchronousExecutor().asCoroutineDispatcher()
    }
}