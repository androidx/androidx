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

package androidx.work.rxjava3

import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker.Result
import androidx.work.ProgressUpdater
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.futures.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(JUnit4::class)
class SetCompletableProgressTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock(Context::class.java)
    }

    @Test
    fun testSetProgressCompletable() {
        val progressUpdater = ProgressUpdater { _, _, _ ->
            val future = SettableFuture.create<Void>()
            future.set(null)
            future
        }
        val worker =
            TestRxWorker(context, createWorkerParams(progressUpdater = progressUpdater))
        val result = worker.startWork().get()
        assertEquals(result, Result.success())
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
        RxWorkerTest.InstantWorkTaskExecutor(),
        WorkerFactory.getDefaultWorkerFactory(),
        progressUpdater,
        foregroundUpdater
    )
}
