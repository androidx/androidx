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

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.utils.workForeground
import androidx.work.worker.TestForegroundWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
public class WorkForegroundRunnableTest : DatabaseTest() {
    private lateinit var context: Context
    private lateinit var configuration: Configuration
    private lateinit var executor: Executor
    private lateinit var progressUpdater: ProgressUpdater
    private lateinit var foregroundUpdater: CapturingForegroundUpdater
    private lateinit var taskExecutor: TaskExecutor

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        executor = SynchronousExecutor()
        configuration =
            Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setExecutor(executor).build()
        progressUpdater = mock(ProgressUpdater::class.java)
        foregroundUpdater = CapturingForegroundUpdater()
        taskExecutor = InstantWorkTaskExecutor()
    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 30)
    public fun doesNothing_forRegularWorkRequests() {
        val work = OneTimeWorkRequest.Builder(TestWorker::class.java).build()

        insertWork(work)
        val worker = getWorker(work)
        runBlocking { workForeground(work, worker) }
        assertThat(foregroundUpdater.calledParams).isNull()
    }

    @Test
    @MediumTest
    public fun callGetForeground_forExpeditedWorkFailure() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }
        val work = getExpeditedRequest<TestWorker>()
        insertWork(work)
        val worker = getWorker(work)
        try {
            runBlocking { workForeground(work, worker) }
            fail("Worker should have thrown an IllegalStateException at the `foregroundInfo` call.")
        } catch (ise: IllegalStateException) {
            // Nothing to do here. Test succeeded.
        }
    }

    @Test
    @SmallTest
    public fun callGetForeground_forExpeditedWork() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }

        val work = getExpeditedRequest<TestForegroundWorker>()
        insertWork(work)
        val worker = getWorker(work)

        runBlocking { workForeground(work, worker) }
        val (id, foregroundInfo) =
            foregroundUpdater.calledParams
                ?: throw AssertionError("setForegroundAsync must be called")
        assertThat(id).isEqualTo(work.id)
        assertThat(foregroundInfo.notificationId).isEqualTo(TestForegroundWorker.NotificationId)
    }

    @SmallTest
    public fun callGetForeground_forExpeditedWork3() {
        if (Build.VERSION.SDK_INT >= 31) {
            return
        }

        val work = getExpeditedRequest<TestWorker>()
        insertWork(work)
        val worker = getWorker(work)

        try {
            worker.getForegroundInfo() // should throw expected exception here
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

    private fun getWorker(work: OneTimeWorkRequest) =
        configuration.workerFactory.createWorkerWithDefaultFallback(
            context,
            work.workSpec.workerClassName,
            newWorkerParams(work)
        ) as Worker

    private suspend fun workForeground(work: OneTimeWorkRequest, worker: Worker) =
        workForeground(context, work.workSpec, worker, foregroundUpdater, taskExecutor)

    private fun newWorkerParams(workRequest: WorkRequest) =
        WorkerParameters(
            UUID.fromString(workRequest.stringId),
            Data.EMPTY,
            listOf<String>(),
            WorkerParameters.RuntimeExtras(),
            1,
            0,
            executor,
            Dispatchers.Default,
            taskExecutor,
            configuration.workerFactory,
            progressUpdater,
            foregroundUpdater
        )
}

private class CapturingForegroundUpdater : ForegroundUpdater {
    var calledParams: Pair<UUID, ForegroundInfo>? = null

    override fun setForegroundAsync(
        context: Context,
        id: UUID,
        foregroundInfo: ForegroundInfo
    ): ListenableFuture<Void?> {
        calledParams = id to foregroundInfo
        return launchFuture { null }
    }
}
