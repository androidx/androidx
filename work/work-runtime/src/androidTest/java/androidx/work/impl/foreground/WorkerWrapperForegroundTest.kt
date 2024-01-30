/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.foreground

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.WorkerWrapper
import androidx.work.impl.schedulers
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.worker.StopAwareForegroundWorker
import androidx.work.worker.TestForegroundWorker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy

@RunWith(AndroidJUnit4::class)
@LargeTest
class WorkerWrapperForegroundTest {
    private lateinit var context: Context
    private lateinit var handler: Handler
    private lateinit var config: Configuration
    private lateinit var executor: ExecutorService
    private lateinit var internalExecutor: ExecutorService
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var workDatabase: WorkDatabase
    private lateinit var workManager: WorkManagerImpl
    private lateinit var foregroundProcessor: ForegroundProcessor

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        // Prevent startService here to avoid notifications during tests
        val componentName = ComponentName(context, SystemForegroundService::class.java)
        doReturn(componentName).`when`(context).startService(any<Intent>())
        doReturn(context).`when`(context).applicationContext

        executor = Executors.newSingleThreadExecutor()
        internalExecutor = Executors.newSingleThreadExecutor()
        handler = Handler(Looper.getMainLooper())
        config = Configuration.Builder()
            .setExecutor(executor)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

        taskExecutor = WorkManagerTaskExecutor(internalExecutor)

        workDatabase = WorkDatabase.create(
            context, taskExecutor.serialTaskExecutor, config.clock, true)
        workManager = WorkManagerImpl(
                context = context,
                configuration = config,
                workTaskExecutor = taskExecutor,
                workDatabase = workDatabase,
                schedulersCreator = schedulers(mock(Scheduler::class.java))
        )
        WorkManagerImpl.setDelegate(workManager)
        // Foreground processor
        foregroundProcessor = mock(ForegroundProcessor::class.java)
    }

    @Test
    fun testWorkerWrapper_doesNotResolveBackingJobImmediately() {
        val request = OneTimeWorkRequest.Builder(StopAwareForegroundWorker::class.java)
            .build()

        workDatabase.workSpecDao().insertWorkSpec(request.workSpec)

        val wrapper = WorkerWrapper.Builder(
            context,
            config,
            taskExecutor,
            foregroundProcessor,
            workDatabase,
            workDatabase.workSpecDao().getWorkSpec(request.stringId)!!,
            emptyList()
        ).build()

        wrapper.run()
        val future = wrapper.future as SettableFuture<Boolean>
        assertThat(future.isDone, `is`(false))
        wrapper.interrupt(0)
        assertThat(future.isDone, `is`(true))
    }

    @Test
    fun testWorkerWrapper_resolvesBackingJob_whenWorkerCompletes() {
        val request = OneTimeWorkRequest.Builder(TestForegroundWorker::class.java)
            .build()

        workDatabase.workSpecDao().insertWorkSpec(request.workSpec)
        val wrapper = WorkerWrapper.Builder(
            context,
            config,
            taskExecutor,
            foregroundProcessor,
            workDatabase,
            workDatabase.workSpecDao().getWorkSpec(request.stringId)!!,
            emptyList()
        ).build()

        wrapper.run()
        val future = wrapper.future as SettableFuture<Boolean>
        val latch = CountDownLatch(1)
        future.addListener({
                assertThat(future.isDone, `is`(true))
                latch.countDown()
            },
            executor
        )

        latch.await(5, TimeUnit.SECONDS)
        assertThat(latch.count, `is`(0L))
    }
}
