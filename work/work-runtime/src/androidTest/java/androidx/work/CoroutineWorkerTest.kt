/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.testutils.awaitWorkerEnqueued
import androidx.work.testutils.awaitWorkerFinished
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class CoroutineWorkerTest {
    val workerFactory = TrackingWorkerFactory()
    val configuration =
        Configuration.Builder().setWorkerFactory(workerFactory)
            .setTaskExecutor(Executors.newSingleThreadExecutor()).build()
    val env = TestEnv(configuration)
    val taskExecutor = env.taskExecutor
    val fakeChargingTracker = TestConstraintTracker(true, env.context, env.taskExecutor)
    val trackers = Trackers(
        context = env.context,
        taskExecutor = env.taskExecutor,
        batteryChargingTracker = fakeChargingTracker
    )
    val greedyScheduler = GreedyScheduler(env, trackers)
    val workManager = WorkManager(env, listOf(greedyScheduler), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    fun testCoroutineWorker_basicUsage() = runBlocking {
        val request = OneTimeWorkRequest.from(SuccessCoroutineWorker::class.java)
        workManager.enqueue(request)
        val workInfo = workManager.awaitWorkerFinished(request.id)
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(workInfo.outputData.getLong("output", 0L)).isEqualTo(999L)
    }

    @Test
    fun testCoroutineWorker_failingWorker() = runBlocking {
        val request = OneTimeWorkRequest.from(ThrowingCoroutineWorker::class.java)
        workManager.enqueue(request)
        val workInfo = workManager.awaitWorkerFinished(request.id)
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.FAILED)
    }

    @Test
    fun testCoroutineWorker_interruptionCancelsJob() = runBlocking {
        val request =
            OneTimeWorkRequest.Builder(CancellationCheckingWorker::class.java)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(request)
        val worker = workerFactory.await(request.id) as CancellationCheckingWorker
        worker.doWorkCalled.await()
        fakeChargingTracker.constraintState = false
        workManager.awaitWorkerEnqueued(request.id)
        assertThat(worker.cancelled).isTrue()
    }

    @Test
    fun testProgressUpdates() = runBlocking {
        val request = OneTimeWorkRequest.from(ProgressUpdatingWorker::class.java)
        workManager.enqueue(request)
        val worker = workerFactory.await(request.id) as ProgressUpdatingWorker

        val progress1 = workManager.getWorkInfoByIdFlow(request.id)
            .first { it.progress.getInt("progress", 0) != 0 }.progress
        assertThat(progress1.getInt("progress", 0)).isEqualTo(1)
        worker.firstCheckPoint.complete(Unit)

        val progress2 = workManager.getWorkInfoByIdFlow(request.id)
            .first { it.progress.getInt("progress", 0) != 1 }.progress
        assertThat(progress2.getInt("progress", 0)).isEqualTo(100)
        worker.secondCheckPoint.complete(Unit)
        val workInfo = workManager.awaitWorkerFinished(request.id)
        assertThat(workInfo.state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(workInfo.progress).isEqualTo(Data.EMPTY)
    }
}

class SuccessCoroutineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success(workDataOf("output" to 999L))
}

class ThrowingCoroutineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        throw IllegalStateException("Failing worker")
    }
}

class CancellationCheckingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    var cancelled = false
    val doWorkCalled = CompletableDeferred<Unit>()
    override suspend fun doWork(): Result {
        doWorkCalled.complete(Unit)
        try {
            // suspends forever
            CompletableDeferred<Unit>().await()
        } catch (c: CancellationException) {
            cancelled = true
            throw c
        }
        return Result.success()
    }
}

class ProgressUpdatingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    val firstCheckPoint = CompletableDeferred<Unit>()
    val secondCheckPoint = CompletableDeferred<Unit>()

    override suspend fun doWork(): Result {
        setProgress(workDataOf("progress" to 1))
        firstCheckPoint.await()
        setProgress(workDataOf("progress" to 100))
        secondCheckPoint.await()
        return Result.success()
    }
}
