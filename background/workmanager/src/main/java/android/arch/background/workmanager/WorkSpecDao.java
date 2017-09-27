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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.Work.STATUS_ENQUEUED;
import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.background.workmanager.model.WorkSpec;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

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
    List<WorkSpec> getWorkSpecs(List<String> ids);

    /**
     * Updates the status of a {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @param status The new status
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workspec SET status=:status WHERE id=:id")
    int setWorkSpecStatus(String id, int status);

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
     * Retrieves work ids for items that are runnable (items that are {@code ENQUEUED} and don't
     * have dependencies).
     *
     * @return A {@link LiveData} list of work ids.
     */
    @Query("SELECT id FROM workspec WHERE status=" + STATUS_ENQUEUED + " AND id NOT IN "
            + "(SELECT DISTINCT work_spec_id FROM dependency)")
    LiveData<List<String>> getRunnableWorkIds();
}
