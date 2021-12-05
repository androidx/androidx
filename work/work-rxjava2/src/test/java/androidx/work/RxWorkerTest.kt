/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class RxWorkerTest {
    private val syncExecutor = SynchronousExecutor()
    private val result = ListenableWorker.Result.success()

    @Test
    fun simple() {
        val worker = Single.just(result).toWorker()
        assertThat(worker.startWork().get(), `is`(result))
    }

    @Test
    fun cancelForwarding() {
        val latch = CountDownLatch(1)
        val worker = Single
            .never<ListenableWorker.Result>()
            .doOnDispose {
                latch.countDown()
            }.toWorker(createWorkerParams(syncExecutor))
        val future = worker.startWork()
        future.cancel(false)
        if (!latch.await(1, TimeUnit.MINUTES)) {
            throw AssertionError("should've been disposed")
        }
    }

    @Test
    fun failedWork() {
        val error: Throwable = RuntimeException("a random error")
        val worker = Single
            .error<ListenableWorker.Result>(error)
            .toWorker(createWorkerParams(syncExecutor))
        val future = worker.startWork()
        try {
            future.get()
            Assert.fail("should've throw an exception")
        } catch (t: Throwable) {
            assertThat(t.cause, `is`(error))
        }
    }

    @Test
    fun verifyCorrectDefaultScheduler() {
        var executorDidRun = false
        var runnerDidRun = false
        val runner = Runnable {
            runnerDidRun = true
        }
        val executor = Executor {
            executorDidRun = true
            it.run()
        }

        val rxWorker = Single.just(result).toWorker(createWorkerParams(executor))
        rxWorker.backgroundScheduler.scheduleDirect(runner)
        assertThat(runnerDidRun, `is`(true))
        assertThat(executorDidRun, `is`(true))
    }

    @Test
    fun customScheduler() {
        var executorDidRun = false
        val executor = Executor {
            executorDidRun = true
            it.run()
        }
        var testSchedulerDidRun = false
        val testScheduler = Schedulers.from {
            testSchedulerDidRun = true
            it.run()
        }
        val params = createWorkerParams(executor)
        val worker = object : RxWorker(mock(Context::class.java), params) {
            override fun createWork() = Single.just(result)
            override fun getBackgroundScheduler() = testScheduler
        }
        assertThat(worker.startWork().get(), `is`(result))
        assertThat(executorDidRun, `is`(false))
        assertThat(testSchedulerDidRun, `is`(true))
    }

    private fun createWorkerParams(
        executor: Executor = SynchronousExecutor(),
        progressUpdater: ProgressUpdater = mock(ProgressUpdater::class.java),
        foregroundUpdater: ForegroundUpdater = mock(ForegroundUpdater::class.java)
    ) = WorkerParameters(
        UUID.randomUUID(),
        Data.EMPTY,
        emptyList(),
        WorkerParameters.RuntimeExtras(),
        1,
        executor,
        InstantWorkTaskExecutor(),
        WorkerFactory.getDefaultWorkerFactory(),
        progressUpdater,
        foregroundUpdater
    )

    private fun Single<ListenableWorker.Result>.toWorker(
        params: WorkerParameters = createWorkerParams()
    ): RxWorker {
        return object : RxWorker(mock(Context::class.java), params) {
            override fun createWork() = this@toWorker
        }
    }

    class InstantWorkTaskExecutor : TaskExecutor {

        private val mSynchronousExecutor = SynchronousExecutor()
        private val mSerialExecutor = SerialExecutor(mSynchronousExecutor)

        override fun postToMainThread(runnable: Runnable) {
            runnable.run()
        }

        override fun getMainThreadExecutor(): Executor {
            return mSynchronousExecutor
        }

        override fun getSerialTaskExecutor(): SerialExecutor {
            return mSerialExecutor
        }
    }
}
