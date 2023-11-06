/*
 * Copyright 2023 The Android Open Source Project
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

import android.app.job.JobParameters.STOP_REASON_CANCELLED_BY_APP
import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.testutils.launchTester
import androidx.work.worker.CompletableWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 31)
class StopReasonTest {
    val workerFactory = TrackingWorkerFactory()
    val configuration = Configuration.Builder().setWorkerFactory(workerFactory)
        .setTaskExecutor(Executors.newSingleThreadExecutor()).build()
    val env = TestEnv(configuration)
    val fakeChargingTracker = TestConstraintTracker(false, env.context, env.taskExecutor)
    val trackers = Trackers(
        context = env.context,
        taskExecutor = env.taskExecutor,
        batteryChargingTracker = fakeChargingTracker
    )
    val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    fun testStopReasonPropagated() = runBlocking {
        fakeChargingTracker.constraintState = true
        val request = OneTimeWorkRequest.Builder(CompletableWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(request).await()
        val worker = workerFactory.await(request.id)
        val tester = launchTester(workManager.getWorkInfoByIdFlow(request.id))
        val runningWorkInfo = tester.awaitNext()
        assertThat(runningWorkInfo.state).isEqualTo(WorkInfo.State.RUNNING)
        assertThat(runningWorkInfo.stopReason).isEqualTo(WorkInfo.STOP_REASON_NOT_STOPPED)

        fakeChargingTracker.constraintState = false
        val workInfo = tester.awaitNext()
        assertThat(worker.isStopped).isTrue()
        assertThat(worker.stopReason).isEqualTo(STOP_REASON_CONSTRAINT_CHARGING)
        assertThat(workInfo.stopReason).isEqualTo(STOP_REASON_CONSTRAINT_CHARGING)
    }

    @Test
    fun testGetStopReasonWhileRunning() = runBlocking {
        val request = OneTimeWorkRequest.Builder(CompletableWorker::class.java).build()
        workManager.enqueue(request)
        val worker = workerFactory.await(request.id)
        workManager.getWorkInfoByIdFlow(request.id).first { it.state == WorkInfo.State.RUNNING }
        assertThat(worker.stopReason).isEqualTo(WorkInfo.STOP_REASON_NOT_STOPPED)
    }

    @Test
    fun testStopReasonWhenCancelled() = runBlocking {
        val request = OneTimeWorkRequest.Builder(CompletableWorker::class.java).build()
        workManager.enqueue(request)
        val worker = workerFactory.await(request.id)
        workManager.getWorkInfoByIdFlow(request.id).first { it.state == WorkInfo.State.RUNNING }
        workManager.cancelWorkById(request.id)
        val workInfo = workManager.getWorkInfoByIdFlow(request.id)
            .first { it.state == WorkInfo.State.CANCELLED }
        assertThat(worker.isStopped).isTrue()
        assertThat(worker.stopReason).isEqualTo(STOP_REASON_CANCELLED_BY_APP)
        assertThat(workInfo.stopReason).isEqualTo(STOP_REASON_CANCELLED_BY_APP)
    }

    @Test
    fun testStopReasonWhenCancelledPreRun() = runBlocking {
        val request = OneTimeWorkRequest.Builder(CompletableWorker::class.java)
            .setInitialDelay(10, TimeUnit.DAYS).build()
        workManager.enqueue(request).await()
        workManager.cancelWorkById(request.id).await()
        val workInfo = workManager.getWorkInfoById(request.id).await()
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.CANCELLED)
        assertThat(workInfo.stopReason).isEqualTo(WorkInfo.STOP_REASON_NOT_STOPPED)
    }
}
