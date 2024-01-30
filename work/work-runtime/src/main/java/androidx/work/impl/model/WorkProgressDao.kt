/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.work.Data

/**
 * A DAO for [WorkProgress].
 *
 */
@Dao
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface WorkProgressDao {
    /**
     * Inserts a [WorkProgress] into the database.
     *
     * @param progress The [WorkProgress]
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(progress: WorkProgress)

    /**
     * Deletes a [WorkProgress] from the database.
     *
     * @param workSpecId The [WorkSpec] id
     */
    @Query("DELETE from WorkProgress where work_spec_id=:workSpecId")
    fun delete(workSpecId: String)

    /**
     * Removes all [WorkProgress] entries from the [WorkProgress] table.
     */
    @Query("DELETE FROM WorkProgress")
    fun deleteAll()

    /**
     * @param workSpecId The [String] workSpec id
     * @return The progress [Data] associated with the given [String] workSpec id.
     */
    @Query("SELECT progress FROM WorkProgress WHERE work_spec_id=:workSpecId")
    fun getProgressForWorkSpecId(workSpecId: String): Data?
}
