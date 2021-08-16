/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.os.BuildCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.WorkForegroundRunnable
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
public class WorkForegroundRunnableTest : DatabaseTest() {
    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var executor: Executor
    private lateinit var progressUpdater: ProgressUpdater
    private lateinit var foregroundUpdater: ForegroundUpdater
    private lateinit var taskExecutor: TaskExecutor

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        executor = SynchronousExecutor()
        configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(executor)
            .build()
        progressUpdater = mock(ProgressUpdater::class.java)
        foregroundUpdater = mock(ForegroundUpdater::class.java)
        taskExecutor = InstantWorkTaskExecutor()
    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 30)
    public fun doesNothing_forRegularWorkRequests() {
        val work = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .build()

        insertWork(work)
        val worker = spy(
            configuration.mWorkerFactory.createWorkerWithDefaultFallback(
                context,
                work.workSpec.workerClassName,
                newWorkerParams(work)
            )!!
        )
        val runnable = WorkForegroundRunnable(
            context,
            work.workSpec,
            worker,
            foregroundUpdater,
            taskExecutor
        )
        runnable.run()
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
        verifyNoMoreInteractions(foregroundUpdater)
    }

    @Test
    @MediumTest
    public fun callGetForeground_forExpeditedWork1() {
        if (BuildCompat.isAtLeastS()) {
            return
        }

        val work = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        insertWork(work)
        val worker = spy(
            configuration.mWorkerFactory.createWorkerWithDefaultFallback(
                context,
                work.workSpec.workerClassName,
                newWorkerParams(work)
            )!!
        )
        val runnable = WorkForegroundRunnable(
            context,
            work.workSpec,
            worker,
            foregroundUpdater,
            taskExecutor
        )
        runnable.run()
        verify(worker).foregroundInfoAsync
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
    }

    @Test
    @SmallTest
    public fun callGetForeground_forExpeditedWork2() {
        if (BuildCompat.isAtLeastS()) {
            return
        }

        val work = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        insertWork(work)
        val worker = spy(
            configuration.mWorkerFactory.createWorkerWithDefaultFallback(
                context,
                work.workSpec.workerClassName,
                newWorkerParams(work)
            )!!
        )

        val notification = mock(Notification::class.java)
        val id = 10
        val foregroundInfo = ForegroundInfo(id, notification)
        val foregroundFuture = SettableFuture.create<ForegroundInfo>()
        foregroundFuture.set(foregroundInfo)
        `when`(worker.foregroundInfoAsync).thenReturn(foregroundFuture)
        val runnable = WorkForegroundRunnable(
            context,
            work.workSpec,
            worker,
            foregroundUpdater,
            taskExecutor
        )
        runnable.run()
        verify(worker).foregroundInfoAsync
        verify(foregroundUpdater).setForegroundAsync(context, work.id, foregroundInfo)
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
    }

    private fun newWorkerParams(workRequest: WorkRequest) = WorkerParameters(
        UUID.fromString(workRequest.stringId),
        Data.EMPTY,
        listOf<String>(),
        WorkerParameters.RuntimeExtras(),
        1,
        executor,
        taskExecutor,
        configuration.mWorkerFactory,
        progressUpdater,
        foregroundUpdater
    )
}
