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

package androidx.work.impl.background.greedy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Clock
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.RunnableScheduler
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.Processor
import androidx.work.impl.StartStopToken
import androidx.work.impl.TestWorkManagerImpl
import androidx.work.impl.WorkLauncher
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.close
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.clearManifestDefaultCache
import androidx.work.impl.utils.isDefaultProcess
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that [GreedyScheduler] is enabled in every process when
 * `Configuration.isProcessIsolationEnabled` is `true`, and keeps the legacy default-process-only
 * behavior when it is `false`.
 *
 * This Robolectric test intentionally complements `GreedySchedulerTest` in `androidTest`.
 */
@RunWith(RobolectricTestRunner::class)
// Exercises both the pre-API-28 reflection path and the API 28+ path in ProcessUtils.
@Config(manifest = Config.NONE, sdk = [24, Config.TARGET_SDK])
class GreedySchedulerProcessIsolationTest {

    private lateinit var context: Context
    private lateinit var processor: Processor
    private lateinit var workLauncher: RecordingWorkLauncher
    private lateinit var runnableScheduler: FakeRunnableScheduler
    private val directExecutor = SynchronousExecutor()
    private val fixedClock = Clock { FIXED_TIME_MILLIS }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        processor = mock(Processor::class.java)
        workLauncher = RecordingWorkLauncher()
        runnableScheduler = FakeRunnableScheduler()
    }

    @Test
    fun schedule_isolationDisabled_defaultProcess_startsWork() {
        val configuration = configuration(isolationEnabled = false, inDefaultProcess = true)
        val scheduler = greedyScheduler(configuration)
        val workSpec = unconstrainedWorkSpec()

        scheduler.schedule(workSpec)

        assertThat(workLauncher.startedWorkSpecIds).containsExactly(workSpec.id)
        assertThat(runnableScheduler.runnables).hasSize(1) // TimeLimiter timeout runnable
        verify(processor).addExecutionListener(scheduler)
        assertThat(scheduler.mInDefaultProcess).isTrue()
    }

    @Test
    fun schedule_isolationDisabled_secondaryProcess_ignoresWork() {
        val configuration = configuration(isolationEnabled = false, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)

        scheduler.schedule(unconstrainedWorkSpec())

        assertThat(workLauncher.startedWorkSpecIds).isEmpty()
        assertThat(runnableScheduler.runnables).isEmpty()
        verify(processor, never()).addExecutionListener(any())
        assertThat(scheduler.mInDefaultProcess).isFalse()
    }

    @Test
    fun cancel_isolationDisabled_defaultProcess_stopsWork() {
        val configuration = configuration(isolationEnabled = false, inDefaultProcess = true)
        val scheduler = greedyScheduler(configuration)
        val workSpec = unconstrainedWorkSpec()
        scheduler.schedule(workSpec)
        assertThat(runnableScheduler.runnables).hasSize(1) // TimeLimiter timeout runnable

        scheduler.cancel(workSpec.id)

        assertThat(workLauncher.stoppedWorkSpecIds).containsExactly(workSpec.id)
        assertThat(runnableScheduler.runnables).isEmpty()
        assertThat(scheduler.mInDefaultProcess).isTrue()
    }

    @Test
    fun cancel_isolationDisabled_secondaryProcess_ignoresCancellation() {
        val configuration = configuration(isolationEnabled = false, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)
        val delayedWorkTracker =
            DelayedWorkTracker(scheduler, runnableScheduler, configuration.clock)
        scheduler.setDelayedWorkTracker(delayedWorkTracker)
        val workSpec = unconstrainedWorkSpec(initialDelayMillis = 1_000L)
        delayedWorkTracker.schedule(workSpec, FIXED_TIME_MILLIS + 1_000L)
        check(scheduler.mInDefaultProcess == null && runnableScheduler.runnables.size == 1) {
            "fixture: expected unresolved default-process state and one pending delayed runnable"
        }

        scheduler.cancel(workSpec.id)

        verify(processor, never()).addExecutionListener(any())
        assertThat(workLauncher.stoppedWorkSpecIds).isEmpty()
        assertThat(runnableScheduler.runnables).hasSize(1)
        assertThat(scheduler.mInDefaultProcess).isFalse()
    }

    @Test
    fun schedule_isolationEnabled_secondaryProcess_startsWork() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)
        val workSpec = unconstrainedWorkSpec()

        scheduler.schedule(workSpec)

        assertThat(workLauncher.startedWorkSpecIds).containsExactly(workSpec.id)
        verify(processor).addExecutionListener(scheduler)
    }

    @Test
    fun schedule_isolationEnabled_defaultProcess_startsWorkWithoutResolvingProcess() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = true)
        val scheduler = greedyScheduler(configuration)
        val workSpec = unconstrainedWorkSpec()

        scheduler.schedule(workSpec)

        assertThat(workLauncher.startedWorkSpecIds).containsExactly(workSpec.id)
        // Verifies isolated mode short-circuits before ProcessUtils.isDefaultProcess() (avoiding
        // reflection / ActivityManager IPC on API < 28 even when running in the default process).
        assertThat(scheduler.mInDefaultProcess).isNull()
    }

    @Test
    fun schedule_isolationEnabled_ignoresStaleDefaultProcessState() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)
        // Unreachable in production (the isolation path never writes mInDefaultProcess); guards
        // that the isolation check takes precedence over the cached default-process state (e.g. if
        // someone reorders the isolation check after the cached check, work in secondary processes
        // would be silently ignored even when isolation is enabled).
        scheduler.mInDefaultProcess = false
        val workSpec = unconstrainedWorkSpec()

        scheduler.schedule(workSpec)

        assertThat(workLauncher.startedWorkSpecIds).containsExactly(workSpec.id)
    }

    @Test
    fun cancel_isolationEnabled_secondaryProcess_stopsWork() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)
        val immediateWorkSpec = unconstrainedWorkSpec()
        val delayedWorkSpec = unconstrainedWorkSpec(initialDelayMillis = 1_000L)
        scheduler.schedule(immediateWorkSpec, delayedWorkSpec)
        assertThat(workLauncher.startedWorkSpecIds).containsExactly(immediateWorkSpec.id)
        // 1 TimeLimiter timeout runnable (immediateWorkSpec) + 1 DelayedWorkTracker runnable.
        assertThat(runnableScheduler.runnables).hasSize(2)

        scheduler.cancel(delayedWorkSpec.id)
        assertThat(runnableScheduler.runnables).hasSize(1) // TimeLimiter timeout runnable

        scheduler.cancel(immediateWorkSpec.id)
        assertThat(workLauncher.stoppedWorkSpecIds).containsExactly(immediateWorkSpec.id)
        assertThat(runnableScheduler.runnables).isEmpty()
    }

    @Test
    fun cancel_isolationEnabled_defaultProcess_cancelsWithoutResolvingProcess() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = true)
        val scheduler = greedyScheduler(configuration)
        val workSpec = unconstrainedWorkSpec()
        scheduler.schedule(workSpec)
        assertThat(runnableScheduler.runnables).hasSize(1) // TimeLimiter timeout runnable

        scheduler.cancel(workSpec.id)

        assertThat(workLauncher.stoppedWorkSpecIds).containsExactly(workSpec.id)
        assertThat(runnableScheduler.runnables).isEmpty()
        assertThat(scheduler.mInDefaultProcess).isNull()
    }

    @Test
    fun cancel_isolationEnabled_secondaryProcess_registersExecutionListener() {
        val configuration = configuration(isolationEnabled = true, inDefaultProcess = false)
        val scheduler = greedyScheduler(configuration)

        scheduler.cancel(unconstrainedWorkSpec().id)

        verify(processor).addExecutionListener(scheduler)
    }

    @Test
    fun createSchedulers_isolationEnabled_honorsGreedySchedulerEnabled() {
        // Characterization test: createSchedulers does not read isProcessIsolationEnabled today,
        // guarding that isolated mode continues to honor Configuration.isGreedySchedulerEnabled.
        val enabledConfig =
            configuration(
                isolationEnabled = true,
                inDefaultProcess = false,
                greedySchedulerEnabled = true,
            )
        withTestWorkManager(enabledConfig) { workManager ->
            assertThat(workManager.schedulers.any { it is GreedyScheduler }).isTrue()
        }

        val disabledConfig =
            configuration(
                isolationEnabled = true,
                inDefaultProcess = false,
                greedySchedulerEnabled = false,
            )
        withTestWorkManager(disabledConfig) { workManager ->
            assertThat(workManager.schedulers.any { it is GreedyScheduler }).isFalse()
        }
    }

    /**
     * Builds a [Configuration] whose `defaultProcessName` either matches the active process
     * ([inDefaultProcess] `== true`) or names a different process, simulating a secondary process.
     */
    private fun configuration(
        isolationEnabled: Boolean,
        inDefaultProcess: Boolean,
        greedySchedulerEnabled: Boolean = true,
    ): Configuration {
        val defaultProcessName =
            if (inDefaultProcess) context.packageName else "${context.packageName}:other_default"
        val configuration =
            Configuration.Builder()
                .setDefaultProcessName(defaultProcessName)
                .setProcessIsolationEnabled(isolationEnabled)
                .setGreedySchedulerEnabled(greedySchedulerEnabled)
                .setClock(fixedClock)
                .setRunnableScheduler(runnableScheduler)
                .setExecutor(directExecutor)
                .setTaskExecutor(directExecutor)
                .build()
        check(isDefaultProcess(context, configuration) == inDefaultProcess) {
            "fixture: unexpected isDefaultProcess result"
        }
        return configuration
    }

    private fun greedyScheduler(configuration: Configuration): GreedyScheduler {
        val taskExecutor = WorkManagerTaskExecutor(directExecutor)
        return GreedyScheduler(
            context,
            configuration,
            Trackers(context, taskExecutor),
            processor,
            workLauncher,
            taskExecutor,
        )
    }

    private inline fun withTestWorkManager(
        configuration: Configuration,
        block: (WorkManagerImpl) -> Unit,
    ) {
        val taskExecutor = WorkManagerTaskExecutor(directExecutor)
        val workManager = TestWorkManagerImpl(context, configuration, taskExecutor)
        try {
            block(workManager)
        } finally {
            workManager.close()
            clearManifestDefaultCache()
        }
    }

    private fun unconstrainedWorkSpec(initialDelayMillis: Long = 0L): WorkSpec =
        OneTimeWorkRequest.Builder(NoOpWorker::class.java)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
            .workSpec
            .apply { lastEnqueueTime = FIXED_TIME_MILLIS }

    private class RecordingWorkLauncher : WorkLauncher {
        val startedWorkSpecIds = mutableListOf<String>()
        val stoppedWorkSpecIds = mutableListOf<String>()

        override fun startWork(
            workSpecId: StartStopToken,
            runtimeExtras: WorkerParameters.RuntimeExtras?,
        ) {
            startedWorkSpecIds += workSpecId.id.workSpecId
        }

        override fun stopWork(workSpecId: StartStopToken, reason: Int) {
            stoppedWorkSpecIds += workSpecId.id.workSpecId
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

    private class NoOpWorker(context: Context, parameters: WorkerParameters) :
        Worker(context, parameters) {
        override fun doWork(): Result = Result.success()
    }

    companion object {
        private const val FIXED_TIME_MILLIS = 1_700_000_000_000L
    }
}
