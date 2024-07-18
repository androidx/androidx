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

package androidx.work

import android.app.job.JobParameters.STOP_REASON_TIMEOUT
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.testutils.launchTester
import androidx.work.worker.CompletableWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 31)
class GreedySchedulerTimeoutTest {
    val workerFactory = TrackingWorkerFactory()
    private val runnableScheduler = ManualDefaultRunnableScheduler()
    val configuration = Configuration.Builder()
        .setRunnableScheduler(runnableScheduler)
        .setWorkerFactory(workerFactory)
        .setTaskExecutor(Executors.newSingleThreadExecutor())
        .build()
    val env = TestEnv(configuration)
    val trackers = Trackers(
        context = env.context,
        taskExecutor = env.taskExecutor,
    )
    val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    fun testWorkerTimesout() = runBlocking {
        val request = OneTimeWorkRequest.Builder(CompletableWorker::class.java).build()
        workManager.enqueue(request).await()
        val worker = workerFactory.await(request.id)
        val tester = launchTester(workManager.getWorkInfoByIdFlow(request.id))
        val runningWorkInfo = tester.awaitNext()
        assertThat(runningWorkInfo.state).isEqualTo(WorkInfo.State.RUNNING)
        runnableScheduler.executedFutureRunnables(TimeUnit.HOURS.toMillis(2))
        val stopInfo = tester.awaitNext()
        assertThat(stopInfo.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(worker.stopReason).isEqualTo(STOP_REASON_TIMEOUT)
    }

    private class ManualDefaultRunnableScheduler : RunnableScheduler {
        private val map = mutableMapOf<Runnable, Long>()
        private val lock = Any()

        override fun scheduleWithDelay(delayInMillis: Long, runnable: Runnable) {
            synchronized(lock) { map[runnable] = System.currentTimeMillis() + delayInMillis }
        }

        override fun cancel(runnable: Runnable) {
            synchronized(lock) { map.remove(runnable) }
        }

        fun executedFutureRunnables(duration: Long) {
            val current = System.currentTimeMillis()
            val runnables = mutableListOf<Runnable>()
            synchronized(lock) {
                map.filter { current + duration >= it.value }.keys.toCollection(runnables)
            }
            runnables.forEach { it.run() }
        }
    }
}
