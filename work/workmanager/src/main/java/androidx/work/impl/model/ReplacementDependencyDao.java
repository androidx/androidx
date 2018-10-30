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

/**
 * The Data Access Object for {@link ReplacementDependency}.
 */
@Dao
public interface ReplacementDependencyDao {

    /**
     * Attempts to insert a {@link ReplacementDependency} into the database.
     *
     * @param replacementDependency The ReplacementDependency to insert
     */
    @Insert(onConflict = IGNORE)
    void insertReplacementDependency(ReplacementDependency replacementDependency);

    /**
     * Returns if a particular WorkSpec has blocking replacement dependencies.
     *
     * @param id The WorkSpec id to check
     * @return {@code true} if the WorkSpec has blocking replacement dependencies
     */
    @Query("SELECT COUNT(*)>0 FROM replacementdependency WHERE work_spec_id=:id")
    boolean hasBlockingReplacementDependencies(String id);
}
