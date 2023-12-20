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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
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
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.impl.workers.ARGUMENT_CLASS_NAME
import androidx.work.impl.workers.ConstraintTrackingWorker
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.worker.LatchWorker
import androidx.work.worker.RetryWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkUpdateTest {
    val workerFactory = TrackingWorkerFactory()
    val testClock = TestOverrideClock()
    val configuration =
        Configuration.Builder().setClock(testClock).setWorkerFactory(workerFactory)
            .setTaskExecutor(Executors.newSingleThreadExecutor()).build()
    val env = TestEnv(configuration)
    val taskExecutor = env.taskExecutor
    val fakeChargingTracker = TestConstraintTracker(false, env.context, env.taskExecutor)
    val trackers = Trackers(
        context = env.context,
        taskExecutor = env.taskExecutor,
        batteryChargingTracker = fakeChargingTracker
    )
    val greedyScheduler = GreedyScheduler(env, trackers)
    val workManager = WorkManager(env, listOf(greedyScheduler), trackers)

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @Test
    @MediumTest
    fun constraintsUpdate() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(oneTimeWorkRequest).result.get()
        val requestId = oneTimeWorkRequest.id

        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(requestId)
            .build()

        val operation = workManager.updateWork(updatedRequest)
        assertThat(operation.get()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(requestId)
    }

    @Test
    @MediumTest
    fun updateRunningOneTimeWork() {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(LatchWorker::class.java).build()
        workManager.enqueue(oneTimeWorkRequest).result.get()
        val worker = workerFactory.awaitWorker(oneTimeWorkRequest.id) as LatchWorker
        // requiresCharging constraint is faked, so it will never be satisfied
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(oneTimeWorkRequest.id)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(APPLIED_FOR_NEXT_RUN)
        worker.mLatch.countDown()
        workManager.awaitSuccess(oneTimeWorkRequest.id)
    }

    @Test
    @MediumTest
    fun failFinished() {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.awaitSuccess(oneTimeWorkRequest.id)
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(oneTimeWorkRequest.id)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(NOT_APPLIED)
    }

    @Test
    @MediumTest
    fun failWorkDoesntExit() {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.awaitSuccess(oneTimeWorkRequest.id)
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(UUID.randomUUID()).build()
        try {
            workManager.updateWork(updatedRequest).get()
            throw AssertionError()
        } catch (e: ExecutionException) {
            assertThat(e).hasCauseThat().isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    @MediumTest
    fun updateTags() {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialDelay(10, DAYS)
            .addTag("previous")
            .build()
        workManager.enqueue(oneTimeWorkRequest).result.get()

        val updatedWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialDelay(10, DAYS)
            .setId(oneTimeWorkRequest.id)
            .addTag("test")
            .build()
        assertThat(workManager.updateWork(updatedWorkRequest).get()).isEqualTo(APPLIED_IMMEDIATELY)

        val info = workManager.getWorkInfoById(oneTimeWorkRequest.id).get()
        assertThat(info.tags).contains("test")
        assertThat(info.tags).doesNotContain("previous")
    }

    @Test
    @MediumTest
    fun updateTagsWhileRunning() {
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .addTag("original").build()
        workManager.enqueue(request).result.get()
        val serialExecutorBlocker = CountDownLatch(1)
        // stop any execution on serialTaskExecutor
        taskExecutor.serialTaskExecutor.execute {
            serialExecutorBlocker.await()
        }
        // will add startWork task to the serialTaskExecutor queue
        greedyScheduler.onConstraintsStateChanged(request.workSpec, ConstraintsMet)
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .setId(request.id)
            .addTag("updated")
            .build()
        // will add update task to the serialTaskExecutor queue
        val updateResult = workManager.updateWork(updatedRequest)
        serialExecutorBlocker.countDown()
        val worker = workerFactory.awaitWorker(request.id)
        assertThat(worker.tags).contains("original")
        assertThat(worker.tags).doesNotContain("updated")
        assertThat(updateResult.get()).isEqualTo(APPLIED_FOR_NEXT_RUN)
    }

    @Test
    @MediumTest
    fun updateWorkerClass() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(oneTimeWorkRequest).result.get()
        val requestId = oneTimeWorkRequest.id

        val updatedRequest = OneTimeWorkRequest.Builder(LatchWorker::class.java)
            .setId(requestId)
            .build()

        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(APPLIED_IMMEDIATELY)
        // verifying that new worker has been started
        val worker = workerFactory.awaitWorker(oneTimeWorkRequest.id) as LatchWorker
        worker.mLatch.countDown()
        workManager.awaitSuccess(requestId)
    }

    @Test
    @MediumTest
    fun progressReset() {
        // requiresCharging constraint is faked, so it will be controlled in the test
        val request = OneTimeWorkRequest.Builder(ProgressWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(request).result.get()
        fakeChargingTracker.constraintState = true
        val runningLatch = CountDownLatch(1)
        lateinit var runningObserver: Observer<WorkInfo>
        val liveData = workManager.getWorkInfoByIdLiveData(request.id)
        runningObserver = Observer {
            // not only running, but setProgressAsync call was made
            if (it.state == State.RUNNING && it.progress.size() != 0) {
                runningLatch.countDown()
                liveData.removeObserver(runningObserver)
            }
        }
        taskExecutor.mainThreadExecutor.execute { liveData.observeForever(runningObserver) }
        assertThat(runningLatch.await(5, TimeUnit.SECONDS)).isTrue()
        // will trigger worker to be stopped
        fakeChargingTracker.state = false

        // wait worker to be stopped
        val stoppedLatch = CountDownLatch(1)
        lateinit var stoppedObserver: Observer<WorkInfo>
        stoppedObserver = Observer {
            if (it.state == State.ENQUEUED) {
                stoppedLatch.countDown()
                liveData.removeObserver(stoppedObserver)
            }
        }
        taskExecutor.mainThreadExecutor.execute { liveData.observeForever(stoppedObserver) }
        assertThat(stoppedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val info = workManager.getWorkInfoById(request.id).get()
        assertThat(info.progress).isEqualTo(TEST_DATA)

        val updatedRequest = OneTimeWorkRequest.Builder(ProgressWorker::class.java)
            .setId(request.id)
            .addTag("bla")
            .build()
        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(APPLIED_IMMEDIATELY)
        val updatedInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(updatedInfo.tags).contains("bla")
        assertThat(updatedInfo.progress).isEqualTo(Data.EMPTY)
    }

    @Test
    @MediumTest
    fun continuationLeafUpdate() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true)).build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.beginWith(step1).then(step2).enqueue().result.get()
        val updatedStep2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(step2.id).addTag("updated").build()
        assertThat(workManager.updateWork(updatedStep2).get()).isEqualTo(APPLIED_IMMEDIATELY)
        val workInfo = workManager.getWorkInfoById(step2.id).get()
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        assertThat(workInfo.tags).contains("updated")
    }

    @Test
    @MediumTest
    fun continuationLeafRoot() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true)).build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.beginWith(step1).then(step2).enqueue().result.get()
        val workInfo = workManager.getWorkInfoById(step2.id).get()
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        val updatedStep1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(step1.id).build()
        assertThat(workManager.updateWork(updatedStep1).get()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(step2.id)
    }

    @Test
    @MediumTest
    fun chainsViaExistingPolicyLeafUpdate() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true)).build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step1)
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step2)
        val updatedStep2 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(step2.id).addTag("updated").build()
        assertThat(workManager.updateWork(updatedStep2).get()).isEqualTo(APPLIED_IMMEDIATELY)
        val workInfo = workManager.getWorkInfoById(step2.id).get()
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        assertThat(workInfo.tags).contains("updated")
    }

    @Test
    @MediumTest
    fun chainsViaExistingPolicyRootUpdate() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val step1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true)).build()
        val step2 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step1)
        workManager.enqueueUniqueWork("name", ExistingWorkPolicy.APPEND, step2)
        val workInfo = workManager.getWorkInfoById(step2.id).get()
        assertThat(workInfo.state).isEqualTo(State.BLOCKED)
        val updatedStep1 = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setId(step1.id).build()
        assertThat(workManager.updateWork(updatedStep1).get()).isEqualTo(APPLIED_IMMEDIATELY)
        workManager.awaitSuccess(step2.id)
    }

    @Test
    @MediumTest
    fun oneTimeWorkToPeriodic() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(requiresCharging = true)).build()
        workManager.enqueue(request).result.get()
        val updatedRequest =
            PeriodicWorkRequest.Builder(TestWorker::class.java, 1, DAYS)
                .build()
        try {
            workManager.updateWork(updatedRequest).get()
            throw AssertionError()
        } catch (e: ExecutionException) {
            assertThat(e).hasCauseThat().isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    @MediumTest
    fun periodicWorkToOneTime() {
        // requiresCharging constraint is faked, so it will never be satisfied
        val request = PeriodicWorkRequest.Builder(TestWorker::class.java, 1, DAYS)
            .setConstraints(Constraints(requiresCharging = true))
            .build()
        workManager.enqueue(request).result.get()
        val updatedRequest = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        try {
            workManager.updateWork(updatedRequest).get()
            throw AssertionError()
        } catch (e: ExecutionException) {
            assertThat(e).hasCauseThat().isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    @MediumTest
    fun updateRunningPeriodicWorkRequest() {
        val request = PeriodicWorkRequest.Builder(LatchWorker::class.java, 1, DAYS)
            .addTag("original").build()
        workManager.enqueue(request).result.get()
        val updatedRequest =
            PeriodicWorkRequest.Builder(LatchWorker::class.java, 1, DAYS)
                .setId(request.id).addTag("updated").build()
        val worker = workerFactory.awaitWorker(request.id) as LatchWorker
        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(APPLIED_FOR_NEXT_RUN)
        val latch = CountDownLatch(1)
        taskExecutor.serialTaskExecutor.execute { latch.countDown() }
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(worker.isStopped).isFalse()
        assertThat(worker.tags).contains("original")
        assertThat(worker.tags).doesNotContain("updated")
        worker.mLatch.countDown()
        val reenqueueLatch = CountDownLatch(1)
        val workInfoLD = workManager.getWorkInfoByIdLiveData(request.id)
        lateinit var observer: Observer<WorkInfo>
        observer = Observer {
            if (it.state == State.ENQUEUED) {
                reenqueueLatch.countDown()
                workInfoLD.removeObserver(observer)
            }
        }
        taskExecutor.mainThreadExecutor.execute { workInfoLD.observeForever(observer) }
        assertThat(reenqueueLatch.await(3, TimeUnit.SECONDS)).isTrue()
        val newTags = workManager.getWorkInfoById(request.id).get().tags
        assertThat(newTags).contains("updated")
        assertThat(newTags).doesNotContain("original")
    }

    @MediumTest
    @Test
    fun updatePeriodicWorkAfterFirstPeriod() {
        val request = PeriodicWorkRequest.Builder(TestWorker::class.java, 1, DAYS)
            .addTag("original").build()
        val onExecutedLatch = CountDownLatch(1)
        env.processor.addExecutionListener { id, _ ->
            if (id.workSpecId == request.stringId) onExecutedLatch.countDown()
        }
        workManager.enqueue(request).result.get()
        workerFactory.awaitWorker(request.id)
        workManager.awaitReenqueued(request.id)

        val updatedRequest =
            PeriodicWorkRequest.Builder(TestWorker::class.java, 1, DAYS)
                // requiresCharging constraint is faked, so it will never be satisfied
                .setConstraints(Constraints(requiresCharging = true))
                .setId(request.id).addTag("updated").build()

        assertThat(workManager.updateWork(updatedRequest).get()).isEqualTo(APPLIED_IMMEDIATELY)

        val newTags = workManager.getWorkInfoById(request.id).get().tags
        assertThat(newTags).contains("updated")
        assertThat(newTags).doesNotContain("original")
        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        assertThat(workSpec.periodCount).isEqualTo(1)
    }

    @MediumTest
    @Test
    fun updateRetryingOneTimeWork() {
        val request = OneTimeWorkRequest.Builder(RetryWorker::class.java)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, DAYS)
            .build()
        workManager.enqueue(request)
        // await worker to be created
        val worker1 = workerFactory.awaitWorker(request.id)
        assertThat(worker1.runAttemptCount).isEqualTo(0)
        workManager.awaitReenqueued(request.id)
        // rewind time so can updated worker can run
        val spec = workManager.workDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        val delta = spec.calculateNextRunTime() - System.currentTimeMillis()
        assertThat(delta).isGreaterThan(0)
        workManager.workDatabase.workSpecDao().setLastEnqueueTime(
            request.stringId,
            spec.lastEnqueueTime - delta
        )
        val updated = OneTimeWorkRequest.Builder(TestWorker::class.java).setId(request.id)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, DAYS)
            .build()
        workManager.updateWork(updated).get()
        workManager.awaitSuccess(request.id)
        val worker2 = workerFactory.awaitWorker(request.id)
        assertThat(worker2.runAttemptCount).isEqualTo(1)
    }

    @MediumTest
    @Test
    fun updateCorrectNextRunTime() {
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialDelay(10, TimeUnit.MINUTES).build()
        val enqueueTime = System.currentTimeMillis()
        workManager.enqueue(request).result.get()
        workManager.workDatabase.workSpecDao().setLastEnqueueTime(
            request.stringId,
            enqueueTime - TimeUnit.MINUTES.toMillis(5)
        )
        val updated = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialDelay(20, TimeUnit.MINUTES)
            .setId(request.id)
            .build()
        workManager.updateWork(updated).get()
        val workSpec = workManager.workDatabase.workSpecDao().getWorkSpec(request.stringId)!!
        val delta = workSpec.calculateNextRunTime() - enqueueTime
        // enqueue time isn't very accurate but delta should be about 15 minutes, because
        // enqueueTime was rewound 5 minutes back.
        assertThat(delta).isGreaterThan(TimeUnit.MINUTES.toMillis(14))
        assertThat(delta).isLessThan(TimeUnit.MINUTES.toMillis(16))
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    fun testUpdatePeriodicWorker_preservesConstraintTrackingWorker() {
        val originRequest = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setInitialDelay(10, HOURS).build()
        workManager.enqueue(originRequest).result.get()
        val updateRequest = OneTimeWorkRequest.Builder(RetryWorker::class.java)
            .setId(originRequest.id).setInitialDelay(10, HOURS)
            .setConstraints(Constraints(requiresBatteryNotLow = true))
            .build()
        workManager.updateWork(updateRequest).get()
        val workSpec = env.db.workSpecDao().getWorkSpec(originRequest.stringId)!!
        assertThat(workSpec.workerClassName).isEqualTo(ConstraintTrackingWorker::class.java.name)
        assertThat(workSpec.input.getString(ARGUMENT_CLASS_NAME))
            .isEqualTo(RetryWorker::class.java.name)
    }

    @Test
    @MediumTest
    fun updateWorkerGeneration() {
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(WorkerWithParam::class.java)
            .setInitialDelay(10, DAYS)
            .build()
        workManager.enqueue(oneTimeWorkRequest).result.get()

        val updatedWorkRequest = OneTimeWorkRequest.Builder(WorkerWithParam::class.java)
            .setId(oneTimeWorkRequest.id)
            .build()

        assertThat(workManager.updateWork(updatedWorkRequest).get()).isEqualTo(APPLIED_IMMEDIATELY)
        val worker = workerFactory.awaitWorker(oneTimeWorkRequest.id) as WorkerWithParam
        assertThat(worker.generation).isEqualTo(1)
        val workInfo = workManager.getWorkInfoById(oneTimeWorkRequest.id).get()
        assertThat(workInfo.generation).isEqualTo(1)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride() {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val nextRunTimeMillis = HOURS.toMillis(10)

        val request = PeriodicWorkRequest.Builder(
            TestWorker::class.java, 1, DAYS
        ).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.get()

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .setNextScheduleTimeOverride(nextRunTimeMillis)
                .build()
        ).get()

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(nextRunTimeMillis)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride_multipleGenerations() {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)
        val overrideScheduleTimeMillis2 = HOURS.toMillis(12)

        val request = PeriodicWorkRequest.Builder(
            TestWorker::class.java, 1, DAYS
        ).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.get()

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
                .build()
        ).get()
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis)

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .setNextScheduleTimeOverride(overrideScheduleTimeMillis2)
                .build()
        ).get()

        val workInfo2 = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo2.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis2)
    }

    @Test
    @SmallTest
    fun updateNextScheduleTimeOverride_overridesBackoff() {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)

        val request = PeriodicWorkRequest.Builder(
            TestWorker::class.java, 1, DAYS
        ).setBackoffCriteria(BackoffPolicy.LINEAR, HOURS.toMillis(1), HOURS)
            .setInitialDelay(2, DAYS)
            .build()
        request.workSpec.runAttemptCount = 1
        workManager.enqueue(request).result.get()

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
                .build()
        ).get()

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(overrideScheduleTimeMillis)
        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        // attemptCount is still kept, just not used in the schedule time calculation.
        assertThat(workSpec.runAttemptCount).isEqualTo(1)
    }

    @Test
    @SmallTest
    fun clearNextScheduleTimeOverride_incrementGeneration() {
        testClock.currentTimeMillis = HOURS.toMillis(5)
        val overrideScheduleTimeMillis = HOURS.toMillis(10)

        val request = PeriodicWorkRequest.Builder(
            TestWorker::class.java, 1, DAYS
        ).setInitialDelay(2, DAYS)
            .setNextScheduleTimeOverride(overrideScheduleTimeMillis)
            .build()
        workManager.enqueue(request).result.get()

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .clearNextScheduleTimeOverride()
                .setInitialDelay(2, DAYS)
                .build()
        ).get()

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertThat(workInfo.nextScheduleTimeMillis).isEqualTo(
            testClock.currentTimeMillis + DAYS.toMillis(2)
        )

        val workSpec = env.db.workSpecDao().getWorkSpec(request.stringId)!!
        assertThat(workSpec.nextScheduleTimeOverride).isEqualTo(Long.MAX_VALUE)
        // Still needs to increment the generation to propagate the new cleared value.
        assertThat(workSpec.nextScheduleTimeOverrideGeneration).isEqualTo(2)
    }

    @Test
    @SmallTest
    fun clearNextScheduleTimeOverride_noExistingOverride_incrementGenerationAnyway() {
        testClock.currentTimeMillis = HOURS.toMillis(5)

        val request = PeriodicWorkRequest.Builder(
            TestWorker::class.java, 1, DAYS
        ).setInitialDelay(2, DAYS).build()
        workManager.enqueue(request).result.get()

        workManager.updateWork(
            PeriodicWorkRequest.Builder(
                TestWorker::class.java, 1, DAYS
            ).setId(request.id)
                .clearNextScheduleTimeOverride()
                .build()
        ).get()

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

class WorkerWithParam(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    val generation = workerParams.generation
    override fun doWork(): Result = Result.success()
}

private fun WorkManagerImpl.awaitSuccess(id: UUID) =
    getWorkInfoByIdLiveData(id).awaitSuccess(workTaskExecutor)

private fun WorkManagerImpl.awaitReenqueued(id: UUID) {
    val reenqueueLatch = CountDownLatch(1)
    val workInfoLD = getWorkInfoByIdLiveData(id)
    lateinit var observer: Observer<WorkInfo>
    observer = Observer {
        if (it.state == State.ENQUEUED) {
            reenqueueLatch.countDown()
            workInfoLD.removeObserver(observer)
        }
    }
    workTaskExecutor.mainThreadExecutor.execute { workInfoLD.observeForever(observer) }
    assertThat(reenqueueLatch.await(3, TimeUnit.SECONDS)).isTrue()
}

private fun LiveData<WorkInfo>.awaitSuccess(taskExecutor: TaskExecutor) {
    val latch = CountDownLatch(1)
    lateinit var observer: Observer<WorkInfo>
    var result = State.SUCCEEDED
    observer = Observer {
        if (it.state.isFinished) {
            result = it.state
            latch.countDown()
            this@awaitSuccess.removeObserver(observer)
        }
    }
    taskExecutor.mainThreadExecutor.execute {
        this@awaitSuccess.observeForever(observer)
    }
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
    assertThat(result).isEqualTo(State.SUCCEEDED)
}

private val TEST_DATA = Data.Builder().put("key", "test").build()

class ProgressWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private val latch = CountDownLatch(1)
    override fun doWork(): Result {
        setProgressAsync(TEST_DATA).get()
        latch.await()
        return Result.retry()
    }

    override fun onStopped() {
        super.onStopped()
        latch.countDown()
    }
}
