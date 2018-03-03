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

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Stores Alarm ids for a {@link WorkSpec}.
 */
@Entity(tableName = "alarmInfo",
        foreignKeys = {
                @ForeignKey(
                        entity = WorkSpec.class,
                        parentColumns = "id",
                        childColumns = "work_spec_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE)})
public class AlarmInfo {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "work_spec_id")
    String mWorkSpecId;

    @ColumnInfo(name = "alarm_id")
    int mAlarmId;

    public AlarmInfo() {
        // Default no-arg constructor for Room.
    }

    @Ignore
    public AlarmInfo(@NonNull String workSpecId, int alarmId) {
        mWorkSpecId = workSpecId;
        mAlarmId = alarmId;
    }

    public int getAlarmId() {
        return mAlarmId;
    }

    public void setAlarmId(int alarmId) {
        mAlarmId = alarmId;
    }

    public String getWorkSpecId() {
        return mWorkSpecId;
    }

    public void setWorkSpecId(@NonNull String workSpecId) {
        mWorkSpecId = workSpecId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlarmInfo alarmInfo = (AlarmInfo) o;

        if (mAlarmId != alarmInfo.mAlarmId) return false;
        return mWorkSpecId != null ? mWorkSpecId.equals(alarmInfo.mWorkSpecId)
                : alarmInfo.mWorkSpecId == null;
    }

    @Override
    public int hashCode() {
        int result = mWorkSpecId != null ? mWorkSpecId.hashCode() : 0;
        result = 31 * result + mAlarmId;
        return result;
    }
}
