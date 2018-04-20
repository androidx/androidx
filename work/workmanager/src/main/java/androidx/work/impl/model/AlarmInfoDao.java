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

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A Data Access Object for {@link AlarmInfo}.
 */
@Dao
public interface AlarmInfoDao {

    /**
     * Inserts a {@link AlarmInfo} into the database.
     *
     * @param alarmInfo The {@link AlarmInfo} to be inserted into the database.
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    void insertAlarmInfo(@NonNull AlarmInfo alarmInfo);

    /**
     * @param workSpecId The {@link WorkSpec} identifier.
     * @return The instance of {@link AlarmInfo} if exists.
     */
    @Nullable
    @Query("SELECT * FROM alarmInfo WHERE work_spec_id=:workSpecId")
    AlarmInfo getAlarmInfo(@NonNull String workSpecId);

    /**
     * Removes alarms corresponding to the {@link WorkSpec} identifier.
     *
     * @param workSpecId The {@link WorkSpec} identifier.
     */
    @Query("DELETE FROM alarmInfo where work_spec_id=:workSpecId")
    void removeAlarmInfo(@NonNull String workSpecId);

}
