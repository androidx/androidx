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

package androidx.work.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.concurrent.futures.CallbackToFutureAdapter.getFuture
import androidx.concurrent.futures.await
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Configuration
import androidx.work.DirectExecutor
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Success
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkerParameters
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.TestEnv
import androidx.work.worker.CompletableWorker
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class WorkerWrapperTestKt {
    val factory = TrackingWorkerFactory()
    val configuration =
        Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).setWorkerFactory(factory).build()
    val testEnv = TestEnv(configuration)

    @Test
    fun testWorkerWrapper_handlesWorkSpecDeletion() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(CompletableWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        val future = workerWrapper.launch()
        val completableWorker = factory.await(workRequest.id) as CompletableWorker
        testEnv.db.workSpecDao().delete(workRequest.stringId)
        completableWorker.result.complete(Success())
        assertThat(future.await()).isFalse()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isNull()
    }

    @Test
    fun testRunning() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(DoWorkAwareWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        val future = workerWrapper.launch()
        val worker = factory.await(workRequest.id) as DoWorkAwareWorker
        worker.doWorkEvent.await()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isEqualTo(RUNNING)
        worker.resultCompleter.set(Success())
        assertThat(future.await()).isFalse()
    }

    @Test
    fun testInterruptionRunning() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(DoWorkAwareWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        val future = workerWrapper.launch()
        val worker = factory.await(workRequest.id) as DoWorkAwareWorker
        worker.doWorkEvent.await()
        workerWrapper.interrupt(0)
        assertThat(future.await()).isTrue()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isEqualTo(ENQUEUED)
    }

    @Test
    fun testInterruptionPreStartWork() =
        runBlocking<Unit> {
            val workRequest = OneTimeWorkRequest.from(CompletableWorker::class.java)
            testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
            val workerWrapper = WorkerWrapper(workRequest.workSpec)
            // gonna block main thread, so startWork() can't be called.
            val mainThreadBlocker = CountDownLatch(1)
            testEnv.taskExecutor.mainThreadExecutor.execute {
                mainThreadBlocker.await()
                workerWrapper.interrupt(0)
            }
            val future = workerWrapper.launch()
            factory.await(workRequest.id)
            // worker is created, but can't start work because main thread is blocked
            // this call will unblock main thread, but interrupt worker
            mainThreadBlocker.countDown()
            assertThat(future.await()).isTrue()
            // tricky moment, currently due to the race Worker can go through
            // running state. Exact order would be:
            // - WorkerWrapper reaches trySetRunning, but doesn't enter it
            // - WorkerWrapper.interrupt() happens and resolves the future (ENQUEUED state)
            // - WorkerWrapper enters trySetRunning and sets the state RUNNING
            // - WorkerWrapper enters tryCheckForInterruptionAndResolve again and
            //   set the state back to ENQUEUE

            testEnv.db
                .workSpecDao()
                .getWorkStatusPojoFlowDataForIds(listOf(workRequest.stringId))
                .map { it.first() }
                .first { it.state == ENQUEUED }
        }

    @Test
    fun testWorker_getsRunAttemptCount() =
        runBlocking<Unit> {
            val workRequest =
                OneTimeWorkRequest.Builder(CompletableWorker::class.java)
                    .setInitialRunAttemptCount(10)
                    .build()

            testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
            val workerWrapper = WorkerWrapper(workRequest.workSpec)
            workerWrapper.launch()
            val worker = factory.await(workRequest.id) as CompletableWorker
            assertThat(worker.runAttemptCount).isEqualTo(10)
            worker.result.complete(Success())
        }

    @Test
    fun stopReason_available_in_synchronous_listener_of_startWork() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(StopReasonAtCancellationWorker::class.java)

        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        val future = workerWrapper.launch()
        val worker = factory.await(workRequest.id) as StopReasonAtCancellationWorker
        worker.startWorkDeferred.await()
        val mainThreadDeferred = CompletableDeferred<Unit>()
        // making sure that task running ListenableWorker.startWork on the main thread is fully done
        // and it posted resulting tasks to task executor
        testEnv.taskExecutor.mainThreadExecutor.execute { mainThreadDeferred.complete(Unit) }

        waitTaskExecutorIdle()
        mainThreadDeferred.await()
        workerWrapper.interrupt(WorkInfo.STOP_REASON_TIMEOUT)
        future.await()
        assertThat(worker.stopReasonAtCancellation).isEqualTo(WorkInfo.STOP_REASON_TIMEOUT)
    }

    @SdkSuppress(maxSdkVersion = 30)
    @Test
    fun onStopped_called_in_between_getForegroundAsync_and_startWork() = runBlocking {
        val workRequest =
            OneTimeWorkRequest.Builder(TestForegroundWithStopWorker::class.java)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        val mainThreadBlocker = CountDownLatch(1)
        testEnv.taskExecutor.mainThreadExecutor.execute { mainThreadBlocker.await() }
        val future = workerWrapper.launch()
        val worker = factory.await(workRequest.id) as TestForegroundWithStopWorker
        worker.stopBlock = { workerWrapper.interrupt(WorkInfo.STOP_REASON_TIMEOUT) }
        // allowing main thread to run. getForegroundInfoAsync will be called and completed.
        // Afterwards worker will be stopped, before calling startWork()
        mainThreadBlocker.countDown()
        future.await()
        assertThat(worker.isStopped).isEqualTo(true)
    }

    fun WorkerWrapper(spec: WorkSpec) =
        WorkerWrapper.Builder(
                testEnv.context,
                configuration,
                testEnv.taskExecutor,
                NoOpForegroundProcessor,
                testEnv.db,
                spec,
                emptyList()
            )
            .build()

    private suspend fun waitTaskExecutorIdle() {
        val deferred = CompletableDeferred<Unit>()
        val taskExecutor = testEnv.taskExecutor.serialTaskExecutor

        lateinit var idleRunnable: Runnable
        idleRunnable = Runnable {
            if (taskExecutor.hasPendingTasks()) {
                taskExecutor.execute(idleRunnable)
            } else {
                deferred.complete(Unit)
            }
        }
        taskExecutor.execute(idleRunnable)
        return deferred.await()
    }
}

class DoWorkAwareWorker(appContext: Context, params: WorkerParameters) :
    ListenableWorker(appContext, params) {
    val doWorkEvent = CompletableDeferred<Unit>()
    lateinit var resultCompleter: Completer<Result>
    val onStopEvent = CompletableDeferred<Unit>()
    private val result = getFuture {
        resultCompleter = it
        "DoWorkAwareWorker"
    }

    override fun startWork(): ListenableFuture<Result> {
        doWorkEvent.complete(Unit)
        return result
    }

    override fun onStopped() {
        super.onStopped()
        onStopEvent.complete(Unit)
    }
}

class StopReasonAtCancellationWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    private lateinit var completer: Completer<Result>
    var stopReasonAtCancellation: Int = WorkInfo.STOP_REASON_NOT_STOPPED
    val startWorkDeferred = CompletableDeferred<Unit>()

    @SuppressLint("NewApi")
    override fun startWork(): ListenableFuture<Result> =
        getFuture {
                completer = it
                "startWork"
            }
            .also {
                it.addListener({ stopReasonAtCancellation = stopReason }, DirectExecutor.INSTANCE)
                startWorkDeferred.complete(Unit)
            }
}

class TestForegroundWithStopWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    var stopBlock: () -> Unit = {}

    override fun startWork(): ListenableFuture<Result> = getFuture {
        it.setException(IllegalStateException("Should not be called"))
        "TestForegroundBlockingMainWorker"
    }

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> = getFuture {
        val channel =
            NotificationChannelCompat.Builder("test", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("hello")
                .build()
        NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
        val notification =
            NotificationCompat.Builder(applicationContext, "test")
                .setOngoing(true)
                .setTicker("ticker")
                .setContentText("content text")
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .build()
        val info = ForegroundInfo(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        val mainExecutor = taskExecutor.mainThreadExecutor
        // synchronously resolve it, also posting to main executor a task to stop worker
        // it will happen before a task that will call startWork().
        it.set(info)
        mainExecutor.execute { stopBlock() }
        "TestForegroundBlockingMainWorker getAsync"
    }
}
