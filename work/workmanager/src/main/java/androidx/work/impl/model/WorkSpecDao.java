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

import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;

import java.util.List;

import androidx.work.Arguments;
import androidx.work.State;

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
     * @param ids The identifiers of desired {@link WorkSpec}s.
     * @return The {@link WorkSpec}s with the requested IDs.
     */
    @Query("SELECT * FROM workspec WHERE id IN (:ids)")
    WorkSpec[] getWorkSpecs(List<String> ids);

    /**
     * Retrieves {@link WorkSpec}s with the given tag.
     *
     * @param tag The tag of the desired {@link WorkSpec}s.
     * @return The {@link WorkSpec}s with the requested tag.
     */
    @Query("SELECT id, state FROM workspec WHERE id IN "
            + "(SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<WorkSpec.IdAndState> getWorkSpecIdAndStatesForTag(String tag);

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
     * Retrieves the state of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return The state of the {@link WorkSpec}
     */
    @Query("SELECT state FROM workspec WHERE id=:id")
    State getState(String id);

    /**
     * For a {@link WorkSpec} identifier, retrieves its {@link WorkSpec.IdStateAndOutput}.
     *
     * @param id The identifier of the {@link WorkSpec}
     * @return A list of {@link WorkSpec.IdStateAndOutput}
     */
    @Query("SELECT id, state, output FROM workspec WHERE id=:id")
    WorkSpec.IdStateAndOutput getIdStateAndOutput(String id);

    /**
     * For a list of {@link WorkSpec} identifiers, retrieves a {@link LiveData} list of their
     * {@link WorkSpec.IdStateAndOutput}.
     *
     * @param ids The identifier of the {@link WorkSpec}s
     * @return A {@link LiveData} list of {@link WorkSpec.IdStateAndOutput}
     */
    @Query("SELECT id, state, output FROM workspec WHERE id IN (:ids)")
    LiveData<List<WorkSpec.IdStateAndOutput>> getIdStateAndOutputsLiveData(List<String> ids);

    /**
     * Retrieves {@link WorkSpec}s that have state {@code ENQUEUED} or {@code RUNNING}
     *
     * @return A list of {@link WorkSpec}s.
     */
    @Query("SELECT * FROM workspec WHERE (state=" + EnumTypeConverters.StateIds.ENQUEUED
            + " OR state=" + EnumTypeConverters.StateIds.RUNNING
            + ") AND period_start_time<=:startTime")
    List<WorkSpec> getEligibleWorkSpecs(long startTime);

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
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work
     * @return A list of work ids
     */
    @Query("SELECT id FROM workspec WHERE state!=" + EnumTypeConverters.StateIds.SUCCEEDED
            + " AND state!=" + EnumTypeConverters.StateIds.FAILED
            + " AND id IN (SELECT work_spec_id FROM worktag WHERE tag=:tag)")
    List<String> getUnfinishedWorkWithTag(@NonNull String tag);
}
