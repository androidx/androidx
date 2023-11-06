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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.Success
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.TestEnv
import androidx.work.worker.CompletableWorker
import com.google.common.truth.Truth.assertThat
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
    val configuration = Configuration.Builder().setWorkerFactory(factory).build()
    val testEnv = TestEnv(configuration)

    @Test
    fun testWorkerWrapper_handlesWorkSpecDeletion() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(CompletableWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        testEnv.taskExecutor.serialTaskExecutor.execute(workerWrapper)
        val completableWorker = factory.await(workRequest.id) as CompletableWorker
        testEnv.db.workSpecDao().delete(workRequest.stringId)
        completableWorker.result.complete(Success())
        assertThat(workerWrapper.future.await()).isFalse()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isNull()
    }

    @Test
    fun testRunning() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(DoWorkAwareWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        testEnv.taskExecutor.serialTaskExecutor.execute(workerWrapper)
        val worker = factory.await(workRequest.id) as DoWorkAwareWorker
        worker.doWork.await()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isEqualTo(RUNNING)
        worker.result.complete(Success())
        assertThat(workerWrapper.future.await()).isFalse()
    }

    @Test
    fun testInterruptionRunning() = runBlocking {
        val workRequest = OneTimeWorkRequest.from(DoWorkAwareWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        testEnv.taskExecutor.serialTaskExecutor.execute(workerWrapper)
        val worker = factory.await(workRequest.id) as DoWorkAwareWorker
        worker.doWork.await()
        workerWrapper.interrupt(0)
        assertThat(workerWrapper.future.await()).isTrue()
        assertThat(testEnv.db.workSpecDao().getState(workRequest.stringId)).isEqualTo(ENQUEUED)
    }

    @Test
    fun testInterruptionPreStartWork() = runBlocking<Unit> {
        val workRequest = OneTimeWorkRequest.from(CompletableWorker::class.java)
        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        // gonna block main thread, so startWork() can't be called.
        val mainThreadBlocker = CountDownLatch(1)
        testEnv.taskExecutor.mainThreadExecutor.execute {
            mainThreadBlocker.await()
            workerWrapper.interrupt(0)
        }
        testEnv.taskExecutor.serialTaskExecutor.execute(workerWrapper)
        factory.await(workRequest.id)
        // worker is created, but can't start work because main thread is blocked
        // this call will unblock main thread, but interrupt worker
        mainThreadBlocker.countDown()
        assertThat(workerWrapper.future.await()).isTrue()
        // tricky moment, currently due to the race Worker can go through
        // running state. Exact order would be:
        // - WorkerWrapper reaches trySetRunning, but doesn't enter it
        // - WorkerWrapper.interrupt() happens and resolves the future (ENQUEUED state)
        // - WorkerWrapper enters trySetRunning and sets the state RUNNING
        // - WorkerWrapper enters tryCheckForInterruptionAndResolve again and
        //   set the state back to ENQUEUE

        testEnv.db.workSpecDao().getWorkStatusPojoFlowDataForIds(listOf(workRequest.stringId))
            .map { it.first() }.first { it.state == ENQUEUED }
    }

    @Test
    fun testWorker_getsRunAttemptCount() = runBlocking<Unit> {
        val workRequest = OneTimeWorkRequest.Builder(CompletableWorker::class.java)
            .setInitialRunAttemptCount(10).build()

        testEnv.db.workSpecDao().insertWorkSpec(workRequest.workSpec)
        val workerWrapper = WorkerWrapper(workRequest.workSpec)
        testEnv.taskExecutor.serialTaskExecutor.execute(workerWrapper)
        val worker = factory.await(workRequest.id) as CompletableWorker
        assertThat(worker.runAttemptCount).isEqualTo(10)
        worker.result.complete(Success())
    }

    fun WorkerWrapper(spec: WorkSpec) = WorkerWrapper.Builder(
        testEnv.context, configuration, testEnv.taskExecutor,
        NoOpForegroundProcessor, testEnv.db, spec, emptyList()
    ).build()
}

class DoWorkAwareWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    val doWork = CompletableDeferred<Unit>()
    val result = CompletableDeferred<Result>()
    override suspend fun doWork(): Result {
        doWork.complete(Unit)
        return result.await()
    }
}
