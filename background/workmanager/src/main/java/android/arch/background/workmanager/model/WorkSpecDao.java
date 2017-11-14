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

package android.arch.background.workmanager.model;

import static android.arch.background.workmanager.BaseWork.STATUS_FAILED;
import static android.arch.background.workmanager.BaseWork.STATUS_SUCCEEDED;
import static android.arch.background.workmanager.Work.STATUS_BLOCKED;
import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.background.workmanager.Work.STATUS_RUNNING;
import static android.arch.persistence.room.OnConflictStrategy.FAIL;

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
     * Deletes a {@link WorkSpec} with the given id.
     *
     * @param id The WorkSpec id
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
    WorkSpec[] getWorkSpecs(String... ids);

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
     * Retrieves {@link WorkSpec}s that have status {@code STATUS_ENQUEUED}, have no constraints,
     * and are not periodic.
     *
     * @return A {@link LiveData} list of {@link WorkSpec}s.
     */
    @Query("SELECT * FROM workspec WHERE status=" + STATUS_ENQUEUED + " AND "
            + " requires_charging=0 AND requires_device_idle=0 AND requires_battery_not_low=0 AND "
            + " requires_storage_not_low=0 AND required_network_type=0 AND interval_duration=0 AND"
            + " content_uri_triggers IS NULL")
    LiveData<List<WorkSpec>> getForegroundEligibleWorkSpecs();

    /**
     * Retrieves work ids for items that are no longer considered blocked (items that are currently
     * {@code STATUS_BLOCKED} but aren't in the {@link Dependency} table).
     *
     * @return An array of work ids.
     */
    @Query("SELECT id FROM workspec WHERE status=" + STATUS_BLOCKED + " AND id NOT IN "
            + "(SELECT DISTINCT work_spec_id FROM dependency)")
    String[] getUnblockedWorkIds();

    /**
     * Retrieves work ids for unfinished work with a given tag.
     *
     * @param tag The tag used to identify the work.
     * @return A list of work ids.
     */
    @Query("SELECT id FROM workspec WHERE status!=" + STATUS_SUCCEEDED + " AND status!="
            + STATUS_FAILED + " AND tag=:tag")
    List<String> getUnfinishedWorkWithTag(@NonNull String tag);

    /**
     * Retrieves work ids for unfinished work with a given tag prefix.
     *
     * @param tagPrefix The tag prefix used to identify the work.
     * @return A list of work ids.
     */
    @Query("SELECT id FROM workspec WHERE status!=" + STATUS_SUCCEEDED + " AND status!="
            + STATUS_FAILED + " AND tag LIKE :tagPrefix || '%'")
    List<String> getUnfinishedWorkWithTagPrefix(@NonNull String tagPrefix);

    /**
     * Clears all work.
     */
    @Query("DELETE FROM workspec")
    void clearAll();

    String CONSTRAINT_SUFFIX =
            " AND (status=" + STATUS_ENQUEUED + " OR status=" + STATUS_RUNNING + ") AND "
            + "(interval_duration=0 OR (:allowPeriodic AND interval_duration>0))";

    /**
     * Returns ids for work items that have a battery charging constraint.
     *
     * @param allowPeriodic {@code true} to allow periodic jobs to be returned
     * @return A list of {@link WorkSpec} ids that have a battery charging constraint.
     */
    @Query("SELECT id FROM workspec WHERE requires_charging=1" + CONSTRAINT_SUFFIX)
    LiveData<List<String>> getIdsForBatteryChargingController(boolean allowPeriodic);

    /**
     * Returns ids for work items that have a battery not low constraint.
     *
     * @param allowPeriodic {@code true} to allow periodic jobs to be returned
     * @return A list of {@link WorkSpec} ids that have a battery not low constraint.
     */
    @Query("SELECT id FROM workspec WHERE requires_battery_not_low=1" + CONSTRAINT_SUFFIX)
    LiveData<List<String>> getIdsForBatteryNotLowController(boolean allowPeriodic);

    /**
     * Returns ids for work items that have a storage not low constraint.
     *
     * @param allowPeriodic {@code true} to allow periodic jobs to be returned
     * @return A list of {@link WorkSpec} ids that have a storage not low constraint.
     */
    @Query("SELECT id FROM workspec WHERE requires_storage_not_low=1" + CONSTRAINT_SUFFIX)
    LiveData<List<String>> getIdsForStorageNotLowController(boolean allowPeriodic);

    /**
     * Returns ids for enqueued work items that have the required {@code networkType} constraint.
     *
     * @param networkType The {@link Constraints.NetworkType} network type
     * @param allowPeriodic {@code true} to allow periodic jobs to be returned
     * @return A list of {@link WorkSpec} ids that have the {@code networkType} constraint.
     */
    @Query("SELECT id FROM workspec WHERE required_network_type=:networkType" + CONSTRAINT_SUFFIX)
    LiveData<List<String>> getIdsForNetworkTypeController(
            @Constraints.NetworkType int networkType, boolean allowPeriodic);
}
