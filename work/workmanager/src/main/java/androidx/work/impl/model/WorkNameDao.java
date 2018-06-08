/*
 * Copyright 2018 The Android Open Source Project
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

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link WorkName}s.
 */
@Dao
public interface WorkNameDao {

    /**
     * Inserts a {@link WorkName} into the table.
     *
     * @param workName The {@link WorkName} to insert
     */
    @Insert(onConflict = IGNORE)
    void insert(WorkName workName);

    /**
     * Retrieves all {@link WorkSpec} ids in the given named graph.
     *
     * @param name The matching name
     * @return All {@link WorkSpec} ids in the given named graph
     */
    @Query("SELECT work_spec_id FROM workname WHERE name=:name")
    List<String> getWorkSpecIdsWithName(String name);
}
