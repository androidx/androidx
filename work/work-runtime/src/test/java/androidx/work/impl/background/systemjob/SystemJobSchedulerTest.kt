/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.impl.background.systemjob

import android.app.job.JobScheduler
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.RunnableScheduler
import androidx.work.WorkInfo
import androidx.work.impl.WorkDatabase
import androidx.work.impl.background.systemjob.SystemJobScheduler.FGS_JOB_DELAY_MILLIS
import androidx.work.impl.background.systemjob.SystemJobScheduler.FGS_RESCHEDULE_INTERVAL_MILLIS
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.generationalId
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, minSdk = 24)
class SystemJobSchedulerTest {
    companion object {
        private const val TEST_WORK_SPEC_ID = "temp-work-id"
    }

    private lateinit var context: Context
    private lateinit var database: WorkDatabase
    private lateinit var jobScheduler: JobScheduler
    private lateinit var fakeRunnableScheduler: FakeRunnableScheduler
    private lateinit var systemJobScheduler: SystemJobScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val configuration = Configuration.Builder().build()
        val executor = Executor { it.run() }

        database =
            WorkDatabase.create(context, executor, configuration.clock, /* useTestDatabase= */ true)

        fakeRunnableScheduler = FakeRunnableScheduler()
        jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        systemJobScheduler =
            SystemJobScheduler(
                context,
                database,
                configuration,
                jobScheduler,
                SystemJobInfoConverter(
                    context,
                    configuration.clock,
                    configuration.isMarkingJobsAsImportantWhileForeground(),
                ),
                fakeRunnableScheduler,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testOnForegroundChanged_promotion_schedulesDelayedJobAndLoop() {
        val workSpec = insertWorkSpec(TEST_WORK_SPEC_ID)
        val generationalId = workSpec.generationalId()

        // Given the work is initially scheduled in JobScheduler (not delayed)
        systemJobScheduler.schedule(workSpec)
        val jobId = database.systemIdInfoDao().getSystemIdInfo(generationalId)!!.systemId
        val initialJob = jobScheduler.allPendingJobs.first { it.id == jobId }
        assertThat(initialJob.minLatencyMillis).isEqualTo(0L)
        database.workSpecDao().setState(WorkInfo.State.RUNNING, TEST_WORK_SPEC_ID)

        // When the work is promoted to foreground (FGS)
        systemJobScheduler.onForegroundChanged(generationalId, true)

        // Then verify the job is rescheduled with the FGS delay
        val pendingJobs = jobScheduler.allPendingJobs
        assertThat(pendingJobs).hasSize(1)
        val delayedJob = pendingJobs.first { it.id == jobId }
        assertThat(delayedJob.minLatencyMillis).isEqualTo(FGS_JOB_DELAY_MILLIS)

        // And verify the rescheduling loop is scheduled
        assertThat(fakeRunnableScheduler.runnables).hasSize(1)
        val (_, delay) = fakeRunnableScheduler.runnables.entries.first()
        assertThat(delay).isEqualTo(FGS_RESCHEDULE_INTERVAL_MILLIS)
    }

    @Test
    fun testOnForegroundChanged_loopExecution_reschedulesJobAndLoop() {
        val workSpec = insertWorkSpec(TEST_WORK_SPEC_ID)
        val generationalId = workSpec.generationalId()

        // Given the work is scheduled and promoted to foreground
        systemJobScheduler.schedule(workSpec)
        database.workSpecDao().setState(WorkInfo.State.RUNNING, TEST_WORK_SPEC_ID)
        systemJobScheduler.onForegroundChanged(generationalId, true)
        val jobId = database.systemIdInfoDao().getSystemIdInfo(generationalId)!!.systemId
        val runnable = fakeRunnableScheduler.runnables.keys.first()

        // When the loop runnable executes
        runnable.run()

        // Then verify the job is still scheduled in the scheduler with the correct delay.
        val rescheduledJobs = jobScheduler.allPendingJobs
        assertThat(rescheduledJobs).hasSize(1)
        val jobAfter = rescheduledJobs.first { it.id == jobId }
        assertThat(jobAfter.minLatencyMillis).isEqualTo(FGS_JOB_DELAY_MILLIS)

        // And verify the loop rescheduled itself
        assertThat(fakeRunnableScheduler.runnables).containsKey(runnable)
        assertThat(fakeRunnableScheduler.runnables[runnable])
            .isEqualTo(FGS_RESCHEDULE_INTERVAL_MILLIS)
    }

    @Test
    fun testOnForegroundChanged_demotion_cancelsLoop() {
        val workSpec = insertWorkSpec(TEST_WORK_SPEC_ID)
        val generationalId = workSpec.generationalId()

        // Given the work is scheduled and promoted to foreground
        systemJobScheduler.schedule(workSpec)
        database.workSpecDao().setState(WorkInfo.State.RUNNING, TEST_WORK_SPEC_ID)
        systemJobScheduler.onForegroundChanged(generationalId, true)
        val runnable = fakeRunnableScheduler.runnables.keys.first()

        // When the work is demoted from foreground
        systemJobScheduler.onForegroundChanged(generationalId, false)

        // Then verify the loop is cancelled (runnable removed from the fake scheduler)
        assertThat(fakeRunnableScheduler.runnables).doesNotContainKey(runnable)
    }

    private fun insertWorkSpec(id: String): WorkSpec {
        val workSpec =
            WorkSpec(id, "androidx.work.worker.TestWorker").apply {
                lastEnqueueTime = System.currentTimeMillis()
            }
        database.workSpecDao().insertWorkSpec(workSpec)
        return workSpec
    }
}

private class FakeRunnableScheduler : RunnableScheduler {
    val runnables = mutableMapOf<Runnable, Long>()

    override fun scheduleWithDelay(delayInMillis: Long, runnable: Runnable) {
        runnables[runnable] = delayInMillis
    }

    override fun cancel(runnable: Runnable) {
        runnables.remove(runnable)
    }
}
