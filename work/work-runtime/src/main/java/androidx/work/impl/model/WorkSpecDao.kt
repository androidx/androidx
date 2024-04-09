/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.model

import android.annotation.SuppressLint
import android.app.job.JobParameters.STOP_REASON_CANCELLED_BY_APP
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.work.CONSTRAINTS_COLUMNS
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.impl.model.WorkTypeConverters.StateIds.CANCELLED
import androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES
import androidx.work.impl.model.WorkTypeConverters.StateIds.ENQUEUED
import androidx.work.impl.model.WorkTypeConverters.StateIds.RUNNING
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.intellij.lang.annotations.Language

/**
 * The Data Access Object for [WorkSpec]s.
 */
@Dao
@SuppressLint("UnknownNullness")
interface WorkSpecDao {
    /**
     * Attempts to insert a [WorkSpec] into the database.
     *
     * @param workSpec The WorkSpec to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertWorkSpec(workSpec: WorkSpec)

    /**
     * Deletes [WorkSpec]s from the database.
     *
     * @param id The WorkSpec id to delete.
     */
    @Query("DELETE FROM workspec WHERE id=:id")
    fun delete(id: String)

    /**
     * @param id The identifier
     * @return The WorkSpec associated with that id
     */
    @Query("SELECT * FROM workspec WHERE id=:id")
    fun getWorkSpec(id: String): WorkSpec?

    /**
     *
     * @param name The work graph name
     * @return The [WorkSpec]s labelled with the given name
     */
    @Query(
        "SELECT id, state FROM workspec WHERE id IN " +
            "(SELECT work_spec_id FROM workname WHERE name=:name)"
    )
    fun getWorkSpecIdAndStatesForName(name: String): List<WorkSpec.IdAndState>

    /**
     * @return All WorkSpec ids in the database.
     */
    @Query("SELECT id FROM workspec")
    fun getAllWorkSpecIds(): List<String>

    /**
     * @return A [LiveData] list of all WorkSpec ids in the database.
     */
    @Transaction
    @Query("SELECT id FROM workspec")
    fun getAllWorkSpecIdsLiveData(): LiveData<List<String>>

    /**
     * Updates the state of at least one [WorkSpec] by ID.
     *
     * @param state The new state
     * @param id The IDs for the [WorkSpec]s to update
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET state=:state WHERE id=:id")
    fun setState(state: WorkInfo.State, id: String): Int

    /**
     * Sets cancelled state for workspec
     *
     * @param id The IDs for the [WorkSpec]s to update
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec " +
        "SET stop_reason = CASE WHEN state=$RUNNING THEN $STOP_REASON_CANCELLED_BY_APP " +
        "ELSE ${WorkInfo.STOP_REASON_NOT_STOPPED} END, state=$CANCELLED WHERE id=:id")
    fun setCancelledState(id: String): Int

    /**
     * Increment periodic counter.
     */
    @Query("UPDATE workspec SET period_count=period_count+1 WHERE id=:id")
    fun incrementPeriodCount(id: String)

    /**
     * Updates the output of a [WorkSpec].
     *
     * @param id The [WorkSpec] identifier to update
     * @param output The [Data] to set as the output
     */
    @Query("UPDATE workspec SET output=:output WHERE id=:id")
    fun setOutput(id: String, output: Data)

    /**
     * Updates the period start time of a [WorkSpec].
     *
     * @param id The [WorkSpec] identifier to update
     * @param enqueueTime The time when the period started.
     */
    @Query("UPDATE workspec SET last_enqueue_time=:enqueueTime WHERE id=:id")
    fun setLastEnqueueTime(id: String, enqueueTime: Long)

    /**
     * Increment run attempt count of a [WorkSpec].
     *
     * @param id The identifier for the [WorkSpec]
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=run_attempt_count+1 WHERE id=:id")
    fun incrementWorkSpecRunAttemptCount(id: String): Int

    /**
     * Reset run attempt count of a [WorkSpec].
     *
     * @param id The identifier for the [WorkSpec]
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=0 WHERE id=:id")
    fun resetWorkSpecRunAttemptCount(id: String): Int

    /**
     * Updates the next schedule time of a [WorkSpec].
     *
     * @param id The [WorkSpec] identifier to update
     * @param nextScheduleTimeOverrideMillis The next schedule time in millis since epoch. See
     * [WorkSpec.nextScheduleTimeOverride]
     */
    @Query("UPDATE workspec SET next_schedule_time_override=:nextScheduleTimeOverrideMillis " +
        "WHERE id=:id")
    fun setNextScheduleTimeOverride(id: String, nextScheduleTimeOverrideMillis: Long)

    /**
     * Resets the next schedule time override of a [WorkSpec] if the override generation has not
     * changed.
     *
     * @param id The identifier for the [WorkSpec]
     */
    @Query(
        "UPDATE workspec SET next_schedule_time_override=${Long.MAX_VALUE} WHERE " +
            "(id=:id AND next_schedule_time_override_generation=:overrideGeneration)"
    )
    fun resetWorkSpecNextScheduleTimeOverride(id: String, overrideGeneration: Int)

    /**
     * Retrieves the state of a [WorkSpec].
     *
     * @param id The identifier for the [WorkSpec]
     * @return The state of the [WorkSpec]
     */
    @Query("SELECT state FROM workspec WHERE id=:id")
    fun getState(id: String): WorkInfo.State?

    /**
     * For a [WorkSpec] identifier, retrieves its [WorkSpec.WorkInfoPojo].
     *
     * @param id The identifier of the [WorkSpec]
     * @return A list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query("SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id=:id")
    fun getWorkStatusPojoForId(id: String): WorkSpec.WorkInfoPojo?

    /**
     * For a list of [WorkSpec] identifiers, retrieves a [List] of their
     * [WorkSpec.WorkInfoPojo].
     *
     * @param ids The identifier of the [WorkSpec]s
     * @return A [List] of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query("SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN (:ids)")
    fun getWorkStatusPojoForIds(ids: List<String>): List<WorkSpec.WorkInfoPojo>

    /**
     * For a list of [WorkSpec] identifiers, retrieves a [LiveData] list of their
     * [WorkSpec.WorkInfoPojo].
     *
     * @param ids The identifier of the [WorkSpec]s
     * @return A [LiveData] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_IDS)
    fun getWorkStatusPojoLiveDataForIds(ids: List<String>): LiveData<List<WorkSpec.WorkInfoPojo>>

    /**
     * For a list of [WorkSpec] identifiers, retrieves a [LiveData] list of their
     * [WorkSpec.WorkInfoPojo].
     *
     * @param ids The identifier of the [WorkSpec]s
     * @return A [Flow] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_IDS)
    fun getWorkStatusPojoFlowDataForIds(ids: List<String>): Flow<List<WorkSpec.WorkInfoPojo>>

    /**
     * Retrieves a list of [WorkSpec.WorkInfoPojo] for all work with a given tag.
     *
     * @param tag The tag for the [WorkSpec]s
     * @return A list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(
        """SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN
            (SELECT work_spec_id FROM worktag WHERE tag=:tag)"""
    )
    fun getWorkStatusPojoForTag(tag: String): List<WorkSpec.WorkInfoPojo>

    /**
     * Retrieves a [LiveData] list of [WorkSpec.WorkInfoPojo] for all work with a
     * given tag.
     *
     * @param tag The tag for the [WorkSpec]s
     * @return A [LiveData] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_TAG)
    fun getWorkStatusPojoFlowForTag(tag: String): Flow<List<WorkSpec.WorkInfoPojo>>

    /**
     * Retrieves a [LiveData] list of [WorkSpec.WorkInfoPojo] for all work with a
     * given tag.
     *
     * @param tag The tag for the [WorkSpec]s
     * @return A [LiveData] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_TAG)
    fun getWorkStatusPojoLiveDataForTag(tag: String): LiveData<List<WorkSpec.WorkInfoPojo>>

    /**
     * Retrieves a list of [WorkSpec.WorkInfoPojo] for all work with a given name.
     *
     * @param name The name of the [WorkSpec]s
     * @return A list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(
        "SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN " +
            "(SELECT work_spec_id FROM workname WHERE name=:name)"
    )
    fun getWorkStatusPojoForName(name: String): List<WorkSpec.WorkInfoPojo>

    /**
     * Retrieves a [LiveData] list of [WorkSpec.WorkInfoPojo] for all work with a
     * given name.
     *
     * @param name The name for the [WorkSpec]s
     * @return A [LiveData] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_NAME)
    fun getWorkStatusPojoLiveDataForName(name: String): LiveData<List<WorkSpec.WorkInfoPojo>>

    /**
     * Retrieves a [Flow] list of [WorkSpec.WorkInfoPojo] for all work with a given name.
     *
     * @param name The name for the [WorkSpec]s
     * @return A [LiveData] list of [WorkSpec.WorkInfoPojo]
     */
    @Transaction
    @Query(WORK_INFO_BY_NAME)
    fun getWorkStatusPojoFlowForName(name: String): Flow<List<WorkSpec.WorkInfoPojo>>

    /**
     * Gets all inputs coming from prerequisites for a particular [WorkSpec].  These are
     * [Data] set via `Worker#setOutputData()`.
     *
     * @param id The [WorkSpec] identifier
     * @return A list of all inputs coming from prerequisites for `id`
     */
    @Query(
        """SELECT output FROM workspec WHERE id IN
             (SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id)"""
    )
    fun getInputsFromPrerequisites(id: String): List<Data>

    /**
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work
     * @return A list of work ids
     */
    @Query(
        "SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES +
            " AND id IN (SELECT work_spec_id FROM worktag WHERE tag=:tag)"
    )
    fun getUnfinishedWorkWithTag(tag: String): List<String>

    /**
     * Retrieves work ids for unfinished work with a given name.
     *
     * @param name THe tag used to identify the work
     * @return A list of work ids
     */
    @Query(
        "SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES +
            " AND id IN (SELECT work_spec_id FROM workname WHERE name=:name)"
    )
    fun getUnfinishedWorkWithName(name: String): List<String>

    /**
     * Retrieves work ids for all unfinished work.
     *
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES)
    fun getAllUnfinishedWork(): List<String>

    /**
     * @return `true` if there is pending work.
     */
    @Query("SELECT COUNT(*) > 0 FROM workspec WHERE state NOT IN $COMPLETED_STATES LIMIT 1")
    fun hasUnfinishedWorkFlow(): Flow<Boolean>

    /**
     * Marks a [WorkSpec] as scheduled.
     *
     * @param id        The identifier for the [WorkSpec]
     * @param startTime The time at which the [WorkSpec] was scheduled.
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET schedule_requested_at=:startTime WHERE id=:id")
    fun markWorkSpecScheduled(id: String, startTime: Long): Int

    /**
     * @return The time at which the [WorkSpec] was scheduled.
     */
    @Query("SELECT schedule_requested_at FROM workspec WHERE id=:id")
    fun getScheduleRequestedAtLiveData(id: String): LiveData<Long?>

    /**
     * Resets the scheduled state on the [WorkSpec]s that are not in a a completed state.
     * @return The number of rows that were updated
     */
    @Query(
        "UPDATE workspec SET schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET +
            " WHERE state NOT IN " + COMPLETED_STATES
    )
    fun resetScheduledState(): Int

    /**
     * @return The List of [WorkSpec]s that are eligible to be scheduled.
     */
    @Query(
        "SELECT * FROM workspec WHERE " +
            "state=" + ENQUEUED +
            // We only want WorkSpecs which have not been previously scheduled.
            " AND schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET +
            // Order by period start time so we execute scheduled WorkSpecs in FIFO order
            " ORDER BY last_enqueue_time" +
            " LIMIT " +
            "(SELECT MAX(:schedulerLimit" + "-COUNT(*), 0) FROM workspec WHERE" +
            " schedule_requested_at<>" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET +
            // content_uri_triggers aren't counted here because they have separate limit
            " AND LENGTH(content_uri_triggers)=0" +
            " AND state NOT IN " + COMPLETED_STATES +
            ")"
    )
    fun getEligibleWorkForScheduling(schedulerLimit: Int): List<WorkSpec>

    /**
     * @return The List of [WorkSpec]s that are eligible to be scheduled.
     */
    @Query(
        "SELECT * FROM workspec WHERE " +
            "state=$ENQUEUED" +
            // We only want WorkSpecs which have not been previously scheduled.
            " AND schedule_requested_at=${WorkSpec.SCHEDULE_NOT_REQUESTED_YET}" +
            " AND LENGTH(content_uri_triggers)<>0" +
            // Order by period start time so we execute scheduled WorkSpecs in FIFO order
            " ORDER BY last_enqueue_time"
    )
    fun getEligibleWorkForSchedulingWithContentUris(): List<WorkSpec>

    /**
     * @return The List of [WorkSpec]s that can be scheduled irrespective of scheduling
     * limits.
     */
    @Query(
        "SELECT * FROM workspec WHERE " +
            "state=$ENQUEUED" +
            // Order by period start time so we execute scheduled WorkSpecs in FIFO order
            " ORDER BY last_enqueue_time" +
            " LIMIT :maxLimit"
    )
    fun getAllEligibleWorkSpecsForScheduling(maxLimit: Int): List<WorkSpec> // Unfinished work
    // We only want WorkSpecs which have been scheduled.
    /**
     * @return The List of [WorkSpec]s that are unfinished and scheduled.
     */
    @Query(
        "SELECT * FROM workspec WHERE " + // Unfinished work
            "state=" + ENQUEUED + // We only want WorkSpecs which have been scheduled.
            " AND schedule_requested_at<>" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
    )
    fun getScheduledWork(): List<WorkSpec>

    /**
     * @return The List of [WorkSpec]s that are running.
     */
    @Query(
        "SELECT * FROM workspec WHERE " + // Unfinished work
            "state=" + WorkTypeConverters.StateIds.RUNNING
    )
    fun getRunningWork(): List<WorkSpec>

    /**
     * @return The List of [WorkSpec] which completed recently.
     */
    @Query(
        "SELECT * FROM workspec WHERE " +
            "last_enqueue_time >= :startingAt" +
            " AND state IN " + COMPLETED_STATES +
            " ORDER BY last_enqueue_time DESC"
    )
    fun getRecentlyCompletedWork(startingAt: Long): List<WorkSpec>

    /**
     * Immediately prunes eligible work from the database meeting the following criteria:
     * - Is finished (succeeded, failed, or cancelled)
     * - Has zero unfinished dependents
     */
    @Query(
        "DELETE FROM workspec WHERE " +
            "state IN " + COMPLETED_STATES +
            " AND (SELECT COUNT(*)=0 FROM dependency WHERE " +
            "    prerequisite_id=id AND " +
            "    work_spec_id NOT IN " +
            "        (SELECT id FROM workspec WHERE state IN " + COMPLETED_STATES + "))"
    )
    fun pruneFinishedWorkWithZeroDependentsIgnoringKeepForAtLeast()

    @Query("UPDATE workspec SET generation=generation+1 WHERE id=:id")
    fun incrementGeneration(id: String)

    @Update
    fun updateWorkSpec(workSpec: WorkSpec)

    @Query(
        "Select COUNT(*) FROM workspec WHERE LENGTH(content_uri_triggers)<>0" +
            " AND state NOT IN $COMPLETED_STATES"
    )
    fun countNonFinishedContentUriTriggerWorkers(): Int

    @Query("UPDATE workspec SET stop_reason=:stopReason WHERE id=:id")
    fun setStopReason(id: String, stopReason: Int)
}

fun WorkSpecDao.getWorkStatusPojoFlowDataForIds(id: UUID): Flow<WorkInfo?> =
    getWorkStatusPojoFlowDataForIds(listOf("$id"))
        .map { it.firstOrNull()?.toWorkInfo() }.distinctUntilChanged()

fun WorkSpecDao.getWorkStatusPojoFlowForName(
    dispatcher: CoroutineDispatcher,
    name: String
): Flow<List<WorkInfo>> = getWorkStatusPojoFlowForName(name).dedup(dispatcher)

fun WorkSpecDao.getWorkStatusPojoFlowForTag(
    dispatcher: CoroutineDispatcher,
    tag: String
): Flow<List<WorkInfo>> = getWorkStatusPojoFlowForTag(tag).dedup(dispatcher)

internal fun Flow<List<WorkSpec.WorkInfoPojo>>.dedup(
    dispatcher: CoroutineDispatcher
): Flow<List<WorkInfo>> = map { list -> list.map { pojo -> pojo.toWorkInfo() } }
    .distinctUntilChanged()
    .flowOn(dispatcher)

private const val WORK_INFO_COLUMNS = "id, state, output, run_attempt_count, generation" +
    ", $CONSTRAINTS_COLUMNS, initial_delay, interval_duration, flex_duration, backoff_policy" +
    ", backoff_delay_duration, last_enqueue_time, period_count, next_schedule_time_override, " +
    "stop_reason"

@Language("sql")
private const val WORK_INFO_BY_IDS = "SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN (:ids)"

@Language("sql")
private const val WORK_INFO_BY_TAG = """SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN
            (SELECT work_spec_id FROM worktag WHERE tag=:tag)"""

@Language("sql")
private const val WORK_INFO_BY_NAME = "SELECT $WORK_INFO_COLUMNS FROM workspec WHERE id IN " +
    "(SELECT work_spec_id FROM workname WHERE name=:name)"
