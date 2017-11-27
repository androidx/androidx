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

package android.arch.background.workmanager.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * The Data Access Object for {@link WorkInput}.
 */
@Dao
public interface WorkInputDao {

    /**
     * Inserts the given {@link WorkInput} into the database.
     *
     * @param workInput The {@link WorkInput} to insert
     */
    @Insert
    void insert(WorkInput workInput);

    /**
     * Gets all of the input {@link WorkInput} for a given {@link WorkSpec}.
     *
     * @param id The identifier for the {@link WorkSpec}
     * @return A list of {@link Arguments}; this includes the ones specified when the work item is
     * created, as well as any arguments passed by work items directly preceding this one.  Note
     * that if not all preceding work items have finished executing, this call may return an
     * incomplete list of arguments.
     */
    @Query("SELECT arguments FROM WorkInput WHERE work_spec_id=:id")
    List<Arguments> getArguments(String id);
}
