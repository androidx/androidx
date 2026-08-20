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
import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.RequiresApi
import androidx.room.Room
import androidx.work.Clock
import androidx.work.ExecutionEventListener
import androidx.work.ExperimentalEventsApi
import androidx.work.ListenableWorker
import androidx.work.Logger
import androidx.work.ScheduleEventListener
import androidx.work.WorkInfo
import androidx.work.analytics.impl.WorkMetricsDatabase
import androidx.work.analytics.impl.model.WorkMetricsSpec
import androidx.work.analytics.impl.model.getWorkMetricsInfos
import androidx.work.analytics.impl.utils.toRawQuery
import java.time.Duration
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.jvm.JvmOverloads
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val WORK_METRICS_DB_NAME = "androidx.work.analytics.workmetricsdb"
private val TAG = Logger.tagWithPrefix("WorkMetricsInfoRepository")
private val CLEANUP_INTERVAL_MILLIS = 1.days.inWholeMilliseconds

/** Repository class that calculates and stores metrics info and analytics about workers. */
@ExperimentalEventsApi
@ExperimentalWorkMetricsApi
public class WorkMetricsInfoRepository
internal constructor(
    private val database: WorkMetricsDatabase,
    private val clock: Clock,
    retentionTimeMillis: Long = DEFAULT_RETENTION_TIME_MILLIS,
) : ScheduleEventListener, ExecutionEventListener {

    private val retentionTimeMillis: Long
    private val dao = database.workMetricsSpecDao()
    private val lastCleanupTime = AtomicLong(0)

    init {
        val clamped =
            if (retentionTimeMillis > MAX_RETENTION_TIME_MILLIS) {
                Logger.get()
                    .warning(
                        TAG,
                        "Retention time $retentionTimeMillis ms exceeds maximum allowed " +
                            "$MAX_RETENTION_TIME_MILLIS ms. Clamping to maximum.",
                    )
                MAX_RETENTION_TIME_MILLIS
            } else if (retentionTimeMillis <= 0) {
                throw IllegalArgumentException(
                    "Retention time must be positive: $retentionTimeMillis"
                )
            } else {
                retentionTimeMillis
            }
        this.retentionTimeMillis = clamped
    }

    /**
     * Creates an instance of [WorkMetricsInfoRepository] with a default retention of
     * [DEFAULT_RETENTION_TIME_MILLIS].
     *
     * It is recommended that the [Executor] passed here is the same as the one passed into
     * [androidx.work.Configuration.Builder.setTaskExecutor]. If no executor is provided, Room's
     * default executors will be used instead.
     *
     * Note that this executor is only used for database operations and not for running the event
     * hooks themselves.
     *
     * @param context The application [Context].
     * @param dbExecutor The [Executor] passed to Room on which database queries and transactions
     *   will be run.
     */
    @JvmOverloads
    public constructor(
        context: Context,
        dbExecutor: Executor? = null,
    ) : this(
        createDatabase(context, dbExecutor),
        Clock { System.currentTimeMillis() },
        DEFAULT_RETENTION_TIME_MILLIS,
    )

    private val finishedMetricsInfos = MutableSharedFlow<WorkMetricsInfo>(extraBufferCapacity = 64)

    /**
     * Creates an instance of [WorkMetricsInfoRepository] with a custom retention time.
     *
     * @param context The application [Context].
     * @param retentionTime The retention time. Clamped to a maximum of [MAX_RETENTION_TIME_MILLIS].
     *   Must be positive.
     * @param retentionTimeUnit The [TimeUnit] for [retentionTime].
     * @param dbExecutor The [Executor] passed to Room on which database queries and transactions
     *   will be run.
     */
    @JvmOverloads
    public constructor(
        context: Context,
        retentionTime: Long,
        retentionTimeUnit: TimeUnit,
        dbExecutor: Executor? = null,
    ) : this(
        createDatabase(context, dbExecutor),
        Clock { System.currentTimeMillis() },
        retentionTimeUnit.toMillis(retentionTime),
    )

    /**
     * Creates an instance of [WorkMetricsInfoRepository] with a custom retention duration.
     *
     * @param context The application [Context].
     * @param retentionDuration The retention duration. Clamped to a maximum of
     *   [MAX_RETENTION_TIME_MILLIS]. Must be positive.
     * @param dbExecutor The [Executor] passed to Room on which database queries and transactions
     *   will be run.
     */
    @RequiresApi(26)
    @JvmOverloads
    public constructor(
        context: Context,
        retentionDuration: Duration,
        dbExecutor: Executor? = null,
    ) : this(context, retentionDuration.toMillis(), TimeUnit.MILLISECONDS, dbExecutor)

    private val lastStartTimes = Collections.synchronizedMap(mutableMapOf<String, Long>())
    private val pendingUpdates = Collections.synchronizedMap(mutableMapOf<String, WorkInfo>())

    /**
     * A hot [Flow] that emits a [WorkMetricsInfo] whenever one finishes.
     *
     * A [WorkMetricsInfo] is considered finished when the request period is complete or obsolete,
     * either because the work finished or a new request period started (e.g. if the work request is
     * updated or a periodic request completes a period).
     */
    public val finishedWorkMetricsInfoFlow: Flow<WorkMetricsInfo> = finishedMetricsInfos

    /**
     * Gets a list of [WorkMetricsInfo] snapshots for a given work id.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @return A list of [WorkMetricsInfo] records associated with the [workId].
     */
    public suspend fun getWorkMetricsInfoById(workId: UUID): List<WorkMetricsInfo> {
        return dao.getWorkMetricsInfos(workId.toString())
    }

    /**
     * Gets a list of [WorkMetricsInfo] matching the query criteria.
     *
     * @param query The [WorkMetricsQuery] containing filter parameters.
     * @return A list of [WorkMetricsInfo] records matching the query parameters.
     */
    public suspend fun getWorkMetricsInfos(query: WorkMetricsQuery): List<WorkMetricsInfo> {
        return dao.getWorkMetricsInfos(query.toRawQuery())
    }

    override suspend fun onEnqueued(workInfo: WorkInfo) {
        val spec = workInfo.toWorkMetricsSpec()
        try {
            insertWorkMetricsSpec(spec, workInfo.tags)
        } catch (e: SQLiteConstraintException) {
            throw IllegalStateException(
                "Active WorkMetricsSpec already exists for work ${workInfo.id} " +
                    "generation ${workInfo.generation} period ${spec.periodCount}",
                e,
            )
        }
    }

    override suspend fun onUpdated(oldWorkInfo: WorkInfo, updatedWorkInfo: WorkInfo) {
        val id = oldWorkInfo.id.toString()
        val currentTime = clock.currentTimeMillis()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(oldWorkInfo, finishedInfos, "onUpdated")
                    ?: return@runInTransaction
            val isRunning = spec.state == WorkMetricsInfo.State.RUNNING
            if (!isRunning) {
                lastStartTimes.remove(id)
                pendingUpdates.remove(id)
                markObsoleteAndInsertUpdated(spec, updatedWorkInfo, currentTime, finishedInfos)
            } else {
                pendingUpdates[id] = updatedWorkInfo
            }
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onUnblocked(workInfo: WorkInfo) {
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onUnblocked")
                    ?: return@runInTransaction
            dao.setUnblockTime(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                unblockTime = clock.currentTimeMillis(),
            )
            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onCancelled(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onCancelled")
                    ?: return@runInTransaction
            lastStartTimes.remove(id)
            finalizeSpecAndCollectInfo(
                spec,
                WorkMetricsInfo.State.CANCELLED,
                clock.currentTimeMillis(),
                finishedInfos,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onStarted(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onStarted")
                    ?: return@runInTransaction
            if (workInfo.runAttemptCount != spec.runAttemptCount + 1) {
                Logger.get()
                    .warning(
                        TAG,
                        "Run attempt count mismatch in onStarted for work ID $id. " +
                            "WorkInfo runAttemptCount: ${workInfo.runAttemptCount}, " +
                            "DB runAttemptCount: ${spec.runAttemptCount}",
                    )
            }
            val currentTime = clock.currentTimeMillis()
            lastStartTimes[spec.workSpecId] = currentTime
            if (spec.firstStartTimeMillis == WorkMetricsSpec.TIME_NOT_SET) {
                dao.setFirstStartTime(
                    workId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                    startTime = currentTime,
                )
            }

            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.RUNNING,
            )
            dao.setRunAttemptCount(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                runAttemptCount = workInfo.runAttemptCount,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onStopped(stopReason: Int, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onStopped")
                    ?: return@runInTransaction
            val currentTime = clock.currentTimeMillis()
            val duration = calculateExecutionDuration(spec, currentTime)
            dao.setWorkerDuration(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                workerDuration = duration,
            )
            dao.setTotalRuntime(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                totalRuntime = spec.totalRuntimeMillis + duration,
            )
            val pendingUpdate = pendingUpdates.remove(id)
            if (pendingUpdate != null) {
                markObsoleteAndInsertUpdated(spec, pendingUpdate, currentTime, finishedInfos)
            } else {
                dao.setState(
                    workId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                    state = WorkMetricsInfo.State.ENQUEUED_PENDING,
                )
            }
            val currentCounts = spec.stopReasonCounts
            val updatedCounts =
                currentCounts.toMutableMap().apply {
                    this[stopReason] = (this[stopReason] ?: 0) + 1
                }
            dao.setStopReasonCounts(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                stopReasonCounts = updatedCounts,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onFinished(result: ListenableWorker.Result, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val currentTime = clock.currentTimeMillis()
        val isPeriodic = workInfo.periodicityInfo != null
        val finishedInfos = mutableListOf<WorkMetricsInfo>()

        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onFinished")
                    ?: return@runInTransaction

            val duration = calculateExecutionDuration(spec, currentTime)
            dao.setTotalRuntime(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                totalRuntime = spec.totalRuntimeMillis + duration,
            )
            val pendingUpdate = pendingUpdates.remove(id)

            if (result is ListenableWorker.Result.Retry) {
                if (pendingUpdate != null) {
                    markObsoleteAndInsertUpdated(spec, pendingUpdate, currentTime, finishedInfos)
                } else {
                    dao.setState(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                        state = WorkMetricsInfo.State.ENQUEUED_PENDING,
                    )
                    dao.incrementExplicitRetryCount(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                    )
                }
            } else {
                dao.setWorkerDuration(
                    workId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                    workerDuration = duration,
                )
                val state =
                    when (result) {
                        is ListenableWorker.Result.Success -> WorkMetricsInfo.State.SUCCEEDED
                        is ListenableWorker.Result.Failure -> WorkMetricsInfo.State.FAILED
                        else -> throw IllegalArgumentException("Unknown result: $result")
                    }
                finalizeSpecAndCollectInfo(spec, state, currentTime, finishedInfos)

                if (isPeriodic) {
                    val nextWorkInfo = pendingUpdate ?: workInfo
                    val newSpec = nextWorkInfo.toWorkMetricsSpec(periodCount = spec.periodCount + 1)
                    newSpec.state = WorkMetricsInfo.State.ENQUEUED_PENDING
                    insertWorkMetricsSpec(newSpec, nextWorkInfo.tags)
                }
            }
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onException(throwable: Throwable, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onException")
                    ?: return@runInTransaction
            val currentTime = clock.currentTimeMillis()
            val duration = calculateExecutionDuration(spec, currentTime)
            pendingUpdates.remove(id)
            dao.setWorkerDuration(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                workerDuration = duration,
            )
            dao.setTotalRuntime(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                totalRuntime = spec.totalRuntimeMillis + duration,
            )
            finalizeSpecAndCollectInfo(
                spec,
                WorkMetricsInfo.State.FAILED,
                currentTime,
                finishedInfos,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    override suspend fun onPrerequisiteFailed(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val finishedInfos = mutableListOf<WorkMetricsInfo>()
        database.runInTransaction {
            val spec =
                resolveAndReconcileSpec(workInfo, finishedInfos, "onPrerequisiteFailed")
                    ?: return@runInTransaction
            pendingUpdates.remove(id)
            finalizeSpecAndCollectInfo(
                spec,
                WorkMetricsInfo.State.FAILED,
                clock.currentTimeMillis(),
                finishedInfos,
            )
        }
        finishedInfos.forEach { finishedMetricsInfos.emit(it) }
    }

    private fun calculateExecutionDuration(spec: WorkMetricsSpec, currentTime: Long): Long {
        val lastStartTime = lastStartTimes.remove(spec.workSpecId)
        return if (lastStartTime != null) {
            currentTime - lastStartTime
        } else {
            0L
        }
    }

    /**
     * Resolves the current active [WorkMetricsSpec] for the given [workInfo], reconciling any
     * orphaned older-generation records left in an active state after process death.
     *
     * Under our strict 1-active-invariant architecture, at most one record with state not in
     * `COMPLETED_STATES` exists at any time. If an older-generation record is found (e.g., due to a
     * crash mid-run before the updated generation was inserted), it is finalized as
     * [WorkMetricsInfo.State.OBSOLETE_UPDATED] and a new [WorkMetricsSpec] for [workInfo] is
     * created and inserted.
     */
    private fun resolveAndReconcileSpec(
        workInfo: WorkInfo,
        finishedInfos: MutableList<WorkMetricsInfo>,
        hookName: String,
    ): WorkMetricsSpec? {
        val id = workInfo.id.toString()
        val currentSpec = dao.getCurrentWorkMetricsSpec(id)
        if (currentSpec != null) {
            if (currentSpec.generation < workInfo.generation) {
                val currentTime = clock.currentTimeMillis()
                lastStartTimes.remove(currentSpec.workSpecId)
                pendingUpdates.remove(currentSpec.workSpecId)
                return markObsoleteAndInsertUpdated(
                    currentSpec,
                    workInfo,
                    currentTime,
                    finishedInfos,
                )
            }
            if (currentSpec.generation > workInfo.generation) {
                Logger.get()
                    .warning(
                        TAG,
                        "Generation mismatch in $hookName for work ID $id. " +
                            "DB generation: ${currentSpec.generation}, " +
                            "Event generation: ${workInfo.generation}",
                    )
                return null
            }
            return currentSpec
        }
        Logger.get()
            .warning(
                TAG,
                "Expected an active WorkMetricsSpec for work ID $id in $hookName, " +
                    "but none was found.",
            )
        val spec = workInfo.toWorkMetricsSpec()
        insertWorkMetricsSpec(spec, workInfo.tags)
        return spec
    }

    private fun markObsoleteAndInsertUpdated(
        spec: WorkMetricsSpec,
        updatedWorkInfo: WorkInfo,
        currentTime: Long,
        finishedInfos: MutableList<WorkMetricsInfo>,
    ): WorkMetricsSpec {
        finalizeSpecAndCollectInfo(
            spec,
            WorkMetricsInfo.State.OBSOLETE_UPDATED,
            currentTime,
            finishedInfos,
        )
        return insertUpdatedSpec(updatedWorkInfo, currentTime)
    }

    private fun finalizeSpecAndCollectInfo(
        spec: WorkMetricsSpec,
        state: WorkMetricsInfo.State,
        currentTime: Long,
        finishedInfos: MutableList<WorkMetricsInfo>,
    ) {
        dao.setFinishTime(
            workId = spec.workSpecId,
            generation = spec.generation,
            periodCount = spec.periodCount,
            finishTime = currentTime,
        )
        dao.setState(
            workId = spec.workSpecId,
            generation = spec.generation,
            periodCount = spec.periodCount,
            state = state,
        )
        dao.getWorkMetricsInfo(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
            )
            ?.let { finishedInfos.add(it) }
    }

    private fun insertUpdatedSpec(updatedWorkInfo: WorkInfo, currentTime: Long): WorkMetricsSpec {
        val updatedSpec = updatedWorkInfo.toWorkMetricsSpec()
        if (updatedWorkInfo.state == WorkInfo.State.BLOCKED) {
            // Preserve ENQUEUED_BLOCKED mapped by toWorkMetricsSpec()
        } else {
            // When work is updated mid-run, the snapshot of updatedWorkInfo received in onUpdated
            // can retain the RUNNING state from the active generation 0 attempt. Since generation 1
            // has not started executing yet, we reset its initial state to ENQUEUED_PENDING and
            // record its unblock time since it is unblocked and ready to run.
            updatedSpec.state = WorkMetricsInfo.State.ENQUEUED_PENDING
            updatedSpec.unblockTimeMillis = currentTime
        }
        insertWorkMetricsSpec(updatedSpec, updatedWorkInfo.tags)
        return updatedSpec
    }

    private fun insertWorkMetricsSpec(spec: WorkMetricsSpec, tags: Set<String>) {
        spec.enqueueTimeMillis = clock.currentTimeMillis()
        dao.insertWorkMetricsSpec(spec, tags)
        pruneOldMetricsIfNecessary()
    }

    private fun pruneOldMetricsIfNecessary() {
        val currentTime = clock.currentTimeMillis()
        val lastCleanup = lastCleanupTime.get()
        if (currentTime - lastCleanup < CLEANUP_INTERVAL_MILLIS) {
            return
        }
        if (lastCleanupTime.compareAndSet(lastCleanup, currentTime)) {
            val threshold = currentTime - retentionTimeMillis
            dao.deleteFinishedSpecsOlderThan(threshold)
        }
    }

    internal fun WorkInfo.toWorkMetricsSpec(periodCount: Int = 0): WorkMetricsSpec {
        return WorkMetricsSpec(
            workSpecId = this.id.toString(),
            generation = this.generation,
            periodCount = periodCount,
            workerClassName = this.workerClassName,
            state = this.state.toWorkMetricsState(),
        )
    }

    private fun WorkInfo.State.toWorkMetricsState(): WorkMetricsInfo.State {
        return when (this) {
            WorkInfo.State.ENQUEUED -> WorkMetricsInfo.State.ENQUEUED_PENDING
            WorkInfo.State.RUNNING -> WorkMetricsInfo.State.RUNNING
            WorkInfo.State.SUCCEEDED -> WorkMetricsInfo.State.SUCCEEDED
            WorkInfo.State.FAILED -> WorkMetricsInfo.State.FAILED
            WorkInfo.State.BLOCKED -> WorkMetricsInfo.State.ENQUEUED_BLOCKED
            WorkInfo.State.CANCELLED -> WorkMetricsInfo.State.CANCELLED
        }
    }

    public companion object {
        /** The default retention time for finished work metrics (7 days). */
        @JvmField public val DEFAULT_RETENTION_TIME_MILLIS: Long = 7.days.inWholeMilliseconds

        /** The maximum allowed retention time for finished work metrics (30 days). */
        @JvmField public val MAX_RETENTION_TIME_MILLIS: Long = 30.days.inWholeMilliseconds

        private fun createDatabase(context: Context, dbExecutor: Executor?): WorkMetricsDatabase {
            val builder =
                Room.databaseBuilder(
                        context.applicationContext,
                        WorkMetricsDatabase::class.java,
                        WORK_METRICS_DB_NAME,
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
            if (dbExecutor != null) {
                builder.setQueryExecutor(dbExecutor)
                builder.setTransactionExecutor(dbExecutor)
            }
            return builder.build()
        }
    }
}
