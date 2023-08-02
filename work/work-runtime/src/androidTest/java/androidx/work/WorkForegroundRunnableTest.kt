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
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.WorkForegroundRunnable
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
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
        val worker = getWorkerSpy(work)
        val runnable = getWorkForegroundRunnable(work, worker)
        runnable.run()
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
        verifyNoMoreInteractions(foregroundUpdater)
    }

    @Test
    @MediumTest
    public fun callGetForeground_forExpeditedWork1() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }
        val work = getExpeditedRequest<TestWorker>()
        insertWork(work)
        val worker = getWorkerSpy(work)
        val runnable = getWorkForegroundRunnable(work, worker)
        runnable.run()
        verify(worker).foregroundInfoAsync
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
    }

    @Test
    @SmallTest
    public fun callGetForeground_forExpeditedWork2() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }

        val work = getExpeditedRequest<TestWorker>()
        insertWork(work)
        val worker = getWorkerSpy(work)
        val foregroundInfo = getForegroundInfo()
        doReturn(foregroundInfo).`when`(worker).foregroundInfo
        val runnable = getWorkForegroundRunnable(work, worker)
        runnable.run()
        verify(worker).foregroundInfo
        verify(worker).foregroundInfoAsync
        verify(foregroundUpdater).setForegroundAsync(context, work.id, foregroundInfo)
        assertThat(runnable.future.isDone, `is`(equalTo(true)))
    }

    private fun getForegroundInfo(): ForegroundInfo {
        val notification = mock(Notification::class.java)
        val id = 10
        return ForegroundInfo(id, notification)
    }

    @SmallTest
    public fun callGetForeground_forExpeditedWork3() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }

        val work = getExpeditedRequest<TestWorker>()
        insertWork(work)
        val worker = getWorkerSpy(work)

        try {
         worker.foregroundInfo // should throw expected exception here
        } catch (ise: IllegalStateException) {
            // Nothing to do here. Test succeeded.
            return
        }
        fail("Worker should have thrown an IllegalStateException at the `foregroundInfo` call.")
    }

    private inline fun <reified T : ListenableWorker> getExpeditedRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(T::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }

    private fun getWorkerSpy(work: OneTimeWorkRequest) = spy(
        configuration.mWorkerFactory.createWorkerWithDefaultFallback(
            context,
            work.workSpec.workerClassName,
            newWorkerParams(work)
        ) as Worker
    )

    private fun getWorkForegroundRunnable(
        work: OneTimeWorkRequest,
        worker: Worker
    ) = WorkForegroundRunnable(
        context,
        work.workSpec,
        worker,
        foregroundUpdater,
        taskExecutor
    )

    private fun newWorkerParams(workRequest: WorkRequest) = WorkerParameters(
        UUID.fromString(workRequest.stringId),
        Data.EMPTY,
        listOf<String>(),
        WorkerParameters.RuntimeExtras(),
        1,
        0,
        executor,
        taskExecutor,
        configuration.mWorkerFactory,
        progressUpdater,
        foregroundUpdater
    )
}
