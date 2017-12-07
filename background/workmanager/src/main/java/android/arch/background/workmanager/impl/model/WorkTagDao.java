/*
 * Copyright 2017 The Android Open Source Project
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

import static android.arch.persistence.room.OnConflictStrategy.FAIL;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link WorkTag}s.
 */
@Dao
public interface WorkTagDao {

    /**
     * Inserts a {@link WorkTag} into the table.
     *
     * @param workTag The {@link WorkTag} to insert
     */
    @Insert(onConflict = FAIL)
    void insert(WorkTag workTag);

    /**
     * Retrieves all {@link WorkSpec} id's with the given tag.
     *
     * @param tag The matching tag
     * @return All {@link WorkSpec} id's with the given tag
     */
    @Query("SELECT work_spec_id FROM worktag WHERE tag=:tag")
    List<String> getWorkSpecsWithTag(String tag);
}
