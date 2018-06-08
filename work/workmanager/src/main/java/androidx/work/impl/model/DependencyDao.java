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

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

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
    @Insert(onConflict = IGNORE)
    void insertDependency(Dependency dependency);

    /**
     * Determines if a {@link WorkSpec} has completed all prerequisites.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return {@code true} if the {@link WorkSpec} has no pending prerequisites.
     */
    @Query("SELECT COUNT(*)=0 FROM dependency WHERE work_spec_id=:id AND prerequisite_id IN "
            + "(SELECT id FROM workspec WHERE state!="
            + WorkTypeConverters.StateIds.SUCCEEDED + ")")
    boolean hasCompletedAllPrerequisites(String id);

    /**
     * Gets all the direct prerequisites for a particular {@link WorkSpec}.
     *
     * @param id The {@link WorkSpec} identifier
     * @return A list of all prerequisites for {@code id}
     */
    @Query("SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id")
    List<String> getPrerequisites(String id);

    /**
     * Gets all {@link WorkSpec} id's dependent on a given id
     *
     * @param id A {@link WorkSpec} identifier
     * @return A list of all identifiers that depend on the input
     */
    @Query("SELECT work_spec_id FROM dependency WHERE prerequisite_id=:id")
    List<String> getDependentWorkIds(String id);

    /**
     * Determines if a {@link WorkSpec} has any dependents.
     *
     * @param id A {@link WorkSpec} identifier
     * @return {@code true} if the {@link WorkSpec} has WorkSpecs that depend on it
     */
    @Query("SELECT COUNT(*)>0 FROM dependency WHERE prerequisite_id=:id")
    boolean hasDependents(String id);
}
