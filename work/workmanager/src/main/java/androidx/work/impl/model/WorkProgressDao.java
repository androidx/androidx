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

package androidx.work.impl.model;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.work.Data;

import java.util.List;

/**
 * A DAO for {@link WorkProgress}.
 *
 * @hide
 */
@Dao
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface WorkProgressDao {
    /**
     * Inserts a {@link WorkProgress} into the database.
     *
     * @param progress The {@link WorkProgress}
     */
    @Insert(onConflict = REPLACE)
    void insert(@NonNull WorkProgress progress);

    /**
     * @param workSpecId The {@link String} workSpec id
     * @return The progress {@link Data} associated with the given {@link String} workSpec id.
     */
    @Nullable
    @Query("SELECT progress FROM WorkProgress WHERE work_spec_id=:workSpecId")
    Data getProgressForWorkSpecId(@NonNull String workSpecId);

    /**
     * @param workSpecIds The {@link List} of workSpec ids
     * @return The {@link List} of progress {@link Data} associated with the given workSpec ids.
     */
    @NonNull
    @Query("SELECT progress FROM WorkProgress WHERE work_spec_id IN (:workSpecIds)")
    List<Data> getProgressForWorkSpecIds(@NonNull List<String> workSpecIds);
}
