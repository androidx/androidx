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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class that holds the information about a foreign key reference, i.e.
 * [androidx.room.ForeignKey].
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ForeignKeyBundle(
    @SerialName("table") val table: String,
    @SerialName("onDelete") val onDelete: String,
    @SerialName("onUpdate") val onUpdate: String,
    @SerialName("columns") val columns: List<String>,
    @SerialName("referencedColumns") val referencedColumns: List<String>
) : SchemaEquality<ForeignKeyBundle> {

    override fun isSchemaEqual(other: ForeignKeyBundle): Boolean {
        if (table != other.table) return false
        if (onDelete != other.onDelete) return false
        if (onUpdate != other.onUpdate) return false
        // order matters
        return (columns == other.columns && referencedColumns == other.referencedColumns)
    }
}
