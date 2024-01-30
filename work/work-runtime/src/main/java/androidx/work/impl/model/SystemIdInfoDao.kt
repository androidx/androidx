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
package androidx.work.impl.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@JvmDefaultWithCompatibility
/**
 * A Data Access Object for [SystemIdInfo].
 */
@Dao
interface SystemIdInfoDao {
    /**
     * Inserts a [SystemIdInfo] into the database.
     *
     * @param systemIdInfo The [SystemIdInfo] to be inserted into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSystemIdInfo(systemIdInfo: SystemIdInfo)

    /**
     * @param workSpecId The [WorkSpec] identifier.
     * @return The instance of [SystemIdInfo] if exists.
     */
    @Query("SELECT * FROM SystemIdInfo WHERE work_spec_id=:workSpecId AND generation=:generation")
    fun getSystemIdInfo(workSpecId: String, generation: Int): SystemIdInfo?

    fun getSystemIdInfo(id: WorkGenerationalId) = getSystemIdInfo(id.workSpecId, id.generation)

    /**
     * Removes [SystemIdInfo] corresponding to the [WorkSpec] identifier.
     *
     * @param workSpecId The [WorkSpec] identifier.
     */
    @Query("DELETE FROM SystemIdInfo where work_spec_id=:workSpecId AND generation=:generation")
    fun removeSystemIdInfo(workSpecId: String, generation: Int)

    /**
     * Removes [SystemIdInfo] corresponding to the [WorkSpec] identifier.
     *
     * @param workSpecId The [WorkSpec] identifier.
     */
    @Query("DELETE FROM SystemIdInfo where work_spec_id=:workSpecId")
    fun removeSystemIdInfo(workSpecId: String)

    fun removeSystemIdInfo(id: WorkGenerationalId) =
        removeSystemIdInfo(id.workSpecId, id.generation)

    /**
     * @return The [List] of [WorkSpec] ids.
     */
    @Query("SELECT DISTINCT work_spec_id FROM SystemIdInfo")
    fun getWorkSpecIds(): List<String>
}
