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
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ConfigurationExecutorsTest {
    val workerFactory = TrackingWorkerFactory()
    val executor =
        Executors.newSingleThreadExecutor {
            Thread(it).also { thread -> thread.name = threadTestName }
        }

    @Test
    fun testSetExecutor() = runBlocking {
        val configuration =
            Configuration.Builder().setWorkerFactory(workerFactory).setExecutor(executor).build()
        val env = TestEnv(configuration)
        val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
        val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)
        WorkManagerImpl.setDelegate(workManager)

        val blockingRequest = OneTimeWorkRequest.from(ThreadNameWorker::class.java)
        workManager.enqueue(blockingRequest)
        val blockingWorker = workerFactory.await(blockingRequest.id) as ThreadNameWorker
        assertThat(blockingWorker.threadNameDeferred.await()).isEqualTo(threadTestName)

        val coroutineRequest = OneTimeWorkRequest.from(CoroutineDispatcherWorker::class.java)
        workManager.enqueue(coroutineRequest)
        val coroutineWorker = workerFactory.await(coroutineRequest.id) as CoroutineDispatcherWorker

        val coroutineDispatcher = coroutineWorker.coroutineDispatcherDeferred.await()
        assertThat(coroutineDispatcher?.asExecutor()).isEqualTo(executor)
    }

    @Test
    fun testSetWorkerCoroutineDispatcher() = runBlocking {
        val dispatcher = executor.asCoroutineDispatcher()
        val configuration =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setWorkerCoroutineContext(dispatcher)
                .build()
        val env = TestEnv(configuration)
        val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
        val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)
        WorkManagerImpl.setDelegate(workManager)

        val blockingRequest = OneTimeWorkRequest.from(ThreadNameWorker::class.java)
        workManager.enqueue(blockingRequest)
        val blockingWorker = workerFactory.await(blockingRequest.id) as ThreadNameWorker
        assertThat(blockingWorker.threadNameDeferred.await()).isEqualTo(threadTestName)

        val coroutineRequest = OneTimeWorkRequest.from(CoroutineDispatcherWorker::class.java)
        workManager.enqueue(coroutineRequest)
        val coroutineWorker = workerFactory.await(coroutineRequest.id) as CoroutineDispatcherWorker

        val coroutineDispatcher = coroutineWorker.coroutineDispatcherDeferred.await()
        assertThat(coroutineDispatcher).isEqualTo(dispatcher)
    }

    @Test
    fun testSetBoth() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val configuration =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setExecutor(executor)
                .setWorkerCoroutineContext(dispatcher)
                .build()
        val env = TestEnv(configuration)
        val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
        val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)
        WorkManagerImpl.setDelegate(workManager)

        val blockingRequest = OneTimeWorkRequest.from(ThreadNameWorker::class.java)
        workManager.enqueue(blockingRequest)
        val blockingWorker = workerFactory.await(blockingRequest.id) as ThreadNameWorker
        assertThat(blockingWorker.threadNameDeferred.await()).isEqualTo(threadTestName)

        val coroutineRequest = OneTimeWorkRequest.from(CoroutineDispatcherWorker::class.java)
        workManager.enqueue(coroutineRequest)
        val coroutineWorker = workerFactory.await(coroutineRequest.id) as CoroutineDispatcherWorker

        val coroutineDispatcher = coroutineWorker.coroutineDispatcherDeferred.await()
        assertThat(coroutineDispatcher).isEqualTo(dispatcher)
    }

    @Test
    fun testSetNeither() = runBlocking {
        val configuration = Configuration.Builder().setWorkerFactory(workerFactory).build()
        val env = TestEnv(configuration)
        val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
        val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)
        WorkManagerImpl.setDelegate(workManager)

        val coroutineRequest = OneTimeWorkRequest.from(CoroutineDispatcherWorker::class.java)
        workManager.enqueue(coroutineRequest)
        val coroutineWorker = workerFactory.await(coroutineRequest.id) as CoroutineDispatcherWorker

        val coroutineDispatcher = coroutineWorker.coroutineDispatcherDeferred.await()
        assertThat(coroutineDispatcher).isEqualTo(Dispatchers.Default)
    }

    @Test
    fun testSetCoroutineContextOverride() = runBlocking {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val configuration =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setExecutor(executor)
                .setWorkerCoroutineContext(dispatcher)
                .build()
        val env = TestEnv(configuration)
        val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
        val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)
        WorkManagerImpl.setDelegate(workManager)

        val coroutineRequest = OneTimeWorkRequest.from(CoroutineContextOverridingWorker::class.java)
        workManager.enqueue(coroutineRequest)
        val coroutineWorker =
            workerFactory.await(coroutineRequest.id) as CoroutineContextOverridingWorker

        val coroutineDispatcher = coroutineWorker.coroutineDispatcherDeferred.await()
        @Suppress("DEPRECATION")
        assertThat(coroutineDispatcher).isEqualTo(coroutineWorker.coroutineContext)
    }
}

private const val threadTestName = "configuration_test"

class ThreadNameWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val threadNameDeferred = CompletableDeferred<String>()

    override fun doWork(): Result {
        threadNameDeferred.complete(Thread.currentThread().name)
        return Result.success()
    }
}

class CoroutineDispatcherWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    val coroutineDispatcherDeferred = CompletableDeferred<CoroutineDispatcher?>()

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun doWork(): Result {
        coroutineDispatcherDeferred.complete(currentCoroutineContext()[CoroutineDispatcher])
        return Result.success()
    }
}

class CoroutineContextOverridingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Suppress("OVERRIDE_DEPRECATION")
    override val coroutineContext: CoroutineDispatcher
        get() = dispatcher

    val coroutineDispatcherDeferred = CompletableDeferred<CoroutineDispatcher?>()

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun doWork(): Result {
        coroutineDispatcherDeferred.complete(currentCoroutineContext()[CoroutineDispatcher])
        return Result.success()
    }
}
