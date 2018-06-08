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

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A Data Access Object for {@link SystemIdInfo}.
 */
@Dao
public interface SystemIdInfoDao {
    /**
     * Inserts a {@link SystemIdInfo} into the database.
     *
     * @param systemIdInfo The {@link SystemIdInfo} to be inserted into the database.
     */
    @Insert(onConflict = REPLACE)
    void insertSystemIdInfo(@NonNull SystemIdInfo systemIdInfo);

    /**
     * @param workSpecId The {@link WorkSpec} identifier.
     * @return The instance of {@link SystemIdInfo} if exists.
     */
    @Nullable
    @Query("SELECT * FROM SystemIdInfo WHERE work_spec_id=:workSpecId")
    SystemIdInfo getSystemIdInfo(@NonNull String workSpecId);

    /**
     * Removes {@link SystemIdInfo} corresponding to the {@link WorkSpec} identifier.
     *
     * @param workSpecId The {@link WorkSpec} identifier.
     */
    @Query("DELETE FROM SystemIdInfo where work_spec_id=:workSpecId")
    void removeSystemIdInfo(@NonNull String workSpecId);
}
