/*
 * Copyright 2024 The Android Open Source Project
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

/** Base class that holds common schema information about an entity. */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class BaseEntityBundle {
    @SerialName("tableName") public abstract val tableName: String
    @SerialName("createSql") public abstract val createSql: String
    @SerialName("fields") public abstract val fields: List<FieldBundle>
    @SerialName("primaryKey") public abstract val primaryKey: PrimaryKeyBundle
    @SerialName("indices") public abstract val indices: List<IndexBundle>
    @SerialName("foreignKeys") public abstract val foreignKeys: List<ForeignKeyBundle>

    public companion object {
        public const val NEW_TABLE_PREFIX: String = "_new_"
    }

    public val newTableName: String
        get() {
            return NEW_TABLE_PREFIX + tableName
        }

    public val fieldsByColumnName: Map<String, FieldBundle> by lazy {
        fields.associateBy { it.columnName }
    }

    /** CREATE TABLE SQL query that uses the actual table name. */
    public fun createTable(): String {
        return replaceTableName(createSql, tableName)
    }

    /** CREATE TABLE SQL query that uses the table name with "new" prefix. */
    public fun createNewTable(): String {
        return replaceTableName(createSql, newTableName)
    }

    /** Renames the table with [newTableName] to [tableName]. */
    public fun renameToOriginal(): String {
        return "ALTER TABLE $newTableName RENAME TO $tableName"
    }

    /** Creates the list of SQL queries that are necessary to create this entity. */
    public abstract fun buildCreateQueries(): List<String>
}
