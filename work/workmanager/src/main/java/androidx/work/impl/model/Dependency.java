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

package androidx.work.impl.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * Database entity that defines a dependency between two {@link WorkSpec}s.
 *
 * @hide
 */

@Entity(foreignKeys = {
        @ForeignKey(
                entity = WorkSpec.class,
                parentColumns = "id",
                childColumns = "work_spec_id",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE),
        @ForeignKey(
                entity = WorkSpec.class,
                parentColumns = "id",
                childColumns = "prerequisite_id",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)},
        primaryKeys = {"work_spec_id", "prerequisite_id"},
        indices = {
                @Index(value = {"work_spec_id"}),
                @Index(value = {"prerequisite_id"})})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Dependency {
    @NonNull
    @ColumnInfo(name = "work_spec_id")
    public final String workSpecId;

    @NonNull
    @ColumnInfo(name = "prerequisite_id")
    public final String prerequisiteId;

    public Dependency(@NonNull String workSpecId, @NonNull String prerequisiteId) {
        this.workSpecId = workSpecId;
        this.prerequisiteId = prerequisiteId;
    }
}
