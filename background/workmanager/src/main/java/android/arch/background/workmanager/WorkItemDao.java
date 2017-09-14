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

import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link WorkItem}s.
 */
@Dao
public interface WorkItemDao {

    /**
     * @param id The identifier
     * @return The {@link WorkItem} associated with that identifier
     */
    @Query("SELECT * FROM workitem WHERE id=:id")
    WorkItem getWorkItem(int id);

    /**
     * Attempts to insert a WorkItem into the database.
     *
     * @param workItems The {@link WorkItem}s to insert
     */
    @Insert(onConflict = FAIL)
    void insertWorkItems(List<WorkItem> workItems);

    /**
     * Updates the status of a {@link WorkItem}.
     *
     * @param id     The identifier for the {@link WorkItem}
     * @param status The new status
     * @return The number of rows that were updated (should be 0 or 1)
     */
    @Query("UPDATE workitem SET status=:status WHERE id=:id")
    int setWorkItemStatus(int id, int status);
}
