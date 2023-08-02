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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo

import com.google.gson.annotations.SerializedName

/**
 * @constructor Creates a foreign key bundle with the given parameters. Holds the information about
 * a foreign key reference.
 *
 * @property table             The target table
 * @property onDelete          OnDelete action
 * @property onUpdate          OnUpdate action
 * @property columns           The list of columns in the current table
 * @property referencedColumns The list of columns in the referenced table
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class ForeignKeyBundle(
    @field:SerializedName("table")
    public open val table: String,
    @field:SerializedName("onDelete")
    public open val onDelete: String,
    @field:SerializedName("onUpdate")
    public open val onUpdate: String,
    @field:SerializedName("columns")
    public open val columns: List<String>,
    @field:SerializedName("referencedColumns")
    public open val referencedColumns: List<String>
) : SchemaEquality<ForeignKeyBundle> {

    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    private constructor() : this("", "", "", emptyList(), emptyList())

    override fun isSchemaEqual(other: ForeignKeyBundle): Boolean {
        if (table != other.table) return false
        if (onDelete != other.onDelete) return false
        if (onUpdate != other.onUpdate) return false
        // order matters
        return (columns == other.columns && referencedColumns == other.referencedColumns)
    }
}
