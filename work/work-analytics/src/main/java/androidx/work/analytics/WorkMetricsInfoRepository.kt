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
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val WORK_METRICS_DB_NAME = "androidx.work.analytics.workmetricsdb"
private val TAG = Logger.tagWithPrefix("WorkMetricsInfoRepository")

/** Repository class that calculates and stores metrics info and analytics about workers. */
@ExperimentalEventsApi
@ExperimentalWorkMetricsApi
public class WorkMetricsInfoRepository
internal constructor(private val database: WorkMetricsDatabase, private val clock: Clock) :
    ScheduleEventListener, ExecutionEventListener {

    /**
     * Creates an instance of [WorkMetricsInfoRepository].
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
    ) : this(createDatabase(context, dbExecutor), Clock { System.currentTimeMillis() })

    private val finishedMetricsInfos = MutableSharedFlow<WorkMetricsInfo>(extraBufferCapacity = 64)

    private val dao = database.workMetricsSpecDao()

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
        return dao.getWorkMetricsSpecs(workId.toString()).map { it.toWorkMetricsInfo() }
    }

    override suspend fun onEnqueued(workInfo: WorkInfo) {
        val spec = workInfo.toWorkMetricsSpec()
        try {
            insertWorkMetricsSpec(spec)
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
        var finishedMetricsInfo: WorkMetricsInfo? = null
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, oldWorkInfo, "onUpdated")) {
                return@runInTransaction
            }
            dao.setFinishTime(
                workId = spec!!.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                finishTime = currentTime,
            )
            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.OBSOLETE_UPDATED,
            )
            finishedMetricsInfo =
                dao.getWorkMetricsSpec(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                    )!!
                    .toWorkMetricsInfo()

            var updatedSpec = updatedWorkInfo.toWorkMetricsSpec()
            if (updatedWorkInfo.state == WorkInfo.State.ENQUEUED) {
                updatedSpec.unblockTimeMillis = currentTime
            }
            insertWorkMetricsSpec(updatedSpec)
        }
        if (finishedMetricsInfo != null) {
            finishedMetricsInfos.emit(finishedMetricsInfo)
        }
    }

    override suspend fun onUnblocked(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onUnblocked")) {
                return@runInTransaction
            }
            dao.setUnblockTime(
                workId = spec!!.workSpecId,
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
    }

    override suspend fun onCancelled(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        var finishedMetricsInfo: WorkMetricsInfo? = null
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onCancelled")) {
                return@runInTransaction
            }
            dao.setFinishTime(
                workId = spec!!.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                finishTime = clock.currentTimeMillis(),
            )
            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.CANCELLED,
            )
            finishedMetricsInfo =
                dao.getWorkMetricsSpec(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                    )!!
                    .toWorkMetricsInfo()
        }
        if (finishedMetricsInfo != null) {
            finishedMetricsInfos.emit(finishedMetricsInfo)
        }
    }

    override suspend fun onStarted(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onStarted")) {
                return@runInTransaction
            }
            if (workInfo.runAttemptCount != spec!!.runAttemptCount + 1) {
                Logger.get()
                    .warning(
                        TAG,
                        "Run attempt count mismatch in onStarted for work ID $id. " +
                            "WorkInfo runAttemptCount: ${workInfo.runAttemptCount}, " +
                            "DB runAttemptCount: ${spec.runAttemptCount}",
                    )
            }
            if (spec!!.firstStartTimeMillis == WorkMetricsSpec.TIME_NOT_SET) {
                dao.setFirstStartTime(
                    workId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                    startTime = clock.currentTimeMillis(),
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
    }

    override suspend fun onStopped(stopReason: Int, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onStopped")) {
                return@runInTransaction
            }
            dao.setState(
                workId = spec!!.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.ENQUEUED_PENDING,
            )
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
    }

    override suspend fun onFinished(result: ListenableWorker.Result, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        val currentTime = clock.currentTimeMillis()
        val isPeriodic = workInfo.periodicityInfo != null
        var finishedMetricsInfo: WorkMetricsInfo? = null

        database.runInTransaction {
            val state =
                when (result) {
                    is ListenableWorker.Result.Success -> WorkMetricsInfo.State.SUCCEEDED
                    is ListenableWorker.Result.Failure -> WorkMetricsInfo.State.FAILED
                    is ListenableWorker.Result.Retry -> WorkMetricsInfo.State.ENQUEUED_PENDING
                    else -> throw IllegalArgumentException("Unknown result: $result")
                }

            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onFinished")) {
                return@runInTransaction
            }

            if (result is ListenableWorker.Result.Retry) {
                dao.setState(
                    workId = spec!!.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                    state = state,
                )
                dao.incrementExplicitRetryCount(
                    workId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                )
            } else {
                dao.setFinishTime(
                    workId = spec!!.workSpecId,
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
                finishedMetricsInfo =
                    dao.getWorkMetricsSpec(
                            workId = spec.workSpecId,
                            generation = spec.generation,
                            periodCount = spec.periodCount,
                        )!!
                        .toWorkMetricsInfo()

                if (isPeriodic) {
                    var newSpec = workInfo.toWorkMetricsSpec(periodCount = spec.periodCount + 1)
                    newSpec.state = WorkMetricsInfo.State.ENQUEUED_PENDING
                    insertWorkMetricsSpec(newSpec)
                }
            }
        }
        if (finishedMetricsInfo != null) {
            finishedMetricsInfos.emit(finishedMetricsInfo)
        }
    }

    override suspend fun onException(throwable: Throwable, workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        var finishedMetricsInfo: WorkMetricsInfo? = null
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onException")) {
                return@runInTransaction
            }
            dao.setFinishTime(
                workId = spec!!.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                finishTime = clock.currentTimeMillis(),
            )
            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.FAILED,
            )
            finishedMetricsInfo =
                dao.getWorkMetricsSpec(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                    )!!
                    .toWorkMetricsInfo()
        }
        if (finishedMetricsInfo != null) {
            finishedMetricsInfos.emit(finishedMetricsInfo)
        }
    }

    override suspend fun onPrerequisiteFailed(workInfo: WorkInfo) {
        val id = workInfo.id.toString()
        var finishedMetricsInfo: WorkMetricsInfo? = null
        database.runInTransaction {
            val spec = dao.getCurrentWorkMetricsSpec(id)
            if (!checkCurrentMetricsSpec(spec, workInfo, "onPrerequisiteFailed")) {
                return@runInTransaction
            }
            dao.setFinishTime(
                workId = spec!!.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                finishTime = clock.currentTimeMillis(),
            )
            dao.setState(
                workId = spec.workSpecId,
                generation = spec.generation,
                periodCount = spec.periodCount,
                state = WorkMetricsInfo.State.FAILED,
            )
            finishedMetricsInfo =
                dao.getWorkMetricsSpec(
                        workId = spec.workSpecId,
                        generation = spec.generation,
                        periodCount = spec.periodCount,
                    )!!
                    .toWorkMetricsInfo()
        }
        if (finishedMetricsInfo != null) {
            finishedMetricsInfos.emit(finishedMetricsInfo)
        }
    }

    private fun insertWorkMetricsSpec(spec: WorkMetricsSpec) {
        spec.enqueueTimeMillis = clock.currentTimeMillis()
        dao.insertWorkMetricsSpec(spec)
    }

    private fun checkCurrentMetricsSpec(
        spec: WorkMetricsSpec?,
        workInfo: WorkInfo,
        hookName: String,
    ): Boolean {
        val id = workInfo.id.toString()
        if (spec == null) {
            // Although this is expected for the "update during running" case since any hook after
            // onStarted (e.g., onFinished, onStopped, onException) will be with the later
            // generation, we log a warning here to track unexpected missing entries.
            // TODO (b/511074795): Properly handle update while running.
            Logger.get()
                .warning(
                    TAG,
                    "Expected an active WorkMetricsSpec for work ID $id in $hookName, " +
                        "but none was found.",
                )
            return false
        }
        if (spec.generation != workInfo.generation) {
            Logger.get()
                .warning(
                    TAG,
                    "Generation mismatch in $hookName for work ID $id. " +
                        "DB generation: ${spec.generation}, " +
                        "Event generation: ${workInfo.generation}",
                )
            return false
        }
        return true
    }

    internal fun WorkInfo.toWorkMetricsSpec(periodCount: Int = 0): WorkMetricsSpec {
        return WorkMetricsSpec(
            workSpecId = this.id.toString(),
            generation = this.generation,
            periodCount = periodCount,
            workerClassName = this.workerClassName,
            state = this.state.toWorkMetricsState(),
            tags = this.tags.toList(),
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
