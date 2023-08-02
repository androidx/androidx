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

package androidx.work

import android.app.Notification
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.futures.SettableFuture
import com.google.common.truth.Truth.assertThat
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(JUnit4::class)
class RxForegroundInfoTest {
    @Test
    fun testForegroundInfo() {
        val context = mock(Context::class.java)
        val foregroundInfo = WorkerGetForeground(context, createWorkerParams())
            .foregroundInfoAsync.get()
        assertThat(foregroundInfo).isEqualTo(testForegroundInfo)
    }

    @Test
    fun testSetForegroundInfo() {
        val context = mock(Context::class.java)
        var actualForegroundInfo: ForegroundInfo? = null
        val foregroundUpdater = ForegroundUpdater { _, _, foregroundInfo ->
            actualForegroundInfo = foregroundInfo
            val future = SettableFuture.create<Void>()
            future.set(null)
            future
        }
        val worker = WorkerSetForeground(context, createWorkerParams(
            foregroundUpdater = foregroundUpdater
        ))
        val result = worker.startWork().get()
        assertThat(result).isEqualTo(Result.success())
        assertThat(actualForegroundInfo).isEqualTo(testForegroundInfo)
    }
}

private val testForegroundInfo = ForegroundInfo(10, mock(Notification::class.java))

private class WorkerGetForeground(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        throw UnsupportedOperationException()
    }

    override fun getForegroundInfo(): Single<ForegroundInfo> = Single.just(testForegroundInfo)
}

private class WorkerSetForeground(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        setForeground(testForegroundInfo).blockingAwait()
        return Single.just(Result.success())
    }
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
    0,
    executor,
    RxWorkerTest.InstantWorkTaskExecutor(),
    WorkerFactory.getDefaultWorkerFactory(),
    progressUpdater,
    foregroundUpdater
)
