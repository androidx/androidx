/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * Database entity that defines a dependency between two {@link WorkItem}s.
 */

// TODO(xbhatnag): Replace with single foreign key. (b/65681278)
@Entity(foreignKeys = {
        @ForeignKey(
                entity = WorkItem.class,
                parentColumns = "id",
                childColumns = "work_item_id",
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
        @ForeignKey(
                entity = WorkItem.class,
                parentColumns = "id",
                childColumns = "prerequisite_id",
                onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)},
        indices = {@Index(value = "work_item_id"), @Index(value = "prerequisite_id")})
class Dependency {
    @ColumnInfo(name = "id")
    @PrimaryKey
    Integer mId;

    @ColumnInfo(name = "work_item_id")
    int mWorkItemId;

    @ColumnInfo(name = "prerequisite_id")
    int mPrerequisiteId;
}
