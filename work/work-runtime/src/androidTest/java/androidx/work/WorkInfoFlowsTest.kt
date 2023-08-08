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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ExistingWorkPolicy.APPEND
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.testutils.launchTester
import androidx.work.worker.LatchWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class WorkInfoFlowsTest {
    val workerFactory = TrackingWorkerFactory()
    val configuration = Configuration.Builder().setWorkerFactory(workerFactory)
        .setTaskExecutor(Executors.newSingleThreadExecutor())
        .build()
    val env = TestEnv(configuration)
    val fakeChargingTracker = TestConstraintTracker(false, env.context, env.taskExecutor)
    val trackers = Trackers(
        context = env.context,
        taskExecutor = env.taskExecutor,
        batteryChargingTracker = fakeChargingTracker
    )
    val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    val unrelatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
        .setInitialDelay(1, TimeUnit.DAYS)
        .build()

    @Test
    fun flowById() = runBlocking {
        val request = OneTimeWorkRequest.Builder(LatchWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        val tester = launchTester(workManager.getWorkInfoByIdFlow(request.id))
        assertThat(tester.awaitNext()).isNull()
        workManager.enqueue(unrelatedRequest)
        workManager.enqueue(request)
        assertThat(tester.awaitNext().state).isEqualTo(WorkInfo.State.ENQUEUED)
        fakeChargingTracker.constraintState = true
        assertThat(tester.awaitNext().state).isEqualTo(WorkInfo.State.RUNNING)
        val worker = workerFactory.awaitWorker(request.id) as LatchWorker
        worker.mLatch.countDown()
        assertThat(tester.awaitNext().state).isEqualTo(WorkInfo.State.SUCCEEDED)
    }

    @Test
    fun flowByName() = runBlocking<Unit> {
        val request1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        val request2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        val tester = launchTester(workManager.getWorkInfosForUniqueWorkFlow("name"))
        assertThat(tester.awaitNext()).isEmpty()
        workManager.enqueue(unrelatedRequest)
        workManager.enqueueUniqueWork("name", KEEP, request1)
        val firstList = tester.awaitNext()
        assertThat(firstList.size).isEqualTo(1)
        assertThat(firstList.first().id).isEqualTo(request1.id)
        workManager.enqueueUniqueWork("name", APPEND, request2)
        val secondList = tester.awaitNext()
        assertThat(secondList.size).isEqualTo(2)
        assertThat(secondList.map { it.id }).containsExactly(request1.id, request2.id)
    }

    @Test
    fun flowByQuery() = runBlocking<Unit> {
        val request1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        val request2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        val query = WorkQuery.fromIds(request1.id, request2.id)
        val tester = launchTester(workManager.getWorkInfosFlow(query))
        assertThat(tester.awaitNext()).isEmpty()
        workManager.enqueue(unrelatedRequest)
        workManager.enqueue(request1)
        val firstList = tester.awaitNext()
        assertThat(firstList.size).isEqualTo(1)
        assertThat(firstList.first().id).isEqualTo(request1.id)
        workManager.enqueue(request2)
        val secondList = tester.awaitNext()
        assertThat(secondList.size).isEqualTo(2)
        assertThat(secondList.map { it.id }).containsExactly(request1.id, request2.id)
    }

    @Test
    fun flowByTag() = runBlocking<Unit> {
        val request1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .addTag("tag")
            .build()
        val request2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .addTag("tag")
            .build()
        val tester = launchTester(workManager.getWorkInfosByTagFlow("tag"))

        assertThat(tester.awaitNext()).isEmpty()
        workManager.enqueue(unrelatedRequest)
        workManager.enqueue(request1)
        val firstList = tester.awaitNext()
        assertThat(firstList.size).isEqualTo(1)
        assertThat(firstList.first().id).isEqualTo(request1.id)
        workManager.enqueue(unrelatedRequest)
        workManager.enqueue(request2)
        val secondList = tester.awaitNext()
        assertThat(secondList.size).isEqualTo(2)
        assertThat(secondList.map { it.id }).containsExactly(request1.id, request2.id)
    }
}
