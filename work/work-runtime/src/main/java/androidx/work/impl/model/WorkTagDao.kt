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
package androidx.work.impl.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@JvmDefaultWithCompatibility
/**
 * The Data Access Object for [WorkTag]s.
 */
@Dao
interface WorkTagDao {
    /**
     * Inserts a [WorkTag] into the table.
     *
     * @param workTag The [WorkTag] to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(workTag: WorkTag)

    /**
     * Deletes [WorkSpec]s from the database.
     *
     * @param id The WorkSpec id to delete.
     */
    @Query("DELETE FROM worktag WHERE work_spec_id=:id")
    fun deleteByWorkSpecId(id: String)

    /**
     * Retrieves all [WorkSpec] ids with the given tag.
     *
     * @param tag The matching tag
     * @return All [WorkSpec] ids with the given tag
     */
    @Query("SELECT work_spec_id FROM worktag WHERE tag=:tag")
    fun getWorkSpecIdsWithTag(tag: String): List<String>

    /**
     * Retrieves all tags for a given [WorkSpec] id.
     *
     * @param id The id of the [WorkSpec]
     * @return A list of tags for that [WorkSpec]
     */
    @Query("SELECT DISTINCT tag FROM worktag WHERE work_spec_id=:id")
    fun getTagsForWorkSpecId(id: String): List<String>

    fun insertTags(id: String, tags: Set<String>) {
        tags.forEach { tag -> insert(WorkTag(tag, id)) }
    }
}
