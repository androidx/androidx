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
import androidx.room.migration.bundle.SchemaEqualityUtil.checkSchemaEquality

import com.google.gson.annotations.SerializedName

/**
 * Data class that holds the schema information about an
 * [androidx.room.Entity].
 *
 * @constructor Creates a new bundle.
 *
 * @property tableName The table name.
 * @property createSql Create query with the table name placeholder.
 * @property fields The list of fields.
 * @property primaryKey The primary key.
 * @property indices The list of indices
 * @property foreignKeys The list of foreign keys
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class EntityBundle(
    @SerializedName("tableName")
    public open val tableName: String,
    @SerializedName("createSql")
    public open val createSql: String,
    @SerializedName("fields")
    public open val fields: List<FieldBundle>,
    @SerializedName("primaryKey")
    public open val primaryKey: PrimaryKeyBundle,
    @SerializedName("indices")
    public open val indices: List<IndexBundle>,
    @SerializedName("foreignKeys")
    public open val foreignKeys: List<ForeignKeyBundle>
) : SchemaEquality<EntityBundle> {

    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    private constructor() : this(
        "",
        "",
        emptyList(),
        PrimaryKeyBundle(false, emptyList()),
        emptyList(),
        emptyList()
    )

    public companion object {
        public const val NEW_TABLE_PREFIX: String = "_new_"
    }

    @Transient
    public open val newTableName: String = NEW_TABLE_PREFIX + tableName

    @delegate:Transient
    public open val fieldsByColumnName: Map<String, FieldBundle> by lazy {
        fields.associateBy { it.columnName }
    }

    /**
     * @return Create table SQL query that uses the actual table name.
     */
    public open fun createTable(): String {
        return replaceTableName(createSql, tableName)
    }

    /**
     * @return Create table SQL query that uses the table name with "new" prefix.
     */
    public open fun createNewTable(): String {
        return replaceTableName(createSql, newTableName)
    }

    /**
     * @return Renames the table with {@link #getNewTableName()} to {@link #getTableName()}.
     */
    public open fun renameToOriginal(): String {
        return "ALTER TABLE " + newTableName + " RENAME TO " + tableName
    }

    /**
     * @return Creates the list of SQL queries that are necessary to create this entity.
     */
    public open fun buildCreateQueries(): Collection<String> {
        return buildList {
            add(createTable())
            this@EntityBundle.indices.forEach { indexBundle ->
                add(indexBundle.create(tableName))
            }
        }
    }

    override fun isSchemaEqual(other: EntityBundle): Boolean {
        if (tableName != other.tableName) {
            return false
        }
        return checkSchemaEquality(
            fieldsByColumnName,
            other.fieldsByColumnName
        ) &&
            checkSchemaEquality(primaryKey, other.primaryKey) &&
            checkSchemaEquality(indices, other.indices) &&
            checkSchemaEquality(foreignKeys, other.foreignKeys)
    }
}
