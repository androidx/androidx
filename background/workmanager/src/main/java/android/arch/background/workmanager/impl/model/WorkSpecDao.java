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

package android.arch.background.workmanager.impl.model;

import static android.arch.background.workmanager.Constraints.NETWORK_NOT_REQUIRED;
import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Work.STATUS_RUNNING;
import static android.arch.background.workmanager.impl.BaseWork.STATUS_CANCELLED;
import static android.arch.background.workmanager.impl.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.impl.BaseWork.STATUS_SUCCEEDED;
import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.background.workmanager.Arguments;
import android.arch.background.workmanager.Work;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * The Data Access Object for {@link WorkSpec}s.
 */
@Dao
public interface WorkSpecDao {

    /**
     * Attempts to insert a {@link WorkSpec} into the database.
     *
     * @param workSpec The WorkSpec to insert.
     */
    @Insert(onConflict = FAIL)
    void insertWorkSpec(WorkSpec workSpec);

    /**
     * @param id The identifier
     * @return The WorkSpec associated with that id
     */
    @Query("SELECT * FROM workspec WHERE id=:id")
    WorkSpec getWorkSpec(String id);

    /**
     * Retrieves {@link WorkSpec}s with the identifiers.
     *
     * @param ids The identifiers of desired {@link WorkSpec}s.
     * @return The {@link WorkSpec}s with the requested IDs.
     */
    @Query("SELECT * FROM workspec WHERE id IN (:ids)")
    WorkSpec[] getWorkSpecs(List<String> ids);

    /**
     * Updates the status of at least one {@link WorkSpec} by ID.
     *
     * @param status The new status
     * @param ids The IDs for the {@link WorkSpec}s to update
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET status=:status WHERE id IN (:ids)")
    int setStatus(@Work.WorkStatus int status, String... ids);

    /**
     * Updates the output of a {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier to update
     * @param output The {@link Arguments} to set as the output
     */
    @Query("UPDATE workspec SET output=:output WHERE id=:id")
    void setOutput(String id, Arguments output);

    /**
     * Updates the period start time of a {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier to update
     * @param periodStartTime The time when the period started.
     */
    @Query("UPDATE workspec SET period_start_time=:periodStartTime WHERE id=:id")
    void setPeriodStartTime(String id, long periodStartTime);

    /**
     * Increment run attempt count of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=run_attempt_count+1 WHERE id=:id")
    int incrementWorkSpecRunAttemptCount(String id);

    /**
     * Reset run attempt count of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET run_attempt_count=0 WHERE id=:id")
    int resetWorkSpecRunAttemptCount(String id);

    /**
     * Retrieves the status of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The status of the {@link WorkSpec}
     */
    @Query("SELECT status FROM workspec WHERE id=:id")
    @Work.WorkStatus
    int getWorkSpecStatus(String id);

    /**
     * Retrieves a {@link LiveData} status of a {@link WorkSpec}
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The {@link LiveData} status of the {@link WorkSpec}
     */
    @Query("SELECT status FROM workspec WHERE id=:id")
    LiveData<Integer> getWorkSpecLiveDataStatus(String id);

    /**
     * Retrieves {@link WorkSpec}s that have have status {@code STATUS_ENQUEUED} or
     * {@code STATUS_RUNNING} at least one constraint, no initial delay, and are not periodic.
     *
     * @return A {@link LiveData} list of {@link WorkSpec}s.
     */
    @Query("SELECT * FROM workspec WHERE ("
            + "requires_battery_not_low=1 OR requires_charging=1 OR requires_storage_not_low=1 OR "
            + "required_network_type!=" + NETWORK_NOT_REQUIRED + ") AND "
            + "(status=" + STATUS_ENQUEUED + " OR status=" + STATUS_RUNNING + ") AND "
            + "initial_delay=0 AND interval_duration=0")
    LiveData<List<WorkSpec>> getConstraintsTrackerEligibleWorkSpecs();

    /**
     * Retrieves {@link WorkSpec}s that have status {@code STATUS_ENQUEUED}, have no constraints,
     * no initial delay, and are not periodic.
     *
     * @return A {@link LiveData} list of {@link WorkSpec}s.
     */
    @Query("SELECT * FROM workspec WHERE status=" + STATUS_ENQUEUED + " AND "
            + " requires_charging=0 AND requires_device_idle=0 AND content_uri_triggers IS NULL"
            + " AND requires_battery_not_low=0 AND requires_storage_not_low=0"
            + " AND required_network_type=0 AND initial_delay=0 AND interval_duration=0")
    LiveData<List<WorkSpec>> getForegroundEligibleWorkSpecs();

    /**
     * Gets all inputs coming from prerequisites for a particular {@link WorkSpec}.  These are
     * {@link Arguments} set via {@code Worker#setOutput()}.
     *
     * @param id The {@link WorkSpec} identifier
     * @return A list of all inputs coming from prerequisites for {@code id}
     */
    @Query("SELECT output FROM workspec WHERE id IN "
            + "(SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id)")
    List<Arguments> getInputsFromPrerequisites(String id);

    /**
     * Retrieves a {@link LiveData} of the output for a {@link WorkSpec}
     * @param id
     * @return
     */
    @Query("SELECT output FROM workspec WHERE id=:id")
    LiveData<Arguments> getOutput(String id);

    /**
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work.
     * @return A list of work ids.
     */
    @Query("SELECT id FROM workspec WHERE status!=" + STATUS_SUCCEEDED + " AND status!="
            + STATUS_FAILED + " AND id IN (SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<String> getUnfinishedWorkWithTag(@NonNull String tag);

    /**
     * Deletes all non-pending work from the database that isn't a prerequisite for other work.
     * Calling this method repeatedly until it returns 0 will allow you to prune all work chains
     * that are finished.
     *
     * @return The number of deleted work items
     */
    @Query("DELETE FROM workspec WHERE status IN "
            + "(" + STATUS_CANCELLED + ", " + STATUS_FAILED + ", " + STATUS_SUCCEEDED + ") AND "
            + "id NOT IN (SELECT DISTINCT prerequisite_id FROM dependency)")
    int pruneLeaves();
}
