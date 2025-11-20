/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo.State
import androidx.work.WorkManager.UpdateResult.APPLIED_FOR_NEXT_RUN
import androidx.work.WorkManager.UpdateResult.APPLIED_IMMEDIATELY
import androidx.work.WorkManager.UpdateResult.NOT_APPLIED
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestConstraintTracker
import androidx.work.impl.testutils.TestOverrideClock
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.impl.workers.ARGUMENT_CLASS_NAME
import androidx.work.impl.workers.ConstraintTrackingWorker
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.testutils.awaitWorkerEnqueued
import androidx.work.testutils.awaitWorkerFinished
import androidx.work.worker.CompletableWorker
import androidx.work.worker.RetryWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkUpdateTest {
    val workerFactory = TrackingWorkerFactory()
    val testClock = TestOverrideClock()
    val configuration =
        Configuration.Builder()
            .setClock(testClock)
            .setWorkerFactory(workerFactory)
            .setTaskExecutor(Executors.newSingleThreadExecutor())
            .build()
    val env = TestEnv(configuration)
    val taskExecutor = env.taskExecutor
    val fakeChargingTracker = TestConstraintTracker(false, env.context, env.taskExecutor)
    val trackers =
        Trackers(
            context = env.context,
            taskExecutor = env.taskExecutor,
            batteryChargingTracker = fakeChargingTracker,
        )
    val greedyScheduler = GreedyScheduler(env, trackers)
    val workManager = WorkManager(env, listOf(greedyScheduler), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    @MediumTest
    fun constraintsUpdate() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val oneTimeWorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(oneTimeWorkRequest).result.await()
        val requestId = oneTimeWorkRequest.id

        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class).setId(requestId).build()

        val operation = workManager.updateWork(updatedRequest)
        assertThat(operation.await()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(requestId)
    }

    @Test
    @MediumTest
    fun updateRunningOneTimeWork() = runTest {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(CompletableWorker::class).build()
        workManager.enqueue(oneTimeWorkRequest).result.await()
        val worker = workerFactory.await(oneTimeWorkRequest.id) as CompletableWorker
        // requiresCharging constraint is faked, so it will never be satisfied
        val updatedRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setId(oneTimeWorkRequest.id)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(APPLIED_FOR_NEXT_RUN)
        worker.result.complete(Result.success())
        workManager.awaitSuccess(oneTimeWorkRequest.id)
    }

    @Test
    @MediumTest
    fun failFinished() = runTest {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.awaitSuccess(oneTimeWorkRequest.id)
        val updatedRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setId(oneTimeWorkRequest.id)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(NOT_APPLIED)
    }

    @Test
    @MediumTest
    fun failWorkDoesntExit() = runTest {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.awaitSuccess(oneTimeWorkRequest.id)
        val updatedRequest =
            OneTimeWorkRequest.Builder(TestWorker::class).setId(UUID.randomUUID()).build()
        try {
            workManager.updateWork(updatedRequest).await()
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    @MediumTest
    fun updateTags() = runTest {
        val oneTimeWorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setInitialDelay(10, DAYS)
                .addTag("previous")
                .build()
        workManager.enqueue(oneTimeWorkRequest).result.await()

        val updatedWorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setInitialDelay(10, DAYS)
                .setId(oneTimeWorkRequest.id)
                .addTag("test")
                .build()
        assertThat(workManager.updateWork(updatedWorkRequest).await())
            .isEqualTo(APPLIED_IMMEDIATELY)

        val info = workManager.getWorkInfoByIdFlow(oneTimeWorkRequest.id).first()!!
        assertThat(info.tags).contains("test")
        assertThat(info.tags).doesNotContain("previous")
    }

    // this test verifies scenario when tags for the worker
    // is read at the same moment when Processor considers worker running,
    // which is different from moment when WorkDatabase is updated.
    // Otherwise we can run older version of the worker with new tags.
    // This is the reason why it has special behavior when all execution
    // on serialTaskExecutor is blocked.
    @Test
    @MediumTest
    fun updateTagsWhileRunning() = runTest {
        val request =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .addTag("original")
                .build()
        workManager.enqueue(request).result.await()
        val serialExecutorBlocker = CountDownLatch(1)
        // stop any execution on serialTaskExecutor
        taskExecutor.serialTaskExecutor.execute { serialExecutorBlocker.await() }
        // will add startWork task to the serialTaskExecutor queue
        greedyScheduler.onConstraintsStateChanged(request.workSpec, ConstraintsMet)
        val updatedRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .setId(request.id)
                .addTag("updated")
                .build()
        // will add update task to the serialTaskExecutor queue
        val updateResult = workManager.updateWork(updatedRequest)
        serialExecutorBlocker.countDown()
        val worker = workerFactory.await(request.id)
        assertThat(worker.tags).contains("original")
        assertThat(worker.tags).doesNotContain("updated")
        assertThat(updateResult.await()).isEqualTo(APPLIED_FOR_NEXT_RUN)
    }

    @Test
    @MediumTest
    fun updateWorkerClass() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val oneTimeWorkRequest =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(oneTimeWorkRequest).result.await()
        val requestId = oneTimeWorkRequest.id

        val updatedRequest =
            OneTimeWorkRequest.Builder(CompletableWorker::class).setId(requestId).build()

        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(APPLIED_IMMEDIATELY)
        // verifying that new worker has been started
        val worker = workerFactory.await(oneTimeWorkRequest.id) as CompletableWorker
        worker.result.complete(Result.success())
        workManager.awaitSuccess(requestId)
    }

    @Test
    @MediumTest
    fun progressReset() = runTest {
        // requiresCharging constraint is faked, so it will be controlled in the test
        val request =
            OneTimeWorkRequest.Builder(ProgressWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(request).result.await()
        fakeChargingTracker.constraintState = true
        workManager.getWorkInfoByIdFlow(request.id).filterNotNull().first {
            it.state == State.RUNNING && it.progress.size() != 0
        }
        // will trigger worker to be stopped
        fakeChargingTracker.state = false
        val info = workManager.awaitWorkerEnqueued(request.id)

        assertThat(info.progress).isEqualTo(TEST_DATA)

        val updatedRequest =
            OneTimeWorkRequest.Builder(ProgressWorker::class)
                .setId(request.id)
                .addTag("bla")
                .build()
        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(APPLIED_IMMEDIATELY)
        val updatedInfo = workManager.getWorkInfoByIdFlow(request.id).first()!!
        assertThat(updatedInfo.tags).contains("bla")
        assertThat(updatedInfo.progress).isEqualTo(Data.EMPTY)
    }

    @Test
    @MediumTest
    fun continuationLeafUpdate() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.beginWith(step1).then(step2).enqueue().result.await()
        val updatedStep2 =
            OneTimeWorkRequest.Builder(TestWorker::class).setId(step2.id).addTag("updated").build()
        assertThat(workManager.updateWork(updatedStep2).await()).isEqualTo(APPLIED_IMMEDIATELY)
        val workInfo = workManager.getWorkInfoById(step2.id).await()!!
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        assertThat(workInfo.tags).contains("updated")
    }

    @Test
    @MediumTest
    fun continuationLeafRoot() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.beginWith(step1).then(step2).enqueue().result.await()
        val workInfo = workManager.getWorkInfoById(step2.id).await()!!
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        val updatedStep1 = OneTimeWorkRequest.Builder(TestWorker::class).setId(step1.id).build()
        assertThat(workManager.updateWork(updatedStep1).await()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(step2.id)
    }

    @Test
    @MediumTest
    fun chainsViaExistingPolicyLeafUpdate() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step1)
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step2)
        val updatedStep2 =
            OneTimeWorkRequest.Builder(TestWorker::class).setId(step2.id).addTag("updated").build()
        assertThat(workManager.updateWork(updatedStep2).await()).isEqualTo(APPLIED_IMMEDIATELY)
        val workInfo = workManager.getWorkInfoById(step2.id).await()!!
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        assertThat(workInfo.tags).contains("updated")
    }

    @Test
    @MediumTest
    fun chainsViaExistingPolicyRootUpdate() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class).build()
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step1)
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step2)
        val workInfo = workManager.getWorkInfoById(step2.id).await()!!
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        val updatedStep1 = OneTimeWorkRequest.Builder(TestWorker::class).setId(step1.id).build()
        assertThat(workManager.updateWork(updatedStep1).await()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(step2.id)
    }

    @Test
    @MediumTest
    fun oneTimeWorkToPeriodic() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val request =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(request).result.await()
        val updatedRequest = PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS).build()
        try {
            workManager.updateWork(updatedRequest).await()
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    @MediumTest
    fun periodicWorkToOneTime() = runTest {
        // requiresCharging constraint is faked, so it will never be satisfied
        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                .setConstraints(Constraints(requiresCharging = true))
                .build()
        workManager.enqueue(request).result.await()
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class).build()
        try {
            workManager.updateWork(updatedRequest).await()
            throw AssertionError()
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    @MediumTest
    fun updateRunningPeriodicWorkRequest() = runTest {
        val request =
            PeriodicWorkRequest.Builder(CompletableWorker::class, 1, DAYS)
                .addTag("original")
                .build()
        workManager.enqueue(request).result.await()
        val updatedRequest =
            PeriodicWorkRequest.Builder(CompletableWorker::class, 1, DAYS)
                .setId(request.id)
                .addTag("updated")
                .build()
        val worker = workerFactory.await(request.id) as CompletableWorker
        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(APPLIED_FOR_NEXT_RUN)
        assertThat(worker.isStopped).isFalse()
        assertThat(worker.tags).contains("original")
        assertThat(worker.tags).doesNotContain("updated")
        worker.result.complete(Result.success())
        workManager.awaitWorkerEnqueued(request.id)
        val newTags = workManager.getWorkInfoById(request.id).await()!!.tags
        assertThat(newTags).contains("updated")
        assertThat(newTags).doesNotContain("original")
    }

    @MediumTest
    @Test
    fun updatePeriodicWorkAfterFirstPeriod() = runTest {
        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS).addTag("original").build()
        workManager.enqueue(request).result.await()
        workerFactory.await(request.id)
        workManager.awaitWorkerEnqueued(request.id)

        val updatedRequest =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                // requiresCharging constraint is faked, so it will never be satisfied
                .setConstraints(Constraints(requiresCharging = true))
                .setId(request.id)
                .addTag("updated")
                .build()

        assertThat(workManager.updateWork(updatedRequest).await()).isEqualTo(APPLIED_IMMEDIATELY)

        val newTags = workManager.getWorkInfoById(request.id).await()!!.tags
        assertThat(newTags).contains("updated")
        assertThat(newTags).doesNotContain("original")
        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        assertThat(workSpec.periodCount).isEqualTo(1)
    }

    @MediumTest
    @Test
    fun updateRetryingOneTimeWork() = runTest {
        val request =
            OneTimeWorkRequest.Builder(RetryWorker::class)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, DAYS)
                .build()
        workManager.enqueue(request)
        val executionDeferred = CompletableDeferred<Unit>()
        env.processor.addExecutionListener { _, _ ->
            env.taskExecutor.serialTaskExecutor.execute { executionDeferred.complete(Unit) }
        }
        // await worker to be created
        val worker1 = workerFactory.await(request.id)
        assertThat(worker1.runAttemptCount).isEqualTo(0)
        workManager.awaitWorkerEnqueued(request.id)
        // rescheduling routine (see Schedulers.registerRescheduling) must not
        // trigger unwanted execution of a worker with the rewinded lastEnqueueTime.
        // To achieve this we add our own ExecutionListener
        executionDeferred.await()
        // rewind time so can updated worker can run
        val spec = workManager.workDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        val delta = spec.calculateNextRunTime() - System.currentTimeMillis()
        assertThat(delta).isGreaterThan(0)
        workManager.workDatabase
            .workSpecDao()
            .setLastEnqueueTime(request.stringId, spec.lastEnqueueTime - delta)
        val updated =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setId(request.id)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, DAYS)
                .build()
        workManager.updateWork(updated).await()
        workManager.awaitSuccess(request.id)
        val worker2 = workerFactory.await(request.id)
        assertThat(worker2.runAttemptCount).isEqualTo(1)
    }

    @MediumTest
    @Test
    fun updateCorrectNextRunTime() = runTest {
        val request =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build()
        val enqueueTime = System.currentTimeMillis()
        workManager.enqueue(request).result.await()
        workManager.workDatabase
            .workSpecDao()
            .setLastEnqueueTime(request.stringId, enqueueTime - TimeUnit.MINUTES.toMillis(5))
        val updated =
            OneTimeWorkRequest.Builder(TestWorker::class)
                .setInitialDelay(20, TimeUnit.MINUTES)
                .setId(request.id)
                .build()
        workManager.updateWork(updated).await()
        val workSpec = workManager.workDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        val delta = workSpec.calculateNextRunTime() - enqueueTime
        // enqueue time isn't very accurate but delta should be about 15 minutes, because
        // enqueueTime was rewound 5 minutes back.
        assertThat(delta).isGreaterThan(TimeUnit.MINUTES.toMillis(14))
        assertThat(delta).isLessThan(TimeUnit.MINUTES.toMillis(16))
    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 25)
    fun testUpdatePeriodicWorker_preservesConstraintTrackingWorker() = runTest {
        val originRequest =
            OneTimeWorkRequest.Builder(TestWorker::class).setInitialDelay(10, HOURS).build()
        workManager.enqueue(originRequest).result.await()
        val updateRequest =
            OneTimeWorkRequest.Builder(RetryWorker::class)
                .setId(originRequest.id)
                .setInitialDelay(10, HOURS)
                .setConstraints(Constraints(requiresBatteryNotLow = true))
                .build()
        workManager.updateWork(updateRequest).await()
        val workSpec = env.db.workSpecDao().getWorkSpec(originRequest.stringId)!!
        assertThat(workSpec.workerClassName).isEqualTo(ConstraintTrackingWorker::class.java.name)
        assertThat(workSpec.input.getString(ARGUMENT_CLASS_NAME))
            .isEqualTo(RetryWorker::class.java.name)
    }

    @Test
    @MediumTest
    fun updateWorkerGeneration() = runTest {
        val oneTimeWorkRequest =
            OneTimeWorkRequest.Builder(WorkerWithParam::class).setInitialDelay(10, DAYS).build()
        workManager.enqueue(oneTimeWorkRequest).result.await()

        val updatedWorkRequest =
            OneTimeWorkRequest.Builder(WorkerWithParam::class).setId(oneTimeWorkRequest.id).build()

        assertThat(workManager.updateWork(updatedWorkRequest).await())
            .isEqualTo(APPLIED_IMMEDIATELY)
        val worker = workerFactory.await(oneTimeWorkRequest.id) as WorkerWithParam
        assertThat(worker.generation).isEqualTo(1)
        val workInfo = workManager.getWorkInfoById(oneTimeWorkRequest.id).await()!!
        assertThat(workInfo.generation).isEqualTo(1)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride() = runTest {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val nextRunTimeMillis = HOURS.toMillis(10)

        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.await()

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .setNextScheduleTimeOverride(nextRunTimeMillis)
                    .build()
            )
            .await()

        val workInfo = workManager.getWorkInfoById(request.id).await()!!
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(nextRunTimeMillis)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride_multipleGenerations() = runTest {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)
        val overrideScheduleTimeMillis2 = HOURS.toMillis(12)

        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.await()

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
                    .build()
            )
            .await()
        val workInfo = workManager.getWorkInfoById(request.id).await()!!
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis)

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .setNextScheduleTimeOverride(overrideScheduleTimeMillis2)
                    .build()
            )
            .await()

        val workInfo2 = workManager.getWorkInfoById(request.id).await()!!
        assertThat(workInfo2.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis2)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride_overridesBackoff() = runTest {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)

        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, HOURS.toMillis(1), HOURS)
                .setInitialDelay(2, DAYS)
                .build()
        request.workSpec.runAttemptCount = 1
        workManager.enqueue(request).result.await()

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
                    .build()
            )
            .await()

        val workInfo = workManager.getWorkInfoById(request.id).await()!!
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis)
        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        // attemptCount is still kept, just not used in the schedule time calculation.
        assertThat(workSpec.runAttemptCount).isEqualTo(1)
    }

    @Test
    @SmallTest
    fun clearNextScheduleTimeOverride_incrementGeneration() = runTest {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)

        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                .setInitialDelay(2, DAYS)
                .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
                .build()
        workManager.enqueue(request).result.await()

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .clearNextScheduleTimeOverride()
                    .setInitialDelay(2, DAYS)
                    .build()
            )
            .await()

        val workInfo = workManager.getWorkInfoById(request.id).await()!!
        assertThat(workInfo.nextScheduleTimeMillis)
            .isEqualTo(testClock.currentTimeMillis + DAYS.toMillis(2))

        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        assertThat(workSpec.nextScheduleTimeOverride).isEqualTo(Long.MAX_VALUE)
        // Still needs to increment the generation to propagate the new cleared value.
        assertThat(workSpec.nextScheduleTimeOverrideGeneration).isEqualTo(2)
    }

    @Test
    @SmallTest
    fun clearNextScheduleTimeOverride_noExistingOverride_incrementGenerationAnyway() = runTest {
        testClock.currentTimeMillis = HOURS.toMillis(5)

        val request =
            PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.await()

        workManager
            .updateWork(
                PeriodicWorkRequest.Builder(TestWorker::class, 1, DAYS)
                    .setId(request.id)
                    .clearNextScheduleTimeOverride()
                    .build()
            )
            .await()

        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        assertThat(workSpec.nextScheduleTimeOverride).isEqualTo(Long.MAX_VALUE)

        // Technically I believe any 'clear' call could leave the generation the same, since it's
        // only being checked to see if WorkerWrapper should _clear_ it after running. But this is
        // a simpler implementation, so verify it anyway.
        assertThat(workSpec.nextScheduleTimeOverrideGeneration).isEqualTo(1)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork()
        WorkManagerImpl.setDelegate(null)
    }
}

class WorkerWithParam(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val generation = workerParams.generation

    override fun doWork(): Result = Result.success()
}

private suspend fun WorkManagerImpl.awaitSuccess(id: UUID) {
    val state = awaitWorkerFinished(id).state
    assertThat(state).isEqualTo(State.SUCCEEDED)
}

private val TEST_DATA = Data.Builder().put("key", "test").build()

class ProgressWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    // will never be completed actually, so worker has to be explicitly stopped
    private val deferred = CompletableDeferred<Unit>()

    override suspend fun doWork(): Result {
        setProgress(TEST_DATA)
        deferred.await()
        return Result.retry()
    }
}
