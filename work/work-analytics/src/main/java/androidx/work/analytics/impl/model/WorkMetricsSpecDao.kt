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
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
     * Inserts a new [WorkMetricsSpec] record into the database.
     *
     * @param spec The [WorkMetricsSpec] to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertWorkMetricsSpec(spec: WorkMetricsSpec)

    /**
     * Retrieves all [WorkMetricsSpec] records associated with a specific [workId], ordered by their
     * enqueue time in ascending order.
     *
     * @param workId The identifier of the [androidx.work.WorkRequest].
     * @return A list of matching [WorkMetricsSpec] records.
     */
    @Query(
        "SELECT * FROM WorkMetricsSpec WHERE work_spec_id = :workId ORDER BY enqueue_time_ms ASC"
    )
    fun getWorkMetricsSpecs(workId: String): List<WorkMetricsSpec>

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
}
