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
package androidx.work.impl.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * The Data Access Object for [Dependency].
 */
@Dao
interface DependencyDao {
    /**
     * Attempts to insert a [Dependency] into the database.
     *
     * @param dependency The [Dependency]s to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDependency(dependency: Dependency)

    /**
     * Determines if a [WorkSpec] has completed all prerequisites.
     *
     * @param id The identifier for the [WorkSpec]
     * @return `true` if the [WorkSpec] has no pending prerequisites.
     */
    @Query(
        "SELECT COUNT(*)=0 FROM dependency WHERE work_spec_id=:id AND prerequisite_id IN " +
            "(SELECT id FROM workspec WHERE state!=" +
            WorkTypeConverters.StateIds.SUCCEEDED + ")"
    )
    fun hasCompletedAllPrerequisites(id: String): Boolean

    /**
     * Gets all the direct prerequisites for a particular [WorkSpec].
     *
     * @param id The [WorkSpec] identifier
     * @return A list of all prerequisites for `id`
     */
    @Query("SELECT prerequisite_id FROM dependency WHERE work_spec_id=:id")
    fun getPrerequisites(id: String): List<String>

    /**
     * Gets all [WorkSpec] id's dependent on a given id
     *
     * @param id A [WorkSpec] identifier
     * @return A list of all identifiers that depend on the input
     */
    @Query("SELECT work_spec_id FROM dependency WHERE prerequisite_id=:id")
    fun getDependentWorkIds(id: String): List<String>

    /**
     * Determines if a [WorkSpec] has any dependents.
     *
     * @param id A [WorkSpec] identifier
     * @return `true` if the [WorkSpec] has WorkSpecs that depend on it
     */
    @Query("SELECT COUNT(*)>0 FROM dependency WHERE prerequisite_id=:id")
    fun hasDependents(id: String): Boolean
}
