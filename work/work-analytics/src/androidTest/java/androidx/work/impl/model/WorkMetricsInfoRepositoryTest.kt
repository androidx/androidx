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

package androidx.work.analytics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Clock
import androidx.work.Data
import androidx.work.ExperimentalEventsApi
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.analytics.impl.WorkMetricsDatabase
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Constants used across multiple tests to ensure consistency and avoid duplication. */
private const val TEST_WORKER_CLASS = "androidx.work.impl.workers.TestWorker"
private val TEST_TAGS = setOf("test-tag", "analytics")

/** A simple TestClock implementation that allows explicit control over the reported time. */
private class TestClock(var currentTime: Long = 1000L) : Clock {
    override fun currentTimeMillis(): Long = currentTime
}

@ExperimentalEventsApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class WorkMetricsInfoRepositoryTest {
    private lateinit var database: WorkMetricsDatabase
    private lateinit var repository: WorkMetricsInfoRepository
    private val testClock = TestClock()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkMetricsDatabase::class.java).build()
        repository = WorkMetricsInfoRepository(database, testClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun onEnqueued_updatesStateAndTimestamp() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)

        val results = repository.getWorkMetricsInfoById(workId)
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(workId, result.workSpecId)
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, result.state)
        assertEquals(1000L, result.enqueueTimeMillis)
    }

    @Test
    fun onEnqueued_duplicate_throwsException() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        repository.onEnqueued(workInfo)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.onEnqueued(workInfo) }
        }
    }

    @Test
    fun onEnqueued_blockedWork_updatesState() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, state = WorkInfo.State.BLOCKED)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)

        val results = repository.getWorkMetricsInfoById(workId)
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(workId, result.workSpecId)
        assertEquals(WorkMetricsInfo.State.ENQUEUED_BLOCKED, result.state)
        assertEquals(1000L, result.enqueueTimeMillis)
    }

    @Test
    fun onUnblocked_updatesState() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, state = WorkInfo.State.BLOCKED)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, result.state)
        assertEquals(2000L, result.unblockTimeMillis)
    }

    @Test
    fun onStarted_updatesStateToRunning() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.RUNNING, result.state)
        assertEquals(3000L, result.firstStartTimeMillis)
    }

    @Test
    fun onStarted_multipleCalls_preservesFirstStartTime() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onStopped(
            WorkInfo.STOP_REASON_UNKNOWN,
            workInfo.copy(state = WorkInfo.State.ENQUEUED),
        )

        testClock.currentTime = 5000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(3000L, result.firstStartTimeMillis)
        assertEquals(WorkMetricsInfo.State.RUNNING, result.state)
    }

    @Test
    fun onStopped_updatesStateToEnqueuedPending() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, generation = 0)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onStopped(
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY,
            workInfo.copy(state = WorkInfo.State.ENQUEUED),
        )

        val updatedInfos = repository.getWorkMetricsInfoById(workId)
        assertEquals(1, updatedInfos.size)
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, updatedInfos[0].state)
    }

    @Test
    fun onFinished_oneTimeSuccess_finalizesRecord() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onFinished(
            ListenableWorker.Result.success(),
            workInfo.copy(state = WorkInfo.State.SUCCEEDED),
        )

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.SUCCEEDED, result.state)
        assertEquals(4000L, result.finishTimeMillis)
    }

    @Test
    fun onFinished_oneTimeFailure_finalizesRecord() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onFinished(
            ListenableWorker.Result.failure(),
            workInfo.copy(state = WorkInfo.State.FAILED),
        )

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.FAILED, result.state)
        assertEquals(4000L, result.finishTimeMillis)
    }

    @Test
    fun onFinished_oneTimeRetry_resetsToPending() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onFinished(
            ListenableWorker.Result.retry(),
            workInfo.copy(state = WorkInfo.State.ENQUEUED),
        )

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, result.state)
    }

    @Test
    fun onFinished_periodicSuccess_createsNewRecord() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, isPeriodic = true)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onFinished(
            ListenableWorker.Result.success(),
            workInfo.copy(state = WorkInfo.State.SUCCEEDED),
        )

        val results = repository.getWorkMetricsInfoById(workId)
        assertEquals(2, results.size)

        val finishedPeriod = results.find { it.state == WorkMetricsInfo.State.SUCCEEDED }!!
        assertEquals(4000L, finishedPeriod.finishTimeMillis)

        val currentPeriod = results.find { it.state == WorkMetricsInfo.State.ENQUEUED_PENDING }!!
        assertEquals(4000L, currentPeriod.enqueueTimeMillis)
    }

    @Test
    fun onFinished_periodicFailure_createsNewRecord() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, isPeriodic = true)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onFinished(
            ListenableWorker.Result.failure(),
            workInfo.copy(state = WorkInfo.State.FAILED),
        )

        val results = repository.getWorkMetricsInfoById(workId)
        assertEquals(2, results.size)

        val finishedPeriod = results.find { it.state == WorkMetricsInfo.State.FAILED }!!
        assertEquals(4000L, finishedPeriod.finishTimeMillis)

        val currentPeriod = results.find { it.state == WorkMetricsInfo.State.ENQUEUED_PENDING }!!
        assertEquals(4000L, currentPeriod.enqueueTimeMillis)
    }

    @Test
    fun onCancelled_finalizesRecord() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)

        testClock.currentTime = 3000L
        repository.onCancelled(workInfo.copy(state = WorkInfo.State.CANCELLED))

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.CANCELLED, result.state)
        assertEquals(3000L, result.finishTimeMillis)
    }

    @Test
    fun onUpdated_finalizesOldAsObsoleteAndInsertsNew() = runTest {
        val workId = UUID.randomUUID()
        var workInfoGen0 =
            createTestWorkInfo(id = workId, generation = 0, state = WorkInfo.State.BLOCKED)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfoGen0)

        val enqueuedResult = repository.getWorkMetricsInfoById(workId)
        assertEquals(WorkMetricsInfo.State.ENQUEUED_BLOCKED, enqueuedResult[0].state)

        testClock.currentTime = 2000L
        workInfoGen0 = workInfoGen0.copy(state = WorkInfo.State.ENQUEUED)
        repository.onUnblocked(workInfoGen0)

        testClock.currentTime = 3000L
        val workInfoGen1 = workInfoGen0.copy(generation = 1)
        repository.onUpdated(workInfoGen0, workInfoGen1)

        val results = repository.getWorkMetricsInfoById(workId)
        assertEquals(2, results.size)

        val oldRecord = results.find { it.generation == 0 }!!
        assertEquals(WorkMetricsInfo.State.OBSOLETE_UPDATED, oldRecord.state)
        assertEquals(3000L, oldRecord.finishTimeMillis)

        val newRecord = results.find { it.generation == 1 }!!
        assertEquals(WorkMetricsInfo.State.ENQUEUED_PENDING, newRecord.state)
        assertEquals(3000L, newRecord.enqueueTimeMillis)
        assertEquals(3000L, newRecord.unblockTimeMillis)
    }

    @Test
    fun onException_finalizesRecordAsFailed() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)
        testClock.currentTime = 2000L
        repository.onUnblocked(workInfo)
        testClock.currentTime = 3000L
        repository.onStarted(workInfo.copy(state = WorkInfo.State.RUNNING))

        testClock.currentTime = 4000L
        repository.onException(
            RuntimeException("test"),
            workInfo.copy(state = WorkInfo.State.FAILED),
        )

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.FAILED, result.state)
        assertEquals(4000L, result.finishTimeMillis)
    }

    @Test
    fun onPrerequisiteFailed_finalizesRecordAsFailed() = runTest {
        val workId = UUID.randomUUID()
        val workInfo = createTestWorkInfo(id = workId, state = WorkInfo.State.BLOCKED)

        testClock.currentTime = 1000L
        repository.onEnqueued(workInfo)

        testClock.currentTime = 2000L
        repository.onPrerequisiteFailed(workInfo.copy(state = WorkInfo.State.FAILED))

        val result = repository.getWorkMetricsInfoById(workId)[0]
        assertEquals(WorkMetricsInfo.State.FAILED, result.state)
        assertEquals(2000L, result.finishTimeMillis)
    }

    private fun WorkInfo.copy(
        state: WorkInfo.State = this.state,
        generation: Int = this.generation,
    ): WorkInfo {
        return WorkInfo(
            id = this.id,
            state = state,
            tags = this.tags,
            outputData = this.outputData,
            progress = this.progress,
            runAttemptCount = this.runAttemptCount,
            generation = generation,
            constraints = this.constraints,
            initialDelayMillis = this.initialDelayMillis,
            periodicityInfo = this.periodicityInfo,
            nextScheduleTimeMillis = this.nextScheduleTimeMillis,
            stopReason = this.stopReason,
            workerClassName = this.workerClassName,
        )
    }

    private fun createTestWorkInfo(
        id: UUID,
        generation: Int = 0,
        state: WorkInfo.State = WorkInfo.State.ENQUEUED,
        isPeriodic: Boolean = false,
        tags: Set<String> = TEST_TAGS,
        workerClass: String = TEST_WORKER_CLASS,
    ): WorkInfo {
        val periodicityInfo =
            if (isPeriodic) {
                WorkInfo.PeriodicityInfo(
                    repeatIntervalMillis = 15 * 60 * 1000,
                    flexIntervalMillis = 15 * 60 * 1000,
                )
            } else null

        return WorkInfo(
            id = id,
            state = state,
            tags = tags,
            outputData = Data.EMPTY,
            progress = Data.EMPTY,
            runAttemptCount = 0,
            generation = generation,
            workerClassName = workerClass,
            periodicityInfo = periodicityInfo,
        )
    }
}
