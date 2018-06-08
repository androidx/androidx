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
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Stores system ids for a {@link WorkSpec} id.
 *
 * @hide
 */
@Entity(foreignKeys = {
                @ForeignKey(
                        entity = WorkSpec.class,
                        parentColumns = "id",
                        childColumns = "work_spec_id",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.CASCADE)})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemIdInfo {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "work_spec_id")
    public final String workSpecId;

    @ColumnInfo(name = "system_id")
    public final int systemId;

    public SystemIdInfo(@NonNull String workSpecId, int systemId) {
        this.workSpecId = workSpecId;
        this.systemId = systemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SystemIdInfo that = (SystemIdInfo) o;

        if (systemId != that.systemId) return false;
        return workSpecId.equals(that.workSpecId);
    }

    @Override
    public int hashCode() {
        int result = workSpecId.hashCode();
        result = 31 * result + systemId;
        return result;
    }
}
