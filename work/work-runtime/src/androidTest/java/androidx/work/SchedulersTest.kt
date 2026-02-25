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
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.impl.Processor
import androidx.work.impl.Scheduler
import androidx.work.impl.StartStopTokens
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkLauncherImpl
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.background.greedy.GreedyScheduler
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.testutils.TrackingWorkerFactory
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.worker.FailureWorker
import androidx.work.worker.LatchWorker
import androidx.work.worker.StopAwareWorker
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SchedulersTest {
    val factory = TrackingWorkerFactory()
    val configuration = Configuration.Builder().setWorkerFactory(factory).build()
    val env = TestEnv(configuration)
    val context = env.context
    val trackers = Trackers(context, env.taskExecutor)
    val greedyScheduler = GreedyScheduler(env, trackers)

    private val limitedSlotsScheduler = FakeScheduler(hasLimitedSchedulingSlots = true)
    private val unlimitedSlotsScheduler = FakeScheduler(hasLimitedSchedulingSlots = false)
    private val fakeSchedulers =
        listOf<FakeScheduler>(limitedSlotsScheduler, unlimitedSlotsScheduler)

    @Test
    fun runDependency() {
        val cancelled = mutableSetOf<String>()
        val trackingScheduler =
            object : Scheduler {
                override fun schedule(vararg workSpecs: WorkSpec?) {}

                override fun cancel(workSpecId: String) {
                    cancelled.add(workSpecId)
                }

                override fun hasLimitedSchedulingSlots() = false
            }
        val wm =
            WorkManagerImpl(context, configuration, env.taskExecutor, env.db) {
                context: Context,
                configuration: Configuration,
                taskExecutor: TaskExecutor,
                _: WorkDatabase,
                trackers: Trackers,
                processor: Processor ->
                listOf(
                    GreedyScheduler(
                        context,
                        configuration,
                        trackers,
                        processor,
                        WorkLauncherImpl(processor, taskExecutor),
                        taskExecutor,
                    ),
                    trackingScheduler,
                )
            }

        val workRequest = OneTimeWorkRequest.from(TestWorker::class.java)
        val dependency = OneTimeWorkRequest.from(TestWorker::class.java)
        wm.beginWith(workRequest).then(dependency).enqueue()
        val finishedLatch = CountDownLatch(1)
        env.taskExecutor.mainThreadExecutor.execute {
            wm.getWorkInfoByIdLiveData(dependency.id).observeForever {
                if (it?.state == WorkInfo.State.SUCCEEDED) finishedLatch.countDown()
            }
        }
        assertThat(finishedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cancelled).containsExactly(workRequest.stringId, dependency.stringId)
    }

    @Test
    fun failedWorker() {
        val cancelled = mutableSetOf<String>()
        val trackingScheduler =
            object : Scheduler {
                override fun schedule(vararg workSpecs: WorkSpec?) {}

                override fun cancel(workSpecId: String) {
                    cancelled.add(workSpecId)
                }

                override fun hasLimitedSchedulingSlots() = false
            }
        val wm =
            WorkManagerImpl(
                context,
                configuration,
                env.taskExecutor,
                env.db,
                listOf(trackingScheduler, greedyScheduler),
                env.processor,
                trackers,
            )

        val workRequest = OneTimeWorkRequest.from(FailureWorker::class.java)
        wm.enqueue(workRequest)
        val finishedLatch = CountDownLatch(1)
        env.taskExecutor.mainThreadExecutor.execute {
            wm.getWorkInfoByIdLiveData(workRequest.id).observeForever {
                if (it?.state == WorkInfo.State.FAILED) finishedLatch.countDown()
            }
        }
        assertThat(finishedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cancelled).containsExactly(workRequest.stringId)
    }

    @Test
    fun interruptionReschedules() {
        val schedulers = mutableListOf<Scheduler>()
        val wm =
            WorkManagerImpl(
                context,
                configuration,
                env.taskExecutor,
                env.db,
                schedulers,
                env.processor,
                trackers,
            )
        val scheduledSpecs = mutableListOf<WorkSpec>()
        val cancelledIds = mutableListOf<String>()
        val launcher = WorkLauncherImpl(env.processor, env.taskExecutor)
        val scheduler =
            object : Scheduler {
                val tokens = StartStopTokens.create()

                override fun schedule(vararg workSpecs: WorkSpec) {
                    scheduledSpecs.addAll(workSpecs)
                    workSpecs.forEach {
                        if (it.runAttemptCount == 0) launcher.startWork(tokens.tokenFor(it))
                    }
                }

                override fun cancel(workSpecId: String) {
                    cancelledIds.add(workSpecId)
                }

                override fun hasLimitedSchedulingSlots() = false
            }

        schedulers.add(scheduler)
        val request = OneTimeWorkRequest.from(StopAwareWorker::class.java)
        wm.enqueue(request)
        val reenqueuedLatch = CountDownLatch(1)
        var running = false
        env.taskExecutor.mainThreadExecutor.execute {
            wm.getWorkInfoByIdLiveData(request.id).observeForever {
                when (it?.state) {
                    WorkInfo.State.RUNNING -> {
                        launcher.stopWork(scheduler.tokens.remove(request.stringId).first())
                        running = true
                    }
                    WorkInfo.State.ENQUEUED -> {
                        if (running) reenqueuedLatch.countDown()
                        running = false
                    }
                    else -> {}
                }
            }
        }
        assertThat(reenqueuedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cancelledIds).containsExactly(request.stringId)
        val workSpec = scheduledSpecs.last()
        assertThat(workSpec.id).isEqualTo(request.stringId)
        assertThat(workSpec.runAttemptCount).isEqualTo(1)
    }

    @Test
    fun periodicReschedules() {
        val schedulers = mutableListOf<Scheduler>()
        val wm =
            WorkManagerImpl(
                context,
                configuration,
                env.taskExecutor,
                env.db,
                schedulers,
                env.processor,
                trackers,
            )
        val scheduledSpecs = mutableListOf<WorkSpec>()
        val cancelledIds = mutableListOf<String>()
        val launcher = WorkLauncherImpl(env.processor, env.taskExecutor)
        val scheduler =
            object : Scheduler {
                val tokens = StartStopTokens.create()

                override fun schedule(vararg workSpecs: WorkSpec) {
                    scheduledSpecs.addAll(workSpecs)
                    workSpecs.forEach {
                        if (it.periodCount == 0) launcher.startWork(tokens.tokenFor(it))
                    }
                }

                override fun cancel(workSpecId: String) {
                    cancelledIds.add(workSpecId)
                }

                override fun hasLimitedSchedulingSlots() = false
            }

        schedulers.add(scheduler)
        val request =
            PeriodicWorkRequest.Builder(LatchWorker::class.java, 1L, TimeUnit.DAYS).build()
        wm.enqueue(request)
        val reenqueuedLatch = CountDownLatch(1)
        var running = false
        val worker = factory.awaitWorker(request.id) as LatchWorker
        env.taskExecutor.mainThreadExecutor.execute {
            wm.getWorkInfoByIdLiveData(request.id).observeForever {
                when (it?.state) {
                    WorkInfo.State.RUNNING -> {
                        running = true
                        worker.mLatch.countDown()
                    }
                    WorkInfo.State.ENQUEUED -> {
                        if (running) reenqueuedLatch.countDown()
                        running = false
                    }
                    else -> {}
                }
            }
        }
        assertThat(reenqueuedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cancelledIds).containsExactly(request.stringId)
        val workSpec = scheduledSpecs.last()
        assertThat(workSpec.id).isEqualTo(request.stringId)
        assertThat(workSpec.periodCount).isEqualTo(1)
    }

    @Test
    fun representativeJobs_allConstraintsFitWithinLimit() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(maxSchedulerLimit)
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        val constraints1 =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val constraints2 = Constraints.Builder().setRequiresCharging(true).build()
        val constraints3 = Constraints.Builder().setRequiresStorageNotLow(true).build()

        val workSpecs =
            listOf(
                // ws1 and ws2 have same constraints
                createWorkSpec(testEnv, "ws1", constraints1, 0L),
                createWorkSpec(testEnv, "ws2", constraints1, 1000L),

                // ws3 and ws4 have unique constraints
                createWorkSpec(testEnv, "ws3", constraints2, 500L),
                createWorkSpec(testEnv, "ws4", constraints3, 200L),
            )

        insertWorkSpecs(db, workSpecs)

        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        for (fakeScheduler in fakeSchedulers) {
            assertThat(fakeScheduler.scheduledWork)
                .containsExactlyElementsIn(workSpecs.map { it.id })
        }
    }

    @Test
    fun representativeJobs_uniqueConstraintsWithinLimit_nonUniqueConstraintCanceled() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(
                    // SDK 23 halves the max slots for double scheduling.
                    if (Build.VERSION.SDK_INT == 23) 2 * maxSchedulerLimit else maxSchedulerLimit
                )
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        // Fill the scheduler with matching constraints
        val workSpecs = buildList {
            for (i in 1..maxSchedulerLimit) {
                add(
                    createWorkSpec(
                        testEnv,
                        "ws_initial_$i",
                        Constraints.Builder().setRequiresCharging(true).build(),
                        1000L,
                    )
                )
            }
        }
        insertWorkSpecs(db, workSpecs)
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        val initialScheduledIds = limitedSlotsScheduler.scheduledWork
        assertThat(initialScheduledIds).hasSize(maxSchedulerLimit)

        // Add a work with a different constraint
        val uniqueWs =
            createWorkSpec(
                testEnv,
                "ws_unique",
                Constraints.Builder().setRequiresCharging(false).build(),
                0L,
            )
        insertWorkSpecs(db, listOf(uniqueWs))
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        val allWork = workSpecs.plus(uniqueWs)

        // Unlimited slot scheduler schedules all work and nothing is canceled.
        assertThat(unlimitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.map { it.id })

        // Verify that the last enqueued, non-unique worker was replaced by the unique one.
        assertThat(limitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.minus(workSpecs.last()).map { it.id })
    }

    @Test
    fun representativeJobs_uniqueConstraintsWithinLimit_soonestRepresentativeScheduled() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(
                    // SDK 23 halves the max slots for double scheduling.
                    if (Build.VERSION.SDK_INT == 23) 2 * maxSchedulerLimit else maxSchedulerLimit
                )
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        // Fill the scheduler with matching constraints
        val workSpecs = buildList {
            for (i in 1..maxSchedulerLimit) {
                add(createWorkSpec(testEnv, "ws_initial_$i", Constraints.Builder().build(), 1000L))
            }
        }
        insertWorkSpecs(db, workSpecs)
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        // Add a work with earlier delay.
        val earlierWs = createWorkSpec(testEnv, "ws_sooner", Constraints.Builder().build(), 0L)
        insertWorkSpecs(db, listOf(earlierWs))
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        val allWork = workSpecs.plus(earlierWs)

        // Unlimited slot scheduler schedules all work and nothing is canceled.
        assertThat(unlimitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.map { it.id })

        // Verify that the earlier worker replaces the last enqueued of the later ones.
        assertThat(limitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.minus(workSpecs.last()).map { it.id })
    }

    @Test
    fun representativeJobs_uniqueConstraintsExceedsLimit_soonestRepresentativeScheduled() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(
                    // SDK 23 halves the max slots for double scheduling.
                    if (Build.VERSION.SDK_INT == 23) 2 * maxSchedulerLimit else maxSchedulerLimit
                )
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        // Fill the scheduler with various unique constraints
        val workSpecs = buildList {
            for (i in 0 until maxSchedulerLimit) {
                // Unique up to 2^5 (but we only need the limit, 20).
                fun bit(n: Int) = (i shr n) and 1 != 0

                val charging = bit(0)
                val idle = bit(1)
                val storage = bit(2)
                val batteryNotLow = bit(3)
                val networkType =
                    if (bit(4)) {
                        NetworkType.CONNECTED
                    } else {
                        NetworkType.UNMETERED
                    }

                add(
                    createWorkSpec(
                        testEnv,
                        "ws_unique_$i",
                        Constraints.Builder()
                            .setRequiresCharging(charging)
                            .setRequiresDeviceIdle(idle)
                            .setRequiresStorageNotLow(storage)
                            .setRequiresBatteryNotLow(batteryNotLow)
                            .setRequiredNetworkType(networkType)
                            .build(),
                        1000L,
                    )
                )
            }
        }

        // Check that test logic setup unique workSpecs correctly.
        assertThat(workSpecs.map { it.constraints }.distinct()).hasSize(maxSchedulerLimit)

        insertWorkSpecs(db, workSpecs)
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        // Add a work with earlier delay.
        val earlierWs = createWorkSpec(testEnv, "ws_sooner", Constraints.Builder().build(), 0L)
        insertWorkSpecs(db, listOf(earlierWs))
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        val allWork = workSpecs.plus(earlierWs)

        // Unlimited slot scheduler schedules all work and nothing is canceled.
        assertThat(unlimitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.map { it.id })

        // Verify that the last earlier worker replaces the later one.
        assertThat(limitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.minus(workSpecs.last()).map { it.id })
    }

    @Test
    fun representativeJobs_rescheduledRepresentativeJob_isNotCanceled() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(maxSchedulerLimit)
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        val ws1 = createWorkSpec(testEnv, "ws1", Constraints.Builder().build())
        insertWorkSpecs(db, listOf(ws1))

        // First schedule pass
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)
        // Reschedule without any changes
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        for (fakeScheduler in fakeSchedulers) {
            assertThat(fakeScheduler.scheduledWork).containsExactly(ws1.id)
        }
    }

    @Test
    fun representativeJobs_evictedJobsRescheduledAfterCompletion() {
        val maxSchedulerLimit = 20
        val config =
            Configuration.Builder()
                .setWorkerFactory(factory)
                .setMaxSchedulerLimit(
                    // SDK 23 halves the max slots for double scheduling.
                    if (Build.VERSION.SDK_INT == 23) 2 * maxSchedulerLimit else maxSchedulerLimit
                )
                .setRepresentativeJobsEnabled(true)
                .build()
        val testEnv = TestEnv(config)
        val db = testEnv.db

        // Schedule 20 workers with long delay
        val longDelaySpecs = buildList {
            for (i in 1..maxSchedulerLimit) {
                add(createWorkSpec(testEnv, "ws_long_$i", Constraints.Builder().build(), 1000L))
            }
        }
        insertWorkSpecs(db, longDelaySpecs)
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        // Schedule worker with short delay
        val shortDelaySpec = createWorkSpec(testEnv, "ws_short", Constraints.Builder().build(), 0L)

        insertWorkSpecs(db, listOf(shortDelaySpec))
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        val allWork = longDelaySpecs.plus(shortDelaySpec)
        // Unlimited scheduler contains all workers.
        assertThat(unlimitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.map { it.id })
        // Limited scheduler evicts the last non-unique worker.
        assertThat(limitedSlotsScheduler.scheduledWork)
            .containsExactlyElementsIn(allWork.minus(longDelaySpecs.last()).map { it.id })

        // Finish the short delay worker by marking it as succeeded and cancelling it in scheduler.
        db.workSpecDao().setState(WorkInfo.State.SUCCEEDED, shortDelaySpec.id)
        for (fakeScheduler in fakeSchedulers) fakeScheduler.cancel(shortDelaySpec.id)
        androidx.work.impl.Schedulers.schedule(config, db, fakeSchedulers)

        // All schedulers should now contain the original set of workers including the evicted one.
        for (fakeScheduler in fakeSchedulers) {
            assertThat(fakeScheduler.scheduledWork)
                .containsExactlyElementsIn(longDelaySpecs.map { it.id })
        }
    }

    private fun createWorkSpec(
        env: TestEnv,
        id: String,
        constraints: Constraints,
        initialDelay: Long = 0L,
    ): WorkSpec {
        return WorkSpec(id, "androidx.work.worker.TestWorker").apply {
            this.constraints = constraints
            this.initialDelay = initialDelay
            this.lastEnqueueTime = env.configuration.clock.currentTimeMillis()
        }
    }

    private fun insertWorkSpecs(db: WorkDatabase, workSpecs: List<WorkSpec>) {
        workSpecs.forEach { db.workSpecDao().insertWorkSpec(it) }
    }

    private class FakeScheduler(private val hasLimitedSchedulingSlots: Boolean) : Scheduler {

        private val _scheduledWork = mutableSetOf<String>()

        val scheduledWork: Set<String>
            get() = _scheduledWork

        override fun schedule(vararg workSpecs: WorkSpec) {
            _scheduledWork.addAll(workSpecs.map { it.id })
        }

        override fun cancel(workSpecId: String) {
            _scheduledWork.remove(workSpecId)
        }

        override fun hasLimitedSchedulingSlots(): Boolean = hasLimitedSchedulingSlots
    }
}
