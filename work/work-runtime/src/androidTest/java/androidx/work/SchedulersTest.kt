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
            WorkManagerImpl(
                context,
                configuration,
                env.taskExecutor,
                env.db,
            ) {
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
                        taskExecutor
                    ),
                    trackingScheduler
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
                trackers
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
                trackers
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
                trackers
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
}
