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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo
import androidx.room.migration.bundle.SchemaEqualityUtil.checkSchemaEquality
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class that holds the schema information about an [androidx.room.Fts3] or
 * [androidx.room.Fts4] entity.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FtsEntityBundle(
    @SerialName("tableName") override val tableName: String,
    @SerialName("createSql") override val createSql: String,
    @SerialName("fields") override val fields: List<FieldBundle>,
    @SerialName("primaryKey") override val primaryKey: PrimaryKeyBundle,
    @SerialName("indices") override val indices: List<IndexBundle> = emptyList(),
    @SerialName("foreignKeys") override val foreignKeys: List<ForeignKeyBundle> = emptyList(),
    @SerialName("ftsVersion") val ftsVersion: String,
    @SerialName("ftsOptions") val ftsOptions: FtsOptionsBundle,
    @SerialName("contentSyncTriggers") val contentSyncSqlTriggers: List<String>
) : BaseEntityBundle(), SchemaEquality<FtsEntityBundle> {

    /** Creates the list of SQL queries that are necessary to create this entity. */
    override fun buildCreateQueries(): List<String> {
        return buildList {
            add(createTable())
            addAll(contentSyncSqlTriggers)
        }
    }

    override fun isSchemaEqual(other: FtsEntityBundle): Boolean {
        if (tableName != other.tableName) {
            return false
        }
        return checkSchemaEquality(fieldsByColumnName, other.fieldsByColumnName) &&
            checkSchemaEquality(primaryKey, other.primaryKey) &&
            checkSchemaEquality(indices, other.indices) &&
            checkSchemaEquality(foreignKeys, other.foreignKeys) &&
            ftsVersion == other.ftsVersion &&
            checkSchemaEquality(ftsOptions, other.ftsOptions)
    }

    /** Gets the list of shadow table names corresponding to the FTS virtual table. */
    val shadowTableNames: List<String> by lazy {
        val currentTable = this@FtsEntityBundle.tableName
        buildList { SHADOW_TABLE_NAME_SUFFIXES.forEach { suffix -> add(currentTable + suffix) } }
    }

    companion object {
        private val SHADOW_TABLE_NAME_SUFFIXES =
            listOf("_content", "_segdir", "_segments", "_stat", "_docsize")
    }
}
