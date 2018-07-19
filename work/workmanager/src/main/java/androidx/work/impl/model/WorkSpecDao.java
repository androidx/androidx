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

package androidx.work.impl.model;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

import static androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.support.annotation.NonNull;

import androidx.work.Data;
import androidx.work.State;

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
    @Insert(onConflict = IGNORE)
    void insertWorkSpec(WorkSpec workSpec);

    /**
     * Deletes {@link WorkSpec}s from the database.
     *
     * @param id The WorkSpec id to delete.
     */
    @Query("DELETE FROM workspec WHERE id=:id")
    void delete(String id);

    /**
     * @param id The identifier
     * @return The WorkSpec associated with that id
     */
    @Query("SELECT * FROM workspec WHERE id=:id")
    WorkSpec getWorkSpec(String id);

    /**
     * Retrieves {@link WorkSpec}s with the identifiers.
     *
     * @param ids The identifiers of desired {@link WorkSpec}s
     * @return The {@link WorkSpec}s with the requested IDs
     */
    @Query("SELECT * FROM workspec WHERE id IN (:ids)")
    WorkSpec[] getWorkSpecs(List<String> ids);

    /**
     * Retrieves {@link WorkSpec}s labelled with a given name.
     *
     * @param name The work graph name
     * @return The {@link WorkSpec}s labelled with the given name
     */
    @Query("SELECT id, state FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    List<WorkSpec.IdAndState> getWorkSpecIdAndStatesForName(String name);

    /**
     * @return All WorkSpec ids in the database.
     */
    @Query("SELECT id FROM workspec")
    List<String> getAllWorkSpecIds();

    /**
     * Updates the state of at least one {@link WorkSpec} by ID.
     *
     * @param state The new state
     * @param ids The IDs for the {@link WorkSpec}s to update
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET state=:state WHERE id IN (:ids)")
    int setState(State state, String... ids);

    /**
     * Updates the output of a {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier to update
     * @param output The {@link Data} to set as the output
     */
    @Query("UPDATE workspec SET output=:output WHERE id=:id")
    void setOutput(String id, Data output);

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
     * Retrieves the state of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The state of the {@link WorkSpec}
     */
    @Query("SELECT state FROM workspec WHERE id=:id")
    State getState(String id);

    /**
     * For a {@link WorkSpec} identifier, retrieves its {@link WorkSpec.WorkStatusPojo}.
     *
     * @param id The identifier of the {@link WorkSpec}
     * @return A list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id=:id")
    WorkSpec.WorkStatusPojo getWorkStatusPojoForId(String id);

    /**
     * For a list of {@link WorkSpec} identifiers, retrieves a {@link List} of their
     * {@link WorkSpec.WorkStatusPojo}.
     *
     * @param ids The identifier of the {@link WorkSpec}s
     * @return A {@link List} of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN (:ids)")
    List<WorkSpec.WorkStatusPojo> getWorkStatusPojoForIds(List<String> ids);

    /**
     * For a list of {@link WorkSpec} identifiers, retrieves a {@link LiveData} list of their
     * {@link WorkSpec.WorkStatusPojo}.
     *
     * @param ids The identifier of the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN (:ids)")
    LiveData<List<WorkSpec.WorkStatusPojo>> getWorkStatusPojoLiveDataForIds(List<String> ids);

    /**
     * Retrieves a list of {@link WorkSpec.WorkStatusPojo} for all work with a given tag.
     *
     * @param tag The tag for the {@link WorkSpec}s
     * @return A list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<WorkSpec.WorkStatusPojo> getWorkStatusPojoForTag(String tag);

    /**
     * Retrieves a {@link LiveData} list of {@link WorkSpec.WorkStatusPojo} for all work with a
     * given tag.
     *
     * @param tag The tag for the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    LiveData<List<WorkSpec.WorkStatusPojo>> getWorkStatusPojoLiveDataForTag(String tag);

    /**
     * Retrieves a list of {@link WorkSpec.WorkStatusPojo} for all work with a given name.
     *
     * @param name The name of the {@link WorkSpec}s
     * @return A list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    List<WorkSpec.WorkStatusPojo> getWorkStatusPojoForName(String name);

    /**
     * Retrieves a {@link LiveData} list of {@link WorkSpec.WorkStatusPojo} for all work with a
     * given name.
     *
     * @param name The name for the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.WorkStatusPojo}
     */
    @Transaction
    @Query("SELECT id, state, output FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM workname WHERE name=:name)")
    LiveData<List<WorkSpec.WorkStatusPojo>> getWorkStatusPojoLiveDataForName(String name);

    /**
     * Gets all inputs coming from prerequisites for a particular {@link WorkSpec}.  These are
     * {@link Data} set via {@code Worker#setOutputData()}.
     *
     * @param id The {@link WorkSpec} identifier
     * @return A list of all inputs coming from prerequisites for {@code id}
     */
    @Query("SELECT output FROM workspec WHERE id IN "
            + "(SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id)")
    List<Data> getInputsFromPrerequisites(String id);

    /**
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES
            + " AND id IN (SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<String> getUnfinishedWorkWithTag(@NonNull String tag);

    /**
     * Retrieves work ids for unfinished work with a given name.
     *
     * @param name THe tag used to identify the work
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES
            + " AND id IN (SELECT work_spec_id FROM workname WHERE name=:name)")
    List<String> getUnfinishedWorkWithName(@NonNull String name);

    /**
     * Retrieves work ids for all unfinished work.
     *
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state NOT IN " + COMPLETED_STATES)
    List<String> getAllUnfinishedWork();

    /**
     * Marks a {@link WorkSpec} as scheduled.
     *
     * @param id        The identifier for the {@link WorkSpec}
     * @param startTime The time at which the {@link WorkSpec} was scheduled.
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET schedule_requested_at=:startTime WHERE id=:id")
    int markWorkSpecScheduled(@NonNull String id, long startTime);

    /**
     * Resets the scheduled state on the {@link WorkSpec}s that are not in a a completed state.
     * @return The number of rows that were updated
     */
    @Query("UPDATE workspec SET schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
            + " WHERE state NOT IN " + COMPLETED_STATES)
    int resetScheduledState();

    /**
     * @return The List of {@link WorkSpec}s that are eligible to be scheduled.
     */
    @Query("SELECT * from workspec WHERE "
            + "state=" + WorkTypeConverters.StateIds.ENQUEUED
            // We only want WorkSpecs which have not been previously scheduled.
            + " AND schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
            + " LIMIT "
                + "(SELECT :schedulerLimit" + "-COUNT(*) FROM workspec WHERE"
                    + " schedule_requested_at<>" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
                    + " AND state NOT IN " + COMPLETED_STATES
                + ")"
    )
    List<WorkSpec> getEligibleWorkForScheduling(int schedulerLimit);

    /**
     * Immediately prunes eligible work from the database meeting the following criteria:
     * - Is finished (succeeded, failed, or cancelled)
     * - Has zero unfinished dependents
     */
    @Query("DELETE FROM workspec WHERE "
            + "state IN " + COMPLETED_STATES
            + " AND (SELECT COUNT(*)=0 FROM dependency WHERE "
            + "    prerequisite_id=id AND "
            + "    work_spec_id NOT IN "
            + "        (SELECT id FROM workspec WHERE state IN " + COMPLETED_STATES + "))")
    void pruneFinishedWorkWithZeroDependentsIgnoringKeepForAtLeast();
}
