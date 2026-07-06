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

package androidx.work.analytics.impl.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.work.analytics.WorkMetricsInfo
import androidx.work.analytics.impl.model.WorkMetricsInfoStateStrings.COMPLETED_STATES

internal object WorkMetricsInfoStateStrings {
    const val SUCCEEDED = "SUCCEEDED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
    const val OBSOLETE_UPDATED = "OBSOLETE_UPDATED"

    const val COMPLETED_STATES = "('$SUCCEEDED', '$FAILED', '$CANCELLED', '$OBSOLETE_UPDATED')"
}

/**
 * Database Access Object (DAO) for [WorkMetricsSpec] operations.
 *
 * This DAO provides methods to insert, retrieve, and update information regarding work requests. At
 * any given time, a unique [androidx.work.WorkRequest] should have at most one "current active"
 * [WorkMetricsSpec] entry.
 */
@Dao
internal interface WorkMetricsSpecDao {
    /**
     * Inserts a new [WorkMetricsSpec] record into the database along with its associated tags in a
     * single transaction.
     *
     * @param spec The [WorkMetricsSpec] to be inserted.
     * @param tags The set of tag strings associated with the work request.
     */
    @Transaction
    fun insertWorkMetricsSpec(spec: WorkMetricsSpec, tags: Set<String> = emptySet()) {
        insertWorkMetricsSpec(spec)
        val tagsToInsert =
            tags.map { tag ->
                WorkMetricsTag(
                    tag = tag,
                    workSpecId = spec.workSpecId,
                    generation = spec.generation,
                    periodCount = spec.periodCount,
                )
            }
        if (tagsToInsert.isNotEmpty()) {
            insertWorkMetricsTags(tagsToInsert)
        }
    }

    /**
     * Inserts a new [WorkMetricsSpec] record into the database.
     *
     * @param spec The [WorkMetricsSpec] to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertWorkMetricsSpec(spec: WorkMetricsSpec)

    /**
     * Inserts [WorkMetricsTag] records into the database.
     *
     * @param tags The list of [WorkMetricsTag]s to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertWorkMetricsTags(tags: List<WorkMetricsTag>)

    /**
     * Retrieves all [WorkMetricsSpec] and tags associated with a specific [workId], ordered by
     * their enqueue time in ascending order.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @return A map of matching [WorkMetricsSpec] records with the set of their corresponding tags.
     */
    @Query(
        """
        SELECT WorkMetricsSpec.*, WorkMetricsTag.tag FROM WorkMetricsSpec
        LEFT JOIN WorkMetricsTag ON WorkMetricsSpec.work_spec_id = WorkMetricsTag.work_spec_id
            AND WorkMetricsSpec.generation = WorkMetricsTag.generation
            AND WorkMetricsSpec.period_count = WorkMetricsTag.period_count
        WHERE WorkMetricsSpec.work_spec_id = :workId
        ORDER BY WorkMetricsSpec.enqueue_time_ms ASC
    """
    )
    suspend fun getWorkMetricsSpecsAndTags(
        workId: String
    ): Map<WorkMetricsSpec, Set<@MapColumn("tag") String>>

    /**
     * Retrieves [WorkMetricsInfo] records using a raw SQL query inside a database transaction.
     *
     * @param query The raw SQL query.
     */
    @Transaction
    suspend fun getWorkMetricsInfos(query: SupportSQLiteQuery): List<WorkMetricsInfo> {
        val specs = getWorkMetricsSpecs(query)
        if (specs.isEmpty()) return emptyList()
        val workSpecIds = specs.map { it.workSpecId }.distinct()
        val tagsList = getWorkSpecTagsByIds(workSpecIds)
        val tagsMap =
            tagsList.groupBy({ Triple(it.workSpecId, it.generation, it.periodCount) }, { it.tag })
        return specs.map { spec ->
            val key = Triple(spec.workSpecId, spec.generation, spec.periodCount)
            val tags = tagsMap[key]?.toSet() ?: emptySet()
            spec.toWorkMetricsInfo(tags)
        }
    }

    /**
     * Retrieves [WorkMetricsSpec] records using a raw SQL query.
     *
     * @param query The raw SQL query.
     * @return The list of matching [WorkMetricsSpec] records.
     */
    @RawQuery suspend fun getWorkMetricsSpecs(query: SupportSQLiteQuery): List<WorkMetricsSpec>

    /**
     * Retrieves all tags associated with a list of [workSpecIds].
     *
     * @param workSpecIds The list of identifiers of work requests.
     * @return The list of matching [WorkMetricsTag]s.
     */
    @Query("SELECT * FROM WorkMetricsTag WHERE work_spec_id IN (:workSpecIds)")
    suspend fun getWorkSpecTagsByIds(workSpecIds: List<String>): List<WorkMetricsTag>

    /**
     * Retrieves the completed [WorkMetricsInfo] record with its tags directly from the database.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @return The matching [WorkMetricsInfo] record, or null if none exists.
     */
    @Transaction
    fun getWorkMetricsInfo(workId: String, generation: Int, periodCount: Int): WorkMetricsInfo? {
        val spec = getWorkMetricsSpec(workId, generation, periodCount) ?: return null
        val tags = getTags(workId, generation, periodCount).toSet()
        return spec.toWorkMetricsInfo(tags)
    }

    /**
     * Retrieves a specific [WorkMetricsSpec] record matching the primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @return The matching [WorkMetricsSpec] record, or null if none exists.
     */
    @Query(
        """
        SELECT * FROM WorkMetricsSpec
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun getWorkMetricsSpec(workId: String, generation: Int, periodCount: Int): WorkMetricsSpec?

    /**
     * Retrieves the tags associated with a specific work request execution.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @return A list of tags that match the specified primary key.
     */
    @Query(
        """
        SELECT tag FROM WorkMetricsTag
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun getTags(workId: String, generation: Int, periodCount: Int): List<String>

    /**
     * Retrieves the current active [WorkMetricsSpec] record for a specific [workId].
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @return The matching [WorkMetricsSpec] record, or null if none exists.
     */
    @Query(
        """
        SELECT * FROM WorkMetricsSpec
        WHERE work_spec_id = :workId AND state NOT IN $COMPLETED_STATES
        ORDER BY enqueue_time_ms DESC LIMIT 1
    """
    )
    fun getCurrentWorkMetricsSpec(workId: String): WorkMetricsSpec?

    /**
     * Updates the state of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param state The new state to be set.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET state = :state
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setState(
        workId: String,
        generation: Int,
        periodCount: Int,
        state: WorkMetricsInfo.State,
    ): Int

    /**
     * Sets the finish time of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param finishTime The finish time in milliseconds.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET finish_time_ms = :finishTime
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setFinishTime(workId: String, generation: Int, periodCount: Int, finishTime: Long): Int

    /**
     * Sets the first start time of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param startTime The start time in milliseconds.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET first_start_time_ms = :startTime
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setFirstStartTime(workId: String, generation: Int, periodCount: Int, startTime: Long): Int

    /**
     * Sets the unblock time of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param unblockTime The unblock time in milliseconds.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET unblock_time_ms = :unblockTime
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setUnblockTime(workId: String, generation: Int, periodCount: Int, unblockTime: Long): Int

    /**
     * Sets the run attempt count of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param runAttemptCount The run attempt count to be set.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET run_attempt_count = :runAttemptCount
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setRunAttemptCount(
        workId: String,
        generation: Int,
        periodCount: Int,
        runAttemptCount: Int,
    ): Int

    /**
     * Increments the explicit retry count of a [WorkMetricsSpec] matching the specified primary
     * keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET explicit_retry_count = explicit_retry_count + 1
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun incrementExplicitRetryCount(workId: String, generation: Int, periodCount: Int): Int

    /**
     * Sets the stop reason counts of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param stopReasonCounts The map of stop reasons to their respective counts.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET stop_reasons = :stopReasonCounts
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setStopReasonCounts(
        workId: String,
        generation: Int,
        periodCount: Int,
        stopReasonCounts: Map<Int, Int>,
    ): Int

    /**
     * Deletes completed [WorkMetricsSpec] records that finished before the specified threshold
     * time.
     *
     * @param thresholdTimeMillis The threshold time in milliseconds. Completed records with a
     *   finish time less than this value will be deleted.
     * @return The number of deleted rows.
     */
    @Query(
        """
        DELETE FROM WorkMetricsSpec
        WHERE state IN $COMPLETED_STATES AND finish_time_ms < :thresholdTimeMillis
    """
    )
    fun deleteFinishedSpecsOlderThan(thresholdTimeMillis: Long): Int

    /**
     * Sets the worker duration of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param workerDuration The worker duration in milliseconds.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET worker_duration_ms = :workerDuration
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setWorkerDuration(
        workId: String,
        generation: Int,
        periodCount: Int,
        workerDuration: Long,
    ): Int

    /**
     * Sets the total runtime of a [WorkMetricsSpec] matching the specified primary keys.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @param generation The generation of the work request.
     * @param periodCount The period count of the work request.
     * @param totalRuntime The total runtime in milliseconds.
     * @return The number of updated rows.
     */
    @Query(
        """
        UPDATE WorkMetricsSpec SET total_runtime_ms = :totalRuntime
        WHERE work_spec_id = :workId AND generation = :generation AND period_count = :periodCount
    """
    )
    fun setTotalRuntime(workId: String, generation: Int, periodCount: Int, totalRuntime: Long): Int
}

/** Retrieves all [WorkMetricsInfo] records associated with a specific [workId]. */
internal suspend fun WorkMetricsSpecDao.getWorkMetricsInfos(workId: String): List<WorkMetricsInfo> {
    return getWorkMetricsSpecsAndTags(workId).map { (spec, tags) -> spec.toWorkMetricsInfo(tags) }
}
