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
package androidx.work.impl.model

import androidx.annotation.RestrictTo
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Database entity that defines a mapping from a tag to a [WorkSpec] id.
 *
 */
@Entity(
    foreignKeys = [ForeignKey(
        entity = WorkSpec::class,
        parentColumns = ["id"],
        childColumns = ["work_spec_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )], primaryKeys = ["tag", "work_spec_id"], indices = [Index(value = ["work_spec_id"])]
)
@RestrictTo(
    RestrictTo.Scope.LIBRARY_GROUP
)
class WorkTag(
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "work_spec_id")
    val workSpecId: String
)
