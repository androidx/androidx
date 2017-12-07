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

package android.arch.background.workmanager.impl.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.support.annotation.NonNull;

/**
 * Database entity that defines a mapping from a tag to a {@link WorkSpec} id.
 */

@Entity(foreignKeys = {
        @ForeignKey(
                entity = WorkSpec.class,
                parentColumns = "id",
                childColumns = "work_spec_id",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)},
        primaryKeys = {"tag", "work_spec_id"})
public class WorkTag {

    @NonNull
    @ColumnInfo(name = "tag")
    String mTag;

    @NonNull
    @ColumnInfo(name = "work_spec_id")
    String mWorkSpecId;

    public WorkTag(@NonNull String tag, @NonNull String workSpecId) {
        mTag = tag;
        mWorkSpecId = workSpecId;
    }

    @NonNull
    public String getTag() {
        return mTag;
    }

    public void setTag(@NonNull String mTag) {
        this.mTag = mTag;
    }

    @NonNull
    public String getWorkSpecId() {
        return mWorkSpecId;
    }

    public void setWorkSpecId(@NonNull String mWorkSpecId) {
        this.mWorkSpecId = mWorkSpecId;
    }
}
