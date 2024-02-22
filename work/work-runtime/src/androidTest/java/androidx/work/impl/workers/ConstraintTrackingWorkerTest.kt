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

package androidx.work.impl.workers

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter.getFuture
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.DoWorkAwareWorker
import androidx.work.impl.NoOpForegroundProcessor
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.WorkerWrapper
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.workDataOf
import androidx.work.worker.CompletableWorker
import androidx.work.worker.EchoingWorker
import androidx.work.worker.ExceptionWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ConstraintTrackingWorkerTest {
    val workerFactory = TrackingWorkerFactory()
    val configuration =
        Configuration.Builder().setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
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
    fun testFailingWorker() = runBlocking {
        val workerWrapper = create(ExceptionWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        workerFactory.await(workerWrapper.workSpecId)
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.FAILED)
    }

    @Test
    fun testSelfCancellingWorker() = runBlocking {
        val workerWrapper = create(SelfCancellingWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        val worker = awaitWorker<SelfCancellingWorker>(workerWrapper)
        workerWrapper.future.await()
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.FAILED)
        assertThat(worker.stopCounter).isEqualTo(0)
    }

    @Test
    fun testConstraintTrackingWorker_onConstraintsNotMet() = runBlocking {
        // charging constraint isn't satisfied
        fakeChargingTracker.constraintState = false
        val workerWrapper = create(TestWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        workerFactory.await(workerWrapper.workSpecId)
        workerWrapper.future.await()
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.ENQUEUED)
    }

    @Test
    fun testConstraintTrackingWorker_onConstraintsMet() = runBlocking {
        val workerWrapper = create(EchoingWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        workerFactory.await(workerWrapper.workSpecId)
        workerWrapper.future.await()
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.SUCCEEDED)
        val outputData = workManager.getWorkInfoById(workerWrapper.workSpecId).await().outputData
        assertThat(outputData.getBoolean(TEST_ARGUMENT_NAME, false)).isTrue()
    }

    @Test
    fun testConstraintTrackingWorker_onConstraintsChanged() = runBlocking {
        val workerWrapper = create(CompletableWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        workerFactory.await(workerWrapper.workSpecId)
        fakeChargingTracker.constraintState = false
        workerWrapper.future.await()
        val state = workManager.awaitNotRunning(workerWrapper)
        assertThat(state).isEqualTo(State.ENQUEUED)
    }

    @Test
    fun testConstraintTrackingWorker_stopPropagated() = runBlocking {
        val workerWrapper = create(DoWorkAwareWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)

        val worker = awaitWorker<DoWorkAwareWorker>(workerWrapper)
        worker.doWorkEvent.await()
        launch { workerWrapper.interrupt(0) }

        workerWrapper.future.await()
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.ENQUEUED)
        // WorkerWrapper future is resolved before cancellation is fully propagated in coroutines
        withTimeoutOrNull(300) { worker.onStopEvent.await() }
            ?: throw AssertionError("onStopEventDidHappened")
    }

    @Test
    fun test_runOnMain() = runBlocking {
        val workerWrapper = create(ThreadAssertingWorker::class)
        taskExecutor.serialTaskExecutor.execute(workerWrapper)
        workerWrapper.future.await()
        assertThat(workManager.awaitNotRunning(workerWrapper)).isEqualTo(State.SUCCEEDED)
    }

    private fun create(delegate: KClass<*>): WorkerWrapper {
        val input =
            workDataOf(ARGUMENT_CLASS_NAME to delegate.qualifiedName!!, TEST_ARGUMENT_NAME to true)
        val request = OneTimeWorkRequest.Builder(ConstraintTrackingWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .setInputData(input)
            .build()
        workManager.workDatabase.workSpecDao().insertWorkSpec(request.workSpec)
        return WorkerWrapper.Builder(
            env.context, env.configuration, env.taskExecutor,
            NoOpForegroundProcessor, workManager.workDatabase, request.workSpec, emptyList()
        ).build()
    }

    // It correctly handles the fact that at first is created ConstraintTrackingWorker
    // and only later dev's worker itself
    private suspend inline fun <reified T> awaitWorker(workerWrapper: WorkerWrapper) =
        workerFactory.createdWorkers.mapNotNull { it[workerWrapper.workSpecId] as? T }.first()
}

private const val TEST_ARGUMENT_NAME = "test"

class ThreadAssertingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : ListenableWorker(appContext, workerParams) {
    override fun startWork(): ListenableFuture<Result> = getFuture {
        if (Looper.getMainLooper() != Looper.myLooper())
            throw AssertionError("start work must be called on main thread")
        it.set(Result.success())
        "ThreadAssertingWorker"
    }
}

class SelfCancellingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : ListenableWorker(appContext, workerParams) {
    var stopCounter: Int = 0

    override fun startWork(): ListenableFuture<Result> = getFuture {
        it.setCancelled()
    }
    override fun onStopped() {
        stopCounter++
    }
}

private suspend fun WorkManager.awaitNotRunning(workerWrapper: WorkerWrapper) =
    getWorkInfoByIdFlow(workerWrapper.workSpecId).first { it.state != State.RUNNING }.state

private val WorkerWrapper.workSpecId
    get() = UUID.fromString(workGenerationalId.workSpecId)
