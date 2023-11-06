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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.ListenableWorker.Result.Success
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.await
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.workDataOf
import androidx.work.worker.CompletableWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class WorkContinuationImplTestKt {
    val workerFactory = TrackingWorkerFactory()
    val configuration =
        Configuration.Builder().setWorkerFactory(workerFactory)
            .setTaskExecutor(Executors.newSingleThreadExecutor()).build()
    val env = TestEnv(configuration)
    val taskExecutor = env.taskExecutor
    val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
    val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    fun testContinuation_joinPassesAllOutput() = runBlocking<Unit> {
        val intTag = "myint"
        val stringTag = "mystring"
        val firstWork = OneTimeWorkRequest.from(CompletableWorker::class.java)
        val secondWork = OneTimeWorkRequest.from(CompletableWorker::class.java)
        val firstContinuation = workManager.beginWith(firstWork)
        val secondContinuation = workManager.beginWith(secondWork)
        val combined = WorkContinuation.combine(listOf(firstContinuation, secondContinuation))
        combined.enqueue().await()
        val thirdId = combined.workInfos.await().map { it.id }
            .first { it != firstWork.id && it != secondWork.id }
        (workerFactory.await(firstWork.id) as CompletableWorker).result
            .complete(Success(workDataOf(intTag to 1, stringTag to "hello")))
        (workerFactory.await(secondWork.id) as CompletableWorker).result
            .complete(Success(workDataOf(intTag to 3)))
        val info = workManager.getWorkInfoByIdFlow(thirdId)
            .first { it.state == WorkInfo.State.SUCCEEDED }
        assertThat(info.outputData.size()).isEqualTo(2)
        assertThat(info.outputData.getStringArray(stringTag)).isEqualTo(arrayOf("hello"))
        val intArray = info.outputData.getIntArray(intTag)!!.sortedArray()
        assertThat(intArray).isEqualTo(intArrayOf(1, 3))
    }
}
