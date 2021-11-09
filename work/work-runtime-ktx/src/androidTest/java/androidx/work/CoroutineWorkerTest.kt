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

package androidx.work

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.WorkProgressUpdater
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.workers.ProgressUpdatingWorker
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@SmallTest
class CoroutineWorkerTest {

    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var database: WorkDatabase
    private lateinit var workManagerImpl: WorkManagerImpl
    private lateinit var progressUpdater: ProgressUpdater
    private lateinit var mForegroundUpdater: ForegroundUpdater

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance()
            .setDelegate(object : androidx.arch.core.executor.TaskExecutor() {
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

        context = ApplicationProvider.getApplicationContext()
        configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        workManagerImpl = WorkManagerImpl(
            context, configuration,
            InstantWorkTaskExecutor()
        )
        WorkManagerImpl.setDelegate(workManagerImpl)
        database = workManagerImpl.workDatabase
        // No op
        progressUpdater = ProgressUpdater { _, _, _ ->
            val future = SettableFuture.create<Void>()
            future.set(null)
            future
        }
        mForegroundUpdater = mock(ForegroundUpdater::class.java)
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
                workerFactory,
                progressUpdater,
                mForegroundUpdater
            )
        ) as SynchronousCoroutineWorker

        assertThat(worker.job.isCompleted, `is`(false))

        val future = worker.startWork()
        val result = future.get()

        assertThat(future.isDone, `is`(true))
        assertThat(future.isCancelled, `is`(false))
        assertThat(result, `is`(instanceOf(ListenableWorker.Result.Success::class.java)))
        assertThat(
            (result as ListenableWorker.Result.Success).outputData.getLong(
                "output", 0L
            ),
            `is`(999L)
        )
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
                workerFactory,
                progressUpdater,
                mForegroundUpdater
            )
        ) as SynchronousCoroutineWorker

        assertThat(worker.job.isCancelled, `is`(false))
        worker.future.cancel(true)
        assertThat(worker.job.isCancelled, `is`(true))
    }

    @Test
    @LargeTest
    fun testProgressUpdates() {
        val workerFactory = WorkerFactory.getDefaultWorkerFactory()
        val progressUpdater = spy(WorkProgressUpdater(database, workManagerImpl.workTaskExecutor))
        val workRequest = OneTimeWorkRequestBuilder<ProgressUpdatingWorker>().build()
        database.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val worker = workerFactory.createWorkerWithDefaultFallback(
            context,
            ProgressUpdatingWorker::class.java.name,
            WorkerParameters(
                workRequest.id,
                Data.EMPTY,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                configuration.executor,
                workManagerImpl.workTaskExecutor,
                workerFactory,
                progressUpdater,
                mForegroundUpdater
            )
        ) as ProgressUpdatingWorker

        runBlocking {
            val result = worker.doWork()
            val captor = ArgumentCaptor.forClass(Data::class.java)
            verify(progressUpdater, times(2))
                .updateProgress(
                    any(Context::class.java),
                    any(UUID::class.java),
                    captor.capture()
                )
            assertThat(result, `is`(instanceOf(ListenableWorker.Result.Success::class.java)))
            val recent = captor.allValues.lastOrNull()
            assertThat(recent?.getInt(ProgressUpdatingWorker.Progress, 0), `is`(100))
            val progress = database.workProgressDao().getProgressForWorkSpecId(workRequest.stringId)
            assertThat(progress, nullValue())
        }
    }

    @Test
    @LargeTest
    fun testProgressUpdatesForRetry() {
        val workerFactory = WorkerFactory.getDefaultWorkerFactory()
        val progressUpdater = spy(WorkProgressUpdater(database, workManagerImpl.workTaskExecutor))
        val input = workDataOf(ProgressUpdatingWorker.Expected to "Retry")
        val workRequest = OneTimeWorkRequestBuilder<ProgressUpdatingWorker>()
            .setInputData(input)
            .build()
        database.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val worker = workerFactory.createWorkerWithDefaultFallback(
            context,
            ProgressUpdatingWorker::class.java.name,
            WorkerParameters(
                workRequest.id,
                Data.EMPTY,
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                configuration.executor,
                workManagerImpl.workTaskExecutor,
                workerFactory,
                progressUpdater,
                mForegroundUpdater
            )
        ) as ProgressUpdatingWorker

        runBlocking {
            val result = worker.doWork()
            val captor = ArgumentCaptor.forClass(Data::class.java)
            verify(progressUpdater, times(2))
                .updateProgress(
                    any(Context::class.java),
                    any(UUID::class.java),
                    captor.capture()
                )
            assertThat(result, `is`(instanceOf(ListenableWorker.Result.Success::class.java)))
            val recent = captor.allValues.lastOrNull()
            assertThat(recent?.getInt(ProgressUpdatingWorker.Progress, 0), `is`(100))
            val progress = database.workProgressDao().getProgressForWorkSpecId(workRequest.stringId)
            assertThat(progress, nullValue())
        }
    }

    class SynchronousExecutor : Executor {

        override fun execute(command: Runnable) {
            command.run()
        }
    }

    class InstantWorkTaskExecutor : TaskExecutor {

        private val mSynchronousExecutor = SynchronousExecutor()
        private val mSerialExecutor = SerialExecutor(mSynchronousExecutor)

        override fun postToMainThread(runnable: Runnable) {
            runnable.run()
        }

        override fun getMainThreadExecutor(): Executor {
            return mSynchronousExecutor
        }

        override fun executeOnBackgroundThread(runnable: Runnable) {
            mSerialExecutor.execute(runnable)
        }

        override fun getBackgroundExecutor(): SerialExecutor {
            return mSerialExecutor
        }
    }

    class SynchronousCoroutineWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            return Result.success(workDataOf("output" to 999L))
        }

        override val coroutineContext = SynchronousExecutor().asCoroutineDispatcher()
    }
}