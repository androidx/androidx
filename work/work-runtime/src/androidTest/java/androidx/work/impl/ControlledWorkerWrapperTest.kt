/*
 * Copyright 2021 The Android Open Source Project
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
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ControlledWorkerWrapperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val taskExecutor = ManualTaskExecutor()
    private val backgroundExecutor = ManualExecutor()
    private val workDatabase = WorkDatabase.create(
        context, taskExecutor.serialTaskExecutor, SystemClock(), true
    )
    private val foregroundInfoFuture = SettableFuture.create<ForegroundInfo>()
    private val resultFuture = SettableFuture.create<Result>().also { it.set(Result.success()) }

    @Test
    fun testInterruptionsBefore() {
        val work = OneTimeWorkRequest.Builder(TestWrapperWorker::class.java).build()
        workDatabase.workSpecDao().insertWorkSpec(work.workSpec)
        lateinit var worker: TestWrapperWorker
        val workerWrapper = workerWrapper(work.stringId) { worker = it }
        workerWrapper.run()

        while (taskExecutor.serialTaskExecutor.hasPendingTask() ||
            backgroundExecutor.hasPendingTask()
        ) {
            taskExecutor.serialTaskExecutor.drain()
            backgroundExecutor.drain()
        }
        workerWrapper.interrupt(0)
        drainAll()
        assertThat(workerWrapper.future.isDone).isTrue()
        assertThat(worker.startWorkWasCalled).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R) // getForegroundInfoAsync isn't called on S
    fun testInterruptionsBetweenGetForegroundInfoAsyncAndStartWork() {
        val work = OneTimeWorkRequest.Builder(TestWrapperWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()
        workDatabase.workSpecDao().insertWorkSpec(work.workSpec)
        lateinit var worker: TestWrapperWorker
        val workerWrapper = workerWrapper(work.stringId) { worker = it }
        workerWrapper.run()
        drainAll()
        assertThat(worker.getForegroundInfoAsyncWasCalled).isTrue()
        assertThat(worker.startWorkWasCalled).isFalse()
        foregroundInfoFuture.set(
            ForegroundInfo(
                0,
                NotificationCompat.Builder(context, "test").build()
            )
        )
        workerWrapper.interrupt(0)
        drainAll()
        assertThat(worker.startWorkWasCalled).isFalse()
        assertThat(workerWrapper.future.isDone).isTrue()
    }

    private fun drainAll() {
        while (
            taskExecutor.serialTaskExecutor.hasPendingTask() ||
            backgroundExecutor.hasPendingTask() ||
            taskExecutor.mainExecutor.hasPendingTask()
        ) {
            taskExecutor.serialTaskExecutor.drain()
            backgroundExecutor.drain()
            taskExecutor.mainExecutor.drain()
        }
    }

    private fun workerWrapper(
        id: String,
        workerInterceptor: (TestWrapperWorker) -> Unit
    ): WorkerWrapper {
        val config = Configuration.Builder()
            .setExecutor(backgroundExecutor)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    val worker = TestWrapperWorker(
                        appContext, workerParameters,
                        foregroundInfoFuture, resultFuture
                    )
                    workerInterceptor(worker)
                    return worker
                }
            }).build()
        return WorkerWrapper.Builder(
            context,
            config,
            taskExecutor,
            NoOpForegroundProcessor,
            workDatabase,
            workDatabase.workSpecDao().getWorkSpec(id)!!,
            emptyList()
        ).build()
    }
}

internal class TestWrapperWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val foregroundInfoFuture: ListenableFuture<ForegroundInfo>,
    private val resultFuture: ListenableFuture<Result>
) : ListenableWorker(
    appContext, workerParams
) {
    var getForegroundInfoAsyncWasCalled = false
    var startWorkWasCalled = false

    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        getForegroundInfoAsyncWasCalled = true
        return foregroundInfoFuture
    }

    override fun startWork(): ListenableFuture<Result> {
        startWorkWasCalled = true
        return resultFuture
    }
}

object NoOpForegroundProcessor : ForegroundProcessor {
    override fun startForeground(workSpecId: String, foregroundInfo: ForegroundInfo) {
    }
}

class ManualExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>(10)

    override fun execute(runnable: Runnable) {
        tasks.add(runnable)
    }

    fun drain() {
        while (tasks.isNotEmpty()) {
            val head = tasks.removeFirst()
            head.run()
        }
    }

    fun hasPendingTask() = tasks.isNotEmpty()
}

class ManualTaskExecutor : TaskExecutor {
    val mainExecutor = ManualExecutor()
    val serialTaskExecutor = ManualExecutor()
    private val serialBackgroundExecutor = SerialExecutorImpl(serialTaskExecutor)

    override fun getMainThreadExecutor() = mainExecutor

    override fun getSerialTaskExecutor(): SerialExecutorImpl = serialBackgroundExecutor
}
