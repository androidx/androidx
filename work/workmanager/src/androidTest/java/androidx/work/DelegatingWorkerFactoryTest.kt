/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.worker.FailureWorker
import androidx.work.worker.TestWorker
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@SmallTest
class DelegatingWorkerFactoryTest : DatabaseTest() {

    private lateinit var context: Context
    private lateinit var factory: DelegatingWorkerFactory
    private lateinit var progressUpdater: ProgressUpdater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        factory = DelegatingWorkerFactory()
        progressUpdater = mock(ProgressUpdater::class.java)
    }

    @Test
    fun testDelegate() {
        factory.addFactory(NoOpFactory())
        factory.addFactory(FailedWorkerFactory())

        val request = OneTimeWorkRequest.from(TestWorker::class.java)
        insertWork(request)
        val params: WorkerParameters = newWorkerParams(factory, progressUpdater)
        val worker = factory.createWorkerWithDefaultFallback(
            context,
            TestWorker::class.java.name,
            params
        )

        assertThat(worker, notNullValue())
        assertThat(worker, instanceOf(FailureWorker::class.java))
    }

    @Test
    fun testDelegate_defaultFactory() {
        factory = DelegatingWorkerFactory()
        val request = OneTimeWorkRequest.from(TestWorker::class.java)
        insertWork(request)
        val params: WorkerParameters = newWorkerParams(factory, progressUpdater)
        val worker = factory.createWorkerWithDefaultFallback(
            context,
            TestWorker::class.java.name,
            params
        )

        assertThat(worker, notNullValue())
        assertThat(worker, instanceOf(TestWorker::class.java))
    }

    private fun newWorkerParams(factory: WorkerFactory, updater: ProgressUpdater) =
        WorkerParameters(
            UUID.randomUUID(),
            Data.EMPTY,
            listOf<String>(),
            WorkerParameters.RuntimeExtras(),
            1,
            SynchronousExecutor(),
            WorkManagerTaskExecutor(SynchronousExecutor()),
            factory,
            updater
        )
}

class NoOpFactory : WorkerFactory() {
    override fun createWorker(
        context: Context,
        workerClass: String,
        parameters: WorkerParameters
    ): ListenableWorker? {
        return null
    }
}

class FailedWorkerFactory : WorkerFactory() {
    override fun createWorker(
        context: Context,
        workerClass: String,
        parameters: WorkerParameters
    ): ListenableWorker? {
        return FailureWorker(context, parameters)
    }
}
