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

/**
 * The Data Access Object for {@link Dependency}.
 */
@Dao
public interface DependencyDao {
    /**
     * Attempts to insert a {@link Dependency} into the database.
     *
     * @param dependency The {@link Dependency}s to insert
     */
    @Insert(onConflict = FAIL)
    void insertDependency(Dependency dependency);

    /**
     * Determines if a {@link WorkSpec is dependent on other {@link WorkSpec}s
     * that are not in a {@value Work#STATUS_SUCCEEDED} state.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return true if the {@link WorkSpec} is dependent on other {@link WorkSpec}s
     */
    @Query("SELECT COUNT(id) > 0 FROM workspec WHERE status!=2 AND id IN"
            + "(SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id)")
    boolean hasDependencies(String id); // TODO: Replace 2 with STATUS_SUCCEEDED constant
                                     // TODO: Refactor this method to a separate DAO.
}
