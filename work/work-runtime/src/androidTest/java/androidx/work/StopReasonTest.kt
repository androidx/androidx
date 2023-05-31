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

import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.impl.Processor
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkLauncherImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.background.greedy.GreedyScheduler
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.worker.InfiniteTestWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 31)
class StopReasonTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val workerFactory = TrackingWorkerFactory()
    val configuration = Configuration.Builder().setWorkerFactory(workerFactory).build()
    val executor = Executors.newSingleThreadExecutor()
    val taskExecutor = WorkManagerTaskExecutor(executor)
    val fakeChargingTracker = TestConstraintTracker(false, context, taskExecutor)
    val trackers = Trackers(
        context = context,
        taskExecutor = taskExecutor,
        batteryChargingTracker = fakeChargingTracker
    )
    val db = WorkDatabase.create(context, executor, configuration.clock, true)

    val processor = Processor(context, configuration, taskExecutor, db)
    val launcher = WorkLauncherImpl(processor, taskExecutor)
    val greedyScheduler = GreedyScheduler(
        context, configuration, trackers, processor, launcher,
        taskExecutor
    )
    val workManager = WorkManagerImpl(
        context, configuration, taskExecutor, db, listOf(greedyScheduler), processor, trackers
    )

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    fun testStopReasonPropagated() = runBlocking {
        fakeChargingTracker.constraintState = true
        val request = OneTimeWorkRequest.Builder(InfiniteTestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(request).await()
        val worker = workerFactory.await(request.id)
        fakeChargingTracker.constraintState = false
        workManager.getWorkInfoByIdFlow(request.id).first { it.state == WorkInfo.State.ENQUEUED }
        assertThat(worker.isStopped).isTrue()
        assertThat(worker.stopReason).isEqualTo(STOP_REASON_CONSTRAINT_CHARGING)
    }

    @Test
    fun testGetStopReasonThrowsWhileRunning() = runBlocking {
        val request = OneTimeWorkRequest.Builder(InfiniteTestWorker::class.java).build()
        workManager.enqueue(request)
        val worker = workerFactory.await(request.id)
        workManager.getWorkInfoByIdFlow(request.id).first { it.state == WorkInfo.State.RUNNING }
        try {
            worker.stopReason
            throw AssertionError()
        } catch (e: IllegalStateException) {
            // it is expected to happen
        }
    }
}